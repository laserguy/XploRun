package com.xplorun;

import android.app.Application;
import android.preference.PreferenceManager;

import com.xplorun.model.DB;

import org.osmdroid.config.Configuration;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class App extends Application {

    private static App instance;

    public static App getInstance() {
        return instance;
    }
    @Inject TTSService tts;
    @Inject
    BackgroundService service;
    @Inject
    DB db;

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        service.start();
        db.activeRoute(null);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(Params.USER_AGENT);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        service.quit();
        tts.shutdown();
    }
}
