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

import com.dparker.apps.checkvalve.exceptions.NullResponseException;
import com.github.koraktor.steamcondenser.servers.GameServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EngineQuery implements Runnable {
    private final Handler handler;
    private final String server;
    private final int port;
    private final int timeout;

    private static final String TAG = EngineQuery.class.getSimpleName();

    /**
     * Construct a new instance of the EngineQuery class to determine a server's engine.
     *
     * @param s The IP address or URL of the server
     * @param p The listen port of the server
     * @param t The timeout for the server query (in milliseconds)
     * @param h The handler to use
     */
    public EngineQuery(String s, int p, int t, Handler h) {
        this.server = s;
        this.port = p;
        this.timeout = t;
        this.handler = h;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        Message msg = new Message();

        msg.obj = getServerEngine(server, port, timeout);
        handler.sendMessage(msg);
    }

    public GameServer getServerEngine(String s, int p, int t) {
        int appId;
        int byteNum;
        int firstByte;
        int secondByte;

        try {
            int engine;

            DatagramSocket socket = new DatagramSocket();

            // UDP datagram packets
            byte[] arrayIn = new byte[2048];
            DatagramPacket packetIn = new DatagramPacket(arrayIn, arrayIn.length);
            DatagramPacket packetOut = PacketFactory.getPacket(Values.BYTE_A2S_INFO, Values.A2S_INFO_QUERY.getBytes());

            // Connect to the remote server
            socket.setSoTimeout(t * 1000);
            socket.connect(InetAddress.getByName(s), p);

            // Show an error if the connection attempt failed
            if( !socket.isConnected() ) {
                socket.close();
                return null;
            }

            // Send the query string to the server
            socket.send(packetOut);

            // Receive the response packet from the server
            socket.receive(packetIn);

            if( arrayIn[4] == Values.BYTE_CHALLENGE_RESPONSE ) {
                byte[] challengeResponse = new byte[]{
                        arrayIn[5], arrayIn[6], arrayIn[7], arrayIn[8]
                };

                packetIn = new DatagramPacket(arrayIn, arrayIn.length);
                packetOut = PacketFactory.getPacket(
                        Values.BYTE_A2S_INFO,
                        Values.A2S_INFO_QUERY.getBytes(),
                        challengeResponse
                );

                // Send the query string to the server
                socket.send(packetOut);

                // Receive the response packet from the server
                socket.receive(packetIn);
            }

            if( arrayIn[4] == Values.BYTE_SOURCE_INFO ) {
                // Start at byte 6 in the response data
                byteNum = 6;

                // Skip all data before the app ID
                while (arrayIn[byteNum] != 0x00)
                    byteNum++;

                byteNum++;

                while (arrayIn[byteNum] != 0x00)
                    byteNum++;

                byteNum++;

                while (arrayIn[byteNum] != 0x00)
                    byteNum++;

                byteNum++;

                while (arrayIn[byteNum] != 0x00)
                    byteNum++;

                byteNum++;

                // Get the old Steam application ID
                firstByte = arrayIn[byteNum] & 0xff;
                secondByte = arrayIn[byteNum + 1] & 0xff;
                appId = firstByte | (secondByte << 8);

                // If the app ID is less than 200 then the engine is assumed to be GoldSource,
                // but we'll check for the 64-bit game ID later and use that if possible since
                // it's more accurate
                engine = (appId < 200) ? Values.ENGINE_GOLDSRC : Values.ENGINE_SOURCE;

                // Skip the next 9 bytes
                byteNum += 9;

                // Skip the game version string
                while (arrayIn[byteNum] != 0x00)
                    byteNum++;

                byteNum++;

                // If we're not at the end of the array then get the additional data
                if (byteNum < arrayIn.length) {
                    // This byte is the Extra Data Flag (EDF)
                    int EDF = arrayIn[byteNum];
                    byteNum++;

                    // Skip the port number if included
                    if ((EDF & 0x80) > 0) byteNum += 2;

                    // Skip the SteamID if included
                    if ((EDF & 0x10) > 0) byteNum += 8;

                    // Skip SourceTV information if included
                    if ((EDF & 0x40) > 0) {
                        byteNum += 2;

                        while (arrayIn[byteNum] != 0x00)
                            byteNum++;

                        byteNum++;
                    }

                    // Skip the server tags (sv_tags) if included
                    if ((EDF & 0x20) > 0) {
                        while (arrayIn[byteNum] != 0)
                            byteNum++;

                        byteNum++;
                    }

                    // Get the 64-bit game ID if it's included
                    if ((EDF & 0x01) > 0) {
                        // Get the app ID from the lowest 24 bits of the game ID
                        appId = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).put(arrayIn, byteNum, 3).put((byte) 0x00).getInt(0);

                        // Use this app ID to determine the engine
                        engine = (appId < 200) ? Values.ENGINE_GOLDSRC : Values.ENGINE_SOURCE;
                    }
                }
            }
            else if( arrayIn[4] == Values.BYTE_GOLDSRC_INFO ) {
                engine = Values.ENGINE_GOLDSRC;
            }
            else {
                Log.d(TAG, "getServerEngine(): Response did not match expected types.");
                socket.close();
                throw new NullResponseException();
            }

            socket.close();

            if( engine == Values.ENGINE_GOLDSRC ) {
                Log.i(TAG, "Server engine is GoldSrc");
            }
            else if( engine == Values.ENGINE_SOURCE ) {
                Log.i(TAG, "Server engine is Source");
            }
            else {
                Log.w(TAG, "Unrecognized server engine");
                return null;
            }

            return Server.getServer(engine, InetAddress.getByName(server), port);
        }
        // Handle an exception (socket timeout or incorrect response type)
        catch( Exception e ) {
            Log.w(TAG, "getServerEngine(): Caught an exception:", e);
            return null;
        }
    }
}