///
/**
 *
 *
 * For HBO, this code just shows AI task's response time and calculates the average throughput
 */

// @@@ check for  "Uncomment for Bayes auto Trigger" and uncomment those codes for auto trigger in
package com.arcore.AI_ResourceControl;
/*for HBO it collects data and checks if HBO trigger is needed*/
/**
 * From around 350 line is offloading part
 * At Around 246 line, I add my own offload optimize function
 * Author @Ze Li for Offload part
 *Edited: 09/12/2024
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.floorMod;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.nextDown;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.opengl.Matrix;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.SizeF;
import android.widget.TextView;

import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Vector3;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

import org.checkerframework.checker.index.qual.LengthOf;
import org.w3c.dom.Text;


public class balancer implements Runnable {





    private float[] linear_velocity = {0, 0, 0};  // x, y, z  linear v


    private float[] rotate_velocity = {0, 0, 0}; // x,y,z rotate v

    private float[] camera_position = {0, 0, 0};
    private double[] rotation = {0,0,0,0};
    private float dt = 1.0F;

    boolean hbo_trigger=false;// this enables autonomous HBO activation , make sure it's true, I temporary made it false
    //boolean offload_trigger = true; // After first HBO, we need to trigger offloading and once one offloading done
    // we need set this flag back to false until next HBO finished
    private double sendResponseTimeCode = -1000000; //set this flag to judge the data we send to data collector server

    int binCap=7;
    private final MainActivity mInstance;
    float ref_ratio=0.5f;
    int objC;
    double sensitivity[] ;
    float objquality[];
    double tris_share[];
    double current_tris;
    Map <Integer, Double> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f , 0.4f, 0.2f, 0.05f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
    double [][]fProfit;
    double [][] tRemainder;
    int [][] track_obj;
    int sleepTime=30;
    //float candidate_obj[] = new float[total_obj];
    double tMin[] ;
    int missCounter=3;//means at least 4 noises
    // int aiIndx;
    TextView posText_re,posText_thr,posText_q,posText_app_hbo, posVisibleTris;
    double reward=0;

    // Used for collect B_t & triangle count
    int port = 3434;

    //Used for RL test
    boolean RL_trigger = true;

    public balancer(MainActivity mInstance) {

        this.mInstance = mInstance;

        objC=mInstance.objectCount+1;
        sensitivity = new double[objC];
        tris_share = new double[objC];
        objquality= new float[objC];// 1- degradation-error
        // aiIndx=ai_index;
        //model_index;

        //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
        fProfit= new double[objC][coarse_Ratios.length];
        tRemainder= new double[objC][coarse_Ratios.length];
        track_obj= new int[objC][coarse_Ratios.length];
        //float candidate_obj[] = new float[total_obj];
        tMin = new double[objC];


        posText_re= mInstance.findViewById(R.id.app_re);
        posText_q= mInstance.findViewById(R.id.app_quality);
        posText_thr= mInstance.findViewById(R.id.app_thr);
        // posText_mir= mInstance.findViewById(R.id.app_bt);
        posText_app_hbo  = mInstance. findViewById(R.id.app_bt);
        posVisibleTris = mInstance.findViewById(R.id.max_triangle);


    }

    @SuppressLint("SuspiciousIndentation")
    @Override


    public void run() {

        boolean accmodel = true;// all AI throughput trained models and RE are accurate
        boolean accRe = true;// this is to check if the trained model for re is accurate
        boolean trainedTris = false;
        boolean trainedRT = false;
        boolean trainedRE = false;
        double maxTrisThreshold = mInstance.orgTrisAllobj;
        double minTrisThreshold = maxTrisThreshold * mInstance.coarse_Ratios[mInstance.coarse_Ratios.length - 1];
        double meanRt = 0;
        double meanThr = 0;
        //totTris;
        double meanDk = 0; // mean current dis
        double meanDkk = 0; // mean of d in the next period-> you need to cal the average of predicted d for all the objects
        double pred_meanD = mInstance.pred_meanD_cur; // for next 1 sc
        boolean fault_thr = false;
        int tris_window = 4;

        int re_window = 12;


        double period_init_tris = mInstance.total_tris;// this is the starting triangle count



        for (int i = 0; i < mInstance.objectCount; i++) {
            double cur_dis = (double) mInstance.renderArray.get(i).return_distance();
            meanDk += cur_dis;
            meanDkk += mInstance.predicted_distances.get(i).get(1); // 0: next 1 sec, 1: next 2s :  // gets the first time, next 2s-- 3s of every object, ie. d1 of every obj

        }

        int objc = mInstance.objectCount;
        ///totTris = mInstance.total_tris;// this might change in the middle of call, so it's better to directly use total_tris , not this local variable


        if (objc > 0) {
            meanDk /= objc;
            meanDkk /= objc;

            //  meanDk = (double) Math.round((double) 1000 * meanDk) / 1000;
            // meanDkk = (double) Math.round((double) 1000 * meanDkk) / 1000;

            if (objc == 1) {
                mInstance.initial_meanD = meanDk;
                mInstance.initial_totT = mInstance.total_tris;
            }
            if (meanDkk == 0)
                meanDkk = meanDk;
            if (pred_meanD == 0)
                pred_meanD = meanDk; // for the first objects

        }


        //I got an error for regression since decimation occurs in UI thread and Modeling runs at the same time
        // solution is to start data collection after one period passes from algorithm
        // else{ // just collect data when algorithm was applied in the last period



// Train the H model for each AI task from line 127 to


        //   int variousTris = mInstance.trisMeanDisk.keySet().size();


        double avg_msred_H = 0; // is equal to sum(msr_Hi)/N -> reset every period
        double avg_est_H = 0; // is equal to sum(H^i)/N -> reset every period

        // mInstance.trisMeanDisk.put(totTris, pred_meanD); //one-time correct: should have predicted value dist => removes from the head (older data) -> to then add to the tail
// this gets above bincap sooner than the rest of lists, so, we need to keep the size not more than 5

        int Ai_count = mInstance.mList.size();
        double avg_AIlatencyPeriod=0;
        double[] AI_latency = new double[Ai_count];

        for (int aiIndx = 0; aiIndx < Ai_count; aiIndx++) {

            double[] t_h = mInstance.getResponseT(aiIndx);
            meanRt = t_h[0];
            meanThr = t_h[1];

            while (meanThr > 500 ||meanThr < 0.5) // we wanna get a correct value
            { t_h=mInstance.getResponseT(aiIndx);
                meanThr = t_h[1];
                meanRt = t_h[0];
            }

            AI_latency[aiIndx] = meanRt;

            AiItemsViewModel taskView=mInstance.mList.get(aiIndx);
            // first find the best offline AI response Time = EXPECTED RESPONSE TIme
            int indq = mInstance.excel_BestofflineAIname.indexOf(taskView.getModels().get(taskView.getCurrentModel()));
            double expected_time = mInstance.excel_BestofflineAIRT.get(indq);
            // find the actual response Time
            double actual_rpT=meanRt;
            int device_index = taskView.getCurrentDevice();


            avg_AIlatencyPeriod+=(actual_rpT-expected_time)/actual_rpT;




            if (aiIndx == 0) {
                // TextView posText = (TextView) mInstance.findViewById(R.id.rspT);
                posText_re.setText("RT1: " + String.valueOf(meanRt));
            } else if (aiIndx == 1) {

                //  TextView posText2 = mInstance.findViewById(R.id.rspT1);
                posText_q.setText("RT2: " + String.valueOf(meanRt));
            }
            else if (aiIndx == 2) {
                //    TextView posText3 = mInstance.findViewById(R.id.rspT2);
                posText_thr.setText("RT3: " + String.valueOf(meanRt));
            }





        }


        //  Uncomment for Bayes auto Trigger
        if(mInstance.curBysIters==-1)
            mInstance.afterHbo_counter++;// count consecutive HBO activation

        mInstance.avgq = calculateMeanQuality();
        double avgAIltcy= avg_AIlatencyPeriod/ mInstance.mList.size();
        mInstance.avgl = avgAIltcy;
        reward =mInstance.avgq - (mInstance.reward_weight*avgAIltcy);
        reward=(double) (Math.round((double) (reward * 100))) / 100;


        mInstance.best_BT=(double) (Math.round((double) (mInstance.best_BT * 100))) / 100;



        posText_app_hbo.setText("B_t: "+ String.valueOf(reward));
        posText_thr.setText("best_BT: " + String.valueOf(mInstance.best_BT));

        // Test output of B_t and Triangle count:
        current_tris = mInstance.total_tris;
        //System.out.println("B_t:"+reward+"  " + "Triangle Count:" + current_tris);



/**
 * Following code are used for HBO project
 */
        if(hbo_trigger) {

            if (mInstance.best_BT != 0 && mInstance.curBysIters == -1) {// if it's not in the middle of another Bayesian


                if (mInstance.afterHbo_counter <= 3)// this is to adjust the reward and remove any noises for the first three data collected after HBO activation
                    mInstance.best_BT = (reward + mInstance.best_BT) / 2;

                double perc_error = (mInstance.best_BT - reward) / mInstance.best_BT;
                if (perc_error > 0.05 || perc_error < -0.1)// below is the function of server button
                // if BT gets worst by object addition, error becomes higher negative, if we farther awa, error becomes positive
                {
                    mInstance.hbo_trigger_false_counter++;
                    Log.d("DelegateRequest Msg", "Current perc_error:   " + perc_error);
                    if (mInstance.hbo_trigger_false_counter >= 3)// we won't iimmidiately trigger HBO, we'll wait till
                    {

                        mInstance.hbo_trigger_false_counter = 0;

                        mInstance.runOnUiThread(() -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mInstance);
                            builder.setTitle("HBO message")
                                    .setMessage("HBO is triggered since distance has changed")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton("Cancel", (dialog, which) -> {
                                        dialog.dismiss();
                                    })
                                    .show();
                            Log.d("HandlerExample", "This is running on the main thread.");
                        });

                        ModelRequestManager.getInstance().add(new ModelRequest(mInstance.getApplicationContext(), mInstance, mInstance.deleg_req, "delegate"), false, false);
                        mInstance.deleg_req += 1;
                    }
                } else
                    mInstance.hbo_trigger_false_counter = 0;// we want to get 5 Bts consecutively with error
            }
        }
        mInstance.runOnUiThread(() -> {
            posVisibleTris.setText("Visible Tris: " + posVisibleTriangle());
        });


        writequality();
       predictPosition();

    }

    public void predictPosition(){
        Frame frame = mInstance.getFragment().getArSceneView().getArFrame();
        if(frame == null) return;
        Pose pose = frame.getCamera().getPose();
        double[] currentQuat = {pose.qw(), pose.qx(),pose.qy(), pose.qz()}; // Current quaternion
        float[] currentPosition = {pose.tx(), pose.ty(), pose.tz()};

        // Predict Velocity and angular velocity first
        long currentTime = SystemClock.elapsedRealtimeNanos();
        if (mInstance.lastPredictionTime == 0) {
            mInstance.lastPredictionTime = currentTime;
            mInstance.lastPosition = currentPosition.clone();
            mInstance.lastQuat = currentQuat.clone();
            return;
        }
        double dt = (currentTime - mInstance.lastPredictionTime) / 1e9;
        mInstance.lastPredictionTime = currentTime;


        double[] linearVelocity = new double[3];
        if (mInstance.lastPosition != null) {
            for (int i = 0; i < 3; i++) {
                linearVelocity[i] = (currentPosition[i] - mInstance.lastPosition[i]) / dt;
            }
        }
        mInstance.lastPosition = currentPosition.clone();

        double[] rotateVelocity = new double[3];
        if(mInstance.lastQuat != null){
            // 计算四元数变化量：Δq = lastQuat⁻¹ * currentQuat
            double[] deltaQ = multiplyQuaternion(inverseQuaternion(mInstance.lastQuat), currentQuat);

            // 将四元数转换为轴角表示
            double theta = 2 * Math.acos(deltaQ[0]);
            double[] axis = new double[3];
            double sinHalfTheta = Math.sin(theta / 2);
            if (sinHalfTheta > 0.001) { // 避免除以零
                axis[0] = deltaQ[1] / sinHalfTheta;
                axis[1] = deltaQ[2] / sinHalfTheta;
                axis[2] = deltaQ[3] / sinHalfTheta;
            } else {
                // 小角度情况，使用泰勒展开近似
                axis[0] = deltaQ[1];
                axis[1] = deltaQ[2];
                axis[2] = deltaQ[3];
                theta = 2 * Math.sqrt(deltaQ[1]*deltaQ[1] + deltaQ[2]*deltaQ[2] + deltaQ[3]*deltaQ[3]);
            }

            // 计算角速度矢量（rad/s）
            rotateVelocity[0] = (theta * axis[0]) / dt;
            rotateVelocity[1] = (theta * axis[1]) / dt;
            rotateVelocity[2] = (theta * axis[2]) / dt;
        }

        // 更新历史四元数
        mInstance.lastQuat = currentQuat.clone();

        if (mInstance.angular_counter == 0) {
            mInstance.w_hat = rotateVelocity.clone();
        } else {
            for (int i = 0; i < 3; i++) {
                mInstance.w_hat[i] = mInstance.predictAlpha * rotateVelocity[i]
                        + (1 - mInstance.predictAlpha) * mInstance.w_hat[i];
            }
        }
        mInstance.angular_counter++;


        if (mInstance.linear_counter == 0) {
            mInstance.v_hat = linearVelocity.clone();
        } else {
            for (int i = 0; i < 3; i++) {
                mInstance.v_hat[i] = mInstance.predictAlpha * linearVelocity[i]
                        + (1 - mInstance.predictAlpha) * mInstance.v_hat[i];
            }
        }
        mInstance.linear_counter++;


        // Compute the predict rotation matrix and linear velocity in world coordinator
        double[] temp_quat = currentQuat;
        double[] omega = new double[]{
                mInstance.w_hat[0] * 0.5 * dt,
                mInstance.w_hat[1] * 0.5 * dt,
                mInstance.w_hat[2] * 0.5 * dt
        };
        double qw = temp_quat [0] - omega[0] * temp_quat [1] - omega[1] * temp_quat [2] - omega[2] * temp_quat [3];
        double qx = temp_quat [1] + omega[0] * temp_quat [0] + omega[2] * temp_quat [2] - omega[1] * temp_quat [3];
        double qy = temp_quat [2] + omega[1] * temp_quat [0] - omega[2] * temp_quat [1] + omega[0] * temp_quat [3];
        double qz = temp_quat [3] + omega[2] * temp_quat [0] + omega[1] * temp_quat [1] - omega[0] * temp_quat [2];
        double norm = Math.sqrt(qw*qw + qx*qx + qy*qy + qz*qz);
        qw /= norm;
        qx /= norm;
        qy /= norm;
        qz /= norm;
        temp_quat = new double[]{qw, qx, qy, qz};
        double[][] rotation_matrix = quaternionToRotationMatrix(temp_quat);
        Log.d("EMADebug", "Y-Axis: " + Arrays.toString(rotation_matrix[1]));
        double[] vel = applyRotation(rotation_matrix, mInstance.v_hat);

        // Compute next position for camera
        double[] predictPosition = new double[3];
        for (int i = 0; i < 3; i++) {
//            currentPosition[i] += (float) (vel[i] * dt);
//            if(i==1){
//                if (Math.abs(vel[1]) < 0.1) {
//                    predictPosition[i] = currentPosition[i] + vel[i] * dt;
//                } else {
//                    double accel = (vel[1] - mInstance.lastVY) / dt;
//                    predictPosition[i] = currentPosition[i] + vel[1] * dt + 0.5 * accel * dt * dt;
//                }
//            }else {
                predictPosition[i] = currentPosition[i] + vel[i] * dt;
            //}
        }
        mInstance.lastVY = vel[1];
        mInstance.pos = predictPosition;
        mInstance.quat = temp_quat;
        rotation = currentQuat;
        camera_position = currentPosition;
        Log.d("EMADebug", "Actual Y: " + camera_position[1] +
                ", Predicted Y: " + predictPosition[1] + ", lastVY: " + mInstance.lastVY);
        Log.d("EMADebug", "Matrix Y: " + Arrays.toString(rotation_matrix[1]));
        writeDataForModel();
    }


    // Use Following Code to convert angular velocity
    public static double[][] quaternionToRotationMatrix(double[] q) {
        double qw = q[0], qx = q[1], qy = q[2], qz = q[3];

        double[][] R = new double[3][3];

        R[0][0] = 1 - 2 * (qy * qy + qz * qz);
        R[0][1] = 2 * (qx * qy - qw * qz);
        R[0][2] = 2 * (qx * qz + qw * qy);

        R[1][0] = 2 * (qx * qy + qw * qz);
        R[1][1] = 1 - 2 * (qx * qx + qz * qz);
        R[1][2] = 2 * (qy * qz - qw * qx);

        R[2][0] = 2 * (qx * qz - qw * qy);
        R[2][1] = 2 * (qy * qz + qw * qx);
        R[2][2] = 1 - 2 * (qx * qx + qy * qy);

        return R;
    }

    public static double[] applyRotation(double[][] R, double[] localVelocity) {
        double[] worldVelocity = new double[3];

        for (int i = 0; i < 3; i++) {
            worldVelocity[i] = R[i][0] * localVelocity[0] +
                    R[i][1] * localVelocity[1] +
                    R[i][2] * localVelocity[2];
        }

        return worldVelocity;
    }
    private double[] inverseQuaternion(double[] q) {
        return new double[]{q[0], -q[1], -q[2], -q[3]};
    }


    private double[] multiplyQuaternion(double[] q1, double[] q2) {
        return new double[]{
                q1[0]*q2[0] - q1[1]*q2[1] - q1[2]*q2[2] - q1[3]*q2[3],
                q1[0]*q2[1] + q1[1]*q2[0] + q1[2]*q2[3] - q1[3]*q2[2],
                q1[0]*q2[2] - q1[1]*q2[3] + q1[2]*q2[0] + q1[3]*q2[1],
                q1[0]*q2[3] + q1[1]*q2[2] - q1[2]*q2[1] + q1[3]*q2[0]
        };
    }







    public void writeDataForModel(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "ModelData"+mInstance. fileseries+".csv";
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
           StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date()));
            sb.append(',');
            sb.append(Arrays.toString(mInstance.quat).replaceAll("[\\[\\] ]", ""));       // Rotation (quaternion)
            sb.append(',');
            sb.append(Arrays.toString(mInstance.pos).replaceAll("[\\[\\] ]", ""));       // Rotation (quaternion)
            sb.append(',');
            sb.append(Arrays.toString(rotation).replaceAll("[\\[\\] ]", ""));       // Rotation (quaternion)
            sb.append(',');
            sb.append(Arrays.toString(camera_position).replaceAll("[\\[\\] ]", "")); // Camera position
            sb.append(",");
            sb.append(Double.toString(mInstance.lastVY));
            sb.append(",");
            double max_tris = posVisibleTriangle();
            sb.append(max_tris);
            sb.append('\n');
            writer.write(sb.toString());

        }catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }

    }



    public void writequality(){


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Quality"+mInstance. fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            for (int i=0; i<objC-1; i++) {
                double curtris = mInstance.renderArray.get(i).orig_tris * mInstance.ratioArray.get(i);
                float r1 = mInstance.ratioArray.get(i); // current object decimation ratio
//               if (mInstance.renderArray.get(i).fileName.contains("0.6")) // third scenario has ratio 0.6
//                   r1 = 0.6f; // jsut for scenario3 objects are decimated
//               else if(mInstance.renderArray.get(i).fileName.contains("0.3")) // sixth scenario has ratio 0.3
//                   r1=0.3f;


                float r2 = ref_ratio * r1; // wanna compare obj level of sensitivity to see if we decimate object more -> to (ref *curr) ratio, would the current object hurt more than the other ones?
                int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(i).fileName);// search in excel file to find the name of current object and get access to the index of current object
                // excel file has all information for the degredation model
                float gamma = mInstance.excel_gamma.get(indq);
                float a = mInstance.excel_alpha.get(indq);
                float b = mInstance.excel_betta.get(indq);
                float c = mInstance.excel_c.get(indq);
                float d_k = mInstance.renderArray.get(i).return_distance();// current distance

                float tmper1 = Calculate_deg_er(a, b, c, d_k, gamma, r1); // deg error for current sit
                float tmper2 = Calculate_deg_er(a, b, c, d_k, gamma, r2); // deg error for more decimated obj


                float max_nrmd = mInstance.excel_maxd.get(indq);
                tmper1 = tmper1 / max_nrmd; // normalized
                tmper2= tmper2 /max_nrmd;

                if (tmper2 < 0)
                    tmper2 = 0;

                //Qi−Qi,r divided by Ti(1−Rr) = (1-er1) - (1-er2) / ....
                sensitivity[i] = (abs(tmper2 - tmper1) / (curtris - (ref_ratio * curtris)));
                tmper1 = (float) Math.round((float) (tmper1 * 1000)) / 1000;

                StringBuilder sb = new StringBuilder();
                sb.append(dateFormat.format(new Date()));
                sb.append(',');
                sb.append(mInstance.renderArray.get(i).fileName+"_n"+(i+1)+"_d"+(d_k));
                sb.append(',');
                sb.append(sensitivity[i]);
                sb.append(',');
                sb.append(r1);
                sb.append(',');
                sb.append(1-tmper1);
                sb.append(',');
                sb.append(reward);
                sb.append(',');
                sb.append(mInstance.best_BT);
                sb.append(',');
                sb.append((mInstance.best_BT-reward)/mInstance.best_BT);
                int hbo_running=0;// if hbo is not running, hence we have good data to show
                if(mInstance.curBysIters!=-1)
                    hbo_running=1;
                //  sb.append(mInstance.tasks.toString());
                sb.append(',');
                sb.append(hbo_running);


                sb.append(',');
                sb.append(mInstance.afterHbo_counter);
                sb.append('\n');
                writer.write(sb.toString());
//                System.out.println("done!");
            }
        }catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }

    }
//    public void writeThr(double realThr, double predThr, boolean trainedFlag,int aiIndx,double ai_acc,double meanH, double pred_H,double perAI_mape){ // AI throughput information for each task individually and response time for all models
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(dateFormat.format(new Date())); sb.append(',').append((aiIndx)).append(",");
//        sb.append(realThr);sb.append(',').append(predThr);sb.append(',').append(trainedFlag);sb.append(',').append(ai_acc).append(",");
//        sb.append(mInstance.rohTLRt.get(aiIndx));sb.append(',').append(mInstance.rohDLRt.get(aiIndx));sb.append(',').append(mInstance.deltaLRt.get(aiIndx));sb.append(',');
//        sb.append(mInstance.baseline_AIRt.get(aiIndx)* mInstance.des_Rt_weight);
//        sb.append(','); sb.append(mInstance.des_Q).append(',');
//        sb.append(mInstance.total_tris);
//      //  sb.append(',').append( meanthr);// this is measured directly
//        sb.append(','); sb.append(meanH).append(',').append(pred_H).append(',').append(perAI_mape);
//
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
//
//
//        for (int i=0;i<mInstance.mList.size();i++)
//        {
//            AiItemsViewModel taskView=mInstance.mList.get(i);
//            sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
//                    .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
//                    .append(",").append(taskView.getInferenceT()).append(",").append(taskView.getOverheadT());
//
//
//        }
//
//
//
//
//
//            sb.append('\n');
//            writer.write(sb.toString());
//            System.out.println("done!");
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }
//    }


    public void writeThr(double realThr, double predThr, boolean trainedFlag,double ai_acc){


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Throughput"+mInstance. fileseries+".csv";
        StringBuilder sb = new StringBuilder();



        sb.append(dateFormat.format(new Date())); sb.append(",,");
        sb.append(realThr);sb.append(',').append(predThr);sb.append(',').append(trainedFlag);sb.append(',').append(ai_acc).append(",");
        sb.append(" , , ,");// for weights
        // sb.append(mInstance.rohTL.get(aiIndx));sb.append(',').append(mInstance.rohDL.get(aiIndx));sb.append(',').append(mInstance.deltaL.get(aiIndx));sb.append(',');
        sb.append(mInstance.des_Thr);
        sb.append(','); sb.append(mInstance.des_Q).append(',');
        sb.append(mInstance.total_tris);

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            for (AiItemsViewModel taskView :mInstance.mList) {

                sb.append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                        .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                        .append(",").append(taskView.getInferenceT()).append(",").append(taskView.getOverheadT());
            }
            sb.append('\n');
            writer.write(sb.toString());
//            System.out.println("done!");
        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }
    }
    public void writeWeights( boolean ai_acc,int ai_indx){ // AI throughput information for each task individually and response time for all models

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Weights"+mInstance. fileseries+".csv";
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date())); sb.append(',').append(ai_acc);
        AiItemsViewModel taskView=mInstance.mList.get(ai_indx);
        sb .append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                .append(" ,")  .append(",").append(mInstance.nrmest_weights.get(ai_indx));
        sb.append('\n');

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            writer.write(sb.toString());
//            System.out.println("done!");
        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }
    }


    public void write_weightedH( double avg_msr_H, double avg_est_H, double msr, double pre, double Mape,boolean acc_model){ // AI throughput information for each task individually and response time for all models

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Weighted_throughput"+mInstance. fileseries+".csv";
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date())); sb.append(',').append(    avg_msr_H+","+avg_est_H+","+Mape+
                ","+msr+","+pre+","+acc_model);

        sb.append('\n');
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            writer.write(sb.toString());
//            System.out.println("done!");
        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }
    }


    // public void writeRE(double realRe, double predRe, boolean trainedFlag,double totT, double nextT, boolean trainedT, double pAR, double pAI){
    public void writeRE(double realRe, double predRe, boolean trainedFlag, double totT, double nextT, double algTris, boolean trainedT,
                        double pAR, double pAI, boolean accM, double totTris, double avgq, long duration){

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String currentFolder = mInstance.getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "RE"+mInstance. fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {
            StringBuilder sb = new StringBuilder();
            sb.append(dateFormat.format(new Date())); sb.append(',');
            sb.append(realRe);sb.append(',');
            sb.append(predRe);
            sb.append(',');  sb.append(trainedFlag);
            sb.append(',');  sb.append(totT);
            sb.append(',');  sb.append(nextT);
            sb.append(',');  sb.append(algTris);
            sb.append(',');  sb.append(trainedT);
            sb.append(',');  sb.append(pAR);
            sb.append(',');  sb.append(pAI);
            sb.append(',');  sb.append(accM);// if both models are accurate
            sb.append(',');  sb.append(totTris);// if both models are accurate
            sb.append(',');  sb.append(avgq);
            sb.append(',');  sb.append(duration);
            sb.append('\n');
            writer.write(sb.toString());
//            System.out.println("done!");
        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
        }
    }



    public float calculateMeanQuality( ) {

        float sumQual=0;
        for (int ind = 0; ind < mInstance.objectCount; ind++)
        {
            int i =  mInstance.excelname.indexOf( mInstance.renderArray.get(ind).fileName);
            float gamma = mInstance.excel_gamma.get(i);
            float a = mInstance.excel_alpha.get(i);
            float b = mInstance.excel_betta.get(i);
            float c = mInstance.excel_c.get(i);
            float d = mInstance.renderArray.get(ind).return_distance();
            float curQ = mInstance.ratioArray.get(ind);


            float deg_error = (float) Math.round((float) (Calculate_deg_er(a, b, c, d, gamma, curQ) * 1000)) / 1000;
            float max_nrmd = mInstance.excel_maxd.get(i);

            float cur_degerror = deg_error / max_nrmd;
            float quality= 1- cur_degerror;
            objquality[ind]=quality;
            sumQual+=quality;


        }


        return sumQual/mInstance.objectCount;



    }

    public float Calculate_deg_er(float a,float b,float creal,float d,float gamma, float r1) {

        float error;
        if(r1==1)
            return  0f;
        error = (float) (((a * Math.pow(r1,2)) + (b * r1) + creal) / (Math.pow(d , gamma)));
        return error;
    }



    float odraAlg(float tUP) throws InterruptedException {


        candidate_obj = new HashMap<>();
        Map<Integer, Double> sortedcandidate_obj = new HashMap<>();
        float sum_org_tris = 0; // sum of all tris of the objects o the screen

        for (int ind = 0; ind < mInstance.objectCount; ind++) {

            sum_org_tris += mInstance.renderArray.get(ind).orig_tris;// this will ne used to cal min of tris needed at each row (object) in bellow


            double curtris = mInstance.renderArray.get(ind).orig_tris * mInstance.ratioArray.get(ind);
            current_tris = curtris;


            tris_share[ind] = (curtris / tUP);
            candidate_obj.put(ind, sensitivity[ind] / tris_share[ind]);


        }
        sortedcandidate_obj = sortByValue(candidate_obj, false); // second arg is for order-> ascending or not? NO
        // Up to here, the candidate objects are known


        double updated_sum_org_tris = sum_org_tris; // keeps the last value which is sum_org_tris - tris1-tris2-....
        for (int i : sortedcandidate_obj.keySet()) { // check this gets the candidate object index to calculate min weight
            double sum_org_tris_minus = updated_sum_org_tris - mInstance.renderArray.get(i).orig_tris; // this is summ of tris for all the objects except the current one
            updated_sum_org_tris = sum_org_tris_minus;
            tMin[i] = coarse_Ratios[coarse_Ratios.length - 1] * sum_org_tris_minus;// minimum tris needs for object i+1 to object n
            ///@@@@ if this line works lonely, delete the extra line for the last object to zero in the alg
        }

        Map.Entry<Integer, Double> entry = sortedcandidate_obj.entrySet().iterator().next();
        int key = entry.getKey(); // get access to the first key -> to see if it is the first object for bellow code

        int prevInd = 0;
        for (int i : sortedcandidate_obj.keySet()){  // line 10 i here is equal to alphai -> the obj with largest candidacy
            // check this gets the candidate object index to maintain its quality
            for (int j = 0; j < coarse_Ratios.length; j++) {

                int indq = mInstance.excelname.indexOf(mInstance.renderArray.get(i).fileName);// search in excel file to find the name of current object and get access to the index of current object
                float gamma =mInstance. excel_gamma.get(indq);
                float a =mInstance. excel_alpha.get(indq);
                float b = mInstance.excel_betta.get(indq);
                float c =mInstance. excel_c.get(indq);
                float d_k = mInstance.renderArray.get(i).return_distance();// current distance
                float max_nrmd = mInstance.excel_maxd.get(indq);
                float quality = 1 -( Calculate_deg_er(a, b, c, d_k, gamma, coarse_Ratios[j]) / max_nrmd  ); // deg error for current sit

                if (i == key && tUP >= mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]) { // the first object in the candidate list
                    fProfit[i][j] = quality;// Fα(i),j ←Qα(i),j -> i is alpha i
                    tRemainder[i][j] = tUP - (mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]);
                } else //  here is the dynamic programming section
                    for (int s = 0; s < coarse_Ratios.length; s++) {

                        double f = fProfit[prevInd][s] + quality;
                        double t = tRemainder[prevInd][s] - (mInstance.renderArray.get(i).getOrg_tris() * coarse_Ratios[j]);
                        if (t >= tMin[i] && fProfit[i][j] < f) {

                            fProfit[i][j] = f;
                            tRemainder[i][j] = t;
                            track_obj[i][j] = s;
                        }

                    }//

            }//for j  up to here we reach line 25
            prevInd=i;
        }// for i
/// start with object with least priority

        sortedcandidate_obj = sortByValue(candidate_obj, true); // to iterate through the list from lowest to highest values

        int lowPobjIndx = sortedcandidate_obj.entrySet().iterator().next().getKey(); // line 26
        double tmp=fProfit[lowPobjIndx][0];
        int j=0;
        for  (int maxindex=1;maxindex<coarse_Ratios.length;maxindex++) // line 27
            if(fProfit[lowPobjIndx][maxindex]>tmp)// finds the index of coarse-grain ratio with maximum profit
            {
                tmp = fProfit[lowPobjIndx][maxindex];
                j=maxindex;
            }


        for (int i : sortedcandidate_obj.keySet())
        //if ( mInstance.renderArray.size()>i &&  mInstance.renderArray.get(i)!=null)

        {// to avoid null pointer error

            mInstance.total_tris = mInstance.total_tris - (mInstance.ratioArray.get(i) * (mInstance.o_tris.get(i)));// total =total -1*objtris
            mInstance.ratioArray.set(i, coarse_Ratios[j]);


            mInstance.runOnUiThread(() -> mInstance.renderArray.get(i).decimatedModelRequest(mInstance.ratioArray.get(i), i, false));

            mInstance.total_tris = mInstance.total_tris + (mInstance.ratioArray.get(i) *  mInstance.renderArray.get(i).orig_tris);// total = total + 0.8*objtris
            // mInstance.trisDec.put(mInstance.total_tris,true);

            j = track_obj[i][j];
            Thread.sleep(sleepTime);// added to prevent the crash happens while redrawing all the objects at the same time


        }


        return (float) mInstance.total_tris; // this returns the total algorithm triangle count



    }


    private static Map<Integer, Double> sortByValue(Map<Integer, Double> unsortMap, final boolean order)
    {
        List<Map.Entry<Integer, Double>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }






    public double posVisibleTriangle(){
        double maxTris = 0;
        for(int i = 0; i < mInstance.objectCount; i++){
            Vector3 worldPosition = mInstance.renderArray.get(i).baseAnchor.getWorldPosition();
            if(mInstance.isObjectVisible(worldPosition)){
                maxTris += mInstance.o_tris.get(i);
            }
        }
        return maxTris;
    }



}


