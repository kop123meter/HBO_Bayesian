package com.arcore.AI_ResourceControl;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.UnknownHostException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * if you wanna test hbo vs baselines, make baseline1_2 flag true
 * This class is to communicate with java server, it requests the suggested delegate and triangle count, then sends a msg to the
 * activity main and applies the delegates by calling the background thread named Bayesian and function  bys.apply_delegate_tris(selected_combinations);
 *      Communication with server running on a thread
 */



public class DelegateRequestRunnable implements Runnable {

    private final ModelRequest modelRequest;

    // Context needed to save the image in the internal storage
    private final Context context;

    String bayesian_delg="";

    private final ModelRequestManager mInstance;

    static final int DOWNLOAD_FAILED = -1;
    static final int DOWNLOAD_PENDING = 1;
    static final int DOWNLOAD_STARTED = 2;
    static final int DOWNLOAD_COMPLETE = 3;

    boolean baseline1_2=false;// when we wanna run bayesian + two baselines we set this true here and in the activity main

    //download chunk size, make sure to match on server thread
    private final int BUFFER_SIZE = 160000;


    //final ModelRequestTask modelDownloadTask;


    //public ModelRequestRunnable( float cr,String filename, float percReduc, Context context, ModelRequestManager mInstance) {

    public DelegateRequestRunnable(ModelRequest modelRequest, ModelRequestManager mInstance ) {
        this.modelRequest = modelRequest;


        this.context = modelRequest.getAppContext();

        this.mInstance = mInstance;


    }



    @SuppressLint("SuspiciousIndentation")
    @Override
    public void run() {


    try{

// for using the local memory undo the comment
            Log.d("DelegateRequest", "Entering Runnable");
            try {

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                try {
                    //@@@pc address
                    modelRequest.activityMain.curBysIters=0;// reset it for the next runs of Bayesian

                    Socket socket = new Socket("192.168.1.42", 4444);
                    // Open output stream
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    ///nill test to trigger HBO-> we write a code to python server and then activate it
                    out.println("activate");
                    //flush stream
                    out.flush();


                    ////
                    BufferedInputStream socketInputStream1;
                    socketInputStream1 = new BufferedInputStream(socket.getInputStream());
                    byte[] buffer1 = new byte[BUFFER_SIZE];
                    int read1;
                    int totalRead1 = 0;
                    long startTime2 = System.currentTimeMillis();

                    int exploration_phase = modelRequest.activityMain.exploration_phase;// initially 5 to explore 5 delegates on pyhon client
                    int max_iteration=modelRequest.activityMain.max_iteration;// we define it in python client 15 iterations and the last applying the best

                    read1 = socketInputStream1.read(buffer1);
//                    int iterations=0;
                    while (read1 == -1) // wait and listen
                        read1 = socketInputStream1.read(buffer1);


                    // this is just for the exploration phase , it recieves all the
//                    if (phase.equals("exploration")){
                   // out = new PrintWriter(socket.getOutputStream(), true);
                    while (read1 != -1) {//the msg is ready from the python client
                            String message = new String(buffer1, 0, read1);
                            // you need to gain the msg either from file stored in java_cleint.txt or the direct msg from java that comes from python client
                            long startTime = System.currentTimeMillis();
                            //  fout.write(buffer1, 0, read1);

                            //if (modelRequest.activityMain.curBysIters < max_iteration-1){// this is for all bayesian trials - before Dec 2023
                            if (modelRequest.activityMain.curBysIters < max_iteration){ // we already include a +1 in activity main to account for the last application of the best input (after a cycle of bayesian)
                                String[] elements = message.substring(19, message.length() - 3).split(",");
                                // Create a double array to store the converted elements
                                double[] doubleArray = new double[elements.length];
                                // Convert the elements to double and store them in the double array
                                for (int i = 0; i < elements.length; i++) {
                                    doubleArray[i] = Double.parseDouble(elements[i].trim());
                                }
                                modelRequest.all_delegates = doubleArray;
                            // this is to test if we can trigger the delegate applier- remove soon
                            Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
                            msg.obj = modelRequest;
                            modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);

                            long endTime = System.currentTimeMillis();
                            //  Log.d("DelegateReq", "Buffer write time: " + (endTime - startTime) + " milliseconds");

                            while (modelRequest.activityMain.avg_reward == 0) ;
                            //    Log.d("data received on java client, reward is: ", String.valueOf(modelRequest.activityMain.avg_reward));
                            out.println(modelRequest.activityMain.avg_reward);
                            //flush stream
                            out.flush();
                            exploration_phase -= 1;
                            modelRequest.activityMain.curBysIters += 1;
                            modelRequest.activityMain.avg_reward = 0; // restart the reWARD

                                 }
                            else if(baseline1_2){// // this is afeter finishing bayesian to run baseline1 and 2 => this is to achieve the index of best delegate from bayesian
                                Log.d("Bayesian Msg", ": " + message + " milliseconds");
                                String elements = message.substring(19, message.length() - 3);
                                Log.d("Bayesian ary", ": " + elements + " milliseconds");
                                double reward=Double.parseDouble(elements);;// this is the best bys reward
                                int bestInd=modelRequest.activityMain.bysRewardsLog.indexOf(reward);
                                modelRequest.activityMain.bayesian1_bestTR=modelRequest.activityMain.bysTratioLog.get(bestInd);
                                modelRequest.activityMain.bayesian1_bestLcty=modelRequest.activityMain.bysAvgLcyLog.get(bestInd);
                                modelRequest.req="baseline";
                                modelRequest.activityMain.avg_AIperK.clear();
                                Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
                               msg.obj = modelRequest;
                              modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);
                                modelRequest.activityMain.curBysIters += 1;
                            }
                            else
                                break;// this is for other baselines
                            //it's max_iteration+1 cause we have max_iteration+1 Msgs from python client => want to receive the last msg of the best reward to obtain the Tratio and Avglatency data for the baseline
                            if(modelRequest.activityMain.curBysIters ==max_iteration+1)// we did apply all the exploration and exploitation from python client
                                break;

                            if ( exploration_phase == 0)// break if that's not in the exploration phase or we have finished 5 times exploration
                              exploration_phase=1; // we make it 1 that shows we are in the exploitation phase

                            read1 = socketInputStream1.read(buffer1);
                            while (read1 == -1) // wait and listen
                                read1 = socketInputStream1.read(buffer1);

                        }


                    long endTime2 = System.currentTimeMillis();
                    Log.d("DelegateReq", "Total execution Time: " + (endTime2 - startTime2) + " milliseconds");

                    //fout.flush();

                    // tell server that file is received so it doesn't close connection
//                    PrintWriter outM = new PrintWriter(socket.getOutputStream(), true);
//                    outM.println("File received");


//added from model req manager
//         nill moved above           Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
//                    msg.obj = modelRequest;

//                    outM.flush();
                    // close all the streams
                   // fout.close();
                    socketInputStream1.close();
//                    outM.close();
                    in.close();
                    //fout.close();
                    out.close();
                    socket.close();

//                    modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);


                    ModelRequestManager.dlgRequestList.remove(modelRequest);


                }//}


                catch (Exception e) {
                    mInstance.handleState(DOWNLOAD_FAILED, modelRequest);
                    Log.w("ModelRequestRunnable", e.getMessage());
                }


            } catch (InterruptedException e) {

                Log.w("ModelRequestRunnable", e.getMessage());
            } finally {

            }


    }
    catch (Exception e) {
        mInstance.handleState(DOWNLOAD_FAILED, modelRequest);
        Log.w("ModelRequestRunnable", e.getMessage());
    }
    }//run


    public void decimateall(float ratio){

        modelRequest.activityMain.total_tris =0;
        //  Part 2:  start to apply the triangle count and OTDA

        for (int i =0;i<modelRequest.activityMain.objectCount;i++)
        {// to avoid null pointer error

            modelRequest.activityMain.ratioArray.set(i, ratio);
            int finalI = i;
            modelRequest.activityMain.runOnUiThread(() -> modelRequest.activityMain.renderArray.get(finalI).decimatedModelRequest(modelRequest.activityMain.ratioArray.get(finalI), finalI, false));
            modelRequest.activityMain.total_tris = modelRequest.activityMain.total_tris + (modelRequest.activityMain.ratioArray.get(i) *  modelRequest.activityMain.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
            try {
                Thread.sleep(7);// added to prevent the crash happens while redrawing all the objects at the same time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void send_reward_toserver(double reward){

        try {

            //@@@pc address
            Socket socket = new Socket("192.168.1.42", 4444);
            // Open output stream
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //send reward result
            //int reward=2000;
            String msg_toserver="sending_rewards:"+reward;

            out.println(msg_toserver);
            //flush stream
            out.flush();

            while (!((new String(in.readLine())).equals("File received"))) ;// when file is recieved, we close the socket

            out.close();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
