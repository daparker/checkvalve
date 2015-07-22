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
 * Define the PlayerRecord class
 */
public class PlayerRecord implements Parcelable {
    private String name;
    private String time;
    private long kills;
    private int index;

    /**
     * Construct a new instance of the PlayerRecord class.
     * 
     * @param name The player's name
     * @param time How long the player has been connected (HH:MM:SS)
     * @param kills The number of kills for the player
     * @param index The index number of the player in the A2S_PLAYER response
     */
    public PlayerRecord( String name, String time, long kills, int index ) {
        this.name = name;
        this.time = time;
        this.kills = kills;
        this.index = index;
    }

    public String getName() {
        return this.name;
    }

    public String getTime() {
        return this.time;
    }

    public long getKills() {
        return this.kills;
    }

    public int getIndex() {
        return this.index;
    }

    public void setName( String s ) {
        this.name = s;
    }

    public void setTime( String t ) {
        this.time = t;
    }

    public void setKills( long k ) {
        this.kills = k;
    }

    public void setIndex( int i ) {
        this.index = i;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel( Parcel dest, int flags ) {
        dest.writeString(this.name);
        dest.writeString(this.time);
        dest.writeLong(this.kills);
        dest.writeInt(this.index);
    }

    public static final Parcelable.Creator<PlayerRecord> CREATOR = new Parcelable.Creator<PlayerRecord>() {
        public PlayerRecord createFromParcel( Parcel in ) {
            return new PlayerRecord(in);
        }

        public PlayerRecord[] newArray( int size ) {
            return new PlayerRecord[size];
        }
    };

    private PlayerRecord( Parcel in ) {
        name = in.readString();
        time = in.readString();
        kills = in.readLong();
        index = in.readInt();
    }
}
