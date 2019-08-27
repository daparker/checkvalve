/*
 * Copyright 2010-2019 by David A. Parker <parker.david.a@gmail.com>
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
import android.os.Message;
import android.util.Log;
import android.annotation.SuppressLint;
import android.content.Context;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

@SuppressLint("DefaultLocale")
public class BackgroundServerQuery implements Runnable {
    private DatagramSocket socket;
    private Handler handler;
    private Context context;
    private ArrayList<String> messages;
    private int status;

    private static final String TAG = BackgroundServerQuery.class.getSimpleName();

    /**
     * Construct a new instance of the ServerQuery class for collecting server information.
     * 
     * @param c The context to use
     * @param h The Handler to use
     */
    public BackgroundServerQuery( Context c, Handler h ) {
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

    public void queryServers() throws UnsupportedEncodingException {        
        // Get the server list from the database
        DatabaseProvider database = new DatabaseProvider(context);
        ServerRecord[] serverList = database.getEnabledServers();
        database.close();
        
        // The outgoing data only needs to be set up once
        byte[] arrayOut = new byte[25];
        ByteBuffer bufferOut = ByteBuffer.wrap(arrayOut);
        
        bufferOut.order(ByteOrder.BIG_ENDIAN);        
        bufferOut.putInt(Values.INT_PACKET_HEADER);
        bufferOut.put(Values.BYTE_A2S_INFO);
        bufferOut.put(Values.A2S_INFO_QUERY.getBytes("UTF-8"));
        bufferOut.put((byte)0x00);

        messages = new ArrayList<String>();

        for( int i = 0; i < serverList.length; i++ ) {            
            ServerRecord sr = serverList[i];

            String serverName = new String();
            String serverURL = sr.getServerURL();
            String serverNickname = sr.getServerNickname();
            int serverPort = sr.getServerPort();
            int serverTimeout = sr.getServerTimeout();
            int serverListPos = sr.getServerListPosition();
                        
            // Use the nickname in error rows if there is one, otherwise use
            // the URL and port 
            if( serverNickname.length() > 0 ) {
                serverName = serverNickname;
            }
            else {
                serverName = serverURL
                        .concat(":")
                        .concat(Integer.toString(serverPort));
            }
            
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
                    addErrorRow(serverName, serverListPos);
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
                    addErrorRow(serverName, serverListPos);
                    
                    continue;
                }

                byte packetType = bufferIn.get();
                
                if( packetType != Values.BYTE_SOURCE_INFO && packetType != Values.BYTE_GOLDSRC_INFO ) {
                    // Packet type did not match 0x49 or 0x6D
                    String rcv = "0x" + String.format("%2s", Byte.toString(packetType)).replace(' ','0').toUpperCase();
                    
                    Log.w(TAG, "Response type " + rcv + " from " + serverIP + ":" + Integer.toString(serverPort) 
                            + " does not match expected values 0x49 or 0x6d");
                    
                    addErrorRow(serverName, serverListPos);

                    continue;
                }
            }
            catch( SocketTimeoutException e ) {
                Log.d(TAG, "queryServers(): No response from server " + serverName);
                addErrorRow(serverName, serverListPos);
            }
            catch( Exception e ) {
                Log.d(TAG, "queryServers(): Caught an exception:", e);
                addErrorRow(serverName, serverListPos);
            }

            status++;
        }
    }
    
    public void addErrorRow( String host, int pos ) {
        messages.add(host);
    }
}
