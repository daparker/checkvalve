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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

@SuppressLint("DefaultLocale")
public class BackgroundServerQuery implements Runnable {
    private final Handler handler;
    private final Context context;
    private ArrayList<String> messages;
    private int status;

    private static final String TAG = BackgroundServerQuery.class.getSimpleName();

    /**
     * Construct a new instance of the ServerQuery class for collecting server information.
     *
     * @param c The context to use
     * @param h The Handler to use
     */
    public BackgroundServerQuery(Context c, Handler h) {
        this.context = c;
        this.status = 0;
        this.handler = h;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        status = 0;

        Log.d(TAG, "Calling queryServers()");

        try {
            queryServers();
        }
        catch( Exception e ) {
            Log.w(TAG, "run(): Caught an exception:", e);
            status = -1;
        }

        Message msg = new Message();
        msg.what = status;
        msg.obj = messages;

        this.handler.sendMessage(msg);
        Log.d(TAG, "Done.");
    }

    private void queryServers() throws UnsupportedEncodingException {
        // Get the server list from the database
        DatabaseProvider database = new DatabaseProvider(context);
        ServerRecord[] serverList = database.getEnabledServers();
        DatagramSocket socket;

        database.close();

        messages = new ArrayList<>();

        for (ServerRecord sr : serverList) {
            String serverName;
            String serverURL = sr.getServerURL();
            String serverNickname = sr.getServerNickname();
            int serverPort = sr.getServerPort();
            int serverTimeout = sr.getServerTimeout();

            // Use the nickname in error rows if there is one, otherwise use the URL and port
            if ( ! serverNickname.isEmpty() ) {
                serverName = serverNickname;
            } else {
                serverName = serverURL
                        .concat(":")
                        .concat(Integer.toString(serverPort));
            }

            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(serverTimeout * 1000);

                // Byte array for packet data
                byte[] arrayIn = new byte[1400];

                // UDP datagram packets
                DatagramPacket packetOut = PacketFactory.getPacket(Values.BYTE_A2S_INFO, Values.A2S_INFO_QUERY.getBytes());
                DatagramPacket packetIn = new DatagramPacket(arrayIn, arrayIn.length);

                // Connect to the remote server
                socket.connect(InetAddress.getByName(serverURL), serverPort);

                // Show an error if the connection attempt failed
                if (!socket.isConnected()) {
                    addErrorRow(serverName);
                    continue;
                }

                String serverIP = socket.getInetAddress().getHostAddress();

                // Send the query string to the server
                socket.send(packetOut);

                // Receive the response packet from the server
                socket.receive(packetIn);

                // If we received a challenge response then send another query for the server info
                if (arrayIn[4] == Values.BYTE_CHALLENGE_RESPONSE) {
                    Log.d(TAG, "queryServers(): Received a challenge response from " + serverURL + ":" + serverPort);

                    byte[] challengeResponse = new byte[]{
                            arrayIn[5], arrayIn[6], arrayIn[7], arrayIn[8]
                    };

                    // UDP datagram packets
                    packetOut = PacketFactory.getPacket(
                            Values.BYTE_A2S_INFO,
                            Values.A2S_INFO_QUERY.getBytes(),
                            challengeResponse
                    );

                    packetIn = new DatagramPacket(arrayIn, arrayIn.length);

                    // Send the query string to the server
                    socket.send(packetOut);

                    // Receive the response packet from the server
                    socket.receive(packetIn);
                }

                // Close the UDP socket
                socket.close();

                // Byte buffer for the received packet data
                ByteBuffer bufferIn = ByteBuffer.wrap(arrayIn);
                bufferIn.order(ByteOrder.BIG_ENDIAN);

                // Get the 4-byte header from the packet data
                int packetHeader = bufferIn.getInt();

                // Make sure the packet includes the expected header bytes
                if (packetHeader != Values.INT_PACKET_HEADER) {
                    String rcv = "0x" + String.format("%8s", Integer.toHexString(packetHeader)).replace(' ', '0').toUpperCase();
                    Log.w(TAG, "Packet header " + rcv + " does not match expected value 0xFFFFFFFF");
                    addErrorRow(serverName);

                    continue;
                }

                // Get the 1-byte packet type
                byte packetType = bufferIn.get();

                // Make sure the packet type matches what we expect it to
                if (packetType != Values.BYTE_SOURCE_INFO && packetType != Values.BYTE_GOLDSRC_INFO) {
                    String rcv = "0x" + String.format("%2s", Byte.toString(packetType)).replace(' ', '0').toUpperCase();

                    Log.w(TAG, "Response type " + rcv + " from " + serverIP + ":" + serverPort
                            + " does not match expected values 0x49 or 0x6d");

                    addErrorRow(serverName);
                    continue;
                }
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "queryServers(): No response from server " + serverName);
                addErrorRow(serverName);
            } catch (Exception e) {
                Log.d(TAG, "queryServers(): Caught an exception:", e);
                addErrorRow(serverName);
            }

            status++;
        }
    }

    private void addErrorRow(String host) {
        messages.add(host);
    }
}
