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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ChallengeResponseQuery implements Runnable {
    private Handler handler;
    private String server;
    private int status;
    private int port;
    private int timeout;
    private long rowId;
    private byte[] challengeResponse;

    private static final String TAG = ChallengeResponseQuery.class.getSimpleName();

    public ChallengeResponseQuery( String server, int port, int timeout, long rowId, Handler handler ) {
        this.server = server;
        this.port = port;
        this.timeout = timeout;
        this.rowId = rowId;
        this.handler = handler;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        status = 0;

        getChallengeResponse();

        if( handler != null ) {
            Bundle bundle = new Bundle();
            Message msg = new Message();

            bundle.putLong(Values.EXTRA_ROW_ID, this.rowId);
            bundle.putByteArray(Values.EXTRA_CHALLENGE_RESPONSE, this.challengeResponse);

            msg.obj = bundle;
            msg.what = status;

            if( challengeResponse == null ) Log.w(TAG, "run(): Challenge response is null");

            handler.sendMessage(msg);
        }
    }

    public void getChallengeResponse() {
        try {
            // Byte arrays for packet data
            byte[] arrayIn = new byte[9];
            byte[] arrayOut = new byte[9];
            
            ByteBuffer bufferOut = ByteBuffer.wrap(arrayOut);
            bufferOut.order(ByteOrder.BIG_ENDIAN);
            
            // Use A2S_PLAYER query string with 0xFFFFFFFF to get the challenge number
            bufferOut.putInt(Values.INT_PACKET_HEADER);
            bufferOut.put(Values.BYTE_A2S_PLAYER);
            bufferOut.putInt(Values.INT_PACKET_HEADER);
            bufferOut.flip();

            // UDP datagram packets
            DatagramPacket packetOut = new DatagramPacket(arrayOut, arrayOut.length);
            DatagramPacket packetIn = new DatagramPacket(arrayIn, arrayIn.length);

            // Create a socket for querying the server
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeout * 1000);

            // Connect to the remote server
            socket.connect(InetAddress.getByName(server), port);

            // Return null if the connection attempt failed
            if( !socket.isConnected() ) {
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
            challengeResponse = packetIn.getData();
            
            Log.d(TAG, "Received packet contains " + packetIn.getLength() + " bytes");
            Log.d(TAG, "Challenge response contains " + challengeResponse.length + " bytes");

            Log.d(TAG, "getChallengeResponse() finished: challengeResponse=" + ByteBuffer.wrap(challengeResponse).getInt(5) + "; status=" + status);
        }
        catch( Exception e ) {
            Log.w(TAG, "getChallengeResponse(): Caught an exception:", e);
            challengeResponse = null;
            status = 1;
        }

        return;
    }
}
