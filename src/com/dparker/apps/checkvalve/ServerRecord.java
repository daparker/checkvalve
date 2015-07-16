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

package com.dparker.apps.checkvalve;

/*
 * Define the ServerRecord class
 */
public class ServerRecord
{
	private String server_name;
	private int server_port;
	private int server_timeout;
	
	public ServerRecord() {}
	
	public ServerRecord(String s, int p, int t)
	{
		this.server_name = s;
		this.server_port = p;
		this.server_timeout = t;
	}
	
	public String getServerName()
	{
		return this.server_name;
	}
	
	public int getServerPort()
	{
		return this.server_port;
	}
	
	public int getServerTimeout()
	{
		return this.server_timeout;
	}
	
	public void setServerName(String s)
	{
		this.server_name = s;
	}
	
	public void setServerPort(int p)
	{
		this.server_port = p;
	}
	
	public void setServerTimeout(int t)
	{
		this.server_timeout = t;
	}
}