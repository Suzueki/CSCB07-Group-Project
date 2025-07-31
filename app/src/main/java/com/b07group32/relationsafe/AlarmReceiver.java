package com.b07group32.relationsafe;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class AlarmReciever extends BroadcastReceiver{

    private NotificationManager notifManager;
    private int NOTIFICATION_ID = 1;
    private String channelID = "10";
    private String channelDescription = "123";
    private String logTag = "AlarmReciever";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(logTag, "Received request");
        notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        deliverNotification(context);
    }

    private void deliverNotification(Context context) {
        Log.d(logTag, "Notification queued");
        Intent contentIntent = new Intent(context, ScheduleFragment.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        createChannel(context);
        showNotification(context);
    }
    @SuppressLint("MissingPermission")
    public void showNotification(Context context){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !notificationManager.areNotificationsEnabled()) {
            return;
        }
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Task completed")
                .setContentText("The background task has completed successfully.")
                .setColor(ContextCompat.getColor(context, R.color.black))
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }
    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    channelID,
                    channelDescription,
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
        }
    }
}
