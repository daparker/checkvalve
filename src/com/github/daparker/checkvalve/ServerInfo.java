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

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Define the ServerInfo class
 */
public class ServerInfo implements Parcelable {
    private String nickname;
    private String name;
    private String addr;
    private String game;
    private String version;
    private String map;
    private String tags;
    private int port;
    private int numPlayers;
    private int maxPlayers;
    private int listPos;
    private long rowId;
    private long ping;

    public ServerInfo() {}

    /**
     * Create a new ServerInfo object using information from an A2S_INFO response
     * 
     * @param nickname The server's nickname in CheckValve
     * @param name The server's name
     * @param addr The server's IP address
     * @param game The game which is running on the server
     * @param version The version of the game
     * @param map The current map
     * @param tags The server's tags
     * @param port The listen port of the server
     * @param numPlayers The number of players on the server
     * @param maxPlayers The max number of players the server will support
     */
    public ServerInfo( String nickname, String name, String addr, String game, String version, String map,
            String tags, int port, int numPlayers, int maxPlayers, int listPos, long rowId, long ping ) {
        this.nickname = nickname;
        this.name = name;
        this.addr = addr;
        this.game = game;
        this.version = version;
        this.map = map;
        this.tags = tags;
        this.port = port;
        this.numPlayers = numPlayers;
        this.maxPlayers = maxPlayers;
        this.listPos = listPos;
        this.rowId = rowId;
        this.ping = ping;
    }

    public void setNickname( String s ) {
        this.nickname = s;
    }
    
    public void setName( String s ) {
        this.name = s;
    }
    
    public void setAddress( String s ) {
        this.addr = s;
    }
    
    public void setGame( String s ) {
        this.game = s;
    }
    
    public void setVersion( String s ) {
        this.version = s;
    }
    
    public void setMap( String s ) {
        this.map = s;
    }
    
    public void setTags( String s ) {
        this.tags = s;
    }
    
    public void setPort( int i ) {
        this.port = i;
    }
    
    public void setNumPlayers( int i ) {
        this.numPlayers = i;
    }
    
    public void setListPos( int i ) {
        this.listPos = i;
    }
    
    public void setRowId( long l ) {
        this.rowId = l;
    }
    
    public void setMaxPlayers( int i ) {
        this.maxPlayers = i;
    }
    
    public void setPing( long l ) {
        this.ping = l;
    }
    
    public String getNickame() {
        return this.nickname;
    }
    
    public String getName() {
        return this.name;
    }

    public String getAddr() {
        return this.addr;
    }
    
    public String getGame() {
        return this.game;
    }

    public String getVersion() {
        return this.version;
    }

    public String getMap() {
        return this.map;
    }

    public String getTags() {
        return this.tags;
    }
    
    public int getPort() {
        return this.port;
    }

    public int getNumPlayers() {
        return this.numPlayers;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }
    
    public int getListPos() {
        return this.listPos;
    }
    
    public long getRowId() {
        return this.rowId;
    }
    
    public long getPing() {
        return this.ping;
    }
    
    public int describeContents() {
        return 0;
    }

    public void writeToParcel( Parcel dest, int flags ) {
        dest.writeString(this.nickname);
        dest.writeString(this.name);
        dest.writeString(this.addr);
        dest.writeString(this.game);
        dest.writeString(this.version);
        dest.writeString(this.map);
        dest.writeString(this.tags);
        dest.writeInt(this.port);
        dest.writeInt(this.numPlayers);
        dest.writeInt(this.maxPlayers);
        dest.writeInt(this.listPos);
        dest.writeLong(this.rowId);
        dest.writeLong(this.ping);
    }

    public static final Parcelable.Creator<ServerInfo> CREATOR = new Parcelable.Creator<ServerInfo>() {
        public ServerInfo createFromParcel( Parcel in ) {
            return new ServerInfo(in);
        }

        public ServerInfo[] newArray( int size ) {
            return new ServerInfo[size];
        }
    };

    private ServerInfo( Parcel in ) {
        nickname = in.readString();
        name = in.readString();
        addr = in.readString();
        game = in.readString();
        version = in.readString();
        map = in.readString();
        tags = in.readString();
        port = in.readInt();
        numPlayers = in.readInt();
        maxPlayers = in.readInt();
        listPos = in.readInt();
        rowId = in.readLong();
        ping = in.readLong();
    }
}