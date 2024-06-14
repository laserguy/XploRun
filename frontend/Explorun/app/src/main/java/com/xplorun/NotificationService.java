package com.xplorun;

import static android.app.Notification.FOREGROUND_SERVICE_IMMEDIATE;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.xplorun.R;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class NotificationService extends Service {
    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 895498;
    public static final String ACTION_NOTIFY = "NOTIFY";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_ICON = "icon";
    public NotificationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
    }

    public static void notify(String text, int icon) {
        var notify = new Intent(App.getInstance(), NotificationService.class);
        notify.setAction(NotificationService.ACTION_NOTIFY);
        notify.putExtra(NotificationService.EXTRA_MESSAGE, text);
        notify.putExtra(NotificationService.EXTRA_ICON, icon);
        App.getInstance().startForegroundService(notify);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (ACTION_NOTIFY.equals(intent.getAction())) {
                makeNotification(getApplicationContext(),
                        getString(R.string.app_name),
                        intent.getStringExtra(EXTRA_MESSAGE),
                        intent.getIntExtra(EXTRA_ICON, R.drawable.osm_ic_follow_me_on));
                return START_STICKY;
            }
        }

        var notification = makeNotification(getApplicationContext(),
                getString(R.string.app_name), "", R.drawable.osm_ic_follow_me);
        startForeground(1, notification);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel(String name) {
        NotificationChannel channel = new NotificationChannel(getString(R.string.app_name),
                name, NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

    private Notification makeNotification(Context context, String title, String text, int icon) {
        createNotificationChannel(getString(R.string.app_name));
        Intent intent = new Intent(context, NavigateActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                NOTIFICATION_ID, intent, PendingIntent.FLAG_MUTABLE);

        Notification n = new NotificationCompat.Builder(context, getString(R.string.app_name))
                .setContentTitle("ExploRun - Active Run")
                .setContentText(text)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setSmallIcon(icon)
                .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), icon))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text)
                        .setBigContentTitle(title))
                .build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return n;
    }

    @Override
    public void onDestroy() {
        if (notificationManager != null)
            notificationManager.cancelAll();
        super.onDestroy();
    }
}
