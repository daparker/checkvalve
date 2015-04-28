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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.net.*;

public class ChallengeResponseQuery implements Runnable
{
    private Handler handler;
    private String server;
    private int status;
    private int port;
    private int timeout;
    private long rowId;
    private byte[] challengeResponse;

    private static final String TAG = ChallengeResponseQuery.class.getSimpleName();

    public ChallengeResponseQuery( String server, int port, int timeout, long rowId, Handler handler )
    {
        this.server = server;
        this.port = port;
        this.timeout = timeout;
        this.rowId = rowId;
        this.handler = handler;
    }

    public void run()
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        
        status = 0;

        getChallengeResponse();

        if( handler != null )
        {
            Bundle bundle = new Bundle();
            Message msg = new Message();

            bundle.putLong(Values.EXTRA_ROW_ID, this.rowId);
            bundle.putByteArray(Values.EXTRA_CHALLENGE_RESPONSE, this.challengeResponse);

            msg.obj = bundle;
            msg.what = status;

            if( challengeResponse == null )
                Log.w(TAG, "run(): Challenge response is null");

            handler.sendMessage(msg);
        }
    }

    public void getChallengeResponse()
    {
        DatagramSocket socket;

        DatagramPacket packetOut;
        DatagramPacket packetIn;

        byte[] bufferOut;
        byte[] bufferIn;

        try
        {
            // Use A2S_PLAYER query string with 0xFFFFFFFF to get the challenge number
            String queryString = "\u00FF\u00FF\u00FF\u00FF\u0055\u00FF\u00FF\u00FF\u00FF";

            // Byte buffers for packet data
            bufferOut = queryString.getBytes("ISO8859_1");
            bufferIn = new byte[1400];

            // UDP datagram packets
            packetOut = new DatagramPacket(bufferOut, bufferOut.length);
            packetIn = new DatagramPacket(bufferIn, bufferIn.length);

            // Create a socket for querying the server
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout * 1000);

            // Connect to the remote server
            socket.connect(InetAddress.getByName(server), port);

            // Return null if the connection attempt failed
            if( !socket.isConnected() )
            {
                Log.e(TAG, "getChallangeResponse(): Socket is not connected");
                socket.close();
                status = 1;
                return;
            }

            // Send the query string and get the response packet
            socket.send(packetOut);
            socket.receive(packetIn);

            // Close the socket
            socket.close();

            // Store the challenge response in a byte array
            challengeResponse = new byte[packetIn.getLength()];

            for( int i = 0; i < packetIn.getLength(); i++ )
                challengeResponse[i] = bufferIn[i];

            status = 0;

            Log.d(TAG, "getChallengeResponse() finished: challengeResponse=" + challengeResponse.toString() + "; status=" + status);
        }
        catch( Exception e )
        {
            Log.w(TAG, "getChallengeResponse(): Caught an exception:");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( StackTraceElement x : ste )
                Log.w(TAG, "    " + x.toString());

            challengeResponse = null;
            status = 1;
        }

        return;
    }
}
