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

package com.dparker.apps.checkvalve;

import android.util.Log;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/*
 * Define the PacketData class
 */
public class PacketFactory {
    private static final String TAG = PacketFactory.class.getSimpleName();

    public static DatagramPacket getPacket(byte packetType, byte[] packetData) {
        byte[] content = new byte[packetData.length + 5];
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(Values.INT_PACKET_HEADER);
        buffer.put(packetType);
        buffer.put(packetData);

        return new DatagramPacket(content, content.length);
    }

    public static DatagramPacket getPacket(byte packetType, byte[] packetData, byte[] extraData) {
        byte[] content = new byte[packetData.length + extraData.length + 5];
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(Values.INT_PACKET_HEADER);
        buffer.put(packetType);
        buffer.put(packetData);
        buffer.put(extraData);

        return new DatagramPacket(content, content.length);
    }
}