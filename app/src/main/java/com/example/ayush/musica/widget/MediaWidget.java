package com.example.ayush.musica.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.ayush.musica.R;
import com.example.ayush.musica.SongService;
import com.example.ayush.musica.activity.DetailActivity;
import com.example.ayush.musica.utility.Songs;
import com.example.ayush.musica.utility.Store;

import butterknife.internal.Utils;

import static com.example.ayush.musica.AppConstants.ACTION_NEXT;
import static com.example.ayush.musica.AppConstants.ACTION_PAUSE;
import static com.example.ayush.musica.AppConstants.ACTION_PLAY;
import static com.example.ayush.musica.AppConstants.ACTION_PLAY_WIDGET;
import static com.example.ayush.musica.AppConstants.ACTION_PREVIOUS;
import static com.example.ayush.musica.AppConstants.BROADCAST_PLAY_NEW_SONG;


public class MediaWidget extends AppWidgetProvider {
    private static MediaWidget sInstance;
    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.example.ayush.mp3",
                    "com.example.ayush.musica.widget.MediaWidget");

    public static synchronized MediaWidget getInstance() {
        if (sInstance == null) {
            sInstance = new MediaWidget();
        }
        return sInstance;
    }


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        defaultAppWidget(context, appWidgetIds);

        Intent updateIntent = new Intent();
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
        // }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {

    }

    private void defaultAppWidget(Context context, int[] appWidgetId) {

        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.media_widget);

        Store store = new Store(context);
        int index = store.getSongIndex();
        if (index < 0) index = 0;
        Songs song = store.getMediaList().get(index);

        views.setTextViewText(R.id.widget_song_name, song.getSongTitle());
        setButton(context, views, false);
        pushUpdate(context, appWidgetId, views);
    }

    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        return (appWidgetIds.length > 0);
    }

    public void notifyChange(SongService service, String msg) {
        if (hasInstances(service)) {
            if (ACTION_PLAY.equals(msg) || ACTION_PAUSE.equals(msg)) {
                performUpdate(service, null);
            }
        }
    }

    public void performUpdate(SongService service, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.media_widget);
        Store store = new Store(service.getApplicationContext());
        int index = store.getSongIndex();
        if (index < 0) index = 0;
        Songs song = store.getMediaList().get(index);

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        byte[] rawCover;
        Bitmap cover;
        Uri uri = Uri.parse(song.getSongUri());
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        metadataRetriever.setDataSource(service.getApplicationContext(), uri);
        rawCover = metadataRetriever.getEmbeddedPicture();
        if (null != rawCover) {
            bfo.inSampleSize = 2;
            cover = BitmapFactory.decodeByteArray(rawCover, 0, rawCover.length, bfo);
            views.setImageViewBitmap(R.id.widget_background_image,cover);
        }



        views.setViewVisibility(R.id.widget_song_name, View.VISIBLE);
        views.setTextViewText(R.id.widget_song_name, song.getSongTitle());

        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.widget_play_btn, R.drawable.pause);
        } else {
            views.setImageViewResource(R.id.widget_play_btn, R.drawable.play);
        }

        setButton(service, views, playing);
        pushUpdate(service, appWidgetIds, views);

    }

    private void pushUpdate(Context context, int[] appWidgetId, RemoteViews view) {

        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetId != null)
            gm.updateAppWidget(appWidgetId, view);
        else {
            gm.updateAppWidget(THIS_APPWIDGET, view);
        }
    }

    private void setButton(Context context, RemoteViews views, boolean p) {
        Intent intent;
        PendingIntent pendingIntent;

        final ComponentName service = new ComponentName(context, SongService.class);

        if (p) {
            intent = new Intent().setAction(ACTION_PAUSE);
            intent.setComponent(service);
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget_play_btn, pendingIntent);
        } else {
            intent = new Intent().setAction(ACTION_PLAY);
            intent.setComponent(service);
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.widget_play_btn, pendingIntent);

        }

        intent = new Intent(context, DetailActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(intent);
        pendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_layout_root, pendingIntent);

        intent = new Intent(ACTION_NEXT);
        intent.setComponent(service);
          pendingIntent = PendingIntent.getService(context, 0 , intent, 0 );
        views.setOnClickPendingIntent(R.id.widget_next_btn, pendingIntent);

        intent = new Intent(ACTION_PREVIOUS);
        intent.setComponent(service);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.widget_previous_btn, pendingIntent);

    }

    public   Bitmap decodeSampledBitmapFromResource(
            String pathName) {
        int reqWidth,reqHeight;
       // reqWidth = ;
       // reqWidth = (reqWidth/5)*2;
        //reqHeight = reqWidth;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
//  BitmapFactory.decodeStream(is, null, options);
        BitmapFactory.decodeFile(pathName, options);
// Calculate inSampleSize
      //  options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
// Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public   int calculateInSampleSize(BitmapFactory.Options options,
                                       int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
}

