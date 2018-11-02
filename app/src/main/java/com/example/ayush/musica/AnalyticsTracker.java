package com.example.ayush.musica;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

public class AnalyticsTracker extends Application {
    private static GoogleAnalytics sAnalytics;
    private static Tracker sTracker;
    private static AnalyticsTracker sInstance;

    public static synchronized AnalyticsTracker getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        sAnalytics = GoogleAnalytics.getInstance(this);
    }

    synchronized public Tracker getDefaultTracker() {
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        if (sTracker == null) {
            sTracker = sAnalytics.newTracker(R.xml.global_tracker);
        }
        return sTracker;
    }

    public void trackScreenView(String screenName) {
        Tracker t = getDefaultTracker();
        t.setScreenName(screenName);
        t.send(new HitBuilders.ScreenViewBuilder().build());
        GoogleAnalytics.getInstance(this).dispatchLocalHits();
    }

    public void trackException(Exception e) {
        if (e != null) {
            Tracker t = getDefaultTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription(
                            new StandardExceptionParser(this, null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build()
            );
        }

    }

    public void trackEvent(String category, String action, String label) {
        Tracker t = getDefaultTracker();
        t.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).build());
    }

}
