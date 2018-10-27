package com.example.ayush.musica.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
//import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.ayush.musica.interfaces.MusicClickListner;
import com.example.ayush.musica.adapters.MusicListAdapter;
import com.example.ayush.musica.R;
import com.example.ayush.musica.utility.Songs;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  implements MusicClickListner {
    private Integer color = Color.parseColor("#000000");
    public int vibrant;
    public int mDarkMutedColor;
    public int mMutedColor;
    public View view;



    private ArrayList<Songs> arrayList = new ArrayList<>();
    RecyclerView mRecyclerView;
    private MusicListAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.main_music_Recyler);
        permission();


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
        mAdapter = new MusicListAdapter(this, arrayList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);


    }
    public void getMusic()
    {
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

            // Permission is not granted
            // Should we show an explanation?
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

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            doStuff();
        }



    }


    @Override
    public void onMusicNameClick(Songs song,int position) {
        Toast.makeText(this,song.getSongTitle()+"CLICKED",Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this,DetailActivity.class);
        intent.putExtra("songList",arrayList);
        intent.putExtra("songIndex",position);
        intent.putExtra("song",song);
        Log.i("Array", "arrayList size in main" + arrayList.size());
        startActivity(intent);
    }
}










    /*public void blurImageView() {
        // ImageView resultImage = (ImageView) findViewById(R.id.image);
        Bitmap resultBmp = com.example.ayush.bakingapp.BlurBuilder.blurImage(this, BitmapFactory.decodeResource(getResources(), R.drawable.image));
        //resultImage.setImageBitmap(resultBmp);
        //resultImage.setAlpha(100);

        BitmapDrawable ob = new BitmapDrawable(getResources(), resultBmp);
        view.setBackgroundColor(getResources().getColor(R.color.white));
        view.setBackground(ob);
        loadImageColor();
        Toast.makeText(this, "DOOOOONEEEE", Toast.LENGTH_SHORT).show();
        view.getBackground().setAlpha(50);




      /*  final Activity activity = getActivity();
        final View content = activity.findViewById(android.R.id.content).getRootView();
        if (content.getWidth() > 0) {
            Bitmap image = BitmapFactory.decodeResource(getResources(), constants.images[value]);
            root.setBackgroundDrawable(new BitmapDrawable(activity.getResources(), image));
        } else {
            content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Bitmap image = BitmapFactory.decodeResource(getResources(), constants.images[value]);
                    // Bitmap image = BlurBuilder.blur(content,getContext());
                    root.setBackgroundDrawable(new BitmapDrawable(activity.getResources(), image));
                }
            });
        }*/
        //root.getBackground().setAlpha(250);

    /*

    public void loadImageColor() {

        Log.i("TAG", "BEFORE PICASSO");
        int drawableResourceId = this.getResources().getIdentifier("image", "drawable", this.getPackageName());

       /* Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@NonNull Palette palette) {
                Palette.Swatch textSwatch = palette.getVibrantSwatch();
                if (textSwatch != null) {
                    color = textSwatch.getRgb();
                    Log.i("TAG","PICASSO Target onGenerated");
                }
                vibrant = palette.getLightVibrantColor(color);
                mDarkMutedColor = palette.getDarkMutedColor(color);
                mMutedColor = palette.getMutedColor(color);
                setBackground();

            }
        });

        Picasso.get().load(drawableResourceId)
                .into(new Target() {



                          @Override
                          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                              Log.i("TAG","PICASSO Target onBitmapLoaded");
                              Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                  @Override
                                  public void onGenerated(@NonNull Palette palette) {
                                      Palette.Swatch textSwatch = palette.getVibrantSwatch();
                                      if (textSwatch != null) {
                                          color = textSwatch.getRgb();
                                          Log.i("TAG","PICASSO Target onGenerated");
                                      }
                                      vibrant = palette.getLightVibrantColor(color);
                                      mDarkMutedColor = palette.getDarkMutedColor(color);
                                      mMutedColor = palette.getMutedColor(color);
                                      setBackground();

                                  }
                              });

                          }

                          @Override
                          public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                              Log.i("TAG", e.getMessage());

                          }

                          @Override
                          public void onPrepareLoad(Drawable placeHolderDrawable) {

                          }
                      }
                );
    }


    public void setBackground() {
        //view.setBackgroundColor(vibrant);
        Toast.makeText(this, "COLOR ADDED", Toast.LENGTH_SHORT).show();
    }

}
*/