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

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Window;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.content.Intent;
import android.database.Cursor;
import com.github.daparker.checkvalve.R;
import java.lang.Math;
import java.net.*;

public class QueryPlayers extends Activity
{
    private DatabaseProvider database;
    private Cursor databaseCursor;
    private TableLayout player_info_table;
    private TableLayout message_table;

    private long rowId;
    private byte[] challengeResponse;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent thisIntent = getIntent();
        rowId = thisIntent.getLongExtra("rowId", 0);
        challengeResponse = thisIntent.getByteArrayExtra("challengeResponse");

        database = new DatabaseProvider(this);
        database.open();

        setContentView(R.layout.queryplayers);

        player_info_table = (TableLayout)findViewById(R.id.player_info_table);
        message_table = (TableLayout)findViewById(R.id.message_table);
        message_table.setVisibility(-1);

        try
        {
            queryPlayers();
        }
        catch( Exception e )
        {
            setResult(-1);

            if( database.isOpen() ) database.close();

            finish();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if( !database.isOpen() ) database.open();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if( database.isOpen() ) database.close();
    }

    public void queryPlayers() throws Exception
    {
        if( !database.isOpen() ) database.open();

        message_table.removeAllViews();

        databaseCursor = database.getServer(rowId);

        DatagramSocket socket;

        DatagramPacket packetOut;
        DatagramPacket packetIn;

        // Byte buffers for packet data
        byte[] bufferOut;
        byte[] bufferIn;
        byte[] tempBuffer;
        //byte[] challengeResponse;

        // String variables
        String serverURL = new String();

        // Integer variables
        int byteNum = 0;
        int serverPort = 0;
        int serverTimeout = 0;

        // Integer variables
        try
        {
            serverURL = databaseCursor.getString(1);
            serverPort = databaseCursor.getInt(2);
            serverTimeout = databaseCursor.getInt(3);

            String header = new String();
            String name = new String();
            String totaltime = new String();

            short numplayers = 0;
            short numpackets = 0;
            short thispacket = 0;
            short index = 0;
            short hours = 0;
            short minutes = 0;
            short seconds = 0;

            long kills = 0;
            float time = 0;

            socket = new DatagramSocket();
            socket.setSoTimeout(serverTimeout * 1000);

            // Get the challenge response needed for the A2S_PLAYER query
            //challengeResponse = ServerQuery.getChallengeResponse(serverURL, serverPort, serverTimeout);

            //if( challengeResponse == null ) throw new NullPointerException();

            // Challenge response becomes the A2S_PLAYER query by changing 0x41 to 0x55
            bufferOut = challengeResponse;
            bufferOut[4] = 0x55;

            tempBuffer = new byte[1400];

            // UDP datagram packets
            packetOut = new DatagramPacket(bufferOut, bufferOut.length);
            packetIn = new DatagramPacket(tempBuffer, tempBuffer.length);

            // Connect to the remote server
            socket.connect(InetAddress.getByName(serverURL), serverPort);

            // Show an error if the connection attempt failed
            if( !socket.isConnected() ) throw new SocketException();

            // Send the A2S_PLAYER query string and get the response packet
            socket.send(packetOut);
            socket.receive(packetIn);

            bufferIn = new byte[packetIn.getLength()];

            for( int x = 0; x < bufferIn.length; x++ )
            {
                bufferIn[x] = tempBuffer[x];
            }

            String[] packets = new String[100];

            // Store the response data in a string
            String response = new String(bufferIn, "ISO8859_1");

            // Get the header info to see if data has been split over multiple packets
            header = response.substring(0, 4);

            numpackets = 1;

            // If the first 4 header bytes are 0xFFFFFFFE then there are multiple packets
            if( header.equals("\u00FF\u00FF\u00FF\u00FE") )
            {
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
                if( thispacket == 0 )
                {
                    byteNum += 6;
                    numplayers = (short)bufferIn[byteNum++];
                }

                packets[thispacket] = response.substring(byteNum, response.length());

                for( int i = 1; i < numpackets; i++ )
                {
                    // Receive the response packet from the server
                    socket.receive(packetIn);

                    // Store the response data in a string
                    response = new String(bufferIn, "ISO8859_1");

                    // Get rid of 12 header bytes
                    byteNum = 9;
                    thispacket = (short)bufferIn[byteNum];
                    byteNum += 2;

                    // If this is packet 0 then skip the next 6 header bytes
                    if( thispacket == 0 )
                    {
                        byteNum += 6;
                        numplayers = (short)bufferIn[byteNum++];
                    }

                    packets[thispacket] = response.substring(byteNum, response.length());
                }
            }
            else
            {
                // Get number of players (6th byte)
                numplayers = (short)bufferIn[5];
                packets[0] = response.substring(6, response.length());
            }

            socket.close();

            if( numplayers == 0 )
            {
                setResult(-2);

                if( database.isOpen() ) database.close();

                finish();
            }

            for( int i = 0; i < numpackets; i++ )
            {
                String thisPacket = packets[i];

                byte[] bytes = thisPacket.getBytes("ISO8859_1");

                byteNum = 0;

                while( byteNum < thisPacket.length() )
                {
                    name = new String();
                    totaltime = new String();
                    kills = 0;

                    // Get this player's index
                    index = bytes[byteNum++];

                    while( bytes[byteNum] != 0x00 )
                        name += (char)bytes[byteNum++];
                    byteNum++;

                    kills = (bytes[byteNum]) | (bytes[byteNum + 1] >> 8) | (bytes[byteNum + 2] >> 16)
                            | (bytes[byteNum + 3] >> 24);

                    byteNum += 4;

                    time = Float.intBitsToFloat((int)((long)(bytes[byteNum] & 0xff)
                            | (long)((bytes[byteNum + 1] & 0xff) << 8) | (long)((bytes[byteNum + 2] & 0xff) << 16) | (long)((bytes[byteNum + 3] & 0xff) << 24)));

                    byteNum += 4;

                    seconds = (short)(time % 60);
                    time -= seconds;
                    minutes = (short)((time / 60) % 60);
                    hours = (short)(Math.floor(time / 3600));

                    String hourString = (hours < 10)?"0" + Integer.toString(hours):Integer.toString(hours);
                    String minuteString = (minutes < 10)?"0" + Integer.toString(minutes):Integer.toString(minutes);
                    String secondString = (seconds < 10)?"0" + Integer.toString(seconds):Integer.toString(seconds);

                    totaltime = hourString + ":" + minuteString + ":" + secondString;

                    TextView playerName = new TextView(this);
                    TextView numKills = new TextView(this);
                    TextView connected = new TextView(this);

                    playerName.setId(i * 100);
                    playerName.setText(name);
                    playerName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f);
                    playerName.setPadding(5, 0, 5, 0);
                    playerName.setGravity(Gravity.LEFT);
                    playerName.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    numKills.setId(i * 200);
                    numKills.setText(Long.toString(kills));
                    numKills.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f);
                    numKills.setPadding(5, 0, 5, 0);
                    numKills.setGravity(Gravity.CENTER_HORIZONTAL);
                    numKills.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    connected.setId(i * 300);
                    connected.setText(totaltime);
                    connected.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f);
                    connected.setPadding(5, 0, 5, 0);
                    connected.setGravity(Gravity.CENTER_HORIZONTAL);
                    connected.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    // Create a TableRow and give it an ID
                    TableRow row = new TableRow(this);
                    row.setId(index);
                    row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                    row.addView(playerName);
                    row.addView(numKills);
                    row.addView(connected);

                    // Add the TableRow to the TableLayout
                    player_info_table.addView(row, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                }

                player_info_table.setVisibility(1);
            }
        }
        /*        catch( NullPointerException npe )
                {
                    player_info_table.setVisibility(-1);

                    String message = new String();

                    message += this.getText(R.string.msg_no_challenge_response);
                    message += " " + serverURL + ":" + serverPort;

                    TextView errorMessage = new TextView(this);

                    errorMessage.setId(1);
                    errorMessage.setText(message);
                    errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    errorMessage.setPadding(3, 0, 3, 0);
                    errorMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    // Create a TableRow and give it an ID
                    TableRow messageRow = new TableRow(this);
                    messageRow.setId(2);
                    messageRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                    messageRow.addView(errorMessage);

                    // Add the TableRow to the TableLayout
                    message_table.addView(messageRow, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                    message_table.setVisibility(1);
                }*/
        catch( NullPointerException npe )
        {
            String message = new String();
            message += this.getText(R.string.msg_no_challenge_response);
            message += " " + serverURL + ":" + serverPort;
            UserVisibleMessage.showMessage(QueryPlayers.this, message);
        }
        catch( SocketException se )
        {
            player_info_table.setVisibility(-1);

            String message = new String();

            message += "Socket error: ";
            message += " " + serverURL + ":" + serverPort;

            TextView errorMessage = new TextView(this);

            errorMessage.setId(1);
            errorMessage.setText(message);
            errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
            errorMessage.setPadding(3, 0, 3, 0);
            errorMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            // Create a TableRow and give it an ID
            TableRow messageRow = new TableRow(this);
            messageRow.setId(2);
            messageRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            messageRow.addView(errorMessage);

            // Add the TableRow to the TableLayout
            message_table.addView(messageRow, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            message_table.setVisibility(1);
        }
        // Handle a socket timeout
        catch( Exception e )
        {
            player_info_table.setVisibility(-1);

            String message = new String();

            message += this.getText(R.string.msg_no_response);
            message += " " + serverURL + ":" + serverPort;

            TextView errorMessage = new TextView(this);

            errorMessage.setId(1);
            errorMessage.setText(message);
            errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
            errorMessage.setPadding(3, 0, 3, 0);
            errorMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            // Create a TableRow and give it an ID
            TableRow messageRow = new TableRow(this);
            messageRow.setId(2);
            messageRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            messageRow.addView(errorMessage);

            // Add the TableRow to the TableLayout
            message_table.addView(messageRow, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            message_table.setVisibility(1);
        }

        database.close();
    }
}