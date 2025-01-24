package com.arcore.AI_ResourceControl;
import android.content.Context;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FastDelegateRequestRunnable implements Runnable{
    private final ModelRequest modelRequest;

    private final Context context;
    private final ModelRequestManager mInstance;

    private double remining_task;

    public String send_message;

    private double[] delegate_array = new double[4];

    private final int BUFFER_SIZE = 160000;


    public FastDelegateRequestRunnable(ModelRequest modelRequest, ModelRequestManager mInstance, double remain_task) {
        this.modelRequest = modelRequest;

        this.context = modelRequest.getAppContext();

        this.mInstance = mInstance;

        this.remining_task = remain_task;

    }

    @Override
    public void run() {
        Log.d("fast_sc","Fast Delegate is starting");
        try {
            if (Thread.interrupted()) {
                throw new InterruptedException("Thread was interrupted");
            }
            try {
                Socket socket = new Socket(modelRequest.activityMain.server_IP_address, modelRequest.activityMain.server_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                String message = in.readLine();

                while (true){
                    Log.d("fast_sc","Message:     " + message);
                    boolean finish_flag = false;
                    if(message!=null)
                        finish_flag = transferMessage(message);
                    if(finish_flag){
                        break;
                    }
                    if(send_message != null && !send_message.isEmpty()){
                        Log.d("fast_sc", "Send to server:    " + send_message);
                        out.println(send_message);
                        out.flush();
                        send_message = "";
                    }
                    message = in.readLine();
                }

            }catch(Exception e){

            }

        } catch (InterruptedException e) {
            Log.d("fast_sc", "Thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }


    }

    /**
     * Function: Get Status for Server
     */
    public String getQualityAndLatency(){
        double quality = modelRequest.activityMain.avgq;
        double latency = modelRequest.activityMain.avgl;
        String msg = "state:" + quality + "," +latency;
        modelRequest.activityMain.avg_reward = 0;
        return msg;
    }

    /**
     * Transfer received message from server
     */

    public boolean transferMessage(String message){
        String reset = "reset";
        String action = "action:";
        String finish_flag = "finish";


        if(message.startsWith(reset)) {
            send_message = getQualityAndLatency() + "," + modelRequest.getRemaining_task();
        }

        else if(message.startsWith(action)){
            // Take Action
            //Step 1: Translate the action
            // Message format: [50% TaskCPU, GPU, NPU, Triangle count]
            String[] parts1 = message.split(":");
            String[] parts = parts1[1].split(",");

            if(parts.length == 4){
                for(int i = 0; i < 4; i++) {
                    delegate_array[i]  = Double.parseDouble(parts[i].trim());
                    Log.d("fast_sc", "R" + i + ":     " +delegate_array[i]);
                }
                delegate_array[3] = Double.parseDouble(parts[3].trim());
                Log.d("fast_sc","CPU:  " + delegate_array[0]
                                        + "GPU:    "+delegate_array[1]
                                        + "NPU:    " + delegate_array[2]
                                        + "Triangle Count" + delegate_array[3]);
                modelRequest.all_delegates = delegate_array;
                Log.d("fast_sc","Starting to send message to main");
                Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
                msg.obj = modelRequest;
                modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);
                Log.d("fast_sc","Send success and waiting for new reward");
                while (modelRequest.activityMain.avg_reward == 0){
//                    Log.d("fast_sc","avg_reward: " + modelRequest.activityMain.avg_reward);
                }
                Log.d("fast_sc","Reward:    " + modelRequest.activityMain.avg_reward);
                modelRequest.activityMain.avg_reward = 0;
                send_message = getQualityAndLatency();

            }
        }
        else if(message.startsWith(finish_flag))
            return true;

        return false;
    }


}
