package com.xplorun;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.xplorun.R;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;

public class StartRunActivity extends AppCompatActivity {
    private MapView mapView;
    private static final int NOTIFICATION_ID=895498;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_run);
        notificationManager = getSystemService(NotificationManager.class);
        loadUI();
    }

    public void loadUI(){
        Context ctx=getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue("Explorun/1.0");
        MaterialButton cancelButton = findViewById(R.id.cancel_run);
        cancelButton.setOnClickListener(view -> {
            RoutesSingleton.getInstance().setActiveRoute(null);
            finish();
        });

        MaterialButton endRunButton = findViewById(R.id.button_rate_now);
        endRunButton.setOnClickListener(view -> startFeedbackActivity());
        mapView = findViewById(R.id.mapViewRun);
        mapView.setMultiTouchControls(true);

        makeNotification(getApplicationContext());
    }

    private void createNotificationChannel(String name) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
//        CharSequence name = getString(R.string.not_channel);
//        String description = getString(R.string.channel_description);
        NotificationChannel channel = new NotificationChannel(getString(R.string.app_name),
                name, NotificationManager.IMPORTANCE_UNSPECIFIED);
//      channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        notificationManager.createNotificationChannel(channel);
    }

    private void makeNotification(Context context) {
        createNotificationChannel(getString(R.string.app_name));
        Intent intent = new Intent(context, StartRunActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                NOTIFICATION_ID, intent, PendingIntent.FLAG_MUTABLE);

        Notification n = new NotificationCompat.Builder(context,getString(R.string.app_name))
                .setContentTitle("ExploRun - Active Run")
                .setContentText("Route")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.osm_ic_follow_me)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.osm_ic_follow_me))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Stat goes here")
                        .setBigContentTitle("Stats"))
                .build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(NOTIFICATION_ID, n);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        //BackgroundService.startRun(getApplicationContext());
    }


    public void startFeedbackActivity(){
        Intent intent = new Intent(this, FeedbackActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
    }
}
