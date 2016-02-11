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
 * Define the ServerRecord class
 */
public class QueryDebugLog {
    private StringBuilder sb;

    /**
     * Construct a new instance of the ServerRecord class.
     */
    public QueryDebugLog() {
    	sb = new StringBuilder();
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