package com.example.ayush.musica.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.ayush.musica.AnalyticsTracker;
import com.example.ayush.musica.R;
import com.example.ayush.musica.adapters.MusicListAdapter;
import com.example.ayush.musica.adapters.MusicSearchAdapter;
import com.example.ayush.musica.interfaces.MusicClickListner;
import com.example.ayush.musica.interfaces.MusicSearchClickListner;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.utility.Store;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.ayush.musica.utility.AppConstants.FIRST_LAUNCH_KEY;
import static com.example.ayush.musica.utility.AppConstants.IS_FIRST_LAUNCH;
import static com.example.ayush.musica.utility.AppConstants.MAIN_ACTIVITY_SCREEN_NAME;

public class MainActivity extends AppCompatActivity implements MusicClickListner, MusicSearchClickListner {

    private ArrayList<Songs> arrayList;
    @BindView(R.id.main_music_Recyler)
    RecyclerView mRecyclerView;
    Toolbar mToolbar;
    MaterialSearchView materialSearchView;

    private MusicListAdapter mAdapter;
    private MusicSearchAdapter mSearchAdapter;
    private SharedPreferences sharedPreferences;
    private Store store;
    private AnalyticsTracker application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Adding the Analytics tracker here
        application = (AnalyticsTracker) getApplication();


        materialSearchView = findViewById(R.id.search_view);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        store = new Store(getApplicationContext());

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        sharedPreferences = this.getSharedPreferences(FIRST_LAUNCH_KEY, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(IS_FIRST_LAUNCH, true)) {
            permission();
            sharedPreferences.edit().putBoolean(IS_FIRST_LAUNCH, false).apply();
        } else {
            arrayList = new ArrayList<>();
            arrayList = store.getMediaList();
            mAdapter = new MusicListAdapter(this, arrayList);
            mRecyclerView.setAdapter(mAdapter);
        }


        mSearchAdapter = new MusicSearchAdapter(this, arrayList);
        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && !newText.isEmpty()) {
                    ArrayList<Songs> listFound = new ArrayList<>();
                    for (Songs item : arrayList) {
                        if (item.getSongTitle().toLowerCase().contains(newText.toLowerCase())) {
                            listFound.add(item);
                        }
                    }
                    MusicSearchAdapter a = new MusicSearchAdapter(MainActivity.this, listFound);
                    mRecyclerView.setAdapter(a);
                }
                return true;
            }
        });

        materialSearchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {

            }

            @Override
            public void onSearchViewClosed() {
                mAdapter.setMusicAdapter(arrayList);
                mRecyclerView.setAdapter(mAdapter);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doStuff();
                } else {
                    showDialog();
                }
            }
        }
    }

    public void doStuff() {

        getMusic();
        Collections.sort(arrayList);

        mAdapter = new MusicListAdapter(this, arrayList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);


    }

    public void getMusic() {
        arrayList = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            int songId = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int uri = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);


            do {
                long currentId = songCursor.getLong(songId);
                String currentTitle = songCursor.getString(songTitle);
                String currentArtist = songCursor.getString(songArtist);
                String currentUri = songCursor.getString(uri);

                arrayList.add(new Songs(currentId, currentTitle, currentArtist, currentUri));

            } while (songCursor.moveToNext());


        }
    }

    public void permission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        0);
            }
        } else {
            doStuff();
        }


    }

    public void onRefresh() {
        getMusic();
        Collections.sort(arrayList);
        store.saveMediaList(arrayList);
        store.storeSongIndex(-1);
    }

    @Override
    public void onMusicNameClick(Songs song, int position) {
        Intent intent = new Intent(this, DetailActivity.class);
        storeDetails(position);
        startActivity(intent);
    }

    @Override
    public void onMusicSearchNameClick(Songs song, int position) {
        Intent intent = new Intent(this, DetailActivity.class);
        findBySongId(song.getSongID());
        startActivity(intent);
    }

    public void findBySongId(long sId) {
        searchAsynckTask s = new searchAsynckTask();
        s.execute(sId);
    }

    public void storeDetails(int i) {
        store.storeSongIndex(i);
    }

    public void showDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(getResources().getString(R.string.access_denied))
                .setMessage(getResources().getString(R.string.access_denied_explanation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        materialSearchView.setMenuItem(item);

        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        application.trackScreenView(MAIN_ACTIVITY_SCREEN_NAME);
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_view:
                materialSearchView.setMenuItem(item);
            case R.id.songs_favourite:
                Intent i = new Intent(MainActivity.this, FavouritesActivity.class);
                startActivity(i);
            case R.id.song_list_Refresh:
                onRefresh();
                Toast.makeText(MainActivity.this, getResources().getString(R.string.refreshed_all_songs), Toast.LENGTH_SHORT).show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (materialSearchView.isSearchOpen()) {
            materialSearchView.closeSearch();
            return;
        }
        super.onBackPressed();
    }

    private class searchAsynckTask extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(Long... longs) {
            Long sId = longs[0];
            ArrayList<Songs> songList = store.getMediaList();
            for (int i = 0; i < songList.size(); i++) {
                if (songList.get(i).getSongID() == sId) {
                    store.storeSongIndex(i);
                    break;
                }
            }
            return null;
        }
    }
}