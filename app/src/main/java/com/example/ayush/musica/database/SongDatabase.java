package com.example.ayush.musica.database;

import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.locks.Lock;

public abstract class SongDatabase extends RoomDatabase {
private static final String LOG_TAG = "SONGDATABASE";
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "FavouriteSongList";
    private static SongDatabase sInstance;

    public static SongDatabase getsInstance(Context context){
        if(sInstance == null){
            synchronized (LOCK){
                Log.d(LOG_TAG, "Creating new Database Instance");
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                                        SongDatabase.class,SongDatabase.DATABASE_NAME)
                                         .build();
            }
        }
        Log.d(LOG_TAG,"Getting Database Instance");
        return sInstance;
    }
}
