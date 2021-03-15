package com.example.facetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2  {
    private static final int  cameraCode = 0;
   // private boolean mUserRequestedInstall = true;
    String TAG  = "Auto-encoder";

    cameraView cam;
    Mat gray, rgb;
    static TextView hr_display, time_display;
    Button start;

    AEHRView hrv;
    Context context;
    FirebaseVisionFaceDetector detector;

    boolean  canDetect = false, didInitFaceFeatures=false, canInitTracker = false;

    static boolean didSetParameters = false;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        cam = findViewById(R.id.cameView);

        //checkPermissionAndAsk();
        hr_display = findViewById(R.id.hr_hest);  //display the heart rate
        time_display = findViewById(R.id.time_count);//display the time (~25 seconds)
        start = findViewById(R.id.start); //button to start the system
        hrv = findViewById(R.id.hrViewAE);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                canInitTracker = true;
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
          checkPermissionAndAsk(); //check camera permission
        }

    }

    public void enableCamera(){
        //cam.setDisplayOrientation(90);
        loadTracker();
        cam.enableView();
        cam.setCameraIndex(1);
        cam.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        setRes();
       // checkAr();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if(rgb!=null)
            rgb.release();

        if(gray!=null)
            gray.release();


        rgb = inputFrame.rgba();
        gray = inputFrame.gray().clone();

        //Core.rotate(rgb, rgb, Core.ROTATE_90_CLOCKWISE);
        Core.flip(rgb, rgb, 1);

        if(hrv.frame!=null)
            hrv.frame.release();

        hrv.frame = rgb.clone(); //the frame that is processed for regions tracking

        if(!processing && !canDetect && !didSetParameters){
            //firebase regions tracking is slow so we run it in a worker thread
            workerFB w = new workerFB();
            w.execute(rgb.clone());
            //  Log.e("TAG", "FACES being called??");
        }/* */

        if( detectedFaces!=null && detectedFaces.size()>0 && !didInitFaceFeatures && !didSetParameters){
            for (FirebaseVisionFace face : detectedFaces) {

                android.graphics.Rect r = face.getBoundingBox();
                Point tl = new Point(r.left, r.top);
                Point br = new Point(r.right, r.bottom);
                tl = transformPoint(tl, rgb.height());
                br = transformPoint(br, rgb.height());
                Imgproc.rectangle(rgb, tl, br, new Scalar(0, 255, 0), 4);



                List<FirebaseVisionPoint> leftEyeCBrows = face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).getPoints();
                List<FirebaseVisionPoint> leftEyeCBrowsB = face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM).getPoints();
                List<FirebaseVisionPoint> leftEye = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
                List<FirebaseVisionPoint> rightEyeCBrowsB = face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM).getPoints();
                List<FirebaseVisionPoint> rightEyeCBrows = face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP).getPoints();
                List<FirebaseVisionPoint> rightEye = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints();
                List<FirebaseVisionPoint> nose1 =  face.getContour(FirebaseVisionFaceContour.NOSE_BRIDGE).getPoints();
                List<FirebaseVisionPoint> nose2 =  face.getContour(FirebaseVisionFaceContour.NOSE_BOTTOM).getPoints();
                List<FirebaseVisionPoint> topLip =  face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints();
                List<FirebaseVisionPoint> bottomLip =  face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints();
                List<FirebaseVisionPoint> faceContour = face.getContour(FirebaseVisionFaceContour.FACE).getPoints();

                Log.e("TAG", "infos: leftEye: "+leftEyeCBrows.size());
                Log.e("TAG", "infos: righttEye: "+rightEyeCBrows.size());
                Log.e("TAG", "infos: nose1: "+nose1.size());
                Log.e("TAG", "infos: nose2: "+nose2.size());
                Log.e("TAG", "infos: topLip: "+topLip.size());
                Log.e("TAG", "infos: bottomLip: "+bottomLip.size());
                Log.e("TAG", "infos: facial: "+faceContour.size());

                drawContour(leftEyeCBrows, rgb);
                drawContour(rightEyeCBrows, rgb);
                drawContour(leftEyeCBrowsB, rgb);
                drawContour(rightEyeCBrowsB, rgb);
                drawContour(nose1, rgb);
                drawContour(nose2, rgb);
                drawContour(topLip, rgb);
                drawContour(bottomLip, rgb);
                drawContour(faceContour, rgb);
                //drawContour(faceContour, rgb);

                if(!didSetParameters && canInitTracker){
                    //List<FirebaseVisionPoint> sum = concatList(leftEyeCBrows, leftEyeCBrowsB);
                    List<FirebaseVisionPoint> lEye1 = concatList(leftEye, leftEyeCBrows);
                    List<FirebaseVisionPoint> rEye1 = concatList(rightEye, rightEyeCBrows);
                    List<FirebaseVisionPoint> Nose1 = concatList(nose1, nose2);
                    List<FirebaseVisionPoint> mouth1 = concatList(topLip, bottomLip);

                    List<Point> lEye = convertContour(lEye1, rgb);
                    List<Point> rEye = convertContour(rEye1, rgb);
                    List<Point> Nose = convertContour(Nose1, rgb);
                    List<Point> mouth = convertContour(mouth1, rgb);
                    hrv.initParameters(rgb.clone(), lEye, rEye, Nose, mouth);
                    //setParameters(initialMat.getNativeObjAddr(), tl.x, tl.y, br.x, br.y);
                    //initParameters(initialMat.getNativeObjAddr()/*, CameraListener1.currentBack.getNativeObjAddr()*/, convertToArray(sum, rgb), convertToArray(faceContour, rgb));
                    didSetParameters = true;
                }
            }
        }

        if(didSetParameters && hrv.regions.size()>0){
            Log.e("TAG", "here??? drawing");
            for(int i=0; i<hrv.regions.size(); i++){
                Rect2d r = hrv.regions.get(i);
                Imgproc.rectangle(rgb, r.tl(), r.br(), new Scalar(255, 0, 255), 3);
            }
        }



        return rgb;
    }

    public  void loadTracker(){
        FirebaseApp.initializeApp(context);

        FirebaseVisionFaceDetectorOptions realTimeOpts = new FirebaseVisionFaceDetectorOptions.Builder()
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                .build();
        detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(realTimeOpts);
    }

    //C++ function
    public native String stringFromJNI();


    public void checkPermissionAndAsk()  {
        Log.e("TAG", "will ask for permission");
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED) {
            Log.e("TAG", "permission is granted?");
            // Search_Dir(new File("mnt/sdcard"));
            //showButtons();
            enableCamera();
        }else {
            Log.e("TAG", "Permission not granted ... asking");
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
                Toast.makeText(MainActivity.this, "Please grant the requested permission to get your task done!", Toast.LENGTH_LONG).show();

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, cameraCode);
            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, cameraCode);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.e("TAG", "Asking? ...");
        switch (requestCode) {
            case cameraCode:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission with request code 1 granted
                    // Search_Dir(new File("mnt/sdcard"));
                    //showButtons();
                    enableCamera();
                } else {
                    //permission with request code 1 was not granted
                    // Toast.makeText(this, "Permission was not Granted" , Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        System.exit(0);
    }

    public void setRes(){
        // didsetRes =true;
        List<Camera.Size> mResolutionList = cam.getResolutionList();
        int mid = mResolutionList.size()/2;
        int w1 = 800;
        int diff = 800;
        Camera.Size size = mResolutionList.get(0);
        for(int i = 0; i< mResolutionList.size(); i++){
            Camera.Size size1 = mResolutionList.get(i);
            Log.e("[res front]", "("+size.width+", "+size.height+")");
            int diff1 = Math.abs(w1 - size1.width);
            if(diff1<diff){
                diff = diff1;
                size = size1;
            }
        }

        Log.e("[res]", "HR front Res ("+size.height+", "+size.width+")");
        cam.setResolution(size);

    }



//--------------------------------face tracking handling parts---------------------------
public void drawContour(List<FirebaseVisionPoint> contour, Mat img){
    List<Point> c = convertContour(contour, img);

    for(int i=0; i<c.size(); i++){
        Imgproc.circle(img, c.get(i), 4, new Scalar(255, 0, 255), -1);
        Imgproc.line(img, c.get(i), c.get((i+1)%c.size()), new Scalar(0, 255, 0), 3);
    }
}

    List<FirebaseVisionPoint> concatList(List<FirebaseVisionPoint> a, List<FirebaseVisionPoint> b){
        List<FirebaseVisionPoint> out = new ArrayList<>();

        for(int i=0; i<a.size(); i++)
            out.add(a.get(i));

        for(int i=0; i<b.size(); i++)
            out.add(b.get(i));

        return out;
    }

    float [][] convertToArray(List<FirebaseVisionPoint> contour, Mat img){
        List<Point> out = new ArrayList<>();

        for(int i=0; i<contour.size(); i++){
            Point p = transformPoint(new Point(contour.get(i).getX(), contour.get(i).getY()), img.height());
            out.add(p);
        }

        float [][] arr = new float[out.size()][2];

        for(int i=0; i<out.size(); i++){
            arr[i][0] = (float) out.get(i).x;
            arr[i][1] = (float)  out.get(i).y;
        }

        return arr;
    }

    public List<Point> convertContour( List<FirebaseVisionPoint> contour, Mat img){
        List<Point> out = new ArrayList<>();

        for(int i=0; i<contour.size(); i++){
            Point p = transformPoint(new Point(contour.get(i).getX(), contour.get(i).getY()), img.height());
            out.add(p);
        }

        return out;
    }


    Bitmap matToBitmap(Mat m){
        Bitmap bmp = null;
        Mat tmp = new Mat (m.rows(), m.cols(), CvType.CV_8U, new Scalar(4));
        try {
            Imgproc.cvtColor(m, tmp, Imgproc.COLOR_RGB2RGBA);
            // Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(m, bmp);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}
        return bmp;
    }

    Point transformPoint(Point p, double height){
        Point out = new Point(p.y, p.x);
        double half =  height/2;
        double diff = half - out.y;
        out.y += 2*diff;

        return out;
    }

    Mat initialMat;
    boolean processing = false;
    List<FirebaseVisionFace> detectedFaces;
    class workerFB extends AsyncTask<Object, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            processing = true;
        }

        @Override
        protected Void doInBackground(Object... objects) {

            final Mat m = (Mat) objects[0];
            Log.e("TAG", "BEFORE FACE SIZE("+m.size().width+", "+m.size().height+")");
            //flipTranspose(m.getNativeObjAddr());
            Mat mat = new Mat(new org.opencv.core.Size(m.height(), m.width()), m.type());
            Core.flip(m.t(), mat, 1);
            Bitmap img = matToBitmap(mat);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(img);
            Log.e("TAG", "AFTER FACE SIZE("+mat.size().width+", "+mat.size().height+")");

            long start = System.currentTimeMillis();
            Task<List<FirebaseVisionFace>> result = detector.detectInImage(image).addOnSuccessListener(
                    new OnSuccessListener<List<FirebaseVisionFace>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionFace> faces) {
                            if(faces.size()>0) {
                                detectedFaces = faces;
                                initialMat = m;
                            }
                            processing = false;
                            Log.e(":APP", "FACES: SOME FACES DETECTED! "+faces.size());
                        }
                    })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    processing = false;
                                    Log.e(":APP", "FACES: NO FACE DETECTED!");
                                }
                            });
            long end = System.currentTimeMillis();
            // Log.e("TAG", "FACES time: "+(end - start)+" ms");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //  processing = false;
        }
    }/**/
}