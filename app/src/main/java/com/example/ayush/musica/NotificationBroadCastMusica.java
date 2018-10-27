package com.example.ayush.musica;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.ayush.musica.activity.DetailActivity;
import com.example.ayush.musica.service.AudioServiceBinder;

public class NotificationBroadCastMusica extends BroadcastReceiver {
    AudioServiceBinder audioServiceBinder = null;
    DetailActivity detailActivity = new DetailActivity();
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action){
            case NotificationBuilder.NOTIFI_PAUSE:

                //context.playSong();
                Toast.makeText(context,"Notify Play",Toast.LENGTH_SHORT).show();
                break;
            case NotificationBuilder.NOTIFI_NEXT:
                //detailActivity.playNextSong();
                Toast.makeText(context,"Notify Next",Toast.LENGTH_SHORT).show();
                break;
            case NotificationBuilder.NOTIFI_STOP:
               // audioServiceBinder.stopAudio();
                Toast.makeText(context,"Notify Stop",Toast.LENGTH_SHORT).show();
                break;

        }
    }
}
