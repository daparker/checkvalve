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

import java.util.concurrent.TimeoutException;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.github.koraktor.steamcondenser.exceptions.RCONBanException;
import com.github.koraktor.steamcondenser.exceptions.RCONNoAuthException;
import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;

public class RconAuth implements Runnable {
    private Handler handler;
    private String password;
    private SourceServer ssrv;
    private GoldSrcServer gsrv;
    private Object obj;

    private static final String TAG = RconAuth.class.getSimpleName();

    /**
     * Class for authenticating RCON.
     * 
     * @param p The RCON password to use
     * @param s The SourceServer on which to execute the command
     * @param h The Handler to use
     */
    public RconAuth( String p, SourceServer s, Handler h ) {
        this.password = p;
        this.ssrv = s;
        this.handler = h;
    }

    /**
     * Class for authenticating RCON.
     * 
     * @param p The RCON password to use
     * @param g The GoldSrcServer on which to execute the command
     * @param h The Handler to use
     */
    public RconAuth( String p, GoldSrcServer g, Handler h ) {
        this.password = p;
        this.gsrv = g;
        this.handler = h;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Message msg = new Message();
        int status = 0;

        status = rconAuthenticate();

        msg.what = status;

        if( obj != null )
            msg.obj = obj;

        this.handler.sendMessage(msg);
    }

    public int rconAuthenticate() {
        try {
            if( gsrv != null ) {
                gsrv.rconAuth(password);
                gsrv.rconExec("status");
                obj = gsrv;
            }
            else {
                ssrv.rconAuth(password);
                obj = ssrv;
            }

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
            Log.w(TAG, "rconAuthenticate(): Caught exception: " + e.toString());
            Log.w(TAG, "Stack trace:");

            StackTraceElement[] ste = e.getStackTrace();

            for( StackTraceElement x : ste )
                Log.w(TAG, "    " + x.toString());

            obj = e;

            return 5;
        }
    }
}
