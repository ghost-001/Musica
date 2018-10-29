package com.example.ayush.musica.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.ayush.musica.utility.Songs;

import java.util.List;

@Dao
public interface FavouriteDao {

    @Query("SELECT * FROM favouritesPlaylist")
    List<Songs> loadAllFavouriteSongs();

    @Insert
    void InsertSong(Songs song);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateFavourites(Songs song);

    @Delete
    void deleteSong(Songs song);
}
