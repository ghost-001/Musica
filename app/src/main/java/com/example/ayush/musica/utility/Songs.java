package com.example.ayush.musica.utility;

import android.os.Parcel;
import android.os.Parcelable;

public class Songs implements Parcelable {
    private long mSongID;
    private String mSongTitle;
    private String mSongartist;
    private String mSongUri;


    public Songs(long id, String title, String artist, String uri){
        mSongID = id;
        mSongTitle = title;
        mSongartist = artist;
        mSongUri = uri;
    }

    public String getmSongUri() {
        return mSongUri;
    }

    public void setmSongUri(String mSongUri) {
        this.mSongUri = mSongUri;
    }

    public long getSongID(){
        return mSongID;
    }

    public String getSongTitle(){
        return mSongTitle;
    }
    public String getmSongartist() {
        return mSongartist;
    }

    public void setmSongartist(String mSongartist) {
        this.mSongartist = mSongartist;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mSongID);
        dest.writeString(this.mSongTitle);
        dest.writeString(this.mSongartist);
        dest.writeString(this.mSongUri);
    }

    protected Songs(Parcel in) {
        this.mSongID = in.readLong();
        this.mSongTitle = in.readString();
        this.mSongartist = in.readString();
        this.mSongUri = in.readString();
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