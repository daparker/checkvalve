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

package com.github.daparker.checkvalve;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

/**
 * This class implements a <tt>BroadcastReceiver</tt> listening for network events, and sends messages to the caller via
 * a <tt>Handler</tt> when events occur.
 */
public class NetworkEventReceiver implements Runnable {
    private static final String TAG = "NetworkEventReceiver";

    private boolean registered;
    private boolean connected;
    private int lastNetworkType;
    private BroadcastReceiver receiver;
    private IntentFilter filter;
    private Context context;
    private Handler handler;
    private int event;

    /**
     * Construct a new instance of the NetworkEventReceiver class.
     * 
     * This class implements a <tt>BroadcastReceiver</tt> listening for network events, and sends messages to the caller
     * via a <tt>Handler</tt> when events occur.
     * 
     * @param c The context to use
     * @param h The handler to use
     */
    public NetworkEventReceiver( Context c, Handler h ) {
        this.context = c;
        this.handler = h;
        this.event = 0;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        registered = false;
        connected = false;
        lastNetworkType = 0;
        filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        Log.i(TAG, "Starting network event receiver.");

        try {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive( Context x, Intent i ) {
                    Log.i(TAG, "A network event has been received (event #" + event + ").");

                    // Determine whether connectivity has been completely lost
                    if( i.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) ) {
                        Log.w(TAG, "Network connectivity has been lost.");
                        handler.sendEmptyMessage(-1);
                        connected = false;
                    }
                    else {
                        ConnectivityManager c = (ConnectivityManager)x.getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo n = c.getActiveNetworkInfo();

                        if( n == null ) {
                            Log.d(TAG, "ConnectivityManager.getActiveNetworkInfo() is null.");
                            Log.d(TAG, "No active network connections exist.");
                        }
                        else {
                            String state = "";

                            switch( n.getState() ) {
                                case CONNECTING:
                                    state = "Connecting";
                                    connected = false;
                                    break;
                                case CONNECTED:
                                    state = "Connected";
                                    connected = true;
                                    break;
                                case DISCONNECTING:
                                    state = "Disconnecting";
                                    connected = false;
                                    break;
                                case DISCONNECTED:
                                    state = "Disconnected";
                                    connected = false;
                                    break;
                                case SUSPENDED:
                                    state = "Suspended";
                                    connected = false;
                                    break;
                                case UNKNOWN:
                                    state = "Unknown";
                                    connected = false;
                                    break;
                                default:
                                    state = "Other";
                                    connected = false;
                                    break;
                            }

                            int type = n.getType();
                            String typeName = n.getTypeName();
                            String available = (n.isAvailable())?"true":"false";

                            Log.i(TAG, "[receiver=" + receiver.hashCode() + "][event=" + event + "] TYPE: " + typeName + " (" + type + ")");
                            Log.i(TAG, "[receiver=" + receiver.hashCode() + "][event=" + event + "] STATE: " + state);
                            Log.i(TAG, "[receiver=" + receiver.hashCode() + "][event=" + event + "] AVAILABLE: " + available);

                            // The first event (0) will always be received just after the receiver
                            // is registered, so we'll ignore it and only notify the parent thread
                            // about events thereafter.
                            if( event == 0 ) {
                                lastNetworkType = type;
                            }
                            else {
                                if( connected ) {
                                    if( type != lastNetworkType ) {
                                        lastNetworkType = type;
                                        handler.sendEmptyMessage(1);
                                    }
                                    else {
                                        Log.d(TAG, "Ignoring event #" + event + " (duplicate)");
                                    }
                                }
                            }
                        }
                    }

                    event++;
                }
            };

            registerReceiver();
        }
        catch( Exception e ) {
            Log.w(TAG, "run(): Caught an exception:", e);
            unregisterReceiver();
            handler.sendEmptyMessage(-2);
            Log.i(TAG, "Shutting down network event receiver thread.");
            return;
        }
    }

    /**
     * Registers the BroadcastReceiver if it is not already registered.
     */
    public void registerReceiver() {
        if( ! registered ) {
            try {
                Log.i(TAG, "Registering broadcast receiver.");
                context.registerReceiver(receiver, filter);

                Log.i(TAG, "Resetting event counter.");
                event = 0;

                registered = true;
            }
            catch( Exception e ) {
                Log.w(TAG, "registerReceiver(): Caught an exception:", e);
                Log.w(TAG, "Failed to register broadcast receiver.");
            }
        }
    }

    /**
     * Unregisters the BroadcastReceiver if it is currently registered.
     */
    public void unregisterReceiver() {
        if( registered ) {
            try {
                Log.i(TAG, "Unregistering broadcast receiver.");
                context.unregisterReceiver(receiver);
                registered = false;
            }
            catch( Exception e ) {
                Log.w(TAG, "unregisterReceiver(): Caught an exception:", e);
                Log.w(TAG, "Failed to unregister broadcast receiver.");
            }
        }
    }

    /**
     * Determines if the BroadcastReceiver is currently registered.
     * 
     * @return A boolean value indicating whether or not the receiver is registered.
     */
    public boolean isRegistered() {
        return this.registered;
    }

    /**
     * Gets a copy of the receiver.
     * 
     * @return The receiver as a BroadcastReceiver object
     */
    public BroadcastReceiver getReceiver() {
        return this.receiver;
    }

    /**
     * Shuts down the NetworkEventReceiver. This method simply calls the <tt>interrupt()<tt>
     * method on the NetworkEventReceiver object's thread.
     */
    public void shutDown() {
        Log.i(TAG, "Shutdown was requested; calling interrupt() on this thread.");
        Thread.currentThread().interrupt();
    }
}