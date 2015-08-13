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

package com.github.daparker.checkvalve;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;

public class ServerCheck implements Runnable {
    private Handler handler;
    private String server;
    private int port;
    private int timeout;

    private static final String TAG = ServerCheck.class.getSimpleName();

    /**
     * Construct a new instance of the ServerCheck class.
     * <p>
     * Calling <tt>start()</tt> on this instance will cause it to connect to the
     * specified host and port, and perform an A2S_INFO query to confirm it is
     * an HLDS/SRCDS listen server.
     * </p>
     * <p>
     * One of the following will be sent to the handler as the <tt>what<tt> value:
     * <ul>
     * <b>0</b> (The query was successful)<br />
     * <b>1</b> (Encountered UnknownHostException)<br />
     * <b>2</b> (Encountered SocketException)<br />
     * <b>3</b> (Encountered UnsupportedEncodingException)<br />
     * <b>4</b> (Encountered IOException)<br />
     * <b>5</b> (Encountered some other exception)<br />
     * </ul>
     * </p>
     * @param server The URL or IP address of the server to be queried
     * @param port The port of the server to be queried
     * @param timeout The connection timeout for the server query
     * @param handler The Handler to which the status code is returned.
     */
    public ServerCheck( String server, int port, int timeout, Handler handler ) {
        this.server = server;
        this.port = port;
        this.timeout = timeout;
        this.handler = handler;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        int status = 0;

        // A2S_INFO query string
        String queryString = "\u00FF\u00FF\u00FF\u00FF\u0054Source Engine Query\0";

        try {
            // Create a UDP socket
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeout * 1000);

            // Byte buffers for packet data
            byte[] bufferOut = queryString.getBytes("ISO8859_1");
            byte[] bufferIn = new byte[1400];

            // UDP datagram packets
            DatagramPacket packetOut = new DatagramPacket(bufferOut, bufferOut.length);
            DatagramPacket packetIn = new DatagramPacket(bufferIn, bufferIn.length);

            // Connect to the remote server
            socket.connect(InetAddress.getByName(server), port);

            // Send the query string to the server
            socket.send(packetOut);

            // Receive the response packet from the server
            socket.receive(packetIn);

            // Close the UDP socket
            socket.close();
        }
        catch( UnknownHostException e ) {
            status = 1;
        }
        catch( SocketException e ) {
            status = 2;
        }
        catch( UnsupportedEncodingException e ) {
            status = 3;
        }
        catch( IOException e ) {
            status = 4;
        }
        catch( Exception e ) {
            Log.w(TAG, "Caught an exception:", e);
            status = 5;
        }

        handler.sendEmptyMessage(status);
    }
}