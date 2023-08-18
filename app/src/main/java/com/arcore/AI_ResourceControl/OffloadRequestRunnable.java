package com.arcore.AI_ResourceControl;
import android.content.Context;
import android.media.Image;
import android.os.Environment;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 *      Communication with server running on a thread
 */



public class OffloadRequestRunnable implements Runnable {

    private final ModelRequest modelRequest;


  //  private final LinkedList<ModelRequest> repeatedRequestList;;

    // File name indicates the name of the file to send
    private final String filename;

    // Context needed to save the image in the internal storage
    private final Context context;

    //percent reduction
    private final String percReduc;
    private final float cacheratio;
    private final float perc;
   // private List<Float> cacheratio;
    private final ModelRequestManager mInstance;

    static final int DOWNLOAD_FAILED = -1;
    static final int DOWNLOAD_PENDING = 1;
    static final int DOWNLOAD_STARTED = 2;
    static final int DOWNLOAD_COMPLETE = 3;
    Image imgFrame;

    //download chunk size, make sure to match on server thread
    private final int BUFFER_SIZE = 16000;


    //final ModelRequestTask modelDownloadTask;


    //public ModelRequestRunnable( float cr,String filename, float percReduc, Context context, ModelRequestManager mInstance) {

    public OffloadRequestRunnable(ModelRequest modelRequest, ModelRequestManager mInstance ) {
        this.modelRequest = modelRequest;

        this.filename = modelRequest.getFilename();
        this.context = modelRequest.getAppContext();
        this.percReduc = Float.toString(modelRequest.getPercentageReduction());
        this.perc=modelRequest.getPercentageReduction();
        this.mInstance = mInstance;
        this.cacheratio=modelRequest.getCache();
       // this.imgFrame=modelRequest.getImage();

        //this.repeatedRequestList= new LinkedList<ModelRequest>(repeatedRList);



    }


    @Override
    public void run() {


    try{

//instead of chechking all ratios you can check cache andddd also check if percreduc !=1


            Log.d("OffloadRequest", "Entering Runnable");

            try {

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                try {

                    // Establish connection with the server
                    //@@@pc address
                    Socket socket = new Socket("192.168.1.41", 4444);
                    // Open output stream
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedInputStream socketInputStream1;
                    socketInputStream1 = new BufferedInputStream(socket.getInputStream());
                /*
                    //send image address and task type to run inference
                    String img_add="mouse.jpg";
                    String task="classifier";

                    out.println(img_add);
                     out.println(task);
                    // flush stream
                    out.flush();
                  */

 //*********** ADDD MARCH 23 SEND Nil.jpeg to server === PART 2 of server

                    File out_file=  new File(context.getExternalFilesDir(null), "/frame.jpg");
                    FileInputStream outputFile = new FileInputStream(out_file);
                    OutputStream socketOutputStream = socket.getOutputStream();
                    int read;
                    byte[] buffer = new byte[160000];
                    int writingtime=0;
                    while ((read = outputFile.read(buffer)) != -1) {
                        long startTime = System.currentTimeMillis();
                        socketOutputStream.write(buffer, 0, read);
                        long endTime = System.currentTimeMillis();
                        System.out.println("socketOutputStream buffer [" + read + " bytes] write time 1: " + (endTime - startTime) + " milliseconds");
                         writingtime+= (endTime - startTime);
                        socketOutputStream.flush();
                    }
                    //send end of message
                    out.print("!]!!]!!");
                    out.flush();

//     needed??? // need to wait until server receives the frame and give us alarm
                     while (!((new String(in.readLine())).equals("Frame received"))) ;


          //*********** ADDD MARCH 23 SEND Nil.jpeg to server === PART 2 of server

//************** PART 2 PHONE To recieve result.txt

                    FileWriter myWriter = new FileWriter(context.getExternalFilesDir(null)+ "/inference.txt");
                    long startTime2 = System.currentTimeMillis();
                    String s;

                    while ((s = in.readLine()) != null) {

                        long startTime = System.currentTimeMillis();
                        if (s.endsWith("!]]!][!")) {
                            // don't write last -7 bytes, those are for the delimiter
                            myWriter.write(s, 0, s.length() - 7);
                            break;
                        }
                        else  myWriter.write(s);

                        long endTime = System.currentTimeMillis();
                        Log.d("Inference result", " Inference resultwrite time: " + (endTime - startTime) + " milliseconds");
                    }

                    long endTime2 = System.currentTimeMillis();
                    Log.d("tot time", "Total execution Time: " + (endTime2 - startTime2) + " milliseconds");

                    myWriter.close();
                    // fout.flush();

                    // tell server that file is received so it doesn't close connection
                    PrintWriter outM = new PrintWriter(socket.getOutputStream(), true);
                    outM.println("File received");

//**************To recieve result.txt

//added from modelreqmanager
                    Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
                    msg.obj = modelRequest;
                    modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);
                    ModelRequestManager.mRequestList.remove(modelRequest);

                    outputFile.close();

                    out.close();
                    outM.flush();
                    // close all the streams

                    socketInputStream1.close();
                    socketOutputStream.close();
                   // fout.close();
                    outM.close();

                    in.close();

                    socket.close();


                }//}


                catch (Exception e) {
                    mInstance.handleState(DOWNLOAD_FAILED, modelRequest);
                    Log.w("ModelRequestRunnable", e.getMessage());
                }


            } catch (InterruptedException e) {

                Log.w("ModelRequestRunnable", e.getMessage());
            } finally {
                //modelDownloadTask.setDownloadThread(null);

                //Thread.interrupted();
            }




    }
    catch (Exception e) {
        mInstance.handleState(DOWNLOAD_FAILED, modelRequest);
        Log.w("ModelRequestRunnable", e.getMessage());
    }
    }//run
}
