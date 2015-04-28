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

/*
 * Define the ServerRecord class
 */
public class ServerRecord
{
    private String name;
    private String rcon;
    private int port;
    private int timeout;
    private int listpos;
    private long rowId;

    /**
     * Construct a new instance of the ServerRecord class.
     * 
     * @param s The IP address or URL of this server
     * @param r The RCON password for this server
     * @param p The listen port of this server
     * @param t The query timeout for this server
     * @param l The position of this server within the server list
     * @param i The unique row ID of this server within the database
     */
    public ServerRecord( String s, String r, int p, int t, int l, long i )
    {
        this.name = s;
        this.rcon = r;
        this.port = p;
        this.timeout = t;
        this.listpos = l;
        this.rowId = i;
    }

    public String getServerName()
    {
        return this.name;
    }

    public String getServerRCONPassword()
    {
        return this.rcon;
    }

    public int getServerPort()
    {
        return this.port;
    }

    public int getServerTimeout()
    {
        return this.timeout;
    }

    public int getServerListPosition()
    {
        return this.listpos;
    }

    public long getServerRowID()
    {
        return this.rowId;
    }

    public void setServerName( String s )
    {
        this.name = s;
    }

    public void setServerRCONPassword( String r )
    {
        this.rcon = r;
    }

    public void setServerPort( int p )
    {
        this.port = p;
    }

    public void setServerTimeout( int t )
    {
        this.timeout = t;
    }

    public void setServerListPosition( int l )
    {
        this.listpos = l;
    }

    public void setServerRowID( long i )
    {
        this.rowId = i;
    }
}