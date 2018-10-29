package com.example.ayush.musica;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.ayush.musica.activity.DetailActivity;
import com.example.ayush.musica.service.AudioServiceBinder;

import static com.example.ayush.musica.AppConstants.ACTION_NEXT;
import static com.example.ayush.musica.AppConstants.ACTION_PAUSE;
import static com.example.ayush.musica.AppConstants.ACTION_PAUSE_WIDGET;
import static com.example.ayush.musica.AppConstants.ACTION_PLAY;
import static com.example.ayush.musica.AppConstants.ACTION_PLAY_WIDGET;

public class NotificationBroadCastMusica extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action){
            case ACTION_PLAY_WIDGET:

                Toast.makeText(context,"Notify Play",Toast.LENGTH_SHORT).show();
                break;
            case ACTION_PAUSE_WIDGET:
                Toast.makeText(context,"Notify Next",Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
