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

import android.util.Log;

import com.github.koraktor.steamcondenser.servers.GameServer;
import com.github.koraktor.steamcondenser.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;

import java.net.InetAddress;

/**
 * This class implements the getServer() method which acts as a factory for
 * GameServer objects.
 *
 * @author David A. Parker
 */
public class Server {
    private static final String TAG = Server.class.getSimpleName();

    private Server() {
    } // Do not allow instances

    /**
     * Get a GameServer object of the appropriate type (SourceServer or GoldSrcServer)
     * based on the engine type.
     *
     * @param engineType Integer constant representing the engine type
     * @param server     The address of the game server
     * @param port       The listen port of the game server
     * @return A SourceServer if the engine is Source, GoldSrcServer if the engine is
     * GoldSrc, or <tt>null</tt> if an error occurred
     */
    public static GameServer getServer(int engineType, InetAddress server, int port) {
        try {
            if( engineType == Values.ENGINE_SOURCE ) {
                return new SourceServer(server, port);
            }
            else if( engineType == Values.ENGINE_GOLDSRC ) {
                return new GoldSrcServer(server, port);
            }
            else {
                Log.w(TAG, "getServer(): Unknown egnine type: " + engineType);
                return null;
            }
        }
        catch( Exception e ) {
            Log.w(TAG, "getServer(): Caught an exception:", e);
            return null;
        }
    }
}