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

package com.github.daparker.checkvalve.backup;

import com.github.daparker.checkvalve.exceptions.InvalidBackupFileException;
import android.util.Base64;

public class ServerBackupRecord {
    private String url;
    private String rcon;
    private String name;
    private int port;
    private int timeout;
    private int listpos;
    private int enabled;

    public ServerBackupRecord() {
        this.port = -1;
        this.timeout = -1;
        this.listpos = -1;
        this.enabled = -1;
    }

    public ServerBackupRecord(String name, String url, int port, int timeout, int listpos, String rcon, int enabled) {
        this.name = name;
        this.url = url;
        this.port = port;
        this.timeout = timeout;
        this.listpos = listpos;
        this.rcon = rcon;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getURL() {
        return url;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getListPos() {
        return listpos;
    }

    public String getRCONPassword() {
        return rcon;
    }
    
    public int getEnabled() {
        return enabled;
    }

    public void setName(String s) throws InvalidBackupFileException {
        if( name != null ) {
            throw new InvalidBackupFileException();
        }
        
        name = s;
    }

    public void setURL(String s) throws InvalidBackupFileException {
        if( url != null ) {
            throw new InvalidBackupFileException();
        }
        
        url = s;
    }

    public void setPort(int i) throws InvalidBackupFileException {
        if( port >=0 ) {
            throw new InvalidBackupFileException();
        }
        
        port = i;
    }

    public void setTimeout(int i) throws InvalidBackupFileException {
        if( timeout >= 0 ) {
            throw new InvalidBackupFileException();
        }
        
        timeout = i;
    }

    public void setListPos(int i) throws InvalidBackupFileException {
        if( listpos >= 0 ) {
            throw new InvalidBackupFileException();
        }
        
        listpos = i;
    }

    public void setRCONPassword(String s) throws InvalidBackupFileException {
        if( rcon != null ) {
            throw new InvalidBackupFileException();
        }

        try {
            if( s.length() > 0 )
                rcon = new String(Base64.decode(s, Base64.DEFAULT), "UTF-8");
            else
                rcon = s;
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }
    
    public void setEnabled(int i) throws InvalidBackupFileException {
        if( enabled >= 0 ) {
            throw new InvalidBackupFileException();
        }
        
        if( i != 0 && i != 1 ) {
            throw new InvalidBackupFileException();
        }
        
        enabled = i;
    }
    
    public boolean isValid() {
        return (url != null && port >= 0 && timeout >= 0 && listpos >= 0);
    }
}