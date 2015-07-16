/**
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the new BSD License.
 *
 * Copyright (c) 2008-2009, Sebastian Staudt
 */

package com.github.koraktor.steamcondenser.steam.servers;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import com.github.koraktor.steamcondenser.exceptions.RCONNoAuthException;
import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.packets.rcon.RCONAuthRequestPacket;
import com.github.koraktor.steamcondenser.steam.packets.rcon.RCONAuthResponse;
import com.github.koraktor.steamcondenser.steam.packets.rcon.RCONExecRequestPacket;
import com.github.koraktor.steamcondenser.steam.packets.rcon.RCONExecResponsePacket;
import com.github.koraktor.steamcondenser.steam.packets.rcon.RCONPacket;
import com.github.koraktor.steamcondenser.steam.sockets.RCONSocket;
import com.github.koraktor.steamcondenser.steam.sockets.SourceSocket;

/**
 * A Source game server.
 *
 * @author Sebastian Staudt
 */
public class SourceServer extends GameServer {

	protected RCONSocket rconSocket;

	/**
	 * @param ipAddress The IP of the server to connect to
	 * @param portNumber The port number of the server
	 */
	public SourceServer(InetAddress ipAddress, int portNumber)
			throws IOException {
		super(portNumber);
		this.rconSocket = new RCONSocket(ipAddress, portNumber);
		this.socket = new SourceSocket(ipAddress, portNumber);
	}

	/**
	 * Authenticate via RCON
	 * @throws IOException
	 * @throws SteamCondenserException
	 * @throws TimeoutException
	 */
	public boolean rconAuth(String password)
			throws IOException, TimeoutException, SteamCondenserException {
		this.rconRequestId = new Random().nextInt();

		this.rconSocket.send(new RCONAuthRequestPacket(this.rconRequestId, password));
		RCONPacket reply = this.rconSocket.getReply();
		return (reply.getRequestId() == this.rconRequestId);
	}

	/**
	 * Execute a command on the server via RCON
	 * @throws IOException
	 * @throws SteamCondenserException
	 * @throws TimeoutException
	 */
	public String rconExec(String command)
			throws IOException, TimeoutException, SteamCondenserException {
		this.rconSocket.send(new RCONExecRequestPacket(this.rconRequestId, command));
		ArrayList<RCONExecResponsePacket> responsePackets = new ArrayList<RCONExecResponsePacket>();
		RCONPacket responsePacket;
		
		/*
		 * CheckValve
		 * 1.3.0
		 * Added a retry loop to fix a random timeout issue with RCON.
		 * We need to re-check for reply data sometimes if the reply
		 * was empty.
		 */
		int keepTrying = 1;		// Loop runs as long as this is 1
		int maxAttempts = 4;	// Max. number of times to check for a reply
		int readAttempt = 0;	// Number of the current attempt to read reply data
		
		while( keepTrying == 1 )
		{
			try {
				while(true) {
					responsePacket = this.rconSocket.getReply();
					//if(responsePacket instanceof RCONAuthResponse) {
					//	throw new RCONNoAuthException();
					//}
					if(responsePacket instanceof RCONAuthResponse) {
						responsePacket = this.rconSocket.getReply();
						if(responsePacket instanceof RCONAuthResponse) {
							throw new RCONNoAuthException();
						}
					}
					responsePackets.add((RCONExecResponsePacket) responsePacket);
				}
			} catch(TimeoutException e) {
				if(responsePackets.isEmpty()) {
					if( ++readAttempt == maxAttempts )
						// Throw a TimeoutException if we have checked (maxAttemps) times
						// and have received no data
						throw e;
				}
				else {
					// If we have received data then break the loop
					keepTrying = 0;
				}
			}
		}

		String response = new String();
		for(RCONExecResponsePacket packet : responsePackets) {
			response += packet.getResponse();
		}

		return response.trim();
	}

	/**
	 * Splits the player status obtained with "rcon status"
	 * @param playerStatus
	 * @return Split player data
	 */
	protected ArrayList<String> splitPlayerStatus(String playerStatus) {
		ArrayList<String> playerData = new ArrayList<String>(Arrays.asList(playerStatus.substring(1).split("\\s+")));
        playerData.remove(3);
        return playerData;
	}
}
