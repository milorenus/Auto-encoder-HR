package com.example.facetracker;

import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.List;

import static com.example.facetracker.SignalProcessingClass.checkArray;
import static com.example.facetracker.SignalProcessingClass.copyArray;
import static com.example.facetracker.SignalProcessingClass.filterData;
import static com.example.facetracker.SignalProcessingClass.getHRS;
import static com.example.facetracker.SignalProcessingClass.getOurHRs;
import static com.example.facetracker.SignalProcessingClass.transpose;


public class AutoEncoderMethod {
    private static final String INPUT_NAME1 = "input:0";// "code/Identity:0"; //dense_input
    private static final String OUTPUT_NAME2 = "code/Relu:0";
    private static final String WITHOUT_FILTER1 = "file:///android_asset/withoutFilter-face1.pb";
    private static final String WITH_FILTER1 = "file:///android_asset/withFilter-face1.pb";
    private static TensorFlowInferenceInterface wf, wtf;
    public static float [] without_filter_single(double [] inputs){
        float [] res = {0};


        float [] inner = doubleArrayToFloatArray(inputs);
        Log.e("TAG", "HR Array size: ["+inner.length+"]");
        wtf.feed(INPUT_NAME1, inner, 1, 8);
        wtf.run(new String[]{OUTPUT_NAME2});
        wtf.fetch(OUTPUT_NAME2, res);



        // float res1[] = {res_x[0], res_y[0]  , res_z[0]};

        return res;
    }

    public static float [] with_filter_single(double [] inputs){
        float [] res = {0};


        float [] inner = doubleArrayToFloatArray(inputs);
        Log.e("TAG", "HR Array size: ["+inner.length+"]");
        wf.feed(INPUT_NAME1, inner, 1, 8);
        wf.run(new String[]{OUTPUT_NAME2});
        wf.fetch(OUTPUT_NAME2, res);



        // float res1[] = {res_x[0], res_y[0]  , res_z[0]};

        return res;
    }

    public static double [] floatArrayToDoubleArray(float [] arr){
        double[] out = new double[arr.length];

        for(int i=0; i<arr.length; i++)
            out[i] = arr[i];

        return out;
    }

    public static float [] doubleArrayToFloatArray(double [] arr){
        float[] out = new float[arr.length];

        for(int i=0; i<arr.length; i++)
            out[i] = (float) arr[i];

        return out;
    }

    public static  double[][] WithoutFencoder(double [][] data){
        if(data.length<data[0].length)
            data = transpose(data);

        double [][] out = new double[data.length][];
        checkArray(data, "encoder inside");
        for(int i=0; i<data.length; i++){
            out[i] = floatArrayToDoubleArray(without_filter_single(data[i]));

        }
        Log.e("TAG", "Inside: HR1 "+out.length+" "+data.length);
        return out;
    }

    public static  double[][] WithFencoder(double [][] data){
        if(data.length<data[0].length)
            data = transpose(data);

        double [][] out = new double[data.length][];
        checkArray(data, "encoder inside");
        for(int i=0; i<data.length; i++){
            out[i] = floatArrayToDoubleArray(with_filter_single(data[i]));

        }
        Log.e("TAG", "Inside: HR1 "+out.length+" "+data.length);
        return out;
    }

    //main computing function
    static int ComputeHR(double [][] arr, double highPass, double lowPass, double freq) {

        wf = new TensorFlowInferenceInterface(heartrateView.context.getAssets(), WITH_FILTER1);
        wtf = new TensorFlowInferenceInterface(heartrateView.context.getAssets(), WITHOUT_FILTER1);
        double [][] copy = copyArray(arr);

        arr = transpose(arr); //transpose
        arr =  filterData(arr, highPass, lowPass, freq); //signal filtering

        double [][] before_filter = WithoutFencoder(copy); //apply the auto-encoder(AE) to unfiltered signal
        before_filter = filterData(transpose(before_filter), highPass, lowPass, freq); //firlter the signal from the AE
        //before_filter = transpose(before_filter);

        double [][] after_filter = WithFencoder(transpose(arr));//apply the AE to the filtered signal

        after_filter = transpose(after_filter);

        Log.e("TAG", "SHAPE["+after_filter.length+" "+after_filter[0].length+"]");
        Log.e("TAG", "SHAPE["+before_filter.length+" "+before_filter[0].length+"]");

        List<Integer> WTF = getHRS(before_filter, highPass, lowPass, freq, before_filter.length);
        List<Integer> WTF1 = getOurHRs(before_filter, highPass, lowPass, freq, WTF);


        List<Integer> WF = getHRS(after_filter, highPass, lowPass, freq, before_filter.length);
        List<Integer> WF1 = getOurHRs(after_filter, highPass, lowPass, freq, WTF);

        return WF1.get(0);
    }

}
