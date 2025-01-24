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
    int objC;
    double sensitivity[] ;
    float objquality[];
    double tris_share[];

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
        objC=modelRequest.activityMain.objectCount+1;
        sensitivity = new double[objC];
        tris_share = new double[objC];
        objquality= new float[objC];

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
        double quality = calculateMeanQuality();
        double latency = calculate_Latency();
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
//                while (modelRequest.activityMain.avg_reward == 0){
////                    Log.d("fast_sc","avg_reward: " + modelRequest.activityMain.avg_reward);
//                }
//                Log.d("fast_sc","Reward:    " + modelRequest.activityMain.avg_reward);
//                modelRequest.activityMain.avg_reward = 0;
                send_message = getQualityAndLatency();

            }
        }
        else if(message.startsWith(finish_flag))
            return true;

        return false;
    }


    public float calculateMeanQuality( ) {

        float sumQual=0;
        for (int ind = 0; ind < modelRequest.activityMain.objectCount; ind++)
        {
            int i =  modelRequest.activityMain.excelname.indexOf( modelRequest.activityMain.renderArray.get(ind).fileName);
            float gamma = modelRequest.activityMain.excel_gamma.get(i);
            float a = modelRequest.activityMain.excel_alpha.get(i);
            float b = modelRequest.activityMain.excel_betta.get(i);
            float c = modelRequest.activityMain.excel_c.get(i);
            float d = modelRequest.activityMain.renderArray.get(ind).return_distance();
            float curQ = modelRequest.activityMain.ratioArray.get(ind);


            float deg_error = (float) Math.round((float) (Calculate_deg_er(a, b, c, d, gamma, curQ) * 1000)) / 1000;
            float max_nrmd = modelRequest.activityMain.excel_maxd.get(i);

            float cur_degerror = deg_error / max_nrmd;
            float quality= 1- cur_degerror;
            objquality[ind]=quality;
            sumQual+=quality;


        }
        return sumQual/modelRequest.activityMain.objectCount;
    }
    public float Calculate_deg_er(float a,float b,float creal,float d,float gamma, float r1) {

        float error;
        if(r1==1)
            return  0f;
        error = (float) (((a * Math.pow(r1,2)) + (b * r1) + creal) / (Math.pow(d , gamma)));
        return error;
    }

    public double calculate_Latency(){

        int Ai_count = modelRequest.activityMain.mList.size();
        double avg_AIlatencyPeriod=0;
        double[] AI_latency = new double[Ai_count];
        double meanRt = 0;
        double meanThr = 0;

        for (int aiIndx = 0; aiIndx < Ai_count; aiIndx++) {

            double[] t_h = modelRequest.activityMain.getResponseT(aiIndx);
            meanRt = t_h[0];
            meanThr = t_h[1];

            while (meanThr > 500 ||meanThr < 0.5) // we wanna get a correct value
            { t_h=modelRequest.activityMain.getResponseT(aiIndx);
                meanThr = t_h[1];
                meanRt = t_h[0];
            }

            AI_latency[aiIndx] = meanRt;

            AiItemsViewModel taskView=modelRequest.activityMain.mList.get(aiIndx);
            // first find the best offline AI response Time = EXPECTED RESPONSE TIme
            int indq = modelRequest.activityMain.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));
            double expected_time = modelRequest.activityMain.excel_BestofflineAIRT.get(indq);
            // find the actual response Time
            double actual_rpT=meanRt;

            avg_AIlatencyPeriod+=(actual_rpT-expected_time)/actual_rpT;


        }
       return avg_AIlatencyPeriod/ modelRequest.activityMain.mList.size();

    }


}
