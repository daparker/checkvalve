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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

/*
 * Define the DatabaseProvider class
 */
public class DatabaseProvider extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 7;

    private static final String TAG = DatabaseProvider.class.getSimpleName();

    private static final String DATABASE_NAME = "servers_db";
    private static final String TABLE_SERVERS = "servers";
    private static final String TABLE_SETTINGS = "settings";
    private static final String SERVERS_ROWID = "row_id";
    private static final String SERVERS_SERVER = "server";
    private static final String SERVERS_PORT = "port";
    private static final String SERVERS_TIMEOUT = "timeout";
    private static final String SERVERS_LISTPOS = "list_position";
    private static final String SERVERS_RCON = "rcon_password";
    private static final String SETTINGS_ROWID = "row_id";
    private static final String SETTINGS_RCON_WARN_UNSAFE = "rcon_warn_unsafe";
    private static final String SETTINGS_RCON_SHOW_PASSWORDS = "rcon_show_passwords";
    private static final String SETTINGS_RCON_SHOW_SUGGESTIONS = "rcon_show_suggestions";
    private static final String SETTINGS_SHOW_SERVER_IP = "show_ip";
    private static final String SETTINGS_SHOW_SERVER_MAP = "show_map";
    private static final String SETTINGS_SHOW_SERVER_PLAYERS = "show_num_players";
    private static final String SETTINGS_SHOW_SERVER_GAME = "show_game_info";
    private static final String SETTINGS_SHOW_SERVER_TAGS = "show_tags";
    private static final String SETTINGS_DEFAULT_QUERY_PORT = "default_query_port";
    private static final String SETTINGS_DEFAULT_QUERY_TIMEOUT = "default_query_timeout";
    private static final String SETTINGS_DEFAULT_RELAY_HOST = "default_relay_host";
    private static final String SETTINGS_DEFAULT_RELAY_PORT = "default_relay_port";
    private static final String SETTINGS_DEFAULT_RELAY_PASSWORD = "default_relay_password";
    private static final String SETTINGS_VALIDATE_NEW_SERVERS = "validate_new_servers";

    private static final String CREATE_TABLE_SERVERS =
            "CREATE TABLE " + TABLE_SERVERS + "("
                    + SERVERS_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + SERVERS_SERVER + " TEXT NOT NULL, "
                    + SERVERS_PORT + " TEXT NOT NULL, "
                    + SERVERS_TIMEOUT + " INTEGER NOT NULL, "
                    + SERVERS_LISTPOS + " INTEGER NOT NULL DEFAULT 0, "
                    + SERVERS_RCON + " TEXT NOT NULL DEFAULT ''"
                    + ");";

    private static final String CREATE_TABLE_SETTINGS =
            "CREATE TABLE " + TABLE_SETTINGS + "("
                    + SETTINGS_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + SETTINGS_RCON_WARN_UNSAFE + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_RCON_SHOW_PASSWORDS + " INTEGER NOT NULL DEFAULT 0, "
                    + SETTINGS_RCON_SHOW_SUGGESTIONS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_IP + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_MAP + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_PLAYERS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_GAME + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_TAGS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_VALIDATE_NEW_SERVERS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_DEFAULT_QUERY_PORT + " INTEGER NOT NULL DEFAULT 27015, "
                    + SETTINGS_DEFAULT_QUERY_TIMEOUT + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_DEFAULT_RELAY_HOST + " TEXT NOT NULL DEFAULT '', "
                    + SETTINGS_DEFAULT_RELAY_PORT + " INTEGER NOT NULL DEFAULT 23456, "
                    + SETTINGS_DEFAULT_RELAY_PASSWORD + " TEXT NOT NULL DEFAULT ''"
                    + ");";

    private static final Object[] lock = new Object[0];

    /**
     * Construct a new instance of the DatabaseProvider class.
     * <p>
     * This class provides persistent access to the application's database.
     * </p>
     * <p>
     * 
     * @param c The context to use
     *        </p>
     */
    public DatabaseProvider( Context context )
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        /*
        synchronized(lock)
        {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT tbl_name FROM sqlite_master", null);
            
            c.moveToFirst();
            
            for( int i = 0; i < c.getCount(); i++ )
            {
                Log.d(TAG, "SQLite database table " + i + " is named " + c.getString(0) + " with schema:");
                
                Cursor x = db.rawQuery("SELECT * FROM " + c.getString(0) + " LIMIT 1", null);
                x.moveToFirst();
                
                for( int j = 0; j < x.getColumnCount(); j++ )
                    Log.d(TAG, "    [" + j + "][" + x.getType(j) + "] => '" + x.getColumnName(j) + "'");
                
                x.close();
                
                if( ! c.isLast() )
                    c.moveToNext();
            }
            
            c.close();
        }
        */
    }

    @Override
    public void onCreate( SQLiteDatabase db )
    {
        try
        {
            Log.i(TAG, "Creating table " + TABLE_SERVERS);
            db.execSQL(CREATE_TABLE_SERVERS);

            Log.i(TAG, "Creating table " + TABLE_SETTINGS);
            db.execSQL(CREATE_TABLE_SETTINGS);

            ContentValues values = new ContentValues();

            Log.i(TAG, "Assembling default values in ContentValues object " + values.toString());
            values.put(SETTINGS_RCON_WARN_UNSAFE, 1);
            values.put(SETTINGS_RCON_SHOW_PASSWORDS, 1);
            values.put(SETTINGS_RCON_SHOW_SUGGESTIONS, 1);
            values.put(SETTINGS_SHOW_SERVER_IP, 1);
            values.put(SETTINGS_SHOW_SERVER_MAP, 1);
            values.put(SETTINGS_SHOW_SERVER_PLAYERS, 1);
            values.put(SETTINGS_SHOW_SERVER_GAME, 1);
            values.put(SETTINGS_SHOW_SERVER_TAGS, 1);
            values.put(SETTINGS_VALIDATE_NEW_SERVERS, 1);
            values.put(SETTINGS_DEFAULT_QUERY_PORT, 27015);
            values.put(SETTINGS_DEFAULT_QUERY_TIMEOUT, 1);
            values.put(SETTINGS_DEFAULT_RELAY_HOST, "");
            values.put(SETTINGS_DEFAULT_RELAY_PORT, 23456);
            values.put(SETTINGS_DEFAULT_RELAY_PASSWORD, "");

            Log.i(TAG, "Inserting default values");
            db.insert(TABLE_SETTINGS, null, values);
        }
        catch( SQLiteException e )
        {
            Log.w(TAG, "Caught an exception while upgrading database:");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( int i = 0; i < ste.length; i++ )
                Log.e(TAG, "    " + ste[i].toString());
        }
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion )
    {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if( oldVersion == 3 )
        {
            try
            {
                Log.i(TAG, "Creating table " + TABLE_SETTINGS);
                db.execSQL(CREATE_TABLE_SETTINGS);

                ContentValues values = new ContentValues();

                Log.i(TAG, "Assembling default values in ContentValues object " + values.toString());
                values.put(SETTINGS_RCON_WARN_UNSAFE, 1);
                values.put(SETTINGS_RCON_SHOW_PASSWORDS, 1);
                values.put(SETTINGS_RCON_SHOW_SUGGESTIONS, 1);
                values.put(SETTINGS_SHOW_SERVER_IP, 1);
                values.put(SETTINGS_SHOW_SERVER_MAP, 1);
                values.put(SETTINGS_SHOW_SERVER_PLAYERS, 1);
                values.put(SETTINGS_SHOW_SERVER_GAME, 1);
                values.put(SETTINGS_SHOW_SERVER_TAGS, 1);
                values.put(SETTINGS_DEFAULT_QUERY_PORT, 27015);
                values.put(SETTINGS_DEFAULT_QUERY_TIMEOUT, 1);
                values.put(SETTINGS_DEFAULT_RELAY_HOST, "");
                values.put(SETTINGS_DEFAULT_RELAY_PORT, 23456);
                values.put(SETTINGS_DEFAULT_RELAY_PASSWORD, "");

                Log.i(TAG, "Inserting default values");
                db.insert(TABLE_SETTINGS, null, values);
            }
            catch( SQLiteException e )
            {
                Log.w(TAG, "Caught an exception while upgrading database:");
                Log.w(TAG, e.toString());

                StackTraceElement[] ste = e.getStackTrace();

                for( int i = 0; i < ste.length; i++ )
                    Log.e(TAG, "    " + ste[i].toString());
            }
        }

        if( oldVersion == 4 )
        {
            try
            {
                Log.i(TAG, "Updating table " + TABLE_SETTINGS);

                db.execSQL("ALTER TABLE " + TABLE_SETTINGS
                        + " ADD COLUMN " + SETTINGS_RCON_SHOW_SUGGESTIONS + " INTEGER NOT NULL DEFAULT 1");

                Log.i(TAG, "Successfully added column '" + SETTINGS_RCON_SHOW_SUGGESTIONS + "'");
            }
            catch( SQLiteException e )
            {
                Log.w(TAG, "Caught an exception while upgrading database:");
                Log.w(TAG, e.toString());

                StackTraceElement[] ste = e.getStackTrace();

                for( int i = 0; i < ste.length; i++ )
                    Log.e(TAG, "    " + ste[i].toString());
            }
        }

        if( oldVersion == 5 )
        {
            try
            {
                Log.i(TAG, "Updating table " + TABLE_SETTINGS);

                db.execSQL("ALTER TABLE " + TABLE_SETTINGS
                        + " ADD COLUMN " + SETTINGS_VALIDATE_NEW_SERVERS + " INTEGER NOT NULL DEFAULT 1");

                Log.i(TAG, "Successfully added column '" + SETTINGS_VALIDATE_NEW_SERVERS + "'");
            }
            catch( SQLiteException e )
            {
                Log.w(TAG, "Caught an exception while upgrading database:");
                Log.w(TAG, e.toString());

                StackTraceElement[] ste = e.getStackTrace();

                for( int i = 0; i < ste.length; i++ )
                    Log.e(TAG, "    " + ste[i].toString());
            }
        }

        if( oldVersion == 6 )
        {
            try
            {
                Log.i(TAG, "onUpgrade(): Updating table " + TABLE_SERVERS);

                Cursor c = db.query(
                        TABLE_SERVERS,
                        new String[] { SERVERS_ROWID, SERVERS_LISTPOS },
                        null,
                        null,
                        null,
                        null,
                        SERVERS_LISTPOS
                        );

                int count = c.getCount();

                for( int i = 0; i < count; i++ )
                {
                    c.moveToPosition(i);

                    long rowId = c.getLong(0);
                    int oldPos = c.getInt(1);
                    int newPos = (i + 1);

                    if( oldPos != newPos )
                    {
                        ContentValues values = new ContentValues();
                        values.put(SERVERS_LISTPOS, newPos);

                        Log.i(TAG, "onUpgrade(): Updating server " + rowId + "; changing list position from " + oldPos + " to " + newPos);

                        db.update(TABLE_SERVERS, values, SERVERS_ROWID + " = " + rowId, null);
                    }
                }

                c.close();

                Log.i(TAG, "onUpgrade(): Successfully updated server list positions");
            }
            catch( SQLiteException e )
            {
                Log.w(TAG, "Caught an exception while upgrading database:");
                Log.w(TAG, e.toString());

                StackTraceElement[] ste = e.getStackTrace();

                for( int i = 0; i < ste.length; i++ )
                    Log.e(TAG, "    " + ste[i].toString());
            }
        }
    }

    public long getServerCount()
    {
        long result = 0;

        synchronized( lock )
        {
            SQLiteDatabase db = this.getReadableDatabase();
            result = DatabaseUtils.queryNumEntries(db, TABLE_SERVERS);
        }

        return result;
    }

    /**
     * Adds a new server to the database.
     * <p>
     * 
     * @param server The URL or IP address of the server
     * @param port The listen port of the server
     * @param timeout The query timeout for the server (in seconds)
     * @param password The RCON password for the server
     *        </p>
     *        <p>
     * @return The ID of the newly created database row.
     *         </p>
     */
    public long insertServer( String server, int port, int timeout, String password )
    {
        long result;
        int pos = getLastPosition() + 1;

        ContentValues values = new ContentValues();

        values.put(SERVERS_SERVER, server);
        values.put(SERVERS_PORT, port);
        values.put(SERVERS_TIMEOUT, timeout);
        values.put(SERVERS_LISTPOS, pos);
        values.put(SERVERS_RCON, password);

        synchronized( lock )
        {
            SQLiteDatabase db = this.getWritableDatabase();
            result = db.insert(TABLE_SERVERS, null, values);
        }

        return result;
    }

    /**
     * Deletes the specified server from the database.
     * <p>
     * 
     * @param rowId The database row which contains the server's data
     *        </p>
     *        <p>
     * @return A boolean value indicating whether or not the operation was successful.
     *         </p>
     */
    public boolean deleteServer( long rowId )
    {
        boolean result = false;
        Cursor c;

        int listPos = getServerListPosition(rowId);

        if( listPos != -1 )
        {
            synchronized( lock )
            {
                SQLiteDatabase db = this.getWritableDatabase();

                result = db.delete(TABLE_SERVERS, SERVERS_ROWID + "=" + rowId, null) > 0;

                if( result == true )
                {
                    c = db.query(
                            TABLE_SERVERS,
                            new String[] { SERVERS_ROWID },
                            SERVERS_LISTPOS + " > " + listPos,
                            null,
                            null,
                            null,
                            SERVERS_LISTPOS
                            );

                    while( c.moveToNext() )
                        this.moveServerUp(c.getLong(0));

                    c.close();
                }
            }
        }

        return result;
    }

    public int getServerListPosition( long rowId )
    {
        int result = -1;
        Cursor c;

        synchronized( lock )
        {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[] { SERVERS_LISTPOS },
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null
                    );

            if( c != null )
            {
                if( c.getCount() > 0 )
                {
                    c.moveToFirst();
                    result = c.getInt(0);
                }

                c.close();
            }
        }

        return result;
    }

    /**
     * Queries the database for all server information.
     * <p>
     * 
     * @return A <tt>ServerRecord array</tt> containing the results of the query.
     *         </p>
     */
    public ServerRecord[] getAllServers()
    {
        Cursor c;

        ServerRecord[] result = null;

        synchronized( lock )
        {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[] {
                            SERVERS_ROWID,
                            SERVERS_SERVER,
                            SERVERS_PORT,
                            SERVERS_TIMEOUT,
                            SERVERS_LISTPOS,
                            SERVERS_RCON },
                    null,
                    null,
                    null,
                    null,
                    SERVERS_LISTPOS
                    );

            int count = c.getCount();

            result = new ServerRecord[count];

            for( int i = 0; i < count; i++ )
            {
                c.moveToPosition(i);

                result[i] = new ServerRecord(
                        c.getString(1),
                        c.getString(5),
                        c.getInt(2),
                        c.getInt(3),
                        c.getInt(4),
                        c.getLong(0));
            }

            c.close();
        }

        return result;
    }

    /**
     * Queries the database for the specified server's data.
     * <p>
     * 
     * @param rowId The database row which contains the server's data
     *        </p>
     *        <p>
     * @return A <tt>Cursor</tt> containing the results of the query.
     *         </p>
     *         <p>
     * @throws SQLException </p>
     */
    public ServerRecord getServer( long rowId )
    {
        ServerRecord result = null;

        Cursor c;

        synchronized( lock )
        {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[] {
                            SERVERS_ROWID,
                            SERVERS_SERVER,
                            SERVERS_PORT,
                            SERVERS_TIMEOUT,
                            SERVERS_LISTPOS,
                            SERVERS_RCON },
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null
                    );

            if( c != null )
            {
                if( c.getCount() > 0 )
                {
                    c.moveToFirst();

                    result = new ServerRecord(
                            c.getString(1),
                            c.getString(5),
                            c.getInt(2),
                            c.getInt(3),
                            c.getInt(4),
                            c.getLong(0)
                            );
                }

                c.close();
            }
        }

        return result;
    }

    /**
     * Updates the specified server's information in the database.
     * <p>
     * 
     * @param rowId The database row which contains the server's data
     * @param server The URL or IP address of the server
     * @param port The listen port of the server
     * @param timeout The query timeout for the server (in seconds)
     * @param password The RCON password for the server
     *        </p>
     *        <p>
     * @return A boolean value indicating whether or not the operation was successful.
     *         </p>
     */
    public boolean updateServer( long rowId, String server, int port, int timeout, String password )
    {
        boolean result;

        ContentValues values = new ContentValues();

        values.put(SERVERS_SERVER, server);
        values.put(SERVERS_PORT, port);
        values.put(SERVERS_TIMEOUT, timeout);
        values.put(SERVERS_RCON, password);

        synchronized( lock )
        {
            SQLiteDatabase db = this.getWritableDatabase();
            result = db.update(TABLE_SERVERS, values, SERVERS_ROWID + "=" + rowId, null) > 0;
        }

        return result;
    }

    /**
     * Gets the last used position number in the database.
     * <p>
     * 
     * @return The last used position number.
     *         </p>
     */
    public int getLastPosition()
    {
        int result = 0;

        synchronized( lock )
        {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor c = db.query(
                    TABLE_SERVERS,
                    new String[] { SERVERS_LISTPOS },
                    null,
                    null,
                    null,
                    null,
                    SERVERS_LISTPOS + " DESC", "1"
                    );

            if( c.getCount() > 0 )
            {
                c.moveToFirst();
                result = c.getInt(0);
            }

            c.close();
        }

        return result;
    }

    /**
     * Moves the specified server up one position in the list.
     * <p>
     * If the server is already at position 0, then this method simply returns <b>true</b> and takes no other action.
     * </p>
     * <p>
     * 
     * @param rowId The database row which contains the server's data
     *        </p>
     *        <p>
     * @return A boolean value indicating whether or not the operation was successful.
     *         </p>
     */
    public boolean moveServerUp( long rowId )
    {
        Cursor c;
        boolean result;
        int r1, r2, r3;

        synchronized( lock )
        {
            SQLiteDatabase db = this.getWritableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[] { SERVERS_LISTPOS },
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null
                    );

            if( c.getCount() == 0 )
            {
                result = false;
            }
            else
            {
                c.moveToFirst();

                int oldPos = c.getInt(0);
                int newPos = oldPos - 1;

                Log.d(TAG, "moveServerUp(): rowId=" + rowId + "; oldPos=" + oldPos + "; newPos=" + newPos);

                if( oldPos == 1 )
                {
                    Log.d(TAG, "moveServerUp(): Not moving server because oldPos is 1");
                    result = true;
                }
                else
                {
                    Log.d(TAG, "moveServerUp(): Moving server from position " + oldPos + " to " + newPos);

                    ContentValues cv1 = new ContentValues();
                    ContentValues cv2 = new ContentValues();
                    ContentValues cv3 = new ContentValues();

                    cv1.put(SERVERS_LISTPOS, -1);
                    cv2.put(SERVERS_LISTPOS, oldPos);
                    cv3.put(SERVERS_LISTPOS, newPos);

                    r1 = db.update(TABLE_SERVERS, cv1, SERVERS_LISTPOS + "=" + oldPos, null);
                    r2 = db.update(TABLE_SERVERS, cv2, SERVERS_LISTPOS + "=" + newPos, null);
                    r3 = db.update(TABLE_SERVERS, cv3, SERVERS_LISTPOS + "= -1", null);

                    Log.d(TAG, "moveServerUp(): r1=" + r1 + "; r2=" + r2 + "; r3=" + r3);

                    result = ((r1 == 1) && (r2 == 1) && (r3 == 1));
                }
            }

            c.close();
        }

        return result;
    }

    /**
     * Move the specified server down one position in the list.
     * <p>
     * If the server is already at the last position, then this method simply returns <b>true</b> and takes no other
     * action.
     * </p>
     * <p>
     * 
     * @param rowId The database row which contains the server's data
     *        </p>
     *        <p>
     * @return A boolean value indicating whether or not the operation was successful.
     *         </p>
     *         <p>
     * @see com.github.daparker.checkvalve.DatabaseProvider#getLastPosition()
     */
    public boolean moveServerDown( long rowId )
    {
        Cursor c;
        boolean result;
        int r1, r2, r3;

        synchronized( lock )
        {
            SQLiteDatabase db = this.getWritableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[] { SERVERS_LISTPOS },
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null
                    );

            if( c.getCount() == 0 )
            {
                result = false;
            }
            else
            {
                c.moveToFirst();

                int oldPos = c.getInt(0);
                int newPos = oldPos + 1;

                Log.d(TAG, "moveServerDown(): rowId=" + rowId + "; oldPos=" + oldPos + "; newPos=" + newPos);

                if( oldPos == getLastPosition() )
                {
                    Log.d(TAG, "moveServerDown(): Not moving server because oldPos is the last position");
                    result = true;
                }
                else
                {
                    Log.d(TAG, "moveServerDown(): Moving server from position " + oldPos + " to " + newPos);

                    ContentValues cv1 = new ContentValues();
                    ContentValues cv2 = new ContentValues();
                    ContentValues cv3 = new ContentValues();

                    cv1.put(SERVERS_LISTPOS, -1);
                    cv2.put(SERVERS_LISTPOS, oldPos);
                    cv3.put(SERVERS_LISTPOS, newPos);

                    r1 = db.update(TABLE_SERVERS, cv1, SERVERS_LISTPOS + "=" + oldPos, null);
                    r2 = db.update(TABLE_SERVERS, cv2, SERVERS_LISTPOS + "=" + newPos, null);
                    r3 = db.update(TABLE_SERVERS, cv3, SERVERS_LISTPOS + "= -1", null);

                    Log.d(TAG, "moveServerDown(): r1=" + r1 + "; r2=" + r2 + "; r3=" + r3);

                    result = ((r1 == 1) && (r2 == 1) && (r3 == 1));
                }
            }

            c.close();
        }

        return result;
    }

    /**
     * Get the current settings as a <tt>Bundle</tt>.
     * <p>
     * 
     * @return A <tt>Bundle</tt> which contains all values from the settings table.
     *         </p>
     */
    public Bundle getSettingsAsBundle()
    {
        Cursor c;
        String column = new String();
        Bundle result = new Bundle();

        String[] cols = new String[] {
                SETTINGS_RCON_WARN_UNSAFE,
                SETTINGS_RCON_SHOW_PASSWORDS,
                SETTINGS_RCON_SHOW_SUGGESTIONS,
                SETTINGS_SHOW_SERVER_IP,
                SETTINGS_SHOW_SERVER_MAP,
                SETTINGS_SHOW_SERVER_PLAYERS,
                SETTINGS_SHOW_SERVER_GAME,
                SETTINGS_SHOW_SERVER_TAGS,
                SETTINGS_VALIDATE_NEW_SERVERS,
                SETTINGS_DEFAULT_QUERY_PORT,
                SETTINGS_DEFAULT_QUERY_TIMEOUT,
                SETTINGS_DEFAULT_RELAY_HOST,
                SETTINGS_DEFAULT_RELAY_PORT,
                SETTINGS_DEFAULT_RELAY_PASSWORD };

        synchronized( lock )
        {
            SQLiteDatabase db = this.getReadableDatabase();
            c = db.query(TABLE_SETTINGS, cols, null, null, null, null, SETTINGS_ROWID);

            c.moveToFirst();

            for( int i = 0; i < c.getColumnCount(); i++ )
            {
                column = c.getColumnName(i);

                if( column.equals(SETTINGS_RCON_WARN_UNSAFE) )
                    result.putBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_RCON_SHOW_PASSWORDS) )
                    result.putBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_RCON_SHOW_SUGGESTIONS) )
                    result.putBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_SHOW_SERVER_IP) )
                    result.putBoolean(Values.SETTING_SHOW_SERVER_IP, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_SHOW_SERVER_GAME) )
                    result.putBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_SHOW_SERVER_MAP) )
                    result.putBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_SHOW_SERVER_PLAYERS) )
                    result.putBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_SHOW_SERVER_TAGS) )
                    result.putBoolean(Values.SETTING_SHOW_SERVER_TAGS, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_VALIDATE_NEW_SERVERS) )
                    result.putBoolean(Values.SETTING_VALIDATE_NEW_SERVERS, (c.getInt(i) == 1)?true:false);
                else if( column.equals(SETTINGS_DEFAULT_QUERY_PORT) )
                    result.putInt(Values.SETTING_DEFAULT_QUERY_PORT, c.getInt(i));
                else if( column.equals(SETTINGS_DEFAULT_QUERY_TIMEOUT) )
                    result.putInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, c.getInt(i));
                else if( column.equals(SETTINGS_DEFAULT_RELAY_HOST) )
                    result.putString(Values.SETTING_DEFAULT_RELAY_HOST, c.getString(i));
                else if( column.equals(SETTINGS_DEFAULT_RELAY_PORT) )
                    result.putInt(Values.SETTING_DEFAULT_RELAY_PORT, c.getInt(i));
                else if( column.equals(SETTINGS_DEFAULT_RELAY_PASSWORD) )
                    result.putString(Values.SETTING_DEFAULT_RELAY_PASSWORD, c.getString(i));
            }

            c.close();
        }

        Log.i(TAG, "getSettingsAsBundle(): Returning Bundle " + result.toString());
        return result;
    }

    /**
     * Update the application settings in the database.
     * <p>
     * 
     * @param settings A <tt>Bundle</tt> which contains the new settings values.
     *        </p>
     *        <p>
     * @return A boolean value indicating whether or not the update was successful.
     *         </p>
     */
    public boolean updateSettings( Bundle settings )
    {
        int result;
        ContentValues values = new ContentValues();

        // Get boolean values from the Bundle as integers (0 or 1)
        int warnUnsafe = (settings.getBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, true))?1:0;
        int showPwds = (settings.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, true))?1:0;
        int showSuggest = (settings.getBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, true))?1:0;
        int showIP = (settings.getBoolean(Values.SETTING_SHOW_SERVER_IP, true))?1:0;
        int showGame = (settings.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, true))?1:0;
        int showMap = (settings.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, true))?1:0;
        int showPlayers = (settings.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, true))?1:0;
        int showTags = (settings.getBoolean(Values.SETTING_SHOW_SERVER_TAGS, true))?1:0;
        int validate = (settings.getBoolean(Values.SETTING_VALIDATE_NEW_SERVERS, true))?1:0;

        // Get int values from the Bundle
        int defaultQueryPort = settings.getInt(Values.SETTING_DEFAULT_QUERY_PORT, 27015);
        int defaultQueryTimeout = settings.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, 1);
        int defaultRelayPort = settings.getInt(Values.SETTING_DEFAULT_RELAY_PORT, 23456);

        // Get string values from the Bundle
        String defaultRelayHost = settings.getString(Values.SETTING_DEFAULT_RELAY_HOST);
        String defaultRelayPswd = settings.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD);

        values.put(SETTINGS_RCON_WARN_UNSAFE, warnUnsafe);
        values.put(SETTINGS_RCON_SHOW_PASSWORDS, showPwds);
        values.put(SETTINGS_RCON_SHOW_SUGGESTIONS, showSuggest);
        values.put(SETTINGS_SHOW_SERVER_IP, showIP);
        values.put(SETTINGS_SHOW_SERVER_MAP, showGame);
        values.put(SETTINGS_SHOW_SERVER_PLAYERS, showPlayers);
        values.put(SETTINGS_SHOW_SERVER_GAME, showMap);
        values.put(SETTINGS_SHOW_SERVER_TAGS, showTags);
        values.put(SETTINGS_VALIDATE_NEW_SERVERS, validate);
        values.put(SETTINGS_DEFAULT_QUERY_PORT, defaultQueryPort);
        values.put(SETTINGS_DEFAULT_QUERY_TIMEOUT, defaultQueryTimeout);
        values.put(SETTINGS_DEFAULT_RELAY_HOST, defaultRelayHost);
        values.put(SETTINGS_DEFAULT_RELAY_PORT, defaultRelayPort);
        values.put(SETTINGS_DEFAULT_RELAY_PASSWORD, defaultRelayPswd);

        synchronized( lock )
        {
            SQLiteDatabase db = this.getWritableDatabase();

            Log.i(TAG, "Updating " + TABLE_SETTINGS + " with ContentValues " + values.toString());
            result = db.update(TABLE_SETTINGS, values, null, null);
            Log.i(TAG, "Updated " + result + " row(s)");
        }

        return (result == 0)?false:true;
    }
}