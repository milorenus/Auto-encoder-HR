package com.example.facetracker;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.JavaCameraView;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by johnpeterlomaliza on 9/21/17.
 */

public class cameraView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "Sample::Tutorial3View";
    private String mPictureFileName;

    public cameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
       // mCamera.setDisplayOrientation(90);
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public List<String> getWBList(){
        return mCamera.getParameters().getSupportedWhiteBalance();
    }

    public String getWB(){
        List<String> wb = mCamera.getParameters().getSupportedWhiteBalance();
        mCamera.getParameters().setWhiteBalance(wb.get(1));
        return mCamera.getParameters().getWhiteBalance();
    }

    public void setFocusMode(Context item, int type){
        Camera.Parameters params = mCamera.getParameters();
        mCamera.cancelAutoFocus();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {

            }
        });

        List<String> focusModes = params.getSupportedFocusModes();
        switch (type){
            case 0:
                if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                else
                    Toast.makeText(item, "autofocus not supported", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else
                    Toast.makeText(item, "continuous video not supported", Toast.LENGTH_SHORT).show();
                break;
        }



        mCamera.setParameters(params);

    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
        //mCamera.setDisplayOrientation(0);
    }
    public void setFocus(){

    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
}