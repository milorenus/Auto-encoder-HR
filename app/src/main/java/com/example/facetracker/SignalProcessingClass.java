package com.example.facetracker;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.List;

import uk.me.berndporr.iirj.Butterworth;

public class SignalProcessingClass {
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "encoded";
    private static String OUTPUT_NAME1 = "code/Elu:0"; //output/BiasAdd:0
    private static String INPUT_NAME1 = "input:0";// "code/Identity:0"; //dense_input
    private static String OUTPUT_NAME2 = "code/Relu:0";
    private static final String WITHOUT_FILTER1 = "file:///android_asset/withoutFilter-face1.pb";
    private static final String WITH_FILTER1 = "file:///android_asset/withFilter-face1.pb";
    private static TensorFlowInferenceInterface wf, wtf;
    public static Butterworth butterworth = new Butterworth();
    static IcaMethodComputer ICA = new IcaMethodComputer();

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
        double out [] = new double[arr.length];

        for(int i=0; i<arr.length; i++)
            out[i] = arr[i];

        return out;
    }

    public static float [] doubleArrayToFloatArray(double [] arr){
        float out [] = new float[arr.length];

        for(int i=0; i<arr.length; i++)
            out[i] = (float) arr[i];

        return out;
    }

    public static  double[][] WithoutFencoder(double [][] data){
        double [][] out = new double[data.length][];
        checkArray(data, "encoder inside");
        for(int i=0; i<data.length; i++){
            out[i] = floatArrayToDoubleArray(without_filter_single(data[i]));

        }
        Log.e("TAG", "Inside: HR1 "+out.length+" "+data.length);
        return out;
    }/**/

    public static  double[][] WithFencoder(double [][] data){
        double [][] out = new double[data.length][];
        checkArray(data, "encoder inside");
        for(int i=0; i<data.length; i++){
            out[i] = floatArrayToDoubleArray(with_filter_single(data[i]));

        }
        Log.e("TAG", "Inside: HR1 "+out.length+" "+data.length);
        return out;
    }/**/

    static double mean(List<Double> list){
        double avg = 0;
        for(int i=0; i<list.size(); i++)
            avg += list.get(i);

        avg/= list.size();
        return avg;
    }

    public static List<List<Double>> divideList(int length, List<Double> list){
        List<List<Double>> out = new ArrayList<>();

        for(int i=0; i<list.size(); i+=length){
            List<Double> temp = new ArrayList<>();
            for(int j=i; j<(i+length); j++){
                temp.add(list.get(j));
            }
            out.add(temp);
        }
        return out;
    }

    public static double highestFreq(List<Double> list, double highPass, double lowPass, double frequency){
        //data: input data, must be spaced equally in time.
        //lowPass: The cutoff frequency at which
        //frequency: The frequency of the input data.
        double data[] = new double[list.size()];
        for(int i=0; i<data.length; i++){
            data[i] = list.get(i);
        }

        //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
        int minPowerOf2 = 1;
        while(minPowerOf2 < data.length)
            minPowerOf2 = 2 * minPowerOf2;

        //pad with zeros
        double[] padded = new double[minPowerOf2];
        for(int i = 0; i < data.length; i++)
            padded[i] = data[i];

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);

        Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

        double[] frequencyDomain = new double[fourierTransform.length];
        for(int i = 0; i < frequencyDomain.length; i++)
            frequencyDomain[i] = frequency * i / (double)fourierTransform.length;

        double [] mag = new double[fourierTransform.length];

        for(int i=0; i<mag.length; i++){
            mag[i] = fourierTransform[i].abs();
        }

        int maxInd = 0;

        for(int i=0; i<frequencyDomain.length; i++){
            if(frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass){
                if(maxInd == 0){
                    maxInd = i;
                }else{
                    if(mag[maxInd] < mag[i]){
                        maxInd = i;
                    }
                }
            }
        }


        return frequencyDomain[maxInd];
    }//highest frequency

    public static double highestFreqBis(List<Double> list, double highPass, double lowPass, double frequency){
        //data: input data, must be spaced equally in time.
        //lowPass: The cutoff frequency at which
        //frequency: The frequency of the input data.
        Log.e("TAG", "HR1 Welch data["+list.size()+"]");
        double data[] = new double[list.size()];
        for(int i=0; i<data.length; i++){
            data[i] = list.get(i);
        }

        //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
        int minPowerOf2 = 1;
        while(minPowerOf2 < data.length)
            minPowerOf2 = 2 * minPowerOf2;

        //pad with zeros
        double[] padded = new double[minPowerOf2];
        for(int i = 0; i < data.length; i++)
            padded[i] = data[i];

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

        double[] frequencyDomain = new double[fourierTransform.length];
        for(int i = 0; i < frequencyDomain.length; i++)
            frequencyDomain[i] = frequency * i / (double)fourierTransform.length;

        double [] mag = new double[fourierTransform.length];

        for(int i=0; i<mag.length; i++) {
            mag[i] = fourierTransform[i].abs();
        }

        powerSpectrum ps = new powerSpectrum(data.length/2, 2, frequency);
        ps.transform(data);
        double ps1 [] = ps.spectrum;

        int mxi = 0;
        for(int i=0; i<ps1.length; i++){
            if(frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass){
                if(mxi == 0){
                    mxi = i;
                }else{
                    if(ps1[mxi] < ps1[i]){
                        mxi = i;
                    }
                }
            }
        }


        return frequencyDomain[mxi];
    }//highest frequency bis

    public static double twoDominant(List<Double> list, double highPass, double lowPass, double frequency){
        //data: input data, must be spaced equally in time.
        //lowPass: The cutoff frequency at which
        //frequency: The frequency of the input data.
        double data[] = new double[list.size()];
        for(int i=0; i<data.length; i++){
            data[i] = list.get(i);
        }

        //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
        int minPowerOf2 = 1;
        while(minPowerOf2 < data.length)
            minPowerOf2 = 2 * minPowerOf2;

        //pad with zeros
        double[] padded = new double[minPowerOf2];
        for(int i = 0; i < data.length; i++)
            padded[i] = data[i];

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

        double[] frequencyDomain = new double[fourierTransform.length];
        for(int i = 0; i < frequencyDomain.length; i++)
            frequencyDomain[i] = frequency * i / (double)fourierTransform.length;

        double [] mag = new double[fourierTransform.length];

        for(int i=0; i<mag.length; i++){
            mag[i] = fourierTransform[i].abs();
        }

        int maxInd = 0, secondInd = 1;

        for(int i=0; i<frequencyDomain.length; i++){
            if(frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass){
                if(maxInd == 0){
                    if(mag[i]<mag[i+1]) {
                        maxInd = i+1;
                        secondInd = i;
                    }else {
                        maxInd = i;
                        secondInd = i+1;
                    }
                }else{
                    if(mag[maxInd] < mag[i]){
                        maxInd = i;
                    }else {
                        if(mag[secondInd] < mag[i]){
                            secondInd = i;
                        }
                    }
                }
            }
        }
        List<Double> re = new ArrayList<>();
        re.add(frequencyDomain[maxInd]); re.add(frequencyDomain[secondInd]);

        double periodicity = mag[maxInd] + mag[secondInd];
        double percent =0;

        for(int i=0; i<frequencyDomain.length; i++) {
            if (frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass && i!= maxInd && i!=secondInd) {
                percent += mag[i];
            }
        }


        return periodicity/percent;
    }//two dominents

    public static double twoDominantBis(List<Double> list, double highPass, double lowPass, double frequency){
        //data: input data, must be spaced equally in time.
        //lowPass: The cutoff frequency at which
        //frequency: The frequency of the input data.
        double data[] = new double[list.size()];
        for(int i=0; i<data.length; i++){
            data[i] = list.get(i);
        }

        //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
        int minPowerOf2 = 1;
        while(minPowerOf2 < data.length)
            minPowerOf2 = 2 * minPowerOf2;

        //pad with zeros
        double[] padded = new double[minPowerOf2];
        for(int i = 0; i < data.length; i++)
            padded[i] = data[i];

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

        double[] frequencyDomain = new double[fourierTransform.length];
        for(int i = 0; i < frequencyDomain.length; i++)
            frequencyDomain[i] = frequency * i / (double)fourierTransform.length;


        powerSpectrum ps = new powerSpectrum(data.length/2, 2, frequency);
        ps.transform(data);
        double ps1 [] = ps.spectrum;

        int maxInd = 0, secondInd = 1;

        for(int i=0; i<ps1.length; i++){
            if(frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass){
                if(maxInd == 0){
                    if(ps1[i]<ps1[i+1]) {
                        maxInd = i+1;
                        secondInd = i;
                    }else {
                        maxInd = i;
                        secondInd = i+1;
                    }
                }else{
                    if(ps1[maxInd] < ps1[i]){
                        maxInd = i;
                    }else {
                        if(ps1[secondInd] < ps1[i]){
                            secondInd = i;
                        }
                    }
                }
            }
        }
        List<Double> re = new ArrayList<>();
        re.add(frequencyDomain[maxInd]); re.add(frequencyDomain[secondInd]);

        double periodicity = ps1[maxInd] + ps1[secondInd];
        double percent =0;

        for(int i=0; i<ps1.length; i++) {
            if (frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass && i!= maxInd && i!=secondInd) {
                percent += ps1[i];
            }
        }


        return periodicity/percent;
    }//two dominents bis

    static double periodicity(List<Double> list, double highPass, double lowPass, double frequency){
        return twoDominant(list, highPass, lowPass, frequency);

    }

    static double periodicityBis(List<Double> list, double highPass, double lowPass, double frequency){
        return twoDominantBis(list, highPass, lowPass, frequency);

    }

    static double periodicityAuto(List<Double> list, double highPass, double lowPass, double frequency){
        double freq = highestFreq(list, highPass, lowPass, frequency);
        double period = frequency/freq;
        double auto = 0;
        int max =1;
        for(int i=1; i<=max; i++){
            List<Double> lagged = lagList(list, (int)period * i);
            double co = Correlation(listToArray(list), listToArray(lagged));
            auto += co;
        }

        return auto/max;
    }//autocorrelation periodicity

    static double periodicityAutoBis(List<Double> list, double highPass, double lowPass, double frequency){
        double freq = highestFreqBis(list, highPass, lowPass, frequency);
        double period = frequency/freq;
        double auto = 0;
        int max =1;
        for(int i=1; i<=max; i++){
            List<Double> lagged = lagList(list, (int)period * i);
            double co = Correlation(listToArray(list), listToArray(lagged));
            auto += co;
        }

        return auto/max;
    }//autocorelation periodicity bis

    static List<Double> lagList(List<Double> in, int lag){
        List<Double> out; // = new ArrayList<>();
        double [] arr = new double[in.size()];
        for(int i=0; i<in.size(); i++){
            arr[i] = in.get((i+lag)%in.size());
        }
        out = arrayToList(arr);
        return out;
    }
    static int maxPeriods(List<List<Double>> list, double highPass, double lowPass, double frequency){
        double pers [] = new double[list.size()];
        for(int i=0; i<list.size(); i++){
            pers[i] = periodicity(list.get(i), highPass, lowPass, frequency);
        }
        int max = 0;
        for(int i=1;i<pers.length;i++){
            if(pers[i]>pers[max]){
                max = i;
            }
        }
        return max;
    }// maximum periodicity

    static int maxPeriodsBis(List<List<Double>> list, double highPass, double lowPass, double frequency){
        double pers [] = new double[list.size()];
        for(int i=0; i<list.size(); i++){
            pers[i] = periodicityBis(list.get(i), highPass, lowPass, frequency);
        }
        int max = 0;
        for(int i=1;i<pers.length;i++){
            if(pers[i]>pers[max]){
                max = i;
            }
        }
        return max;
    }// maximum priodicity bis

    public static double[] fourierLowPassFilter(double[] data, double highPass, double lowPass, double frequency){
        //data: input data, must be spaced equally in time.
        //lowPass: The cutoff frequency at which
        //frequency: The frequency of the input data.

        //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
        int minPowerOf2 = 1;
        while(minPowerOf2 < data.length)
            minPowerOf2 = 2 * minPowerOf2;

        //pad with zeros
        double[] padded = new double[minPowerOf2];
        for(int i = 0; i < data.length; i++)
            padded[i] = data[i];


        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

        //build the frequency domain array
        double[] frequencyDomain = new double[fourierTransform.length];
        for(int i = 0; i < frequencyDomain.length; i++)
            frequencyDomain[i] = frequency * i / (double)fourierTransform.length;

        //build the classifier array, 2s are kept and 0s do not pass the filter
        double[] keepPoints = new double[frequencyDomain.length];
        keepPoints[0] = 1;
        for(int i = 1; i < frequencyDomain.length; i++){
            if(frequencyDomain[i] < lowPass && frequencyDomain[i] > highPass)
                keepPoints[i] = 2;
            else
                keepPoints[i] = 0;
        }

        //filter the fft
        for(int i = 0; i < fourierTransform.length; i++)
            fourierTransform[i] = fourierTransform[i].multiply((double)keepPoints[i]);

        //invert back to time domain
        Complex[] reverseFourier = transformer.transform(fourierTransform, TransformType.INVERSE);

        //get the real part of the reverse
        double[] result = new double[data.length];
        for(int i = 0; i< result.length; i++){
            result[i] = reverseFourier[i].getReal();
        }

        return result;
    }// fourier filter

    static List<Double> arrayToList(double arr[]){
        List<Double> doubles = new ArrayList<>();
        for(int i=0; i<arr.length; i++){
            doubles.add(arr[i]);
        }
        return doubles;
    }

    static double [] listToArray(List<Double> list){
        double arr[] = new double[list.size()];
        for(int i=0; i<list.size(); i++){
            arr[i] = list.get(i);
        }
        return arr;
    }

    static List<Float> doubleArrayToFloatList(double [] arr){
        List<Float> list = new ArrayList<>();
        for(int i=0; i<arr.length; i++){
            double d = arr[i];
            list.add((float)d);
        }

        return list;
    }

    static List<Integer> getOurHRs(double [][] data, double highPass, double lowPass, double freq, List<Integer> HRs){
        List<Integer> hrs = new ArrayList<>();

        for(int i=0; i<data.length; i++){
            List<Float> list = doubleArrayToFloatList(data[i]);
            Log.e("TAG", "LOGGER count: "+i+" size: "+list.size());
            int hr = ourAlgorithm.getHR(list, lowPass, highPass, freq/*, HRs.get(i)*/);
            hrs.add(hr);
        }

        return hrs;
    }

    static double mean(double [] list){
        double average = 0;
        for(int i=0; i<list.length; i++){
            average+= list[i];
        }
        return average/list.length;
    }

    static double [] adjust(double [] arr){
        double re [] = new double[arr.length];
        double average = mean(arr);
        for(int i=0; i<arr.length; i++){
            re[i] = arr[i] - average;
        }
        return re;
    }

    static double [][] filterData(double [][] data, double highPass, double lowPass, double freq){
        double [][] fill = new double[data.length][];
        for(int i=0; i<data.length; i++){
            fill[i] = fourierLowPassFilter(data[i], highPass, lowPass, freq);
        }
        return fill;
    }

    static double [][] adjustData(double [][] data){
        double [][] fill = new double[data.length][];
        for(int i=0; i<data.length; i++){
            fill[i] = adjust(data[i]);
        }
        return fill;
    }
    static  double time1 = 0;

    static double  [][] transpose(double [][] a){
        double[][] out = new double[a[0].length][a.length];

        for(int i=0; i<out.length; i++){
            for(int j=0; j<out[0].length; j++){
                out[i][j] = a[j][i];
            }
        }
        return out;
    }

    static void checkArray(double [][] arr, String name){
        if(arr == null || arr.length == 0 || arr[0].length == 0){
            Log.e("TAG", "HR1 See data : NULL ARRAY -> "+name);
        }else {
            Log.e("TAG", "HR1 See data : "+arr.length+" - "+arr[0].length+" name: "+name);
        }
    }

    static double [][] copyArray(double [][] arr){
        double [][] out = new double[arr.length][arr[0].length];

        for(int i=0; i<arr.length; i++){
            for(int j=0; j<arr[i].length; j++){
                out[i][j] = arr[i][j];
            }
        }
        return out;
    }

    public static double [] butterWorthFilderSingle(double [] data){
        double [] out = new double[data.length];
        for(int i=0; i<data.length; i++){
            out[i] = butterworth.filter(data[i]);
        }
        return out;
    }

    public static double [][] butterWorthFilterDouble(double [][] data){
        double [][] out = new double[data.length][];
        for(int i=0; i<data.length; i++){
            out[i] = butterWorthFilderSingle(data[i]);
        }

        return out;
    }

    static double Correlation(double [] arr1, double [] arr2){
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        return correlation.correlation(arr1, arr2);
    }

    static double [][] computePCA(double [][] data){
        Mat data1 = new Mat(data[0].length, data.length, CvType.CV_64FC1);
        Log.e("PCA", "Input width "+data.length);

        for(int i=0; i<data1.rows(); i++){
            for(int j=0; j<data1.cols(); j++) {
                data1.put(i, j, data[j][i]);
            }
        }
        Log.e("PCA", " width "+data1.cols()+" height "+data1.rows());
        Mat mean = new Mat(), vectors = new Mat();
        Core.PCACompute(data1, mean, vectors, data.length);
        Mat result = new Mat(data[0].length, data.length, CvType.CV_64FC1);
        Core.PCAProject(data1, mean, vectors, result);
        Log.e("PCA", " width "+vectors.cols()+" height "+vectors.rows());
        Log.e("PCA", "projected width "+result.cols()+" height "+result.rows());
        double [][] out = new double[result.cols()][result.rows()];

        for(int i=0; i<result.cols(); i++){
            for(int j=0; j<result.rows(); j++){
                double [] don = result.get(j, i);
                out[i][j] = don[0];
            }
        }
        return out;
    }

    static List<Integer> getHRS(double [][] components, double highPass, double lowPass, double freq, int max){
        List<Integer> freqs = new ArrayList<>();
        if(max>components.length)
            max = components.length;

        for(int i =0; i<max; i++){
            double fr = highestFreq(arrayToList(components[i]), highPass, lowPass, freq);
            freqs.add((int)(60*fr));
        }
        return freqs;
    }
    static List<Integer> getHRSBis(double [][] components, double highPass, double lowPass, double freq, int max){
        List<Integer> freqs = new ArrayList<>();
        if(max>components.length)
            max = components.length;

        for(int i =0; i<max; i++){
            double fr = highestFreqBis(arrayToList(components[i]), highPass, lowPass, freq);
            freqs.add((int)(60*fr));
        }
        return freqs;
    }

    public static double[] getMag(double [] data, double highPass, double lowPass, double frequency) {
        //data: input data, must be spaced equally in time.
        //lowPass: The cutoff frequency at which
        //frequency: The frequency of the input data.

        //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
        int minPowerOf2 = 1;
        while (minPowerOf2 < data.length)
            minPowerOf2 = 2 * minPowerOf2;

        //pad with zeros
        double[] padded = new double[minPowerOf2];
        for (int i = 0; i < data.length; i++)
            padded[i] = data[i];

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

        double[] frequencyDomain = new double[fourierTransform.length];
        for (int i = 0; i < frequencyDomain.length; i++)
            frequencyDomain[i] = frequency * i / (double) fourierTransform.length;

        double[] mag = new double[fourierTransform.length];

        for (int i = 0; i < mag.length; i++) {
            mag[i] = fourierTransform[i].abs();
        }
        return mag;
    }

    static List<List<Double>> douleArrayToListOfLists(double [][] arr, int max){
        List<List<Double>> loL= new ArrayList<>();
        if(max>arr.length){
            max= arr.length;
        }

        for(int i=0; i<max; i++){
            List<Double> list = new ArrayList<>();
            for(int j=0; j<arr[i].length; j++){
                list.add(arr[i][j]);
            }
            loL.add(list);
        }
        return loL;
    }




    //main computing function
    static List<List<Integer>> processData(double [][] arr, double [][] arr1, double [][] c/*List<Double>[] data, List<Double>[] data1*/, double highPass, double lowPass, double freq){
        double centerFreq = highPass + (lowPass - highPass)/2;
        double widthFreq = lowPass - highPass;
        butterworth.bandPass(5, freq, centerFreq, widthFreq);

        wf = new TensorFlowInferenceInterface(heartrateView.context.getAssets(), WITH_FILTER1);
        wtf = new TensorFlowInferenceInterface(heartrateView.context.getAssets(), WITHOUT_FILTER1);

        double [][] copy = copyArray(arr);
        arr = transpose(arr);
        arr1 = transpose(arr1);
        c = transpose(c);




        long ss1 = System.currentTimeMillis();
        arr =  filterData(arr, highPass, lowPass, freq);// butterWorthFilterDouble(arr);
        long ss2 = System.currentTimeMillis();
        arr1 = filterData(arr, highPass, lowPass, freq);// butterWorthFilterDouble(arr1);


        double [][] before_filter = WithoutFencoder(copy);
        before_filter = filterData(transpose(before_filter), highPass, lowPass, freq);// butterWorthFilterDouble(transpose(before_filter));
        before_filter = transpose(before_filter);

        long ss3 = System.currentTimeMillis();
        double [][] after_filter = WithFencoder(transpose(arr));
        long ss4 = System.currentTimeMillis();

        before_filter = transpose(before_filter);
        after_filter = transpose(after_filter);

        double [][] c1 = copyArray(arr);
        RealMatrix rm1 = MatrixUtils.createRealMatrix(c1);
        RealMatrix rm2 = ICA.compute(rm1);
        RealMatrix rm3 = rm2.multiply(rm1);
        c1 = rm3.getData();

        List<Integer> ica_based = getHRS(c1, highPass, lowPass, freq, c1.length);

        Log.e("TAG", "Testing size after["+after_filter.length+" "+after_filter[0].length+"]");
        Log.e("TAG", "Testing size before["+before_filter.length+" "+before_filter[0].length+"]");

        List<Integer> pre = getHRS(arr, highPass, lowPass, freq, arr.length);
        List<Integer> pre1 = getHRS(arr1, highPass, lowPass, freq, arr.length);

        List<Integer> WTF = getHRS(before_filter, highPass, lowPass, freq, before_filter.length);
        long ss5 = System.currentTimeMillis();
        List<Integer> WTF1 = getOurHRs(before_filter, highPass, lowPass, freq, WTF);
        long ss6 = System.currentTimeMillis();

        List<Integer> WF = getHRS(after_filter, highPass, lowPass, freq, before_filter.length);
        List<Integer> WF1 = getOurHRs(after_filter, highPass, lowPass, freq, WTF);

        Log.e("TAG", "HR1 comp filtering: "+(ss2-ss1)+" ms");
        Log.e("TAG", "HR1 comp encoding: "+(ss4-ss3)+" ms");
        Log.e("TAG", "HR1 comp heartrate comp: "+(ss6-ss5)+" ms");


        String s = "", s1 = "";
        for(int i=0; i<pre.size(); i++){
            if(i+1 == pre.size()){
                s += ""+pre.get(i);
            }else{
                s+= pre.get(i)+" -- ";
            }
        }

        for(int i=0; i<pre.size(); i++){
            if(i+1 == pre.size()){
                s1 += ""+pre1.get(i);
            }else{
                s1+= pre1.get(i)+" -- ";
            }
        }

        Log.e("sss", "pre poccess: "+s);
        Log.e("sss", "pre poccess: "+s1);

        long step3 = System.currentTimeMillis();
        arr = adjustData(arr);
        Log.e("[-data-]" , ""+arr.length+" -- "+arr[0].length);
        // arr = transpose(arr);
        double pca [][] =  computePCA(arr); //getPCA(arr);

        double [][] printPCA = new double[5][];
        double [][] printMag = new double[5][];
        for(int i=0; i<5; i++){
            printPCA[i] = pca[i];
            printMag[i] = getMag(pca[i], highPass, lowPass, freq);
        }

        List<Integer> rep1 = getHRS(pca, highPass, lowPass, freq, 5);
        //Log.e("TAG", "LOGGER("+toEncode.length+","+toEncode[0].length+")");

        double correlations [] = new double[5];
        //double Autocorrelations [] = new double[5];
        // Log.e("sss", "moves size: "+movesSignal.size());
        int max_1 =0, max_2 =0, max_3 =0;
        for(int i=0; i<3;/*5->3*/ i++) {
            if(correlations[i]>correlations[max_1]){
                max_1 = i;
            }
            //    Log.e("sss", "Correlation(" + i + ") : " + correlations[i]);
        } /* */

        arr1 = adjustData(arr1);
        double pca1 [][] = computePCA(arr1);
        double [][] printMag1 = new double[5][];
        // arr1 = transpose(arr1);
        // double pca1 [][] = getPCA(arr1);
        double [][] printPCA1 = new double[5][];

        for(int i=0; i<5; i++){
            printPCA1[i] = pca1[i];
            printMag1[i] = getMag(pca1[i], highPass, lowPass, freq);
        }


        for(int i=0; i<3;/*5->3*/ i++) {
            if(correlations[i]>correlations[max_2]){
                max_2 = i;
            }
            Log.e("sss", "Correlation(" + i + ") : " + correlations[i]);
        }

        for(int i=0; i<5; i++){
            // correlations[i] = Correlation(adjust(listToArray(movesSignal)), adjust(pca[i]));
            //Autocorrelations[i] = AutoCorrelation(adjust(adjust(pca[i])));
            double [] temp_pca = adjust(pca1[i]);
            List<Double> list = arrayToList(temp_pca);
            correlations[i] = periodicityAutoBis(list, highPass, lowPass, freq);
        }

        int mx = 5;
        if(mx>pca1.length)
            mx = 5;

        double corr [] = new double[mx];

        for(int i=0; i<mx; i++){
            double rec = periodicity(arrayToList(pca1[i]), highPass, lowPass, freq);
            double scale = correlations[i]/rec;
            corr[i] = 0.5 * correlations[i] * scale + 0.5 * rec;
        }

        int max_comb = 0;

        for(int i=0; i<corr.length; i++) {
            if(corr[i]>corr[max_comb]){
                max_comb = i;
            }
            Log.e("sss", "Correlation(" + i + ") : " + correlations[i]);
        }

        for(int i=0; i<5; i++) {
            if(correlations[i]>correlations[max_3]){
                max_3 = i;
            }
            Log.e("sss", "Correlation(" + i + ") : " + correlations[i]);
        }

        List<Integer> rep2 = getHRS(pca1, highPass, lowPass, freq, 5);
        List<Integer> repp = getHRSBis(pca1, highPass, lowPass, freq, 5);

        for(int i=0; i<rep2.size(); i++){
            Log.e("[cmp]", " compare fft: "+rep2.get(i)+" BPM welch: "+repp.get(i)+" BPM");
        }

        int max1 = maxPeriods(douleArrayToListOfLists(pca,5), highPass, lowPass, freq);
        int max2 = maxPeriods(douleArrayToListOfLists(pca1, 5), highPass, lowPass, freq);

        int maxw1 = maxPeriodsBis(douleArrayToListOfLists(pca,5), highPass, lowPass, freq);
        int maxw2 = maxPeriodsBis(douleArrayToListOfLists(pca1, 5), highPass, lowPass, freq);

        Log.e("sss", "HR [MAX PERIODICITY]: P1:    "+max1+" -- "+max2);
        Log.e("sss", "HR [MAX PERIODICITY]: Weclh: "+maxw1+" -- "+maxw2);
        Log.e("sss", "HR [MAX PERIODICITY]: P2:    "+max_1+" -- "+max_2+" -- "+max_3);
        Log.e("sss", "HR [Max COMBINATION]: P1&P2: "+max_comb);


        Log.e("TAG", "HR1====================================================");
        for(int i=0; i<WTF.size(); i++){
            Log.e("TAG", "HR1 Keras NEW Without Filter no DO: "+WTF.get(i)+" BPM");
        }
        Log.e("TAG", "HR1====================================================");
        for(int i=0; i<WTF1.size(); i++){
            Log.e("TAG", "HR1 Keras NEW Without Filter ours no DO: "+WTF1.get(i)+" BPM");
        }
        Log.e("TAG", "HR1====================================================");
        for(int i=0; i<WF.size(); i++){
            Log.e("TAG", "HR1 Keras NEW With Filter no DO: "+WF.get(i)+" BPM");
        }
        Log.e("TAG", "HR1====================================================");
        for(int i=0; i<WF1.size(); i++){
            Log.e("TAG", "HR1 Keras NEW With Filter ours no DO: "+WF1.get(i)+" BPM");
        }

        List<List<Integer>> loL = new ArrayList<>();
        real_HR = WTF1.get(0);
        loL.add(pre); loL.add(pre1); loL.add(rep1); loL.add(rep2); //loL.add(pre2);
        return loL;
    }
    static int real_HR;

}
