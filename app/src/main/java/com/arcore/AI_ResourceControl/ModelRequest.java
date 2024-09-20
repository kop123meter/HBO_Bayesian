package com.arcore.AI_ResourceControl;


import android.content.Context;
import android.media.Image;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ModelRequest {

    private String filename;// either object file name for decimation or
    private float percentageReduction;
    private Context appContext;
    private WeakReference<MainActivity> mainActivityWeakReference;
    public  final MainActivity activityMain;
    private int ID;

    private double remaining_task;
    private Queue<Integer> similarRequestIDArray;
    private float cache;
    String delegate;
    public double[] all_delegates = new double[30];
  //  public List<double[]> delg_list = new ArrayList<>();
    String req="decimate";// this is to connect to the server to receive delegate and triangle count
    //private List<Float> cacheratio ;

    ModelRequest(float cr,String filename, float percentageReduction, Context context, MainActivity mainActivity, int mID)// this is for decimation request
    {
        Log.d("ModelRequest", "Created ModelRequest - filename: " + filename);
        this.filename = filename;
        this.percentageReduction = percentageReduction;
        this.appContext = context;
        this.activityMain=mainActivity;// reference to activityMain class
        this.mainActivityWeakReference = new WeakReference<MainActivity>(mainActivity);
        this.ID = mID;
        this.similarRequestIDArray = new LinkedBlockingQueue<Integer>();
        this.similarRequestIDArray.add(this.ID);
        this.cache=cr;
        req="decimate";
        //this.cacheratio=new ArrayList<>(cacheratio) ;
    }

    ModelRequest( Context context, MainActivity mainActivity, int id,String req) // this is for bayesian delegate request
    {
        this.activityMain=mainActivity;
        this.req=req;
        Log.d("ModelRequest", "Created ModelRequest - filename: " + filename);
        this.appContext = context;
        this.mainActivityWeakReference = new WeakReference<MainActivity>(mainActivity);
        this.ID = id;
        this.remaining_task = mainActivity.mList.size() - mainActivity.serverList.size();
//        System.out.println("SIZE1 : "  + mainActivity.mList.size());
//        System.out.println("SIZE2 : "  + mainActivity.serverList.size());


    }


//    ModelRequest(String aiName, Context context, MainActivity mainActivity,Image img)
//    {
//
//        this.filename = aiName;
//        Log.d("ModelRequest", "Created ModelRequest - filename: " + filename);
//        this.appContext = context;
//        this.mainActivityWeakReference = new WeakReference<MainActivity>(mainActivity);
//        imgFrame=img;
//    }


//    public Image getImage()
//    {
//        return imgFrame;
//    }
    public float getCache()
    {
        return cache;
    }
    public float getPercentageReduction()
    {
        return percentageReduction;
    }
    public String getFilename()
    {
        return filename;
    }

    public Context getAppContext()
    {
        return appContext;
    }

    public double getRemaining_task(){return remaining_task;}

    public int getID() { return ID; }
    public Queue<Integer> getSimilarRequestIDArray() { return similarRequestIDArray; }
    public void addIDToArray(int ID)
    {
        similarRequestIDArray.offer(ID);
    }
    public WeakReference<MainActivity> getMainActivityWeakReference()
    {
        return mainActivityWeakReference;
    }

    public void setRemaining_task(int task_num){
        this.remaining_task = task_num;
    }






}
