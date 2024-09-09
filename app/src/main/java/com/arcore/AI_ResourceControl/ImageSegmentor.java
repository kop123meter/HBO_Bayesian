/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.arcore.AI_ResourceControl;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
/**
 * Segments images with Tensorflow Lite.
 */
public abstract class ImageSegmentor {
  // Display preferences
  private static final float GOOD_PROB_THRESHOLD = 0.3f;
  private static final int SMALL_COLOR = 0xffddaa88;
  private int threads;
  private String device;
  /** Tag for the {@link Log}. */
  private static final String TAG = "TfLiteCameraDemo";

  /** Number of results to show in the UI. */
  private static final int RESULTS_TO_SHOW = 3;

  /** Dimensions of inputs. */
  private static final int DIM_BATCH_SIZE = 1;

  private static final int DIM_PIXEL_SIZE = 3;
  String fileseries;
  MainActivity instance;
  /** Preallocated buffers for storing image data in. */
  private int[] intValues = new int[getImageSizeX() * getImageSizeY()];

  /** Options for configuring the Interpreter. */
  private final Interpreter.Options tfliteOptions = new Interpreter.Options();

  /** The loaded TensorFlow Lite model. */
  private MappedByteBuffer tfliteModel;

  /** An instance of the driver class to run model inference with Tensorflow Lite. */
  protected Interpreter tflite;

  /** Labels corresponding to the output of the vision model. */
  private List<String> labelList;

  /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
  protected ByteBuffer imgData = null;

  /** A background image**/

  public Bitmap result = null;
  public long duration = 0;

  /** multi-stage low pass filter * */
  private float[][] filterLabelProbArray = null;

  private static final int FILTER_STAGES = 3;
  private static final float FILTER_FACTOR = 0.4f;

  private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
      new PriorityQueue<>(
          RESULTS_TO_SHOW,
          new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
              return (o1.getValue()).compareTo(o2.getValue());
            }
          });

  /** holds a gpu delegate */
  GpuDelegate gpuDelegate = null;
  /** holds an nnapi delegate */
  NnApiDelegate nnapiDelegate = null;

  /** Initializes an {@code ImageSegmentor}. */
  ImageSegmentor(Activity activity) throws IOException {
    tfliteModel = loadModelFile(activity);
    tflite = new Interpreter(tfliteModel, tfliteOptions);
    imgData =
        ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE
                * getImageSizeX()
                * getImageSizeY()
                * DIM_PIXEL_SIZE
                * getNumBytesPerChannel());
    imgData.order(ByteOrder.nativeOrder());
    Log.d(TAG, "Created a Tensorflow Lite Image Segmentor.");
  }

  /** Segments a frame from the preview stream. */
  void segmentFrame(Bitmap bitmap){
          ///, int mode, Bitmap fgd, Bitmap bgd) {
    if (tflite == null) {
      Log.e(TAG, "Image segmentor has not been initialized; Skipped.");
      //builder.append(new SpannableString("Uninitialized Segmentor."));
    }
    convertBitmapToByteBuffer(bitmap);
    // Here's where the magic happens!!!
    long startTime = SystemClock.uptimeMillis();
    System.out.println("Current Device:" + getDevice());
    runInference(getDevice());


   //Log.d("MODE", String.valueOf(mode));

//    if(mode==1)
//      // Perform alpha blend
//      result=imageblend(fgd, bgd, Boolean.FALSE );
//    else if (mode==2)
//      // Perform alpha blend with color transfer
//      result=imageblend(fgd, bgd, Boolean.TRUE );
//    else if(mode==0)
//      // Apply bokeh effect tp video
//      result=videobokeh(fgd);
//    else if (mode==3)
//      result=smoothblend(fgd, bgd, Boolean.FALSE );

    long endTime = SystemClock.uptimeMillis();

    duration = endTime - startTime;
//    Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));

  }



  private void recreateInterpreter() {
     if (tflite != null) {
      tflite.close();
      tflite = new Interpreter(tfliteModel, tfliteOptions);
    }
  }

  public void useGpu() {
    if (gpuDelegate == null) {
      gpuDelegate =  new GpuDelegate();
      tfliteOptions.addDelegate(gpuDelegate);
      device = "GPU";
      tfliteOptions.setAllowFp16PrecisionForFp32(Boolean.TRUE);
      recreateInterpreter();

    }
  }

  public void useCPU() {

    device = "CPU";
    recreateInterpreter();
  }

  public void useNNAPI() {
    nnapiDelegate = new NnApiDelegate();
    tfliteOptions.addDelegate(nnapiDelegate);
    device = "NNAPI";
    recreateInterpreter();
  }

  public void useRemote(){

    device = "SERVER";
    recreateInterpreter();
  }


  String getDevice() {
    return device;
  }

  public void setNumThreads(int numThreads) {
    tfliteOptions.setNumThreads(numThreads);
    threads = numThreads;
    recreateInterpreter();
  }
  /** return thread count*/
  public int getNumThreads() {
    return threads;
  }


  /** Closes tflite to release resources. */
  public void close() {
    tflite.close();
    tflite = null;
    if (gpuDelegate != null) {
      gpuDelegate.close();
      gpuDelegate = null;
    }
    if (nnapiDelegate != null) {
      nnapiDelegate.close();
      nnapiDelegate = null;
    }
    tfliteModel = null;
  }


  /** Memory-map the model file in Assets. */
  private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
    AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
    FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset = fileDescriptor.getStartOffset();
    long declaredLength = fileDescriptor.getDeclaredLength();
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
  }

  /** Writes Image data into a {@code ByteBuffer}. */
  private void convertBitmapToByteBuffer(Bitmap bitmap) {
    if (imgData == null) {
      return;
    }
    imgData.rewind();
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    // Convert the image to floating point.
    int pixel = 0;
    long startTime = SystemClock.uptimeMillis();
    for (int i = 0; i < getImageSizeX(); ++i) {
      for (int j = 0; j < getImageSizeY(); ++j) {
        final int val = intValues[pixel++];
        addPixelValue(val);
      }
    }
    long endTime = SystemClock.uptimeMillis();
    Log.d(TAG, "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
  }


  /**
   * Get the name of the model file stored in Assets.
   *
   * @return
   */
  protected abstract String getModelPath();


  /**
   * Get the image size along the x axis.
   *
   * @return
   */
  protected abstract int getImageSizeX();

  /**
   * Get the image size along the y axis.
   *
   * @return
   */
  protected abstract int getImageSizeY();

  /**
   * Get the number of bytes that is used to store a single color channel value.
   *
   * @return
   */
  protected abstract int getNumBytesPerChannel();

  /**
   * Add pixelValue to byteBuffer.
   *
   * @param pixelValue
   */
  protected abstract void addPixelValue(int pixelValue);

  protected abstract void runInference(String device);

  /**
   * Define Offload Server Ip and Port
   * */

  /**
   * Get the total number of labels.
   *
   * @return
   */

  //public abstract Bitmap imageblend(Bitmap fg_bmp, Bitmap bg_bmp, Boolean harmonize);

  /**
   * Perform alpha blend with mask
   *
   * @return
   */

  //public abstract Bitmap videobokeh(Bitmap fg_bmp);

  /**
   * Apply bokeh effect on video
   *
   * @return
   */
  //public abstract Bitmap smoothblend(Bitmap fg_bmp, Bitmap bg_bmp, Boolean harmonize);
  /**
   * Use renderscript for smoothing and blending
   *
   * @return
   */
}
