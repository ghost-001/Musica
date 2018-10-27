package com.example.ayush.musica.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class BackgroundAudioService extends Service {

    private AudioServiceBinder audioServiceBinder = new AudioServiceBinder();
    public BackgroundAudioService(){

    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return audioServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}
