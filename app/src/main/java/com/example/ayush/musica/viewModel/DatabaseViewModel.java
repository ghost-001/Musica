package com.example.ayush.musica.viewModel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.ayush.musica.database.SongDatabase;
import com.example.ayush.musica.utility.Songs;

import java.util.List;

public class DatabaseViewModel extends AndroidViewModel {

    private LiveData<List<Songs>> songList;

    public DatabaseViewModel(@NonNull Application application) {
        super(application);
        SongDatabase songDatabase = SongDatabase.getsInstance(this.getApplication());
        Log.i("FAB", "ACTIVELY Retrieving from db ");
        songList = songDatabase.favouriteDao().loadAllFavouriteSongs();
    }

    public LiveData<List<Songs>> getSongList() {
        return songList;
    }
}
