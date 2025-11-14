package com.arcore.AI_ResourceControl;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * if you wanna test hbo vs baselines, make baseline1_2 flag true
 * This class is to communicate with java server, it requests the suggested delegate and triangle count, then sends a msg to the
 * activity main and applies the delegates by calling the background thread named Bayesian and function  bys.apply_delegate_tris(selected_combinations);
 *      Communication with server running on a thread
 */



public class DelegateRequestRunnable implements Runnable {

    private final ModelRequest modelRequest;



//    private volatile CountDownTimer sceneTimer;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean timerFinished = new AtomicBoolean(false);

    boolean sendFlag = false;

    // Context needed to save the image in the internal storage
    private final Context context;

    Socket socket = null;
    PrintWriter out = null;
    BufferedReader in = null;




    private final ModelRequestManager mInstance;

    private double remining_task;

    static final int DOWNLOAD_FAILED = -1;
    static final int DOWNLOAD_PENDING = 1;
    static final int DOWNLOAD_STARTED = 2;
    static final int DOWNLOAD_COMPLETE = 3;

    private long lastApplyTime = -1;
    private long currentApplyTime = -1;

    boolean baseline1_2=true;// when we wanna run bayesian + two baselines we set this true here and in the activity main

    //download chunk size, make sure to match on server thread
    private final int BUFFER_SIZE = 160000;


    //final ModelRequestTask modelDownloadTask;


    //public ModelRequestRunnable( float cr,String filename, float percReduc, Context context, ModelRequestManager mInstance) {

    public DelegateRequestRunnable(ModelRequest modelRequest, ModelRequestManager mInstance, double remain_task) {
        this.modelRequest = modelRequest;


        this.context = modelRequest.getAppContext();

        this.mInstance = mInstance;

        this.remining_task = remain_task;


    }

    public void setRemainTask(double num){
        this.remining_task = num;
    }




    @SuppressLint("SuspiciousIndentation")
    @Override
    public void run() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

// for using the local memory undo the comment
        Log.d("DelegateRequest", "Entering Runnable");
        try {

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            try {
                //@@@pc address
                modelRequest.activityMain.curBysIters=0;// reset it for the next runs of Bayesian


                socket = new Socket(modelRequest.activityMain.server_IP_address, modelRequest.activityMain.server_PORT);
                    // Open output stream
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));



                ///nill test to trigger HBO-> we write a code to python server and then activate it

                String activate_msg = "delegate/activate:" + remining_task;
                Log.d("HBO_MSG", "Remaining Task: " + remining_task);
                out.println(activate_msg);
                Log.d("OFFLOAD_MSG","Send Active MSG  " + activate_msg);

                //flush stream
                out.flush();

                // Send remaining Task Num to Optimize




                ////
                BufferedInputStream socketInputStream1;
                socketInputStream1 = new BufferedInputStream(socket.getInputStream());
                byte[] buffer1 = new byte[BUFFER_SIZE];
                int read1;
                int totalRead1 = 0;
                long startTime2 = System.currentTimeMillis();

                int exploration_phase = modelRequest.activityMain.exploration_phase;// initially 5 to explore 5 delegates on pyhon client
                int max_iteration=modelRequest.activityMain.max_iteration;// we define it in python client 15 iterations and the last applying the best


                //This is for test
                // Read from input stream and send back everything received
//                while ((read1 = socketInputStream1.read(buffer1)) != -1) {
//                    String receivedData = new String(buffer1, 0, read1);
//                    Log.d("DelegateRequest Msg", "Received: " + receivedData);
//
//                    // Send back the received data
//                    out.println("Android ECO: ["+receivedData+"]");
//                    out.flush();
//                }




                read1 = socketInputStream1.read(buffer1);
                Log.d("DelegateRequest Msg","reading 1");
                while (read1 == -1) // wait and listen
                    read1 = socketInputStream1.read(buffer1);
                Log.d("DelegateRequest Msg","done reading");

                // this is just for the exploration phase , it recieves all the
//                    if (phase.equals("exploration")){
                // out = new PrintWriter(socket.getOutputStream(), true);
                while (read1 != -1) {//the msg is ready from the python client
//                    timerFinished.set(false);
//                    main.post(() -> {
//                        sceneTimer = new CountDownTimer(4000000, 1000) {
//                            @Override
//                            public void onTick(long l) {
//                                sampleLatency();
//                            }
//
//                            @Override
//                            public void onFinish() {
//                                timerFinished.set(true);
//                                writeFinish();
//
//                            }
//                        }.start();});
                    String message = new String(buffer1, 0, read1);
                    Log.d("DelegateRequest Msg","msg: "+message);
                    // you need to gain the msg either from file stored in java_cleint.txt or the direct msg from java that comes from python client
                    long startTime = System.currentTimeMillis();
                    long hbo_beginTime = System.currentTimeMillis();
                    //  fout.write(buffer1, 0, read1);

                    //if (modelRequest.activityMain.curBysIters < max_iteration-1){// this is for all bayesian trials - before Dec 2023
                    if (modelRequest.activityMain.curBysIters < max_iteration){ // we already include a +1 in activity main to account for the last application of the best input (after a cycle of bayesian)
                        String[] elements = message.substring(19, message.length() - 3).split(",");

                        if(modelRequest.activityMain.curBysIters == max_iteration-1 && baseline1_2)// get the best reward to find avg Latency for baseline SML
                        {// Here, we not only apply the best input, but also we take the best reward for baseline of SML = User study

                            double reward= Double.parseDouble(elements[elements.length-1].trim());

                            double[] doubleArray = new double[elements.length-1];// this is because the last data of elements is the best reward
                            // Convert the elements to double and store them in the double array
                            for (int i = 0; i < elements.length-1; i++) {
                                doubleArray[i] = Double.parseDouble(elements[i].trim());
                            }
                            modelRequest.all_delegates = doubleArray;
                            modelRequest.activityMain.all_delegates_LstHBO=doubleArray;// save it for mainactivity as well

                            int bestInd=modelRequest.activityMain.bysRewardsLog.indexOf(reward);
                            Log.d("OFFLOAD_MSG","BEST REWARD:     " + reward);
//                            modelRequest.activityMain.best_BT = reward;
                            modelRequest.activityMain.bayesian1_bestTR=modelRequest.activityMain.bysTratioLog.get(bestInd);
                            modelRequest.activityMain.bayesian1_bestLcty=modelRequest.activityMain.bysAvgLcyLog.get(bestInd);
                        }

                        else {// find the delegate to apply
                            // Create a double array to store the converted elements
                            double[] doubleArray = new double[elements.length];
                            // Convert the elements to double and store them in the double array
                            for (int i = 0; i < elements.length; i++) {
                                doubleArray[i] = Double.parseDouble(elements[i].trim());
                            }
                            modelRequest.all_delegates = doubleArray;
                        }



                        // this is to test if we can trigger the delegate applier- remove soon
                        Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
                        msg.obj = modelRequest;
                        modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);
//                        currentApplyTime = System.currentTimeMillis();
//                        long period = 0;
//                        if(lastApplyTime != -1){
//                            period = currentApplyTime - lastApplyTime;
//                        }
//                        lastApplyTime = currentApplyTime;
//                        String currentFolder = modelRequest.activityMain.getExternalFilesDir(null).getAbsolutePath();
//                        String FILEPATH = currentFolder + File.separator + "overheadData"+modelRequest.activityMain.fileseries+".csv";
//                        StringBuilder sb = new StringBuilder();
//                        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))){
//                            sb.append(currentApplyTime);
//                            sb.append(",");
//                            sb.append(lastApplyTime);
//                            sb.append(",");
//                            sb.append(period);
//                            writer.write(sb.toString());
//                        }catch (Exception e){
//
//                        }

                        long endTime = System.currentTimeMillis();
                        //  Log.d("DelegateReq", "Buffer write time: " + (endTime - startTime) + " milliseconds");



                        while (modelRequest.activityMain.avg_reward == 0); // Apply & measure

                        //    Log.d("data received on java client, reward is: ", String.valueOf(modelRequest.activityMain.avg_reward));
                        Log.d("DelegateRequest Msg",  "from Delegate: ave_reward = " + String.valueOf(modelRequest.activityMain.avg_reward));
                        String msg_toserver="reward/"+String.valueOf(modelRequest.activityMain.avg_reward);
                        out.println(msg_toserver);
                        sendFlag = true;
                        Log.d("DelegateRequest Msg", "Sent successfully   " + msg_toserver);

                        //flush stream
                        out.flush();
                        long hbo_endTime = System.currentTimeMillis();
                        // Log Time Stamp
                        String currentFolder = modelRequest.activityMain.getExternalFilesDir(null).getAbsolutePath();
                        String myFilePath = currentFolder + File.separator + "timeStamps" + modelRequest.activityMain.fileseries + ".csv";
                        try (PrintWriter writer = new PrintWriter(new FileOutputStream(myFilePath, true))) {

                            StringBuilder sbb = new StringBuilder();

                            sbb.append(hbo_beginTime).append(',').append(hbo_endTime).append(',').append(modelRequest.activityMain.begin_action_timeStamp).append(",")
                                    .append(modelRequest.activityMain.begin_measure_timeStamp);
                            Log.d("PeriodTimeStamp", "ht:" + hbo_beginTime
                                                            + ",et:" + hbo_endTime
                                                            + ",at" + modelRequest.activityMain.begin_action_timeStamp
                                                            + ",mt:" + modelRequest.activityMain.begin_measure_timeStamp);
                            sbb.append('\n');
                            writer.write(sbb.toString());

                        } catch (FileNotFoundException e) {
                        }







                        // if the counter does not finish, we need to stop it manually.
//                        if(!timerFinished.get()){
//                            main.post(() ->{
//                                if(sceneTimer != null){
//                                    sceneTimer.cancel();
//                                    sceneTimer = null;
////                                    writeFinishPause();
//                                }
//                            });
//                        }


                        exploration_phase -= 1;
                        Log.d("DelegateRequest Msg", "Current Iter:" +modelRequest.activityMain.curBysIters );
                        if(modelRequest.activityMain.curBysIters==max_iteration-1)// should be done all the time after last HBO iteration
                        //&& modelRequest.activityMain.objectCount==1)// this is done just for one time
                        {
                            Log.d("DelegateRequest Msg", "Sending Best BT  " + modelRequest.activityMain.avg_reward);
                            modelRequest.activityMain.avg_reward=(double) (Math.round((double) (modelRequest.activityMain.avg_reward * 100))) / 100;
                            modelRequest.activityMain.best_BT = modelRequest.activityMain.avg_reward;

                            // Used to trigger Offload
                            modelRequest.activityMain.hbo_is_done = true;
                            modelRequest.activityMain.hbo_finish_time++;


                        }
                        modelRequest.activityMain.curBysIters += 1;
                        modelRequest.activityMain.avg_reward = 0; // restart the reWARD

                    }
                    else if(baseline1_2){// // this is afeter finishing bayesian to run baseline1 and 2 => this is to achieve the index of best delegate from bayesian
                        Log.d("DelegateRequest Msg", ": " + message + " milliseconds");
                        String elements = message.substring(19, message.length() - 3);
                        Log.d("DelegateRequest ary", ": " + elements + " milliseconds");
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
                    if(modelRequest.activityMain.curBysIters ==max_iteration)//                                    +1)// we did apply all the exploration and exploitation from python client
                        break;


                    if ( exploration_phase == 0)// break if that's not in the exploration phase or we have finished 5 times exploration
                        exploration_phase=1; // we make it 1 that shows we are in the exploitation phase

                    read1 = socketInputStream1.read(buffer1);
                    Log.d("DelegateRequest Msg","reading 2");
                    while (read1 == -1) // wait and listen
                        read1 = socketInputStream1.read(buffer1);
                    Log.d("DelegateRequest Msg","done reading");
                }

                modelRequest.activityMain.curBysIters=-1;// reset it for the next runs of Bayesian
                modelRequest.activityMain.afterHbo_counter =0;// we restart this

                long endTime2 = System.currentTimeMillis();
                Log.d("DelegateRequest", "Total execution Time: " + (endTime2 - startTime2) + " milliseconds");



                SystemClock.sleep(1000*60*10); //wait before closing the connection (to collect thermal data)
                socketInputStream1.close();
//                    outM.close();
//                    in.close();
                //fout.close();

                //Comment for second iteration
//
                out.close();
                socket.close();

//                    modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);
                ModelRequestManager.dlgRequestList.remove(modelRequest);

            }//}

            catch (Exception e) {
                mInstance.handleState(DOWNLOAD_FAILED, modelRequest);
                Log.d("DelegateRequest inner try", e.getMessage());
            }

        } catch (InterruptedException e) {

            Log.d("DelegateRequest outer try", e.getMessage());
        } finally {

        }


    }//run

//    public void decimateall(float ratio){
//
//        modelRequest.activityMain.total_tris =0;
//        //  Part 2:  start to apply the triangle count and OTDA
//
//        for (int i =0;i<modelRequest.activityMain.objectCount;i++)
//        {// to avoid null pointer error
//
//            modelRequest.activityMain.ratioArray.set(i, ratio);
//            int finalI = i;
//            modelRequest.activityMain.runOnUiThread(() -> modelRequest.activityMain.renderArray.get(finalI).decimatedModelRequest(modelRequest.activityMain.ratioArray.get(finalI), finalI, false));
//            modelRequest.activityMain.total_tris = modelRequest.activityMain.total_tris + (modelRequest.activityMain.ratioArray.get(i) *  modelRequest.activityMain.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
//            try {
//                Thread.sleep(7);// added to prevent the crash happens while redrawing all the objects at the same time
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }

//    public void send_reward_toserver(double reward){
//
//        try {
//
//            //@@@pc address
//            Socket socket = new Socket(modelRequest.activityMain.server_IP_address, 4444);
//            // Open output stream
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            //send reward result
//            //int reward=2000;
//            String msg_toserver="sending_rewards:"+reward;
//
//            out.println(msg_toserver);
//            //flush stream
//            out.flush();
//
//            while (!((new String(in.readLine())).equals("File received"))) ;// when file is recieved, we close the socket
//
//            out.close();
//        }
//        catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//    }


    void sampleLatency(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = modelRequest.activityMain.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "overheadData"+modelRequest.activityMain.fileseries+".csv";
        StringBuilder sb = new StringBuilder();

        sb.append(dateFormat.format(new Date()));

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))){
            double meanLatency = 0;
            sb.append('\n');
            for (int i=0;i<modelRequest.activityMain.mList.size();i++)
            {


                AiItemsViewModel taskView=modelRequest.activityMain.mList.get(i);
                int deviceIdx = taskView.getCurrentDevice();


                // first find the best offline AI response Time = EXPECTED RESPONSE TIme
                int indq = modelRequest.activityMain.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));// search in excel file to find the name of current object and get access to the index of current object
                // excel file has all information for the AI inference NAME, Delegate, and time
                double expected_time = modelRequest.activityMain.excel_BestofflineAIRT.get(indq);
                // find the actual response Time


                double meanRt = modelRequest.activityMain.mList.get(i).getTot_rps();

                if (deviceIdx == 3) {
                    //Log.d("OFFLOAD_ERR", "Thr:  " + meanThr + "RT:   " + meanRt);
                    //Since I only have one GPU, the GPU also has a queue to execute the inference
                    // When we have one more inferences at same time, we need to get the true latency for the
                    // waiting Task
                    if (meanRt == 0) {
                        meanRt = modelRequest.activityMain.offload_task_latency[i];
                    } else {
                        //Update new latency
                        modelRequest.activityMain.offload_task_latency[i] = meanRt;
                    }

                }
                //double[] t_h = mInstance.getResponseT(i);

                while (meanRt==0) { // we wanna get a correct value and avoid noisy
//                    double[] th = mInstance.getResponseT(i);
//                    meanRt = th[0];
//                    meanThr = th[1];
                    meanRt = modelRequest.activityMain.mList.get(i).getTot_rps();
                    Log.d("OFFLOAD_ERR", "Current :" + meanRt);
                }



                double actual_rpT=meanRt;
                meanLatency += actual_rpT;



                // meanRt = mInstance.getResponseT(aiIndx);// after the objects are decimated
                //meanRt = t_h[0];
                // calculate the latency
//                if(deviceIdx !=3){
                //normalized over curr time this is because we want to have this value minimized
//                avg_AIlatencyPeriod+=(actual_rpT-expected_time)/actual_rpT;
//                task_num++;
//                }



//                if(deviceIdx != 3){
//                    avg_AIlatencyPeriod_local += (actual_rpT-expected_time)/actual_rpT ;
//                }

//                double tmp_lastLatency=mInstance.last_latencyInstanceN;
//                double cur_latency=actual_rpT-expected_time;


//                char tempFlag = 'n';
//                if(modelRequest.activityMain.measureFlag){
//                    tempFlag = 'm';
//                } else if (sendFlag) {
//                    sendFlag = false;
//                    tempFlag = 'r';
//                }


                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append(actual_rpT);
                //.append( mInstance.avg_reponseT.get(i));
            }
            if (modelRequest.activityMain.mList.size() == 0)
                meanLatency = 0;
            else {
                meanLatency = meanLatency / modelRequest.activityMain.mList.size();
            }
            sb.append(',').append(meanLatency).append('\n');
            writer.write(sb.toString());
        }catch (Exception e){

        }
    }
    void writeFinish(){
        String currentFolder = modelRequest.activityMain.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "overheadData"+modelRequest.activityMain.fileseries+".csv";
        StringBuilder sb = new StringBuilder();
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//            long currentTime = System.currentTimeMillis();
            sb.append(',').append("d").append('\n');
            writer.write(sb.toString());
        }catch (Exception e){

        }
    }
    void writeFinishPause(){
        String currentFolder = modelRequest.activityMain.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "overheadData"+modelRequest.activityMain.fileseries+".csv";
        StringBuilder sb = new StringBuilder();
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//            long currentTime = System.currentTimeMillis();
            sb.append(',').append("dd").append('\n');
            writer.write(sb.toString());
        }catch (Exception e){

        }
    }

}
