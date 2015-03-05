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
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.content.Context;
import android.database.Cursor;
import com.github.daparker.checkvalve.R;
import java.net.*;

public class SearchPlayers extends Thread
{
    //private Cursor databaseCursor;
    private Handler handler;
    private Context context;
    private TableRow[] tableRows;
    private TableRow[] messageRows;
    private String search;

    //    public SearchPlayers( Context c, Cursor d, TableRow[] t, TableRow[] m, Handler h, String s )
    //    {
    //        this.context = c;
    //        this.databaseCursor = d;
    //        this.tableRows = t;
    //        this.messageRows = m;
    //        this.handler = h;
    //        this.search = s;
    //    }

    public SearchPlayers( Context c, TableRow[] t, TableRow[] m, Handler h, String s )
    {
        this.context = c;
        this.tableRows = t;
        this.messageRows = m;
        this.handler = h;
        this.search = s;
    }

    public void run()
    {
        searchPlayers(search);

        if( handler != null ) this.handler.sendEmptyMessage(0);
    }

    public void searchPlayers( String search )
    {
        DatabaseProvider database = new DatabaseProvider(context);
        Cursor databaseCursor = null;

        databaseCursor = database.getAllServers();
        databaseCursor.moveToFirst();

        DatagramSocket socket;

        DatagramPacket packetOut;
        DatagramPacket packetIn;

        // Byte buffers for packet data
        byte[] bufferOut;
        byte[] bufferIn;
        byte[] tempBuffer;
        byte[] challengeResponse;

        // String variables
        String serverURL = new String();
        String resultString = new String();

        // Integer variables
        int byteNum = 0;
        int serverPort = 0;
        int serverTimeout = 0;
        int numResults = 0;

        int t = 0;
        int m = 0;

        for( int i = 0; i < databaseCursor.getCount(); i++ )
        {
            try
            {
                serverURL = databaseCursor.getString(1);
                serverPort = databaseCursor.getInt(2);
                serverTimeout = databaseCursor.getInt(3);

                String header = new String();
                String name = new String();

                short numplayers = 0;
                short numpackets = 0;
                short thispacket = 0;

                socket = new DatagramSocket();
                socket.setSoTimeout(serverTimeout * 1000);

                // Get the challenge response needed for the A2S_PLAYER query
                challengeResponse = ServerQuery.getChallengeResponse(serverURL, serverPort, serverTimeout);

                if( challengeResponse == null ) throw new NullPointerException();

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
                    bufferIn[x] = tempBuffer[x];

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
                     * If there are multiple packets, each packet will have 12 header bytes, but the "first" packet
                     * (packet 0) will have an additional 6 header bytes. UDP packets can arrive in any order, so we
                     * need to check the sequence number of each packet to know how many header bytes to strip.
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

                    for( int j = 1; j < numpackets; j++ )
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
                    if( !databaseCursor.isLast() ) databaseCursor.moveToNext();
                    continue;
                }

                for( int k = 0; k < numpackets; k++ )
                {
                    String thisPacket = packets[k];

                    byte[] bytes = thisPacket.getBytes("ISO8859_1");

                    byteNum = 0;

                    while( byteNum < thisPacket.length() )
                    {
                        name = "";
                        resultString = "";

                        // Skip the player index
                        byteNum++;

                        while( bytes[byteNum] != 0x00 )
                            name += (char)bytes[byteNum++];
                        byteNum++;

                        /*
                         * Check for a match
                         */
                        if( name.toLowerCase().indexOf(search.toLowerCase()) > -1 )
                        {
                            // We have a match!
                            numResults++;

                            resultString = "<b>" + name + "</b>";
                            resultString += " " + (String)context.getText(R.string.playing_on);
                            resultString += " " + serverURL + ":" + Integer.toString(serverPort);

                            TextView searchResult = new TextView(context);
                            searchResult.setId(0);
                            searchResult.setText(Html.fromHtml(resultString));
                            searchResult.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                            searchResult.setPadding(5, 0, 5, 0);
                            searchResult.setGravity(Gravity.LEFT);
                            searchResult.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                            TableRow row = new TableRow(context);
                            row.setId(0);
                            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                            row.addView(searchResult);

                            tableRows[t++] = row;
                        }

                        // Skip the next 8 bytes (number of kills and connection time)
                        byteNum += 8;
                    }
                }
            }
            // Handle a socket timeout
            catch( Exception e )
            {
                String message = new String();

                message += context.getText(R.string.msg_no_response);
                message += " " + serverURL + ":" + serverPort;

                TextView errorMessage = new TextView(context);

                errorMessage.setId(1);
                errorMessage.setText(message);
                errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                errorMessage.setPadding(5, 0, 5, 0);
                errorMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                // Create a TableRow and give it an ID
                TableRow messageRow = new TableRow(context);
                messageRow.setId(2);
                messageRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                messageRow.addView(errorMessage);

                // Add the TableRow to the TableLayout
                messageRows[m++] = messageRow;
            }

            if( !databaseCursor.isLast() ) databaseCursor.moveToNext();
        }

        if( numResults == 0 )
        {
            resultString = (String)context.getText(R.string.msg_no_search_results);

            TextView searchResult = new TextView(context);
            searchResult.setId(0);
            searchResult.setText(resultString);
            searchResult.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
            searchResult.setPadding(5, 0, 5, 0);
            searchResult.setGravity(Gravity.LEFT);
            searchResult.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            TableRow row = new TableRow(context);
            row.setId(0);
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            row.addView(searchResult);

            tableRows[0] = row;
        }

        if( databaseCursor != null ) databaseCursor.close();
    }
}