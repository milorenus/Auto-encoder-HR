package com.example.facetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerBoosting;
import org.opencv.tracking.TrackerCSRT;
import org.opencv.tracking.TrackerGOTURN;
import org.opencv.tracking.TrackerKCF;
import org.opencv.tracking.TrackerMIL;
import org.opencv.tracking.TrackerMOSSE;
import org.opencv.tracking.TrackerMedianFlow;
import org.opencv.tracking.TrackerTLD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AEHRView extends heartrateView{
    List<List<Double>> signals_vertical = new ArrayList<>();
    List<List<Double>> signals_horizontal = new ArrayList<>();
    Mat frame;
    int tIndex = 4;

    List<Rect2d> regions = new ArrayList<>();

    public AEHRView(Context context) {
        super(context);
    }

    public AEHRView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AEHRView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void drawIt(Canvas c) {

        int myColor = Color.parseColor("#ffffff");

        if(c!=null) {
            c.drawColor(myColor);
        }

        if(initializedParameters)
            Log.e("TAG", "Initialized parameters");
        else
            Log.e("TAG", "did not Initialized parameters");

        if(frame!=null)
            Log.e("TAG", "Front frame not null");
        else
            Log.e("TAG", "Front frame is null");

        //if(regionBasedMode)
        // Log.e("TAG", "region based mode");

        if(initializedParameters && frame!=null){
            if(signals_vertical.size()<SignalSize) {
                processregions pr = new processregions();
                pr.execute(frame.clone());
            }else {
                if(!startedComputingHR){
                    startedComputingHR = true;
                    computeHR chr = new computeHR();
                    chr.execute();
                }
            }
        }
    }

    ArrayList<Tracker> trackers = new ArrayList<>();
    static String[] trackerTypes = {"BOOSTING", "MIL", "KCF", "TLD","MEDIANFLOW", "GOTURN", "MOSSE", "CSRT"};

    public static Tracker getTracker(String name){
        Tracker tracker = null;
        if(name.equals(trackerTypes[0])){
            tracker = TrackerBoosting.create();
        }else if(name.equals(trackerTypes[1])){
            tracker = TrackerMIL.create();
        }else if(name.equals(trackerTypes[2])){
            tracker = TrackerKCF.create();
        }else if(name.equals(trackerTypes[3])){
            tracker = TrackerTLD.create();
        }else if(name.equals(trackerTypes[4])){
            tracker = TrackerMedianFlow.create();
        }else if(name.equals(trackerTypes[5])){
            tracker = TrackerGOTURN.create();
        }else if(name.equals(trackerTypes[6])){
            tracker = TrackerMOSSE.create();
        }else {
            tracker = TrackerCSRT.create();
        }

        return tracker;
    }//Get OpenCV's region tracker

    public  int getTrackerIndex(Tracker t){
        int index = 0;
        for(int i=1; i<trackers.size(); i++){
            if(trackers.get(i) == t){
                index = i;
            }
        }

        return index;
    }

    public void initParameters(Mat image, List<Point> leftEyes, List<Point>rightEyes,
                               List<Point> nose1, List<Point> mouth1){

        //initialized region parameters
        MatOfPoint mop1 = new MatOfPoint();
        mop1.fromList(leftEyes);

        MatOfPoint mop2 = new MatOfPoint();
        mop2.fromList(rightEyes);

        MatOfPoint mop3 = new MatOfPoint();
        mop3.fromList(nose1);

        MatOfPoint mop4 = new MatOfPoint();
        mop4.fromList(mouth1);

        Rect lEye = Imgproc.boundingRect(mop1);
        Rect rEye = Imgproc.boundingRect(mop2);
        Rect nose = Imgproc.boundingRect(mop3);
        Rect mouth = Imgproc.boundingRect(mop4);




        Rect2d r11 = new Rect2d(lEye.tl().x, lEye.tl().y, lEye.width, lEye.height);
        Rect2d r22 = new Rect2d(rEye.tl().x, rEye.tl().y, rEye.width, rEye.height);
        Rect2d r33 = new Rect2d(nose.tl().x, nose.tl().y, nose.width, nose.height);
        Rect2d r44 = new Rect2d(mouth.tl().x, mouth.tl().y, mouth.width, mouth.height);

        Tracker tracker1 =  getTracker(trackerTypes[tIndex]);
        Tracker tracker2 =  getTracker(trackerTypes[tIndex]);
        Tracker tracker3 =  getTracker(trackerTypes[tIndex]);
        Tracker tracker4 =  getTracker(trackerTypes[tIndex]);

        tracker1.init(image, r11);
        tracker2.init(image, r22);
        tracker3.init(image, r33);
        tracker4.init(image, r44);

        trackers.add(tracker1);
        trackers.add(tracker2);
        trackers.add(tracker3);
        trackers.add(tracker4);


        initializedParameters = true;
        Log.e("TAG", "Initial called "+initializedParameters);
    }

    private class processregions extends AsyncTask<Mat, Void, Void> {
        @Override
        protected Void doInBackground(Mat... mats) {
            Rect2d[] rect2ds = new Rect2d[4];
            Mat rgb = mats[0];

            long start = System.currentTimeMillis();

            //parallel threading for region trackers
            trackers.parallelStream().forEach(tracker -> {
                int index = getTrackerIndex(tracker);
                Rect2d r = new Rect2d();
                tracker.update(rgb, r);
                rect2ds[index] = r;
            }); //Tracking regions in parallel
            long end = System.currentTimeMillis();

            List<Double> d1 = new ArrayList<>();
            d1.add(rect2ds[0].tl().x);
            d1.add(rect2ds[0].br().x);
            d1.add(rect2ds[1].tl().x);
            d1.add(rect2ds[1].br().x);
            d1.add(rect2ds[2].tl().x);
            d1.add(rect2ds[2].br().x);
            d1.add(rect2ds[3].tl().x);
            d1.add(rect2ds[3].br().x);

            List<Double> d2 = new ArrayList<>();
            d2.add(rect2ds[0].tl().y);
            d2.add(rect2ds[0].br().y);
            d2.add(rect2ds[1].tl().y);
            d2.add(rect2ds[1].br().y);
            d2.add(rect2ds[2].tl().y);
            d2.add(rect2ds[2].br().y);
            d2.add(rect2ds[3].tl().y);
            d2.add(rect2ds[3].br().y);

            signals_horizontal.add(d1);
            signals_vertical.add(d2);

            Log.e("TAG", "Calling here?? time: " + (end - start) + " ms");

            if (regions.size() > 0)
                regions.clear();

            regions.addAll(Arrays.asList(rect2ds).subList(0, 4));
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(signals_vertical.size()%myThread.FPS == 0){
                MainActivity.time_display.setText(""+(signals_vertical.size()/myThread.FPS)+" s");
            }
        }
    }

    boolean startedComputingHR = false;

    private class computeHR extends AsyncTask<Void, Void, Integer> {


        @Override
        protected Integer doInBackground(Void... voids) {
            double[][] data = new double[signals_horizontal.size()][signals_horizontal.get(0).size()];

            for(int i=0; i<signals_horizontal.size(); i++){
                List<Double> s = signals_horizontal.get(i);
                for(int j=0; j<s.size(); j++){
                    data[i][j] = s.get(j);
                }
            }
            Log.e("TAG", "SIZE["+data.length+" "+data[0].length+"]");
            data = SignalProcessingClass.transpose(data);
            return AutoEncoderMethod.ComputeHR(data, 0.75, 2.0, myThread.FPS);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            startedComputingHR = false;
            signals_vertical.clear();
            signals_horizontal.clear();

            MainActivity.hr_display.setText(""+integer+" BPM");
        }
    }
}
