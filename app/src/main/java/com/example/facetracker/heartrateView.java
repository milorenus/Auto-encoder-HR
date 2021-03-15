package com.example.facetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import org.opencv.core.Rect;

/**
 * Created by johnpeterlomaliza on 3/8/18.
 */


public class heartrateView extends SurfaceView implements SurfaceHolder.Callback {
static  final int SignalSize = 512;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            ;
    //static {
   //     System.loadLibrary("native-lib");
   // }


    public SurfaceHolder holder;
    myThread HeartRateThread;
    static Context context;
    static Rect pos, neg;
    static boolean canStartTest = false;
    int sampleSize = 0;
    static int callCase = 0;

    static TextView hr_display, time_display;
    boolean hasMetCondition = false;
    boolean isComputingHr = false;
    boolean didStart = false;

    //static boolean regionBasedMode = false;
    boolean initializedParameters = false;
   // Mat front_frame, back_frame;



    //double [][] signals_vertical = new double[SignalSize][8];
    //double [][] signals_horizontal = new double[SignalSize][8];

    public heartrateView(Context context) {
        super(context);
        init(context);
    }
    public heartrateView(Context context, AttributeSet attrs){
        super(context, attrs);
        init(context);
    }

    public heartrateView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context c){
        this.context = c;
        HeartRateThread = new myThread(this);
        holder = this.getHolder();
        holder.addCallback(this);
        //String test = MainActivity.stringFromJNI();
       // Log.e("TAG", "Call from hrv to show native: "+test);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        HeartRateThread.setRunning(true);
        HeartRateThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        boolean retry = true;
        HeartRateThread.setRunning(false);
        while (retry) {
            try {
                HeartRateThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }

        Log.e("TAG", "HRV exited");
    }

    public void drawIt(Canvas c) {



    }








}
