package com.example.ayush.musica.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.ayush.musica.R;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.interfaces.MusicClickListner;

import java.util.ArrayList;


public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicViewHolder> {
    private ArrayList<Songs> songs;
    private ArrayList<String> stringArrayList;
    private Context mContext;
    private final MusicClickListner listner;

    public MusicListAdapter(Context context, ArrayList<Songs> arrayList) {
        this.songs = arrayList;
        Log.i("TAG"," "+ songs.size());
        this.stringArrayList = stringArrayList;
        this.mContext = context;
        listner = (MusicClickListner) context;
    }

    @NonNull
    @Override
    public MusicListAdapter.MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_list_item, parent, false);
        MusicViewHolder viewHolder = new MusicViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MusicListAdapter.MusicViewHolder holder, final int position) {
        holder.bindRestaurant(songs.get(position));
        holder.mTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listner.onMusicNameClick(songs.get(position),position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView mTitleTv;
        public MusicViewHolder(View itemView) {
            super(itemView);
            mTitleTv = itemView.findViewById(R.id.music_name);
            mContext = itemView.getContext();
        }
        public void bindRestaurant(Songs song) {
            mTitleTv.setText(song.getSongTitle());
        }
    }

}
