package com.example.facetracker;

import android.graphics.Canvas;

/**
 * Created by milore on 5/16/2016.
 */
public class myThread extends Thread {
    static final long FPS = 20;
    private heartrateView view;
    private boolean running = false;

    public myThread(heartrateView view) {
        this.view = view;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    @Override
    public void run() {
        long ticksPS = 1000 / FPS;
        long startTime;
        long sleepTime;
        while (running) {
            Canvas c = null;
            startTime = System.currentTimeMillis();
            try {
                c = view.getHolder().lockCanvas();
                synchronized (view.getHolder()) {
                    //  if(!view.wasPaused) {
                    // Log.e("[thread]", "Thread is running");
                    //view.onDraw(c);
                    view.drawIt(c);

                    // }
                }
            } finally {
                if (c != null) {
                    view.getHolder().unlockCanvasAndPost(c);
                }
            }
            sleepTime = ticksPS-(System.currentTimeMillis() - startTime);
            try {
                if (sleepTime > 0)
                    sleep(sleepTime);
                else
                    sleep(10);
            } catch (Exception e) {}
        }
    }
}