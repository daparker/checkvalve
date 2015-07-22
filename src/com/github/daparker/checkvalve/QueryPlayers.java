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
import android.os.Message;
import android.util.Log;
import android.content.Context;
import java.lang.Math;
import java.net.*;
import java.util.ArrayList;

public class QueryPlayers implements Runnable {
    private static final String TAG = QueryPlayers.class.getSimpleName();

    private int status;
    private long rowId;
    private byte[] challengeResponse;
    private Context context;
    private Handler handler;

    public QueryPlayers( Context context, long rowId, byte[] challengeResponse, Handler handler ) {
        this.context = context;
        this.rowId = rowId;
        this.challengeResponse = challengeResponse;
        this.handler = handler;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        status = 0;
        Message msg = new Message();
        ArrayList<PlayerRecord> players = queryPlayers();

        if( players != null )
            msg.obj = players;

        msg.what = status;
        handler.sendMessage(msg);
    }

    public ArrayList<PlayerRecord> queryPlayers() {
        DatabaseProvider database = new DatabaseProvider(this.context);
        ServerRecord sr = database.getServer(rowId);
        database.close();

        DatagramSocket socket;
        DatagramPacket packetOut;
        DatagramPacket packetIn;

        // Player array to be returned
        ArrayList<PlayerRecord> playerList = null;

        // Byte buffers for packet data
        byte[] bufferOut;
        byte[] bufferIn;
        byte[] tempBuffer;

        // String variables
        String serverURL = new String();

        // Integer variables
        int index = 0;
        int byteNum = 0;
        int serverPort = 0;
        int serverTimeout = 0;

        serverURL = sr.getServerName();
        serverPort = sr.getServerPort();
        serverTimeout = sr.getServerTimeout();

        String header = new String();
        String name = new String();
        String totaltime = new String();

        short numplayers = 0;
        short numpackets = 0;
        short thispacket = 0;
        short hours = 0;
        short minutes = 0;
        short seconds = 0;

        long kills = 0;
        float time = 0;

        // Integer variables
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(serverTimeout * 1000);

            // Challenge response becomes the A2S_PLAYER query by changing 0x41 to 0x55
            bufferOut = challengeResponse;
            bufferOut[4] = Values.BYTE_A2S_PLAYER;

            tempBuffer = new byte[1400];

            // UDP datagram packets
            packetOut = new DatagramPacket(bufferOut, bufferOut.length);
            packetIn = new DatagramPacket(tempBuffer, tempBuffer.length);

            // Connect to the remote server
            socket.connect(InetAddress.getByName(serverURL), serverPort);

            // Show an error if the connection attempt failed
            if( !socket.isConnected() ) {
                if( !socket.isClosed() )
                    socket.close();
                
                throw new SocketException();
            }

            // Send the A2S_PLAYER query string and get the response packet
            socket.send(packetOut);
            socket.receive(packetIn);

            bufferIn = new byte[packetIn.getLength()];

            for( int x = 0; x < bufferIn.length; x++ )
                bufferIn[x] = tempBuffer[x];

            String[] packets = new String[100];

            // Store the response data in a string
            String response = new String(bufferIn, "ISO8859_1");

            // Get the header info to see if data has been split over multiple packets
            header = response.substring(0, 4);

            numpackets = 1;

            // If the first 4 header bytes are 0xFFFFFFFE then there are multiple packets
            if( header.equals("\u00FF\u00FF\u00FF\u00FE") ) {
                /*
                 * If there are multiple packets, each packet will have 12 header bytes, but the "first" packet (packet
                 * 0) will have an additional 6 header bytes. UDP packets can arrive in any order, so we need to check
                 * the sequence number of each packet to know how many header bytes to strip.
                 */

                // Get rid of 12 header bytes
                byteNum = 8;
                numpackets = (short)bufferIn[byteNum++];
                thispacket = (short)bufferIn[byteNum++];
                byteNum++;

                // If this is packet 0 then skip the next 5 header bytes
                if( thispacket == 0 ) {
                    byteNum += 6;
                    numplayers = (short)bufferIn[byteNum++];
                }

                packets[thispacket] = response.substring(byteNum, response.length());

                for( int i = 1; i < numpackets; i++ ) {
                    // Receive the response packet from the server
                    socket.receive(packetIn);

                    // Store the response data in a string
                    response = new String(bufferIn, "ISO8859_1");

                    // Get rid of 12 header bytes
                    byteNum = 9;
                    thispacket = (short)bufferIn[byteNum];
                    byteNum += 2;

                    // If this is packet 0 then skip the next 6 header bytes
                    if( thispacket == 0 ) {
                        byteNum += 6;
                        numplayers = (short)bufferIn[byteNum++];
                    }

                    packets[thispacket] = response.substring(byteNum, response.length());
                }
            }
            else {
                // Get number of players (6th byte)
                numplayers = (short)bufferIn[5];
                packets[0] = response.substring(6, response.length());
            }

            socket.close();

            if( numplayers == 0 ) {
                status = -2;
                return null;
            }

            // Initialize the return array once we know how many elements we need
            playerList = new ArrayList<PlayerRecord>();

            for( int i = 0; i < numpackets; i++ ) {
                String thisPacket = packets[i];

                byte[] bytes = thisPacket.getBytes("ISO8859_1");

                byteNum = 0;

                while( byteNum < thisPacket.length() ) {
                    name = new String();
                    totaltime = new String();
                    kills = 0;

                    // Get this player's index
                    index = bytes[byteNum++];

                    while( bytes[byteNum] != 0x00 )
                        name += (char)bytes[byteNum++];
                    byteNum++;

                    kills = (bytes[byteNum])|(bytes[byteNum + 1] >> 8)|(bytes[byteNum + 2] >> 16)|(bytes[byteNum + 3] >> 24);

                    byteNum += 4;

                    time = Float.intBitsToFloat((int)((long)(bytes[byteNum] & 0xff)|(long)((bytes[byteNum + 1] & 0xff) << 8)|(long)((bytes[byteNum + 2] & 0xff) << 16)|(long)((bytes[byteNum + 3] & 0xff) << 24)));

                    byteNum += 4;

                    seconds = (short)(time % 60);
                    time -= seconds;
                    minutes = (short)((time / 60) % 60);
                    hours = (short)(Math.floor(time / 3600));

                    String hourString = (hours < 10)?"0" + Integer.toString(hours):Integer.toString(hours);
                    String minuteString = (minutes < 10)?"0" + Integer.toString(minutes):Integer.toString(minutes);
                    String secondString = (seconds < 10)?"0" + Integer.toString(seconds):Integer.toString(seconds);

                    totaltime = hourString + ":" + minuteString + ":" + secondString;

                    playerList.add(index, new PlayerRecord(name, totaltime, kills, index));
                }
            }

            return playerList;
        }
        catch( Exception e ) {
            status = -1;

            Log.w(TAG, "queryPlayers(): Caught an exception:");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( StackTraceElement x : ste )
                Log.e(TAG, "    " + x.toString());

            return null;
        }
    }
}