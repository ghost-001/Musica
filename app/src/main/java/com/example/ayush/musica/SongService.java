package com.example.ayush.musica;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.example.ayush.musica.activity.DetailActivity;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.utility.Store;
import com.example.ayush.musica.widget.MediaWidget;

import java.io.IOException;
import java.util.ArrayList;

import static com.example.ayush.musica.AppConstants.ACTION_NEXT;
import static com.example.ayush.musica.AppConstants.ACTION_PAUSE;
import static com.example.ayush.musica.AppConstants.ACTION_PLAY;
import static com.example.ayush.musica.AppConstants.ACTION_PREVIOUS;
import static com.example.ayush.musica.AppConstants.ACTION_STOP;
import static com.example.ayush.musica.AppConstants.APP_NAME;
import static com.example.ayush.musica.AppConstants.AUDIO_FOCUS_NOT_AVAILABLE;
import static com.example.ayush.musica.AppConstants.BROADCAST_PLAY_NEW_SONG;
import static com.example.ayush.musica.AppConstants.MEDIA_SESSION_TAG;
import static com.example.ayush.musica.AppConstants.NOTIFICATION_CHANNEL_NAME;
import static com.example.ayush.musica.AppConstants.NOTIFICATION_DESCRIPTION;
import static com.example.ayush.musica.AppConstants.PROGRESS_HANDLER_NULL;
import static com.example.ayush.musica.AppConstants.SONG_RETRIEVE_ERROR;
import static com.example.ayush.musica.AppConstants.SONG_SERVICE_TAG;

public class SongService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, AudioManager.OnAudioFocusChangeListener {


    private static final String CHANNEL_ID = "111";
    public final int UPDATE_PROGRESS_KEY = 1;


    private final IBinder iBinder = new LocalBinder();


    private AudioManager audioManager;
    private static MediaPlayer mediaPlayer;


    private ArrayList<Songs> songs = new ArrayList<>();
    private int songIndex = -1;
    private Songs song;
    private boolean userPause = false;
    private boolean changePos = false;
    private int currentPosition = 0;
    private Store store;

    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private Handler progressHandler;

    private MediaWidget mMediaWidgetProvider = MediaWidget.getInstance();

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            songIndex = store.getSongIndex();
            if (songIndex < 0)
                songIndex = 0;
            song = songs.get(songIndex);
            stopMedia();
            if (mediaPlayer != null)
                mediaPlayer.reset();
            initPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);

        }
    };

    public Handler getProgressHandler() {
        return progressHandler;
    }

    public void setProgressHandler(Handler progressHandler) {
        this.progressHandler = progressHandler;
    }

    @Nullable
    @Override

    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerPlayNewAudio();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    APP_NAME, NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {

            store = new Store(this);
            songIndex = store.getSongIndex();
            songs = store.getMediaList();
            if (songIndex == -1) {
                stopSelf();
                Log.d(SONG_SERVICE_TAG, SONG_RETRIEVE_ERROR);
            } else
                song = songs.get(songIndex);

            if (!requestAudioFocus()) {
                stopSelf();
                Log.d(SONG_SERVICE_TAG, AUDIO_FOCUS_NOT_AVAILABLE);
            }


        } catch (NullPointerException e) {
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }
        handleAction(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mediaSession.release();
        removeNotification();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        removeNotification();
        unregisterReceiver(playNewAudio);
    }

    private void initPlayer() {
        if (mediaPlayer == null)
            mediaPlayer = new MediaPlayer();


        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.reset();


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes mAudioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
            mediaPlayer.setAudioAttributes(
                    mAudioAttributes);

        }
        try {
            mediaPlayer.setDataSource(this, Uri.parse(song.getmSongUri()));

        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
        Thread updateProgressThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    Message updateProgressMsg = new Message();
                    updateProgressMsg.what = UPDATE_PROGRESS_KEY;
                    if (progressHandler != null) {
                        progressHandler.sendMessage(updateProgressMsg);
                        try {
                            if (changePos) {
                                mediaPlayer.seekTo(currentPosition);
                                changePos = false;
                            }
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else Log.d(SONG_SERVICE_TAG, PROGRESS_HANDLER_NULL);
                }
            }
        };
        //updateProgressThread.start();

    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void playMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            buildNotification(PlaybackStatus.PLAYING);
            notifyChange(ACTION_PLAY);
        }
    }

    public void stopMedia() {
        if (mediaPlayer == null)
            return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        notifyChange(ACTION_PAUSE);
    }

    public void pauseMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            userPause = false;
            currentPosition = mediaPlayer.getCurrentPosition();
            removeAudioFocus();
            buildNotification(PlaybackStatus.PAUSED);
            notifyChange(ACTION_PAUSE);
        }
    }

    public void resumeMedia() {
        if (mediaPlayer != null) {
            if (requestAudioFocus()) {
                mediaPlayer.seekTo(currentPosition);
                mediaPlayer.start();
                buildNotification(PlaybackStatus.PLAYING);
                notifyChange(ACTION_PLAY);
            }
        }
    }

    public void changePosition(int i) {
        currentPosition = i * 1000;
        if (mediaPlayer != null)
            changePos = true;
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

    public void resetMediaPlayer() {
        currentPosition = 0;
        mediaPlayer.reset();
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
        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) initPlayer();
                else if (!mediaPlayer.isPlaying() && !userPause)
                    mediaPlayer.start();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                break;
        }
    }

    private boolean requestAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            audioManager = (AudioManager)
                    this.getSystemService(Context.AUDIO_SERVICE);
            int focusRequest = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return true;
            }
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            AudioAttributes mAudioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
            AudioFocusRequest mAudioFocusRequest =
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(mAudioAttributes)
                            .setOnAudioFocusChangeListener(this)
                            .build();
            int focusRequest = audioManager.requestAudioFocus(mAudioFocusRequest);
            if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return true;
            }
        }
        return false;
    }

    private boolean removeAudioFocus() {

        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null)
            return;

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSessionCompat(getApplicationContext(), MEDIA_SESSION_TAG);
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        updateMetaData();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopMedia();
        removeNotification();
        stopSelf();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playMedia();

    }

    @Override
    public void onSeekComplete(MediaPlayer m) {
        mediaPlayer.reset();
        currentPosition = 0;

    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
                R.drawable.image);
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getmSongartist())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getSongTitle())
                .build());

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                super.onCommand(command, extras, cb);
            }

            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();

            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                userPause = true;
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();

                if (songIndex < songs.size() - 1) {
                    songIndex++;
                    store.storeSongIndex(songIndex);
                    song = songs.get(songIndex);
                }

                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
                stopMedia();
                mediaPlayer.reset();
                initPlayer();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                if (songIndex > 0) {
                    songIndex--;
                    store.storeSongIndex(songIndex);
                    song = songs.get(songIndex);
                }
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
                stopMedia();
                mediaPlayer.reset();
                initPlayer();
            }

            @Override
            public void onStop() {
                removeNotification();
                stopSelf();
                super.onStop();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }
        });
    }


    private void buildNotification(PlaybackStatus playbackStatus) {
        int notificationAction = android.R.drawable.ic_media_pause;

        PendingIntent play_PauseAction = null;

        if (playbackStatus == PlaybackStatus.PLAYING) {
            play_PauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            play_PauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.image);

        Intent resultIntent = new Intent(this, DetailActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                                                            PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder noti = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(resultPendingIntent)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0,1))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentTitle(song.getSongTitle())
                .setContentInfo(song.getmSongartist())
                .addAction(notificationAction, ACTION_PAUSE, play_PauseAction)
                .addAction(android.R.drawable.ic_notification_clear_all, ACTION_STOP, playbackAction(4));
        notificationManager.notify(1, noti.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(Integer.valueOf(CHANNEL_ID));
        }
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, SongService.class);
        switch (actionNumber) {
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 4:
                playbackAction.setAction(ACTION_STOP);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);

            default:
                break;
        }
        return null;
    }

    private void handleAction(Intent mediaAction) {
        if (mediaAction == null || mediaAction.getAction() == null) return;

        String action = mediaAction.getAction();
        switch (action) {
            case ACTION_PLAY:
                transportControls.play();
                break;
            case ACTION_PAUSE:
                transportControls.pause();
                break;
            case ACTION_NEXT:
                transportControls.skipToNext();
                break;
            case ACTION_PREVIOUS:
                transportControls.skipToPrevious();
                break;
            case ACTION_STOP:
                stopMedia();
                mediaPlayer.release();
                mediaPlayer = null;
                removeNotification();
                transportControls.stop();
                this.stopSelf();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);

                break;
        }
    }


    private void notifyChange(String what) {

        //Intent i = new Intent(what);
        //sendBroadcast(i);

        Intent broadcastIntent = new Intent(what);
        sendBroadcast(broadcastIntent);


        // Share this notification directly with our widgets
        mMediaWidgetProvider.notifyChange(this, what);

    }

    private void registerPlayNewAudio() {
        IntentFilter intentFilter = new IntentFilter(BROADCAST_PLAY_NEW_SONG);
        registerReceiver(playNewAudio, intentFilter);
    }

    public class LocalBinder extends Binder {
        public SongService getService() {
            return SongService.this;
        }

    }

}
