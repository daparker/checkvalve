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
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.content.Context;
import android.graphics.Typeface;
import java.net.*;
import com.github.daparker.checkvalve.R;

public class ServerQuery implements Runnable
{
    private DatagramSocket socket;
    private Handler handler;
    private Context context;
    private TableRow[][] tableRows;
    private TableRow[] messageRows;
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
    public ServerQuery( Context c, TableRow[][] t, TableRow[] m, Handler h )
    {
        this.messageRows = m;
        this.tableRows = t;
        this.context = c;
        this.status = 0;
        this.handler = h;
    }

    public void run()
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        status = 0;

        Log.d(TAG, "Calling queryServers()");
        queryServers();

        if( handler != null )
            this.handler.sendEmptyMessage(status);
    }

    public void queryServers()
    {
        // Get the server list from the database
        DatabaseProvider database = new DatabaseProvider(context);
        ServerRecord[] serverList = database.getAllServers();
        database.close();

        // Allocate the TableRow array
        if( tableRows == null ) tableRows = new TableRow[serverList.length][6];

        // Initialize the message rows array
        for( int i = 0; i < messageRows.length; i++ )
            messageRows[i] = null;

        // String variables
        String serverURL = new String();
        String serverName = new String();
        String serverIP = new String();
        String serverMap = new String();
        String serverGame = new String();
        String gameVersion = new String();
        String serverTags = new String();

        // Integer variables
        int byteNum = 0;
        int serverPort = 0;
        int serverTimeout = 0;
        int serverNumPlayers = 0;
        int serverMaxPlayers = 0;
        int serverRowId = 0;
        int serverListPos = 0;

        int m = 0;

        for( int i = 0; i < serverList.length; i++ )
        {
            ServerRecord sr = serverList[i];

            try
            {
                socket = new DatagramSocket();

                // String variables
                serverURL = new String();
                serverName = new String();
                serverIP = new String();
                serverMap = new String();
                serverGame = new String();
                gameVersion = new String();
                serverTags = new String();

                serverRowId = (int)sr.getServerRowID();
                serverURL = sr.getServerName();
                serverPort = sr.getServerPort();
                serverTimeout = sr.getServerTimeout();
                serverListPos = sr.getServerListPosition();

                // A2S_INFO query string
                String queryString = "\u00FF\u00FF\u00FF\u00FF\u0054Source Engine Query\0";

                socket.setSoTimeout(serverTimeout * 1000);

                // Byte buffers for packet data
                byte[] bufferOut = queryString.getBytes("ISO8859_1");
                byte[] bufferIn = new byte[1400];

                // UDP datagram packets
                DatagramPacket packetOut = new DatagramPacket(bufferOut, bufferOut.length);
                DatagramPacket packetIn = new DatagramPacket(bufferIn, bufferIn.length);

                // Connect to the remote server
                socket.connect(InetAddress.getByName(serverURL), serverPort);

                // Show an error if the connection attempt failed
                if( !socket.isConnected() ) throw new Exception();

                serverIP = socket.getInetAddress().getHostAddress();

                // Send the query string to the server
                socket.send(packetOut);

                // Receive the response packet from the server
                socket.receive(packetIn);

                // Start at byte 6 in the response data
                byteNum = 6;

                // Get the server name
                while( bufferIn[byteNum] != 0x00 )
                    serverName += (char)bufferIn[byteNum++];

                byteNum++;

                // Get the map name
                while( bufferIn[byteNum] != 0x00 )
                    serverMap += (char)bufferIn[byteNum++];

                byteNum++;

                // Skip the next string (game server path)
                while( bufferIn[byteNum] != 0x00 )
                    byteNum++;

                byteNum++;

                // Get the game description
                while( bufferIn[byteNum] != 0x00 )
                    serverGame += (char)bufferIn[byteNum++];

                byteNum++;

                // Skip the next 2 bytes (Steam application ID)
                byteNum += 2;

                // Get the current number of players and move to the next byte
                serverNumPlayers = (int)bufferIn[byteNum++];

                // Get the maximum number of players and move to the next byte
                serverMaxPlayers = (int)bufferIn[byteNum++];

                byteNum += 5;

                while( bufferIn[byteNum] != 0x00 )
                    gameVersion += (char)bufferIn[byteNum++];

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

                    // Get the server tags (sv_tags) if any are included
                    if( (EDF & 0x20) > 0 ) while( bufferIn[byteNum] != 0 )
                        serverTags += (char)bufferIn[byteNum++];

                    /*
                     * Stop here (we're only interested in getting the server tags in this query)
                     */
                }

                // Close the UDP socket
                socket.close();

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
                gameValue.setText(serverGame + " [" + gameVersion + "]");
                gameValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                gameValue.setPadding(3, 0, 3, 0);
                gameValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

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
            // Handle a socket timeout
            catch( Exception e )
            {
                tableRows[i] = null;

                String message = new String();

                message += context.getText(R.string.msg_no_response);
                message += " " + serverURL + ":" + serverPort;

                TextView errorMessage = new TextView(context);

                //errorMessage.setId(100 + i + 2);
                errorMessage.setId(100 + serverListPos + 2);
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

            status++;
        }
    }
}
