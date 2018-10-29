package com.example.ayush.musica.utility;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static com.example.ayush.musica.AppConstants.MEDIA_ARRAY_LIST;
import static com.example.ayush.musica.AppConstants.SONG_INDEX;
import static com.example.ayush.musica.AppConstants.STORE;

public class Store {
    private SharedPreferences sharedPreferences;
    private Context context;

    public Store(Context c) {
        this.context = c;
    }

    public void saveMediaList(ArrayList<Songs> s) {
        sharedPreferences = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(s);
        editor.putString(MEDIA_ARRAY_LIST, json);
        editor.apply();
    }

    public ArrayList<Songs> getMediaList() {
        sharedPreferences = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(MEDIA_ARRAY_LIST, null);
        Type type = new TypeToken<ArrayList<Songs>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void storeSongIndex(int i) {
        sharedPreferences = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SONG_INDEX, i);
        editor.apply();
    }

    public int getSongIndex(){
        sharedPreferences = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(SONG_INDEX,-1);
    }
}
