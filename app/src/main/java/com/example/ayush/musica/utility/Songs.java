package com.example.ayush.musica.utility;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "favouritesPlaylist")
public class Songs implements Parcelable {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @PrimaryKey(autoGenerate = true)
    private int id;

    private long SongID;
    private String SongTitle;
    private String SongArtist;
    private String SongUri;

   public Songs(){

   }

    public Songs(int id, long songId, String title, String artist, String uri) {
        id = id;
        SongID = songId;
        SongTitle = title;
        SongArtist = artist;
        SongUri = uri;
    }

    @Ignore
    public Songs(long songId, String title, String artist, String uri) {

        SongID = songId;
        SongTitle = title;
        SongArtist = artist;
        SongUri = uri;
    }

    public String getSongUri() {
        return SongUri;
    }

    public void setSongUri(String mSongUri) {
        this.SongUri = mSongUri;
    }

    public void setSongID(long songID) {
        SongID = songID;
    }

    public long getSongID() {
        return SongID;
    }

    public void setSongTitle(String songTitle) {
        SongTitle = songTitle;
    }


    public String getSongTitle() {
        return SongTitle;
    }

    public String getSongArtist() {
        return SongArtist;
    }

    public void setSongArtist(String mSongartist) {
        this.SongArtist = mSongartist;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.SongID);
        dest.writeString(this.SongTitle);
        dest.writeString(this.SongArtist);
        dest.writeString(this.SongUri);
    }

    protected Songs(Parcel in) {
        this.SongID = in.readLong();
        this.SongTitle = in.readString();
        this.SongArtist = in.readString();
        this.SongUri = in.readString();
    }

    public static final Creator<Songs> CREATOR = new Creator<Songs>() {
        @Override
        public Songs createFromParcel(Parcel source) {
            return new Songs(source);
        }

        @Override
        public Songs[] newArray(int size) {
            return new Songs[size];
        }
    };
}