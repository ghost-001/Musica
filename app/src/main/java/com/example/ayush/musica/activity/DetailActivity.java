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
import com.example.ayush.musica.database.SongDatabase;
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
    private SongService songService;
    private Intent playIntent;
    private SongDatabase songDatabase;

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
    @BindView(R.id.detail_music_name)
    TextView mTitle;
    @BindView(R.id.detail_circle_image)
    ImageView mPosterCircle;
    @BindView(R.id.detail_relative_root)
    RelativeLayout mRelativeLayout;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            SongService.LocalBinder binder = (SongService.LocalBinder) iBinder;
            songService = binder.getService();
            serviceBound = true;
            playIntent = new Intent(DetailActivity.this, SongService.class);
            // playButtonClick();

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
        ButterKnife.bind(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        registerReceiver(notifyChangeActiivty, intentFilter);

        songsArrayList = new ArrayList<>();
        store = new Store(this);
        songsArrayList = store.getMediaList();
        songIndex = store.getSongIndex();
        if (songIndex == -1)
            songIndex = 0;
        song = songsArrayList.get(songIndex);

        mRelativeLayout.setAlpha(.9f);
        mTitle.getBackground().setAlpha(110);
        bindBackgroundAudioService();
        createAudioProgressbarUpdater();
        setPosterImage();
        songDatabase = SongDatabase.getsInstance(getApplicationContext());

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

                if (b) songService.changePosition(seekBar.getProgress());

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
//            playIntent.setAction(ACTION_PLAY);
            changePlayIcon();
            if (songPaused) {
                songPaused = false;
                resumeMedia();
            } else if (!songPaused)
                playMedia();
        } else {
            songService.pauseMedia();
            playIntent.setAction(ACTION_PAUSE);
            checkIsPlaying = false;
            songPaused = true;
            changePlayIcon();
            playMedia();
        }

    }

    private void resumeMedia() {
        songService.resumeMedia();
    }

    private void playMedia() {
        if (serviceBound && !songService.isPlaying()) {
            songService.setProgressHandler(progressUpdateHandler);
            ContextCompat.startForegroundService(DetailActivity.this, playIntent);

        } else {
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);
            // createAudioProgressbarUpdater();
            songService.setProgressHandler(progressUpdateHandler);
            sendBroadcast(broadcastIntent);
        }
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
            songService.stopMedia();
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);
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
            songService.stopMedia();
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);
            sendBroadcast(broadcastIntent);
            maxTime.setText(setTime(songService.getTotalAudioDuration()));
        }

    }


    public void changePlayIcon() {
        if (checkIsPlaying) {
            playButton.setImageDrawable(getDrawable(R.drawable.pause));
        } else {
            playButton.setImageDrawable(getDrawable(R.drawable.play));
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
                    if (msg.what == songService.UPDATE_PROGRESS_KEY) {
                        if (songService != null) {
                            try {
                                int currentProgress = songService.getAudioProgress();
                                int currentDuration = songService.getCurrentPosition();
                                int totalDuration = songService.getTotalAudioDuration();
                                mSeekBar.setMax(totalDuration / 1000);
                                currentTime.setText(String.valueOf(currentProgress));
                                mSeekBar.setProgress(currentDuration / 1000);
                                String x = setTime(currentDuration);
                                currentTime.setText(x);
                                maxTime.setText(setTime(totalDuration));
                                if (totalDuration == currentDuration) {
                                    songService.stopSelf();
                                    songService.resetMediaPlayer();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(SONG_SERVICE_TAG, "ERROR IN DETAIL ACTIVITY HANDLER");
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
        Uri uri = Uri.parse(song.getSongUri());
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

    public String setTime(long x) {
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
        if (songService.isPlaying()) {
            songService.saveCurrentPosition();
            songPaused = true;
            outState.putBoolean("SongPaused", serviceBound);
            changePlayIcon();
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
        songPaused = savedInstanceState.getBoolean("SongPaused");
        if (songPaused) {
            checkIsPlaying = true;
            changePlayIcon();
            playIntent = new Intent(DetailActivity.this, SongService.class);
            playIntent.setAction(ACTION_PLAY);
            ContextCompat.startForegroundService(DetailActivity.this, playIntent);
        }
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
/*
public void getColor(Bitmap bitmap) {
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
//}
