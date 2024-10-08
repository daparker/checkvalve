/*
 * Copyright 2010-2024 by David A. Parker <parker.david.a@gmail.com>
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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.dparker.apps.checkvalve.exceptions.InvalidDataTypeException;

import java.nio.charset.StandardCharsets;

/*
 * Define the DatabaseProvider class
 */
public class DatabaseProvider extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 11;

    private static final String TAG = DatabaseProvider.class.getSimpleName();

    public static final String DATABASE_NAME = "servers_db";
    public static final String TABLE_SERVERS = "servers";
    public static final String TABLE_SETTINGS = "settings";
    public static final String TABLE_RELAY_HOSTS = "relay_hosts";
    public static final String SERVERS_ROWID = "row_id";
    public static final String SERVERS_SERVER = "server";
    public static final String SERVERS_PORT = "port";
    public static final String SERVERS_TIMEOUT = "timeout";
    public static final String SERVERS_LISTPOS = "list_position";
    public static final String SERVERS_RCON = "rcon_password";
    public static final String SERVERS_NICKNAME = "nickname";
    public static final String SERVERS_ENABLED = "enabled";
    public static final String SETTINGS_ROWID = "row_id";
    public static final String SETTINGS_RCON_WARN_UNSAFE = "rcon_warn_unsafe";
    public static final String SETTINGS_RCON_SHOW_PASSWORDS = "rcon_show_passwords";
    public static final String SETTINGS_RCON_SHOW_SUGGESTIONS = "rcon_show_suggestions";
    public static final String SETTINGS_RCON_ENABLE_HISTORY = "rcon_enable_history";
    public static final String SETTINGS_RCON_VOLUME_BUTTONS = "rcon_volume_buttons";
    public static final String SETTINGS_RCON_DEFAULT_FONT_SIZE = "rcon_default_font_size";
    public static final String SETTINGS_RCON_INCLUDE_SM = "rcon_include_sm";
    public static final String SETTINGS_SHOW_SERVER_NAME = "show_name";
    public static final String SETTINGS_SHOW_SERVER_IP = "show_ip";
    public static final String SETTINGS_SHOW_SERVER_MAP = "show_map";
    public static final String SETTINGS_SHOW_SERVER_PLAYERS = "show_num_players";
    public static final String SETTINGS_SHOW_SERVER_GAME = "show_game_info";
    public static final String SETTINGS_SHOW_SERVER_TAGS = "show_tags";
    public static final String SETTINGS_SHOW_SERVER_PING = "show_ping";
    public static final String SETTINGS_USE_SERVER_NICKNAME = "show_nickname";
    public static final String SETTINGS_DEFAULT_QUERY_PORT = "default_query_port";
    public static final String SETTINGS_DEFAULT_QUERY_TIMEOUT = "default_query_timeout";
    public static final String SETTINGS_DEFAULT_RELAY_HOST = "default_relay_host";
    public static final String SETTINGS_DEFAULT_RELAY_PORT = "default_relay_port";
    public static final String SETTINGS_DEFAULT_RELAY_PASSWORD = "default_relay_password";
    public static final String SETTINGS_VALIDATE_NEW_SERVERS = "validate_new_servers";
    public static final String SETTINGS_BACKGROUND_QUERY_FREQUENCY = "background_query_frequency";
    public static final String SETTINGS_ENABLE_NOTIFICATION_LED = "enable_notification_led";
    public static final String SETTINGS_ENABLE_NOTIFICATION_SOUND = "enable_notification_sound";
    public static final String SETTINGS_ENABLE_NOTIFICATION_VIBRATE = "enable_notification_vibrate";
    public static final String SETTINGS_ENABLE_NOTIFICATIONS = "enable_notifications";
    public static final String RELAY_HOSTS_ROWID = "row_id";
    public static final String RELAY_HOSTS_HOST = "host";

    private static final String CREATE_TABLE_SERVERS =
            "CREATE TABLE " + TABLE_SERVERS + "("
                    + SERVERS_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + SERVERS_SERVER + " TEXT NOT NULL, "
                    + SERVERS_PORT + " TEXT NOT NULL, "
                    + SERVERS_TIMEOUT + " INTEGER NOT NULL, "
                    + SERVERS_LISTPOS + " INTEGER NOT NULL DEFAULT 0, "
                    + SERVERS_RCON + " TEXT NOT NULL DEFAULT '', "
                    + SERVERS_NICKNAME + " TEXT NOT NULL DEFAULT '', "
                    + SERVERS_ENABLED + " INT NOT NULL DEFAULT 1"
                    + ");";

    private static final String CREATE_TABLE_SETTINGS =
            "CREATE TABLE " + TABLE_SETTINGS + "("
                    + SETTINGS_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + SETTINGS_RCON_WARN_UNSAFE + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_RCON_SHOW_PASSWORDS + " INTEGER NOT NULL DEFAULT 0, "
                    + SETTINGS_RCON_SHOW_SUGGESTIONS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_RCON_ENABLE_HISTORY + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_RCON_VOLUME_BUTTONS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_RCON_DEFAULT_FONT_SIZE + " INTEGER NOT NULL DEFAULT 9, "
                    + SETTINGS_RCON_INCLUDE_SM + " INTEGER NOT NULL DEFAULT 0, "
                    + SETTINGS_SHOW_SERVER_NAME + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_IP + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_MAP + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_PLAYERS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_GAME + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_TAGS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_SHOW_SERVER_PING + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_USE_SERVER_NICKNAME + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_VALIDATE_NEW_SERVERS + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_DEFAULT_QUERY_PORT + " INTEGER NOT NULL DEFAULT 27015, "
                    + SETTINGS_DEFAULT_QUERY_TIMEOUT + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_DEFAULT_RELAY_HOST + " TEXT NOT NULL DEFAULT '', "
                    + SETTINGS_DEFAULT_RELAY_PORT + " INTEGER NOT NULL DEFAULT 23456, "
                    + SETTINGS_DEFAULT_RELAY_PASSWORD + " TEXT NOT NULL DEFAULT '', "
                    + SETTINGS_ENABLE_NOTIFICATION_LED + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_ENABLE_NOTIFICATION_SOUND + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_ENABLE_NOTIFICATION_VIBRATE + " INTEGER NOT NULL DEFAULT 1, "
                    + SETTINGS_ENABLE_NOTIFICATIONS + " INTEGER NOT NULL DEFAULT 0, "
                    + SETTINGS_BACKGROUND_QUERY_FREQUENCY + " INTEGER NOT NULL DEFAULT 10 "
                    + ");";

    private static final String CREATE_TABLE_RELAY_HOSTS =
            "CREATE TABLE " + TABLE_RELAY_HOSTS + "("
                    + RELAY_HOSTS_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + RELAY_HOSTS_HOST + " TEXT NOT NULL DEFAULT ''"
                    + ");";

    // Settings columns which contain boolean values
    private static final String[] boolCols = new String[]{
            SETTINGS_ENABLE_NOTIFICATION_LED,
            SETTINGS_ENABLE_NOTIFICATION_SOUND,
            SETTINGS_ENABLE_NOTIFICATION_VIBRATE,
            SETTINGS_ENABLE_NOTIFICATIONS,
            SETTINGS_RCON_WARN_UNSAFE,
            SETTINGS_RCON_SHOW_PASSWORDS,
            SETTINGS_RCON_SHOW_SUGGESTIONS,
            SETTINGS_RCON_ENABLE_HISTORY,
            SETTINGS_RCON_VOLUME_BUTTONS,
            SETTINGS_RCON_INCLUDE_SM,
            SETTINGS_SHOW_SERVER_NAME,
            SETTINGS_SHOW_SERVER_IP,
            SETTINGS_SHOW_SERVER_MAP,
            SETTINGS_SHOW_SERVER_PLAYERS,
            SETTINGS_SHOW_SERVER_GAME,
            SETTINGS_SHOW_SERVER_TAGS,
            SETTINGS_SHOW_SERVER_PING,
            SETTINGS_USE_SERVER_NICKNAME,
            SETTINGS_VALIDATE_NEW_SERVERS};

    // Settings columns which contain integer values
    private static final String[] intCols = new String[]{
            SETTINGS_RCON_DEFAULT_FONT_SIZE,
            SETTINGS_DEFAULT_QUERY_PORT,
            SETTINGS_DEFAULT_QUERY_TIMEOUT,
            SETTINGS_DEFAULT_RELAY_PORT,
            SETTINGS_BACKGROUND_QUERY_FREQUENCY};

    // Settings columns which contain string values
    private static final String[] stringCols = new String[]{
            SETTINGS_DEFAULT_RELAY_HOST,
            SETTINGS_DEFAULT_RELAY_PASSWORD};

    private static final Object[] lock = new Object[0];

    /**
     * Construct a new instance of the DatabaseProvider class.
     * This class provides persistent access to the application's database.
     *
     * @param context The context to use
     */
    public DatabaseProvider(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        /*
        synchronized(lock)
        {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT tbl_name FROM sqlite_master", null);
            
            c.moveToFirst();
            
            // Dump all table data to logcat for debugging
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
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.i(TAG, "Creating table " + TABLE_SERVERS);
            db.execSQL(CREATE_TABLE_SERVERS);

            Log.i(TAG, "Creating table " + TABLE_SETTINGS);
            db.execSQL(CREATE_TABLE_SETTINGS);

            ContentValues values = new ContentValues();
            values.put(SETTINGS_RCON_WARN_UNSAFE, 1);
            values.put(SETTINGS_RCON_SHOW_PASSWORDS, 1);
            values.put(SETTINGS_RCON_SHOW_SUGGESTIONS, 1);
            values.put(SETTINGS_RCON_ENABLE_HISTORY, 1);
            values.put(SETTINGS_SHOW_SERVER_NAME, 1);
            values.put(SETTINGS_SHOW_SERVER_IP, 1);
            values.put(SETTINGS_SHOW_SERVER_MAP, 1);
            values.put(SETTINGS_SHOW_SERVER_PLAYERS, 1);
            values.put(SETTINGS_SHOW_SERVER_GAME, 1);
            values.put(SETTINGS_SHOW_SERVER_TAGS, 1);
            values.put(SETTINGS_SHOW_SERVER_PING, 1);
            values.put(SETTINGS_USE_SERVER_NICKNAME, 1);
            values.put(SETTINGS_VALIDATE_NEW_SERVERS, 1);
            values.put(SETTINGS_DEFAULT_QUERY_PORT, 27015);
            values.put(SETTINGS_DEFAULT_QUERY_TIMEOUT, 1);
            values.put(SETTINGS_DEFAULT_RELAY_HOST, "");
            values.put(SETTINGS_DEFAULT_RELAY_PORT, 23456);
            values.put(SETTINGS_DEFAULT_RELAY_PASSWORD, "");
            values.put(SETTINGS_BACKGROUND_QUERY_FREQUENCY, 10);
            values.put(SETTINGS_ENABLE_NOTIFICATION_LED, 1);
            values.put(SETTINGS_ENABLE_NOTIFICATION_SOUND, 1);
            values.put(SETTINGS_ENABLE_NOTIFICATION_VIBRATE, 1);
            values.put(SETTINGS_ENABLE_NOTIFICATIONS, 0);

            Log.i(TAG, "Inserting default values");
            db.insert(TABLE_SETTINGS, null, values);

            Log.i(TAG, "Creating table " + TABLE_RELAY_HOSTS);
            db.execSQL(CREATE_TABLE_RELAY_HOSTS);
        }
        catch( SQLiteException e ) {
            Log.w(TAG, "Caught an exception while creating database:", e);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Downgrading database from version " + oldVersion + " to " + newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if( oldVersion < 2 ) {
            // Add the rcon_password column to the servers table
            db.execSQL("ALTER TABLE " + TABLE_SERVERS + " ADD COLUMN " + SERVERS_RCON + " TEXT NOT NULL DEFAULT '';");
        }

        if( oldVersion < 3 ) {
            try {
                // Create the settings table
                Log.i(TAG, "Creating table " + TABLE_SETTINGS);
                db.execSQL(CREATE_TABLE_SETTINGS);

                // Populate the settings table with default values
                ContentValues values = new ContentValues();
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

                Log.i(TAG, "Updating table " + TABLE_SERVERS);

                Cursor c = db.query(
                        TABLE_SERVERS,
                        new String[]{SERVERS_ROWID, SERVERS_LISTPOS},
                        null,
                        null,
                        null,
                        null,
                        SERVERS_LISTPOS);

                int count = c.getCount();

                // Fix list position numbering in the servers table which may have
                // been broken by deletions from the table in previous versions
                for( int i = 0; i < count; i++ ) {
                    c.moveToPosition(i);

                    long rowId = c.getLong(0);
                    int oldPos = c.getInt(1);
                    int newPos = (i + 1);

                    if( oldPos != newPos ) {
                        values = new ContentValues();
                        values.put(SERVERS_LISTPOS, newPos);

                        Log.i(TAG, "Updating server " + rowId + "; changing list position from " + oldPos + " to " + newPos);

                        db.update(TABLE_SERVERS, values, SERVERS_ROWID + " = " + rowId, null);
                    }
                }

                c.close();
            }
            catch( SQLiteException e ) {
                Log.w(TAG, "Caught an exception while upgrading database:", e);
            }
        }

        if( oldVersion < 4 ) {
            try {
                // Add the rcon_enable_history column to the settings table
                Log.i(TAG, "Adding column " + SETTINGS_RCON_ENABLE_HISTORY + " to table " + TABLE_SETTINGS);
                db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_RCON_ENABLE_HISTORY + " INTEGER NOT NULL DEFAULT 1;");

                // Set the default value
                ContentValues values = new ContentValues();
                values.put(SETTINGS_RCON_ENABLE_HISTORY, 1);

                Log.i(TAG, "Setting " + SETTINGS_RCON_ENABLE_HISTORY + " default value to 1");
                db.update(TABLE_SETTINGS, values, null, null);
            }
            catch( Exception e ) {
                Log.w(TAG, "Caught an exception while upgrading database:", e);
            }
        }

        if( oldVersion < 5 ) {
            // Add the relay_hosts table
            Log.i(TAG, "Creating table " + TABLE_RELAY_HOSTS);
            db.execSQL(CREATE_TABLE_RELAY_HOSTS);
        }

        if( oldVersion < 6 ) {
            try {
                // Add the rcon_volume_buttons column to the settings table
                Log.i(TAG, "Adding column " + SETTINGS_RCON_VOLUME_BUTTONS + " to table " + TABLE_SETTINGS);
                db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_RCON_VOLUME_BUTTONS + " INTEGER NOT NULL DEFAULT 1;");

                // Add the rcon_default_font_size column to the settings table
                Log.i(TAG, "Adding column " + SETTINGS_RCON_DEFAULT_FONT_SIZE + " to table " + TABLE_SETTINGS);
                db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_RCON_DEFAULT_FONT_SIZE + " INTEGER NOT NULL DEFAULT 9;");

                // Set the default values
                ContentValues values = new ContentValues();
                values.put(SETTINGS_RCON_VOLUME_BUTTONS, 1);
                values.put(SETTINGS_RCON_DEFAULT_FONT_SIZE, 9);

                Log.i(TAG, "Setting " + SETTINGS_RCON_VOLUME_BUTTONS + " default value to 1");
                Log.i(TAG, "Setting " + SETTINGS_RCON_DEFAULT_FONT_SIZE + " default value to 9");
                db.update(TABLE_SETTINGS, values, null, null);
            }
            catch( Exception e ) {
                Log.w(TAG, "Caught an exception while upgrading database:", e);
            }
        }

        if( oldVersion < 7 ) {
            try {
                // Add the rcon_include_sm column to the settings table
                Log.i(TAG, "Adding column " + SETTINGS_RCON_INCLUDE_SM + " to table " + TABLE_SETTINGS);
                db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_RCON_INCLUDE_SM + " INTEGER NOT NULL DEFAULT 0;");

                // Set the default value
                ContentValues values = new ContentValues();
                values.put(SETTINGS_RCON_INCLUDE_SM, 0);

                Log.i(TAG, "Setting " + SETTINGS_RCON_INCLUDE_SM + " default value to 0");
                db.update(TABLE_SETTINGS, values, null, null);
            }
            catch( Exception e ) {
                Log.w(TAG, "Caught an exception while upgrading database:", e);
            }
        }

        if( oldVersion < 8 ) {
            try {
                // Add the show_ping column to the settings table
                Log.i(TAG, "Adding column " + SETTINGS_SHOW_SERVER_PING + " to table " + TABLE_SETTINGS);
                db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_SHOW_SERVER_PING + " INTEGER NOT NULL DEFAULT 1;");

                // Set the default value
                ContentValues values = new ContentValues();
                values.put(SETTINGS_SHOW_SERVER_PING, 1);

                Log.i(TAG, "Setting " + SETTINGS_SHOW_SERVER_PING + " default value to 1");
                db.update(TABLE_SETTINGS, values, null, null);
            }
            catch( Exception e ) {
                Log.w(TAG, "Caught an exception while upgrading database:", e);
            }
        }

        if( oldVersion < 9 ) {
            try {
                // Add the nickname column to the servers table
                Log.i(TAG, "Adding column " + SERVERS_NICKNAME + " to table " + TABLE_SERVERS);
                db.execSQL("ALTER TABLE " + TABLE_SERVERS + " ADD COLUMN " + SERVERS_NICKNAME + " TEXT NOT NULL DEFAULT '';");

                // Add the show_nickname column to the settings table
                Log.i(TAG, "Adding column " + SETTINGS_USE_SERVER_NICKNAME + " to table " + TABLE_SETTINGS);
                db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_USE_SERVER_NICKNAME + " INTEGER NOT NULL DEFAULT 1;");

                // Set the default value
                ContentValues values = new ContentValues();
                values.put(SETTINGS_USE_SERVER_NICKNAME, 1);

                Log.i(TAG, "Setting " + SETTINGS_USE_SERVER_NICKNAME + " default value to 1");
                db.update(TABLE_SETTINGS, values, null, null);
            }
            catch( Exception e ) {
                Log.w(TAG, "Caught an exception while upgrading database:", e);
            }
        }

        if( oldVersion < 10 ) {
            try {
                // Add the show_name column to the settings table
                Log.i(TAG, "Adding column " + SETTINGS_SHOW_SERVER_NAME + " to table " + TABLE_SETTINGS);
                db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_SHOW_SERVER_NAME + " INTEGER NOT NULL DEFAULT 1;");

                // Set the default value
                ContentValues values = new ContentValues();
                values.put(SETTINGS_SHOW_SERVER_NAME, 1);

                Log.i(TAG, "Setting " + SETTINGS_SHOW_SERVER_NAME + " default value to 1");
                db.update(TABLE_SETTINGS, values, null, null);
            }
            catch( Exception e ) {
                Log.w(TAG, "Caught an exception while upgrading database:", e);
            }
        }

        if( oldVersion < 11 ) {
            // Add the enable_notification_led column to the settings table
            Log.i(TAG, "Adding column " + SETTINGS_ENABLE_NOTIFICATION_LED + " to table " + TABLE_SETTINGS);
            db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_ENABLE_NOTIFICATION_LED + " INTEGER NOT NULL DEFAULT 1;");

            // Add the enable_notification_sound column to the settings table
            Log.i(TAG, "Adding column " + SETTINGS_ENABLE_NOTIFICATION_SOUND + " to table " + TABLE_SETTINGS);
            db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_ENABLE_NOTIFICATION_SOUND + " INTEGER NOT NULL DEFAULT 1;");

            // Add the enable_notification_vibrate column to the settings table
            Log.i(TAG, "Adding column " + SETTINGS_ENABLE_NOTIFICATION_VIBRATE + " to table " + TABLE_SETTINGS);
            db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_ENABLE_NOTIFICATION_VIBRATE + " INTEGER NOT NULL DEFAULT 1;");

            // Add the enable_notifications column to the settings table
            Log.i(TAG, "Adding column " + SETTINGS_ENABLE_NOTIFICATIONS + " to table " + TABLE_SETTINGS);
            db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_ENABLE_NOTIFICATIONS + " INTEGER NOT NULL DEFAULT 0;");

            // Add the background_query_frequency column to the settings table
            Log.i(TAG, "Adding column " + SETTINGS_BACKGROUND_QUERY_FREQUENCY + " to table " + TABLE_SETTINGS);
            db.execSQL("ALTER TABLE " + TABLE_SETTINGS + " ADD COLUMN " + SETTINGS_BACKGROUND_QUERY_FREQUENCY + " INTEGER NOT NULL DEFAULT 10;");

            // Add the enabled column to the servers table
            Log.i(TAG, "Adding column " + SERVERS_ENABLED + " to table " + TABLE_SERVERS);
            db.execSQL("ALTER TABLE " + TABLE_SERVERS + " ADD COLUMN " + SERVERS_ENABLED + " INT NOT NULL DEFAULT 1;");

            // Set the default values for the new settings
            ContentValues settingsValues = new ContentValues();
            settingsValues.put(SETTINGS_ENABLE_NOTIFICATION_LED, 1);
            settingsValues.put(SETTINGS_ENABLE_NOTIFICATION_SOUND, 1);
            settingsValues.put(SETTINGS_ENABLE_NOTIFICATION_VIBRATE, 1);
            settingsValues.put(SETTINGS_ENABLE_NOTIFICATIONS, 0);
            settingsValues.put(SETTINGS_BACKGROUND_QUERY_FREQUENCY, 10);

            Log.i(TAG, "Setting " + SETTINGS_ENABLE_NOTIFICATION_LED + " default value to 1");
            Log.i(TAG, "Setting " + SETTINGS_ENABLE_NOTIFICATION_SOUND + " default value to 1");
            Log.i(TAG, "Setting " + SETTINGS_ENABLE_NOTIFICATION_VIBRATE + " default value to 1");
            Log.i(TAG, "Setting " + SETTINGS_ENABLE_NOTIFICATIONS + " default value to 0");
            Log.i(TAG, "Setting " + SETTINGS_BACKGROUND_QUERY_FREQUENCY + " default value to 10");
            db.update(TABLE_SETTINGS, settingsValues, null, null);

            // Set the default values for the 'enabled' column
            ContentValues serversValues = new ContentValues();
            serversValues.put(SERVERS_ENABLED, 1);

            Log.i(TAG, "Setting " + SERVERS_ENABLED + " default value to 1");
            db.update(TABLE_SERVERS, serversValues, null, null);
        }
    }

    public long getServerCount() {
        long result;

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();
            result = DatabaseUtils.queryNumEntries(db, TABLE_SERVERS);
        }

        return result;
    }

    /**
     * Adds a new server to the database.
     *
     * @param nickname The nickname of the server in CheckValve
     * @param url      The URL or IP address of the server
     * @param port     The listen port of the server
     * @param timeout  The query timeout for the server (in seconds)
     * @param password The RCON password for the server
     * @return The ID of the newly created database row.
     */
    public long insertServer(String nickname, String url, int port, int timeout, String password) {
        long result;
        int pos = getLastPosition() + 1;

        ContentValues values = new ContentValues();
        values.put(SERVERS_NICKNAME, nickname);
        values.put(SERVERS_SERVER, url);
        values.put(SERVERS_PORT, port);
        values.put(SERVERS_TIMEOUT, timeout);
        values.put(SERVERS_LISTPOS, pos);
        values.put(SERVERS_RCON, password);
        values.put(SERVERS_ENABLED, 1);

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();
            result = db.insert(TABLE_SERVERS, null, values);
        }

        return result;
    }

    /**
     * Deletes the specified server from the database.
     *
     * @param rowId The database row which contains the server's data
     * @return A boolean value indicating whether or not the operation was successful.
     */
    public boolean deleteServer(long rowId) {
        boolean result = false;
        Cursor c;

        int listPos = getServerListPosition(rowId);

        if( listPos != -1 ) {
            synchronized( lock ) {
                SQLiteDatabase db = this.getWritableDatabase();

                result = db.delete(TABLE_SERVERS, SERVERS_ROWID + "=" + rowId, null) > 0;

                if( result ) {
                    c = db.query(
                            TABLE_SERVERS,
                            new String[]{SERVERS_ROWID},
                            SERVERS_LISTPOS + " > " + listPos,
                            null,
                            null,
                            null,
                            SERVERS_LISTPOS);

                    while( c.moveToNext() )
                        this.moveServerUp(c.getLong(0));

                    c.close();
                }
            }
        }

        return result;
    }

    /**
     * Deletes all servers from the database.  Only used when restoring a backup file.
     */
    public void deleteAllServers() {
        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_SERVERS, null, null);
        }
    }

    public int getServerListPosition(long rowId) {
        int result = -1;
        Cursor c;

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[]{SERVERS_LISTPOS},
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null);

            if( c != null ) {
                if( c.getCount() > 0 ) {
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
     *
     * @return A <tt>ServerRecord[]</tt> array containing the results of the query.
     */
    public ServerRecord[] getAllServers() {
        Cursor c;

        ServerRecord[] result;

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[]{
                            SERVERS_NICKNAME,
                            SERVERS_SERVER,
                            SERVERS_RCON,
                            SERVERS_PORT,
                            SERVERS_TIMEOUT,
                            SERVERS_LISTPOS,
                            SERVERS_ROWID,
                            SERVERS_ENABLED},
                    null,
                    null,
                    null,
                    null,
                    SERVERS_LISTPOS);

            int count = c.getCount();

            result = new ServerRecord[count];

            for( int i = 0; i < count; i++ ) {
                c.moveToPosition(i);
                result[i] = new ServerRecord(
                        c.getString(0),
                        c.getString(1),
                        c.getString(2),
                        c.getInt(3),
                        c.getInt(4),
                        c.getInt(5),
                        c.getLong(6),
                        (c.getInt(7) == 1));
            }

            c.close();
        }

        return result;
    }

    /**
     * Queries the database for all enabled servers.
     *
     * @return A <tt>ServerRecord[]</tt> array containing the results of the query.
     */
    public ServerRecord[] getEnabledServers() {
        Cursor c;

        ServerRecord[] result;

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[]{
                            SERVERS_NICKNAME,
                            SERVERS_SERVER,
                            SERVERS_RCON,
                            SERVERS_PORT,
                            SERVERS_TIMEOUT,
                            SERVERS_LISTPOS,
                            SERVERS_ROWID,
                            SERVERS_ENABLED},
                    SERVERS_ENABLED + "=1",
                    null,
                    null,
                    null,
                    SERVERS_LISTPOS);

            int count = c.getCount();

            result = new ServerRecord[count];

            for( int i = 0; i < count; i++ ) {
                c.moveToPosition(i);
                result[i] = new ServerRecord(
                        c.getString(0),
                        c.getString(1),
                        c.getString(2),
                        c.getInt(3),
                        c.getInt(4),
                        c.getInt(5),
                        c.getLong(6),
                        (c.getInt(7) == 1));
            }

            c.close();
        }

        return result;
    }

    /**
     * Queries the database for the specified server's data.
     *
     * @param rowId The database row which contains the server's data
     * @return A <tt>ServerRecord</tt> containing the results of the query.
     */
    public ServerRecord getServer(long rowId) {
        ServerRecord result = null;

        Cursor c;

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[]{
                            SERVERS_NICKNAME,
                            SERVERS_SERVER,
                            SERVERS_RCON,
                            SERVERS_PORT,
                            SERVERS_TIMEOUT,
                            SERVERS_LISTPOS,
                            SERVERS_ROWID,
                            SERVERS_ENABLED},
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null);

            if( c != null ) {
                if( c.getCount() > 0 ) {
                    c.moveToFirst();
                    result = new ServerRecord(
                            c.getString(0),
                            c.getString(1),
                            c.getString(2),
                            c.getInt(3),
                            c.getInt(4),
                            c.getInt(5),
                            c.getLong(6),
                            (c.getInt(7) == 1));
                }

                c.close();
            }
        }

        return result;
    }

    /**
     * Updates the specified server's information in the database.
     *
     * @param rowId    The database row which contains the server's data
     * @param nickname The nickname for the server in CheckValve
     * @param server   The URL or IP address of the server
     * @param port     The listen port of the server
     * @param timeout  The query timeout for the server (in seconds)
     * @param password The RCON password for the server
     * @return A boolean value indicating whether or not the operation was successful.
     */
    public boolean updateServer(long rowId, String nickname, String server, int port, int timeout, String password) {
        boolean result;

        ContentValues values = new ContentValues();
        values.put(SERVERS_NICKNAME, nickname);
        values.put(SERVERS_SERVER, server);
        values.put(SERVERS_PORT, port);
        values.put(SERVERS_TIMEOUT, timeout);
        values.put(SERVERS_RCON, password);

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();
            result = db.update(TABLE_SERVERS, values, SERVERS_ROWID + "=" + rowId, null) > 0;
        }

        return result;
    }

    /**
     * Gets the last used position number in the database.
     *
     * @return The last used position number.
     */
    public int getLastPosition() {
        int result = 0;

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor c = db.query(
                    TABLE_SERVERS,
                    new String[]{SERVERS_LISTPOS},
                    null,
                    null,
                    null,
                    null,
                    SERVERS_LISTPOS + " DESC",
                    "1");

            if( c.getCount() > 0 ) {
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
     *
     * @param rowId The database row which contains the server's data
     * @return A boolean value indicating whether or not the operation was successful.
     */
    public boolean moveServerUp(long rowId) {
        Cursor c;
        boolean result;
        int r1, r2, r3;

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[]{SERVERS_LISTPOS},
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null);

            if( c.getCount() == 0 ) {
                result = false;
            }
            else {
                c.moveToFirst();

                int oldPos = c.getInt(0);
                int newPos = oldPos - 1;

                Log.d(TAG, "moveServerUp(): rowId=" + rowId + "; oldPos=" + oldPos + "; newPos=" + newPos);

                if( oldPos == 1 ) {
                    Log.d(TAG, "moveServerUp(): Not moving server because oldPos is 1");
                    result = true;
                }
                else {
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
     *
     * @param rowId The database row which contains the server's data
     * @return A boolean value indicating whether or not the operation was successful.
     * @see com.dparker.apps.checkvalve.DatabaseProvider#getLastPosition()
     */
    public boolean moveServerDown(long rowId) {
        Cursor c;
        boolean result;
        int r1, r2, r3;

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();

            c = db.query(
                    TABLE_SERVERS,
                    new String[]{SERVERS_LISTPOS},
                    SERVERS_ROWID + "=" + rowId,
                    null,
                    null,
                    null,
                    null);

            if( c.getCount() == 0 ) {
                result = false;
            }
            else {
                c.moveToFirst();

                int oldPos = c.getInt(0);
                int newPos = oldPos + 1;

                Log.d(TAG, "moveServerDown(): rowId=" + rowId + "; oldPos=" + oldPos + "; newPos=" + newPos);

                if( oldPos == getLastPosition() ) {
                    Log.d(TAG, "moveServerDown(): Not moving server because oldPos is the last position");
                    result = true;
                }
                else {
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
     *
     * @return A <tt>Bundle</tt> which contains all values from the settings table.
     */
    public Bundle getSettingsAsBundle() {
        Cursor c;
        String column;
        Bundle result = new Bundle();

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();
            c = db.query(TABLE_SETTINGS, null, null, null, null, null, SETTINGS_ROWID);

            c.moveToFirst();

            for( int i = 0; i < c.getColumnCount(); i++ ) {
                column = c.getColumnName(i);

                switch( column ) {
                    case SETTINGS_RCON_WARN_UNSAFE:
                        result.putBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_RCON_SHOW_PASSWORDS:
                        result.putBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_RCON_SHOW_SUGGESTIONS:
                        result.putBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_RCON_ENABLE_HISTORY:
                        result.putBoolean(Values.SETTING_RCON_ENABLE_HISTORY, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_RCON_VOLUME_BUTTONS:
                        result.putBoolean(Values.SETTING_RCON_VOLUME_BUTTONS, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_RCON_DEFAULT_FONT_SIZE:
                        result.putInt(Values.SETTING_RCON_DEFAULT_FONT_SIZE, c.getInt(i));
                        break;
                    case SETTINGS_RCON_INCLUDE_SM:
                        result.putBoolean(Values.SETTING_RCON_INCLUDE_SM, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_SHOW_SERVER_NAME:
                        result.putBoolean(Values.SETTING_SHOW_SERVER_NAME, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_SHOW_SERVER_IP:
                        result.putBoolean(Values.SETTING_SHOW_SERVER_IP, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_SHOW_SERVER_GAME:
                        result.putBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_SHOW_SERVER_MAP:
                        result.putBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_SHOW_SERVER_PLAYERS:
                        result.putBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_SHOW_SERVER_TAGS:
                        result.putBoolean(Values.SETTING_SHOW_SERVER_TAGS, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_SHOW_SERVER_PING:
                        result.putBoolean(Values.SETTING_SHOW_SERVER_PING, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_USE_SERVER_NICKNAME:
                        result.putBoolean(Values.SETTING_USE_SERVER_NICKNAME, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_VALIDATE_NEW_SERVERS:
                        result.putBoolean(Values.SETTING_VALIDATE_NEW_SERVERS, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_DEFAULT_QUERY_PORT:
                        result.putInt(Values.SETTING_DEFAULT_QUERY_PORT, c.getInt(i));
                        break;
                    case SETTINGS_DEFAULT_QUERY_TIMEOUT:
                        result.putInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, c.getInt(i));
                        break;
                    case SETTINGS_DEFAULT_RELAY_HOST:
                        result.putString(Values.SETTING_DEFAULT_RELAY_HOST, c.getString(i));
                        break;
                    case SETTINGS_DEFAULT_RELAY_PORT:
                        result.putInt(Values.SETTING_DEFAULT_RELAY_PORT, c.getInt(i));
                        break;
                    case SETTINGS_DEFAULT_RELAY_PASSWORD:
                        result.putString(Values.SETTING_DEFAULT_RELAY_PASSWORD, c.getString(i));
                        break;
                    case SETTINGS_ENABLE_NOTIFICATION_LED:
                        result.putBoolean(Values.SETTING_ENABLE_NOTIFICATION_LED, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_ENABLE_NOTIFICATION_SOUND:
                        result.putBoolean(Values.SETTING_ENABLE_NOTIFICATION_SOUND, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_ENABLE_NOTIFICATION_VIBRATE:
                        result.putBoolean(Values.SETTING_ENABLE_NOTIFICATION_VIBRATE, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_ENABLE_NOTIFICATIONS:
                        result.putBoolean(Values.SETTING_ENABLE_NOTIFICATIONS, (c.getInt(i) == 1));
                        break;
                    case SETTINGS_BACKGROUND_QUERY_FREQUENCY:
                        result.putInt(Values.SETTING_BACKGROUND_QUERY_FREQUENCY, c.getInt(i));
                        break;
                }
            }

            c.close();
        }

        Log.i(TAG, "getSettingsAsBundle(): Returning Bundle " + result);
        return result;
    }

    /**
     * Update the application settings in the database.
     *
     * @param settings A <tt>Bundle</tt> which contains the new settings values.
     * @return A boolean value indicating whether or not the update was successful.
     */
    public boolean updateSettings(Bundle settings) {
        int result;

        // Get boolean values from the Bundle as integers (0 or 1)
        int warnUnsafe = (settings.getBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, true)) ? 1 : 0;
        int showPwds = (settings.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, true)) ? 1 : 0;
        int showSuggest = (settings.getBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, true)) ? 1 : 0;
        int enableHistory = (settings.getBoolean(Values.SETTING_RCON_ENABLE_HISTORY, true)) ? 1 : 0;
        int volumeButtons = (settings.getBoolean(Values.SETTING_RCON_VOLUME_BUTTONS, true)) ? 1 : 0;
        int includeSM = (settings.getBoolean(Values.SETTING_RCON_INCLUDE_SM, true)) ? 1 : 0;
        int showName = (settings.getBoolean(Values.SETTING_SHOW_SERVER_NAME, true)) ? 1 : 0;
        int showIP = (settings.getBoolean(Values.SETTING_SHOW_SERVER_IP, true)) ? 1 : 0;
        int showGame = (settings.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, true)) ? 1 : 0;
        int showMap = (settings.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, true)) ? 1 : 0;
        int showPlayers = (settings.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, true)) ? 1 : 0;
        int showTags = (settings.getBoolean(Values.SETTING_SHOW_SERVER_TAGS, true)) ? 1 : 0;
        int showPing = (settings.getBoolean(Values.SETTING_SHOW_SERVER_PING, true)) ? 1 : 0;
        int showNickname = (settings.getBoolean(Values.SETTING_USE_SERVER_NICKNAME, true)) ? 1 : 0;
        int validate = (settings.getBoolean(Values.SETTING_VALIDATE_NEW_SERVERS, true)) ? 1 : 0;
        int notificationLED = (settings.getBoolean(Values.SETTING_ENABLE_NOTIFICATION_LED, true)) ? 1 : 0;
        int notificationSound = (settings.getBoolean(Values.SETTING_ENABLE_NOTIFICATION_SOUND, true)) ? 1 : 0;
        int notificationVibrate = (settings.getBoolean(Values.SETTING_ENABLE_NOTIFICATION_VIBRATE, true)) ? 1 : 0;
        int notifications = (settings.getBoolean(Values.SETTING_ENABLE_NOTIFICATIONS, false)) ? 1 : 0;

        // Get int values from the Bundle
        int defaultQueryPort = settings.getInt(Values.SETTING_DEFAULT_QUERY_PORT, 27015);
        int defaultQueryTimeout = settings.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, 1);
        int defaultRelayPort = settings.getInt(Values.SETTING_DEFAULT_RELAY_PORT, 23456);
        int defaultRconFontSize = settings.getInt(Values.SETTING_RCON_DEFAULT_FONT_SIZE, 9);
        int queryFrequency = settings.getInt(Values.SETTING_BACKGROUND_QUERY_FREQUENCY, 10);

        // Get string values from the Bundle
        String defaultRelayHost = settings.getString(Values.SETTING_DEFAULT_RELAY_HOST);
        String defaultRelayPswd = settings.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD);

        ContentValues values = new ContentValues();
        values.put(SETTINGS_RCON_WARN_UNSAFE, warnUnsafe);
        values.put(SETTINGS_RCON_SHOW_PASSWORDS, showPwds);
        values.put(SETTINGS_RCON_SHOW_SUGGESTIONS, showSuggest);
        values.put(SETTINGS_RCON_ENABLE_HISTORY, enableHistory);
        values.put(SETTINGS_RCON_VOLUME_BUTTONS, volumeButtons);
        values.put(SETTINGS_RCON_DEFAULT_FONT_SIZE, defaultRconFontSize);
        values.put(SETTINGS_RCON_INCLUDE_SM, includeSM);
        values.put(SETTINGS_SHOW_SERVER_NAME, showName);
        values.put(SETTINGS_SHOW_SERVER_IP, showIP);
        values.put(SETTINGS_SHOW_SERVER_MAP, showMap);
        values.put(SETTINGS_SHOW_SERVER_PLAYERS, showPlayers);
        values.put(SETTINGS_SHOW_SERVER_GAME, showGame);
        values.put(SETTINGS_SHOW_SERVER_TAGS, showTags);
        values.put(SETTINGS_SHOW_SERVER_PING, showPing);
        values.put(SETTINGS_USE_SERVER_NICKNAME, showNickname);
        values.put(SETTINGS_VALIDATE_NEW_SERVERS, validate);
        values.put(SETTINGS_DEFAULT_QUERY_PORT, defaultQueryPort);
        values.put(SETTINGS_DEFAULT_QUERY_TIMEOUT, defaultQueryTimeout);
        values.put(SETTINGS_DEFAULT_RELAY_HOST, defaultRelayHost);
        values.put(SETTINGS_DEFAULT_RELAY_PORT, defaultRelayPort);
        values.put(SETTINGS_DEFAULT_RELAY_PASSWORD, defaultRelayPswd);
        values.put(SETTINGS_ENABLE_NOTIFICATION_LED, notificationLED);
        values.put(SETTINGS_ENABLE_NOTIFICATION_SOUND, notificationSound);
        values.put(SETTINGS_ENABLE_NOTIFICATION_VIBRATE, notificationVibrate);
        values.put(SETTINGS_ENABLE_NOTIFICATIONS, notifications);
        values.put(SETTINGS_BACKGROUND_QUERY_FREQUENCY, queryFrequency);

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();

            Log.i(TAG, "Updating " + TABLE_SETTINGS + " with ContentValues " + values);
            result = db.update(TABLE_SETTINGS, values, null, null);
            Log.i(TAG, "Updated " + result + " row(s)");
        }

        return (result != 0);
    }

    /**
     * Add a Chat Relay host to the database.
     *
     * @param host The IP address or URL to be added.
     * @return A boolean value indicating whether or not the update was successful.
     */
    public boolean putRelayHost(String host) {
        if( host.isEmpty() ) {
            Log.d(TAG, "putRelayHost(): Host parameter was an empty string; returning false.");
            return false;
        }

        long result;

        ContentValues values = new ContentValues();
        values.put(RELAY_HOSTS_HOST, host);

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();

            Log.i(TAG, "Inserting row into " + TABLE_RELAY_HOSTS + " with ContentValues " + values);
            result = db.insert(TABLE_RELAY_HOSTS, null, values);
            Log.i(TAG, "Inserted row ID = " + result);
        }

        return (result >= 0);
    }

    /**
     * Queries the database for the saved Chat Relay hosts.
     *
     * @return A <tt>String[]</tt> array containing the results of the query.
     */
    public String[] getRelayHosts() {
        String[] result;

        Cursor c;

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            c = db.query(
                    TABLE_RELAY_HOSTS,
                    new String[]{RELAY_HOSTS_HOST},
                    null,
                    null,
                    null,
                    null,
                    RELAY_HOSTS_ROWID);

            int count = c.getCount();

            result = new String[count];

            for( int i = 0; i < count; i++ ) {
                c.moveToPosition(i);
                result[i] = c.getString(0);
            }

            c.close();
        }

        return result;
    }

    /**
     * Deletes all saved Chat Relay hosts from the database.
     */
    public void deleteRelayHosts() {
        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_RELAY_HOSTS, null, null);
        }
    }

    /**
     * Puts all server and setting data into a String for saving to a backup file.
     *
     * @param includeServers  Boolean indicating whether server data should be included
     * @param includeSettings Boolean indicating whether setting data should be included
     * @return A <tt>StringBuilder</tt> containing all of the requested data in backup file format
     */
    @SuppressLint("NewApi")
    public StringBuilder getBackupData(boolean includeServers, boolean includeSettings) {
        StringBuilder sb = new StringBuilder();
        Cursor c = null;

        try {
            if( includeServers ) {
                synchronized( lock ) {
                    SQLiteDatabase db = this.getReadableDatabase();

                    // Get all servers
                    c = db.query(
                            TABLE_SERVERS,
                            new String[]{
                                    SERVERS_ROWID,
                                    SERVERS_NICKNAME,
                                    SERVERS_SERVER,
                                    SERVERS_PORT,
                                    SERVERS_TIMEOUT,
                                    SERVERS_LISTPOS,
                                    SERVERS_RCON,
                                    SERVERS_ENABLED},
                            null,
                            null,
                            null,
                            null,
                            SERVERS_LISTPOS);

                    int rowCount = c.getCount();
                    String rconRaw;
                    String rconB64;

                    for( int i = 0; i < rowCount; i++ ) {
                        c.moveToPosition(i);

                        // Base64 encode the RCON password
                        rconRaw = c.getString(6);
                        rconB64 = (!rconRaw.isEmpty()) ? Base64.encodeToString(rconRaw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP) : "";

                        // Add this server as a CheckValve backup file stanza
                        sb.append("[server]").append("\r\n")
                                .append("nickname=").append(c.getString(1)).append("\r\n")
                                .append("url=").append(c.getString(2)).append("\r\n")
                                .append("port=").append(c.getString(3)).append("\r\n")
                                .append("timeout=").append(c.getInt(4)).append("\r\n")
                                .append("rcon=").append(rconB64).append("\r\n")
                                .append("listpos=").append(c.getInt(5)).append("\r\n")
                                .append("enabled=").append(c.getInt(7)).append("\r\n")
                                .append("\r\n");
                    }

                    c.close();
                }
            }

            if( includeSettings ) {
                synchronized( lock ) {
                    SQLiteDatabase db = this.getReadableDatabase();
                    int columnCount;

                    // Get settings with boolean values
                    c = db.query(TABLE_SETTINGS, boolCols, null, null, null, null, SETTINGS_ROWID);
                    columnCount = c.getColumnCount();
                    c.moveToFirst();

                    for( int i = 0; i < columnCount; i++ ) {
                        // Add this string setting as a CheckValve backup file stanza
                        sb.append("[setting]\r\n")
                                .append("type=bool").append("\r\n")
                                .append("id=").append(c.getColumnName(i)).append("\r\n")
                                .append("value=").append((c.getInt(i) == 0) ? "false" : "true").append("\r\n")
                                .append("\r\n");
                    }

                    c.close();

                    // Get settings with string values
                    c = db.query(TABLE_SETTINGS, stringCols, null, null, null, null, SETTINGS_ROWID);
                    columnCount = c.getColumnCount();
                    c.moveToFirst();

                    for( int i = 0; i < columnCount; i++ ) {
                        // Add this string setting as a CheckValve backup file stanza
                        sb.append("[setting]\r\n")
                                .append("type=string").append("\r\n")
                                .append("id=").append(c.getColumnName(i)).append("\r\n")
                                .append("value=").append(c.getString(i)).append("\r\n")
                                .append("\r\n");
                    }

                    c.close();

                    // Get settings with integer values
                    c = db.query(TABLE_SETTINGS, intCols, null, null, null, null, SETTINGS_ROWID);
                    columnCount = c.getColumnCount();
                    c.moveToFirst();

                    for( int i = 0; i < columnCount; i++ ) {
                        // Add this integer setting as a CheckValve backup file stanza
                        sb.append("[setting]\r\n")
                                .append("type=int").append("\r\n")
                                .append("id=").append(c.getColumnName(i)).append("\r\n")
                                .append("value=").append(c.getString(i)).append("\r\n")
                                .append("\r\n");
                    }

                    c.close();
                }
            }

            return sb;
        }
        catch( Exception e ) {
            Log.e(TAG, "createBackupFile(): Caught an exception:", e);

            if( c != null ) {
                c.close();
            }

            return null;
        }
    }

    /**
     * Determine whether a server nickname already exists in the database.
     *
     * @param s The server nickname to check
     * @return <tt>true</tt> if the nickname exists, <tt>false</tt> otherwise
     */
    public boolean serverNicknameExists(String s) {
        boolean result = false;
        String[] cols = new String[]{SERVERS_NICKNAME};
        String where = String.format("%1$s = '%2$s'", SERVERS_NICKNAME, s);

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor c = db.query(TABLE_SERVERS, cols, where, null, null, null, null);

            if( c != null ) {
                if( c.getCount() > 0 ) {
                    result = true;
                }
            }

            c.close();
        }

        return result;
    }

    /**
     * Get the value of a boolean setting from the database.
     *
     * @param s The name of the column for which the value should be returned
     * @return The value of the column
     * @throws InvalidDataTypeException if the specified column does not hold a boolean setting
     */
    public boolean getBooleanSetting(String s) throws InvalidDataTypeException {
        if( !isBooleanColumn(s) )
            throw new InvalidDataTypeException();

        boolean result = false;
        String[] col = new String[]{s};

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor c = db.query(TABLE_SETTINGS, col, null, null, null, null, null);

            if( c != null ) {
                if( c.getCount() > 0 ) {
                    c.moveToFirst();
                    result = (c.getInt(0) == 1);
                }
            }

            c.close();
        }

        return result;
    }

    /**
     * Get the value of an integer setting from the database.
     *
     * @param s The name of the column for which the value should be returned
     * @return The value of the column
     * @throws InvalidDataTypeException if the specified column does not hold an integer setting
     */
    public int getIntSetting(String s) throws InvalidDataTypeException {
        if( !isIntColumn(s) )
            throw new InvalidDataTypeException();

        int result = 0;
        String[] col = new String[]{s};

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor c = db.query(TABLE_SETTINGS, col, null, null, null, null, null);

            if( c != null ) {
                if( c.getCount() > 0 ) {
                    c.moveToFirst();
                    result = c.getInt(0);
                }
            }

            c.close();
        }

        return result;
    }

    /**
     * Get the value of a String setting from the database.
     *
     * @param s The name of the column for which the value should be returned
     * @return The value of the column
     * @throws InvalidDataTypeException if the specified column does not hold a String setting
     */
    public String getStringSetting(String s) throws InvalidDataTypeException {
        if( !isStringColumn(s) )
            throw new InvalidDataTypeException();

        String result = new String();
        String[] col = new String[]{s};

        synchronized( lock ) {
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor c = db.query(TABLE_SETTINGS, col, null, null, null, null, null);

            if( c != null ) {
                if( c.getCount() > 0 ) {
                    c.moveToFirst();
                    result = c.getString(0);
                }
            }

            c.close();
        }

        return result;
    }

    /**
     * Determine whether the specified column is a boolean setting.
     *
     * @param s The name of the column in the settings table
     * @return <tt>true</tt> of the setting is boolean, <tt>false</tt> otherwise
     */
    private boolean isBooleanColumn(String s) {
        boolean result = false;

        for( String x : boolCols ) {
            if( x.equals(s) ) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Determine whether the specified column is an integer setting.
     *
     * @param s The name of the column in the settings table
     * @return <tt>true</tt> of the setting is an integer, <tt>false</tt> otherwise
     */
    private boolean isIntColumn(String s) {
        boolean result = false;

        for( String x : intCols ) {
            if( x.equals(s) ) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Determine whether the specified column is a string setting.
     *
     * @param s The name of the column in the settings table
     * @return <tt>true</tt> of the setting is a string, <tt>false</tt> otherwise
     */
    private boolean isStringColumn(String s) {
        boolean result = false;

        for( String x : stringCols ) {
            if( x.equals(s) ) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Enable the specified server in the database.
     *
     * @param rowId The database row which contains the server's data
     * @return <tt>true</tt> if the operation is successful, or <tt>false</tt> on error
     */
    public boolean enableServer(long rowId) {
        boolean result;

        ContentValues values = new ContentValues();
        values.put(SERVERS_ENABLED, 1);

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();
            result = db.update(TABLE_SERVERS, values, SERVERS_ROWID + "=" + rowId, null) > 0;
        }

        return result;
    }

    /**
     * Disable the specified server in the database.
     *
     * @param rowId The database row which contains the server's data
     * @return <tt>true</tt> if the operation is successful, or <tt>false</tt> on error
     */
    public boolean disableServer(long rowId) {
        boolean result;

        ContentValues values = new ContentValues();
        values.put(SERVERS_ENABLED, 0);

        synchronized( lock ) {
            SQLiteDatabase db = this.getWritableDatabase();
            result = db.update(TABLE_SERVERS, values, SERVERS_ROWID + "=" + rowId, null) > 0;
        }

        return result;
    }
}