package com.example.ayush.musica.service;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;

import com.example.ayush.musica.NotificationBuilder;
import com.example.ayush.musica.activity.DetailActivity;
import com.example.ayush.musica.utility.Songs;

import java.io.IOException;
import java.util.ArrayList;

public class AudioServiceBinder extends Binder implements AudioManager.OnAudioFocusChangeListener {
    private Uri fileUri = null;
    private ArrayList<Songs> songs = new ArrayList<>();
    private Integer songIndex = 0;
    private Songs song;
    private boolean checkAudioFocus = false;
    private boolean changePos = false;
    private MediaPlayer mediaPlayer = null;
    private int currentPosition = 0;

    private Context context = null;
    private Handler progressHandler;
    private NotificationBuilder notificationBuilder = null;
    private AudioManager audioManager = null;
    public final int UPDATE_PROGRESS_KEY = 1;

    public final int UPDATE_PROGRESS_POSITION = 2;

    public Uri getFileUri() {
        return fileUri;
    }

    public void setFileUri(Uri fileUri) {
        this.fileUri = fileUri;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = (DetailActivity) context;
    }

    public Handler getProgressHandler() {
        return progressHandler;
    }

    public void setProgressHandler(Handler progressHandler) {
        this.progressHandler = progressHandler;
    }



    public Songs getSong() {
        return song;
    }

    public void setSong(Songs song) {
        this.song = song;
    }

    public ArrayList<Songs> getSongs() {
        return songs;
    }

    public void setSongs(ArrayList<Songs> songs) {
        this.songs = songs;
    }

    public Integer getSongIndex() {
        return songIndex;
    }

    public void setSongIndex(Integer songIndex) {
        this.songIndex = songIndex;
    }

    public void createNotification(Context context){
        if(notificationBuilder == null){
            notificationBuilder = new NotificationBuilder(context);
        }
       notificationBuilder.CreateNotification();

    }
    private void initPlayer() {
        try {
            if (mediaPlayer == null) {

                mediaPlayer = new MediaPlayer();
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                        && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                  /*  audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    int focusRequest = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if(focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    {
                        // Play
                        mediaPlayer.setDataSource(getContext(), getFileUri());
                        mediaPlayer.prepare();
                    } */

                } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AudioAttributes mAudioAttributes =
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build();

                    mediaPlayer.setAudioAttributes(
                            mAudioAttributes);
                    if (!checkAudioFocus) {
                        audioManager = (AudioManager)
                                context.getSystemService(Context.AUDIO_SERVICE);
                        AudioFocusRequest mAudioFocusRequest =
                                new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                                        .setAudioAttributes(mAudioAttributes)
                                        .setOnAudioFocusChangeListener(this)
                                        .build();
                        int focusRequest = audioManager.requestAudioFocus(mAudioFocusRequest);
                        switch (focusRequest) {
                            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                                break;
                            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                                checkAudioFocus = true;
                                mediaPlayer.setDataSource(getContext(), getFileUri());
                                mediaPlayer.prepare();
                                break;
                        }
                    } else {
                        mediaPlayer.setDataSource(getContext(), getFileUri());
                        mediaPlayer.prepare();
                    }
                }

                Thread updateProgressThread = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            Message updateProgressMsg = new Message();
                            updateProgressMsg.what = UPDATE_PROGRESS_KEY;
                            progressHandler.sendMessage(updateProgressMsg);
                            try {
                                if (changePos) {
                                    mediaPlayer.seekTo(currentPosition);
                                    Log.i("PROGRESS", "this is currentPosition in thread" + currentPosition);
                                    changePos = false;
                                }
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                Log.i("Play", e.getMessage());
                            }
                        }
                    }
                };
                updateProgressThread.start();


                // }
                //  int result = aud.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                //        AudioManager.AUDIOFOCUS_GAIN);


            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("PLAY", e.getMessage());
        }
    }

    public void startAudio() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
                initPlayer();
                mediaPlayer.start();
            } catch (Exception e) {
                Log.i("Play", e.getMessage());
            }
        } else {
            initPlayer();
            mediaPlayer.start();
        }

    }

    public void pauseAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            currentPosition = mediaPlayer.getCurrentPosition();
            changePos = true;
        }
    }

    public void resumeAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            if (checkAudioFocus)
                audioManager.abandonAudioFocus(this);
            checkAudioFocus = false;
            destroyPlayer();
        }
    }

    public void changePosition(int i) {
        currentPosition = i*1000;
        if (mediaPlayer != null) {
            Log.i("PROGRESS", "this is before" + mediaPlayer.getCurrentPosition());
            Log.i("PROGRESS", "this is currentPosition" + currentPosition);
            changePos = true;
            Log.i("PROGRESS", "this is after " + mediaPlayer.getCurrentPosition());

        }
    }

    private void destroyPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public int getCurrentPosition() {
        int ret = 0;
        if (mediaPlayer != null) {
            ret = mediaPlayer.getCurrentPosition();
        }
        return ret;
    }


    public int getTotalAudioDuration() {
        int ret = 0;
        if (mediaPlayer != null) {
            ret = mediaPlayer.getDuration();
        }
        return ret;
    }

    public int getAudioProgress() {
        int ret = 0;
        int currentPosition = getCurrentPosition();
        int totalAudioDuration = getTotalAudioDuration();
        if (totalAudioDuration > 0) {
            ret = (currentPosition * 100) / totalAudioDuration;
        }
        return ret;
    }

    @Override
    public void onAudioFocusChange(int i) {
        if (i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause
            currentPosition = mediaPlayer.getCurrentPosition();
            pauseAudio();
        } else if (i == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume
            changePos = true;
            //  checkAudioFocus = true;
            //stopAudio();

            startAudio();
        } else if (i == AudioManager.AUDIOFOCUS_LOSS) {
            // Stop or pause depending on your need
            // currentPosition = mediaPlayer.getCurrentPosition();
            //checkAudioFocus = false;
            //stopAudio();
            if (checkAudioFocus) {
                audioManager.abandonAudioFocus(this);
                checkAudioFocus = false;
            }
            currentPosition = mediaPlayer.getCurrentPosition();
            pauseAudio();
        }
    }
}
