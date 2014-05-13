/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2008-2009, Sebastian Staudt
 */

package com.github.koraktor.steamcondenser.steam.servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.packets.A2M_GET_SERVERS_BATCH2_Paket;
import com.github.koraktor.steamcondenser.steam.packets.M2A_SERVER_BATCH_Paket;
import com.github.koraktor.steamcondenser.steam.sockets.MasterServerSocket;

/**
 * A Steam master server
 * @author Sebastian Staudt
 */
public class MasterServer
{
    public static final InetSocketAddress GOLDSRC_MASTER_SERVER = new InetSocketAddress("hl1master.steampowered.com", 27010);
    public static final InetSocketAddress SOURCE_MASTER_SERVER = new InetSocketAddress("hl2master.steampowered.com", 27011);

    public static final byte REGION_US_EAST_COAST = 0x00;
    public static final byte REGION_US_WEST_COAST = 0x01;
    public static final byte REGION_SOUTH_AMERICA = 0x02;
    public static final byte REGION_EUROPE = 0x03;
    public static final byte REGION_ASIA = 0x04;
    public static final byte REGION_AUSTRALIA = 0x05;
    public static final byte REGION_MIDDLE_EAST = 0x06;
    public static final byte REGION_AFRICA = 0x07;
    public static final byte REGION_ALL = (byte)0xFF;

    private MasterServerSocket socket;

    public MasterServer(InetSocketAddress masterServer)
            throws IOException, UnknownHostException
    {
        this.socket = new MasterServerSocket(masterServer.getAddress(), masterServer.getPort());
    }

    public Vector<InetSocketAddress> getServers()
            throws IOException, SteamCondenserException, TimeoutException
    {
        return this.getServers(MasterServer.REGION_ALL, "");
    }

    public Vector<InetSocketAddress> getServers(byte regionCode, String filter)
            throws IOException, SteamCondenserException, TimeoutException
    {
        int failCount    = 0;
        boolean finished = false;
        int portNumber   = 0;
        String hostName  = "0.0.0.0";
        Vector<String> serverStringArray;
        Vector<InetSocketAddress> serverArray = new Vector<InetSocketAddress>();

        do {
            this.socket.send(new A2M_GET_SERVERS_BATCH2_Paket(regionCode, hostName + ":" + portNumber, filter));
            try {
                serverStringArray = ((M2A_SERVER_BATCH_Paket)this.socket.getReply()).getServers();

                for(String serverString : serverStringArray) {
                    hostName = serverString.substring(0, serverString.lastIndexOf(":"));
                    portNumber = Integer.valueOf(serverString.substring(serverString.lastIndexOf(":") + 1));

                    if(!hostName.equals("0.0.0.0") && portNumber != 0) {
                        serverArray.add(new InetSocketAddress(hostName, portNumber));
                    }
                    else {
                        finished = true;
                    }
                }
                failCount = 0;
            }
            catch(TimeoutException e) {
                failCount ++;
                if(failCount == 3) {
                    throw e;
                }
            }
        } while( ! finished);

        return serverArray;
    }
}
