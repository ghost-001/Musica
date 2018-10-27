package com.example.ayush.musica;

import android.annotation.TargetApi;
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
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.util.Log;
import android.widget.Toast;


import com.example.ayush.musica.activity.DetailActivity;
import com.example.ayush.musica.utility.Songs;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import static com.example.ayush.musica.AppConstants.ACTION_NEXT;
import static com.example.ayush.musica.AppConstants.ACTION_PAUSE;
import static com.example.ayush.musica.AppConstants.ACTION_PLAY;
import static com.example.ayush.musica.AppConstants.ACTION_PREVIOUS;
import static com.example.ayush.musica.AppConstants.ACTION_STOP;

public class SongService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, AudioManager.OnAudioFocusChangeListener {


    private static final String CHANNEL_ID = "111";
    public final int UPDATE_PROGRESS_KEY = 1;


    private final IBinder iBinder = new LocalBinder();


    private AudioManager audioManager;
    private static MediaPlayer mediaPlayer;
    private MediaStore.Audio activeAudio;

    private ArrayList<Songs> songs = new ArrayList<>();
    private int songIndex = -1;
    private Songs song;
    private boolean checkAudioFocus = false;
    private boolean changePos = false;
    private int currentPosition = 0;
    private Uri fileUri = null;
    private Context context = null;
    private String str;

    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    private Handler progressHandler;

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //songIndex = intent.getIntExtra("songIndex", 0);
            //song = intent.getParcelableExtra("song");
            //songs = intent.getParcelableArrayListExtra("songList");

            stopMedia();
            if(mediaPlayer != null)
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
        this.context = context;
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
                    "MyApp", NotificationManager.IMPORTANCE_DEFAULT);
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
            str = intent.getStringExtra("faltu");
            //song = intent.getParcelableExtra("song");
            //songs = intent.getParcelableArrayListExtra("songList");
            //songIndex = intent.getIntExtra("songIndex",0);
            Log.i("Fuck", "GOT str");
            Log.i("Fuck", "size songs list" + songs.size());


        } catch (NullPointerException e) {
            stopSelf();
        }

        if(!checkAudioFocus)
        if (!requestAudioFocus()) {
            Log.i("Fuck", "focus not gained");
            stopSelf();
        }

        if(mediaSessionManager == null){
            try {
                initMediaSession();
                initPlayer();
                Log.i("Fuck", "PLayer N session initialized");
            }catch (RemoteException e){
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
                Log.i("Fuck", "PLayer attributes set");
            }
            try {

                    mediaPlayer.setDataSource(getContext(), getFileUri());

                    Log.i("Fuck", "PLayer data source  set");

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

    }

    public boolean isPlaying(){
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
    public void playMedia() {
      if (mediaPlayer != null) {
            mediaPlayer.start();
          buildNotification(PlaybackStatus.PLAYING);
        }
    }

    public void stopMedia() {
        if (mediaPlayer == null)
            return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void pauseMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        currentPosition = mediaPlayer.getCurrentPosition();
            buildNotification(PlaybackStatus.PAUSED);
        }
    }

    public void resumeMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(currentPosition);
            mediaPlayer.start();
            buildNotification(PlaybackStatus.PLAYING);
        }
    }
    public void changePosition(int i){
        currentPosition = i*1000;
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

    public void resetMediaPlayer(){
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
                if(mediaPlayer == null) initPlayer();
              else if (!mediaPlayer.isPlaying())
                    mediaPlayer.start();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer!=null)
            {   mediaPlayer.pause();
               // mediaPlayer.release();
                //mediaPlayer = null;
            }
                break;
        }
    }
    @TargetApi(Build.VERSION_CODES.O)
    private boolean requestAudioFocus() {
        AudioAttributes mAudioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();

       audioManager = (AudioManager)
                getContext().getSystemService(Context.AUDIO_SERVICE);
        AudioFocusRequest mAudioFocusRequest =
                new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(mAudioAttributes)
                        .setOnAudioFocusChangeListener(this)
                        .build();
        int focusRequest = audioManager.requestAudioFocus(mAudioFocusRequest);
        if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            checkAudioFocus = true;
            return true;
        }
        return false;


        /*
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int focusRequest = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Play
                return true;
            }
            return false;
        }else if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            AudioAttributes mAudioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();

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
                    return false;
                case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                    return true;

            }
        }
        return false;*/
    }

    private boolean removeAudioFocus() {
        checkAudioFocus = false;
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) {
            Log.i("Fuck", "esssion is null");
            return;
        }

        Log.i("Fuck", "initMediaSession");
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");

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
        Log.i("Fuck", "PLayer on prepared methode");
    }
    @Override
    public void onSeekComplete(MediaPlayer m) {
        mediaPlayer.reset();
        currentPosition = 0;
        Log.i("XXXX","onSeekCOmplete");
    }
    private void updateMetaData() {
        Log.i("Fuck", "Updating metaData");
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
                R.drawable.image); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getmSongartist())
                //  .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, )
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
                Log.i("Fuck", "session onPlay");
                resumeMedia();

            }

            @Override
            public void onPause() {
                super.onPause();
                Log.i("Fuck", "session onPause");
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
               // skipToNext();
                Log.i("Fuck", "session onSkipTONext");
                songIndex++;
                song = songs.get(songIndex);
                Toast.makeText(getContext(),"SKIP TO NEXT ",Toast.LENGTH_SHORT).show();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
               // getContext().startForeground(NOTIFICATION_ID, notification);
                //mStarted = true;
                stopMedia();
                //reset mediaPlayer
                mediaPlayer.reset();
                initPlayer();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                //skipToPrevious();
                Log.i("Fuck", "session onSKipTOprevious");
                //songIndex--;
               // song = songs.get(songIndex);
                Toast.makeText(getContext(),"SKIP TO NEXT ",Toast.LENGTH_SHORT).show();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
                stopMedia();
                //reset mediaPlayer
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

    @TargetApi(Build.VERSION_CODES.O)
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

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        CharSequence name = "Media playback";
        String description = "Media playback Controls";
        int imp = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, imp);

        notificationChannel.setDescription(description);
        notificationChannel.setShowBadge(false);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationCompat.Builder noti = new NotificationCompat.Builder(context, CHANNEL_ID)
                //.setShowWhen(false)
                // Set the Notification style
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2,4))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setAutoCancel(true)
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(song.getSongTitle())
                .setContentTitle(song.getSongTitle())
                .setContentInfo(song.getmSongartist())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_PauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2))
                .addAction(android.R.drawable.ic_notification_clear_all,"stop",playbackAction(4));
         notificationManager.notify(1, noti.build());


    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(111);
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
                return PendingIntent.getService(this,actionNumber,playbackAction,0);

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
                this.stopSelf();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
                removeNotification();
                transportControls.stop();
                break;
        }
    }

    private void registerPlayNewAudio() {
        IntentFilter intentFilter = new IntentFilter(DetailActivity.BROADCAST_PLAY_NEW_SONG);
        registerReceiver(playNewAudio, intentFilter);
    }

    public class LocalBinder extends Binder {
        public SongService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SongService.this;
        }
    }

}
