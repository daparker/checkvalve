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

import java.util.concurrent.TimeoutException;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.github.koraktor.steamcondenser.exceptions.RCONBanException;
import com.github.koraktor.steamcondenser.exceptions.RCONNoAuthException;
import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.servers.GameServer;

public class RconAuth implements Runnable {
    private static final String TAG = RconAuth.class.getSimpleName();

    private Handler handler;
    private String password;
    private GameServer srv;
    private Object obj;

    /**
     * Class for authenticating RCON.
     * 
     * @param p The RCON password to use
     * @param g The GameServer on which to execute the command
     * @param h The Handler to use
     */
    public RconAuth( String p, GameServer s, Handler h ) {
        this.password = p;
        this.srv = s;
        this.handler = h;
    }
    
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Message msg = new Message();
        int status = rconAuthenticate();

        msg.what = status;

        if( obj != null )
            msg.obj = obj;

        this.handler.sendMessage(msg);
    }

    public int rconAuthenticate() {
        try {
            srv.rconAuth(password);
            srv.rconExec("status");
            obj = srv;
            return 0;
        }
        catch( RCONNoAuthException e ) {
            return 1;
        }
        catch( RCONBanException e ) {
            return 2;
        }
        catch( SteamCondenserException e ) {
            return 3;
        }
        catch( TimeoutException e ) {
            return 4;
        }
        catch( Exception e ) {
            Log.w(TAG, "rconAuthenticate(): Caught exception:", e);
            obj = e;
            return 5;
        }
    }
}
