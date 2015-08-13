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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.content.Context;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import com.github.daparker.checkvalve.R;

public class ServerQuery implements Runnable {
    private DatagramSocket socket;
    private Handler handler;
    private Context context;
    private ArrayList<String> messages;
    private ServerInfo[] serverInfo;
    private int status;

    private static final String TAG = ServerQuery.class.getSimpleName();

    /**
     * Construct a new instance of the ServerQuery class for collecting server information.
     * 
     * @param c The context to use
     * @param t The TableRow array in which server information will be stored
     * @param m The TableRow array in which error messages will be stored
     * @param h The Handler to use
     */
    public ServerQuery( Context c, Handler h ) {
        this.context = c;
        this.status = 0;
        this.handler = h;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        status = 0;
        Bundle b = new Bundle();

        Log.d(TAG, "Calling queryServers()");
        
        try {
            queryServers();
            b.putStringArrayList(Values.MESSAGES, messages);
            b.putParcelableArray(Values.SERVER_INFO, serverInfo);
        }
        catch( Exception e ) {
            Log.w(TAG, "run(): Caught an exception:", e);            
            status = -1;
        }
        
        Message msg = new Message();
        msg.what = status;
        msg.obj = b;
        
        this.handler.sendMessage(msg);
    }

    public void queryServers() throws UnsupportedEncodingException {
        // Get the server list from the database
        DatabaseProvider database = new DatabaseProvider(context);
        ServerRecord[] serverList = database.getAllServers();
        database.close();
        
        // The outgoing data only needs to be set up once
        byte[] arrayOut = new byte[25];
        ByteBuffer bufferOut = ByteBuffer.wrap(arrayOut);
        
        bufferOut.order(ByteOrder.BIG_ENDIAN);        
        bufferOut.putInt(Values.INT_PACKET_HEADER);
        bufferOut.put(Values.BYTE_A2S_INFO);
        bufferOut.put(Values.A2S_INFO_QUERY.getBytes("UTF-8"));
        bufferOut.put((byte)0x00);

        serverInfo = new ServerInfo[serverList.length];
        messages = new ArrayList<String>();

        for( int i = 0; i < serverList.length; i++ ) {
            ServerRecord sr = serverList[i];

            String serverURL = sr.getServerName();
            long serverRowId = sr.getServerRowID();
            int serverPort = sr.getServerPort();
            int serverTimeout = sr.getServerTimeout();
            int serverListPos = sr.getServerListPosition();
            
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(serverTimeout * 1000);

                // Byte buffers for packet data
                byte[] arrayIn = new byte[1400];
                
                ByteBuffer bufferIn = ByteBuffer.wrap(arrayIn);
                bufferIn.order(ByteOrder.BIG_ENDIAN);
                
                // UDP datagram packets
                DatagramPacket packetOut = new DatagramPacket(arrayOut, arrayOut.length);
                DatagramPacket packetIn = new DatagramPacket(arrayIn, arrayIn.length);

                // Connect to the remote server
                socket.connect(InetAddress.getByName(serverURL), serverPort);

                // Show an error if the connection attempt failed
                if( !socket.isConnected() ) {
                    serverInfo[i] = null;
                    addErrorRow(serverURL, serverPort, serverListPos);
                    continue;
                }

                String serverIP = socket.getInetAddress().getHostAddress();

                // Send the query string to the server
                socket.send(packetOut);

                // Receive the response packet from the server
                socket.receive(packetIn);
                
                // Close the UDP socket
                socket.close();

                int packetHeader = bufferIn.getInt();
                
                // Make sure the packet includes the expected header bytes
                if( packetHeader != Values.INT_PACKET_HEADER ) {
                    String rcv = "0x" + String.format("%8s", Integer.toHexString(packetHeader)).replace(' ','0').toUpperCase();
                    Log.w(TAG, "Packet header " + rcv + " does not match expected value 0xFFFFFFFF");
                    serverInfo[i] = null;
                    addErrorRow(serverURL, serverPort, serverListPos);
                    continue;
                }

                byte packetType = bufferIn.get();
                
                if( packetType == Values.BYTE_SOURCE_INFO ) {
                    // Parse response in the Source (and newer GoldSrc) format
                    Log.i(TAG, "Parsing Source Engine response");
                    serverInfo[i] = SI_parseResponseFromSRCDS(arrayIn);
                    serverInfo[i].setAddress(serverIP);
                    serverInfo[i].setPort(serverPort);
                    serverInfo[i].setListPos(serverListPos);
                    serverInfo[i].setRowId(serverRowId);
                }
                else if( packetType == Values.BYTE_GOLDSRC_INFO ) {
                    // Parse response in the old GoldSrc format
                    Log.i(TAG, "Parsing GoldSrc Engine response");
                    serverInfo[i] = SI_parseResponseFromHLDS(arrayIn);
                    serverInfo[i].setAddress(serverIP);
                    serverInfo[i].setPort(serverPort);
                    serverInfo[i].setListPos(serverListPos);
                    serverInfo[i].setRowId(serverRowId);
                }
                else {
                    // Packet type did not match 0x49 or 0x6D
                    String rcv = "0x" + String.format("%2s", Byte.toString(packetType)).replace(' ','0').toUpperCase();
                    Log.w(TAG, "Packet type " + rcv + " does not match expected values 0x49 or 0x6d");
                    serverInfo[i] = null;
                    addErrorRow(serverURL, serverPort, serverListPos);
                    continue;
                }
            }
            catch( Exception e ) {
                serverInfo[i] = null;
                addErrorRow(serverURL, serverPort, serverListPos);
            }

            status++;
        }
    }
    
    public void addErrorRow( String server, int port, int pos ) {
        String message = new String();
        message += context.getText(R.string.msg_no_response);
        message += " " + server + ":" + port;
        messages.add(message);
    }
    
    private ServerInfo SI_parseResponseFromSRCDS(byte[] data) {
        String name = new String();
        String map = new String();
        String game = new String();
        String version = new String();
        String tags = new String();
        int numPlayers = 0;
        int maxPlayers = 0;
        
        // Start at byte 6 in the response data
        int byteNum = 6;

        // Get the server name
        while( data[byteNum] != 0x00 ) name += (char)data[byteNum++];
        byteNum++;

        // Get the map name
        while( data[byteNum] != 0x00 ) map += (char)data[byteNum++];
        byteNum++;

        // Skip the next string (game server path)
        while( data[byteNum] != 0x00 ) byteNum++;
        byteNum++;

        // Get the game description
        while( data[byteNum] != 0x00 ) game += (char)data[byteNum++];
        byteNum++;

        // Skip the next 2 bytes (Steam application ID)
        byteNum += 2;

        // Get the current number of players and move to the next byte
        numPlayers = (int)data[byteNum++];

        // Get the maximum number of players and move to the next byte
        maxPlayers = (int)data[byteNum++];

        byteNum += 5;

        while( data[byteNum] != 0x00 ) version += (char)data[byteNum++];
        byteNum++;

        // If we're not at the end of the array then get the additional data
        if( byteNum < data.length ) {
            // This byte is the Extra Data Flag (EDF)
            int EDF = (int)data[byteNum];
            byteNum++;

            // Skip the port number if included
            if( (EDF & 0x80) > 0 ) byteNum += 2;

            // Skip the SteamID if included
            if( (EDF & 0x10) > 0 ) byteNum += 8;

            // Skip SourceTV information if included
            if( (EDF & 0x40) > 0 ) {
                byteNum += 2;

                while( data[byteNum] != 0x00 ) byteNum++;
                byteNum++;
            }

            // Get the server tags (sv_tags) if any are included
            if( (EDF & 0x20) > 0 ) while( data[byteNum] != 0 )
                tags += (char)data[byteNum++];

            /*
             * Stop here (we're only interested in getting the server tags in this query)
             */
        }
        
        ServerInfo result = new ServerInfo();
        result.setName(name);
        result.setMap(map);
        result.setGame(game);
        result.setVersion(version);
        result.setNumPlayers(numPlayers);
        result.setMaxPlayers(maxPlayers);
        result.setTags(tags);
        
        return result;
    }
    
    public ServerInfo SI_parseResponseFromHLDS(byte[] data) {
        String name = new String();
        String map = new String();
        String game = new String();
        String version = new String();
        String tags = new String();
        int numPlayers = 0;
        int maxPlayers = 0;
        
        // Start at byte 5 in the response data
        int byteNum = 5;

        // Skip the server IP
        while( data[byteNum] != 0x00 ) byteNum++;
        byteNum++;
        
        // Get the server name
        while( data[byteNum] != 0x00 ) name += (char)data[byteNum++];
        byteNum++;

        // Get the map name
        while( data[byteNum] != 0x00 ) map += (char)data[byteNum++];
        byteNum++;

        // Skip the game server path
        while( data[byteNum] != 0x00 ) byteNum++;
        byteNum++;

        // Get the game description
        while( data[byteNum] != 0x00 ) game += (char)data[byteNum++];
        byteNum++;

        // Get the current number of players and move to the next byte
        numPlayers = (int)data[byteNum++];

        // Get the maximum number of players and move to the next byte
        maxPlayers = (int)data[byteNum++];
        
        ServerInfo result = new ServerInfo();
        result.setName(name);
        result.setMap(map);
        result.setGame(game);
        result.setVersion(version);
        result.setNumPlayers(numPlayers);
        result.setMaxPlayers(maxPlayers);
        result.setTags(tags);
        
        return result;
    }
}