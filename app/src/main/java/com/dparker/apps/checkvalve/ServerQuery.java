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
import android.os.Bundle;
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
public class ServerQuery implements Runnable {
    private static final String TAG = ServerQuery.class.getSimpleName();

    private final Handler handler;
    private final Context context;
    private final boolean debug;
    private final QueryDebugLog debugLog;
    private ArrayList<String> messages;
    private ServerInfo[] serverInfo;
    private int status;

    /**
     * Construct a new instance of the ServerQuery class for collecting server information.
     *
     * @param c The context to use
     * @param h The Handler to use
     */
    public ServerQuery(Context c, Handler h, boolean debugMode, QueryDebugLog debugLog) {
        this.context = c;
        this.status = 0;
        this.handler = h;
        this.debug = debugMode;
        this.debugLog = debugLog;
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

        Log.d(TAG, "msg=" + msg);
        Log.d(TAG, "handler=" + handler.toString());
        Log.d(TAG, "Returning msg to handler");
        this.handler.sendMessage(msg);
        Log.d(TAG, "Done.");
    }

    public void queryServers() throws UnsupportedEncodingException {
        long runStartTime = System.currentTimeMillis();

        if( debug ) {
            debugLog.addMessage("Run started at " + runStartTime + "\n");
        }

        // Get the server list from the database
        DatabaseProvider database = new DatabaseProvider(context);
        ServerRecord[] serverList = database.getEnabledServers();
        database.close();

        if( debug ) {
            debugLog.addMessage(serverList.length + " servers will be queried.");
        }

        serverInfo = new ServerInfo[serverList.length];
        messages = new ArrayList<>();

        for( int i = 0; i < serverList.length; i++ ) {
            ServerRecord sr = serverList[i];

            long startTime;
            long endTime;
            long requestTime;
            long queryStart;
            long queryEnd;
            long queryTime;
            long parseStart;
            long parseEnd;
            long parseTime;

            queryStart = System.currentTimeMillis();

            if( debug ) {
                debugLog.addMessage("\nQUERY #" + (i + 1));
                debugLog.addMessage("> Start time: " + queryStart);
            }

            String serverName;
            String serverURL = sr.getServerURL();
            String serverNickname = sr.getServerNickname();
            long serverRowId = sr.getServerRowID();
            int serverPort = sr.getServerPort();
            int serverTimeout = sr.getServerTimeout();
            int serverListPos = sr.getServerListPosition();

            int socketTimeout = 0;

            // Use the nickname in error rows if there is one, otherwise use
            // the URL and port 
            if( ! serverNickname.isEmpty() ) {
                serverName = serverNickname;
            }
            else {
                serverName = serverURL
                        .concat(":")
                        .concat(Integer.toString(serverPort));
            }

            if( debug ) {
                debugLog.addMessage("> Server: " + serverURL + ":" + serverPort);
            }

            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(serverTimeout * 1000);
                socketTimeout = socket.getSoTimeout();

                // Byte buffers for packet data
                byte[] arrayIn = new byte[1400];

                // UDP datagram packets
                DatagramPacket packetOut = PacketFactory.getPacket(Values.BYTE_A2S_INFO, Values.A2S_INFO_QUERY.getBytes());
                DatagramPacket packetIn = new DatagramPacket(arrayIn, arrayIn.length);

                // Connect to the remote server
                socket.connect(InetAddress.getByName(serverURL), serverPort);

                // Show an error if the connection attempt failed
                if( !socket.isConnected() ) {
                    serverInfo[i] = null;
                    addErrorRow(serverName);

                    if( debug ) {
                        debugLog.addMessage("> Socket connect failed!");
                    }

                    continue;
                }

                String serverIP = socket.getInetAddress().getHostAddress();

                if( debug ) {
                    debugLog.addMessage("> Socket is connected");

                    String localAddr = socket.getLocalAddress().getHostAddress();
                    int localPort = socket.getLocalPort();
                    int timeout = socket.getSoTimeout();

                    debugLog.addMessage(">   Local addr: " + localAddr);
                    debugLog.addMessage(">   Local port: " + localPort);
                    debugLog.addMessage(">   Remote addr: " + serverIP);
                    debugLog.addMessage(">   Remote port: " + serverPort);
                    debugLog.addMessage(">   Timeout: " + timeout);
                }

                // Get the start time of the request
                startTime = System.currentTimeMillis();

                // Send the query string to the server
                socket.send(packetOut);

                if( debug ) {
                    debugLog.addMessage("> Sent query to " + serverIP + ":" + serverPort);
                }

                // Receive the response packet from the server
                socket.receive(packetIn);

                if( arrayIn[4] == Values.BYTE_CHALLENGE_RESPONSE ) {
                    Log.d(TAG, "queryServers(): Received a challenge response from " + serverName + ":" + serverPort);

                    if( debug ) {
                        debugLog.addMessage("> Received challenge from " + serverIP + ":" + serverPort);
                    }

                    byte[] challengeResponse = new byte[] {
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

                if( debug ) {
                    debugLog.addMessage("> Received response from " + serverIP + ":" + serverPort);
                }

                // Get the end time of the request
                endTime = System.currentTimeMillis();

                // Calculate how long this request took (the ping time)
                requestTime = endTime - startTime;

                // Close the UDP socket
                socket.close();

                if( debug ) {
                    debugLog.addMessage("> Disconnected from " + serverIP + ":" + serverPort);
                    debugLog.addMessage("> Request took " + requestTime + " ms");
                }

                Log.d(TAG, "queryServers(): Received A2S_INFO response from " + serverName + ":" + serverPort);

                ByteBuffer bufferIn = ByteBuffer.wrap(arrayIn);
                bufferIn.order(ByteOrder.BIG_ENDIAN);

                int packetHeader = bufferIn.getInt();

                // Make sure the packet includes the expected header bytes
                if( packetHeader != Values.INT_PACKET_HEADER ) {
                    String rcv = "0x" + String.format("%8s", Integer.toHexString(packetHeader)).replace(' ', '0').toUpperCase();
                    Log.w(TAG, "Packet header " + rcv + " does not match expected value 0xFFFFFFFF");
                    serverInfo[i] = null;
                    addErrorRow(serverName);

                    if( debug ) {
                        debugLog.addMessage("> ERROR: Invalid response header: " + rcv);
                    }

                    continue;
                }

                byte packetType = bufferIn.get();

                if( packetType == Values.BYTE_SOURCE_INFO ) {
                    // Parse response in the Source (and newer GoldSrc) format
                    Log.i(TAG, "Parsing Source Engine response from " + serverIP + ":" + serverPort);

                    if( debug ) {
                        debugLog.addMessage("> Response type: Source");
                    }

                    parseStart = System.currentTimeMillis();

                    serverInfo[i] = parseResponseFromSRCDS(arrayIn);
                    serverInfo[i].setAddress(serverIP);
                    serverInfo[i].setPort(serverPort);
                    serverInfo[i].setListPos(serverListPos);
                    serverInfo[i].setRowId(serverRowId);
                    serverInfo[i].setPing(requestTime);
                    serverInfo[i].setNickname(serverNickname);

                    parseEnd = System.currentTimeMillis();
                    parseTime = (parseEnd - parseStart);

                    if( debug ) {
                        debugLog.addMessage("> Parsed Source Engine response in " + parseTime + " ms");
                    }
                }
                else if( packetType == Values.BYTE_GOLDSRC_INFO ) {
                    // Parse response in the old GoldSrc format
                    Log.i(TAG, "Parsing GoldSrc Engine response from " + serverIP + ":" + serverPort);

                    if( debug ) {
                        debugLog.addMessage("> Response type: GoldSrc");
                    }

                    parseStart = System.currentTimeMillis();

                    serverInfo[i] = parseResponseFromHLDS(arrayIn);
                    serverInfo[i].setAddress(serverIP);
                    serverInfo[i].setPort(serverPort);
                    serverInfo[i].setListPos(serverListPos);
                    serverInfo[i].setRowId(serverRowId);
                    serverInfo[i].setPing(requestTime);
                    serverInfo[i].setNickname(serverNickname);

                    parseEnd = System.currentTimeMillis();
                    parseTime = (parseEnd - parseStart);

                    if( debug ) {
                        debugLog.addMessage("> Parsed GoldSrc Engine response in " + parseTime + " ms");
                    }
                }
                else {
                    // Packet type did not match 0x49 or 0x6D
                    String rcv = "0x" + String.format("%2s", Byte.toString(packetType)).replace(' ', '0').toUpperCase();
                    Log.w(TAG, "Response type " + rcv + " from " + serverIP + ":" + serverPort
                            + " does not match expected values 0x49 or 0x6d");
                    serverInfo[i] = null;
                    addErrorRow(serverName);

                    if( debug ) {
                        debugLog.addMessage("> ERROR: Invalid response type: " + rcv);
                    }

                    continue;
                }
            }
            catch( SocketTimeoutException e ) {
                if( debug ) {
                    debugLog.addMessage("> ERROR: Socket timed out after " + socketTimeout + " ms");
                }
                serverInfo[i] = null;
                addErrorRow(serverName);
            }
            catch( Exception e ) {
                if( debug ) {
                    debugLog.addMessage("> ERROR: Caught an exception: " + e);
                }
                serverInfo[i] = null;
                addErrorRow(serverName);
            }

            queryEnd = System.currentTimeMillis();
            queryTime = (queryEnd - queryStart);

            if( debug ) {
                debugLog.addMessage("> End time: " + queryEnd);
                debugLog.addMessage("> Query time: " + queryTime + " ms");
            }

            status++;
        }

        long runEndTime = System.currentTimeMillis();

        if( debug ) {
            debugLog.addMessage("\nRun finished at " + runEndTime + "\n");
            long totalRunTime = (runEndTime - runStartTime);
            debugLog.addMessage("Total time: " + totalRunTime + " ms");
        }
    }

    public void addErrorRow(String host) {
        String message = String.format(context.getString(R.string.msg_no_response), host);
        messages.add(message);
    }

    private ServerInfo parseResponseFromSRCDS(byte[] data) {
        String name;
        String map;
        String game;
        String version;
        String tags = new String();

        int numPlayers;
        int maxPlayers;

        PacketData pd = new PacketData(data);

        try {
            pd.setPosition(6);              // Skip the first 6 bytes
            name = pd.getUTF8String();      // Get the server name
            map = pd.getUTF8String();       // Get the map name
            pd.skipString();                // Skip the next string (game server path)
            game = pd.getUTF8String();      // Get the game description
            pd.skip(2);                     // Skip the next 2 bytes (Steam application ID)
            numPlayers = pd.getByte(); // Get the current number of players
            maxPlayers = pd.getByte(); // Get the maximum number of players
            pd.skip(5);                     // Skip 5 bytes
            version = pd.getUTF8String();   // Get the game version

            // If we're not at the end of the array then get the additional data
            if( pd.hasRemaining() ) {
                // This byte is the Extra Data Flag (EDF)
                int EDF = pd.getByte();

                // Skip the port number if included (2 bytes)
                if( (EDF & 0x80) > 0 ) pd.skip(2);

                // Skip the SteamID if included (8 bytes)
                if( (EDF & 0x10) > 0 ) pd.skip(8);

                // Skip SourceTV information if included (2 bytes and a string)
                if( (EDF & 0x40) > 0 ) {
                    pd.skip(2);
                    pd.skipString();
                }

                // Get the server tags (sv_tags) if any are included (string)
                if( (EDF & 0x20) > 0 ) tags = pd.getUTF8String();

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
        catch( Exception e ) {
            Log.w(TAG, "parseResponseFromSRCDS(): Caught an exception:", e);

            if( debug ) {
                debugLog.addMessage("> ERROR: Exception while parsing:");
                debugLog.addMessage("> " + e);
            }

            return null;
        }
    }

    public ServerInfo parseResponseFromHLDS(byte[] data) {
        String name;
        String map;
        String game;
        String version = new String();
        String tags = new String();

        int numPlayers;
        int maxPlayers;

        PacketData pd = new PacketData(data);

        try {
            pd.setPosition(5);              // Skip the first 5 bytes
            pd.skipString();                // Skip the server IP
            name = pd.getUTF8String();      // Get the server name
            map = pd.getUTF8String();       // Get the map name
            pd.skipString();                // Skip the game server path
            game = pd.getUTF8String();      // Get the game description
            numPlayers = pd.getByte();      // Get the current number of players
            maxPlayers = pd.getByte();      // Get the maximum number of players

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
        catch( Exception e ) {
            Log.w(TAG, "parseResponseFromHLDS(): Caught an exception:", e);

            if( debug ) {
                debugLog.addMessage("> ERROR: Exception while parsing:");
                debugLog.addMessage("> " + e.toString());
            }

            return null;
        }
    }
}