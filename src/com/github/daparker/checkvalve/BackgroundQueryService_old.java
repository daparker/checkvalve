/*
 * Copyright 2010-2017 by David A. Parker <parker.david.a@gmail.com>
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

package com.github.daparker.checkvalve;

import java.util.ArrayList;
import com.github.daparker.checkvalve.exceptions.InvalidDataTypeException;
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

@SuppressLint("NewApi")
public class BackgroundQueryService_old extends Service {    
    private static final String TAG = BackgroundQueryService_old.class.getSimpleName();
    private static final Handler resultHandler = new staticHandler();

    private static boolean retry;
    private static boolean running;
    private static boolean querying;
    private static boolean useSound;
    private static boolean useLED;
    private static boolean useVibrate;
    private static long interval;
    private static Context context;
    private static Thread t;
    
    private static class staticHandler extends Handler {
        @SuppressWarnings({ "unchecked", "deprecation" })
        public void handleMessage( Message msg ) {            
            querying = false;
            
            Log.d(TAG, "Background query thread returned " + msg.what);
            
            // A negative "what" code indicates the server query thread failed
            if( msg.what < 0 ) {
                return;
            }

            getSettings();

            ArrayList<String> messages = (ArrayList<String>)msg.obj;
            String messageText = new String();
            Notification n = null;
            
            int id = 0;
            int defaults = 0;
            
            if( ! messages.isEmpty() ) {
                // If this was the first try then try again
                if( retry == false ) {
                    retry = true;
                    querying = true;
                    new Thread(new BackgroundServerQuery(context, resultHandler)).start();
                    return;
                }
                // If this was a retry then notify the user
                else {
                    retry = false;
                    id = 1;
                
                    if( messages.size() == 1 ) {
                        messageText = context.getString(R.string.notification_single_server_down);
                    }
                    else {                        
                        messageText = String.format(
                                context.getString(R.string.notification_multiple_servers_down),
                                Integer.valueOf(messages.size()).toString());
                    }
                    
                    Intent intent = new Intent(context, com.github.daparker.checkvalve.CheckValve.class);
                    PendingIntent pending = PendingIntent.getActivity(context, (int)System.currentTimeMillis(), intent, 0);
                 
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
                    
                    if( android.os.Build.VERSION.SDK_INT < 16 ) {
                        n = nb.getNotification();
                    }
                    else {
                        n = nb.build();
                    }
                    
                    handleNotification(context, n, id);
                }
            }
            else {
                n = null;
                id = 0;
                handleNotification(context, n, id);
            }                    
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        context = BackgroundQueryService_old.this;
        Log.d(TAG, "Created CheckValve background query service.");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
                
        if( ! running ) {
            getSettings();
            retry = false;
            running = true;
            
            t = new Thread() {
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    
                    Thread q = new Thread();
                    
                    try {
                        for(;;) {
                            if( ! querying ) {
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
                                Log.d(TAG, "Background query is still running on thread " + q.toString());
                            }
                            
                            Log.d(TAG, "Sleeping for " + interval + " ms");
                            Thread.sleep(interval);
                        }
                    }
                    catch( InterruptedException ie ) {
                        Log.d(TAG, "Background query thread was interrupted.");
                        return;
                    }
                    catch( Exception e ) {
                        Log.e(TAG, "Caught an exception:", e);
                        return;
                    }
                }
            };

            t.start();
            Log.d(TAG, "Started CheckValve background query service.");
        }

        return Service.START_NOT_STICKY;
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

        t.interrupt();
                
        resultHandler.removeCallbacksAndMessages(null);
        running = false;
        
        Log.d(TAG, "Stopped CheckValve background query service.");
    }
    
    private static void handleNotification(Context c, Notification n, int id) {
        String tag = "CheckValve";
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

        if( id == 0 ) {
            Log.d(TAG, "All servers are up.");
            nm.cancel(tag, 1);
        }
        else {
            Log.d(TAG, "Showing notification [tag=" + tag + "][id=" + id + "]");
            nm.notify(tag, id, n);
        }
    }
    
    private static void getSettings() {
        DatabaseProvider db = new DatabaseProvider(context);
        
        try {
            int freq = db.getIntSetting(DatabaseProvider.SETTINGS_BACKGROUND_QUERY_FREQUENCY);
            interval = (freq * 60000);
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
        
        ConnectivityManager c = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
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