
package com.arcore.AI_ResourceControl;


import android.app.Activity;
import android.app.Application;
import android.app.DownloadManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;




public class ModelRequestManager {

    static final int DOWNLOAD_FAILED = -1;
    static final int DOWNLOAD_PENDING = 1;
    static final int DOWNLOAD_STARTED = 2;
    static final int DOWNLOAD_COMPLETE = 3;


//
    private static final int KEEP_ALIVE_TIME = 100;
    //public static 2 repeatedRequestList;
    //public static int repeatedRequestList;

    private final TimeUnit KEEP_ALIVE_TIME_UNIT;

   // private final LinkedList<ModelRequest> mRequestList;
    static LinkedList<ModelRequest> mRequestList=new LinkedList<ModelRequest>();


    static LinkedList<ModelRequest> repeatedRequestList=new LinkedList<ModelRequest>();

    private final BlockingQueue<Runnable> mWorkQueue;

    private final ThreadPoolExecutor mDownloadThreadPool;

    private final int CORE_THREAD_POOL_SIZE = 50;

    private final int MAX_THREAD_POOL_SIZE = 50;



    //

    private static ModelRequestManager Instance = null;

    //private final Queue<ModelRequest> RequestQueue;

    Thread currentThread;

    ModelRequest currentModelRequest;

    int currentState = 0;

    static
    {
        Instance = new ModelRequestManager();
    }

    private ModelRequestManager()
    {
        //default max is 11
       // RequestQueue = new LinkedBlockingQueue<ModelRequest>();

        KEEP_ALIVE_TIME_UNIT  = TimeUnit.SECONDS;

        mWorkQueue = new LinkedBlockingQueue<Runnable>();

        mDownloadThreadPool = new ThreadPoolExecutor(CORE_THREAD_POOL_SIZE, MAX_THREAD_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mWorkQueue);

       // mRequestList = new LinkedList<ModelRequest>();

       // repeatedRequestList = new LinkedList<ModelRequest>();
    }

    public static ModelRequestManager getInstance()
    {
        return Instance;
    }


    public void handleState(int state, ModelRequest modelRequest)
    {
        switch(state)
        {
            case DOWNLOAD_PENDING:
                Log.d("ModelRequest", "Recieved DOWNLOAD_PENDING state");
                /*if(!RequestQueue.isEmpty() && currentState != DOWNLOAD_STARTED)
                {
                    Log.d("ModelRequest", "Queue is empty and no download is going, creating request.");
                    sendRequest();
                }*/
                break;
            case DOWNLOAD_COMPLETE:
                Log.d("ModelRequest", "Recieved DOWNLOAD_COMPLETE state");
                //Message msg = handler.obtainMessage(currentState, currentModelRequest);

                Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
                msg.obj = modelRequest;
                modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);
                mRequestList.remove(modelRequest);
                currentState = DOWNLOAD_COMPLETE;
                //handleState(DOWNLOAD_PENDING);
                //currentModelRequest = null;
                break;

            case DOWNLOAD_FAILED:
                Log.w("ModelRequest", "Recieved DOWNLOAD_FAILED state");
                // what do?
                break;
        }
    }
//Nil
    public void add(ModelRequest modelRequest, boolean referenceSwitch, boolean offloading) {

       // referenceSwitch=true;// I added it manually here to redraw from the cache

        if(offloading ==true){// this is to send offloading req and recieve result.txt

            Instance.mDownloadThreadPool.execute(new OffloadRequestRunnable(modelRequest, Instance));

        }
        else{

            if (referenceSwitch==true) // means baseline 2 just get back to main for redrawing the obj

               {
                  Message msg = modelRequest.getMainActivityWeakReference().get().getHandler().obtainMessage();
                   msg.obj = modelRequest;
                    modelRequest.getMainActivityWeakReference().get().getHandler().sendMessage(msg);

                }
            else {

                 Iterator requestIterator = mRequestList.iterator();

                 while (requestIterator.hasNext()) {
                       ModelRequest tempRequest = (ModelRequest) requestIterator.next();
                       if (modelRequest.getFilename() == tempRequest.getFilename()
                        && modelRequest.getPercentageReduction() == tempRequest.getPercentageReduction() && modelRequest.getID() != tempRequest.getID()) {
                    Log.d("ModelRequest", "MATCHING FILENAME + CONVERSION. Adding ID " + modelRequest.getID() + " to ID " + tempRequest.getID());
                    tempRequest.addIDToArray(modelRequest.getID());
                    int current_ser_freq = modelRequest.getMainActivityWeakReference().get().Server_reg_Freq.get(modelRequest.getID());// to avoid repatative similar req by similar obj type
                    if (modelRequest.getPercentageReduction() != 1 && modelRequest.getPercentageReduction() != modelRequest.getCache())
                        modelRequest.getMainActivityWeakReference().get().Server_reg_Freq.set(modelRequest.getID(), current_ser_freq - 1);
                    //if(tempRequest.getID()!= modelRequest.getID())// avoid repeatative req for same obj and same reduction ratio
                    //  repeatedRequestList.add(modelRequest);
                    return;// remove repeated since in runnable we will redraw obj that are existed in phone mem
                }

                   if (tempRequest.getID() == modelRequest.getID() && modelRequest.getPercentageReduction() != tempRequest.getPercentageReduction())
                    // means that we have already a request but we changed it so remove that
                      mRequestList.remove(tempRequest);

                    }


                  mRequestList.offer(modelRequest);
                  Log.d("ModelRequest", "Sending ID " + modelRequest.getID() + " out to execute.");
                 // un comment parallelism and start sequential  decimation
                  Instance.mDownloadThreadPool.execute(new ModelRequestRunnable(modelRequest, Instance));
                  // new ModelRequestRunnable(modelRequest, Instance).run();
        }}




        return;

    }

public void clear(){


        mRequestList.clear();
        repeatedRequestList.clear();
        mWorkQueue.clear();
}



}
