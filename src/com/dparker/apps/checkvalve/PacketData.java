/*
 * Copyright 2010-2016 by David A. Parker <parker.david.a@gmail.com>
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.util.Log;

/*
 * Define the PacketData class
 */
public class PacketData {
    private static final String TAG = PacketData.class.getSimpleName();
    private ByteBuffer byteBuffer;
    
    /**
     * Construct a new instance of the PacketData class.
     * <br><br>
     * @param data The byte array containing the packet data
     */
    public PacketData( byte[] data ) {
        this.byteBuffer = ByteBuffer.wrap(data);
        this.byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Get the current byte in the packet data and increase the position by 1.
     * <br><br>
     * @return The next byte in the packet data
     */
    public byte getByte() {
        return byteBuffer.get();
    }
    
    /**
     * Get the byte at the specified index within the packet data and do not change the current position.
     * <br><br>
     * @param index The index of the byte to be read
     * @return The byte located at the specified index
     */
    public byte getByteAt(int index) {
        return byteBuffer.get(index);
    }
    
    /**
     * Returns the int at the current position and increases the position by 4.
     * <br><br>
     * @return The next 4 bytes of packet data composed into an int
     */
    public int getInt() {
        return byteBuffer.getInt();
    }
    
    /**
     * Returns the int starting at the specified index does not change the current position.
     * <br><br>
     * @param index The index at which to begin the 4-byte int
     * @return The 4 bytes of packet data at the specified index composed into an int
     */
    public int getIntAt(int index) {
        return byteBuffer.getInt(index);
    }
    
    /**
     * Returns the float at the current position and increases the position by 4.
     * <br><br>
     * @return The next 4 bytes of packet data composed into a float
     */
    public float getFloat() {
        return byteBuffer.getFloat();
    }
    
    /**
     * Returns the float starting at the specified index and does not change the current position.
     * <br><br>
     * @param index The index at which to begin the 4-byte float
     * @return The 4 bytes of packet data at the specified index composed into a float
     */
    public float getFloatAt(int index) {
        return byteBuffer.getFloat(index);
    }
    
    /**
     * Returns the String beginning at the current position using the default character set.  
     * The position is increased to the first byte after the end of the String.
     * <br><br>
     * @return The String beginning at the current position
     */
    public String getString() {
        int pos = byteBuffer.position();
        int len = 0;
        while( byteBuffer.get(pos++) != (byte)0x00 ) len++;
        byte[] tmpArray = new byte[len];
        byteBuffer.get(tmpArray, 0, tmpArray.length);
        byteBuffer.get();
        
        String str = new String(tmpArray);
        return str;
    }
    
    /**
     * Returns the String beginning at the specified index using the default character set 
     * and does not change the current position.
     * <br><br>
     * @param index The index at which to begin reading the String
     * @return The String beginning at the specified index
     */
    public String getStringAt(int index) {
        int oldPos = byteBuffer.position();
        byteBuffer.position(index);
        
        int pos = byteBuffer.position();
        int len = 0;
        while( byteBuffer.get(pos++) != (byte)0x00 ) len++;
        byte[] tmpArray = new byte[len];
        byteBuffer.get(tmpArray, 0, tmpArray.length);
        byteBuffer.position(oldPos);
        
        String str = new String(tmpArray);
        return str;
    }
    
    /**
     * Returns the String beginning at the current position using the specified character set.  
     * The position is increased to the first byte after the end of the String.
     * <br><br>
     * @param characterSet The name of the character set to use
     * @return The String beginning at the current position, or <tt>null</tt> if an exception occurs
     */
    public String getString(String characterSet) {
        int pos = byteBuffer.position();
        int len = 0;
        while( byteBuffer.get(pos++) != (byte)0x00 ) len++;
        byte[] tmpArray = new byte[len];
        byteBuffer.get(tmpArray, 0, tmpArray.length);
        byteBuffer.get();
        
        try {
            String str = new String(tmpArray, characterSet);    
            return str;
        }
        catch( Exception e ) {
            Log.w(TAG, "getString(): Caught an exception:", e);
            return null;
        }
    }
    
    /**
     * Returns the String beginning at the specified index using the specified character set 
     * and does not change the current position.
     * <br><br>
     * @param index The index at which to begin reading the String
     * @param characterSet The name of the character set to use
     * @return The String beginning at the specified index, or <tt>null</tt> if an exception occurs
     */
    public String getStringAt(int index, String characterSet) {
        int oldPos = byteBuffer.position();
        byteBuffer.position(index);
        
        int pos = byteBuffer.position();
        int len = 0;
        while( byteBuffer.get(pos++) != (byte)0x00 ) len++;
        byte[] tmpArray = new byte[len];
        byteBuffer.get(tmpArray, 0, tmpArray.length);
        byteBuffer.position(oldPos);
        
        try {
            String str = new String(tmpArray, characterSet);    
            return str;
        }
        catch( Exception e ) {
            Log.w(TAG, "getStringAt(): Caught an exception:", e);
            return null;
        }
    }
    
    /**
     * Returns the String beginning at the current position using the UTF-8 character set.  
     * The position is increased to the first byte after the end of the String.
     * <br><br>
     * This method is equivalent to <tt>getString("UTF-8")</tt>.
     * <br><br>
     * @return The UTF-8 encoded String beginning at the current position, or <tt>null</tt> if an exception occurs
     */
    public String getUTF8String() {
        int pos = byteBuffer.position();
        int len = 0;
        while( byteBuffer.get(pos++) != (byte)0x00 ) len++;
        byte[] tmpArray = new byte[len];
        byteBuffer.get(tmpArray, 0, tmpArray.length);
        byteBuffer.get();
        
        try {
            String str = new String(tmpArray, "UTF-8");    
            return str;
        }
        catch( Exception e ) {
            Log.w(TAG, "getUTF8String(): Caught an exception:", e);
            return null;
        }
    }
    
    /**
     * Returns the String beginning at the specified index using the UTF-8 character set
     * and does not change the current position.
     * <br><br>
     * This method is equivalent to <tt>getStringAt(index, "UTF-8")</tt>.
     * <br><br>
     * @param index The index at which to begin reading the String
     * @return The UTF-8 encoded String beginning at the specified index, or <tt>null</tt> if an exception occurs
     */
    public String getUTF8StringAt(int index) {
        int oldPos = byteBuffer.position();
        byteBuffer.position(index);
        
        int pos = byteBuffer.position();
        int len = 0;
        while( byteBuffer.get(pos++) != (byte)0x00 ) len++;
        byte[] tmpArray = new byte[len];
        byteBuffer.get(tmpArray, 0, tmpArray.length);
        byteBuffer.position(oldPos);
        
        try {
            String str = new String(tmpArray, "UTF-8");    
            return str;
        }
        catch( Exception e ) {
            Log.w(TAG, "getUTF8StringAt(): Caught an exception:", e);
            return null;
        }
    }
    
    /**
     * Get the current position within the packet data.
     * <br><br>
     * @return The value of the buffer's current position
     */
    public int getPosition() {
        return byteBuffer.position();
    }
    
    /**
     * Sets the position within the packet data.
     * <br><br>
     * @param newPosition The new position; must not be negative or greater than the length of the packet data
     */
    public void setPosition(int newPosition) {
        byteBuffer.position(newPosition);
    }
    
    /**
     * Determine whether the packet data buffer has data remaining.
     * <br><br>
     * @return <tt>true</tt> if there is data remaining, <tt>false</tt> if the current position is at the end of the data.
     */
    public boolean hasRemaining() {
        return byteBuffer.hasRemaining();
    }
    
    /**
     * Get the number of bytes remaining in the packet data buffer.
     * <br><br>
     * @return The number of bytes remaining between the current position and the end of the packet data
     */
    public int remaining() {
        return byteBuffer.remaining();
    }
    
    /**
     * Skip the String beginning at the current position.  The position is increased to the first byte
     * after the end of the String.
     */
    public void skipString() {
        while( byteBuffer.hasRemaining() && (byteBuffer.get() != (byte)0x00) ) {}
    }
    
    /**
     * Move the position in the buffer forward.  The position is increased by <tt>num</tt>.
     * <br><br>
     * @param num The number of bytes to skip.
     */
    public void skip(int num) {
        byteBuffer.position(byteBuffer.position() + num);
    }
    
    /**
     * Get the byte order currently used by the packet data buffer.
     * <br><br>
     * @return A <tt>ByteOrder</tt> object representing the byte order used by the buffer
     */
    public ByteOrder getByteOrder() {
        return byteBuffer.order();
    }
    
    /**
     * Set the byte order used by the packet data buffer.
     * <br><br>
     * @param byteOrder The byte order to be used by the buffer
     */
    public void setByteOrder(ByteOrder byteOrder) {
        byteBuffer.order(byteOrder);
    }
}