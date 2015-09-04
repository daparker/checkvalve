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

package com.github.daparker.checkvalve;

/*
 * Define the ChatMessage class
 */
public class ChatMessage {
    public byte protocolVersion;
    public byte sayTeamFlag;
    public int serverTimestamp;
    public String gameServerIP;
    public String gameServerPort;
    public String messageTimestamp;
    public String playerName;
    public String playerTeam;
    public String message;

    /**
     * Object which holds the data parsed from a chat message sent by the Chat Relay.
     * 
     * @param proto The Chat Relay protocol version used by the message packet
     * @param stflag Whether or not this is a say_team message (0x00 = say, 0x01 = say_team)
     * @param epoch A timestamp in Unix epoch format added by the Chat Relay server
     * @param gsip The IP address of the game server from which the message originated
     * @param gsport The port of the game server from which the message originated
     * @param tstamp The original timestamp included in the message
     * @param pname The name of the player who sent the message
     * @param pteam The team of the player who sent the message
     * @param msg The text of the message
     */
    public ChatMessage( byte proto, int epoch, byte stflag,
            String gsip, String gsport, String tstamp,
            String pname, String pteam, String msg ) {
        this.protocolVersion = proto;
        this.sayTeamFlag = stflag;
        this.serverTimestamp = epoch;
        this.gameServerIP = gsip.trim();
        this.gameServerPort = gsport.trim();
        this.messageTimestamp = tstamp.trim();
        this.playerName = pname.trim();
        this.playerTeam = pteam.trim();
        this.message = msg.trim();
    }
}