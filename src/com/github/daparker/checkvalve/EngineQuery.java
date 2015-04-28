/*
 * Copyright 2010-2011 by David A. Parker <parker.david.a@gmail.com>
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
import android.os.Message;
import android.util.Log;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.github.koraktor.steamcondenser.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;

public class EngineQuery implements Runnable
{
    private Handler handler;
    private String server;
    private int port;
    private int timeout;
    private SourceServer ssrv;
    private GoldSrcServer gsrv;
    private Object obj;

    private static final String TAG = EngineQuery.class.getSimpleName();

    /**
     * Construct a new instance of the EngineQuery class to determine a server's engine.
     * 
     * @param s The IP address or URL of the server
     * @param p The listen port of the server
     * @param t The timeout for the server query (in milliseconds)
     * @param h The handler to use
     */
    public EngineQuery( String s, int p, int t, Handler h )
    {
        this.server = s;
        this.port = p;
        this.timeout = t;
        this.handler = h;
    }

    public void run()
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        
        int result = 0;
        Message msg = new Message();

        result = getServerEngine(server, port, timeout);
        
        if( obj != null )
            msg.obj = obj;

        msg.what = result;
        
        handler.sendMessage(msg);
    }

    public int getServerEngine(String s, int p, int t)
    {
        int appId = 0;
        int byteNum = 0;
        int firstByte = 0;
        int secondByte = 0;

        try
        {
            int result = 0;
            
            DatagramSocket socket = new DatagramSocket();

            // A2S_INFO query string
            String queryString = "\u00FF\u00FF\u00FF\u00FF\u0054Source Engine Query\0";

            socket.setSoTimeout(t * 1000);

            // Byte buffers for packet data
            byte[] bufferOut = queryString.getBytes("ISO8859_1");
            byte[] bufferIn = new byte[2048];

            // UDP datagram packets
            DatagramPacket packetOut = new DatagramPacket(bufferOut, bufferOut.length);
            DatagramPacket packetIn = new DatagramPacket(bufferIn, bufferIn.length);

            // Connect to the remote server
            socket.connect(InetAddress.getByName(s), p);

            // Show an error if the connection attempt failed
            if( ! socket.isConnected() )
            {
                socket.close();
                return -1;
            }

            // Send the query string to the server
            socket.send(packetOut);

            // Receive the response packet from the server
            socket.receive(packetIn);

            // Start at byte 6 in the response data
            byteNum = 6;

            // Skip all data before the app ID
            while( bufferIn[byteNum] != 0x00 )
                byteNum++;

            byteNum++;

            while( bufferIn[byteNum] != 0x00 )
                byteNum++;

            byteNum++;

            while( bufferIn[byteNum] != 0x00 )
                byteNum++;

            byteNum++;

            while( bufferIn[byteNum] != 0x00 )
                byteNum++;

            byteNum++;

            // Get the old Steam application ID
            firstByte = (int)(bufferIn[byteNum] & 0xff);
            secondByte = (int)(bufferIn[byteNum + 1] & 0xff);
            appId = firstByte | (secondByte << 8);

            // If the app ID is less than 200 then the engine is assumed to be GoldSource,
            // but we'll check for the 64-bit game ID later and use that if possible since
            // it's more accurate
            result = (appId < 200)?Values.ENGINE_GOLDSRC:Values.ENGINE_SOURCE;

            // Skip the next 9 bytes
            byteNum += 9;

            // Skip the game version string
            while( bufferIn[byteNum] != 0x00 )
                byteNum++;

            byteNum++;

            // If we're not at the end of the array then get the additional data
            if( byteNum < bufferIn.length )
            {
                // This byte is the Extra Data Flag (EDF)
                int EDF = (int)bufferIn[byteNum];
                byteNum++;

                // Skip the port number if included
                if( (EDF & 0x80) > 0 ) byteNum += 2;

                // Skip the SteamID if included
                if( (EDF & 0x10) > 0 ) byteNum += 8;

                // Skip SourceTV information if included
                if( (EDF & 0x40) > 0 )
                {
                    byteNum += 2;

                    while( bufferIn[byteNum] != 0x00 )
                        byteNum++;

                    byteNum++;
                }

                // Skip the server tags (sv_tags) if included
                if( (EDF & 0x20) > 0 )
                {
                    while( bufferIn[byteNum] != 0 )
                        byteNum++;

                    byteNum++;
                }

                // Get the 64-bit game ID if it's included
                if( (EDF & 0x01) > 0 )
                {
                    // Get the app ID from the lowest 24 bits of the game ID
                    appId = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put(bufferIn, byteNum, 3).put((byte)0x00).getInt(0);

                    // Use this app ID to determine the engine
                    result = (appId < 200)?Values.ENGINE_GOLDSRC:Values.ENGINE_SOURCE;
                }
            }

            socket.close();
            
            if( result == Values.ENGINE_GOLDSRC )
            {
                gsrv = new GoldSrcServer(InetAddress.getByName(server), port);
                obj = gsrv;
            }
            else
            {
                ssrv = new SourceServer(InetAddress.getByName(server), port);
                obj = ssrv;
            }
            
            return result;
        }
        // Handle a socket timeout
        catch( Exception e )
        {
            Log.w(TAG, "getServerEngine(): Caught an exception: " + e.toString());
            Log.w(TAG, "Stack trace:");
            
            StackTraceElement[] ste = e.getStackTrace();
            
            for( StackTraceElement x : ste )
                Log.w(TAG, "    " + x.toString());
            
            return -1;
        }
    }
}
