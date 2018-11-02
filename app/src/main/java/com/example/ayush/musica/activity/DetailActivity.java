package com.example.ayush.musica.activity;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ayush.musica.AnalyticsTracker;
import com.example.ayush.musica.R;
import com.example.ayush.musica.database.AppExecutors;
import com.example.ayush.musica.database.SongDatabase;
import com.example.ayush.musica.service.SongService;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.utility.Store;
import com.example.ayush.musica.viewModel.DatabaseViewModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.ayush.musica.utility.AppConstants.ACTION_NEXT;
import static com.example.ayush.musica.utility.AppConstants.ACTION_PAUSE;
import static com.example.ayush.musica.utility.AppConstants.ACTION_PLAY;
import static com.example.ayush.musica.utility.AppConstants.ACTION_PREVIOUS;
import static com.example.ayush.musica.utility.AppConstants.ADDED_TO_FAVOURITES;
import static com.example.ayush.musica.utility.AppConstants.BROADCAST_PLAY_NEW_SONG;
import static com.example.ayush.musica.utility.AppConstants.CHECK_IS_PLAYING;
import static com.example.ayush.musica.utility.AppConstants.DETAIL_ACTIVITY_CATEGORY;
import static com.example.ayush.musica.utility.AppConstants.DETAIL_ACTIVITY_SCREEN_NAME;
import static com.example.ayush.musica.utility.AppConstants.ERROR_IN_HANDLER;
import static com.example.ayush.musica.utility.AppConstants.MEDIA_PLAYER_POSITION;
import static com.example.ayush.musica.utility.AppConstants.SONG_SERVICE_TAG;
import static com.example.ayush.musica.utility.BlurBuilder.blurImage;

public class DetailActivity extends AppCompatActivity {


    ArrayList<Songs> songsArrayList;
    Songs song;
    Integer songIndex;
    Long songId = -1L;
    Bitmap background = null;
    @BindView(R.id.detail_relative_root)
    ScrollView mScrollView;

    private Handler progressUpdateHandler = null;

    private boolean checkIsPlaying = false;
    private boolean songPaused = false;
    private boolean serviceBound = false;
    private boolean checkBookmark = false;
    private boolean playingOld = false;

    private Store store;
    private SongService songService;
    private Intent playIntent;
    private SongDatabase songDatabase;
    private int pos = 0;
    private AnalyticsTracker application;
    private Integer songDbId;
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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            SongService.LocalBinder binder = (SongService.LocalBinder) iBinder;
            songService = binder.getService();
            serviceBound = true;
            playIntent = new Intent(DetailActivity.this, SongService.class);
            songService.setProgressHandler(progressUpdateHandler);
            if (playingOld) {
                playIntent.setAction(ACTION_PLAY);
                ContextCompat.startForegroundService(DetailActivity.this, playIntent);
                songService.changePosition(pos);
                changePlayIcon();
            } else playButtonClick();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    private BroadcastReceiver notifyChangeActivity = new BroadcastReceiver() {
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

        //Analytics
        application = (AnalyticsTracker) getApplication();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PLAY);
        intentFilter.addAction(ACTION_PAUSE);
        registerReceiver(notifyChangeActivity, intentFilter);


        if (savedInstanceState != null) {
            checkIsPlaying = savedInstanceState.getBoolean(CHECK_IS_PLAYING);
            pos = savedInstanceState.getInt(MEDIA_PLAYER_POSITION);
            playingOld = true;
        }

        songsArrayList = new ArrayList<>();
        store = new Store(this);
        songsArrayList = store.getMediaList();
        songIndex = store.getSongIndex();
        if (songIndex == -1)
            songIndex = 0;
        song = songsArrayList.get(songIndex);
        songId = song.getSongID();

        songDatabase = SongDatabase.getsInstance(getApplicationContext());
        checkIfBookmark();


        mScrollView.setAlpha(.9f);
        mTitle.getBackground().setAlpha(110);
        bindBackgroundAudioService();
        createAudioProgressbarUpdater();
        setPosterImage();

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.trackEvent(DETAIL_ACTIVITY_CATEGORY, ACTION_PLAY + ACTION_PAUSE, DETAIL_ACTIVITY_SCREEN_NAME);
                playButtonClick();

            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.trackEvent(DETAIL_ACTIVITY_CATEGORY, ACTION_PREVIOUS, DETAIL_ACTIVITY_SCREEN_NAME);
                playPreviousSong();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.trackEvent(DETAIL_ACTIVITY_CATEGORY, ACTION_NEXT, DETAIL_ACTIVITY_SCREEN_NAME);
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
                if (checkBookmark)
                    deleteFromDatabase();
                else
                    saveFavourite();

            }
        });
    }

    public void checkIfBookmark() {
        searchDbAsynckTask task = new searchDbAsynckTask();
        task.execute();
    }

    public void deleteFromDatabase() {
        songDatabase.favouriteDao().deleteSongByID(songDbId);
        checkBookmark = false;
        changeFavIcon();
        Toast.makeText(DetailActivity.this, getResources().getString(R.string.delete_from_favourites), Toast.LENGTH_SHORT).show();
    }

    public void saveFavourite() {
        application.trackEvent(DETAIL_ACTIVITY_CATEGORY, ADDED_TO_FAVOURITES, DETAIL_ACTIVITY_SCREEN_NAME);
        AppExecutors.getsInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                songDatabase.favouriteDao().InsertSong(song);
            }
        });
        checkBookmark = true;
        changeFavIcon();
        Toast.makeText(this, getResources().getString(R.string.added_to_favourites), Toast.LENGTH_SHORT).show();
    }


    private void playButtonClick() {
        if (!checkIsPlaying) {
            changePlayIcon();
            if (!songPaused)
                playMedia();
            else {
                songPaused = false;
                checkIsPlaying = true;
                resumeMedia();
            }
        } else {
            playIntent.setAction(ACTION_PAUSE);
            ContextCompat.startForegroundService(DetailActivity.this, playIntent);
            checkIsPlaying = false;
            songPaused = true;
            changePlayIcon();
        }
    }


    private void resumeMedia() {
        songService.resumeMedia();
    }

    private void playMedia() {
        if (serviceBound && !songService.isPlaying()) {
            checkIsPlaying = true;
            songService.setProgressHandler(progressUpdateHandler);
            ContextCompat.startForegroundService(DetailActivity.this, playIntent);
        } else {
            checkIsPlaying = true;
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);
            createAudioProgressbarUpdater();
            songService.setProgressHandler(progressUpdateHandler);
            sendBroadcast(broadcastIntent);
        }
    }

    public void playPreviousSong() {
        if (songIndex > 0) {
            songIndex--;
            store.storeSongIndex(songIndex);

            song = songsArrayList.get(songIndex);
            songId = song.getSongID();
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);
            sendBroadcast(broadcastIntent);
            setPosterImage();
            checkIsPlaying = true;
            checkBookmark = false;
            checkIfBookmark();
            changeFavIcon();
            changePlayIcon();
            songService.stopMedia();


        }

    }

    public void playNextSong() {
        if (songIndex < songsArrayList.size() - 1) {

            songIndex++;
            store.storeSongIndex(songIndex);

            song = songsArrayList.get(songIndex);
            songId = song.getSongID();
            Intent broadcastIntent = new Intent(BROADCAST_PLAY_NEW_SONG);
            sendBroadcast(broadcastIntent);
            setPosterImage();
            checkIsPlaying = true;
            checkBookmark = false;
            checkIfBookmark();
            changeFavIcon();
            changePlayIcon();
            songService.stopMedia();

            maxTime.setText(setTime(songService.getTotalAudioDuration()));
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

                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(SONG_SERVICE_TAG, ERROR_IN_HANDLER);
                                application.trackException(e);
                            }
                        }
                    }
                }
            };
        }
    }

    public void setPosterImage() {
        mTitle.setText(song.getSongTitle());

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        byte[] rawCover;
        Bitmap cover;

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
        } else {
            mPosterCircle.setImageDrawable(getResources().getDrawable(R.drawable.image));
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

    public void changePlayIcon() {
        if (checkIsPlaying) {
            playButton.setImageDrawable(getDrawable(R.drawable.pause));
        } else {
            playButton.setImageDrawable(getDrawable(R.drawable.play));
        }

    }

    private void changeFavIcon() {
        if (checkBookmark)
            favouriteView.setImageDrawable(getDrawable(R.drawable.ic_favorite_black_24dp));
        else
            favouriteView.setImageDrawable(getDrawable(R.drawable.ic_favorite_border_black_24dp));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (serviceBound) {
            outState.putBoolean(CHECK_IS_PLAYING, checkIsPlaying);
            outState.putInt(MEDIA_PLAYER_POSITION, songService.getCurrentPosition() / 1000);
        }
        if (songService.isPlaying()) {
            songService.saveCurrentPosition();
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        application.trackScreenView(DETAIL_ACTIVITY_SCREEN_NAME);
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        if (serviceBound) {
            unbindService(serviceConnection);
            songService.stopSelf();
        }
        unregisterReceiver(notifyChangeActivity);
        super.onDestroy();
    }

    private class searchDbAsynckTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            final DatabaseViewModel viewModel = ViewModelProviders.of(DetailActivity.this).get(DatabaseViewModel.class);
            viewModel.getSongList().observe(DetailActivity.this, new Observer<List<Songs>>() {
                @Override
                public void onChanged(@Nullable List<Songs> songList) {
                    if (songList.size() != 0) {
                        ArrayList<Songs> ss = new ArrayList<Songs>(songList);
                        for (Songs a : ss) {
                            if (a.getSongID() == songId) {
                                songDbId = a.getId();
                                checkBookmark = true;
                                changeFavIcon();
                                break;
                            }
                        }
                    }
                }
            });
            return null;
        }
    }
}
