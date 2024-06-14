package com.xplorun;


import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xplorun.TTSService.Style;
import com.xplorun.model.DB;
import com.xplorun.model.Step;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BackgroundService extends Thread implements LocationListener {
    private static final String TAG = "log_BackgroundService";
    private final TTSService tts;
    private final DB db;
    private final Context appContext;
    private final BlockingQueue<Location> queue;
    private final ExecutorService executor;
    private boolean running = false;
    boolean voiceEnabled = true;

    private static BackgroundService instance;

    public static BackgroundService getInstance(TTSService tts, DB db, Context context, ExecutorService exe) {
        if (instance == null)
            return instance = new BackgroundService(tts, db, context, exe);
        return instance;
    }

    private BackgroundService(TTSService tts, DB db, Context context, ExecutorService exe) {
        setName("BackgroundThread");
        this.tts = tts;
        this.db = db;
        this.queue = new LinkedBlockingQueue<>(10);
        this.appContext = context;
        this.executor = exe;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (location.getLatitude() == 0 && location.getLongitude() == 0) {
            Log.d(TAG, location.toString());
            return;
        }

        try {
            queue.add(location);
        } catch (IllegalStateException e) {
            queue.remove();
            queue.add(location);
        }
    }

    public void quit() {
        running = false;
        queue.clear();
    }

    @Override
    public void run() {
        running = true;
        Log.i(TAG, "BackgroundThread started");
        while (running) {
            try {
                var loc = queue.poll(1, TimeUnit.SECONDS);
                if (loc != null) handle(loc);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "BackgroundThread finished");
    }

    private void handle(Location location) {
//        Log.e(TAG, "handle: " + location.toString());
        var route = db.activeRoute();
        if (route == null) {
            Log.w(TAG, "No active route");
            return;
        }

        Step step = Utils.getClosest(location, route);
        if (step == null || route.steps.isEmpty()) return;
        route.nextStep(step.nextStep());
        var isFirst3Steps = route.steps.get(0) == step || route.steps.get(1) == step||route.steps.get(2) == step;
        if (!route.started && isFirst3Steps) {
            route.started = true;
            startRun();
        }
        var dist = Math.round(step.distTo(location));
        if (dist > 1000) {
            //teleport?
            return;
        }
        if (voiceEnabled) {
            tts.speak(step.ttsInstruction);
            Log.d(TAG, step.ttsInstruction);
            speakAbout(step, location);
            step.lastSpoken(Long.MAX_VALUE);
            db.update(step);
        }
        db.user().lastDist = dist;
    }

    private void speakAbout(Step step, Location location) {
        Step nextStep = step.nextStep();
        if (nextStep != null) {
            var dist = Math.round(nextStep.distTo(location));
            double diff = dist - db.user().lastDist;
            Log.i(TAG, "diff:" + diff);
            double speed = location.getSpeed();
            if (speed<0.5) {
                tts.speak("Don't stop now!", Style.Friendly);
            } else if (diff > ((speed - 1.12) / (7 - 1.12)) * 15 + 1) {
//                tts.speak("Wrong direction!", Style.Angry);
            }
        }
    }

    public void startRun() {
        executor.submit(() -> tts.speak("Have a great run!", Style.Friendly));
    }
}
