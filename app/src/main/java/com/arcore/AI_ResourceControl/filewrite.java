package com.arcore.AI_ResourceControl;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.ux.ArFragment;

public class filewrite implements  Runnable{

    MainActivity minstance;


    public filewrite(MainActivity mInstance){

        minstance=mInstance;



    }
    @Override
    public void run() {


      Timer  t = new Timer();

       t.scheduleAtFixedRate(




                new TimerTask() {
                    public void run() {
                        float disfromfirstobj=0f;
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                        dateFormat.format(new Date());
                        String current_gpu = null;
                        String current_freq=null;
                        float mean_gpu=0;
                        try {


                            // if(minstance.renderArray[1]!=null)
                             disfromfirstobj= minstance.renderArray.get(0).return_distance();

                            //   GLES20.glDisable(GLES20.GL_CULL_FACE);

                            String[] InstallBusyBoxCmd2 = new String[]{// trying to make gpu freq fixed
                                    "su", "-c", "cat /sys/class/kgsl/kgsl-3d0/max_clock_mhz"};

                            //Process
                            minstance.process2 = Runtime.getRuntime().exec(InstallBusyBoxCmd2);
                            BufferedReader stdInput = new BufferedReader(new
                                    InputStreamReader(minstance.process2.getInputStream()));
// Read the output from the command
                            current_freq = stdInput.readLine();


                            String[] InstallBusyBoxCmd = new String[]{
                                    "su", "-c", "cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"};
                            minstance. process2 = Runtime.getRuntime().exec(InstallBusyBoxCmd);

                            stdInput = new BufferedReader(new
                                    InputStreamReader(minstance.process2.getInputStream()));
// Read the output from the command
                            current_gpu = stdInput.readLine();
                            if (current_gpu != null) {
                                String[] separator = current_gpu.split("%");
                                mean_gpu = mean_gpu + Float.parseFloat(separator[0]);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        String item2 =  dateFormat.format(new Date()) + " current_gpu "+ mean_gpu + " dis "+disfromfirstobj+  " " + minstance.total_tris +" "+ current_freq +"\n";

                        try {
                            FileOutputStream os = new FileOutputStream(minstance.GPU_usage, true);
                            os.write(item2.getBytes());
                            os.close();
                            System.out.println(item2);


                        } catch (IOException e) {
                            Log.e("StatWriting", e.getMessage());
                        }


                    }

                },
                0,      // run first occurrence immediatetly
                1000);


    }




    public void givenUsingTimer_whenSchedulingTaskOnce_thenCorrect() {
        TimerTask task = new TimerTask() {
            public void run() {




                Float mean_gpu=0f;

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                dateFormat.format(new Date());
                String current_gpu = null;
                try {
                    Process process;
                    int i=0;
                    for(i=0 ;i<5;i++) {
                        String[] InstallBusyBoxCmd = new String[]{
                                "su", "-c", "cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"};

                        process = Runtime.getRuntime().exec(InstallBusyBoxCmd);
                        BufferedReader stdInput = new BufferedReader(new
                                InputStreamReader(process.getInputStream()));
// Read the output from the command
                        //System.out.println("Here is the standard output of the command:\n");
                        current_gpu = stdInput.readLine();
                        if (current_gpu != null) {
                            String[] separator = current_gpu.split("%");
                            mean_gpu = mean_gpu + Float.parseFloat(separator[0]);
                        }

                    }
                    mean_gpu= mean_gpu/i;

                } catch (IOException e) {
                    e.printStackTrace();
                }


                String item2 =  dateFormat.format(new Date()) + " num_of_tris: " + minstance.total_tris+ " current_gpu "+ mean_gpu +"\n";
                // + " virtual area " + total_area + " virtual vol " + total_vol + " " + fileName + "\n";
                try {
                    FileOutputStream os = new FileOutputStream(minstance.tris_num, true);
                    os.write(item2.getBytes());
                    os.close();
                    System.out.println(item2);


                } catch (IOException e) {
                    Log.e("StatWriting", e.getMessage());
                }



            }
        };
     //   Timer timer
                minstance.t2= new Timer("Timer");

        long delay = 3000L;
       // timer.schedule(task, delay);
        minstance.t2 .schedule(task, delay);
    }

}
