package com.example.facetracker;

import android.graphics.Point;
import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by johnpeterlomaliza on 2/3/18.
 */

public class ourAlgorithm {
    static double FPS = 20.0;
    static float minVar = 1000.0f;
    static float bestMean = 0.0f;
    public void computeHR(List<Float> signal){

    }

    public static List<Float> getSignal(List<Float> signal, int size, int code){
        if(code == 1){

            List<Float> begin = new ArrayList<>();

            for(int i=0; i<size; i++){
                begin.add(signal.get(i));
            }

            return begin;

        }else if(code ==2){

            List<Float> middle = new ArrayList<>();
            int mid = signal.size()/2;
            int start = mid - size/2;
            int end = mid + size/2;

            for(int i=start; i<end; i++){
                middle.add(signal.get(i));
            }

            return middle;
        }else {
            List<Float> last = new ArrayList<>();

            for(int i= signal.size()-size; i<signal.size(); i++){
                last.add(signal.get(i));
            }

            return last;
        }
    }

    static int getHR(List<Float> list, double lowPass, double highPass, double frequency){
        int size = list.size();
        if(size == 32 || size==64 || size==128 || size == 256 || size == 512){
            if(size == 32 || size == 64) {
                return computeHR(list, lowPass, highPass, frequency);
            }else if(size == 128){
                float a1 = 0.4f;
                float b1 = 0.6f;
                int a = getHR(getSignal(list, 96, 1), lowPass, highPass, frequency);
                int b = getHR(getSignal(list, 96, 3), lowPass, highPass, frequency);
                float mean = a1*a + b1*b;
                return (int)mean;
            }else if(size == 256) {
                float a1 = 0.4f;
                float b1 = 0.6f;
                int a = getHR(getSignal(list, 192, 1), lowPass, highPass, frequency);
                int b = getHR(getSignal(list, 192, 3), lowPass, highPass, frequency);
                float mean = a1*a + b1*b;
                return (int)mean;
            }else {
                float a1 = 0.4f;
                float b1 = 0.6f;
                int a = getHR(getSignal(list, 256, 1), lowPass, highPass, frequency);
                int b = getHR(getSignal(list, 256, 3), lowPass, highPass, frequency);
                float mean = a1*a + b1*b;
                return (int)mean;
            }
        }else{
            if(size == 96){
                float a1 = 0.25f;
                float b1 = 0.35f;
                float c1 = 0.40f;
                int a = computeHR(getSignal(list, 64, 1), lowPass, highPass, frequency);
                int b = computeHR(getSignal(list, 64, 2), lowPass, highPass, frequency);
                int c = computeHR(getSignal(list, 64, 3), lowPass, highPass, frequency);
                float mean = a1*a + b1*b + c1*c;
                return (int)mean;
            }else if(size == 160){
                float a1 = 0.25f;
                float b1 = 0.35f;
                float c1 = 0.40f;
                int a = computeHR(getSignal(list, 128, 1), lowPass, highPass, frequency);
                int b = computeHR(getSignal(list, 128, 2), lowPass, highPass, frequency);
                int c = computeHR(getSignal(list, 128, 3), lowPass, highPass, frequency);
                float mean = a1*a + b1*b + c1*c;
                return (int)mean;
            }else if(size == 192){
                float a1 = 0.25f;
                float b1 = 0.35f;
                float c1 = 0.40f;
                int a = computeHR(getSignal(list, 128, 1), lowPass, highPass, frequency);
                int b = computeHR(getSignal(list, 128, 2), lowPass, highPass, frequency);
                int c = computeHR(getSignal(list, 128, 3), lowPass, highPass, frequency);
                float mean = a1*a + b1*b + c1*c;
                return (int)mean;
            }else {
                float a1 = 0.25f;
                float b1 = 0.35f;
                float c1 = 0.40f;
                int a = computeHR(getSignal(list, 128, 1), lowPass, highPass, frequency);
                int b = computeHR(getSignal(list, 128, 2), lowPass, highPass, frequency);
                int c = computeHR(getSignal(list, 128, 3), lowPass, highPass, frequency);
                float mean = a1*a + b1*b + c1*c;
                return (int)mean;
            }
        }

        //  return 0;
    }

    static List<Float> doubleListToFloatList(List<Double> list){
        List<Float> out = new ArrayList<>();
        for(int i=0; i<list.size(); i++) {
            double p = list.get(i);
            out.add((float) p);
        }

        return out;
    }

    static List<Double> floatListTodoubleList(List<Float> list){
        List<Double> out = new ArrayList<>();
        for(int i=0; i<list.size(); i++) {
            double p = list.get(i);
            out.add(p);
        }

        return out;
    }

    static int computeHR(List<Float> list1, double lowPass, double highPass, double frequency){
        List<Double> dlist = floatListTodoubleList(list1);
        // dlist = signalProcessingClass.filterList(dlist, highPass, lowPass, frequency);
        //list = testHRComputing.filterSignal(list, lowPass, highPass);
        List<Float> list = doubleListToFloatList(dlist);
        List<Point> valleys = new ArrayList<>();
        int start = 0;
        for(int i=1; i<list.size()-1; i++){
            float a = list.get(i-1);
            float b = list.get(i);
            float c = list.get(i+1);
            if((b<=a && b<c) || (b<a && b<=c)) {
                valleys.add(new Point(i, (int) b));
                // Log.e("TAG", "current valleys ("+i+" , "+b+")");
                if (start == 0) {
                    start = i;
                    //  Log.e("TAG", "current start: " + i);
                }
            }
        }
        double freq1 = highestFreq(list, lowPass, highPass, frequency);
        double freq = FPS/freq1;
        start -= (int)(freq/2);
        // Log.e("TAG", "estimated freq: "+freq);
        //Log.e("TAG", "current refined start: "+start);
        int currentEnd = start + (int)freq;
        //Log.e("TAG", "current end: "+currentEnd);
        ArrayList<ArrayList<Point>> lol = new ArrayList<>();

        // while (currentEnd <= list.size()){
        ArrayList<Point> temp = new ArrayList<>();

        for(int i=0; i<valleys.size(); i++){
            if(valleys.get(i).x < currentEnd){
                temp.add(valleys.get(i));
                //Log.e("TAG", "current add "+valleys.get(i).x+" < "+currentEnd);
            }else {
                // Log.e("TAG", "current add "+valleys.get(i).x+" > "+currentEnd);
                lol.add(temp);
                // Log.e("sss", "current temp size: "+temp.size()+" freq "+freq);
                temp = new ArrayList<>();
                temp.add(valleys.get(i));
                currentEnd+= freq;
                //Log.e("TAG", "current in end  "+currentEnd);
            }
        }

        Log.e("TAG", "current lol size: "+lol.size()+" list size: "+list.size());
        float [][] arr = lolToArray(lol);
        printCombinations1(arr, 0, "", (float) freq);

        int hr =  (int)(60 * FPS/bestMean);
        minVar = 1000;
        bestMean = 0;
        // Log.e("TAG", "current estimated HR: "+hr+" BPM");

        return hr;
    }

    public static float [][] lolToArray(ArrayList<ArrayList<Point>> lol){
        float [][] out = new float[lol.size()][];
        for(int i=0; i<lol.size(); i++){
            out[i] = new float[lol.get(i).size()];
            for(int j=0; j<out[i].length; j++){
                ArrayList<Point> points = lol.get(i);
                out[i][j] = points.get(j).x;
            }
        }
        return out;
    }



    public static double highestFreq(List<Float> list, double lowPass, double highPass, double frequency){
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
    }




    private static void printCombinations1(float[][] sets, int n, String prefix, float freq){
        if(n >= sets.length){
            /// System.out.println("{"+prefix.substring(0,prefix.length()-1)+"}");
            float [] arr  = getFloatsFromString(prefix);
            //System.out.println(prefix+" size "+prefix.length());
            float [] vr = variance(arr, freq);
            int hr =  (int)(60 * FPS/vr[0]);
            Log.e("Tag", "estimated candidate: "+hr+" BPM");
            if(vr[1]<minVar){
                minVar = vr[1];
                bestMean = vr[0];
            }
            printArray(arr);
            return;
        }
        for(double s : sets[n]){
            printCombinations1(sets, n+1, prefix+s+",", freq);
        }
    }

    public static float [] getDistances(float [] locs, float freq){
        locs = correction(locs, freq);
        float [] dists = new float[locs.length-1];
        for(int i=1; i<locs.length; i++){
            dists[i-1] = locs[i] - locs[i-1];
        }
        return dists;
    }

    public static float [] correction(float [] arr, float freq){
        ArrayList<Float> locs = new ArrayList<>();
        locs.add(arr[0]);
        for(int i=1; i<arr.length; i++){
            float dist = arr[i] - arr[i-1];
            if((dist/2) >= freq * 0.80f){
                locs.add( arr[i-1] + (dist/2+freq)/2);
                locs.add(arr[i]);
                Log.e("TAG", "estimated corrected....");
            }else {
                locs.add(arr[i]);
            }
        }
        float out [] = new float[locs.size()];
        for(int i=0; i<out.length; i++){
            out[i] = locs.get(i);
        }
        return out;
    }

    public static float mean (float [] arr){
        float avg = 0;
        for(int i=0; i<arr.length; i++){
            avg += arr[i];
        }
        avg /= arr.length;
        return avg;
    }

    public static float weigthMean(float [] arr){
        float avg = mean(arr);
        float var = 0;
        for(int i=0; i<arr.length; i++){
            var += (arr[i] - avg) * (arr[i] - avg);
        }
        var /= arr.length;
        float weightSum = 0;
        float m = 0;
        for(int i=0; i<arr.length; i++){
            float w = var - Math.abs(arr[i] - avg);
            m += w * arr[i];
            weightSum += w;
        }

        m /= weightSum;
        return m;
    }

    public static float [] variance(float [] arr, float freq){
        float [] arr1 = getDistances(arr, freq);
        float var = 0;
        float mean1 = mean(arr1);
        for(int i=0; i<arr1.length; i++){
            var = (arr1[i] - mean1) * (arr1[i] - mean1);
        }
        var /= arr1.length;
        float d [] = {mean1, var};
        return d;
    }

    // public String subString

    public static float [] getFloatsFromString(String string){
        //StringBuilder sb = new StringBuilder(string);
        int end = string.length()-1;
        if(end == -1)
            end = 1;

        String ss = string.substring(0, end);
        // sb.deleteCharAt(string.length()-1);
        // string = ss.toString();
        String[] s = ss.split(",");
        float [] series = new float[s.length];
        for(int i=0; i<s.length; i++){
            series[i] = Float.parseFloat(s[i]);
        }
        return series;
    }

    public static void printArray(float [] arr){
        String s = "";
        for(int i=0; i<arr.length; i++){
            s+= arr[i]+" - ";
        }
        Log.e("TAG", " current combinazon: "+s);
    }

}
