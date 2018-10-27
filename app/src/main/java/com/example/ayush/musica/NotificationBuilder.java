package com.example.ayush.musica;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.example.ayush.musica.service.AudioServiceBinder;

public class NotificationBuilder {
    public static final String CHANNEL_ID = "media_playback_channel";
    public static final String NOTIFI_PLAY = "com.example.ayush.musica.action_play";
    public static final String NOTIFI_PAUSE = "com.example.ayush.musica.action_pause";
    public static final String NOTIFI_NEXT = "com.example.ayush.musica.action_next";
    public static final String NOTIFI_PREVIOUS = "com.example.ayush.musica.action_previous";
    public static final String NOTIFI_STOP = "com.example.ayush.musica.action_stop";
private Context context;
public NotificationBuilder(Context context){
    this.context = context;
}


    public void CreateNotification() {
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.notification);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String id = CHANNEL_ID;
        CharSequence name = "Media playback";
        String description = " Media playback Controls";
        int imp = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel notificationChannel = new NotificationChannel(id, name, imp);

        notificationChannel.setDescription(description);
        notificationChannel.setShowBadge(false);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(notificationChannel);


        Intent notifyIntent = new Intent(context, AudioServiceBinder.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder noti = new NotificationCompat.Builder(context, CHANNEL_ID);
        noti.setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setAutoCancel(false)
                .setChannelId(CHANNEL_ID)
                .setCustomBigContentView(remoteView)
                .setContentTitle("Music Payer")
                .setContentText("Control Audio");
               // .getBigContentView().setTextViewText(R.id.noti_song_name, "Channa Mereya");
        setListeners(remoteView,context);
        notificationManager.notify(1, noti.build());
        //noti.build();

    }


    private void createChannel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String id = CHANNEL_ID;
        CharSequence name = "Media playback";
        String description = " Media playback Controls";
        int imp = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel notificationChannel = new NotificationChannel(id, name, imp);

        notificationChannel.setDescription(description);
        notificationChannel.setShowBadge(false);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    private static void setListeners(RemoteViews view, Context context) {
        Intent pause = new Intent(NOTIFI_PAUSE);
        Intent play = new Intent(NOTIFI_PLAY);
        Intent next = new Intent(NOTIFI_NEXT);
        Intent previous = new Intent(NOTIFI_PREVIOUS);
        Intent stop = new Intent(NOTIFI_STOP);

        PendingIntent pNext = PendingIntent.getBroadcast(context,0,next,PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.noti_song_next,pNext);

        PendingIntent pStop = PendingIntent.getBroadcast(context,0,stop,PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.noti_close,pStop);

        PendingIntent pPause = PendingIntent.getBroadcast(context,0,pause,PendingIntent.FLAG_UPDATE_CURRENT);
        view.setOnClickPendingIntent(R.id.noti_song_pause,pPause);
    }
}
