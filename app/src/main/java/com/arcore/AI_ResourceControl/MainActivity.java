package com.arcore.AI_ResourceControl;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.os.SystemClock;
import android.util.Log;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.CountDownTimer;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
  import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import java.lang.Math;
import java.io.InputStream;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.round;
//import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper;

/*TODO: constant update distance to file
  see if we can update AR capabilities -- find out pointer operation (why will it not draw past 2 meters or whateverz
  update menu popups for simplified files -- thumbnails have to be 64x64
  compare anchor and hit position in place object

*/

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // BitmapUpdaterApi gets bitmap version of ar camera frame each time
    // on onTracking is called. Needed for DynamicBitmapSource
    private final BitmapUpdaterApi bitmapUpdaterApi = new BitmapUpdaterApi();
    static List<AiItemsViewModel> mList = new ArrayList<>();

    private ArFragment fragment;
    private PointerDrawable pointer = new PointerDrawable();
    //private static final String GLTF_ASSET = "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb";
   // private static MainActivity Instance = new MainActivity();
    //private static MainActivity Instance = new com.arcore.MixedAIAR.MainActivity();


    //    static
//    {
//        Instance =
//    }
//    public static MainActivity getInstance()
//    {
//        return Instance;
//    }

    private boolean isTracking;
    private boolean isHitting;

   //  baseRenderable renderArray[] = new baseRenderable[obj_count];
    List<baseRenderable> renderArray =  new ArrayList<baseRenderable>();

    List<Float>ratioArray = new ArrayList<Float>();
    List<Float>cacheArray = new ArrayList<Float>();
    List<Float>updatednetw = new ArrayList<Float>();
    /*please note that this factor selection affects the alpha parameters for throughput model. so make sure to choose it correctly
    * eg, for some AI models if normalized  tris goes from 0.2 to 85, and throughput of AI1 is 9, the alpha_d changes for, 0.002 to 3.4 for one model which is not good compared to other models that might have still alpha d bellow 0.1*/
   // double tris_factor=100000; // to normalize tris and have a better parameters for throughput model
    double tris_factor=1000; // to normalize tris and have a better parameters for throughput model

    int maxtime=6; // 20 means calculates data for next 10 sec ->>>should be even num
    // if 5, goes up to 2.5 s. if 10, goes up to 5s
    double pred_meanD_cur=0; // predicted mean distance in next two second saved as current d in dataCol for next period
    //for RE modeling and algorithm

    List<Double> rE= new ArrayList<>();// keeps the record of RE
    List< List<Double>> rERegList= new ArrayList<List<Double>>();// keeps the record of RE

    long curTrisTime = 0;// holds the time when tot triangle count changes
    private float ref_ratio=0.5f;

    Map <Integer, Float> candidate_obj;
    float []coarse_Ratios=new float[]{1f,0.8f, 0.6f , 0.4f, 0.2f, 0.05f};
    //ArrayList <ArrayList<Float>> F_profit= new ArrayList<>();
  //  boolean datacol=false;
    boolean trainedTris = false;
    //double nextTris = 0;
    //double algNxtTris = 0;
    long t_loop1=0;
    String odraAlg="1";
    //long t_loop2=0;
    StringBuilder tasks = new StringBuilder();


    //$$$$$$$$$$$$$$$$$$$ for XMIRE-
    ListMultimap<Integer, Double> modelMeanRsp = ArrayListMultimap.create();//  a map from tot tris to mean throughput
    ListMultimap<Integer, Double> total_triangle = ArrayListMultimap.create();//  a map from tot tris to mean throughput

    ListMultimap<Integer, Double> coeff_modelMeanRsp = ArrayListMultimap.create();//  a map from tot tris to mean throughput
    ListMultimap<Integer, Double> coeff_total_triangle = ArrayListMultimap.create();//  a map from tot tris to mean throughput

    List<ListMultimap<Double, Double>> rsp_models= new ArrayList<>();// map from each model to throughput an Tris
    List<Double> rohTL= new ArrayList<>();
    List<Double> rohDL= new ArrayList<>();
    List<Double> deltaL= new ArrayList<>();
    List<Boolean> hAI_acc= new ArrayList<>();// the accuracy of AI throughput model


    List<Double> baseline_AIthr= new ArrayList<>();// holds baseline throughput of fixed models running on fixed devices
    List<Double> est_weights= new ArrayList<>();// this is a list of normalized  estimated weights
//    List<Double> msr_weights= new ArrayList<>();// this is a list of normalized measured weights

    List<Double> baseline_est_weights= new ArrayList<>();// the accuracy of AI throughput model

    List<Integer> rsp_miss_counter=new ArrayList<>();
    List<Integer> thr_miss_counter=new ArrayList<>();
    List<Multimap<Double, Double>> thr_models= new LinkedList<>();// map from each model to throughput an Tris -> has HASHEDMULTIMAP that preserves order of insersion
    // List<LinkedHashMultimap<Double, Double>> thr_models= new ArrayList<>();
//    Multimap<Double, Double> hm = LinkedHashMultimap.create();

    int thr_miss_counter1=0;
    double last_tris=0;
    //$$$$$$$$$$$$$$
    double des_Q= 0.7; //# this is avg desired Q
    double des_Thr = 35; // 0.65*throughput; in line 2063,
    List<ListMultimap<Double, List<Double>>> tParamList = new ArrayList<>(); // LinkedHashMultimap to keep order of insersion to hold list of response time
    List<ListMultimap<Double, List<Double>>> thParamList = new ArrayList<>(); // to hold list of throughput

    ListMultimap<Double, List<Double>> thParamList_Mir = ArrayListMultimap.create();

    //ArrayListMultimap.create();//  a map from tot tris to measured RE
    ListMultimap<Integer, List<Double>> rspParamList = ArrayListMultimap.create();//  a map from tot tris to measured RE


  //List<  ListMultimap<Double, Double>> trisMeanDisk =new ArrayList<>();

          ListMultimap<Double, Double> trisMeanDisk = ArrayListMultimap.create();// for all fixed AI tasks we have the same distance from objects

    ListMultimap<Double, Double> trisMeanThr = ArrayListMultimap.create();//  a map from tot tris to mean throughput

    ListMultimap<Integer, Double> modelMeanDisk = ArrayListMultimap.create();//  a map from tot tris to mean dis at current period

    ListMultimap<Double, Double> trisMeanDisk_Mir = ArrayListMultimap.create();//  a map from tot tris to mean dis at current period


    Multimap<Double, Double> trisRe= LinkedHashMultimap.create();
           // new LinkedList<>(); // to hold list of throughput

    ListMultimap<Double, Double> trisReMir = ArrayListMultimap.create();//  a map from tot tris to measured RE
    ListMultimap<Double, List<Double>> reParamList = ArrayListMultimap.create();//  a map from tot tris to measured RE
    ListMultimap<Double, List<Double>> reParamListMir =ArrayListMultimap.create();

    int lastConscCounter=0; // counts the number of consecutive change in tris count, if we reach 5 we will change the tris
    int acc_counter=0;
    double prevtotTris=0;
    double  rohT=-0.06 ;
    double rohD=0.0001;
    double delta=66.92;
    double thRmse;
    double des_weight=0.5;
    //for RE modeling and algorithm

    double orgTrisAllobj=0d;
    public int objectCount = 0;
    private String[] assetList = null;
    //private Integer[] objcount = new Integer[]{1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 160, 170, 180, 190, 200, 220, 240, 260, 300, 340, 380, 430, 500};
    //private float[] distance_log = new float[]{2.24f,2.0f, 2.24f, 2.83f, 3.61f, 4.47f, 5.39f, 6.32f, 7.28f, 8.25f, 9.22f, 10.2f, 11.18f, 12.17f, 13.15f };
    private Double[] desiredQ = new Double[]{0.7, 0.5, 0.3 };
    private String[] desiredalg = new String[]{"1","2" ,"3"};
    private Double[] desiredThr_weight = new Double[]{1.3,1.1, 0.9, 0.8, 0.7 , 0.6, 0.5};
    private String currentModel = null;
     boolean decAll  = true; // older name :referenceObjectSwitchCheck
    private boolean autoPlace = false;// older name multipleSwitchCheck
    private boolean askedbefore = false;
     int nextID = 1;
    boolean under_Perc = false; // it is used for seekbar and the percentages with 0.1 precios like 1.1%, I press 11% in app and /1000 here
    boolean fisrService = false;
    CountDownTimer countDownTimer;
    float agpu=4.10365E-05f;
    float gpu_min_tres=35000;
    float bgpu=44.82908722f;
    float bwidth= 600;
    boolean removePhase=false; // this is for the mixed adding and after that removing scnario
    boolean setDesTh=false;// used just for the first time when we run AI models and get the highest baseline throughput
    List<Double> decTris= new ArrayList<>();// create a list of decimated

    private ArrayList<String> scenarioList = new ArrayList<>();
    private String currentScenario = null;
    private int scenarioTickLength = 50000;// should be always odd/even based on XMIR decision period -> so if dec_p=2, here we select an odd
    //value to make sure tris change from object addition will not affect data collection in balancer.java (it collects data of recent tris)
    //private int removalTickLength = 25000;
    private ArrayList<String> taskConfigList = new ArrayList<>();
    private String currentTaskConfig = null;
    private int taskConfigTickLength = 50000;
    private int pauseLength = 10000;

    double thr_factor=0.6;
    double re_factor=0.9;
    //int rsp_miss_counter=0;
    int re_miss_counter=0;


    List<String> mLines = new ArrayList<>();
    //  List<String> time_tris = new ArrayList<>();
 //   Map<String, Integer> time_tris = new HashMap<>();
  //  Map<String, Integer> time_gpu = new HashMap<>();


    ArFragment arFragment = (ArFragment)
            getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);


    private DecimalFormat posFormat = new DecimalFormat("###.##");
    private final int SEEKBAR_INCREMENT = 10;
    File dateFile;
   // File Nil;
    File obj;
    File tris_num;
    File GPU_usage;
    boolean trisChanged=false;
    float percReduction = 0;
    int decision_p=1;
    List<Integer> o_tris = new ArrayList<>();

    double initial_meanD=0;
    double initial_totT=0;

    List<Float> prevquality = new ArrayList<>();
    Process process2;
    List<Float> excel_alpha = new ArrayList<>();
    List<Float> excel_betta = new ArrayList<>();
    List<Float> excel_filesize = new ArrayList<>();
    List<Float> excel_c = new ArrayList<>();
    List<Float> excel_gamma = new ArrayList<>();
    List<Float> excel_maxd = new ArrayList<>();
    List<String> excelname = new ArrayList<>();
    List<Integer> excel_tris = new ArrayList<>();
    List<Float> excel_mindis = new ArrayList<>();
    List<Boolean> closer = new ArrayList<>();
    List<Float> max_d = new ArrayList<>();
   // List<String> temppredict = new ArrayList<>();
  //  List<String> tempquality = new ArrayList<>();
    List<Float> best_cur_eb = new ArrayList<>();
    List<Float> gpusaving = new ArrayList<>();
    List<String> eng_dec = new ArrayList<>();

    int baseline_index=0;// index of all objects ratio of the coarse_ratio array

    List<String> quality_log = new ArrayList<>();
    List<String> time_log = new ArrayList<>();
    List<String> distance_log = new ArrayList<>();
    List<String> deg_error_log = new ArrayList<>();
    List<Float> obj_quality = new ArrayList<>();
  //  Double prevTris=0d;
    List<Integer> Server_reg_Freq = new ArrayList<>();

    List<Thread> decimate_thread = new ArrayList<>(); //@@ it is needed for server requests
    int decimate_count=0;
    int AI_tasks=0;
    String policy= "Mean";
  //  private String[] Policy_Selection = new String[]{"Aggressive", "Mean", "Conservative"};

    int temp_ww = (((maxtime/2)-1) - (decision_p-1))/ decision_p;
   // private int[] W_Selection = IntStream.range(1, temp_ww).toArray();
   private Integer[] W_Selection= new Integer[temp_ww];

//    private Integer[] BW_Selection= new Integer[]{100, 200, 303, 400, 500,600,700,800,900,1000,1100,1200,1300,1400,1500,1600};
 //   private Integer[] MDE_Selection= new Integer []{2,6};
    int finalw=4;
    float max_d_parameter=0.2f;

//@@@ periodicTotTris of main instance is changed not actual main-> to access it always use getInstance.periodicTotTris
    List<Double> totTrisList= new ArrayList<Double>();// to collect triangle count every 500 ms

    //double l

    float area_percentage=0.5f;

    // Conservative , or mean are other options
    double total_tris=0;



    ///prediction - Nil/Saloni
    private ArrayList<Float> timeLog = new ArrayList<>();

   // List<Float> newdistance;
   Timer t2;
    private float timeInSec = 0;
    float phone_batttery_cap= 12.35f; // in w*h
    private ArrayList<ArrayList<Float> > current = new ArrayList<ArrayList<Float> >();
    private HashMap<Integer, ArrayList<ArrayList<Float>>> prmap=new HashMap<Integer, ArrayList<ArrayList<Float>>>();
    private HashMap<Integer, ArrayList<ArrayList<Float>>> marginmap=new HashMap<Integer, ArrayList<ArrayList<Float>>>();
    private HashMap<Integer, ArrayList<ArrayList<Float>>> errormap=new HashMap<Integer, ArrayList<ArrayList<Float>>>();
    private HashMap<Integer, ArrayList<ArrayList<Float>>> booleanmap=new HashMap<Integer, ArrayList<ArrayList<Float>>>();

   // private LinkedList<LinkedList<Float> > last_errors = new LinkedList<LinkedList<Float> >();
    private LinkedList<Float>  last_errors_x = new LinkedList<Float> ();
    private LinkedList<Float>  last_errors_z = new LinkedList<Float> ();
     HashMap<Integer, ArrayList<Float> >predicted_distances=new HashMap<Integer, ArrayList<Float>>();
    private HashMap<Integer, ArrayList<Float> >nextfive_fourcenters=new HashMap<Integer, ArrayList<Float>>();
    List<Float> d1_prev= new ArrayList<Float>();// for prediction module we need to store dprev
    private float objX, objZ;
    private ArrayList<ArrayList<Float> > nextfivesec = new ArrayList<ArrayList<Float> >();

    // RE regression parameters
    boolean cleanedbin=false;
    private float alpha = 0.7f;
   // int max_datapoint=25;
   int max_datapoint=28;
   // double reRegRMSE= Double.POSITIVE_INFINITY;
    double alphaT = 5.14E-7, alphaD=0.19, alphaH=1.34E-5, zeta=0.29;
    //double nextTris=0; // triangles for the next period

  //  private static final int KEEP_ALIVE_TIME = 500;
  //  private final int CORE_THREAD_POOL_SIZE = 10;
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
    public String fileseries=dateFormat.format(new Date());
    private final int MAX_THREAD_POOL_SIZE = 10;
    //KEEP_ALIVE_TIME_UNIT  =
    private final TimeUnit KEEP_ALIVE_TIME_UNIT= TimeUnit.MILLISECONDS;
    private final BlockingQueue<Runnable> mWorkQueue= new LinkedBlockingQueue<Runnable>();
  //  private final ThreadPoolExecutor algoThreadPool=new ThreadPoolExecutor(CORE_THREAD_POOL_SIZE, MAX_THREAD_POOL_SIZE, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mWorkQueue);


    /**coroutine flow source that captures camera frames from updateTracking() function*/
     DynamicBitmapSource source = new DynamicBitmapSource(bitmapUpdaterApi);

    //Eric code recieves messages from modelrequest manager
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            ModelRequest tempModelRequest = (ModelRequest) inputMessage.obj;
            Queue<Integer> tempIDArray = tempModelRequest.getSimilarRequestIDArray();

            if (decAll==true){//baseline 2
                int ind= tempModelRequest.getID();
              //  renderArray[ind].redraw(ind);
                renderArray.get(ind).redraw(ind);
            }


else{
            while (tempIDArray.isEmpty() == false) { // doesn't come here for baseline 2 - this is for eAR
                //ListIterator renderIterator = renderArray.listIterator(0);

               // int i=tempIDArray.peek();

                for (int i = 0; i < objectCount; i++) {


                    if ( renderArray.get(i).getID() == tempIDArray.peek()) {
                        Log.d("ModelRequest", "renderArray[" + i + "] ID: " +  renderArray.get(i).getID()
                                + " matches tempModelRequest SimilarRequestID: " + tempIDArray.peek());
                        renderArray.get(i).redraw(i);

                        //  }
                         }
                    }
                    tempIDArray.remove();

                }
        }
        }
    };


    public Handler getHandler() {
        return handler;
    }

    //used for abstraction of reference renderable and decimated renderable
    public abstract class baseRenderable {
        public TransformableNode baseAnchor;
        public String fileName;
        private int ID;
        public float orig_tris;
        //public float current_tris;

//        public float getcur_tris() {
//            return current_tris;
//        }
        public float getOrg_tris() {
            return orig_tris;
        }

        public void setAnchor(TransformableNode base) {
            baseAnchor = base;
        }

        public String getFileName() {
            return fileName;
        }

        public int getID() {
            return ID;
        }

        public void setID(int mID) {
            ID = mID;
        }

        public abstract void redraw(int i);

        public abstract void decimatedModelRequest(float percentageReduction, int i, boolean rd);
        public abstract void indirect_redraw(float percentageReduction, int i);
        //public abstract void print(AdapterView<?> parent, int pos);

       // public abstract void distance();

        public abstract float return_distance();


        public abstract float return_distance_predicted(float x, float z);

        public void detach() {
            try {
                baseAnchor.getScene().onRemoveChild(baseAnchor.getParent());
               // baseAnchor.getAnchor().detach();
                baseAnchor.setRenderable(null);
                baseAnchor.setParent(null);

            } catch (Exception e) {
                Log.w("Detach", e.getMessage());
            }

        }


        public void detach_obj() { //Nill
            try {
                baseAnchor.getScene().onRemoveChild(baseAnchor.getParent());
               // baseAnchor.setRenderable(null);
               // baseAnchor.setParent(null);

            } catch (Exception e) {
                Log.w("Detach", e.getMessage());
            }

        }

    }

    //reference renderables, cannot be changed when decimation percentage is selected
    private class refRenderable extends baseRenderable {
        refRenderable(String filename, float tris) {
            this.fileName = filename;
            this.orig_tris=tris;
         //   this.current_tris=tris;
            setID(nextID);
            nextID++;
        }

        public void decimatedModelRequest(float percentageReduction, int i, boolean rd) {
            return;
        }
        public void indirect_redraw(float percentageReduction, int i) {
            return;
        }


        public void redraw(int j) {
            return;
        }



        //public void distance(AdapterView<?> parent, int pos)
/*//        public void distance() {
//            {
//                float dist=1;
//                Frame frame = fragment.getArSceneView().getArFrame();
//
//                if(frame!=null)
//                   dist = ((float) Math.sqrt(Math.pow((baseAnchor.getWorldPosition().x - frame.getCamera().getPose().tx()), 2) + Math.pow((baseAnchor.getWorldPosition().y - frame.getCamera().getPose().ty()), 2) + Math.pow((baseAnchor.getWorldPosition().z - frame.getCamera().getPose().tz()), 2)));
//
//
//            }
//        }*/

        public float return_distance() {

            float dist=0;
            Frame frame = fragment.getArSceneView().getArFrame();
            while( frame==null)
                frame = fragment.getArSceneView().getArFrame();
           // if(frame!=null) {
            dist = ((float) Math.sqrt(Math.pow((baseAnchor.getWorldPosition().x - frame.getCamera().getPose().tx()), 2) + Math.pow((baseAnchor.getWorldPosition().y - frame.getCamera().getPose().ty()), 2) + Math.pow((baseAnchor.getWorldPosition().z - frame.getCamera().getPose().tz()), 2)));
            dist = (float) (Math.round((float) (dist * 100))) / 100;
         //   }

            return dist;

        }



        public float return_distance_predicted(float px,float pz) {

            //Frame frame = fragment.getArSceneView().getArFrame();

            float dist = ((float) Math.sqrt(Math.pow((baseAnchor.getWorldPosition().x - px), 2)  + Math.pow((baseAnchor.getWorldPosition().z - pz), 2)));

            dist = (float)(Math.round((float)(dist * 100))) / 100;
            return dist;

        }

    }


    //Decimated renderable -- has the ability to redraw and make model request from the manager
    private class decimatedRenderable extends baseRenderable {
        decimatedRenderable(String filename, float tris) {
            this.fileName = filename;
            this.orig_tris=tris;
          //  this.current_tris=tris;
            setID(nextID);
            nextID++;
        }


        public void indirect_redraw(float percentageReduction, int id) {

            percReduction = percentageReduction;
            renderArray.get(id).redraw(id);
        }


        public void decimatedModelRequest(float percentageReduction, int id, boolean redraw_direct) {
            //Nil

           // decimate_thread.add(decimate_count, new Thread(){

             //   @Override
               // public void run(){

                    percReduction = percentageReduction;
           //
            //      commented on May 2 2022   ModelRequestManager.getInstance().add(new ModelRequest(cacheArray[id], fileName, percentageReduction, getApplicationContext(), MainActivity.this, id),redraw_direct );
//April 21 Nill , istead of calling mdelreq, sinc we have already downloaded objs from screen, we can call directly redraw
            renderArray.get(id).redraw(id);


              //  }//


           // } );




            //Nil
        }

//        public void distance() {
//            {
//                Frame frame = fragment.getArSceneView().getArFrame();
//
//                float dist = ((float) Math.sqrt(Math.pow((baseAnchor.getWorldPosition().x - frame.getCamera().getPose().tx()), 2) + Math.pow((baseAnchor.getWorldPosition().y - frame.getCamera().getPose().ty()), 2) + Math.pow((baseAnchor.getWorldPosition().z - frame.getCamera().getPose().tz()), 2)));
//
//
//            }
//        }

        public float return_distance() {


            float dist=0;
            Frame frame = fragment.getArSceneView().getArFrame();
            if(frame!=null) {

                 dist = ((float) Math.sqrt(Math.pow((baseAnchor.getWorldPosition().x - frame.getCamera().getPose().tx()), 2) + Math.pow((baseAnchor.getWorldPosition().y - frame.getCamera().getPose().ty()), 2) + Math.pow((baseAnchor.getWorldPosition().z - frame.getCamera().getPose().tz()), 2)));
                dist = (float) (Math.round((float) (dist * 100))) / 100;
            }

            return dist;





        }


        public float return_distance_predicted(float px,float pz) {

           // Frame frame = fragment.getArSceneView().getArFrame();

            float dist = ((float) Math.sqrt(Math.pow((baseAnchor.getWorldPosition().x - px), 2)  + Math.pow((baseAnchor.getWorldPosition().z - pz), 2)));

            dist = (float)(Math.round((float)(dist * 100))) / 100;
            return dist;

        }


        public void redraw(int j) {



            Log.d("ServerCommunication", "Redraw waiting is done");
//Nil april 21
//     try {
//                    Frame frame = fragment.getArSceneView().getArFrame();
//                  //  while(frame==null)
//                     //   frame = fragment.getArSceneView().getArFrame();
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            Uri objUri =Uri.fromFile(new File(getExternalFilesDir(null), "/decimated" +  renderArray.get(j).fileName + ratioArray.get(j) + ".sfb"));


            if (  ratioArray.get(j)==1f)// for the times when perc_reduc is 1, we show the original object
                objUri=  Uri.parse("models/"+ renderArray.get(j).fileName+".sfb" );

            android.content.Context context= fragment.getContext();
            if (context!=null){
                CompletableFuture<Void> renderableFuture =
                        ModelRenderable.builder().setSource(context,objUri )
                               // .setSource(fragment.getContext(),objUri )
                                //.setIsFilamentGltf(true)
                                .build()
                                .thenAccept(renderable -> baseAnchor.setRenderable(renderable))
                                .exceptionally((throwable -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                                    builder.setMessage(throwable.getMessage())
                                            .setTitle("Codelab error!");
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    return null;
                                }));





// update tris and gu log
            }

            context=null;


        }



    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ////////////////
        // manyAI
        ////////////////

        /**coroutine flow source that captures camera frames from updateTracking() function*/
       // DynamicBitmapSource source = new DynamicBitmapSource(bitmapUpdaterApi);
        /** coroutine flow source that passes static jpeg*/
//        BitmapSource source = new BitmapSource(this, "chair_600.jpg");

//        for(int i = 0; i<20; i++) {
        mList.add(new AiItemsViewModel());
//        }
        // Define the recycler view that holds the AI settings cards
        RecyclerView recyclerView_aiSettings = findViewById(R.id.recycler_view_aiSettings);
        AiRecyclerviewAdapter adapter = new AiRecyclerviewAdapter(mList, source, this, MainActivity.this);

        // set the adapter and layout manager for the recycler view
        recyclerView_aiSettings.setAdapter(new AiRecyclerviewAdapter(mList, source, this, MainActivity.this));
        recyclerView_aiSettings.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));


        // Set up UI elements
        Switch switchToggleStream = (Switch) findViewById(R.id.switch_streamToggle);
        Button buttonPushAiTask = (Button) findViewById(R.id.button_pushAiTask);
        Button buttonPopAiTask = (Button) findViewById(R.id.button_popAiTask);
        TextView textNumOfAiTasks = (TextView) findViewById(R.id.text_numOfAiTasks);
      //  TextView textThroughput = (TextView) findViewById(R.id.textView_throughput);
       // TextView textGpuUtilization = (TextView) findViewById(R.id.textView_gpuUtilization);

        buttonPushAiTask.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // get num of ai tasks from textView
                int numAiTasks = Integer.parseInt(textNumOfAiTasks.getText().toString());
                // check for max limit
                if (numAiTasks < 20) {
                    numAiTasks++;
                    // stop stream
                    switchToggleStream.setChecked(false);
                    // update num of AI tasks
                    textNumOfAiTasks.setText(String.format("%d", numAiTasks));
                    mList.add(new AiItemsViewModel());
                    adapter.setMList(mList);
                    recyclerView_aiSettings.setAdapter(adapter);
                }




            }
        });

        buttonPopAiTask.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // get num of ai tasks from textView
                int numAiTasks = Integer.parseInt(textNumOfAiTasks.getText().toString());
                // check for max limit
                if (numAiTasks > 1) {
                    numAiTasks--;
                    // stop stream
                    switchToggleStream.setChecked(false);
                    // update num of AI tasks
                    textNumOfAiTasks.setText(String.format("%d", numAiTasks));
                    mList.remove(numAiTasks);
                    adapter.setMList(mList);
                    recyclerView_aiSettings.setAdapter(adapter);
                }
            }
        });

        switchToggleStream.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    /** Check for null classifiers.
                     *  This will not let you start the stream if any are found
                     */

//                    double  rohT=-0.06 ;
//                    double rohD=0.0001;
//                    double delta=66.92;

                   rsp_models.clear();
                    thr_models.clear();
//                    trisRe=LinkedHashMultimap.create();

                //    trisMeanDisk=( ArrayListMultimap.create());
//                    ListMultimap<Double, Double> trisMeanThr = ArrayListMultimap.create();
                    for (AiItemsViewModel taskView : mList) { // up to the count of AI models we add values to lists below for parameters and slopes and baseline-throughput
                        tasks.append(",").append(taskView.getModels().get(taskView.getCurrentModel()));
                        rsp_models.add( ArrayListMultimap.create()); // for each model we create a map of tris_thr that we had in MIR
                        tParamList.add( ArrayListMultimap.create());

                        thr_models.add(LinkedHashMultimap.create()); //LinkedHashMultimap guarantees to keep the insertion order for keys and values.... we want to have just this in order since the rest multimaps would
                        // be OK pnce we know what is the first key (tris) added for thr_model and we use it to remove the older data from other hashmaps even if they are not in order
                        // for each model we create a map of tris_thr that we had in MIR
                        thParamList.add( ArrayListMultimap.create()); //  ArrayListMultimap only guarantees to keep the insertion order for values within a key, not key order



                       // thParamList.add( ArrayListMultimap.create());
                       // tParamList.add( ArrayListMultimap.create());

                        rohDL.add(rohD);// the weight  for throughput modeling
                        rohTL.add(rohT);
                        deltaL.add(delta);
                        rsp_miss_counter.add(0);
                        thr_miss_counter.add(0);
                        baseline_AIthr.add(0d);
                        est_weights.add(1d);
                        baseline_est_weights.add(1d);// not used
                        //msr_weights.add(0d);
                        hAI_acc.add(false);
                    }




                    boolean noNullModelRunner = true;
                    for (int i = 0; i < mList.size(); i++) {
                        if (mList.get(i).getClassifier()==null && mList.get(i).getObjectDetector()==null
                                && mList.get(i).getNewclassifier()==null
                                && mList.get(i).getGestureClas()==null && mList.get(i).getSegm()==null
                        && mList.get(i).getImgSegmentation()==null
                        ) { // we have three different models now
                            noNullModelRunner = false;
                        }
                    }

                    // The toggle is enabled

                    if(noNullModelRunner) {
                        source.startStream();
                        for (int i = 0; i < mList.size(); i++) {
//                        Log.d("CHECKCHG", String.valueOf((mList.get(i).getClassifier()==null)));
//                            mList.get(i).getCollector().setEnd(System.nanoTime()/1000000);
                          //  if(mList.get(i).getRunCollection())
                            mList.get(i).getCollector().startCollect();
                        }
                    } else {
                   //     Toast toast = Toast.makeText(MainActivity.this, "Set all AI models & Devices before continuing", Toast.LENGTH_LONG);
                     //   toast.show();
                        switchToggleStream.setChecked(false);
                    }
                } else {
                    // The toggle is disabled

                    for (int i = 0; i < mList.size(); i++) {
//                        if (mList.get(i).getCollector() != null) {
                        mList.get(i).getCollector().pauseCollect();

//                        }
                    }
                    source.pauseStream();

                }
            }
        });


//        RecyclerView aiOptionsContainer = findViewById(R.id.recycler_view_aiSettings);
        Button toggleUi = (Button) findViewById(R.id.button_toggleUi);
        toggleUi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
//                getThroughput();
                if(recyclerView_aiSettings.getVisibility()==View.VISIBLE)
                    recyclerView_aiSettings.setVisibility(View.INVISIBLE);
                else {
                    recyclerView_aiSettings.setVisibility(View.VISIBLE);
                }
                toggleAiPushPop();
            }
        });
        //////////////////////////////////////////////////////////////



        // AR //


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView posText1 = (TextView) findViewById(R.id.objnum);
        posText1.setText("obj_num: " + 0);


        TextView posText2 = (TextView) findViewById(R.id.thr);
        posText2.setText("Throughput: " );

        //create the file to store user score data
        dateFile = new File(getExternalFilesDir(null),
                (new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss", java.util.Locale.getDefault()).format(new Date())) + ".txt");

       // Nil = new File(getExternalFilesDir(null), "Nil.txt");
      //  obj = new File(getExternalFilesDir(null), "obj.txt");
        //tris_num = new File(getExternalFilesDir(null), "tris_num.txt");
       // GPU_usage = new File(getExternalFilesDir(null), "GPU_usage.txt");
//        //user score setup
//        Spinner ratingSpinner = (Spinner) findViewById(R.id.userScoreSpinner);
//        ratingSpinner.setOnItemSelectedListener(this);
//        ArrayAdapter<String> ratingAdapter = new ArrayAdapter<String>(com.arcore.MixedAIAR.MainActivity.this,
//                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.user_score));
//        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        ratingSpinner.setAdapter(ratingAdapter);


//Nil need to read from file/ num tris and write to
//

        try {
            InputStream iS = getResources().getAssets().open("tris.txt");
            //BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            InputStreamReader inputreader = new InputStreamReader(iS);
            BufferedReader reader = new BufferedReader(inputreader);
            String line, line1 = "";

            while ((line = reader.readLine()) != null) {
                mLines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



        givenUsingTimer_whenSchedulingTaskOnce_thenCorrect();
        //new filewrite(MainActivity.this).run();

        StringBuilder sb = new StringBuilder();

        //Nil
        int ind = 0;
        while (ind < mLines.size()) {
            sb.append(mLines.get(ind) + "\n ,");
            ind++;
        }


//        String currentFolder = getExternalFilesDir(null).getAbsolutePath();
//        String FILEPATH = currentFolder + File.separator + "CPU_Mem_"+ fileseries+".csv";
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {
//
//            StringBuilder sbb = new StringBuilder();
//
//           sbb.append( "time,7m,PID,USER,PR,NI,VIRT,[RES],SHR,S,%CPU,%MEM,TIME,ARGS");
//
//
//            sbb.append('\n');
//            writer.write(sbb.toString());
//
//            System.out.println("done!");
//
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }


        String currentFolder = getExternalFilesDir(null).getAbsolutePath();
        String  FILEPATH = currentFolder + File.separator + "GPU_Usage_"+ fileseries+".csv";
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {

            StringBuilder sbb = new StringBuilder();
            sbb.append("time2");
            sbb.append(',');
            sbb.append("tris");
            sbb.append(',');
            sbb.append("gpu");
            sbb.append(',');
            sbb.append("distance"); //sbb.append(',');  sbb.append("serv_req");
            sbb.append(',');
            sbb.append("lastobj");
            sbb.append(',');
            sbb.append("objectCount,");
            sbb.append( "7m,PID,USER,PR,NI,VIRT,[RES],SHR,S,%CPU,%MEM,TIME,ARGS");
            sbb.append(',');
            sbb.append("cpu_freq,");
            sbb.append('\n');
            writer.write(sbb.toString());

            System.out.println("done!");

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }


         currentFolder = getExternalFilesDir(null).getAbsolutePath();
         FILEPATH = currentFolder + File.separator +"Throughput"+ fileseries+".csv";


        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {
//date, thr, tris, model, device, thread
            StringBuilder sbb = new StringBuilder();
            sbb.append("time");
            sbb.append(',').append("AI#,");
            sbb.append("Throughput_real");
            sbb.append(',').append("Throughput_pred");
            sbb.append(',').append("trained_flag,").append("Thr_accuracy,");
            sbb.append("rohT").append(',').append("rohD").append(',').append("delta").append(',');
            sbb.append("desiredH").append(',').append("desiredQ").append(',');

            sbb.append("Tris");
            sbb.append(',');
            sbb.append("Model1");
            sbb.append(',');
            sbb.append("Device1");
//            sbb.append(',');
//            sbb.append("Thread1");
            sbb.append(',');
            sbb.append("Inf1");
            sbb.append(',');
            sbb.append("Overhead1");
          //  sbb.append(',').append("Msr_W1").append(',').append("Est-W1");
         //   sbb.append(',').append("rohT").append(',').append("rohD").append(',').append("delta");
//         old for coefficient
//            sbb.append(',');sbb.append("r1");

            sbb.append(',').append("Model2").append(',').append("Device2");
            sbb.append(',').append("Inf2").append(',').append("Overhead2");
          //  sbb.append(',').append("Msr_W2").append(',').append("Est-W2");

            sbb.append(',').append("Model3").append(',').append("Device3");
            sbb.append(',').append("Inf3").append(',').append("Overhead3");
          //  sbb.append(',').append("Msr_W3").append(',').append("Est-W3");

            sbb.append(',').append("Model4").append(',').append("Device4");
            sbb.append(',').append("Inf4").append(',').append("Overhead4");
           // sbb.append(',').append("Msr_W4").append(',').append("Est-W4");

//            sbb.append(',').append("Model5").append(',').append("Device5").append(',').append("Thread5").append(',').append("Task_Throughput5");
//            sbb.append(',').append("Inf5").append(',').append("Overhead5").append(',').append("r5");
//
//            sbb.append(',').append("Model6").append(',').append("Device6").append(',').append("Thread6").append(',').append("Task_Throughput6");
//            sbb.append(',').append("Inf6").append(',').append("Overhead6").append(',').append("r6");
//
//            sbb.append(',').append("Model7").append(',').append("Device7").append(',').append("Thread7").append(',').append("Task_Throughput7");
//            sbb.append(',').append("Inf7").append(',').append("Overhead7").append(',').append("r7");
            sbb.append('\n');
            writer.write(sbb.toString());
            System.out.println("done!");

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }



        currentFolder = getExternalFilesDir(null).getAbsolutePath();
        FILEPATH = currentFolder + File.separator + "RE"+ fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {

            StringBuilder sbb = new StringBuilder();
            sbb.append("time");
            sbb.append(',');
            sbb.append("re_Real");
            sbb.append(',');
            sbb.append("re_pred");
            sbb.append(',');
            sbb.append("trainedRe");
            sbb.append(',');
            sbb.append("curTris");
            sbb.append(',');
            sbb.append("nextTris");
            sbb.append(',');
            sbb.append("Algorithm_Tris");
            sbb.append(',');
            sbb.append("Recalculated Tris");
            sbb.append(',');
            sbb.append("pAR");
            sbb.append(',');
            sbb.append("pAI");
            sbb.append(',');
            sbb.append("TwoModels_Accuracy");
            sbb.append(',');
            sbb.append("tot_tris");
            sbb.append(',');
            sbb.append("Average_Quality");
            sbb.append(',');
            sbb.append("Algorithm_Duration");
            sbb.append('\n');
            writer.write(sbb.toString());
            System.out.println("done!");

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }


        currentFolder = getExternalFilesDir(null).getAbsolutePath();
        FILEPATH = currentFolder + File.separator + "Weights"+ fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {

            StringBuilder sbb = new StringBuilder();
            sbb.append("time,Thr_accuracy,AI_name,").append("Msr_W,").append("Est_W,");

            sbb.append('\n');
            writer.write(sbb.toString());
            System.out.println("done!");

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }


        currentFolder = getExternalFilesDir(null).getAbsolutePath();
        FILEPATH = currentFolder + File.separator + "Weighted_throughput"+ fileseries+".csv";

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {

            StringBuilder sbb = new StringBuilder();
            sbb.append("time,AVG_msr_thr,AVG_pred_thr,percentage_error,Weighted_msr_thr,Weighted_pred_thr,").append("All_AI_accuracy,");

            sbb.append('\n');
            writer.write(sbb.toString());
            System.out.println("done!");

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }




//        currentFolder = getExternalFilesDir(null).getAbsolutePath();
//        FILEPATH = currentFolder + File.separator + "Quality"+ fileseries+".csv";
//
//        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {
//
//            StringBuilder sbb = new StringBuilder();
//            sbb.append("time");
//            sbb.append(',');
//            sbb.append("objectname");
//            sbb.append(',');
//            sbb.append("sensitivity");
//            sbb.append(',');
//            sbb.append("decimation_ratio");
//            sbb.append(',');
//            sbb.append("quality");
//
//            sbb.append('\n');
//            writer.write(sbb.toString());
//            System.out.println("done!");
//
//        } catch (FileNotFoundException e) {
//            System.out.println(e.getMessage());
//        }





        try {

            InputStream inputStream = getResources().getAssets().open("degmodel_file.csv");

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

            BufferedReader br = new BufferedReader(inputStreamReader);
            String line = "";
            line = br.readLine();
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] cols = line.split(",");
                excel_alpha.add(Float.parseFloat(cols[0]));
                excel_betta.add(Float.parseFloat(cols[1]));
                excel_c.add(Float.parseFloat(cols[2]));
                excel_gamma.add(Float.parseFloat(cols[3]));
                excel_maxd.add(Float.parseFloat(cols[4]));
                excel_tris.add(Integer.parseInt(cols[5]));
                excel_mindis.add(Float.parseFloat(cols[7]));
                excel_filesize.add(Float.parseFloat(cols[8]));
                excelname.add((String) (cols[6]));
                //.substring(2, cols[6].length() - 2));

                max_d.add(max_d_parameter * Float.parseFloat(cols[4]));


            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


// prediction codes Requirements
        for (int i = 0; i < maxtime; i++) {
            prmap.put(i, new ArrayList<ArrayList<Float>>());
            marginmap.put(i, new ArrayList<ArrayList<Float>>());
            errormap.put(i, new ArrayList<ArrayList<Float>>());
            booleanmap.put(i, new ArrayList<ArrayList<Float>>());

        }

        //for(int i=0;i < max_datapoint;i++)
        //  last_errors.add(i, new LinkedList<>());


        for (int i = 0; i < maxtime / 2; i++) {
            nextfivesec.add(new ArrayList<Float>());

            nextfive_fourcenters.put(i, new ArrayList<>());
        }

        for (int i = 0; i < maxtime; i++) {

            marginmap.get(i).add(new ArrayList<Float>(Arrays.asList(0.3f, 0.3f)));

            errormap.get(i).add(new ArrayList<Float>(Arrays.asList(0f, 0f)));

        }

        //Nil


        //get the asset list for model select
        try {
            //get list of .sfb's from assets
            //assetList = getAssets().list("models");
            assetList = getAssets().list("models");
            //take off .sfb from every string for use with server communication
            for (int i = 0; i < assetList.length; i++) {
                assetList[i] = assetList[i].substring(0, assetList[i].length() - 4);
            }
            //Log.d("AssetList", Arrays.toString(assetList));
        } catch (IOException e) {
            Log.e("AssetReading", e.getMessage());
        }

        // set up scenario and asset list
        try {
            String curFolder = getExternalFilesDir(null).getAbsolutePath();

            File saveDir = new File(curFolder + File.separator + "saved_scenarios_configs");
            saveDir.mkdirs();
            String[] saves = saveDir.list();

            for (int i = 0; i <= saves.length; ++i) {
                String[] files = new File(curFolder + File.separator + "saved_scenarios_configs" + File.separator + saves[i]).list();
                for (int j = 0; j < 2; ++j) {



                    if (files[j].contains("scenario")) {

                        String number= files[j].split("scenario")[1].split(".csv")[0];
                        scenarioList.add(number + File.separator + files[j]);// it is like 2/scenario2.csv

                    }
                    else if (files[j].contains("config")) {

                        String number= (files[j].split("config")[1]).split(".csv")[0]; // gets the number for the config
                        taskConfigList.add(number + File.separator + files[j]);} // it is like 1/config1.csv
                }

            }
        } catch (Exception e) {
            Log.e("ScenarioReading", e.getMessage());
        }

        //setup the model drop down menu
        Spinner modelSpinner = (Spinner) findViewById(R.id.modelSelect);
        modelSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> modelSelectAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, assetList);
        modelSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelSelectAdapter);

        //setup the model drop down for desired Q and throughout selection
//       Spinner qSpinner = (Spinner) findViewById(R.id.alg);
//        qSpinner.setOnItemSelectedListener(this);
//      ArrayAdapter<Double> qSelectAdapter = new ArrayAdapter<Double>(MainActivity.this,
//               android.R.layout.simple_list_item_1, desiredQ);
//        qSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        qSpinner.setAdapter(qSelectAdapter);
//
//
//        qSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                                              @Override
//           public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
//                                                  // your code here
//               //des_Q= Double.valueOf( qSpinner.getSelectedItem().toString());
//                 odraAlg=( qSpinner.getSelectedItem().toString());// yes or no
//
//                                              }
//            @Override
//            public void onNothingSelected(AdapterView<?> parentView) {
//                // your code here
//            }
//        });


        Spinner qSpinner = (Spinner) findViewById(R.id.alg);
        qSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> qSelectAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, desiredalg);
        qSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qSpinner.setAdapter(qSelectAdapter);


        qSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                //des_Q= Double.valueOf( qSpinner.getSelectedItem().toString());
                odraAlg=( qSpinner.getSelectedItem().toString());// yes or no

            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });


        //setup the model drop down for desired  throughout selection
        Spinner thSpinner = (Spinner) findViewById(R.id.thr_w);
        thSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<Double> thSelectAdapter = new ArrayAdapter<Double>(MainActivity.this,
                android.R.layout.simple_list_item_1, desiredThr_weight);
        thSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        thSpinner.setAdapter(thSelectAdapter);

/*
        thSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // this is for canceled experiment- no longer needed
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                if(switchToggleStream.isChecked()) {
                    //double throughput = getThroughput();
                    double weight = Double.valueOf(thSpinner.getSelectedItem().toString());
               //     if (throughput < 80 && throughput > 10)
                        des_Thr = (double) (Math.round((double) (weight * des_Thr * 1000))) / 1000;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });*/




//            for (int a = 0; a < temp_ww; a++) {
//            W_Selection[a] = a + 1;
//        }
        //setup the model drop down for object count selection
//        Spinner WSpinner = (Spinner) findViewById(R.id.WSelect);
//        WSpinner.setOnItemSelectedListener(this);
//        ArrayAdapter<Integer> WSelectAdapter = new ArrayAdapter<Integer>(MainActivity.this, android.R.layout.simple_list_item_1, W_Selection);
//        //  ArrayAdapter WSelectAdapter1 = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1, Collections.singletonList(W_Selection));
//        WSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        WSpinner.setAdapter(WSelectAdapter);
//
//        final int[] ww = {(int) WSpinner.getSelectedItem()};


        //setup the model drop down for object count selection
//        Spinner BWSpinner = (Spinner) findViewById(R.id.Bwidth);
//        BWSpinner.setOnItemSelectedListener(this);
//        ArrayAdapter<Integer> BWSelectAdapter = new ArrayAdapter<Integer>(MainActivity.this, android.R.layout.simple_list_item_1, BW_Selection);
//        //  ArrayAdapter WSelectAdapter1 = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1, Collections.singletonList(W_Selection));
//        BWSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        BWSpinner.setAdapter(BWSelectAdapter);


        //setup the model drop down for object count selection
//        Spinner policySpinner = (Spinner) findViewById(R.id.policy);
//        policySpinner.setOnItemSelectedListener(this);
//        ArrayAdapter<String> policySelectAdapter = new ArrayAdapter<String>(MainActivity.this,
//                android.R.layout.simple_list_item_1, Policy_Selection);
//        policySelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        policySpinner.setAdapter(policySelectAdapter);

//        policy = policySpinner.getSelectedItem().toString();

        Spinner scenarioSpinner = (Spinner) findViewById(R.id.scenario);
        scenarioSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> scenarioSelectAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, scenarioList);
        scenarioSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scenarioSpinner.setAdapter(scenarioSelectAdapter);

        Spinner taskConfigSpinner = (Spinner) findViewById(R.id.taskConfig);
        taskConfigSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> taskConfigSelectAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, taskConfigList);
        taskConfigSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskConfigSpinner.setAdapter(taskConfigSelectAdapter);

        //decimate all obj at the same time
//        Switch referenceObjectSwitch = (Switch) findViewById(R.id.refSwitch);
//        referenceObjectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//
//                if (b) {
//                    decAll = true;
//                } else {
//                    decAll = false;
//                }
//            }
//        });

// for prediction
//        Switch multipleSwitch = (Switch) findViewById(R.id.refSwitch4);
//        multipleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//
//                if (b) {
//                    autoPlace = true;
//
//                } else {
//                    autoPlace = false;
//                    // stopService(i);
//
//
//                }
//            }
//
//        });


//        Switch underpercSwitch = (Switch) findViewById(R.id.un_percSwitch3);
//        underpercSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (b) {
//                    under_Perc = true;
//                } else {
//                    under_Perc = false;
//                }
//            }
//        });



/*
        //create button listener for predict
        Button predictObjectButton = (Button) findViewById(R.id.predict);

        predictObjectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // int selectedCount = (int) countSpinner.getSelectedItem();

                // if (multipleSwitchCheck == true) {

                bwidth = (int) BWSpinner.getSelectedItem();
                for (int i = 0; i < objectCount; i++)
                    d1_prev.set(i, predicted_distances.get(i).get(0));

                //for eAR
                if (multipleSwitchCheck == true) {// nill feb -> multiple = false

                    Timer t = new Timer();
                    final int[] count = {0}; // should be before here
                    t.scheduleAtFixedRate(
                            new TimerTask() {
                                public void run() {

                                    if (objectCount == 0 || multipleSwitchCheck == false) {
                                        t.cancel();
                                        percReduction = 1;
                                    }
                                    ww[0] = (int) WSpinner.getSelectedItem();



                                    finalw = ww[0];
                                    int dindex = 0;// shows next time index
                                    float d1;

                                    for (int ind = 0; ind < objectCount; ind++) {

                                        new DecisionAlgorithm(MainActivity.this, ind, finalw, dindex).run();
                                        //  MainActivity.this.algoThreadPool.execute(new DecisionAlgorithm(MainActivity.this, ind, finalw, dindex));


                                    }

                                }
                            },
                            0,      // run first occurrence immediatetl
                            (long) (decision_p * 1000));


                } else if (referenceObjectSwitchCheck == true) { // this is for static eAR

                    Timer t = new Timer();
                    final int[] count = {0}; // should be before here
                    t.scheduleAtFixedRate(
                            new TimerTask() {
                                public void run() {

                                    if (objectCount == 0 || referenceObjectSwitchCheck == false) {
                                        t.cancel();
                                        percReduction = 1;
                                    }



                                    int dindex = 0;// shows next time index
                                    //   float  d1;

                                    for (int ind = 0; ind < objectCount; ind++) {

                                        //new Baseline2(MainActivity.this, ind, dindex).run();


                                        int finalInd = ind;
                                        //  float d1 = predicted_distances.get(finalInd).get(0);// gets the first time, next 1s of every object, ie. d1 of every obj

                                        float d1 = renderArray[finalInd].return_distance();

                                        int indq = excelname.indexOf(renderArray[finalInd].fileName);// search in excel file to find the name of current object and get access to the index of current object
                                        // excel file has all information for the degredation model
                                        float gamma = excel_gamma.get(indq);
                                        float a = excel_alpha.get(indq);
                                        float b = excel_betta.get(indq);
                                        float c = excel_c.get(indq);
                                        float q1 = 0.5f;
                                        float q2 = 0.8f;

                                        float deg_error1 = Calculate_deg_er(a, b, c, d1, gamma, q1);
                                        float deg_error2 = Calculate_deg_er(a, b, c, d1, gamma, q2);

                                        float curQ = 1;
                                        float cur_degerror = 0;
                                        float max_nrmd = excel_maxd.get(indq);

                                        float maxd = max_d.get(indq);
                                        if (deg_error1 < maxd) {
                                            curQ = q1;
                                            cur_degerror = deg_error1;
                                        } else if (deg_error2 < maxd) {
                                            curQ = q2;
                                            cur_degerror = deg_error2;
                                        }
                                        // update total tiri, deg log, quality log, time , and distance log, then redraw obj
                                         cur_degerror=cur_degerror / max_nrmd; // normalize it
                                        // float distance= renderArray[finalInd].return_distance();
                                        String last_dis = distance_log.get(finalInd);
                                        distance_log.set(finalInd, last_dis + "," + d1);
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                                        String last_time = time_log.get(finalInd);
                                        time_log.set(finalInd, last_time + "," + dateFormat.format(new Date()).toString());

                                        String lasterror = deg_error_log.get(finalInd);

                                        cur_degerror = (float) (Math.round((float) (cur_degerror * 10000))) / 10000;
                                        lastQuality.set(finalInd,1- cur_degerror);// normalized
                                        deg_error_log.set(finalInd, lasterror + Float.toString(cur_degerror) + ",");

                                        //'''upfdate everythong finally'''
                                        String lastq_log = quality_log.get(finalInd);
                                        quality_log.set(finalInd, lastq_log + curQ + ",");

                                        // update total_tris
                                        if ((curQ) != updateratio[finalInd]) {
                                            total_tris = total_tris - (updateratio[finalInd] * excel_tris.get(indq));// total =total -1*objtris

                                            total_tris = total_tris + (curQ * excel_tris.get(indq));// total = total + 0.8*objtris
                                            curTrisTime= SystemClock.uptimeMillis();

                                           //Camera2BasicFragment.getInstance().update((double) total_tris);// run linear reg

                                            percReduction = curQ;
                                            renderArray[ind].decimatedModelRequest(curQ, ind, referenceObjectSwitchCheck);
                                            //  renderArray[finalInd].redraw(  finalInd ); // you should have 0.8 and 0.5 for all objects

                                        }

                                        updateratio[finalInd] = curQ;


                                    }

                                }
                            },
                            0,      // run first occurrence immediatetl
                            (long) (decision_p * 1000));


                } // end of baseline2


            }// on click
        });


*/

        //create button listener for object placer
        Button placeObjectButton = (Button) findViewById(R.id.placeObjButton);

        placeObjectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {



                    float original_tris=excel_tris.get(excelname.indexOf(currentModel));
                    renderArray.add(objectCount,new decimatedRenderable(modelSpinner.getSelectedItem().toString(),original_tris));



                addObject(Uri.parse("models/" + currentModel + ".sfb"), renderArray.get(objectCount));
                   

//nill temporary oct 24
//                    renderArray[objectCount] = new decimatedRenderable(modelSpinner.getSelectedItem().toString());
//                    addObject(Uri.parse("models/" + currentModel + ".sfb"), renderArray[objectCount]);

                //}



            }




        });


// this is for PAR-AI experiment: the effect of decimation on performance of AI, AI and RE , we add objects fast and then decimate them by 10% every 30 sec- figure 4 in paper
        Button Auto_decimate_butt = (Button) findViewById(R.id.autoD);
        Auto_decimate_butt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                TextView posText = (TextView) findViewById(R.id.dec_req);

                Toast toast = Toast.makeText(MainActivity.this,
                        "Please Upload the decimated objects to the Phone storage", Toast.LENGTH_LONG);

                toast.show();


                int repeat = 6; // num of decimation loop, if it is one, just gathers the data of the first  iteration (original objects on the screen)
                final int[] start = {1};
                final float[] ratio = {80};
                countDownTimer = new CountDownTimer(Long.MAX_VALUE, 30000) {

                    // This is called after every 80 sec interval.
                    public void onTick(long millisUntilFinished) {

                        if (start[0] == repeat) {
                            //AI_tasks+=1;
                            //  posText.setText( String.valueOf(AI_tasks));

                            countDownTimer.cancel();
                            //onPause();

                        }


                        if (start[0] < repeat) {

                           // if (start[0] != 0) {  /// at first we delay the auto decimation for 90 seconds to gather data of all objects in
                                //screen.  start[0] is for the original objects data collection delay

                                for (int i = 0; i < objectCount; i++) {

                                    //decimate all when referenceObjectSwitchCheck= True


                                    //if (under_Perc == false) {
                                    //(o_tris.get(i)/1000) is to have a better est_weights for throughput modeling

                                        total_tris = total_tris - (ratioArray.get(i) * ((double)(o_tris.get(i))/tris_factor)     );// total =total -1*objtris
                                       // ratioArray[i] = ratio[0] / 100f;
                                        ratioArray.set(i, ratio[0] / 100f);
                                        renderArray.get(i).decimatedModelRequest(ratio[0] / 100f, i, false);
                                        posText.setText("Request for " + renderArray.get(i).fileName + " " + ratio[0] / 100f);


                                        // update total_tris
                                        total_tris = total_tris + (ratioArray.get(i) * ((double)(o_tris.get(i))/tris_factor) );// total = total + 0.8*objtris
                                    totTrisList.add(total_tris);

                                    //    trisDec.put(total_tris,true);
                                    if (!decTris.contains(total_tris)) {
                                        decTris.add(  total_tris);
                                    }

                                        curTrisTime= SystemClock.uptimeMillis();
                                        // quality is registered

                                   // }



                                    int finalInd = i;
                                    int indq = excelname.indexOf(renderArray.get(finalInd).fileName);// search in excel file to find the name of current object and get access to the index of current object
//nill added
                                    float d1 = renderArray.get(finalInd).return_distance();
                                    // excel file has all information for the degredation model
                                    float cur_degerror=calculatenrmDeg(indq, finalInd, ratio[0],d1);// normalized deg error
                                    String lasterror = deg_error_log.get(finalInd);
                                    float curQ = ratio[0] / 100f;
                                    //lastQuality.set(finalInd,1-cur_degerror );

                                    deg_error_log.set(finalInd, lasterror + Float.toString(cur_degerror) + ",");
                                    String lastq_log = quality_log.get(finalInd);
                                    quality_log.set(finalInd, lastq_log + curQ + ",");

                                    String last_dis = distance_log.get(finalInd);
                                    distance_log.set(finalInd, last_dis + "," + d1);
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                                    String last_time = time_log.get(finalInd);
                                    time_log.set(finalInd, last_time + "," + dateFormat.format(new Date()).toString());


                                }///for
                            if(ratio[0]== 20)
                                ratio[0]=5; // last ratio
                            else
                                ratio[0] -= 20; // 80-> 60-> 40-> 20 ->

                          //  }// if start0 != 0 // start: 1-> 2 -> 3 -> 4-> 5


                            start[0] += 1;
                        }


                    }

                    public void onFinish() {
                        if (start[0] == repeat) {
                            countDownTimer.cancel();
                            //onPause();

                        }


                    }
                }.start();

                // countDownTimer.start();

            }
        });



        //Remove one object button setup
        Button removeButton = (Button) findViewById(R.id.removeButton);
        removeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                for (int i = 0; i < objectCount; i++) {

                    if (renderArray.get(i).baseAnchor.isSelected()) {




                        total_tris = total_tris - (ratioArray.get(i) * ( ((double)o_tris.get(i))/tris_factor));// total =total -1*objtris
                        orgTrisAllobj -= (ratioArray.get(i) * (((double)o_tris.get(i))/tris_factor));
                        objectCount -= 1;
                        TextView posText = (TextView) findViewById(R.id.objnum);
                        posText.setText("obj_num: " + objectCount);
                        o_tris.remove(i);
                        // till here update : there is a prob : we have all data as list and renderobj is array-> ned to be stores in a list instead
                        // if we remove an object and dec index, we need to also remove obj from all lists while array couldn't be shifted easily

                        // cache array - ratio array - updated ntw - render array -> all to be a list
                        renderArray.get(i).detach();
                        ratioArray.remove(i);
                        cacheArray.remove(i);
                        updatednetw.remove(i);
                        closer.remove(i);
                        prevquality.remove(i);
                        best_cur_eb.remove(i);
                        gpusaving.remove(i);
                        eng_dec.remove(i);
                        quality_log.remove(i);
                        time_log.remove(i);
                        distance_log.remove(i);
                        deg_error_log.remove(i);
                        obj_quality.remove(i);
                        Server_reg_Freq.remove(i);
                        //  decimate_thread.remove(i);
                        renderArray.remove(i);
                        trisChanged=true;


                    }// if the item is selected
                }




            }
        });





        //Clear all objects button setup
        Button clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                for (int i = 0; i < objectCount; i++) {

                    renderArray.get(i).detach();

                }

                //  removePreviousAnchors(); // from net wrong
                ModelRequestManager.getInstance().clear();
                totTrisList.clear();
                predicted_distances.clear();
                quality_log.clear();
                orgTrisAllobj=0;
                objectCount = 0;
                nextID = 1;
                TextView posText = (TextView) findViewById(R.id.objnum);
                posText.setText("obj_num: " + objectCount);



                total_tris = 0;
                ratioArray.clear();
                cacheArray.clear();
                updatednetw.clear();
                o_tris.clear();


                closer.clear();
                prevquality.clear();
                best_cur_eb.clear();
                gpusaving.clear();
                eng_dec.clear();


                quality_log.clear();
                time_log.clear();
                distance_log.clear();

                deg_error_log.clear();
                obj_quality.clear();
                //  lastQuality.clear();
                Server_reg_Freq.clear();

                decimate_thread.clear();
                renderArray .clear();
            }
        });

        Button gc = (Button) findViewById(R.id.gc);
        gc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                reParamList.clear();
                trisMeanDisk.clear();
                trisMeanThr.clear();
                tParamList.clear();
                thParamList.clear();
                trisRe.clear();

                // to start over data collection

                decTris.clear();
            }
            });












        Button autoPlacementButton = (Button) findViewById(R.id.autoPlacement);// load button
        autoPlacementButton.setOnClickListener(view -> {
            runOnUiThread(clearButton::callOnClick);
            mList.clear();

            new Thread(() -> {
                try {
//
                        String curFolder = getExternalFilesDir(null).getAbsolutePath();

                        String taskFilepath = curFolder + File.separator + "saved_scenarios_configs" + File.separator + "save" + currentTaskConfig;
                        InputStreamReader taskInputStreamReader = new InputStreamReader(new BufferedInputStream(new FileInputStream(taskFilepath)));

                    BufferedReader taskBr = new BufferedReader(taskInputStreamReader);
                    taskBr.readLine();  // column names

                  //  final List<Float>[] sortedlist = new List<Float>[1];
                        String sceneFilepath = curFolder + File.separator + "saved_scenarios_configs" + File.separator + "save" + currentScenario;
                        InputStreamReader sceneInputStreamReader = new InputStreamReader(new BufferedInputStream(new FileInputStream(sceneFilepath)));

                    BufferedReader sceneBr = new BufferedReader(sceneInputStreamReader);
                    sceneBr.readLine();  // column names
                    tasks = new StringBuilder();
                    runOnUiThread(() -> {
                        final int[] i = {0};
                        CountDownTimer taskTimer, sceneTimer, removeTimer; // this is to remove objects one by one
                        removeTimer = new CountDownTimer(Long.MAX_VALUE, scenarioTickLength) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                if (objectCount == 0) {
                                    this.cancel();
                                  //switch off is for motivation- exp2
                                    switchToggleStream.setChecked(false);
                                   // runOnUiThread(() -> Toast.makeText(MainActivity.this, "You can pause and save collected data now", Toast.LENGTH_LONG).show());
                                    runOnUiThread(clearButton::callOnClick);
                                    return;
                                }
                               // removePhase=true;


                                // last element in the sorted list would be maximum
                             //int index=   sortedlist[0].get(sortedlist[0].size() - 1);

                                String name = renderArray.get(objectCount-1).fileName;
                                renderArray.get(objectCount-1).baseAnchor.select();
                                runOnUiThread(removeButton::callOnClick);

                               // runOnUiThread(Toast.makeText(MainActivity.this, "Removed " + name, Toast.LENGTH_LONG)::show);
                            }



                            @Override
                            public void onFinish() {
                            }
                        };

                        sceneTimer = new CountDownTimer(Long.MAX_VALUE, scenarioTickLength) { // this is to automate adding objects to the screen
                            @Override
                            public void onTick(long millisUntilFinished) {
                                // tick per 1 second, reading new line each time
                                try {
                                    String record = sceneBr.readLine();
                                    if (record == null) {


                                        // just for detailed exp not for exp 4_1
                                        /*
                                        reParamList.clear();
                                        trisMeanDisk.clear();
                                        trisMeanThr.clear();
                                        tParamList.clear();
                                        trisRe.clear();
                                        reParamList.clear();
                                        // to start over data collection

                                        decTris.clear();*/

                                        this.cancel();




                                        //commented for motv-exp 1 and desing PAR-PAI experiment: commented switchToggleStream.setChecked(false);
                                   //   removeTimer.start();

                                        return;
                                    }

                                    String[] cols = record.split(",");
                                    currentModel = cols[0];
                                    float xOffset = Float.parseFloat(cols[1]);
                                    float yOffset = Float.parseFloat(cols[2]);


                                  //  policy = policySpinner.getSelectedItem().toString();

                                    //modelSpinner.setSelection(modelSelectAdapter.getPosition(currentModel));
                                    float original_tris = excel_tris.get(excelname.indexOf(currentModel));
                                    renderArray.add(objectCount, new decimatedRenderable(currentModel, original_tris));
                                   // commented temp sep



                                     addObject(Uri.parse("models/" + currentModel + ".sfb"), renderArray.get(objectCount), xOffset, yOffset);//





                                    // Toast.makeText(MainActivity.this, String.format("Model: %s\nPos: (%f, %f)", currentModel, xOffset, yOffset), Toast.LENGTH_LONG).show();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFinish() {
                            }
                        };
                        final boolean[] startObject = {false};
                        taskTimer = new CountDownTimer(Long.MAX_VALUE, taskConfigTickLength) {
                            @Override
                            public void onTick(long millisUntilFinished) {

                                /// This is when we turned on the AI tasks and waited for 50s. Now we start the object placement
                                if(startObject[0] ==true){



                                    this.cancel();
                                    sceneTimer.start();
                                    return;

                                }

                                try {
                                    String record = taskBr.readLine();
                                    // this is to run all selected AI tasks -> after this we need to waite for 50 sec and them start the object placement
                                    while (record != null) {

                                        if (record!=null && switchToggleStream.isChecked())// this is to restart the previous AI tasks
                                            switchToggleStream.setChecked(false);

                                        String[] cols = record.split(",");
                                        int numThreads = Integer.parseInt(cols[0]);
                                        String aiModel = cols[1];
                                        String device = cols[2];

                                        AiItemsViewModel taskView = new AiItemsViewModel();
                                        mList.add(taskView);
                                        adapter.setMList(mList);
                                        recyclerView_aiSettings.setAdapter(adapter);
                                        adapter.updateActiveModel(
                                                taskView.getModels().indexOf(aiModel),
                                                taskView.getDevices().indexOf(device),
                                                numThreads,
                                                taskView,
                                                i[0]
                                        );

                                        i[0]++;
                                        textNumOfAiTasks.setText(String.format("%d", i[0]));
//
                                      //  Toast.makeText(MainActivity.this, String.format("New AI Task %s %s %d", taskView.getClassifier().getModelName(), taskView.getClassifier().getDevice(), taskView.getClassifier().getNumThreads()), Toast.LENGTH_SHORT).show();

                                        record = taskBr.readLine();

                                    }

                                    if (record == null) {// this is to immidiately start the AI tasks
                                   //     Toast.makeText(MainActivity.this, "All AI task info has been applied", Toast.LENGTH_LONG).show();
                                        switchToggleStream.setChecked(true);
                                        startObject[0] =true; // to make sure if we have ML tasks running
//                                        for (AiItemsViewModel taskView : mList) {
//                                            tasks.append(",").append(taskView.getModels().get(taskView.getCurrentModel()));}

                                    }


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFinish() {
                            }
                        }.start();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                  //  runOnUiThread(() -> Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });

        Button savePlacementButton = (Button) findViewById(R.id.savePlacement);
        savePlacementButton.setOnClickListener(view -> {
            String curFolder = getExternalFilesDir(null).getAbsolutePath();
            int numSaved = new File(curFolder + File.separator + "saved_scenarios_configs").list().length;
            String saveDir = curFolder + File.separator + "saved_scenarios_configs" + File.separator + "save" + (numSaved+1);
            new File(saveDir).mkdirs();

            String sceneFilepath = saveDir + File.separator + "scenario" + (numSaved+1) + ".csv";
            try (PrintWriter scenePrintWriter = new PrintWriter(new FileOutputStream(sceneFilepath, false))) {
                StringBuilder sbSceneSave = new StringBuilder();

                // column names
                sbSceneSave.append("model")
                    .append(",").append("xOffset")
                    .append(",").append("yOffset")
                    .append("\n");

                android.graphics.Point center = getScreenCenter();
                for (int i = 0; i < objectCount; ++i) {
                    sbSceneSave.append(renderArray.get(i).fileName)
                        .append(",").append(fragment.getArSceneView().getScene().getCamera().worldToScreenPoint(renderArray.get(i).baseAnchor.getWorldPosition()).x - center.x)
                        .append(",").append(fragment.getArSceneView().getScene().getCamera().worldToScreenPoint(renderArray.get(i).baseAnchor.getWorldPosition()).y - center.y)
                        .append("\n");
                }
                scenePrintWriter.write(sbSceneSave.toString());
                scenarioSelectAdapter.add((numSaved+1) + File.separator + "scenario" + (numSaved+1) + ".csv");

                Toast.makeText(MainActivity.this, String.format("Saved %d model placement(s)", objectCount), Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
            }

            String taskFilepath = saveDir + File.separator + "config" + (numSaved+1) + ".csv";
            try (PrintWriter taskPrintWriter = new PrintWriter(new FileOutputStream(taskFilepath))) {
                StringBuilder sbTaskSave = new StringBuilder();

                // column names
                sbTaskSave.append("threads")
                        .append(",").append("aimodel")
                        .append(",").append("device")
                        .append("\n");

                for (AiItemsViewModel taskView : mList) {
                    sbTaskSave.append(taskView.getCurrentNumThreads())
                            .append(",").append(taskView.getModels().get(taskView.getCurrentModel()))
                            .append(",").append(taskView.getDevices().get(taskView.getCurrentDevice()))
                            .append("\n");

                    tasks.append(",").append(taskView.getModels().get(taskView.getCurrentModel()));
                }
                taskPrintWriter.write(sbTaskSave.toString());
                taskConfigSelectAdapter.add((numSaved+1) + File.separator + "config" + (numSaved+1) + ".csv");

                Toast.makeText(MainActivity.this, String.format("Saved %d AI task config(s)", mList.size()), Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
            }
        });

        //seekbar setup
        SeekBar simpleBar = (SeekBar) findViewById(R.id.simpleBar);
        simpleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {

                progress = ((int) Math.round(progress / SEEKBAR_INCREMENT)) * SEEKBAR_INCREMENT;
                seekBar.setProgress(progress);
                TextView simpleBarText = (TextView) findViewById(R.id.simpleBarText);
                simpleBarText.setText(progress + "%");
                int val = (progress * (seekBar.getWidth() - 2 * seekBar.getThumbOffset())) / seekBar.getMax();
                simpleBarText.setX(seekBar.getX() + val + seekBar.getThumbOffset() / 2);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            //Nil
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { // we request for decimation in the app
                Log.d("ServerCommunication", "Tracking Stopped, redrawing...");
                //arFragment.getTransformationSystem().getSelectedNode()


                if (under_Perc == false)
                    percReduction = seekBar.getProgress() / 100f;
                else
                    percReduction = seekBar.getProgress() / 1000f;// for 1.1 % cases

                for (int i = 0; i < objectCount; i++) {


                    if (!renderArray.get(i).baseAnchor.isSelected() && decAll == false)
                    //means that we have s==0 and decAll==0
                    {
                        decAll = false;
                    } else {

                        {

                            float decRatio;

                            if (under_Perc == false) {
                                total_tris = total_tris - (ratioArray.get(i) * (((double)o_tris.get(i))/tris_factor) );// total =total -1*objtris
                                decRatio=seekBar.getProgress() / 100f;
                                ratioArray.set(i, seekBar.getProgress() / 100f);
                                renderArray.get(i).decimatedModelRequest(decRatio, i, decAll);


                                // update total_tris
                                total_tris = total_tris + (ratioArray.get(i) * (((double)o_tris.get(i))/tris_factor))  ;// total = total + 0.8*objtris
                          //      trisDec.put(total_tris,true);
                                if (!decTris.contains(total_tris))
                                   decTris.add(total_tris);
                                curTrisTime= SystemClock.uptimeMillis();
                                // quality is registered
                            } else {
                                total_tris = total_tris - (ratioArray.get(i)*(((double)o_tris.get(i))/tris_factor));// total =total -1*objtris
                                decRatio=seekBar.getProgress() / 1000f;
                                ratioArray.set(i, seekBar.getProgress() / 1000f);
                                renderArray.get(i).decimatedModelRequest(decRatio, i, decAll);

                                // update total_tris
                                total_tris = total_tris + (ratioArray.get(i) * (((double)o_tris.get(i))/tris_factor));// total = total + 0.8*objtris
                             //   trisDec.put(total_tris,true);
                                if (!decTris.contains(total_tris))
                                    decTris.add(total_tris);
                                curTrisTime= SystemClock.uptimeMillis();
                                // quality is registered
                            }

/*
                            float gamma = excel_gamma.get(i);
                            float a = excel_alpha.get(i);
                            float b = excel_betta.get(i);
                            float c = excel_c.get(i);
                            float d1 = renderArray[i].return_distance();

                            float deg_error =
                                    (float) (Math.round((float) (Calculate_deg_er(a, b, c, d1, gamma, decRatio) * 10000))) / 10000;
                            float max_nrmd = excel_maxd.get(i);
                            float cur_degerror=deg_error / max_nrmd;
                            lastQuality.set(i,1-cur_degerror );*/





                        }


                    }


                }///for
                // askedbefore=false;
            }
        });


        //initialized gallery is not used any more, but I didn't want to break anything so it's still here
        initializeGallery();
        fragment = (ArFragment)
                getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            onUpdate();

            //Nill did-> I don't need upadate rtracking called in on update

        });


        //prediction REQ
        Timer t = new Timer();

        t.scheduleAtFixedRate(
                new TimerTask() {

                    public void run() {


                        if(switchToggleStream.isChecked()) // in the begining we collect data for zero tris

                        {
                          // for exp4_baseline comparisons we fix the desired throughput
                           if(!setDesTh){
                               double throughput= getThroughput();
                               if(throughput <2000 && throughput>10)
                               { des_Thr=   (double) (Math.round((double) ( des_weight*throughput* 1000))) / 1000;
                               setDesTh=true;}
                           }

                            if(odraAlg=="1")// either choose the baseline or odra algorithm


                            {
/*
///nil test multimap to see insertion ordr is reserved or not
                                Multimap<Double, Double> hm = LinkedHashMultimap.create();

                                Multimap<Double, Double> hm2 =  ArrayListMultimap.create();
                                // putting elements
                                hm.put(0d, 1000d);
                                hm.put(0d, 1d);
                                hm.put(400d, 40d);
                                hm.put(200d, 20d);
                                hm.put(0d, 100d);
                                hm.put(200d, 2000d);

                                hm2.put(0d, 1000d);
                                hm2.put(0d, 1d);
                                hm2.put(400d, 40d);
                                hm2.put(200d, 20d);
                                hm2.put(0d, 100d);
                                hm2.put(200d, 2000d);

                                // printing LinkedHashMap

                                double firstK=Iterables.getFirst( hm.keys(), null);
                                double lastKey= Iterables.getLast( hm.keys(), null);// to get last key in hashmap
                                double secondVal=Iterables.get(hm.get(200d),1); // to get any index of value set of a key

                                List<Double> test= hm.get(1d).stream().collect(Collectors.toList());
                                double avg= hm.get(1d).stream().mapToDouble(a -> a).average().getAsDouble();
                                hm.removeAll(firstK);
                                hm.removeAll(lastKey);
                                for (Map.Entry<Double, Double> entry :
                                        hm.entries()) {
                                    System.out.println(entry.getKey() + " => "
                                            + ": " + entry.getValue());
                                }*/
                                /// nill test

                                new balancer(MainActivity.this).run(); // balancer has sqrt(rohT^2+ rohD^2) as wi


                            }

                            /* was  for MIR
                            else  if(odraAlg=="2")
                                    new baseline_thr(MainActivity.this).run(); // this is throughput wise baseline- periodically checks if throughput goes below the threshold it will decimate all the objects
                            else
                                new baseline(MainActivity.this).run(); // this is throughput wise baseline- periodically checks if throughput goes below the threshold it will decimate all the objects
*/
                        }

                    }

                },
                0,      // run first occurrence immediately
                2000);






    }



     double getThroughput(){
        Log.d("size", String.valueOf(mList.size()));
        double[] meanthr = new double[mList.size()];// up to the count of different AI models

         double[] meanoverheadT = new double[mList.size()];
         double[] meaninfT = new double[mList.size()];

        BitmapCollector tempCollector;
                //=new BitmapCollector(MainActivity.this);
        for(int i=0; i<mList.size(); i++) {
            tempCollector = mList.get(i).getCollector();
            tempCollector.setMInstance(MainActivity.this);
            int total = tempCollector.getNumOfTimesExecuted();
            if(total != 0) {
                double n=tempCollector.getNumOfTimesExecuted();
                double b=tempCollector.getTotalOverhead();
                meanthr[i]= (double)Math.round(  n*1000 *100/(double)tempCollector.getTotalResponseTime())/100;

                meanoverheadT[i]= (double) Math.round(b* 100/n ) / 100;
                        //tempCollector.getNumOfTimesExecuted();
                meaninfT[i]=(double)Math.round(tempCollector.getTotalInferenceTime()*100/n)/100;
                mList.get(i).getCollector().setNumOfTimesExecuted(0);
                mList.get(i).getCollector().setTotalResponseTime(0);
                mList.get(i).getCollector().setTotalInferenceTime(0);
                mList.get(i).getCollector().setTotalOverhead(0);
                mList.get(i).getCollector().setEnd(System.nanoTime()/1000000);
                mList.get(i).setThroughput(meanthr[i]);// update throughput of each model
                mList.get(i).setInferenceT(meaninfT[i]);// mean inference time of each model
                mList.get(i).setOverheadT(meanoverheadT[i]); // overhead of each  model
            }
        }
//        Log.d("rt", String.valueOf(meanResponseTimes[0]));
        double avg = Arrays.stream(meanthr).average().orElse(Double.NaN);

//        Log.d("Throughput", "rt: " +String.valueOf(avg) +" thrpt: " +String.valueOf((1/avg)*1000));
        return avg;
          //      (1/avg)*1000;
    }




    double getThroughput(int aiIndx){// returns average thr of each model

        double meanthr=0;// up to the count of different AI models

        double meanoverheadT ;
        double meaninfT ;

        BitmapCollector tempCollector;
        int i=aiIndx;
//        for(int i=0; i<mList.size(); i++) {
            tempCollector = mList.get(i).getCollector();
            tempCollector.setMInstance(MainActivity.this);
            int total = tempCollector.getNumOfTimesExecuted();
            if(total != 0) {
                double n=tempCollector.getNumOfTimesExecuted();
                double b=tempCollector.getTotalOverhead();
                meanthr= (double)Math.round(  n*1000 *100/(double)tempCollector.getTotalResponseTime())/100;

                meanoverheadT= (double) Math.round(b* 100/n ) / 100;

                meaninfT=(double)Math.round(tempCollector.getTotalInferenceTime()*100/n)/100;
                mList.get(i).getCollector().setNumOfTimesExecuted(0);
                mList.get(i).getCollector().setTotalResponseTime(0);
                mList.get(i).getCollector().setTotalInferenceTime(0);
                mList.get(i).getCollector().setTotalOverhead(0);
                mList.get(i).getCollector().setEnd(System.nanoTime()/1000000);
                mList.get(i).setThroughput(meanthr);// update throughput of each model
                mList.get(i).setInferenceT(meaninfT);// mean inference time of each model
                mList.get(i).setOverheadT(meanoverheadT); // overhead of each  model
            }


       return meanthr;

    }












// starting the second loop : note that sensitivity calculation is just to detect the candidate object for decimation/maintaining triangle count-> it is apart from actual decimation ratio calculation
   /*
    void odraAlg(float tUP) {

        candidate_obj = new HashMap<>();
        Map<Integer, Float> sortedcandidate_obj = new HashMap<>();
        float sum_org_tris = 0; // sum of all tris of the objects o the screen

        for (int ind = 0; ind < objectCount; ind++) {

            sum_org_tris += renderArray[ind].orig_tris;// this will ne used to cal min of tris needed at each row (object) in bellow


            float curtris = renderArray[ind].orig_tris * ratioArray[ind];
            float r1 = ratioArray[ind]; // current object decimation ratio
            float r2 = ref_ratio * r1; // wanna compare obj level of sensitivity to see if we decimate object more -> to (ref *curr) ratio, would the current object hurt more than the other ones?

            int indq = excelname.indexOf(renderArray[ind].fileName);// search in excel file to find the name of current object and get access to the index of current object
            // excel file has all information for the degredation model
            float gamma = excel_gamma.get(indq);
            float a = excel_alpha.get(indq);
            float b = excel_betta.get(indq);
            float c = excel_c.get(indq);
            float d_k = renderArray[ind].return_distance();// current distance

            float tmper1 = Calculate_deg_er(a, b, c, d_k, gamma, r1); // deg error for current sit
            float tmper2 = Calculate_deg_er(a, b, c, d_k, gamma, r2); // deg error for more decimated obj

            if (tmper2 < 0)
                tmper2 = 0;

            //Qi−Qi,r divided by Ti(1−Rr) = (1-er1) - (1-er2) / ....
            sensitivity[ind] = (abs(tmper2 - tmper1) / (curtris - (ref_ratio * curtris)));
            tris_share[ind] = (curtris / tUP);
            candidate_obj.put(ind, sensitivity[ind] / tris_share[ind]);


        }
        sortedcandidate_obj = sortByValue(candidate_obj, false); // second arg is for order-> ascending or not? NO
        // Up to here, the candidate objects are known


        float updated_sum_org_tris = sum_org_tris; // keeps the last value which is sum_org_tris - tris1-tris2-....
        for (int i : sortedcandidate_obj.keySet()) { // check this gets the candidate object index to calculate min weight
            float sum_org_tris_minus = updated_sum_org_tris - renderArray[i].orig_tris; // this is summ of tris for all the objects except the current one
            updated_sum_org_tris = sum_org_tris_minus;
            tMin[i] = coarse_Ratios[coarse_Ratios.length - 1] * sum_org_tris_minus;// minimum tris needs for object i+1 to object n
            ///@@@@ if this line works lonely, delete the extra line for the last object to zero in the alg
        }

        Map.Entry<Integer, Float> entry = sortedcandidate_obj.entrySet().iterator().next();
        int key = entry.getKey(); // get access to the first key -> to see if it is the first object for bellow code

        int prevInd = 0;
        for (int i : sortedcandidate_obj.keySet()){  // line 10 i here is equal to alphai -> the obj with largest candidacy
            // check this gets the candidate object index to maintain its quality
            for (int j = 0; j < coarse_Ratios.length; j++) {

                int indq = excelname.indexOf(renderArray[i].fileName);// search in excel file to find the name of current object and get access to the index of current object
                float gamma = excel_gamma.get(indq);
                float a = excel_alpha.get(indq);
                float b = excel_betta.get(indq);
                float c = excel_c.get(indq);
                float d_k = renderArray[i].return_distance();// current distance

                float quality = 1 - Calculate_deg_er(a, b, c, d_k, gamma, coarse_Ratios[j]); // deg error for current sit

                if (i == key && tUP >= renderArray[i].getOrg_tris() * coarse_Ratios[j]) { // the first object in the candidate list
                    fProfit[i][j] = quality;// Fα(i),j ←Qα(i),j -> i is alpha i
                    tRemainder[i][j] = tUP - (renderArray[i].getOrg_tris() * coarse_Ratios[j]);
                } else //  here is the dynamic programming section
                    for (int s = 0; s < coarse_Ratios.length; s++) {

                        float f = fProfit[prevInd][s] + quality;
                        float t = tRemainder[prevInd][s] - (renderArray[i].getOrg_tris() * coarse_Ratios[j]);
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
        float tmp=fProfit[lowPobjIndx][0];
        int j=0;
        for  (int maxindex=1;maxindex<coarse_Ratios.length;maxindex++) // line 27
            if(fProfit[lowPobjIndx][maxindex]>tmp)// finds the index of coarse-grain ratio with maximum profit
            {
                tmp = fProfit[lowPobjIndx][maxindex];
                j=maxindex;
            }


        for (int i : sortedcandidate_obj.keySet()) {

                total_tris = total_tris - (ratioArray[i] * o_tris.get(i));// total =total -1*objtris
                ratioArray[i] = coarse_Ratios[j];

            //
            //      commented on May 2 2022   ModelRequestManager.getInstance().add(new ModelRequest(cacheArray[id], fileName, percentageReduction, getApplicationContext(), MainActivity.this, id),redraw_direct );
//April 21 Nill , istead of calling mdelreq, sinc we have already downloaded objs from screen, we can call directly redraw

            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderArray[i].decimatedModelRequest(ratioArray[i], i, false);
                    }
                });
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

                total_tris = total_tris + (ratioArray[i] *  renderArray[i].orig_tris);// total = total + 0.8*objtris
                j = track_obj[i][j];

        }


    }*/


    private static Map<Integer, Float> sortByValue(Map<Integer, Float> unsortMap, final boolean order)
    {
        List<Entry<Integer, Float>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    //for user score selection
    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        switch (parent.getId()) {
            case R.id.modelSelect:
                currentModel = parent.getItemAtPosition(pos).toString();
                break;

            case R.id.scenario:
                currentScenario = parent.getItemAtPosition(pos).toString();
                break;

            case R.id.taskConfig:
                currentTaskConfig = parent.getItemAtPosition(pos).toString();
                break;
//            case R.id.userScoreSpinner:
//                for (int i = 0; i < objectCount; i++) {
//                    if (renderArray[i].baseAnchor.isSelected())
//                        // Nill april 21 added to avoid frame get
//                       // renderArray[i].print(parent, pos);
//                }
//                break;


//            case R.id.MDE:
//
//               // max_d_parameter= (float)(MDESpinner.getSelectedItem())/10;
//
//                  for (int i=0; i< max_d.size(); i++)
//                  {
//
//                      if(max_d_parameter== 0.2f  )
//                      { max_d.set(i, (max_d.get(i) * 0.6f)/0.2f );
//                          max_d_parameter= 0.6f;
//                      }
//                      else
//                      {max_d.set(i, (max_d.get(i) * 0.2f)/0.6f );
//                          max_d_parameter= 0.2f;
//                      }
//                  }
//                // case R.id.refSwitch: // finds distance for all objectssss
//                break;

        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
// pay attention to process 2
    @Override
    public void onPause() {
        super.onPause();

/*
        String currentFolder2 = getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH2 = currentFolder2 + File.separator + "extra_inf.txt";

        PrintWriter fileOut2 = null;
        PrintStream streamOut2 = null;



        int size = current.size();
      //  errorAnalysis2(size);
        String currentFolder = getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "Extra_inf"+ fileseries+".csv";
        Toast.makeText(this,"FILE PATH: " + FILEPATH, Toast.LENGTH_LONG).show();

        if(quality_log.size()!=0) {
            String[] elements = quality_log.get(0).split(","); // num of quality or deg-error log
            // write down headers
            try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, false))) {

                StringBuilder sb = new StringBuilder();
                sb.append("name");
                sb.append(',');
                sb.append("Time_log");
                for (int i = 0; i <= elements.length; i++)
                    sb.append(',');


                for (int i = 0; i < elements.length; i++)
                    sb.append("Q" + (i + 1) + ",");
                sb.append(',');
                sb.append(",");

                for (int i = 0; i < elements.length; i++)
                    sb.append("Dis" + (i + 1) + ",");

                for (int i = 0; i < elements.length; i++)
                    sb.append("DegE" + (i + 1) + ",");
                sb.append('\n');
                writer.write(sb.toString());
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }


            try {
                fileOut2 = new PrintWriter(new FileOutputStream(FILEPATH2, false));

                ///fileOut2.println();
                //fileOut2.println("object information");
                //int ind=0;
                for (int ind = 0; ind < objectCount; ind++) {




                    // for csv file, nill added
                    try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {

                        StringBuilder sbb = new StringBuilder();
                        sbb.append(renderArray.get(ind).fileName);
                        sbb.append(',');

                        sbb.append(time_log.get(ind));
                        sbb.append(',');
                        sbb.append(quality_log.get(ind));
                        sbb.append(',');
                        sbb.append(distance_log.get(ind));
                        ;
                        sbb.append(',');
                        sbb.append(deg_error_log.get(ind));
                        sbb.append(',');

                        sbb.append('\n');
                        writer.write(sbb.toString());
                        System.out.println("done!");
                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    }


                }
                float total_gpu = compute_GPU_ut(decision_p / decision_p, total_tris); // for one second

                fileOut2.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
*/

     //   t2.cancel();
        //process2.destroy();


    }



//    public ArrayList<ArrayList<Float>> findW (int ind)
//    {
//
//
//        int size = current.size();
//        float upos_x= current.get(size-1).get(0);
//        float upos_z=current.get(size-1).get(2);
//        float obj_x= renderArray.get(ind).baseAnchor.getWorldPosition().x;
//        float obj_z= renderArray.get(ind).baseAnchor.getWorldPosition().z;
//        float w = 1;
//        float u_x = upos_x;
//        float u_y = upos_z;
//        boolean userfarther = false;
//        ArrayList<Float>newdistance= new ArrayList<Float>();
//        int counter=1;
//        //boolean flag=false;
//
//        // just onetime order is d0, 0 , for p=2
//        newdistance.add(renderArray.get(ind).return_distance_predicted(upos_x, upos_z) ); // add current dis
//        for (int i=1; i< decision_p; i++)
//                newdistance.add(0f);
//
//        while (userfarther == false &&  ((2*counter* decision_p)-1 <maxtime)  &&  counter< finalw ){
//            float unext_x = prmap.get ( (2*counter* decision_p)-1).get(size-1).get(0);// middle point of c_area
//            float unext_z = prmap.get( (2*counter* decision_p)-1).get(size-1).get(1);;//(uspeedy * decision_p) + u_y;
//
//            if (upos_x <= obj_x && upos_z<=obj_z)
//                 if (unext_x <= obj_x && unext_z<=obj_z && upos_x<=unext_x && upos_z<= unext_z )
//                 {
//                     w += 1;
//                     newdistance.add(renderArray.get(ind).return_distance_predicted(unext_x, unext_z) );
//                     for (int i=1; i< decision_p; i++)
//                         newdistance.add(0f);
//
//
//                 }
//                 else
//                     {userfarther = true;
//                        break; }
//
//
//            else if (upos_x <= obj_x && upos_z>=obj_z)
//                if (unext_x <= obj_x && unext_z>=obj_z && upos_x<=unext_x && upos_z>= unext_z )
//                {
//                    w += 1;
//                    newdistance.add(renderArray.get(ind).return_distance_predicted(unext_x, unext_z) );
//                    for (int i=1; i< decision_p; i++)
//                        newdistance.add(0f);
//
//
//                }
//                else
//                     { userfarther = true;
//                       break; }
//
//             else if (upos_x >= obj_x && upos_z>=obj_z)
//                    if (unext_x >= obj_x && unext_z>=obj_z && upos_x>= unext_x && upos_z>= unext_z)
//                    {
//                        w += 1;
//                        newdistance.add(renderArray.get(ind).return_distance_predicted(unext_x, unext_z) );
//
//                        for (int i=1; i< decision_p; i++)
//                            newdistance.add(0f);
//
//
//                    }
//                     else
//                     {userfarther = true;
//                          break;}
//
//              else if(upos_x >= obj_x && upos_z<=obj_z)
//                  if (unext_x >= obj_x && unext_z<=obj_z && upos_x>= unext_x && upos_z<= unext_z)
//                  {
//                      w += 1;
//                      newdistance.add(renderArray.get(ind).return_distance_predicted(unext_x, unext_z) );
//
//                      for (int i=1; i< decision_p; i++)
//                          newdistance.add(0f);
//
//
//                  }
//                  else
//                      {  userfarther = true;
//                     break;}
//
//            u_x=unext_x;
//            u_y = unext_z;
//            counter++;
//        }
//        ArrayList<ArrayList<Float> > temp1 = new ArrayList<ArrayList<Float> >();
//        for (int i=0;i<3; i++)
//             temp1.add(new ArrayList<>());
//
//        temp1.get(0).add(u_x);
//        temp1.get(1).add(u_y);
//        temp1.set(2,newdistance);
//        return temp1;
//    }




private float computeWidth(ArrayList<Float> point){
        float width=Math.abs(point.get(3)- point.get(5));
        return width;
}

    private float computeLength(ArrayList<Float> point){
        float length=Math.abs(point.get(4)- point.get(6));
        return length;
    }

// this function returns array predicted_distances as a map from obj index to the list of predicted distances for time t
    private  void Findpredicted_distances(){ // centers is the output of FindMiniareas, just for one area , so 01 is for pointx1, 2-3 is for point x2, ... 67, is for point x4


        float distance=0;
        float tempdis=0;
       // float mindis=Integer.MAX_VALUE;
      //  float maxdis=0;
        int jmindex;
        int jmaxdex;
        for (int t=0;t<maxtime/2; t++)

            for (int i=0;i<objectCount; i++) {

                float mindis=Integer.MAX_VALUE;
                float maxdis=0;

                for (int j = 0; j < 4; j++) // we have 4 points to calculate the distance ffrom
           {
               tempdis = renderArray.get(i).return_distance_predicted(nextfive_fourcenters.get(t).get(2 * j), nextfive_fourcenters.get(t).get((2 * j) + 1));
               if (tempdis > maxdis) {
                   maxdis = tempdis;
                  // jmaxdex = j;
               }
               if (tempdis < mindis) {
                   mindis = tempdis;
                  // jmindex = j;
               }
           }// after this, we'll get min and max dis plus their index



//            if(policy== "Aggressive")
//                predicted_distances.get(i).set(t,maxdis);
//            else if (policy== "Conservative")
//                predicted_distances.get(i).set(t,mindis);
           // else { // middle case

                ArrayList<Float> point= nextfivesec.get(t);// get next five area coordinates for time t
                float pointcx= point.get(0); float pointcz= point.get(1);
                tempdis = renderArray.get(i).return_distance_predicted(pointcx, pointcz);
                predicted_distances.get(i).set(t, tempdis);
            //}
       }

    }

    private float FindCons(ArrayList<Float> centers){ // conservative
        float distance=0;



        return distance;
    }

//  Returns t lists of 8 coordinates for 4 points of mini area centers
    private void FindMiniCenters(float percentage ){// this is to find four middle points in x% of width and lenghth of main area


        for (int t=0;t<maxtime/2; t++)
        {
            ArrayList<Float> point= nextfivesec.get(t);// ith element shows what time, 1s, 2, ... or 5th sec
            ArrayList<Float> center= new ArrayList<>();
           float length= computeLength(point);
           float width= computeWidth(point);
           float point1x= point.get(2);
           float point1z= point.get(3);
           float point3x= point.get(6);
           float point3z= point.get(7);

           float wRatio=width * percentage;
           float lRatio=length * percentage;
/// now from point px1, we calculate new x1 and z1:

          float newx1= point1x- (length/2); center.add(newx1);//0
          float newz1= point1z- (width/2);   center.add(newz1);//1

          float newx3= point3x- (length/2);
          float newz3= point3z+ (width/2);

          float newx2= newx1; center.add(newx2);//2
          float newz2=newz3;  center.add(newz2);//3

          center.add(newx3);//4
          center.add(newz3);//5
          float newx4= newx3; center.add(newx4);//6
          float newz4=newz1; center.add(newz4);//7

          nextfive_fourcenters.put(t , center);

        }


    }


    private float[] Findmed(ArrayList<Float> point){
        float [] center= new float[2];
        center[0]= point.get(0); // xcenter
        center[1]=point.get(1);//z center
        return center;
    }



    private void onUpdate() {

//Nil-april 21 did
       boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer);
            } else {
                contentView.getOverlay().remove(pointer);
            }
            contentView.invalidate();
        }

        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
    }
    /**
     * Converts arCore Frame to bitmap, passes to BitmapUpdaterApi
     * TODO: Move this function to background thread.  Only decoding/encoding to bitmap, not high complexity fxn
     * */




    private void passFrameToBitmapUpdaterApi(Frame frame) throws NotYetAvailableException {
        YuvToRgbConverter converter = new YuvToRgbConverter(this);
        Image image = frame.acquireCameraImage();
        Bitmap bmp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);


        converter.yuvToRgb(image, bmp); /** line to be multithreaded*/
        image.close();

//        bitmapUpdaterApi.updateBitmap(bmp);
        bitmapUpdaterApi.setLatestBitmap(bmp);

//        ObjectDet odetector  = new ObjectDet(this);
//        odetector.runObjectDetection(bmp);

       // objectDetectorHelper odet= new  ObjectDetectorHelper(0.5f, 2, 3, 2, 0,MainActivity.this, fileseries );
       // objectDetectorHelper odet= new  ObjectDetectorHelper();
        //  odet.detect(bmp,0);




        ///////writes images as file to storage for testing
//        File path = this.getExternalFilesDir(null);
//        File dir = new File(path, "data");
//        try {
//            File file = new File(dir, bmp+".jpeg");
//            FileOutputStream fOut = new FileOutputStream(file);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
//            fOut.flush();
//            fOut.close();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
////            LOG.i(null, "Save file error!");
////            return false;
//        }
//        System.out.println(bmp);
    }

    private boolean updateTracking() {
        Frame frame = fragment.getArSceneView().getArFrame();//OK not being used for now
        if(frame!=null) {
            try {
                /**AR passes frame to AI*/
                passFrameToBitmapUpdaterApi(frame);
            } catch (NotYetAvailableException e) {
                e.printStackTrace();
            }
        }
        boolean wasTracking = isTracking;
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;

        /*
        TextView posText = (TextView) findViewById(R.id.cameraPosition);
        posText.setText("Camera Position: " +
                "[" + posFormat.format(frame.getCamera().getPose().tx()) +
                "], [" + posFormat.format(frame.getCamera().getPose().ty()) +
                "], [" + posFormat.format(frame.getCamera().getPose().tz()) +
                "]" + "\n");
*/


      //  Log.d("memory inf",ActivityManager.MemoryInfo.);


        //dcm



//        if (multipleSwitchCheck == true) // eAR
//            for (int i = 0; i < objectCount; i++) {
//                // File file;
//                //file = new File(getExternalFilesDir(null), "/decimated" + renderArray[i].fileName + seekBar.getProgress() / 100f + ".sfb");
//
//                float ratio = updateratio[i];
//
//
//                if ((ratio ) != ratioArray[i]   ) {
//                    total_tris= total_tris- (ratioArray[i]* o_tris.get(i));// total =total -1*objtris
//                    ratioArray[i] = ratio;
//                    /// where we update total_Tris
//                    total_tris = total_tris+ (ratioArray[i]* o_tris.get(i));// total = total + 0.8*objtris
//                    curTrisTime= SystemClock.uptimeMillis();
//
//
//
//                    if(updatednetw[i]==0) // we have that obj in another local cache/ no need to add req
//                      renderArray[i].decimatedModelRequest(ratio , i, true);
//                    else{ // we need to req to the server
//
//                        renderArray[i].decimatedModelRequest(ratio , i, false);
//                        Server_reg_Freq.set(i, Server_reg_Freq.get(i)+1);
//                    }
//
//                    if (ratio  != 1 && ratio !=cacheArray[i] ) {
//                        cacheArray[i] = (ratio); // updates the cache
//
//                    }
//                }
//
//
//
//            }///for



//No need to log virtual area and volume and tris,


//need to measure obj1 as a ref updated distance and compare it to obj1_vir distance, if the change is ore than 0.5 m we may u
        //update virtual dis all along objects
        /*
        if(v_dist.size()!=0) {

    float new_obj1_distance = renderArray[0].return_distance();
    if(new_obj1_distance-v_dist.get(0) >=0.4)// if dis change is considerable you need to recalculate total area and vol

    {
            //  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", java.util.Locale.getDefault());
        total_area=0;
        total_vol=0;

        for(int i=0;i<objectCount; i++){

           float newdist=renderArray[i].return_distance();
           //new vol = oldvol * dis1 / dis2
           double newvol= (volume_list.get(i)* v_dist.get(i))/newdist;
           double newarea= (area_list.get(i)* v_dist.get(i))/newdist;
            v_dist.set(i,newdist);
            volume_list.set(i,newvol);
            area_list.set(i,newarea);
            total_vol+=newvol;
            total_area+=newarea;


        }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");


    dateFormat.format(new Date());

    String item2 = dateFormat.format(new Date()) + " num of tris: " + sum + " virtual area " + total_area + " virtual vol " + total_vol + "\n";


    try {
        FileOutputStream os = new FileOutputStream(Nil, true);
        if (item2.getBytes() != time.getBytes()) {

            time = item2;
            os.write(item2.getBytes());
            os.close();
            System.out.println(item2);
        }

        time = item2;


    } catch (IOException e) {
        Log.e("StatWriting", e.getMessage());
    }}

}
*/


        // time=item2;
        // stime+= time+ "\n";
        return isTracking != wasTracking;


    }


    public List<String> predictwindow(MainActivity ma, List<Boolean> cls, float fath, float qprev, float d11, int ww, int ind, int dindex, ArrayList<Float> predicted_d) {


        List<String> temppredict = new ArrayList<String>();
        //temppredict.clear();
        String qlog = "";
        String logbesteb = "";
      //  List<Boolean> temp_closer = new ArrayList<Boolean>(cls);

        //String logbestperiod= "";

        // tempquality.add(logbestperiod);

        if (ww == 0) {

            temppredict.add("0");
            temppredict.add(qlog);
            temppredict.add(logbesteb);
            return temppredict;
        }
        else if (ww > 0) {

            float curdis= d11;
            float nextdis1= predicted_d.get(dindex); //next d1
            if(closer.get(ind) && d11<=nextdis1)
                d11=d11;
            else if (closer.get(ind) && d11> nextdis1)
                d11=nextdis1;
            else // ! closer
                d11=d11;

          //  temppredict.clear();
            float father = 1;
            if (qprev != 1) {
                cacheArray.set(ind, qprev);
                father = qprev;
            } else if (qprev == 1) {
                cacheArray.set(ind, fath);
                father = cacheArray.get(ind);
            }

            prevquality.set(ind, qprev);


            List<String> tempq = new ArrayList<>(ma.QualitySelection(ind, d11));


            float qual1 = Float.parseFloat(tempq.get(0));
            float qual2 = Float.parseFloat(tempq.get(1));
            float eb1 = Float.parseFloat(tempq.get(2));
            float eb2 = Float.parseFloat(tempq.get(3));
/*
            float qual1 = 1f;
            float qual2 = 1f;
            float eb1 = 0f;
            float eb2 = 0f;
*/
            float d1 =  predicted_d.get(dindex);// =dis in simulation code which is next possible d1
            updatecloser(curdis, d1,ind);


            float currdis=d1;
            float nextdis=  predicted_d.get(dindex+decision_p); // next of next d1
            if(closer.get(ind) && d1<=nextdis)
                d1=d1;
            else if (closer.get(ind) && d1> nextdis)
                d1=nextdis;
            else // ! closer
                d1=d1;


            List<String> temppredict1 = new ArrayList<>(ma.predictwindow(ma, closer,father, qual1, d1, ww - 1, ind, dindex + decision_p, predicted_d));
            float eb3 = Float.parseFloat(temppredict1.get(0));
            eb3 += eb1;
            String qq1 = temppredict1.get(1);
            String eblog1 = temppredict1.get(2);

            List<String> temppredict2 = new ArrayList<>(ma.predictwindow(ma, closer,father,
                    qual2, d1, ww - 1, ind, dindex + decision_p, predicted_d));
            float eb4 = Float.parseFloat(temppredict2.get(0));
            eb4 += eb2;
            String qq2 = temppredict2.get(1);
            String eblog2 = temppredict2.get(2);



            if (eb3 >= eb4) {
                prevquality.set(ind, ((float)(Math.round((float)(qual1 * 10))) / 10));
                best_cur_eb.set(ind, ((float)(Math.round((float)(eb1 * 1000))) / 1000));
               // logbesteb = eblog1 + (String.valueOf(Math.round(eb1 * 1000) / 1000)) + ",";
                logbesteb = eblog1 + (String.valueOf((float)(Math.round((float)(eb1 * 1000))) / 1000)) + ",";
                //float x= eb1.setScale(2, RoundingMode.HALF_UP)
                //qlog = (qq1) + String.valueOf(Math.round(qual1 * 1000) / 1000) + ",";
                qlog = (qq1) + (String.valueOf((float)(Math.round((float)(qual1 * 10))) / 10)) + ",";

            } else {
                prevquality.set(ind, ((float)(Math.round((float)(qual2 * 10))) / 10));// precision is up to 0.1 for simplicity of decimation now ( to have almost all decimation levels now)
                best_cur_eb.set(ind, ((float)(Math.round((float)(eb2 * 1000))) / 1000));
                logbesteb = eblog2 + (String.valueOf((float)(Math.round((float)(eb2 * 1000))) / 1000)) + ",";
                qlog = (qq2) + (String.valueOf((float)(Math.round((float)(qual2 * 10))) / 10)) + ",";
            }

            temppredict.clear();
            temppredict.add(String.valueOf((float)(Math.round((float)(Math.max(eb3, eb4) * 1000))) / 1000));

            temppredict.add(qlog);
            temppredict.add(logbesteb);
        }

        return temppredict;


    }

    public void updatecloser(float prevdis, float nextdis, int ind ){

        //List<Boolean> temp_closer = new ArrayList<Boolean>(cls);
        if (prevdis-nextdis>=0.09)// to avoid small errors while standing
            closer.set(ind, true);
        else
            closer.set(ind, false);

    }

    public List<String> QualitySelection(int ind, float d11) {

       int indq = excelname.indexOf(renderArray.get(ind).fileName);

        float gamma = excel_gamma.get(indq);
        float a = excel_alpha.get(indq);
        float b = excel_betta.get(indq);
        float c = excel_c.get(indq);
        float filesize= excel_filesize.get(indq);

        float c1 = (float) (c - ((Math.pow(d11, gamma) * max_d.get(indq)))); //# ax2+bx+c= (d^gamma) * max_deg

        float finalinp = delta(a, b, c1, c, d11, gamma, indq);
//added- Nil
        if (finalinp<0.1 && finalinp >0)
            finalinp=0.1f;


        //float degerror;
        String qresult;

        float q1=1,q2=1;
        float eb1=0,eb2=0;
        float GPU_usagemax=0;
        float quality=1;
        if (closer.get(ind)) {
            qresult = adjustcloser(finalinp, prevquality.get(ind), a, b, c, d11, gamma, ind, indq);

            GPU_usagemax = compute_GPU_eng( decision_p,(float) total_tris);
            q1 = q2 = 1.0f;

        } else {

            qresult = adjustfarther(finalinp, prevquality.get(ind), ind);
            GPU_usagemax = compute_GPU_eng( decision_p,(float) total_tris);
            q1 = q2 = prevquality.get(ind);
        }




        float GPU_usagedec=0, GPU_usagedec2=0;

        double current_tris=0;
        if (qresult=="qprev forall")
           // : # for whole period p show i"' quality '''calculate gpu saving for qprev againsat q=1 eb= saving - dec = saving'''
        {
            quality = prevquality.get(ind);
            if (quality == 0)
                quality = 1;

            //gpu gaused without this obj totally
             current_tris= total_tris - (  (1- quality) * excel_tris.get(indq));
            GPU_usagedec = compute_GPU_eng( decision_p,(float) current_tris);

            q1 = quality;
            gpusaving.set(ind, GPU_usagemax - GPU_usagedec);
            eb1 = gpusaving.get(ind);

            current_tris= total_tris - (  (1- q2) * excel_tris.get(indq));
            GPU_usagedec2 = compute_GPU_eng( decision_p, (float)current_tris);
            eb2 = GPU_usagemax - GPU_usagedec2; // in milli joule
        }
        else if (qresult=="iz") //: # for whole period p show i"' quality
        {
            quality = finalinp;
            if (quality == 0)
              quality = 1;
            current_tris= total_tris - (   (1-quality) * excel_tris.get(indq));
            GPU_usagedec = compute_GPU_eng(decision_p,(float) current_tris);

            gpusaving.set(ind, GPU_usagemax - GPU_usagedec);
            float dec_eng=0f;
            if (quality == cacheArray.get(ind))
              //  eng_dec.set(ind,0f);
                dec_eng=0f;

            else
                dec_eng=update_e_dec_req( filesize, quality);
                //eng_dec.set(ind, update_e_dec_req( filesize, quality) );
        //#for object with index at ti

            eb1 = gpusaving.get(ind) -dec_eng;
                    //eng_dec.get(ind);
            q1 = quality;

            current_tris= total_tris - (   (1-q2) * excel_tris.get(indq));
            GPU_usagedec2 = compute_GPU_eng(decision_p, (float)current_tris);
            eb2 = GPU_usagemax - GPU_usagedec2;

        }


        else if (qresult=="cache forall") {
            current_tris= total_tris - (   (1-cacheArray.get(ind)) * excel_tris.get(indq));
            GPU_usagedec = compute_GPU_eng( decision_p, (float)current_tris);

            quality = cacheArray.get(ind);
            if (quality == 0)
                quality = 1;

            q1 = quality;
      //  #eb1 =
            gpusaving.set(ind, GPU_usagemax - GPU_usagedec);
            eb1= gpusaving.get(ind);


        }

        else if (qresult=="delta1") {
            GPU_usagedec = GPU_usagemax;
            quality = 1;
            q1 = 1;
            eb1 = 0;
            current_tris= total_tris - (  (1-q2) * excel_tris.get(indq));
            GPU_usagedec2 = compute_GPU_eng( decision_p, (float)current_tris);
            eb2 = GPU_usagemax - GPU_usagedec2;

        }
       // tempquality.clear();
        List<String> tempquality = new ArrayList<String>();
        tempquality.add(Float.toString(q1));
        tempquality.add(Float.toString(q2));
        tempquality.add(Float.toString(eb1));
        tempquality.add(Float.toString(eb2));


        return tempquality;


    }


  public float  update_e_dec_req(  float size, float qual){
         //   '''this is to update energy consumption for decimation '''
      //return 0;

      if (qual==1)
            return 0;

      //assume net is 5g
      float eng_network=  (size/ (1000000f * bwidth )) * 1.5f * 1000f;// in milli joule




    //float eng_network= (((229.4f/bwidth) + 23.5f)*size)/1000000 ;// #it is mili joule = mili watt * sec ->

      //1float timesec= size/bwidth*1000000; // sec takes to get the file from server to phone
     // 1float pow_network= eng_network/timesec; // miliwatt

     //float total_phone_energy= phone_batttery_cap * 3600 * 1000000000;
     //float eng_network_perc= (eng_network  / total_phone_energy) *100;

    //float decenergy= eng_network; //# energy for downloading and network constant cost

            return eng_network;// in mili joule


}




    public float compute_GPU_eng(float period, float current_tris) // returns gpu percentage if we decimate obj1 to qual
    {
        //'''this is to calculate gpu utilization having quality
        float gpu_perc=0;
        if(current_tris< gpu_min_tres)
            gpu_perc= bgpu;// baseline }

        else
           gpu_perc = (agpu * current_tris) + bgpu; // gets gpu utilization in percentage for 1 sec

      float gpu_power_eng= ((7.42f * gpu_perc) + 422.9f) * period; // in milli joule
      return gpu_power_eng;

    }


    public float compute_actual_GPU_eng(float period, float gpu_perc) // returns gpu percentage if we decimate obj1 to qual
    {
        //'''this is to calculate gpu utilization having quality

        float gpu_power_eng= ((7.42f * gpu_perc) + 422.9f) * period; // in milli joule
        return gpu_power_eng;

    }


    public float compute_GPU_ut(float period, float current_tris) // returns gpu percentage if we decimate obj1 to qual
    {
        //'''this is to calculate gpu utilization having quality
        float gpu_perc=0;
        if(current_tris< gpu_min_tres)
            gpu_perc= bgpu;// baseline }

        else
            gpu_perc = (agpu * current_tris) + bgpu; // gets gpu utilization in percentage for 1 sec

        return gpu_perc;

    }

    public String adjustcloser(float x1, float prevq, float a, float b, float c, float d11, float gamma, int ind, int indq)//: # four cases we need to adjust xs to 0 or 1 which are cases with at least two '1's
    {
        String value = "";
        if (x1 != 0)//: # 111 or 110
        {
            if (Math.abs(x1 - prevq) < 0.1)
                value = "qprev forall"; //#means i '' for all

            else if (Math.abs(cacheArray.get(ind) - x1) < 0.1 && Testerror(a, b, c, d11, gamma, prevq, indq) == true)
                //: #if we can use the prev downloaded quality instead of the closer quality
                value = "cache forall";// #means i '' for all

            else
                value = "iz"; //#means i '' for d1
        }
        else//:#000, 001
            value = "delta1"; //#means delta1

        return value;

    }


    public boolean Testerror(float a, float b, float creal, float d, float gamma, float r1, int ind) {

    float error = (float) ((a*(Math.pow(r1,2))+b*r1 +creal)/(Math.pow(d,gamma)));
            if(error<=max_d.get(ind))
                return true;
            else
             return false;
    }


    public String adjustfarther(float x1,float prevq, int ind)//: # four cases we need to adjust xs to 0 or 1 which are cases with at least two '1's
    {
        String value="";
         if (x1!=0)//: # 11 or 10
         {
             if (Math.abs(prevq - x1) < 0.1)
                  value = "qprev forall";// #means i '' for all
             else if(Math.abs(cacheArray.get(ind) - x1) < 0.1)
                  value = "cache forall";

             else
                     value = "iz"; //#means i '' for d1
         }

    else if (x1==0 )//: # case 100 and 101
         value="qprev forall"; //#means i'' for all

 return value;

    }

    public float checkerror(float a,float b,float creal,float d,float gamma, int ind) {


       float r1 = 0.1f;
       float error;


       for (int i =0; i< 18; i++) {

           error = (float) (((a * Math.pow(r1,2)) + (b * r1) + creal) / (Math.pow(d , gamma)));
           if (error < max_d.get(ind))
                return r1;
           r1 += 0.05;


       }
       return 0;

   }

    public float Calculate_deg_er(float a,float b,float creal,float d,float gamma, float r1) {

        float error;
        if(r1==1)
          return  0f;
        error = (float) (((a * Math.pow(r1,2)) + (b * r1) + creal) / (Math.pow(d , gamma)));
        return error;
    }


public float delta (float a, float b , float c1,float creal,  float d, float gamma, int ind){

        float r=0f;
        float r1, r2=r;
        float dlt = (float) (Math.pow(b, (2f)) - (4f *(a*c1)));
        //float deg_error;

        // two roots
        if (dlt>0) {
            r1 = (float) (((-b) + Math.sqrt(dlt)) / (2 * a));
            r2 = (float) (((-b) - Math.sqrt(dlt)) / (2 * a));

            if (0.001 < r1 && r1 < 1.0 && 0.001 < r2 && r2 < 1.0)
            {
                r = Math.min(r1, r2);


                return r;
            }
            else if (0.001 < r1 && r1 < 1)
                r2 = checkerror(a, b, creal, d, gamma, ind);

            else if (0.001<r2 && r2<1)
                r1=checkerror(a, b,creal, d, gamma, ind);
     // #x=1

             else {
                r = checkerror(a, b, creal, d, gamma, ind);
                return r;
            }
        }

        else if (dlt==0){
            r1 = (-b) / 2*a;
            if (r1>1 || r1<0)
                r1=checkerror(a, b,creal, d, gamma, ind);
            r2=0;

        }


        else{
            r=checkerror(a, b,creal, d, gamma, ind);
            return r;
        }



        if (r2==0f || r2==1f)
            r=r1;

        else if (r1==0f || r1==1f)
            r=r2;

        else
            r= Math.min(r1,r2);


        return r;
}



    private boolean updateHitTest() {

        Frame frame = fragment.getArSceneView().getArFrame();



        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;


    }

    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth()/2, vw.getHeight()/2);
    }

    //initialized gallery is not used any more, but I didn't want to break anything so it's still here
    //This also creates a file in the apps internal directory to help me find it better, to be honest.
    private void initializeGallery() {
        //LinearLayout galleryR1 = findViewById(R.id.gallery_layout_r1);
     //   RelativeLayout galleryr2 = findViewById(R.id.gallery_layout);
       ConstraintLayout galleryr2 = findViewById(R.id.gallery_layout);

        //row 1

       // File file = new File(this.getExternalFilesDir(null), "/andy1k.glb");



    }


    //this came with the app, it sends out a ray to a plane and wherever it hits, it makes an anchor
    //then it calls placeobject
    private void addObject(Uri model, baseRenderable renderArrayObj) {
        Frame frame = fragment.getArSceneView().getArFrame();
       // while(frame==null)
           // frame = fragment.getArSceneView().getArFrame();


        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {


                    try {
                        Anchor newAnchor = hit.createAnchor();
                        placeObject(fragment, newAnchor, model, renderArrayObj);
                    }

                    catch (Exception e) {
                        e.printStackTrace();
                    }


                    break;
                }
            }

        }




    }

    private void addObject(Uri model, baseRenderable renderArrayObj, float xOffset, float yOffset) {
        Frame frame = fragment.getArSceneView().getArFrame();

        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x + xOffset, pt.y + yOffset);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    try {
                        Anchor newAnchor = hit.createAnchor();
                        placeObject(fragment, newAnchor, model, renderArrayObj);
                    }

                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                }
            }

        }




    }

    private void placeObject(ArFragment fragment, Anchor anchor, Uri model, baseRenderable renderArrayObj) { ;


    try {
        CompletableFuture<Void> renderableFuture =
                ModelRenderable.builder()
                        .setSource(fragment.getContext(), model)
                        // Nill oct 24. remove fillament for
                      //  .setIsFilamentGltf(true)
                        .build()
                        .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable, renderArrayObj))
                        .exceptionally((throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(throwable.getMessage())
                                    .setTitle("Codelab error!");
                            AlertDialog dialog =
                                    builder.create();
                            dialog.show();
                            return null;
                        }));


    } catch (Exception e) {
        e.printStackTrace();
    }



    }



    private void removePreviousAnchors()
    { List<Node> nodeList = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
    for (Node childNode : nodeList) {
        if (childNode instanceof AnchorNode) {
            if (((AnchorNode) childNode).getAnchor() != null) {

        ((AnchorNode) childNode).getAnchor().detach();
        ((AnchorNode) childNode).setParent(null); } } } }

    //takes both the renderable and anchor and actually adds it to the scene.
    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable, baseRenderable renderArrayObj) {

       // GLES20.glDisable(GLES20.GL_CULL_FACE);

        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        //anchorArray[anchorCount] = node;
        //anchorCount++;
        renderArrayObj.setAnchor(node);
        int value=0;
        float volume=0;
        float area=0;

        String objname= renderArrayObj.fileName+ "\n";
        String name= renderArrayObj.fileName;
        System.out.println("name is "+name);



        //Nil

        cacheArray.add(objectCount,1f);
        updatednetw.add(objectCount,0f);

        predicted_distances.put(objectCount, new ArrayList<>());

        for (int i=0; i<maxtime/2; i++)
            predicted_distances.get(objectCount).add(0f);// initiallization, has next distance for every 1 sec
        Server_reg_Freq.add(objectCount,0);


       int indq = excelname.indexOf(renderArray.get(objectCount).fileName);// search in excel file to find the name of current object and get access to the index of current object
        o_tris.add( (Integer) excel_tris.get(indq));
        // update total_tris
        total_tris+=   (  ((double)o_tris.get(objectCount) )/tris_factor)   ;
        orgTrisAllobj+=   (  ((double)o_tris.get(objectCount) )/tris_factor);

        d1_prev.add(objectCount, 0f);



        curTrisTime= SystemClock.uptimeMillis();
        //lastQuality.add(1f);// initialize


        //  Camera2BasicFragment .getInstance().update( (double) total_tris);// run linear reg



        distance_log.add(objectCount, Float.toString( renderArray.get(objectCount).return_distance()) );
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

        time_log.add(objectCount,  dateFormat.format(new Date()).toString() );


        ratioArray.add(objectCount,1f);
        objectCount++;


        gpusaving.add(0f);
        eng_dec.add("");

        closer.add(true);

        prevquality.add(1f);
        best_cur_eb.add(0f);

        quality_log.add("");


        deg_error_log.add("");
        obj_quality.add(1f);

        trisChanged=true;


        TextView posText = (TextView) findViewById(R.id.objnum);
        posText.setText( "obj_num: " +objectCount);



        File tris = new File(MainActivity.this.getFilesDir(), "text");

        //SimpleDateFormat start= new SimpleDateFormat("yyyy/MM/dd/ HH:mm:ss:SSS", java.util.Locale.getDefault());
        //String st= start.format(new Date());


        fragment.getArSceneView().getScene().addChild(anchorNode);// ask for drawing new obj



       // renderArray.get(objectCount-1).distance();



        node.select();
      //  datacol=false;// the object is placed to the screen




    }
    public float calculatenrmDeg(int indq, int finalInd , float ratio, float d1 ){


        float gamma = excel_gamma.get(indq);
        float a = excel_alpha.get(indq);
        float b = excel_betta.get(indq);
        float c = excel_c.get(indq);

        float curQ = ratio / 100f;
        float deg_error =
                (float) (Math.round((float) (Calculate_deg_er(a, b, c, d1, gamma, curQ) * 10000))) / 10000;
        //Nill added
        //float maxd = max_d.get(indq);
        float max_nrmd = excel_maxd.get(indq);
        float cur_degerror=deg_error / max_nrmd;
        return cur_degerror;
    }



    private float nextPoint(float x1, float x2, float y1, float y2, float time)
    {
        float slope = (y2 - y1)/(x2 - x1);
        float y3 = slope*(x2 + time) -(slope*x1) + y1;
        return y3;
    }

    private float nextPointEstimation(float actual, float predicted)
    {
        return (alpha * actual) + ((1 - alpha) * predicted);
    }


    private float[] rotate_point(double rad_angle, float x, float z)
    {
        float[] rotated = new float[2];

        rotated[0] = x* (float)Math.cos(rad_angle) - z * (float)Math.sin(rad_angle);
        rotated[1] = x* (float)Math.sin(rad_angle) + z * (float)Math.cos(rad_angle);

        return rotated;
    }

    private float[] rotate_around_point(double rad_angle, float x, float z, float orgX, float orgZ)
    {
        float[] rotated = new float[2];

        rotated[0] = (x - orgX)* (float)Math.cos(rad_angle) - (z - orgZ) * (float)Math.sin(rad_angle) + orgX;
        rotated[1] = (x - orgX)* (float)Math.sin(rad_angle) + (z - orgZ) * (float)Math.cos(rad_angle) + orgZ;

        return rotated;
    }

//has prmap with too many inf -> enhance this fucntion
    private ArrayList<Float>
    predictNextError2(float time, int ind)
    {
        ArrayList<Float> predictedValues = new ArrayList<>();
        ArrayList<Float> margin = new ArrayList<>();
        ArrayList<Float> error = new ArrayList<>();
        int curr_size = current.size();
        float predictedX = 0f;
        float predictedZ = 0f;
        float actual_errorX = 0f;
        float actual_errorZ = 0f;
        float predict_diffX, predict_diffZ;


        // ind 0,   1,  2,  3,  4, 5 , 6, 7 , 8 , 9 , 10
        //time 0.5, 1, 1.5, 2, 2.5, 3, 3.5,  4,  4.5, 5
        // currsize - i1  2 , 3,  4,   5,   6
        //prmap.get(0) is equall to predicted05
        //prmap.get(10).get(curr_size - 1).get(1) = predicted z value in next 5 sec
        int i1 = ind +2;

        if(curr_size >1 )
        {

            float marginx = 0.3f, marginz = 0.3f;

            if (curr_size > maxtime) {
                predict_diffX =  prmap.get(ind).get(curr_size - i1).get(0) - current.get(curr_size - i1).get(0);
                predict_diffZ = prmap.get(ind).get(curr_size - i1).get(1) - current.get(curr_size - i1).get(2);
                float actual_diffX = current.get(curr_size - 1).get(0) - current.get(curr_size - i1).get(0);
                float actual_diffZ = current.get(curr_size - 1).get(2) - current.get(curr_size - i1).get(2);
                predictedX = nextPointEstimation(actual_diffX,predict_diffX) + current.get(curr_size - 1).get(0);
                predictedZ = nextPointEstimation(actual_diffZ,predict_diffZ)+ current.get(curr_size - 1).get(2);
            }
            else{
                predictedX = nextPoint(timeLog.get(curr_size - 2), timeLog.get(curr_size - 1), current.get(curr_size - 2).get(0), current.get(curr_size - 1).get(0), time);
                predictedZ = nextPoint(timeLog.get(curr_size - 2), timeLog.get(curr_size - 1), current.get(curr_size - 2).get(2), current.get(curr_size - 1).get(2), time);
            }

            if (curr_size > i1) {


                actual_errorX = abs(current.get(curr_size - 1).get(0) - prmap.get(ind).get(curr_size - i1).get(0));// err btw actual coo and predicted point
                actual_errorZ = abs(current.get(curr_size - 1).get(2) - prmap.get(ind).get(curr_size - i1).get(1));
                float margin_x = abs(nextPointEstimation(actual_errorX, marginmap.get(ind).get(curr_size-2).get(0)));
                float margin_z = abs(nextPointEstimation(actual_errorZ, marginmap.get(ind).get(curr_size-2).get(1)));

                if (curr_size>max_datapoint){ // we need to compare the margin with 100 percentile error

                    List<Float> sortedlist_x = new LinkedList<>(last_errors_x);
                    List<Float> sortedlist_z = new LinkedList<>(last_errors_z);
                    Collections.sort(sortedlist_x);
                    Collections.sort(sortedlist_z);
                    float max_x= sortedlist_x.get(sortedlist_x.size() - 1);
                    float max_z= sortedlist_x.get(sortedlist_z.size() - 1);

                    marginx = Math.max(margin_x ,max_x );
                    marginz= Math.max(margin_z, max_z);


                }
                else // traditional point, cur data points are less thatn 25 as an eg,
                {
                    //if(margin_x < marginmap.get(ind).get(curr_size-2).get(0))
                        marginx = Math.max( marginmap.get(ind).get(curr_size-2).get(0),margin_x );
                        marginz= Math.max( margin_z , marginmap.get(ind).get(curr_size-2).get(1));
                   // else
                    //     marginx = margin_x;

                //  if(margin_z < marginmap.get(ind).get(curr_size-2).get(1))
                   //   marginz = marginmap.get(ind).get(curr_size-2).get(1);
                  //else
                        //marginz = margin_z;
            }

            }

            margin.add(marginx);
            margin.add(marginz);

            // nill added sep 12 2022 to minimize storage usage
            if(marginmap.get(ind).size()==30) {
                marginmap.get(ind).remove(0);
                errormap.get(ind).remove(0);

            }


            marginmap.get(ind).add(margin);
            error.add(actual_errorX);
            error.add(actual_errorZ);
            errormap.get(ind).add(error);


            // last error is not needed
            if (last_errors_x.size()<max_datapoint) {

                //int size=last_errors_x.size();
               // last_errors_x.add(size, new LinkedList<>());
                //last_errors_z.add(size, new LinkedList<>());

                last_errors_x.add( actual_errorX);
                last_errors_z.add( actual_errorZ);
            }
            else{

                last_errors_x.remove();// remove the head, or oldest one
                last_errors_z.remove();
              //  last_errors.add(max_datapoint-1,new LinkedList<>() );
                last_errors_x.add( actual_errorX);// add new one
                last_errors_z.add( actual_errorZ);
            }


            double tan_val = (double)((predictedZ-current.get(curr_size - 1).get(2))/(predictedX-current.get(curr_size - 1).get(0)));
            double angle = Math.atan(tan_val);
            predictedValues.add(predictedX); //predicted X value
            predictedValues.add(predictedZ); //predicted Z value


            float[] rotated = rotate_point(angle, marginx,marginz);
            predictedValues.add(predictedX + rotated[0]); //Confidence area X coordinate 1
            predictedValues.add(predictedZ + rotated[1]); //Confidence area Y coordinate 1
            //float[] val1 = rotate_around_point(theta,predictedX + rotated[0], predictedZ + rotated[1],current.get(curr_size - 1).get(0), current.get(curr_size - 1).get(2));

            rotated = rotate_point(angle, marginx,-marginz);
            predictedValues.add(predictedX + rotated[0]); //Confidence area X coordinate 2
            predictedValues.add(predictedZ + rotated[1]); //Confidence area Y coordinate 2

            rotated = rotate_point(angle, -marginx,-marginz);
            predictedValues.add(predictedX + rotated[0]); //Confidence area  X coordinate 3
            predictedValues.add(predictedZ + rotated[1]); //Confidence area Y coordinate 3

            rotated = rotate_point(angle, -marginx,marginz);
            predictedValues.add(predictedX + rotated[0]); //Confidence area X coordinate 4
            predictedValues.add(predictedZ + rotated[1]); //Confidence area Y coordinate 4

        }
        else {
            int count=0;
            for (count=0; count<=9; count++)// we have 10 points for cofidence area
                predictedValues.add(0f);

        }
        return predictedValues;
    }




    private float area_tri(float x1, float y1, float x2, float y2, float x3, float y3)
    {
        return (float)Math.abs((x1*(y2-y3) + x2*(y3-y1)+ x3*(y1-y2))/2.0);
    }



    private ArrayList<Float> check_rect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float x, float y)
    {
        ArrayList<Float> bool_val;
        /* Calculate area of rectangle ABCD */
        float A = area_tri(x1, y1, x2, y2, x3, y3) + area_tri(x1, y1, x4, y4, x3, y3);

        /* Calculate area of triangle PAB */
        float A1 = area_tri(x, y, x1, y1, x2, y2);

        /* Calculate area of triangle PBC */
        float A2 = area_tri(x, y, x2, y2, x3, y3);

        /* Calculate area of triangle PCD */
        float A3 = area_tri(x, y, x3, y3, x4, y4);

        /* Calculate area of triangle PAD */
        float A4 = area_tri(x, y, x1, y1, x4, y4);

        /* Check if sum of A1, A2, A3 and A4  is same as A */
        float sum = A1 + A2 + A3 + A4;
        if(Math.abs(A - sum) < 1e-3)
            bool_val = new ArrayList<Float>(Arrays.asList(1f, A));
        else
            bool_val = new ArrayList<Float>(Arrays.asList(0f, A));

        return bool_val;
    }



//    private void errorAnalysis2(int size)
//    {
//        float area = 0f;
//        for(int i = 0; i < size - maxtime; i++) {
//            for (int k = 0; k < maxtime ; k++) {
//
//                booleanmap.get(k).add(check_rect(prmap.get(k).get(i).get(2), prmap.get(k).get(i).get(3), prmap.get(k).get(i).get(4), prmap.get(k).get(i).get(5),
//                        prmap.get(k).get(i).get(6), prmap.get(k).get(i).get(7), prmap.get(k).get(i).get(8), prmap.get(k).get(i).get(9),
//                        current.get(i + 1 + k).get(0), current.get(i + 1+ k).get(2)));
//
//            }
//        }
//    }


    @SuppressLint("SuspiciousIndentation")
    private void getCpuPer() { //for single process

        String currentFolder = getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "CPU_Mem_"+ fileseries+".csv";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");



        float cpuPer = 0;
        try {

            String[] cmd = {"top", "-s", "6"};
                    //{"top", "-n", "1"};

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));


            String s = null;
            while ((s = stdInput.readLine()) != null)
                if(s.contains("com.arcore.Mix") ){
                    //|| s.contains("%MEM")){

                try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {

                    while(s.contains("  "))
                        s = s.replaceAll("  "," ");

                    s = s.replaceAll(" ",",");



                    writer.write( dateFormat.format(new Date())+"," +s + "\n");

                    System.out.println("done!");



                } catch (FileNotFoundException e) {
                    System.out.println(e.getMessage());
                }

                break; // get out of the loop-> avoids infinite loop

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
       // return cpuPer;
    }




    public void givenUsingTimer_whenSchedulingTaskOnce_thenCorrect() {



        String currentFolder = getExternalFilesDir(null).getAbsolutePath();
        String FILEPATH = currentFolder + File.separator + "GPU_Usage_"+ fileseries+".csv";
        Timer  t = new Timer();

        t.scheduleAtFixedRate(

                new TimerTask() {
                    //        TimerTask task = new TimerTask() {
                    public void run() {
/// test cpu perc
                       // getCpuPer();

                        //    if(objectCount>=0) { // remove- ni april 21 temperory
                        Float mean_gpu = 0f;
                        float gpu_clk=0f;
                        float dist = 0;
                        float cpuFreq=0f;
                       // if (renderArray.size()>=2)

                        String filname=" ";

                        if(objectCount>0) {
                            filname = renderArray.get(objectCount - 1).fileName;
                            dist = renderArray.get(objectCount-1).return_distance();

                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

                        //dateFormat.format(new Date());

                        String current_gpu = null;
                        String current_cpu=null;
                        try {

                            String[] InstallBusyBoxCmd = new String[]{
                                    "su", "-c", "cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"};

                            // process2 = Runtime.getRuntime().exec(InstallBusyBoxCmd); // this is fro oneplus phone

                            // this is for galaxy s10
                            process2 = Runtime.getRuntime().exec("cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"); // this is for S10 phone
                            BufferedReader stdInput = new BufferedReader(new
                                    InputStreamReader(process2.getInputStream()));
// Read the output from the command
                            //System.out.println("Here is the standard output of the command:\n");
                            current_gpu = stdInput.readLine();
                            if (current_gpu != null) {
                                String[] separator = current_gpu.split("%");
                                mean_gpu = mean_gpu + Float.parseFloat(separator[0]);
                            }
//                            cat /sys/devices/system/gpu/max_freq

//                            process2 = Runtime.getRuntime().exec("su -c cat /sys/class/kgsl/kgsl-3d0/gpuclk"); // this is for S10 phone
//                            stdInput = new BufferedReader(new
//                                    InputStreamReader(process2.getInputStream()));
//// Read the output from the command
//                            //System.out.println("Here is the standard output of the command:\n");
//                            current_gpu = stdInput.readLine();
//                            if (current_gpu != null) {
//                              ///  String[] separator = current_gpu.split("%");
//                                gpu_clk =  Float.parseFloat(current_gpu);
//                            }


                            process2 = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"); // this is for S10 phone
                            stdInput = new BufferedReader(new
                                    InputStreamReader(process2.getInputStream()));
// Read the output from the command
                            //System.out.println("Here is the standard output of the command:\n");
                            current_cpu = stdInput.readLine();
                            if (current_cpu != null) {
                                cpuFreq =Float.parseFloat(current_cpu);
                            }


                            process2 = Runtime.getRuntime().exec("top -s 6"); // this is for S10 phone
                            stdInput = new BufferedReader(new
                                    InputStreamReader(process2.getInputStream()));

                            while ((current_cpu = stdInput.readLine()) != null )

                                if(current_cpu.contains("com.arcore.AI_")){ // based on the name of the app which starts with AI_Res...
                               // PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true));

                                     while (current_cpu.contains("  "))
                                        current_cpu =current_cpu.replaceAll("  ", " ");

                                     current_cpu = current_cpu.replaceAll(" ", ",");


                                     break;


                                     }

                        }
                           catch (IOException e) {
                            e.printStackTrace();
                        }


                      //  String item2 = dateFormat.format(new Date()) + " num_of_tris: " + total_tris + " current_gpu " + mean_gpu + " dis " + dist +  " lastobj "+ filname + objectCount+ "\n";


                        try (PrintWriter writer = new PrintWriter(new FileOutputStream(FILEPATH, true))) {

                            StringBuilder sb = new StringBuilder();
                            sb.append(dateFormat.format(new Date())); sb.append(',');
                            sb.append(total_tris); sb.append(',');sb.append(mean_gpu);sb.append(',').append(gpu_clk);sb.append(',')  ;
                            sb.append(dist);

                            sb.append(',');  sb.append(filname);  sb.append(',');
                            sb.append(objectCount);
                            sb.append(',');
                            sb.append(current_cpu);
                            sb.append(',');
                            sb.append(cpuFreq);

                            sb.append('\n');



                            writer.write(sb.toString());

                            System.out.println("done!");

                        } catch (FileNotFoundException e) {
                            System.out.println(e.getMessage());
                        }


                        // This is to collect position prediction every 500 ms
///* nill temporaraly deactivated this
                        if (objectCount >= 1) { // Nil april 21 -> fixed

                            Frame frame = fragment.getArSceneView().getArFrame();//OK
                            while(frame==null)
                                frame = fragment.getArSceneView().getArFrame();//OK

                           // if (frame != null){
                            ///nill added sep 12 to limit the size of current
                            if(current.size()==30)
                                current.remove(0);// removes from the first element

                            // adds as the last element
                            current.add(new ArrayList<Float>(Arrays.asList(frame.getCamera().getPose().tx(), frame.getCamera().getPose().ty(), frame.getCamera().getPose().tz())));

                            // nill added sep 12
                            if(timeLog.size()==30)
                                timeLog.remove(0);


                            timeLog.add(timeInSec);


                            timeInSec = timeInSec + 0.5f;
                            float j = 0.5f;
                            for (int i = 0; i < maxtime; i++) {
                                //nill sep 12 added to limit the size for prmap and current map/arrays
                                if(prmap.get(i).size()==30)
                                    prmap.get(i).remove(0); // removes first element

                                prmap.get(i).add(predictNextError2(j, i));
                               // prmap.get(i).add(predictNextError2(j, i));
                                j += 0.5f;
                            }
                            // nil cmt april 28 if (count[0] % 2 == 0) { // means that we are ignoring 0.5 time data, 0-> next 1s, 2 is for next 2sec , 4 is for row fifth which is 4s in array of next1sec

                            for (int i = 0; i < maxtime / 2; i++) // for next 5 sec if maxtime = 10
                            {
                                // nill added sep 12 - keep list restricted to 30 count
                                if(nextfivesec.get(i).size()==30)
                                    nextfivesec.get(i).remove(0);

                               // nextfivesec.set(i, prmap.get(2 * i + 1).get(count[0]));

                                nextfivesec.set(i, prmap.get(2 * i + 1).get( prmap.get(2 * i + 1).size()-1  )); // get the last value

                            }

                         //nill sep
                             FindMiniCenters(area_percentage);
                            Findpredicted_distances();

                           // count[0]++;


                        }
                        //*/



                    }


                },
                0,      // run first occurrence immediatetly
                2000);
    };



//    float a_t=-8.825245622870156e-06f, b_t=-0.5743863744426319f, c_t= 55.801f;
//
//    float a_re=5.18208816882466E-07f, b_re=0.111009877415992f, c_re=0.00213110940797337f, d_re=0.00118086288001582f;








    private boolean isObjectVisible(Vector3 worldPosition)
    {
        float[] var2 = new float[16];
        Frame frame = fragment.getArSceneView().getArFrame(); // OK not used
        Camera camera = frame.getCamera();

        float[] projmtx = new float[16];
        camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

        float[] viewmtx = new float[16];
        camera.getViewMatrix(viewmtx, 0);
        Matrix.multiplyMM(var2,0,projmtx,0, viewmtx, 0);

        float var5= worldPosition.x;
        float var6 = worldPosition.y;
        float var7 = worldPosition.z;

        float var8 = var5 * var2[3]+ var6 * var2[7] + var7 * var2[11] + 1.0f * var2[15];
        if (var8 < 0f) {
            return false;
        }

        Vector3 var9 = new Vector3();
        var9.x = var5 * var2[0] + var6 * var2[4] + var7 * var2[8] + 1.0f * var2[12];
        var9.x = var9.x / var8;
        if (var9.x < -1f || var9.x > 1f) {
            return false;
        }

        var9.y = var5 * var2[1] + var6 * var2[5] + var7 * var2[9] + 1.0f * var2[13];
        var9.y = var9.y / var8;
        if (var9.y < -1f || var9.y > 1f) {
            return false;
        }

        return true;
    }
    /**
     * Hide buttons to change amount of AI tasks
     * */
    public void toggleAiPushPop() {
        Button buttonPushAiTask = (Button) findViewById(R.id.button_pushAiTask);
        Button buttonPopAiTask = (Button) findViewById(R.id.button_popAiTask);
        TextView textNumOfAiTasks = (TextView) findViewById(R.id.text_numOfAiTasks);

        if (buttonPushAiTask.getVisibility()==View.VISIBLE) {
            buttonPushAiTask.setVisibility(View.INVISIBLE);
            buttonPopAiTask.setVisibility(View.INVISIBLE);
            textNumOfAiTasks.setVisibility(View.INVISIBLE);
        }
        else {
            buttonPushAiTask.setVisibility(View.VISIBLE);
            buttonPopAiTask.setVisibility(View.VISIBLE);
            textNumOfAiTasks.setVisibility(View.VISIBLE);
        }
    }

}
