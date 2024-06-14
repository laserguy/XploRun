package com.xplorun;

import android.content.Context;
import com.xplorun.model.DB;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn({ActivityComponent.class, ServiceComponent.class, SingletonComponent.class})
public class AppModule {

    @Provides
    public Context provideContext() {
        return App.getInstance().getApplicationContext();
    }

    @Provides
    public OSRMRoadManagerCustom provideOSRMRoadManager(Context context) {
        OSRMRoadManagerCustom roadManager = new OSRMRoadManagerCustom(context, Params.USER_AGENT);
        roadManager.setService(Params.OSRM_URL);
        //roadManager.addRequestOption("tidy=true");
        return roadManager;
    }

    @Provides
    public TTSService provideTTS(Context context) {
        return TTSService.getInstance(context);
    }

    @Provides
    public DB provideDB(Context context) {
        return DB.get(context);
    }

    @Provides
    public ExecutorService provideExecutorService() {
        return Executors.newFixedThreadPool(4);
    }

    @Provides
    public BackgroundService provideBackgroundThread(DB db, TTSService tts, Context context, ExecutorService exe) {
        return BackgroundService.getInstance(tts, db, context, exe);
    }
}
