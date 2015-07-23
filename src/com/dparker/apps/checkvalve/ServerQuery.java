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

package com.dparker.apps.checkvalve;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.content.Context;
import android.graphics.Typeface;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.dparker.apps.checkvalve.R;

public class ServerQuery implements Runnable {
    private DatagramSocket socket;
    private Handler handler;
    private Context context;
    private TableRow[][] tableRows;
    private TableRow[] messageRows;
    private int status;
    private int m;

    private static final String TAG = ServerQuery.class.getSimpleName();

    /**
     * Construct a new instance of the ServerQuery class for collecting server information.
     * 
     * @param c The context to use
     * @param t The TableRow array in which server information will be stored
     * @param m The TableRow array in which error messages will be stored
     * @param h The Handler to use
     */
    public ServerQuery( Context c, TableRow[][] t, TableRow[] m, Handler h ) {
        this.messageRows = m;
        this.tableRows = t;
        this.context = c;
        this.status = 0;
        this.handler = h;
        this.m = 0;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        status = 0;

        Log.d(TAG, "Calling queryServers()");
        
        try {
            queryServers();
        }
        catch( Exception e ) {
            Log.w(TAG, "run(): Caught an exception:");
            Log.w(TAG, e.toString());
            
            StackTraceElement[] ste = e.getStackTrace();
            
            for( StackTraceElement x : ste )
                Log.w(TAG, "    " + x.toString());
            
            status = -1;
        }

        if( handler != null ) this.handler.sendEmptyMessage(status);
    }

    public void queryServers() throws UnsupportedEncodingException {
        // Get the server list from the database
        DatabaseProvider database = new DatabaseProvider(context);
        ServerRecord[] serverList = database.getAllServers();
        database.close();
        
        m = 0;

        // The outgoing data only needs to be set up once
        byte[] arrayOut = new byte[25];
        ByteBuffer bufferOut = ByteBuffer.wrap(arrayOut);
        
        bufferOut.order(ByteOrder.BIG_ENDIAN);        
        bufferOut.putInt(Values.INT_PACKET_HEADER);
        bufferOut.put(Values.BYTE_A2S_INFO);
        bufferOut.put(Values.A2S_INFO_QUERY.getBytes("UTF-8"));
        bufferOut.put((byte)0x00);
                
        // Allocate the TableRow array
        if( tableRows == null )
            tableRows = new TableRow[serverList.length][6];

        // Initialize the message rows array
        for( int i = 0; i < messageRows.length; i++ )
            messageRows[i] = null;

        for( int i = 0; i < serverList.length; i++ ) {
            ServerRecord sr = serverList[i];
            Bundle response = new Bundle();

            String serverURL = sr.getServerName();
            int serverRowId = (int)sr.getServerRowID();
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
                    tableRows[i] = null;
                    addErrorRow(serverURL, serverPort, serverListPos);
                }

                String serverIP = socket.getInetAddress().getHostAddress();

                // Send the query string to the server
                socket.send(packetOut);

                // Receive the response packet from the server
                socket.receive(packetIn);

                int packetHeader = bufferIn.getInt();
                
                // Make sure the packet includes the expected header bytes
                if( packetHeader != 0xFFFFFFFF ) {
                    String rcv = "0x" + String.format("%8s", Integer.toHexString(packetHeader)).replace(' ','0').toUpperCase();
                    Log.w(TAG, "Packet header " + rcv + " does not match expected value 0xFFFFFFFF");
                    tableRows[i] = null;
                    addErrorRow(serverURL, serverPort, serverListPos);
                }
                
                byte packetType = bufferIn.get();
                
                if( packetType == 0x49 ) {
                    // Parse response in the Source (and newer GoldSrc) format
                    Log.i(TAG, "Parsing Source Engine response");
                    response = parseResponseFromSRCDS(arrayIn);
                }
                else if( packetType == 0x6d ) {
                    // Parse response in the old GoldSrc format
                    Log.i(TAG, "Parsing GoldSrc Engine response");
                    response = parseResponseFromHLDS(arrayIn);
                }
                else {
                    // Packet type did not match 0x49 or 0x6D
                    String rcv = "0x" + String.format("%2s", Byte.toString(packetType)).replace(' ','0').toUpperCase();
                    Log.w(TAG, "Packet type " + rcv + " does not match expected values 0x49 or 0x6d");
                    tableRows[i] = null;
                    addErrorRow(serverURL, serverPort, serverListPos);
                }
                
                // Close the UDP socket
                socket.close();
                
                String serverName = response.getString(Values.SERVER_NAME);
                String serverMap = response.getString(Values.SERVER_MAP);
                String serverGame = response.getString(Values.SERVER_GAME);
                String gameVersion = response.getString(Values.SERVER_VERSION);
                String serverTags = response.getString(Values.SERVER_TAGS);
                int serverNumPlayers = response.getInt(Values.SERVER_NUM_PLAYERS);
                int serverMaxPlayers = response.getInt(Values.SERVER_MAX_PLAYERS);

                TextView serverLabel = new TextView(context);
                TextView serverValue = new TextView(context);
                TextView ipLabel = new TextView(context);
                TextView ipValue = new TextView(context);
                TextView gameLabel = new TextView(context);
                TextView gameValue = new TextView(context);
                TextView mapLabel = new TextView(context);
                TextView mapValue = new TextView(context);
                TextView playersLabel = new TextView(context);
                TextView playersValue = new TextView(context);
                TextView tagsLabel = new TextView(context);
                TextView tagsValue = new TextView(context);
                TextView spacer = new TextView(context);

                spacer.setId(i * 100);
                spacer.setText("");
                spacer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                spacer.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                serverLabel.setId(i * 200);
                serverLabel.setText(context.getText(R.string.label_server));
                serverLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                serverLabel.setPadding(3, 0, 3, 0);
                serverLabel.setTypeface(null, Typeface.BOLD);
                serverLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                serverValue.setId(i * 300);
                serverValue.setText(serverName);
                serverValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                serverValue.setPadding(3, 0, 3, 0);
                serverValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                ipLabel.setId(i * 400);
                ipLabel.setText(context.getText(R.string.label_ip));
                ipLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                ipLabel.setPadding(3, 0, 3, 0);
                ipLabel.setTypeface(null, Typeface.BOLD);
                ipLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                ipValue.setId(i * 500);
                ipValue.setText(serverIP + ":" + serverPort);
                ipValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                ipValue.setPadding(3, 0, 3, 0);
                ipValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                gameLabel.setId(i * 600);
                gameLabel.setText(context.getText(R.string.label_game));
                gameLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                gameLabel.setPadding(3, 0, 3, 0);
                gameLabel.setTypeface(null, Typeface.BOLD);
                gameLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                gameValue.setId(i * 700);
                gameValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                gameValue.setPadding(3, 0, 3, 0);
                gameValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                if( gameVersion.length() > 0 )
                    gameValue.setText(serverGame + " [" + gameVersion + "]");
                else
                    gameValue.setText(serverGame);

                mapLabel.setId(i * 800);
                mapLabel.setText(context.getText(R.string.label_map));
                mapLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                mapLabel.setPadding(3, 0, 3, 0);
                mapLabel.setTypeface(null, Typeface.BOLD);
                mapLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                mapValue.setId(i * 900);
                mapValue.setText(serverMap);
                mapValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                mapValue.setPadding(3, 0, 3, 0);
                mapValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                playersLabel.setId(i * 1000);
                playersLabel.setText(context.getText(R.string.label_players));
                playersLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                playersLabel.setPadding(3, 0, 3, 0);
                playersLabel.setTypeface(null, Typeface.BOLD);
                playersLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                playersValue.setId(i * 1100);
                playersValue.setText(serverNumPlayers + "/" + serverMaxPlayers);
                playersValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                playersValue.setPadding(3, 0, 3, 0);
                playersValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                tagsLabel.setId(i * 1200);
                tagsLabel.setText(context.getText(R.string.label_tags));
                tagsLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                tagsLabel.setPadding(3, 0, 3, 0);
                tagsLabel.setTypeface(null, Typeface.BOLD);
                tagsLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                tagsValue.setId(i * 1300);
                tagsValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                tagsValue.setPadding(3, 0, 3, 0);
                tagsValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                if( serverTags.length() > 0 )
                    tagsValue.setText(serverTags);
                else
                    tagsValue.setText(context.getText(R.string.msg_no_tags));

                TableRow spacerRow = new TableRow(context);
                spacerRow.setId(0);
                spacerRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                spacerRow.addView(spacer);

                // Create a TableRow and give it an ID
                TableRow serverRow = new TableRow(context);
                serverRow.setId(serverRowId);
                serverRow.setTag(Values.TAG_SERVER_NAME);
                serverRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                TableRow ipRow = new TableRow(context);
                ipRow.setId(serverRowId);
                ipRow.setTag(Values.TAG_SERVER_IP);
                ipRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                TableRow gameRow = new TableRow(context);
                gameRow.setId(serverRowId);
                gameRow.setTag(Values.TAG_SERVER_GAME);
                gameRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                TableRow mapRow = new TableRow(context);
                mapRow.setId(serverRowId);
                mapRow.setTag(Values.TAG_SERVER_MAP);
                mapRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                TableRow playersRow = new TableRow(context);
                playersRow.setId(serverRowId);
                playersRow.setTag(Values.TAG_SERVER_PLAYERS);
                playersRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                TableRow tagsRow = new TableRow(context);
                tagsRow.setId(serverRowId);
                tagsRow.setTag(Values.TAG_SERVER_TAGS);
                tagsRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                serverRow.addView(serverLabel);
                serverRow.addView(serverValue);
                ipRow.addView(ipLabel);
                ipRow.addView(ipValue);
                gameRow.addView(gameLabel);
                gameRow.addView(gameValue);
                mapRow.addView(mapLabel);
                mapRow.addView(mapValue);
                playersRow.addView(playersLabel);
                playersRow.addView(playersValue);
                tagsRow.addView(tagsLabel);
                tagsRow.addView(tagsValue);

                tableRows[i][0] = serverRow;
                tableRows[i][1] = ipRow;
                tableRows[i][2] = gameRow;
                tableRows[i][3] = mapRow;
                tableRows[i][4] = playersRow;
                tableRows[i][5] = tagsRow;
            }
            catch( Exception e ) {
                tableRows[i] = null;
                addErrorRow(serverURL, serverPort, serverListPos);
            }

            status++;
        }
    }
    
    public void addErrorRow( String server, int port, int pos ) {
        String message = new String();

        message += context.getText(R.string.msg_no_response);
        message += " " + server + ":" + port;

        TextView errorMessage = new TextView(context);

        errorMessage.setId(100 + pos + 2);
        errorMessage.setText(message);
        errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
        errorMessage.setPadding(3, 0, 3, 0);
        errorMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        // Create a TableRow and give it an ID
        TableRow messageRow = new TableRow(context);
        messageRow.setId(0);
        messageRow.setBackgroundResource(R.color.translucent_red);
        messageRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        messageRow.addView(errorMessage);

        messageRows[m++] = messageRow;
    }
    
    public Bundle parseResponseFromSRCDS(byte[] bufferIn) {
        Bundle result = new Bundle();

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
        while( bufferIn[byteNum] != 0x00 ) name += (char)bufferIn[byteNum++];
        byteNum++;

        // Get the map name
        while( bufferIn[byteNum] != 0x00 ) map += (char)bufferIn[byteNum++];
        byteNum++;

        // Skip the next string (game server path)
        while( bufferIn[byteNum] != 0x00 ) byteNum++;
        byteNum++;

        // Get the game description
        while( bufferIn[byteNum] != 0x00 ) game += (char)bufferIn[byteNum++];
        byteNum++;

        // Skip the next 2 bytes (Steam application ID)
        byteNum += 2;

        // Get the current number of players and move to the next byte
        numPlayers = (int)bufferIn[byteNum++];

        // Get the maximum number of players and move to the next byte
        maxPlayers = (int)bufferIn[byteNum++];

        byteNum += 5;

        while( bufferIn[byteNum] != 0x00 ) version += (char)bufferIn[byteNum++];
        byteNum++;

        // If we're not at the end of the array then get the additional data
        if( byteNum < bufferIn.length ) {
            // This byte is the Extra Data Flag (EDF)
            int EDF = (int)bufferIn[byteNum];
            byteNum++;

            // Skip the port number if included
            if( (EDF & 0x80) > 0 ) byteNum += 2;

            // Skip the SteamID if included
            if( (EDF & 0x10) > 0 ) byteNum += 8;

            // Skip SourceTV information if included
            if( (EDF & 0x40) > 0 ) {
                byteNum += 2;

                while( bufferIn[byteNum] != 0x00 ) byteNum++;
                byteNum++;
            }

            // Get the server tags (sv_tags) if any are included
            if( (EDF & 0x20) > 0 ) while( bufferIn[byteNum] != 0 )
                tags += (char)bufferIn[byteNum++];

            /*
             * Stop here (we're only interested in getting the server tags in this query)
             */
        }
        
        result.putString(Values.SERVER_NAME, name);
        result.putString(Values.SERVER_MAP, map);
        result.putString(Values.SERVER_GAME, game);
        result.putString(Values.SERVER_VERSION, version);
        result.putString(Values.SERVER_TAGS, tags);
        result.putInt(Values.SERVER_NUM_PLAYERS, numPlayers);
        result.putInt(Values.SERVER_MAX_PLAYERS, maxPlayers);
        
        return result;
    }
    
    public Bundle parseResponseFromHLDS(byte[] bufferIn) {
        Bundle result = new Bundle();

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
        while( bufferIn[byteNum] != 0x00 ) byteNum++;
        byteNum++;
        
        // Get the server name
        while( bufferIn[byteNum] != 0x00 ) name += (char)bufferIn[byteNum++];
        byteNum++;

        // Get the map name
        while( bufferIn[byteNum] != 0x00 ) map += (char)bufferIn[byteNum++];
        byteNum++;

        // Skip the game server path
        while( bufferIn[byteNum] != 0x00 ) byteNum++;
        byteNum++;

        // Get the game description
        while( bufferIn[byteNum] != 0x00 ) game += (char)bufferIn[byteNum++];
        byteNum++;

        // Get the current number of players and move to the next byte
        numPlayers = (int)bufferIn[byteNum++];

        // Get the maximum number of players and move to the next byte
        maxPlayers = (int)bufferIn[byteNum++];
        
        result.putString(Values.SERVER_NAME, name);
        result.putString(Values.SERVER_MAP, map);
        result.putString(Values.SERVER_GAME, game);
        result.putString(Values.SERVER_VERSION, version);
        result.putString(Values.SERVER_TAGS, tags);
        result.putInt(Values.SERVER_NUM_PLAYERS, numPlayers);
        result.putInt(Values.SERVER_MAX_PLAYERS, maxPlayers);
        
        return result;
    }
}
