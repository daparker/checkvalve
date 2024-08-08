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

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.koraktor.steamcondenser.servers.GameServer;

public class RconQuery implements Runnable {
    private static final String TAG = RconQuery.class.getSimpleName();

    private final Handler handler;
    private final String command;
    private final GameServer srv;
    private String response;
    private Object obj;

    /**
     * Class for executing commands via RCON.
     *
     * @param c The command to execute
     * @param s The GameServer on which to execute the command
     * @param h The Handler to use
     */
    public RconQuery(String c, GameServer s, Handler h) {
        this.command = c;
        this.srv = s;
        this.handler = h;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Message msg = new Message();
        msg.what = getRconResponse();

        if( obj != null )
            msg.obj = obj;
        else
            msg.obj = response;

        this.handler.sendMessage(msg);
    }

    public int getRconResponse() {
        try {
            response = srv.rconExec(command);
            return 0;
        }
        catch( Exception e ) {
            Log.w(TAG, "Caught an exception:", e);
            obj = e;
            return 1;
        }
    }
}
