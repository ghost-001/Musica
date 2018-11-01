package com.example.ayush.musica.activity;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ayush.musica.R;
import com.example.ayush.musica.adapters.MusicListAdapter;
import com.example.ayush.musica.database.SongDatabase;
import com.example.ayush.musica.interfaces.MusicClickListner;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.utility.Store;
import com.example.ayush.musica.viewModel.DatabaseViewModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;


public class FavouritesActivity extends AppCompatActivity implements MusicClickListner {


    @BindView(R.id.fav_music_Recyler)
    RecyclerView mRecyclerView;
    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.fav_fav_text)
    TextView mFavText;

    private MusicListAdapter mAdapter;
    private SongDatabase songDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);
        ButterKnife.bind(this);
        songDatabase = SongDatabase.getsInstance(getApplicationContext());

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.favourites));

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        setupDatabaseViewModel();
    }

    public void setupDatabaseViewModel() {
        DatabaseViewModel viewModel = ViewModelProviders.of(this).get(DatabaseViewModel.class);
        viewModel.getSongList().observe(this, new Observer<List<Songs>>() {
            @Override
            public void onChanged(@Nullable List<Songs> songs) {
                ArrayList<Songs> ss = new ArrayList<Songs>(songs);
                if (ss.isEmpty()) {
                    mRecyclerView.setVisibility(GONE);
                    mFavText.setVisibility(View.VISIBLE);
                } else {
                    mAdapter = new MusicListAdapter(FavouritesActivity.this, ss);
                    Log.i("FAB", "ACTIVELY Retrieving from LIVEDATA in VIEWMODEL");
                    mRecyclerView.setAdapter(mAdapter);
                }
            }
        });
    }

    public void findBySongId(long sId) {
        Store store = new Store(getApplicationContext());
        ArrayList<Songs> songList = store.getMediaList();
        for (int i = 0; i < songList.size(); i++) {
            if (songList.get(i).getSongID() == sId) {
                store.storeSongIndex(i);
                break;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.favourites_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_database:
                songDatabase.favouriteDao().deleteTable();
                Toast.makeText(FavouritesActivity.this, getResources().getString(R.string.delete_table_msg), Toast.LENGTH_SHORT).show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onMusicNameClick(Songs song, int position) {
        findBySongId(song.getSongID());
        Intent intent = new Intent(FavouritesActivity.this, DetailActivity.class);
        startActivity(intent);
    }


}
