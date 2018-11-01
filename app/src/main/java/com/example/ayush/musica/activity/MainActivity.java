package com.example.ayush.musica.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.ayush.musica.R;
import com.example.ayush.musica.adapters.MusicListAdapter;
import com.example.ayush.musica.interfaces.MusicClickListner;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.utility.Store;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.ayush.musica.utility.AppConstants.FIRST_LAUNCH_KEY;
import static com.example.ayush.musica.utility.AppConstants.IS_FIRST_LAUNCH;

public class MainActivity extends AppCompatActivity  implements MusicClickListner {

    private ArrayList<Songs> arrayList;
    @BindView(R.id.main_music_Recyler)
    RecyclerView mRecyclerView;
    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;

    private MusicListAdapter mAdapter;
    private SharedPreferences sharedPreferences;
    private Store store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        store = new Store(getApplicationContext());

        sharedPreferences = this.getSharedPreferences(FIRST_LAUNCH_KEY, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(IS_FIRST_LAUNCH, true)) {
            permission();
            sharedPreferences.edit().putBoolean(IS_FIRST_LAUNCH, false).apply();
        } else {
            arrayList = new ArrayList<>();
            arrayList = store.getMediaList();
            mAdapter = new MusicListAdapter(this, arrayList);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            mRecyclerView.setLayoutManager(layoutManager);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setAdapter(mAdapter);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    doStuff();
                }

            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
    public void doStuff(){

        getMusic();
        Collections.sort(arrayList);

        mAdapter = new MusicListAdapter(this, arrayList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);


    }
    public void getMusic()
    {
        arrayList = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if(songCursor != null && songCursor.moveToFirst())
        {
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

            } while(songCursor.moveToNext());


        }
    }
    public void permission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
               // Log.i("FUCK","ERROR PERMISSION");
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        0);


            }
        } else {
            // Permission has already been granted
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
    public void onMusicNameClick(Songs song,int position) {
        Intent intent = new Intent(this,DetailActivity.class);
        storeDetails(position);
        startActivity(intent);
    }
    public void storeDetails(int i){
        store.storeSongIndex(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
}