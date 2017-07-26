/*
 * Copyright 2010-2017 by David A. Parker <parker.david.a@gmail.com>
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
import android.annotation.SuppressLint;
import android.content.Context;
import com.github.daparker.checkvalve.R;
import com.github.daparker.checkvalve.exceptions.NullResponseException;
import com.github.daparker.checkvalve.exceptions.SocketNotConnectedException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressLint({ "NewApi", "DefaultLocale" })
public class SearchPlayers extends Thread {
    private static final String TAG = SearchPlayers.class.getSimpleName();

    private Handler handler;
    private Context context;
    private String search;
    private byte[] challengeResponse;
    
    // New - 2.0.8-dev
    private ArrayList<String> messages;
    private ArrayList<String> players;

    public SearchPlayers( Context c, Handler h, String s ) {
        this.context = c;
        this.handler = h;
        this.search = s.toLowerCase();
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        int status = 0;
        Bundle b = new Bundle();
        
        players = new ArrayList<String>();
        messages = new ArrayList<String>();

        Log.d(TAG, "Calling queryServers()");
        
        try {
            searchPlayers(search);
            b.putStringArrayList(Values.PLAYER_INFO, players);
            b.putStringArrayList(Values.MESSAGES, messages);
        }
        catch( Exception e ) {
            Log.w(TAG, "run(): Caught an exception:", e);            
            status = -1;
        }
        
        Message msg = new Message();
        msg.what = status;
        msg.obj = b;
        
        Log.d(TAG, "msg=" + msg.toString());
        Log.d(TAG, "handler=" + handler.toString());
        Log.d(TAG, "Returning msg to handler");
        this.handler.sendMessage(msg);
        Log.d(TAG, "Done.");
    }

    public void searchPlayers( String search ) {
        DatabaseProvider database = new DatabaseProvider(context);

        DatagramSocket socket;
        DatagramPacket packetOut;
        DatagramPacket packetIn;

        // Byte buffers for packet data
        byte[] bufferOut;
        byte[] bufferIn;

        // String variables
        String serverNickname = new String();
        String serverURL = new String();
        String resultString = new String();
        
        String[] packets;

        // Integer variables
        int serverPort = 0;
        int serverTimeout = 0;

        ServerRecord[] serverList = database.getEnabledServers();
        database.close();

        for( ServerRecord sr : serverList ) {
            try {
                serverNickname = sr.getServerNickname();
                serverURL = sr.getServerURL();
                serverPort = sr.getServerPort();
                serverTimeout = sr.getServerTimeout();

                //String header = new String();
                int header = 0;
                String name = new String();
                String host = new String();

                short numplayers = 0;
                short numpackets = 0;
                short thispacket = 0;

                socket = new DatagramSocket();
                socket.setSoTimeout(serverTimeout * 1000);

                // Use 0xFFFFFFFF as the query string to get the challenge number
                String queryString = "\u00FF\u00FF\u00FF\u00FF\u0055\u00FF\u00FF\u00FF\u00FF";

                // Byte buffers for packet data
                bufferOut = queryString.getBytes("ISO8859_1");
                bufferIn = new byte[1400];

                // UDP datagram packets
                packetOut = new DatagramPacket(bufferOut, bufferOut.length);
                packetIn = new DatagramPacket(bufferIn, bufferIn.length);

                // Connect to the remote server
                socket.connect(InetAddress.getByName(serverURL), serverPort);

                // Return null if the connection attempt failed
                if( !socket.isConnected() ) {
                    Log.e(TAG, "getChallangeResponse(): Socket is not connected");
                    socket.close();

                    throw new SocketNotConnectedException();
                }

                // Send the query string and get the response packet
                socket.send(packetOut);
                socket.receive(packetIn);

                challengeResponse = Arrays.copyOf(bufferIn, packetIn.getLength());

                if( challengeResponse == null ) {
                    if( !socket.isClosed() )
                        socket.close();

                    throw new NullResponseException();
                }

                // Challenge response becomes the A2S_PLAYER query by changing 0x41 to 0x55
                bufferOut = challengeResponse;
                bufferOut[4] = 0x55;
                bufferIn = new byte[1400];

                // UDP datagram packets
                packetOut = new DatagramPacket(bufferOut, bufferOut.length);
                packetIn = new DatagramPacket(bufferIn, bufferIn.length);

                // Send the A2S_PLAYER query string and get the response packet
                socket.send(packetOut);
                socket.receive(packetIn);
                
                ByteBuffer buffer = ByteBuffer.wrap(bufferIn, 0, packetIn.getLength());
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                // Get the header info to see if data has been split over multiple packets
                header = buffer.getInt();

                numpackets = 1;

                // If the first 4 header bytes are 0xFFFFFFFE then there are multiple packets
                if( header == Values.INT_SPLIT_HEADER ) {
                    /*
                     * If there are multiple packets, each packet will have 12 header bytes, but the "first" packet (packet
                     * 0) will have an additional 6 header bytes. UDP packets can arrive in any order, so we need to check
                     * the sequence number of each packet to know how many header bytes to strip.
                     */

                    buffer.getInt();    // Discard the answer ID
                    numpackets = (short)buffer.get();
                    thispacket = (short)buffer.get();
                    buffer.get();       // Discard the next byte

                    // Initialize the array to hold the number of packets in this response
                    packets = new String[numpackets];
                    
                    // If this is packet 0 then skip the next 5 header bytes
                    if( thispacket == 0 ) {
                        buffer.position(buffer.position()+6);
                        numplayers = (short)buffer.get();
                    }

                    packets[thispacket] = new String(bufferIn, buffer.position(), buffer.remaining(), "ISO8859_1");

                    for( int i = 1; i < numpackets; i++ ) {
                        // Receive the response packet from the server
                        socket.receive(packetIn);
                        
                        buffer = ByteBuffer.wrap(bufferIn, 0, packetIn.getLength());

                        // Get rid of 12 header bytes
                        buffer.position(9);
                        thispacket = (short)buffer.get();
                        buffer.position(buffer.position()+2);

                        // If this is packet 0 then skip the next 6 header bytes
                        if( thispacket == 0 ) {
                            buffer.position(buffer.position()+6);
                            numplayers = (short)buffer.get();
                        }

                        packets[thispacket] = new String(bufferIn, buffer.position(), buffer.remaining(), "ISO8859_1");
                    }
                }
                else {
                    // Get number of players (6th byte)
                    buffer.get();
                    numplayers = (short)buffer.get();
                    packets = new String[] {new String(bufferIn, buffer.position(), buffer.remaining(), "ISO8859_1")};
                }

                socket.close();

                if( numplayers == 0 ) continue;

                for( int i = 0; i < packets.length; i++ ) {
                    byte[] byteArray = packets[i].getBytes("ISO8859_1");
                    PacketData pd = new PacketData(byteArray);
                    
                    while( pd.hasRemaining() ) {
                        name = new String();
                        resultString = new String();

                        // Skip the player index
                        pd.skip(1);
                        
                        // Get the player name
                        name = pd.getUTF8String();

                        // Check for a match
                        if( name.toLowerCase().indexOf(search) > -1 ) {
                            // We have a match!

                            if( serverNickname.length() > 0 ) {
                                resultString = String.format(context.getString(R.string.playing_on),
                                        "<b>" + name + "</b>", serverNickname);
                            }
                            else {
                                host = serverURL + ":" + Integer.toString(serverPort);
                                resultString = String.format(context.getString(R.string.playing_on),
                                        "<b>" + name + "</b>", host);
                            }
                            
                            players.add(resultString);
                        }

                        // Skip the next 8 bytes (number of kills and connection time)
                        pd.skip(8);
                    }
                }
            }
            catch( Exception e ) {
                Log.w(TAG, "queryPlayers(): Caught an exception:", e);

                String host = new String();
                
                if( serverNickname.length() > 0 ) {
                    host = serverNickname;
                }
                else {
                    host = serverURL + ":" + Integer.toString(serverPort);
                }
                
                String message = String.format(context.getString(R.string.msg_no_response), host);

                messages.add(message);
            }
        }
    }
}