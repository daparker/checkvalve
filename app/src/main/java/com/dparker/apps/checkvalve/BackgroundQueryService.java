/*
 * Copyright 2010-2019 by David A. Parker <parker.david.a@gmail.com>
 *
 * This file is part of CheckValve, an HLDS/SRCDS query app for Android.
 *
 * CheckValve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * CheckValve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the CheckValve source code.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.dparker.apps.checkvalve;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.dparker.apps.checkvalve.exceptions.InvalidDataTypeException;

import java.util.ArrayList;

@SuppressLint("NewApi")
public class BackgroundQueryService extends Service {
    private static final String TAG = BackgroundQueryService.class.getSimpleName();
    private static final staticHandler resultHandler = new staticHandler();

    private static boolean retry;
    private static boolean running;
    private static boolean querying;
    private static boolean useSound;
    private static boolean useLED;
    private static boolean useVibrate;
    private static long interval;
    private static Context context;
    private static Thread t;

    private static final Runnable r = new Runnable() {
        public void run() {
            Thread q = new Thread();

            if( !querying ) {
                if( networkIsConnected() ) {
                    querying = true;

                    // Run the server queries in a separate thread
                    Log.d(TAG, "Running background query.");
                    q = new Thread(new BackgroundServerQuery(context, resultHandler));
                    q.start();
                }
                else {
                    Log.w(TAG, "Cannot query servers: no network connection.");
                }
            }
            else {
                Log.w(TAG, "Background query is still running on thread " + q);
            }
        }
    };

    private static class staticHandler extends Handler {
        @SuppressWarnings({"unchecked"})
        public void handleMessage(Message msg) {
            querying = false;

            Log.d(TAG, "Background query thread returned " + msg.what);

            // A negative "what" code indicates the server query thread failed
            if( msg.what >= 0 ) {
                ArrayList<String> messages = (ArrayList<String>) msg.obj;

                if( !messages.isEmpty() ) {
                    // If this was the first try then try again
                    if( ! retry ) {
                        retry = true;
                        querying = true;
                        new Thread(new BackgroundServerQuery(context, resultHandler)).start();
                        return;
                    }
                    else {
                        retry = false;
                    }
                }

                handleNotification(messages.size());
            }

            Log.d(TAG, "Sleeping for " + interval + " ms");
            this.postDelayed(r, interval);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = BackgroundQueryService.this;
        Log.d(TAG, "Created CheckValve background query service.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if( !running ) {
            getSettings();
            retry = false;
            running = true;

            t = new Thread() {
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                    try {
                        resultHandler.post(r);
                    }
                    catch( Exception e ) {
                        Log.e(TAG, "Caught an exception:", e);
                    }
                }
            };

            t.start();
            Log.d(TAG, "Started CheckValve background query service.");
        }

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        resultHandler.removeCallbacksAndMessages(null);

        Log.d(TAG, "Stopping CheckValve background query service.");

        try {
            t.interrupt();
            Log.d(TAG, "Stopped CheckValve background query service.");
        }
        catch( Exception e ) {
            Log.e(TAG, "Caught an exception:", e);
        }

        running = false;
    }


    private static void handleNotification(int numServersDown) {
        String tag = "CheckValve";
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if( numServersDown == 0 ) {
            Log.d(TAG, "All servers are up.");
            nm.cancel(tag, 1);
        }
        else {
            String messageText;

            if( numServersDown == 1 ) {
                messageText = context.getString(R.string.notification_single_server_down);
            }
            else {
                messageText = String.format(
                        context.getString(R.string.notification_multiple_servers_down),
                        Integer.valueOf(numServersDown).toString());
            }

            Intent intent = new Intent(context, com.dparker.apps.checkvalve.CheckValve.class);
            intent.putExtra(Values.EXTRA_QUERY_SERVERS, true);

            PendingIntent pending = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);

            Notification n;
            int defaults = 0;

            getSettings();

            // Show a notification of how many servers did not respond
            Notification.Builder nb = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.checkvalve_statusbar)
                    .setContentIntent(pending)
                    .setContentText(messageText)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setTicker(context.getString(R.string.notification_ticker_text))
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true);

            if( useLED ) {
                defaults |= Notification.DEFAULT_LIGHTS;
            }

            if( useSound ) {
                defaults |= Notification.DEFAULT_SOUND;
            }

            if( useVibrate ) {
                defaults |= Notification.DEFAULT_VIBRATE;
            }

            nb.setDefaults(defaults);
            n = nb.build();

            Log.d(TAG, "Showing notification.");
            nm.notify(tag, 1, n);
        }
    }

    private static void getSettings() {
        DatabaseProvider db = new DatabaseProvider(context);

        try {
            int freq = db.getIntSetting(DatabaseProvider.SETTINGS_BACKGROUND_QUERY_FREQUENCY);
            interval = (freq * 60000L);
        }
        catch( InvalidDataTypeException e ) {
            interval = 300000L;
        }

        try {
            useLED = db.getBooleanSetting(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATION_LED);
        }
        catch( InvalidDataTypeException e ) {
            useLED = true;
        }

        try {
            useSound = db.getBooleanSetting(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATION_SOUND);
        }
        catch( InvalidDataTypeException e ) {
            useSound = true;
        }

        try {
            useVibrate = db.getBooleanSetting(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATION_VIBRATE);
        }
        catch( InvalidDataTypeException e ) {
            useVibrate = true;
        }

        db.close();
    }

    private static boolean networkIsConnected() {
        boolean result = false;

        ConnectivityManager c = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo n = c.getActiveNetworkInfo();

        if( n != null ) {
            result = n.isConnected();
        }

        return result;
    }

    public static boolean isRunning() {
        return running;
    }
}