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
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class implements a CheckValve Chat Relay client.
 */
public class Chat implements Runnable
{
    private static final String TAG = "Chat";

    private static byte responseType;
    private static byte protocolVersion;
    private static byte sayTeam;
    private static short contentLength;
    private static int serverTimestamp;
    private static int packetHeader;
    private static String passwordString;
    private static StringBuilder responseMessage;
    private static Socket s;
    private static String[] fields = new String[6];
    private static Message msg;
    private static ChatMessage chatMsg;
    private static InputStream in;
    private static OutputStream out;

    // Make sure the data and request buffers have backing arrays
    private static byte[] dataBytes = new byte[1024];
    private static byte[] requestBytes = new byte[256];
    private static ByteBuffer dataBuffer = ByteBuffer.wrap(dataBytes);
    private static ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);

    // Fields set by the constructor
    private static int chatRelayPort;
    private static String gameServerIP;
    private static String gameServerPort;
    private static String chatRelayPassword;
    private static InetAddress chatRelayIP;
    private static Handler handler;

    /**
     * Construct a new instance of the Chat class. <p> This class implements a CheckValve Chat Relay client. </p> <p>
     * 
     * @param crIP The URL or IP address of the Chat Relay
     * @param crPort The client listen port of the Chat Relay
     * @param crPassword The password for the Chat Relay
     * @param gsIP The URL or IP address of the game server from which you want chat messages
     * @param gsPort The listen port of the game server from which you want chat messages
     * @param h The handler to use </p> <p>
     * @throws UnknownHostException </p>
     */
    public Chat( String crIP, String crPort, String crPassword, String gsIP, String gsPort, Handler h )
            throws UnknownHostException
    {
        chatRelayIP = InetAddress.getByName(crIP);
        chatRelayPort = Integer.parseInt(crPort);
        chatRelayPassword = crPassword;
        gameServerIP = gsIP;
        gameServerPort = gsPort;
        handler = h;
    }

    public void run()
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        try
        {
            runChatRelayClient();
            handler.sendEmptyMessage(255);
        }
        catch( Exception e )
        {
            StackTraceElement[] ste = e.getStackTrace();

            Log.i(TAG, this.toString() + " caught an exception: " + e.toString());
            Log.i(TAG, "Stack trace:");

            for( int i = 0; i < ste.length; i++ )
                Log.i(TAG, "    " + ste[i].toString());

            Log.i(TAG, "Chat Relay client thread is shutting down.");
            return;
        }
        finally
        {
            if( s != null )
            {
                if( !s.isClosed() )
                {
                    try
                    {
                        s.close();
                        Log.i(TAG, "The socket has been closed.");
                    }
                    catch( IOException ioe )
                    {
                        Log.w(TAG, "Caught an exception while shutting down the socket:");
                        Log.w(TAG, ioe.toString());

                        StackTraceElement[] ste = ioe.getStackTrace();

                        for( int i = 0; i < ste.length; i++ )
                            Log.e(TAG, "    " + ste[i].toString());
                    }
                }
            }
        }
    }

    /**
     * Establishes a client connection to the Chat Relay and then listens for incoming data. <p> <b>This method should
     * not be explicitly called. It is started when the <tt>start()</tt> method is called on the Chat object or its
     * thread.</b>
     * 
     * @throws Exception
     */
    public void runChatRelayClient() throws Exception
    {
        final int PACKET_HEADER = 0xFFFFFFFF;
        final byte PTYPE_IDENTITY_STRING = (byte)0x00;
        final byte PTYPE_HEARTBEAT = (byte)0x01;
        final byte PTYPE_CONNECTION_REQUEST = (byte)0x02;
        final byte PTYPE_CONNECTION_FAILURE = (byte)0x03;
        final byte PTYPE_CONNECTION_SUCCESS = (byte)0x04;
        final byte PTYPE_MESSAGE_DATA = (byte)0x05;
        
        int i = 0;
        int pos = 0;
        int end = 0;

        dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.clear();

        requestBuffer.order(ByteOrder.LITTLE_ENDIAN);
        requestBuffer.clear();

        passwordString = "P ";

        if( chatRelayPassword.length() > 0 ) passwordString += chatRelayPassword;

        // Build the connection request packet
        requestBuffer.putInt(PACKET_HEADER);
        requestBuffer.put(PTYPE_CONNECTION_REQUEST);
        requestBuffer.putShort((short)(passwordString.length() + gameServerIP.length() + gameServerPort.length() + 3));
        requestBuffer.put(passwordString.getBytes("UTF-8")).put((byte)0);
        requestBuffer.put(gameServerIP.getBytes("UTF-8")).put((byte)0);
        requestBuffer.put(gameServerPort.getBytes("UTF-8")).put((byte)0);
        requestBuffer.flip();

        try
        {
            //s = new Socket(chatRelayIP, chatRelayPort);
            s = new Socket();
            s.setSoTimeout(2000);
            s.setSendBufferSize(1024);
            s.setReceiveBufferSize(1024);
            s.connect(new InetSocketAddress(chatRelayIP, chatRelayPort), 2000);
            in = s.getInputStream();
            out = s.getOutputStream();

            dataBuffer.clear();

            Log.d(TAG, "Waiting for server identity string.");

            // Get the first 5 bytes of the packet data (header and packet type)
            if( (in.read(dataBytes, 0, 5)) == -1 ) return;

            Log.d(TAG, "Received a packet.");

            // Make sure the header is valid
            if( (packetHeader = dataBuffer.getInt()) != PACKET_HEADER )
            {
                Log.w(TAG, "Rejecting packet: invalid header 0x"
                        + String.format("%s", Integer.toHexString(packetHeader)) + ".");
                handler.sendEmptyMessage(-1);
                return;
            }

            // Get the packet type
            responseType = dataBuffer.get();

            if( responseType == PTYPE_IDENTITY_STRING )
            {
                Log.d(TAG, "Received server identity string.");

                responseMessage = new StringBuilder();

                // Get the content length
                if( (in.read(dataBytes, dataBuffer.position(), 2)) == -1 ) return;

                contentLength = dataBuffer.getShort();
                Log.d(TAG, "Content length is " + contentLength + " bytes.");

                // Make sure the content length is valid
                if( contentLength < 1 || contentLength > 1024 )
                {
                    Log.w(TAG, "Packet contained an invalid content length (" + contentLength + ")");
                    handler.sendEmptyMessage(-1);
                    return;
                }

                // Read the rest of the packet data
                if( (in.read(dataBytes, dataBuffer.position(), contentLength)) == -1 ) return;

                pos = dataBuffer.position();
                end = pos + contentLength;

                for( i = pos; i < end; i++ )
                    responseMessage.append((char)dataBuffer.get());

                Log.i(TAG, "Server identity string is " + responseMessage.toString().trim());

                out.write(requestBuffer.array(), requestBuffer.position(), requestBuffer.limit());
                out.flush();

                s.setSoTimeout(60000);
            }
            else
            {
                Log.w(TAG, "Unexpected packet type 0x" + String.format("%s", Byte.toString(responseType)) + ".");
                handler.sendEmptyMessage(-1);
                return;
            }
        }
        catch( Exception e )
        {
            Log.w(TAG, "Caught an exception:");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( i = 0; i < ste.length; i++ )
                Log.e(TAG, ste[i].toString());

            handler.sendEmptyMessage(-1);
            return;
        }

        if( !s.isConnected() )
        {
            handler.sendEmptyMessage(-1);
            return;
        }

        for( ;; )
        {
            dataBuffer.clear();

            try
            {
                Log.d(TAG, "Waiting for the next packet.");

                // Get the first 5 bytes of the packet data (header and packet type)
                if( (in.read(dataBytes, 0, 5)) == -1 ) return;

                Log.d(TAG, "Received a packet.");
            }
            catch( SocketTimeoutException ste )
            {
                Log.w(TAG, "Timed out while waiting for next packet.");
                continue;
            }

            // Make sure the header is valid
            if( (packetHeader = dataBuffer.getInt()) != PACKET_HEADER )
            {
                Log.w(TAG, "Rejecting packet: invalid header 0x"
                        + String.format("%s", Integer.toHexString(packetHeader)) + ".");
                continue;
            }

            // Get the packet type
            responseType = dataBuffer.get();

            Log.d(TAG, "Packet type is 0x" + String.format("%s", Byte.toString(responseType)) + ".");

            // No need to do anything if this is a heartbeat
            if( responseType == PTYPE_HEARTBEAT )
            {
                handler.sendEmptyMessage(0);
                continue;
            }

            responseMessage = new StringBuilder();

            try
            {
                // Get the content length
                if( (in.read(dataBytes, dataBuffer.position(), 2)) == -1 ) return;

                contentLength = dataBuffer.getShort();
                Log.d(TAG, "Content length is " + contentLength + " bytes.");
            }
            catch( SocketTimeoutException ste )
            {
                Log.w(TAG, "Timed out while reading content length.");
                continue;
            }

            // Make sure the content length is valid
            if( contentLength < 1 || contentLength > 1024 ) continue;

            try
            {
                // Read the rest of the packet data
                if( (in.read(dataBytes, dataBuffer.position(), contentLength)) == -1 ) return;
            }
            catch( SocketTimeoutException ste )
            {
                Log.w(TAG, "Timed out while reading packet data.");
                continue;
            }

            Log.d(TAG, dataBuffer.toString());

            switch( responseType )
            {
                case PTYPE_CONNECTION_SUCCESS:
                    Log.i(TAG, "Connected to " + chatRelayIP.getHostAddress() + ":" + chatRelayPort + ".");
                    handler.sendEmptyMessage(4);
                    break;

                case PTYPE_CONNECTION_FAILURE:
                    pos = dataBuffer.position();
                    end = pos + contentLength;

                    for( i = pos; i < end; i++ )
                        responseMessage.append((char)dataBuffer.get());

                    String error = responseMessage.substring(2).trim();
                    msg = Message.obtain(handler, 3, error);
                    handler.sendMessage(msg);

                    Log.i(TAG, "Connection refused by Chat Relay server: " + error);
                    break;

                case PTYPE_MESSAGE_DATA:
                    protocolVersion = dataBuffer.get();
                    serverTimestamp = dataBuffer.getInt();
                    sayTeam = dataBuffer.get();

                    pos = dataBuffer.position();
                    end = pos + contentLength - 6;

                    for( i = pos; i < end; i++ )
                        responseMessage.append((char)dataBuffer.get());

                    fields = new String(responseMessage.toString().getBytes("UTF-8")).split("\u0000");

                    chatMsg = new ChatMessage(protocolVersion, sayTeam, serverTimestamp, fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);

                    msg = Message.obtain(handler, 5, chatMsg);
                    handler.sendMessage(msg);
                    break;

                default:
                    Log.w(TAG, "Unknown packet type, re-sending request.");
                    out.write(requestBuffer.array(), 0, requestBuffer.position());
                    out.flush();
                    break;
            }
        }
    }

    /**
     * Shuts down the Chat Relay client. <p> This method closes the network socket and then calls <tt>interrupt()<tt> on
     * the Chat object's thread. </p>
     */
    public void shutDown()
    {
        try
        {
            Log.d(TAG, this.toString() + ": Shutdown was requested.");

            if( s != null )
            {
                if( !s.isClosed() )
                {
                    Log.d(TAG, this.toString() + ": Closing socket " + s.toString() + ".");
                    s.close();
                }
            }
        }
        catch( Exception e )
        {
            StackTraceElement[] ste = e.getStackTrace();

            Log.w(TAG, "Caught an exception while closing socket: " + e.toString());
            Log.w(TAG, "Stack trace:");

            for( int i = 0; i < ste.length; i++ )
                Log.e(TAG, "    " + ste[i].toString());

            handler.sendEmptyMessage(-2);
        }

        Log.d(TAG, this.toString() + ": Calling interrupt() on " + Thread.currentThread().toString());
        Thread.currentThread().interrupt();
    }
}