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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

/*
 * Define the DatabaseProvider class
 */
public class DatabaseProvider
{
    private static final int DATABASE_VERSION = 4;

    private static final String TAG = DatabaseProvider.class.getSimpleName();

    private static final String DATABASE_NAME = "servers_db";
    private static final String TABLE_SERVERS = "servers";
    private static final String TABLE_SETTINGS = "settings";
    private static final String SERVERS_FIELD_ROWID = "row_id";
    private static final String SERVERS_FIELD_SERVER = "server";
    private static final String SERVERS_FIELD_PORT = "port";
    private static final String SERVERS_FIELD_TIMEOUT = "timeout";
    private static final String SERVERS_FIELD_LISTPOS = "list_position";
    private static final String SERVERS_FIELD_RCON = "rcon_password";
    private static final String SETTINGS_FIELD_ROWID = "row_id";
    private static final String SETTINGS_FIELD_RCON_WARN_UNSAFE = "rcon_warn_unsafe";
    private static final String SETTINGS_FIELD_RCON_SHOW_PASSWORDS = "rcon_show_passwords";
    private static final String SETTINGS_FIELD_RCON_SHOW_SUGGESTIONS = "rcon_show_suggestions";
    private static final String SETTINGS_FIELD_SHOW_SERVER_IP = "show_ip";
    private static final String SETTINGS_FIELD_SHOW_SERVER_MAP = "show_map";
    private static final String SETTINGS_FIELD_SHOW_SERVER_PLAYERS = "show_num_players";
    private static final String SETTINGS_FIELD_SHOW_SERVER_GAME = "show_game_info";
    private static final String SETTINGS_FIELD_SHOW_SERVER_TAGS = "show_tags";
    private static final String SETTINGS_FIELD_DEFAULT_QUERY_PORT = "default_query_port";
    private static final String SETTINGS_FIELD_DEFAULT_QUERY_TIMEOUT = "default_query_timeout";
    private static final String SETTINGS_FIELD_DEFAULT_RELAY_HOST = "default_relay_host";
    private static final String SETTINGS_FIELD_DEFAULT_RELAY_PORT = "default_relay_port";
    private static final String SETTINGS_FIELD_DEFAULT_RELAY_PASSWORD = "default_relay_password";

    private static final String CREATE_TABLE_SERVERS = "CREATE TABLE " + TABLE_SERVERS + "(" + SERVERS_FIELD_ROWID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SERVERS_FIELD_SERVER + " TEXT NOT NULL, " + SERVERS_FIELD_PORT
            + " TEXT NOT NULL, " + SERVERS_FIELD_TIMEOUT + " INTEGER NOT NULL, " + SERVERS_FIELD_LISTPOS
            + " INTEGER NOT NULL DEFAULT 0, " + SERVERS_FIELD_RCON + " TEXT NOT NULL DEFAULT ''" + ");";

    private static final String CREATE_TABLE_SETTINGS = "CREATE TABLE " + TABLE_SETTINGS + "(" + SETTINGS_FIELD_ROWID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SETTINGS_FIELD_RCON_WARN_UNSAFE
            + " INTEGER NOT NULL DEFAULT 1, " + SETTINGS_FIELD_RCON_SHOW_PASSWORDS + " INTEGER NOT NULL DEFAULT 0, "
            + SETTINGS_FIELD_RCON_SHOW_SUGGESTIONS + " INTEGER NOT NULL DEFAULT 1, " + SETTINGS_FIELD_SHOW_SERVER_IP
            + " INTEGER NOT NULL DEFAULT 1, " + SETTINGS_FIELD_SHOW_SERVER_MAP + " INTEGER NOT NULL DEFAULT 1, "
            + SETTINGS_FIELD_SHOW_SERVER_PLAYERS + " INTEGER NOT NULL DEFAULT 1, " + SETTINGS_FIELD_SHOW_SERVER_GAME
            + " INTEGER NOT NULL DEFAULT 1, " + SETTINGS_FIELD_SHOW_SERVER_TAGS + " INTEGER NOT NULL DEFAULT 1, "
            + SETTINGS_FIELD_DEFAULT_QUERY_PORT + " INTEGER NOT NULL DEFAULT 27015, "
            + SETTINGS_FIELD_DEFAULT_QUERY_TIMEOUT + " INTEGER NOT NULL DEFAULT 1, "
            + SETTINGS_FIELD_DEFAULT_RELAY_HOST + " TEXT NOT NULL DEFAULT '', " + SETTINGS_FIELD_DEFAULT_RELAY_PORT
            + " INTEGER NOT NULL DEFAULT 23456, " + SETTINGS_FIELD_DEFAULT_RELAY_PASSWORD + " TEXT NOT NULL DEFAULT ''"
            + ");";

    private Context context;
    private DatabaseHelper helper;
    private SQLiteDatabase db;

    /**
     * Construct a new instance of the DatabaseProvider class. <p> This class provides persistent access to the
     * application's database. </p> <p>
     * 
     * @param c The context to use </p>
     */
    public DatabaseProvider( Context c )
    {
        this.context = c;
        helper = new DatabaseHelper(context);
    }

    /**
     * Helper class to simplify the creation and upgrading of the application's database. <p> This class extends
     * <tt>SQLiteOpenHelper</tt>. </p>
     */
    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper( Context context )
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
                values.put(SETTINGS_FIELD_RCON_WARN_UNSAFE, 1);
                values.put(SETTINGS_FIELD_RCON_SHOW_PASSWORDS, 1);
                values.put(SETTINGS_FIELD_RCON_SHOW_SUGGESTIONS, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_IP, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_MAP, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_PLAYERS, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_GAME, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_TAGS, 1);
                values.put(SETTINGS_FIELD_DEFAULT_QUERY_PORT, 27015);
                values.put(SETTINGS_FIELD_DEFAULT_QUERY_TIMEOUT, 1);
                values.put(SETTINGS_FIELD_DEFAULT_RELAY_HOST, "");
                values.put(SETTINGS_FIELD_DEFAULT_RELAY_PORT, 23456);
                values.put(SETTINGS_FIELD_DEFAULT_RELAY_PASSWORD, "");

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

            try
            {
                Log.i(TAG, "Creating table " + TABLE_SETTINGS);
                db.execSQL(CREATE_TABLE_SETTINGS);

                ContentValues values = new ContentValues();

                Log.i(TAG, "Assembling default values in ContentValues object " + values.toString());
                values.put(SETTINGS_FIELD_RCON_WARN_UNSAFE, 1);
                values.put(SETTINGS_FIELD_RCON_SHOW_PASSWORDS, 1);
                values.put(SETTINGS_FIELD_RCON_SHOW_SUGGESTIONS, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_IP, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_MAP, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_PLAYERS, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_GAME, 1);
                values.put(SETTINGS_FIELD_SHOW_SERVER_TAGS, 1);
                values.put(SETTINGS_FIELD_DEFAULT_QUERY_PORT, 27015);
                values.put(SETTINGS_FIELD_DEFAULT_QUERY_TIMEOUT, 1);
                values.put(SETTINGS_FIELD_DEFAULT_RELAY_HOST, "");
                values.put(SETTINGS_FIELD_DEFAULT_RELAY_PORT, 23456);
                values.put(SETTINGS_FIELD_DEFAULT_RELAY_PASSWORD, "");

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
    }

    /**
     * Opens the database for reading and writing <p>
     * 
     * @return A <tt>DatabaseProvider</tt> object. </p> <p>
     * @throws SQLException </p>
     */
    public DatabaseProvider open() throws SQLException
    {
        db = helper.getWritableDatabase();
        return this;
    }

    /**
     * Closes the database.
     */
    public void close()
    {
        helper.close();
    }

    /**
     * Determines if the database is open or closed. <p>
     * 
     * @return A boolean value indicating whether or not the database is currently open. </p>
     */
    public boolean isOpen()
    {
        return db.isOpen();
    }

    /**
     * Adds a new server to the database. <p>
     * 
     * @param server The URL or IP address of the server
     * @param port The listen port of the server
     * @param timeout The query timeout for the server (in seconds)
     * @param password The RCON password for the server </p> <p>
     * @return The ID of the newly created database row. </p>
     */
    public long insertServer( String server, int port, int timeout, String password )
    {
        int pos = getLastPosition() + 1;

        ContentValues values = new ContentValues();

        values.put(SERVERS_FIELD_SERVER, server);
        values.put(SERVERS_FIELD_PORT, port);
        values.put(SERVERS_FIELD_TIMEOUT, timeout);
        values.put(SERVERS_FIELD_LISTPOS, pos);
        values.put(SERVERS_FIELD_RCON, password);

        long result = db.insert(TABLE_SERVERS, null, values);

        return result;
    }

    /**
     * Deletes the specified server from the database. <p>
     * 
     * @param rowId The database row which contains the server's data </p> <p>
     * @return A boolean value indicating whether or not the operation was successful. </p>
     */
    public boolean deleteServer( long rowId )
    {
        return db.delete(TABLE_SERVERS, SERVERS_FIELD_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Queries the database for all server information. <p>
     * 
     * @return A <tt>Cursor</tt> containing the results of the query. </p>
     */
    public Cursor getAllServers()
    {
        Cursor result = null;

        try
        {
            result = db.query(TABLE_SERVERS, new String[] { SERVERS_FIELD_ROWID, SERVERS_FIELD_SERVER,
                    SERVERS_FIELD_PORT, SERVERS_FIELD_TIMEOUT, SERVERS_FIELD_LISTPOS, SERVERS_FIELD_RCON }, null, null, null, null, SERVERS_FIELD_LISTPOS);
        }
        catch( Exception e )
        {
            Log.w(TAG, "Caught an exception while querying for all servers.");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( int i = 0; i < ste.length; i++ )
                Log.e(TAG, ste[i].toString());
        }

        return result;
    }

    /**
     * Queries the database for the specified server's data. <p>
     * 
     * @param rowId The database row which contains the server's data </p> <p>
     * @return A <tt>Cursor</tt> containing the results of the query. </p> <p>
     * @throws SQLException </p>
     */
    public Cursor getServer( long rowId ) throws SQLException
    {
        Cursor result = db.query(TABLE_SERVERS, new String[] { SERVERS_FIELD_ROWID, SERVERS_FIELD_SERVER,
                SERVERS_FIELD_PORT, SERVERS_FIELD_TIMEOUT, SERVERS_FIELD_LISTPOS, SERVERS_FIELD_RCON }, SERVERS_FIELD_ROWID
                + "=" + rowId, null, null, null, null);

        if( result != null ) result.moveToFirst();

        return result;
    }

    /**
     * Updates the specified server's information in the database. <p>
     * 
     * @param rowId The database row which contains the server's data
     * @param server The URL or IP address of the server
     * @param port The listen port of the server
     * @param timeout The query timeout for the server (in seconds)
     * @param password The RCON password for the server </p> <p>
     * @return A boolean value indicating whether or not the operation was successful. </p>
     */
    public boolean updateServer( long rowId, String server, int port, int timeout, String password )
    {
        ContentValues values = new ContentValues();

        values.put(SERVERS_FIELD_SERVER, server);
        values.put(SERVERS_FIELD_PORT, port);
        values.put(SERVERS_FIELD_TIMEOUT, timeout);
        values.put(SERVERS_FIELD_RCON, password);

        boolean result = db.update(TABLE_SERVERS, values, SERVERS_FIELD_ROWID + "=" + rowId, null) > 0;

        return result;
    }

    /**
     * Gets the last used position number in the database. <p>
     * 
     * @return The last used position number. </p>
     */
    public int getLastPosition()
    {
        int result = 0;

        Cursor cursor = db.query(TABLE_SERVERS, new String[] { SERVERS_FIELD_LISTPOS }, null, null, null, null, SERVERS_FIELD_LISTPOS
                + " DESC", "1");

        if( cursor.getCount() < 1 ) return 0;

        cursor.moveToFirst();
        result = cursor.getInt(0);
        cursor.close();

        return result;
    }

    /**
     * Moves the specified server up one position in the list. <p> If the server is already at position 0, then this
     * method simply returns <b>true</b> and takes no other action. </p> <p>
     * 
     * @param rowId The database row which contains the server's data </p> <p>
     * @return A boolean value indicating whether or not the operation was successful. </p>
     */
    public boolean moveServerUp( long rowId )
    {
        Cursor cursor;

        cursor = db.query(TABLE_SERVERS, new String[] { SERVERS_FIELD_LISTPOS }, SERVERS_FIELD_ROWID + "=" + rowId, null, null, null, null);

        cursor.moveToFirst();

        int oldPos = cursor.getInt(0);
        int newPos = oldPos - 1;

        cursor.close();

        if( oldPos == 0 ) return true;

        ContentValues a = new ContentValues();
        ContentValues b = new ContentValues();
        ContentValues c = new ContentValues();

        a.put(SERVERS_FIELD_LISTPOS, -1);
        b.put(SERVERS_FIELD_LISTPOS, oldPos);
        c.put(SERVERS_FIELD_LISTPOS, newPos);

        int r1 = db.update(TABLE_SERVERS, a, SERVERS_FIELD_LISTPOS + "=" + oldPos, null);
        int r2 = db.update(TABLE_SERVERS, b, SERVERS_FIELD_LISTPOS + "=" + newPos, null);
        int r3 = db.update(TABLE_SERVERS, c, SERVERS_FIELD_LISTPOS + "= -1", null);

        return ((r1 == 1) && (r2 == 1) && (r3 == 1));
    }

    /**
     * Move the specified server down one position in the list. <p> If the server is already at the last position, then
     * this method simply returns <b>true</b> and takes no other action. </p> <p>
     * 
     * @param rowId The database row which contains the server's data </p> <p>
     * @return A boolean value indicating whether or not the operation was successful. </p> <p>
     * @see com.github.daparker.checkvalve.DatabaseProvider#getLastPosition()
     */
    public boolean moveServerDown( long rowId )
    {
        Cursor cursor = db.query(TABLE_SERVERS, new String[] { SERVERS_FIELD_LISTPOS }, SERVERS_FIELD_ROWID + "="
                + rowId, null, null, null, null);

        cursor.moveToFirst();

        int oldPos = cursor.getInt(0);
        int newPos = oldPos + 1;

        cursor.close();

        if( oldPos == getLastPosition() ) return true;

        ContentValues a = new ContentValues();
        ContentValues b = new ContentValues();
        ContentValues c = new ContentValues();

        a.put(SERVERS_FIELD_LISTPOS, -1);
        b.put(SERVERS_FIELD_LISTPOS, oldPos);
        c.put(SERVERS_FIELD_LISTPOS, newPos);

        int r1 = db.update(TABLE_SERVERS, a, SERVERS_FIELD_LISTPOS + "=" + oldPos, null);
        int r2 = db.update(TABLE_SERVERS, b, SERVERS_FIELD_LISTPOS + "=" + newPos, null);
        int r3 = db.update(TABLE_SERVERS, c, SERVERS_FIELD_LISTPOS + "= -1", null);

        return ((r1 == 1) && (r2 == 1) && (r3 == 1));
    }

    public Cursor getSettingsAsCursor()
    {
        Cursor result = null;

        try
        {
            result = db.query(TABLE_SETTINGS, new String[] { SETTINGS_FIELD_RCON_WARN_UNSAFE,
                    SETTINGS_FIELD_RCON_SHOW_PASSWORDS, SETTINGS_FIELD_RCON_SHOW_SUGGESTIONS, SETTINGS_FIELD_SHOW_SERVER_IP, SETTINGS_FIELD_SHOW_SERVER_MAP,
                    SETTINGS_FIELD_SHOW_SERVER_PLAYERS, SETTINGS_FIELD_SHOW_SERVER_GAME,
                    SETTINGS_FIELD_SHOW_SERVER_TAGS, SETTINGS_FIELD_DEFAULT_QUERY_PORT,
                    SETTINGS_FIELD_DEFAULT_QUERY_TIMEOUT, SETTINGS_FIELD_DEFAULT_RELAY_HOST,
                    SETTINGS_FIELD_DEFAULT_RELAY_PORT, SETTINGS_FIELD_DEFAULT_RELAY_PASSWORD }, null, null, null, null, SETTINGS_FIELD_ROWID);
        }
        catch( Exception e )
        {
            Log.w(TAG, "Caught an exception while querying for all settings.");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( int i = 0; i < ste.length; i++ )
                Log.e(TAG, ste[i].toString());
        }

        Log.i(TAG, "getSettingsAsCursor(): Returning Cursor " + result.toString());
        return result;
    }

    public Bundle getSettingsAsBundle()
    {
        String column = new String();
        Bundle result = new Bundle();
        Cursor cursor = this.getSettingsAsCursor();

        cursor.moveToFirst();

        for( int i = 0; i < cursor.getColumnCount(); i++ )
        {
            column = cursor.getColumnName(i);

            Log.i(TAG, "Cursor=" + cursor.toString() + "; column=" + i + "; name=" + column + "; null="
                    + cursor.isNull(i));

            if( column.equals(SETTINGS_FIELD_RCON_WARN_UNSAFE) )
                result.putBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_RCON_SHOW_PASSWORDS) )
                result.putBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_RCON_SHOW_SUGGESTIONS) )
                result.putBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_SHOW_SERVER_IP) )
                result.putBoolean(Values.SETTING_SHOW_SERVER_IP, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_SHOW_SERVER_GAME) )
                result.putBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_SHOW_SERVER_MAP) )
                result.putBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_SHOW_SERVER_PLAYERS) )
                result.putBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_SHOW_SERVER_TAGS) )
                result.putBoolean(Values.SETTING_SHOW_SERVER_TAGS, (cursor.getInt(i) == 1)?true:false);
            else if( column.equals(SETTINGS_FIELD_DEFAULT_QUERY_PORT) )
                result.putInt(Values.SETTING_DEFAULT_QUERY_PORT, cursor.getInt(i));
            else if( column.equals(SETTINGS_FIELD_DEFAULT_QUERY_TIMEOUT) )
                result.putInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, cursor.getInt(i));
            else if( column.equals(SETTINGS_FIELD_DEFAULT_RELAY_HOST) )
                result.putString(Values.SETTING_DEFAULT_RELAY_HOST, cursor.getString(i));
            else if( column.equals(SETTINGS_FIELD_DEFAULT_RELAY_PORT) )
                result.putInt(Values.SETTING_DEFAULT_RELAY_PORT, cursor.getInt(i));
            else if( column.equals(SETTINGS_FIELD_DEFAULT_RELAY_PASSWORD) )
                result.putString(Values.SETTING_DEFAULT_RELAY_PASSWORD, cursor.getString(i));
        }

        cursor.close();

        Log.i(TAG, "getSettingsAsBundle(): Returning Bundle " + result.toString());
        return result;
    }

    public boolean updateSettings( Bundle settings )
    {
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

        // Get int values from the Bundle
        int defaultQueryPort = settings.getInt(Values.SETTING_DEFAULT_QUERY_PORT, 27015);
        int defaultQueryTimeout = settings.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, 1);
        int defaultRelayPort = settings.getInt(Values.SETTING_DEFAULT_RELAY_PORT, 23456);

        // Get string values from the Bundle
        String defaultRelayHost = settings.getString(Values.SETTING_DEFAULT_RELAY_HOST);
        String defaultRelayPswd = settings.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD);

        values.put(SETTINGS_FIELD_RCON_WARN_UNSAFE, warnUnsafe);
        values.put(SETTINGS_FIELD_RCON_SHOW_PASSWORDS, showPwds);
        values.put(SETTINGS_FIELD_RCON_SHOW_SUGGESTIONS, showSuggest);
        values.put(SETTINGS_FIELD_SHOW_SERVER_IP, showIP);
        values.put(SETTINGS_FIELD_SHOW_SERVER_MAP, showGame);
        values.put(SETTINGS_FIELD_SHOW_SERVER_PLAYERS, showPlayers);
        values.put(SETTINGS_FIELD_SHOW_SERVER_GAME, showMap);
        values.put(SETTINGS_FIELD_SHOW_SERVER_TAGS, showTags);
        values.put(SETTINGS_FIELD_DEFAULT_QUERY_PORT, defaultQueryPort);
        values.put(SETTINGS_FIELD_DEFAULT_QUERY_TIMEOUT, defaultQueryTimeout);
        values.put(SETTINGS_FIELD_DEFAULT_RELAY_HOST, defaultRelayHost);
        values.put(SETTINGS_FIELD_DEFAULT_RELAY_PORT, defaultRelayPort);
        values.put(SETTINGS_FIELD_DEFAULT_RELAY_PASSWORD, defaultRelayPswd);

        Log.i(TAG, "Updating " + TABLE_SETTINGS + " with ContentValues " + values.toString());
        int result = db.update(TABLE_SETTINGS, values, null, null);
        Log.i(TAG, "Updated " + result + " row(s)");

        return (result == 0)?false:true;
    }
}