/*
 * Copyright 2010-2024 by David A. Parker <parker.david.a@gmail.com>
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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.os.Handler;

import com.dparker.apps.checkvalve.exceptions.InvalidDataTypeException;

import java.util.ArrayList;

@TargetApi(21)
public class BackgroundJobService extends JobService {
    private static final String TAG = BackgroundJobService.class.getSimpleName();
    private static final staticHandler resultHandler = new staticHandler();

    private static boolean retry;
    private static boolean querying;
    private static boolean useSound;
    private static boolean useLED;
    private static boolean useVibrate;
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

            Log.d(TAG, "handleMessage(): Background query thread returned " + msg.what);

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
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "onStartJob(): Starting background job.");

        getSettings();
        retry = false;

        t = new Thread() {
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                try {
                    resultHandler.post(r);
                }
                catch( Exception e ) {
                    Log.e(TAG, "onStartJob(): Caught an exception:", e);
                }
            }
        };

        t.start();

        BackgroundJobUtil.scheduleJob(getApplicationContext(), false);
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = BackgroundJobService.this;
        Log.d(TAG, "onCreate(): Created background query job.");
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

        Log.d(TAG, "onDestroy(): Stopping background query job.");

        try {
            t.interrupt();
        }
        catch( Exception e ) {
            Log.e(TAG, "onDestroy(): Caught an exception:", e);
        }
    }

    private static void handleNotification(int numServersDown) {
        String tag = "CheckValve";
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if( numServersDown == 0 ) {
            Log.d(TAG, "handleNotification(): All servers are up.");
            nm.cancel(tag, 1);
        }
        else {
            /*
             * Show a notification of how many servers did not respond
             */

            getSettings();

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

            int defaults = 0;
            Notification n;
            Notification.Builder nb;

            if( Build.VERSION.SDK_INT >= 26 ) {
                // Use a Notification Channel on Android Oreo and above
                NotificationChannel nc = new NotificationChannel(
                        Values.CHANNEL_ID,
                        Values.CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);

                nc.setDescription(Values.CHANNEL_DESCRIPTION);
                nm.createNotificationChannel(nc);

                Log.d(TAG, "handleNotification(): Created notification channel " + Values.CHANNEL_ID);

                nb = new Notification.Builder(context, Values.CHANNEL_ID);
            }
            else {
                nb = new Notification.Builder(context);
            }

            // Set up the notification
            nb.setSmallIcon(R.drawable.checkvalve_statusbar)
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

            // Show the notification
            Log.d(TAG, "handleNotification(): Showing notification.");
            nm.notify(tag, 1, n);
        }
    }

    private static void getSettings() {
        DatabaseProvider db = new DatabaseProvider(context);

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

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
