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
 * Define the ServerInfo class
 */
public class ServerInfo {
    private String name;
    private String addr;
    private String version;
    private String map;
    private String tags;
    private int numPlayers;
    private int maxPlayers;

    public ServerInfo( String name, String addr, String version, String map, String tags, int numPlayers, int maxPlayers ) {
        this.name = name;
        this.addr = addr;
        this.version = version;
        this.map = map;
        this.tags = tags;
        this.numPlayers = numPlayers;
        this.maxPlayers = maxPlayers;
    }

    public String getName() {
        return this.name;
    }

    public String getAddr() {
        return this.addr;
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

    public int getNumPlayers() {
        return this.numPlayers;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }
}