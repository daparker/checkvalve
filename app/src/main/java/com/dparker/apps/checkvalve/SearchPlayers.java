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
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressLint({"NewApi", "DefaultLocale"})
public class SearchPlayers extends Thread {
    private static final String TAG = SearchPlayers.class.getSimpleName();

    private final Handler handler;
    private final Context context;
    private final TableRow[] tableRows;
    private final TableRow[] messageRows;
    private final String search;

    public SearchPlayers(Context c, TableRow[] t, TableRow[] m, Handler h, String s) {
        this.context = c;
        this.tableRows = t;
        this.messageRows = m;
        this.handler = h;
        this.search = s.toLowerCase();
    }

    public void run() {
        searchPlayers(search);

        if( handler != null )
            this.handler.sendEmptyMessage(0);
    }

    @SuppressLint("ResourceType")
    public void searchPlayers(String search) {
        DatabaseProvider database = new DatabaseProvider(context);

        DatagramSocket socket;
        DatagramPacket packetOut;
        DatagramPacket packetIn;

        String[] packets;
        String resultString;
        String serverNickname;
        String serverURL = new String();

        int serverPort = 0;
        int serverTimeout = 0;
        int numResults = 0;
        int t = 0;
        int m = 0;

        ServerRecord[] serverList = database.getAllServers();
        database.close();

        for( ServerRecord sr : serverList ) {
            if( ! sr.isEnabled() )
                continue;

            try {
                serverNickname = sr.getServerNickname();
                serverURL = sr.getServerURL();
                serverPort = sr.getServerPort();
                serverTimeout = sr.getServerTimeout();

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

                // Close the socket
                //socket.close();

                // If we received a challenge response then query again to get the player data
                if( arrayIn[4] == Values.BYTE_CHALLENGE_RESPONSE ) {
                    // Store the challenge response in a byte array
                    byte[] challengeResponse = new byte[] {
                            arrayIn[5], arrayIn[6], arrayIn[7], arrayIn[8]
                    };

                    // UDP datagram packets
                    packetOut = PacketFactory.getPacket(Values.BYTE_A2S_PLAYER, challengeResponse);
                    packetIn = new DatagramPacket(arrayIn, arrayIn.length);

                    // Connect to the remote server
                    socket.connect(InetAddress.getByName(serverURL), serverPort);

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

                int header;
                String name;
                String host;

                short numplayers = 0;
                short numpackets = 0;
                short thispacket = 0;

                ByteBuffer bufferIn = ByteBuffer.wrap(arrayIn, 0, packetIn.getLength());
                bufferIn.order(ByteOrder.LITTLE_ENDIAN);

                // Get the header info to see if data has been split over multiple packets
                header = bufferIn.getInt();

                // If the first 4 header bytes are 0xFFFFFFFE then there are multiple packets
                if( header == Values.INT_SPLIT_HEADER ) {
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
                    if( thispacket == 0 ) {
                        bufferIn.position(bufferIn.position() + 6);
                        numplayers = bufferIn.get();
                    }

                    packets[thispacket] = new String(arrayIn, bufferIn.position(), bufferIn.remaining(), "ISO8859_1");

                    for( int i = 1; i < numpackets; i++ ) {
                        // Receive the response packet from the server
                        socket.receive(packetIn);

                        bufferIn = ByteBuffer.wrap(arrayIn, 0, packetIn.getLength());

                        // Get rid of 12 header bytes
                        bufferIn.position(9);
                        thispacket = bufferIn.get();
                        bufferIn.position(bufferIn.position() + 2);

                        // If this is packet 0 then skip the next 6 header bytes
                        if( thispacket == 0 ) {
                            bufferIn.position(bufferIn.position() + 6);
                            numplayers = bufferIn.get();
                        }

                        packets[thispacket] = new String(arrayIn, bufferIn.position(), bufferIn.remaining(), "ISO8859_1");
                    }
                }
                else {
                    // Get number of players (6th byte)
                    bufferIn.get();
                    numplayers = bufferIn.get();
                    packets = new String[]{new String(arrayIn, bufferIn.position(), bufferIn.remaining(), "ISO8859_1")};
                }

                socket.close();

                if( numplayers == 0 ) continue;

                for( int i = 0; i < packets.length; i++ ) {
                    byte[] byteArray = packets[i].getBytes("ISO8859_1");
                    PacketData pd = new PacketData(byteArray);

                    while( pd.hasRemaining() ) {
                        // Skip the player index
                        pd.skip(1);

                        // Get the player name
                        name = pd.getUTF8String();

                        // Check for a match
                        if( name.toLowerCase().contains(search) ) {
                            // We have a match!
                            numResults++;

                            // Use the server nickname if there is one, otherwise server:port
                            if( ! serverNickname.isEmpty() ) {
                                resultString = String.format(context.getString(R.string.playing_on),
                                        "<b>" + name + "</b>", serverNickname);
                            }
                            else {
                                host = serverURL + ":" + serverPort;
                                resultString = String.format(context.getString(R.string.playing_on),
                                        "<b>" + name + "</b>", host);
                            }

                            TextView searchResult = new TextView(context);
                            searchResult.setId(0);
                            searchResult.setText(Html.fromHtml(resultString));
                            searchResult.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                            searchResult.setPadding(5, 0, 5, 0);
                            searchResult.setGravity(Gravity.START);
                            searchResult.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                            TableRow row = new TableRow(context);
                            row.setId(0);
                            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                            row.addView(searchResult);

                            tableRows[t++] = row;
                        }

                        // Skip the next 8 bytes (number of kills and connection time)
                        pd.skip(8);
                    }
                }
            }
            catch( Exception e ) {
                Log.w(TAG, "queryPlayers(): Caught an exception:", e);

                String host = serverURL + ":" + serverPort;
                String message = String.format(context.getString(R.string.msg_no_response), host);

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
        }

        if( numResults == 0 ) {
            resultString = (String) context.getText(R.string.msg_no_search_results);

            TextView searchResult = new TextView(context);
            searchResult.setId(0);
            searchResult.setText(resultString);
            searchResult.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
            searchResult.setPadding(5, 0, 5, 0);
            searchResult.setGravity(Gravity.START);
            searchResult.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            TableRow row = new TableRow(context);
            row.setId(0);
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            row.addView(searchResult);

            tableRows[0] = row;
        }
    }
}