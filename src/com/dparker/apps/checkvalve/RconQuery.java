/*
 * Copyright 2010-2015 by David A. Parker <parker.david.a@gmail.com>
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

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.github.koraktor.steamcondenser.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;

public class RconQuery implements Runnable {
    private Handler handler;
    private String response;
    private String command;
    private int status;
    private SourceServer ssrv;
    private GoldSrcServer gsrv;
    private Object obj;

    private static final String TAG = RconQuery.class.getSimpleName();

    /**
     * Class for executing commands via RCON.
     * 
     * @param c The command to execute
     * @param s The SourceServer on which to execute the command
     * @param h The Handler to use
     */
    public RconQuery( String c, SourceServer s, Handler h ) {
        this.command = c;
        this.ssrv = s;
        this.handler = h;
    }

    /**
     * Class for executing commands via RCON.
     * 
     * @param c The command to execute
     * @param g The GoldSrcServer on which to execute the command
     * @param h The Handler to use
     */
    public RconQuery( String c, GoldSrcServer g, Handler h ) {
        this.command = c;
        this.gsrv = g;
        this.handler = h;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Message msg = new Message();
        status = 0;

        getRconResponse();

        msg.what = status;

        if( obj != null )
            msg.obj = obj;
        else
            msg.obj = response;

        this.handler.sendMessage(msg);
    }

    public void getRconResponse() {
        try {
            if( ssrv != null )
                response = ssrv.rconExec(command);
            else
                response = gsrv.rconExec(command);
        }
        catch( Exception e ) {
            Log.w(TAG, "Caught exception: " + e.toString());
            status = 1;
            obj = e;
        }
    }
}
