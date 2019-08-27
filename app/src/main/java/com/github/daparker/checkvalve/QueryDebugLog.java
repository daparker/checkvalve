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

package com.github.daparker.checkvalve;

import android.annotation.SuppressLint;
import android.os.Build;

/*
 * Define the QueryDebugLog class
 */
public class QueryDebugLog {
    private StringBuilder sb;

    /**
     * Construct a new instance of the QueryDebugLog class.
     */
    public QueryDebugLog() {
    	sb = new StringBuilder();
    	this.addDebugHeader();
    }

    /**
     * Adds a header with device and serial information to the debug log
     */
    @SuppressLint("NewApi")
    private void addDebugHeader() {
        String brand = Build.BRAND;
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        sb.append("Device: ");
        
        if( brand != null && brand.length() > 0 ) {
            sb.append(brand).append(" ");
        }
        
        if( manufacturer != null && manufacturer.length() > 0 ) {
            sb.append(manufacturer).append(" ");
        }
        
        if( model != null && model.length() > 0 ) {
            sb.append(model).append(" ");
        }
        
        sb.append('\n');
        
        if( Build.VERSION.SDK_INT > 8 ) {
            String serial = Build.SERIAL;
            
            if( serial != null && serial.length() > 0 ) {
                sb.append("Serial: ").append(serial).append('\n');
            }
        }
        
        sb.append('\n');
    }
    
    /**
     * Add a message line to the debug log.
     * 
     * @param m The message to be added to the log
     */
    public void addMessage(String m) {
        sb.append(m).append('\n');
    }

    /**
     * Get the <tt>StringBuilder</tt> containing the messages for this debug log.
     * @return The <tt>StringBuilder</tt> which contains the messages in this debug log.
     */
    public StringBuilder getStringBuilder() {
        return sb;
    }

    /**
     * Get the debug log messages as a String.
     * @return The debug log messages as a String.
     */
    public String getString() {
        return sb.toString();
    }
}