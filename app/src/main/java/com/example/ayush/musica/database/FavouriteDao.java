package com.example.ayush.musica.database;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.ayush.musica.utility.Songs;

import java.util.List;

@Dao
public interface FavouriteDao {

    @Query("SELECT * FROM favouritesPlaylist")
    LiveData<List<Songs>> loadAllFavouriteSongs();

    @Insert
    void InsertSong(Songs song);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateFavourites(Songs song);

    @Query("Delete FROM favouritesPlaylist where id =:id")
    void deleteSongByID(int id);


    @Query("DELETE FROM favouritesPlaylist")
    void deleteTable();
}
