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

package com.github.daparker.checkvalve.backup;

import com.github.daparker.checkvalve.exceptions.InvalidBackupFileException;

public class SettingBackupRecord {
    private String type;
    private String id;
    private String val;

    public SettingBackupRecord() {}

    public SettingBackupRecord(String type, String id, String val) {
        this.type = type;
        this.id = id;
        this.val = val;
    }

    public String getType() {
        return type;
    }

    public String getID() {
        return id;
    }

    public String getValue() {
        return val;
    }

    public void setType(String s) throws InvalidBackupFileException {
        if( type != null ) {
            throw new InvalidBackupFileException();
        }
            
        type = s;
    }

    public void setID(String s) throws InvalidBackupFileException {
        if( id != null ) {
            throw new InvalidBackupFileException();
        }
            
        id = s;
    }

    public void setValue(String s) throws InvalidBackupFileException {
        if( val != null ) {
            throw new InvalidBackupFileException();
        }
            
        val = s;
    }

    public boolean isValid() {
        // Make sure every member attribute has a value
        if( type == null || id == null || val == null ) {
            return false;
        }
        
        if( type.length() == 0 || id.length() == 0 ) {
            return false;
        }
        
        if( type.equals("string") ) {
            // Good enough
            return true;
        }
        
        if( type.equals("bool") ) {
            // Make sure the value is a boolean string
            if( val.equals("true") || val.equals("false") ) {
                return true;
            }
        }
        
        if( type.equals("int") ) {
            // Make sure the value is really a number
            try {
                Integer.parseInt(val);
                return true;
            }
            catch( NumberFormatException nfe ) {
                return false;
            }
        }
        
        return false;
    }
}