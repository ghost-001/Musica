package com.example.ayush.musica.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ayush.musica.R;
import com.example.ayush.musica.SongService;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.utility.Store;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.ayush.musica.AppConstants.ACTION_PAUSE;
import static com.example.ayush.musica.AppConstants.ACTION_PLAY;
import static com.example.ayush.musica.AppConstants.BROADCAST_PLAY_NEW_SONG;
import static com.example.ayush.musica.AppConstants.SONG_SERVICE_TAG;
import static com.example.ayush.musica.utility.BlurBuilder.blurImage;

//import android.support.v7.graphics.Palette;
public class DetailActivity extends AppCompatActivity {

    TextView mTitle;
    RelativeLayout mRelativeLayout;
    ImageView mPosterCircle;
    ArrayList<Songs> songsArrayList;
    Songs song;
    Integer songIndex;
    Bitmap background = null;
    int colorBG;


    public int vibrant;
    public int mDarkMutedColor;
    public int mMutedColor;


    private Handler progressUpdateHandler = null;

    private boolean checkIsPlaying = false;
    private boolean songPaused = false;
    private boolean serviceBound = false;

    private Store store;

    @BindView(R.id.detail_play)
    ImageButton playButton;
    @BindView(R.id.detail_next)
    ImageButton nextButton;
    @BindView(R.id.detail_previous)
    ImageButton previousButton;
    @BindView(R.id.detail_seekbar)
    SeekBar mSeekBar;
    @BindView(R.id.detail_song_current_time)
    TextView currentTime;
    @BindView(R.id.detail_song_max_time)
    TextView maxTime;
    @BindView(R.id.detail_fav)
    ImageView favouriteView;


    private SongService player;


    Intent play;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            SongService.LocalBinder binder = (SongService.LocalBinder) iBinder;
            player = binder.getService();
                serviceBound = true;
                playButtonClick();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    private BroadcastReceiver notifyChangeActiivty = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null)
                switch (intent.getAction()) {
                    case ACTION_PLAY:
                        checkIsPlaying = true;
                        songIndex = store.getSongIndex();
                        if (songIndex != -1)
                            song = songsArrayList.get(songIndex);
                        setPosterImage();
                        changePlayIcon();
                        break;
                    case ACTION_PAUSE:
                        checkIsPlaying = false;
                        changePlayIcon();
                        break;
                }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        registerReceiver(notifyChangeActiivty, intentFilter);

        play = new Intent(DetailActivity.this, SongService.class);

        songsArrayList = new ArrayList<>();

        store = new Store(this);
        songsArrayList = store.getMediaList();
        songIndex = store.getSongIndex();

        if (songIndex == -1)
            songIndex = 0;
        song = songsArrayList.get(songIndex);


        mTitle = findViewById(R.id.detail_music_name);
        mPosterCircle = findViewById(R.id.detail_circle_image);
        mRelativeLayout = findViewById(R.id.detail_relative_root);
        mRelativeLayout.setAlpha(.9f);
        bindBackgroundAudioService();
        setPosterImage();


        mTitle.getBackground().setAlpha(110);
        ButterKnife.bind(this);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playButtonClick();

            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPreviousSong();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNextSong();
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if (b) player.changePosition(seekBar.getProgress());

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        favouriteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFavourite();
            }
        });
    }


    public void saveFavourite() {
        Toast.makeText(this, "CLicked", Toast.LENGTH_SHORT).show();
    }


    private void playButtonClick() {
        if (!checkIsPlaying) {
            checkIsPlaying = true;
            play.setAction(ACTION_PLAY);
            changePlayIcon();
            if (songPaused) {
                songPaused = false;
                resumeMedia();
            } else if (!songPaused)
                playMedia();
        } else {
            player.pauseMedia();
            play.setAction(ACTION_PAUSE);
            checkIsPlaying = false;
            songPaused = true;
            changePlayIcon();
            playMedia();
        }

    }

    private void resumeMedia() {
        player.resumeMedia();
    }

    private void playMedia() {
        if (serviceBound) {

            // player.setContext(this);
           /* player.setFileUri(Uri.parse(song.getmSongUri()));
            player.setContext(this);
            player.setSongIndex(songIndex);
            player.setSongs(songsArrayList);
            player.setSong(song);*/
           // createAudioProgressbarUpdater();
            player.setProgressHandler(progressUpdateHandler);
//            maxTime.setText(setMaxTime(player.getTotalAudioDuration()));

            ContextCompat.startForegroundService(DetailActivity.this, play);

        } else {
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);

            // player.setContext(this);
         /*   player.setFileUri(Uri.parse(song.getmSongUri()));
            player.setContext(this);
            player.setSongIndex(songIndex);
            player.setSongs(songsArrayList);
            player.setSo ng(song);
            */
            createAudioProgressbarUpdater();
            player.setProgressHandler(progressUpdateHandler);
            maxTime.setText(setMaxTime(player.getTotalAudioDuration()));
            sendBroadcast(broadcastIntent);
        }

//        mSeekBar.setMax(player.getTotalAudioDuration() / 1000);

    }

    public void playPreviousSong() {
        if (songIndex > 0) {
            songIndex--;
            store.storeSongIndex(songIndex);

            song = songsArrayList.get(songIndex);
            mTitle.setText(song.getSongTitle());
            setPosterImage();
            checkIsPlaying = true;
            changePlayIcon();
            player.stopMedia();
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);

            // player.setFileUri(Uri.parse(song.getmSongUri()));
            // player.setContext(this);
            /*player.setSongIndex(songIndex);
            player.setSongs(songsArrayList);
            player.setSong(song);*/

            sendBroadcast(broadcastIntent);

        }

    }

    public void playNextSong() {
        if (songIndex < songsArrayList.size() - 1) {

            songIndex++;
            store.storeSongIndex(songIndex);


            song = songsArrayList.get(songIndex);
            mTitle.setText(song.getSongTitle());
            setPosterImage();
            checkIsPlaying = true;
            changePlayIcon();
            player.stopMedia();
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);

            //  player.setFileUri(Uri.parse(song.getmSongUri()));
            //player.setContext(this);
            /*player.setSongIndex(songIndex);
            player.setSongs(songsArrayList);
            player.setSong(song);
            sendBroadcast(broadcastIntent);*/
            sendBroadcast(broadcastIntent);
            maxTime.setText(setMaxTime(player.getTotalAudioDuration()));
        }

    }


    public void changePlayIcon() {
        if (checkIsPlaying) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                playButton.setImageDrawable(getDrawable(R.drawable.pause));
            else
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        } else {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                playButton.setImageDrawable(getDrawable(R.drawable.play));
            else
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
        }

    }

    private void bindBackgroundAudioService() {
        Intent intent = new Intent(DetailActivity.this, SongService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


    }

    private void createAudioProgressbarUpdater() {

        if (progressUpdateHandler == null) {
            progressUpdateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == player.UPDATE_PROGRESS_KEY) {
                        if (player != null) {
                            try {
                                int currentProgress = player.getAudioProgress();
                                int currentDuration = player.getCurrentPosition();
                                int totalDuration = player.getTotalAudioDuration();
                                mSeekBar.setMax(totalDuration / 1000);
                                currentTime.setText(String.valueOf(currentProgress));
                                mSeekBar.setProgress(currentDuration / 1000);
                                String x = setMaxTime(currentDuration);
                                currentTime.setText(x);
                                maxTime.setText(setMaxTime(totalDuration));
                                if (totalDuration == currentDuration) {
                                    player.stopSelf();
                                    player.resetMediaPlayer();
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                                Log.d(SONG_SERVICE_TAG,"ERROR IN DETAIL ACTIVITY HANDLER");
                            }
                        }
                    }
                }
            };
        }
    }

    public void setPosterImage() {

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        byte[] rawCover;
        Bitmap cover;
        mTitle.setText(song.getSongTitle());
        Uri uri = Uri.parse(song.getmSongUri());
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        metadataRetriever.setDataSource(getApplicationContext(), uri);
        rawCover = metadataRetriever.getEmbeddedPicture();
        if (null != rawCover) {
            cover = BitmapFactory.decodeByteArray(rawCover, 0, rawCover.length, bfo);
            Bitmap songImage = BitmapFactory
                    .decodeByteArray(rawCover, 0, rawCover.length);
            mPosterCircle.setImageBitmap(songImage);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            background = blurImage(this, cover);
            BitmapDrawable ob = new BitmapDrawable(getResources(), background);
            final View view = this.getWindow().getDecorView();
            view.setBackground(ob);
        }

    }

    public String setMaxTime(long x) {
        String finalTimerString = "";
        String secondsString = "";

        int hours = (int) (x / (1000 * 60 * 60));
        int minutes = (int) (x % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((x % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        if (hours > 0) {
            finalTimerString = hours + ":";
        }
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;


        return finalTimerString;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }


    @Override
    protected void onDestroy() {
        if (serviceBound) {
            unbindService(serviceConnection);
        }
        unregisterReceiver(notifyChangeActiivty);
        super.onDestroy();
    }

}
        /*setPosterImage();

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playButtonClick();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNextSong();
            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPreviousSong();
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    audioServiceBinder.changePosition(seekBar.getProgress());
                    // mSeekBar.setProgress(seekBar.getProgress());
                    Log.i("PROGRESS", "this is local seekbar progress" + seekBar.getProgress());
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }
    */


    /*
    public String setMaxTime(long x) {
        String finalTimerString = "";
        String secondsString = "";

        int hours = (int) (x / (1000 * 60 * 60));
        int minutes = (int) (x % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((x % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        if (hours > 0) {
            finalTimerString = hours + ":";
        }
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;


        return finalTimerString;
    }

    public String createTimeLable(int time) {
        String timeLable = "";
        int min = time / 1000 / 60;
        int sec = time / 1000 % 60;
        timeLable = min + ":";
        if (sec < 10)
            timeLable += "0";
        timeLable += sec;
        return timeLable;
    }

    public void playNextSong() {
        if (songIndex < songsArrayList.size()) {
            audioServiceBinder.stopAudio();
            songIndex++;
            song = songsArrayList.get(songIndex);
            mTitle.setText(song.getSongTitle());
            setPosterImage();
            Toast.makeText(this, "next " + song.getSongTitle(), Toast.LENGTH_SHORT).show();
            checkIsPlaying = true;
            changePlayIcon();
            playSong();
            maxTime.setText(setMaxTime(audioServiceBinder.getTotalAudioDuration()));
        }
    }

    public void playPreviousSong() {
        if (songIndex > 0) {
            audioServiceBinder.stopAudio();
            songIndex--;
            song = songsArrayList.get(songIndex);
            mTitle.setText(song.getSongTitle());
            setPosterImage();
            Toast.makeText(this, "previous" + song.getSongTitle(), Toast.LENGTH_SHORT).show();
            checkIsPlaying = true;
            changePlayIcon();
            playSong();
            maxTime.setText(setMaxTime(audioServiceBinder.getTotalAudioDuration()));
        }
    }

    public void playButtonClick() {
        if (!checkIsPlaying) {
            checkIsPlaying = true;
           changePlayIcon();
            if(songPaused){
                audioServiceBinder.resumeAudio();
                songPaused = false;
                        }
            else if(!songPaused)
            playSong();


        } else {
            audioServiceBinder.pauseAudio();
            checkIsPlaying = false;
            songPaused = true;
            changePlayIcon();

        }
    }

    public void changePlayIcon(){
        if (checkIsPlaying) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                playButton.setImageDrawable(getDrawable(R.drawable.pause));
            else
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        }else{
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                playButton.setImageDrawable(getDrawable(R.drawable.play));
            else
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
        }

    }

    public void playSong() {

        audioServiceBinder.setSong(song);
        audioServiceBinder.setSongs(songsArrayList);
        audioServiceBinder.setFileUri(Uri.parse(song.getmSongUri()));
        audioServiceBinder.setContext(this);
        createAudioProgressbarUpdater();
        audioServiceBinder.setProgressHandler(progressUpdateHandler);
        audioServiceBinder.startAudio();
    }

    private void bindBackgroundAudioService() {
        if (audioServiceBinder == null) {
            Intent intent = new Intent(DetailActivity.this, BackgroundAudioService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        }
    }

    private void unBoundAudioService() {
        if (audioServiceBinder != null)
            unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
      unBoundAudioService();
        super.onDestroy();
    }


    private void createAudioProgressbarUpdater() {
        if (progressUpdateHandler == null) {
            progressUpdateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == audioServiceBinder.UPDATE_PROGRESS_KEY) {
                        if (audioServiceBinder != null) {
                            int currentProgress = audioServiceBinder.getAudioProgress();
                            currentTime.setText(String.valueOf(currentProgress));
                            mSeekBar.setProgress(audioServiceBinder.getCurrentPosition()/1000);
                            String x = createTimeLable(audioServiceBinder.getCurrentPosition());
                            currentTime.setText(x);
                        }
                    }
                }
            };
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setPosterImage() {

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        byte[] rawCover;
        Bitmap cover;
        Uri uri = Uri.parse(song.getmSongUri());
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        metadataRetriever.setDataSource(getApplicationContext(), uri);
        rawCover = metadataRetriever.getEmbeddedPicture();
        if (null != rawCover) {
            cover = BitmapFactory.decodeByteArray(rawCover, 0, rawCover.length, bfo);
            Bitmap songImage = BitmapFactory
                    .decodeByteArray(rawCover, 0, rawCover.length);
            mPosterCircle.setImageBitmap(songImage);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            background = blurImage(this, cover);
            BitmapDrawable ob = new BitmapDrawable(getResources(), background);
            final View view = this.getWindow().getDecorView();
            view.setBackground(ob);
        }

    }

    @Override
    public void onAudioFocusChange(int i) {
        if(i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        {
            // Pause
            audioServiceBinder.pauseAudio();
        }
        else if(i == AudioManager.AUDIOFOCUS_GAIN)
        {
            // Resume
            audioServiceBinder.startAudio();
        }
        else if(i == AudioManager.AUDIOFOCUS_LOSS)
        {
            // Stop or pause depending on your need
            audioServiceBinder.startAudio();
        }
    }
}

*/
// public void iniializePlayer(){
// MediaSource mediaSource = new ExtractorMediaSource.Factory(Uri.parse(song.getmSongUri()));
        /*playerView.setPlayer(player);
        player.prepare(mediaSource, false, false);
        player.setPlayWhenReady(true);*/
// RenderersFactory renderersFactory = new DefaultRenderersFactory(this, null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
//TrackSelector trackSelector = new DefaultTrackSelector();
//ExtractorFactory extractorFactory = new DefaultExtractorsFactory();
//DataSource dataSource = new DefaultDataSourceFactory(this, Utill.getUserAgent(this, "ExoPlayerIntro"));
// MediaSource mediaSource = new ExtractorMediaSource(uri, dataSource, extractorFactory, new Handler(), Throwable::printStackTrace);
//player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
// player.prepare(mediaSource);
// player.setPlayWhenReady(true);

// }
   /* public void getColor(Bitmap bitmap) {
       Palette p = Palette.from(bitmap).generate();
        Palette.Swatch vibrantSwatch = p.getVibrantSwatch();
        if (vibrantSwatch != null) {
            // int titleColor = vibrantSwatch.getTitleTextColor();
            int color = vibrantSwatch.getRgb();
            int titleColor = vibrant = p.getDarkMutedColor(color);
            colorBG = titleColor;
            //View view = this.getWindow().getDecorView();
            //view.setBackgroundColor(titleColor);\
            // BitmapDrawable ob = new BitmapDrawable(getResources(), background);
            //view.setBackgroundDrawable(ob);


        }
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette p) {
                // Use generated instance
                Palette.Swatch vibrant = p.getVibrantSwatch();
                if (vibrant != null) {
                    int color = vibrant.getRgb();
                    int titleColor = p.getDarkMutedColor(color);
                    colorBG = titleColor;
                    // ...
                }
            }
        });
    }
        */
//L̥}
