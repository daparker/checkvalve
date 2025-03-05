/*
 * Copyright 2010-2025 by David A. Parker <parker.david.a@gmail.com>
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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class QueryPlayers implements Runnable {
    private static final String TAG = QueryPlayers.class.getSimpleName();

    private int status;
    private final long rowId;
    private final Context context;
    private final Handler handler;

    public QueryPlayers(Context context, long rowId, Handler handler) {
        this.context = context;
        this.rowId = rowId;
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
        ArrayList<PlayerRecord> playerList;

        String[] packets;
        String serverURL;
        String name;
        String totaltime;

        int kills;
        int index;
        int header;
        int serverPort;
        int serverTimeout;

        short numplayers = 0;
        short numpackets = 0;
        short thispacket = 0;
        short hours = 0;
        short minutes = 0;
        short seconds = 0;

        float time;

        serverURL = sr.getServerURL();
        serverPort = sr.getServerPort();
        serverTimeout = sr.getServerTimeout();

        try {
            // Byte array for the incoming data
            byte[] arrayIn = new byte[1400];

            // UDP datagram packets
            packetOut = PacketFactory.getPacket(Values.BYTE_A2S_PLAYER, Values.CHALLENGE_QUERY);
            packetIn = new DatagramPacket(arrayIn, arrayIn.length);

            // Create a socket for querying the server
            socket = new DatagramSocket();
            socket.setSoTimeout(serverTimeout * 1000);

            // Connect to the remote server
            socket.connect(InetAddress.getByName(serverURL), serverPort);

            // Bail out if the connection failed
            if( !socket.isConnected() ) {
                if (!socket.isClosed() )
                    socket.close();

                throw new SocketException();
            }

            // Send the query string and get the response packet
            socket.send(packetOut);
            socket.receive(packetIn);

            // If we received a challenge response then query again to get the player data
            if( arrayIn[4] == Values.BYTE_CHALLENGE_RESPONSE ) {
                // Store the challenge response in a byte array
                byte[] challengeResponse = new byte[] {
                        arrayIn[5], arrayIn[6], arrayIn[7], arrayIn[8]
                };

                // UDP datagram packets
                packetOut = PacketFactory.getPacket(Values.BYTE_A2S_PLAYER, challengeResponse);
                packetIn = new DatagramPacket(arrayIn, arrayIn.length);

                // Show an error if the connection attempt failed
                if( !socket.isConnected() ) {
                    if( !socket.isClosed() )
                        socket.close();

                    throw new SocketException();
                }

                // Send the A2S_PLAYER query string and get the response packet
                socket.send(packetOut);
                socket.receive(packetIn);
            }

            if( arrayIn[4] == Values.BYTE_A2S_PLAYER_RESPONSE ) {
                ByteBuffer bufferIn = ByteBuffer.wrap(arrayIn, 0, packetIn.getLength());
                bufferIn.order(ByteOrder.LITTLE_ENDIAN);

                // Get the header info to see if data has been split over multiple packets
                header = bufferIn.getInt();

                numpackets = 1;

                // If the first 4 header bytes are 0xFFFFFFFE then there are multiple packets
                if (header == Values.INT_SPLIT_HEADER) {
                    /*
                     * If there are multiple packets, each packet will have 12 header bytes, but the "first" packet (packet
                     * 0) will have an additional 6 header bytes. UDP packets can arrive in any order, so we need to check
                     * the sequence number of each packet to know how many header bytes to strip.
                     */

                    bufferIn.getInt();    // Discard the answer ID
                    numpackets = bufferIn.get();
                    thispacket = bufferIn.get();
                    bufferIn.get();       // Discard the next byte

                    // Initialize the array to hold the number of packets in this response
                    packets = new String[numpackets];

                    // If this is packet 0 then skip the next 5 header bytes
                    if (thispacket == 0) {
                        bufferIn.position(bufferIn.position() + 6);
                        numplayers = bufferIn.get();
                    }

                    packets[thispacket] = new String(arrayIn, bufferIn.position(), bufferIn.remaining(), "ISO8859_1");

                    for (int i = 1; i < numpackets; i++) {
                        // Receive the response packet from the server
                        socket.receive(packetIn);

                        bufferIn = ByteBuffer.wrap(arrayIn, 0, packetIn.getLength());

                        // Get rid of 12 header bytes
                        bufferIn.position(9);
                        thispacket = bufferIn.get();
                        bufferIn.position(bufferIn.position() + 2);

                        // If this is packet 0 then skip the next 6 header bytes
                        if (thispacket == 0) {
                            bufferIn.position(bufferIn.position() + 6);
                            numplayers = bufferIn.get();
                        }

                        packets[thispacket] = new String(arrayIn, bufferIn.position(), bufferIn.remaining(), "ISO8859_1");
                    }
                } else {
                    // Get number of players (6th byte)
                    bufferIn.get();
                    numplayers = bufferIn.get();
                    packets = new String[]{new String(arrayIn, bufferIn.position(), bufferIn.remaining(), "ISO8859_1")};
                }

                socket.close();

                if (numplayers == 0) {
                    status = -2;
                    return null;
                }

                // Initialize the return array once we know how many elements we need
                playerList = new ArrayList<>();

                for (int i = 0; i < numpackets; i++) {
                    byte[] byteArray = packets[i].getBytes("ISO8859_1");
                    PacketData pd = new PacketData(byteArray);

                    while (pd.hasRemaining()) {
                        index = pd.getByte(); // Get this player's index
                        name = pd.getUTF8String(); // Determine the length of the player name
                        kills = pd.getInt();       // Get the number of kills
                        time = pd.getFloat();      // Get the connected time

                        // Parse the time into hours, minutes, and seconds
                        seconds = (short) (time % 60);
                        time -= seconds;
                        minutes = (short) ((time / 60) % 60);
                        hours = (short) (Math.floor(time / 3600));

                        // Values less than 10 should be left-padded with a zero
                        String hourString = (hours < 10) ? "0" + Integer.toString(hours) : Integer.toString(hours);
                        String minuteString = (minutes < 10) ? "0" + Integer.toString(minutes) : Integer.toString(minutes);
                        String secondString = (seconds < 10) ? "0" + Integer.toString(seconds) : Integer.toString(seconds);

                        // Assemble a string representation of the connected time
                        totaltime = hourString + ":" + minuteString + ":" + secondString;

                        Log.d(TAG, "Adding player [index=" + index + "][name=" + name + "][kills=" + kills + "][time=" + totaltime + "]");

                        // Add this player record to the playerList array at the correct position based on its index
                        if( playerList.isEmpty() || (index >= playerList.get(playerList.size()-1).getIndex()) ) {
                            playerList.add(new PlayerRecord(name, totaltime, kills, index));
                        }
                        else {
                            for( PlayerRecord p : playerList ) {
                                if( index < p.getIndex() ) {
                                    playerList.add(playerList.indexOf(p), new PlayerRecord(name, totaltime, kills, index));
                                }
                            }
                        }
                        // playerList.add(index, new PlayerRecord(name, totaltime, kills, index));
                    }
                }
            }
            else {
                Log.w(TAG, "queryPlayers(): Received packet is not a proper response!");
                return null;
            }
        }
        catch( Exception e ) {
            status = -1;
            Log.w(TAG, "queryPlayers(): Caught an exception:", e);
            return null;
        }

        return playerList;
    }
}