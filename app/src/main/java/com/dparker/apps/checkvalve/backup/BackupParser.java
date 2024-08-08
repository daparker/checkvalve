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

package com.dparker.apps.checkvalve.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.dparker.apps.checkvalve.DatabaseProvider;
import com.dparker.apps.checkvalve.Values;
import com.dparker.apps.checkvalve.exceptions.InvalidBackupFileException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

public class BackupParser implements Runnable {
    private static final String TAG = BackupParser.class.getSimpleName();

    private Uri u;
    private String f;
    private DatabaseProvider database;
    private ServerBackupRecord[] servers;
    private SettingBackupRecord[] settings;
    private String[] flags;
    private final Handler h;
    private final Context c;

    public BackupParser(Context context, String filename, Handler handler) {
        this.c = context;
        this.f = filename;
        this.h = handler;
    }

    public BackupParser(Context context, Uri uri, Handler handler) {
        this.c = context;
        this.u = uri;
        this.h = handler;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        Bundle b = new Bundle();
        boolean result = false;
        int what;

        try {
            database = new DatabaseProvider(c);

            if( getBackupData() ) {
                if( settings.length > 0 )
                    b = getSettingsData();

                if( restoreBackupData(b) )
                    result = true;
            }

            what = (result) ? 0 : 1;
        }
        catch( InvalidBackupFileException ibfe ) {
            Log.e(TAG, "run(): Invalid backup file");
            what = 2;
        }
        catch( Exception e ) {
            Log.e(TAG, "run(): Caught an exception:", e);
            what = 3;
        }
        finally {
            database.close();
            database = null;
        }

        h.sendEmptyMessage(what);
    }

    public boolean getBackupData() throws InvalidBackupFileException {
        ParcelFileDescriptor pfd = null;
        BufferedReader reader;

        try {
            if( u != null )
                pfd = c.getContentResolver().openFileDescriptor(u, "r");
            else
                pfd = ParcelFileDescriptor.open(new File(f), ParcelFileDescriptor.MODE_READ_ONLY);
        }
        catch( FileNotFoundException e ) {
            Log.e(TAG, "getBackupData(): Caught an exception:", e);
        }

        String line;
        int numServers = 0;
        int numSettings = 0;
        int numFlags = 0;
        int numVersions = 0;
        boolean PARSING_SERVER = false;
        boolean PARSING_SETTING = false;
        boolean PARSING_VERSION = false;
        boolean PARSING_FLAG = false;

        try {
            // Don't open a file larger than 1 MB
            if ( pfd.getStatSize() > 1048576 ) {
                Log.e(TAG, "getBackupData(): Selected file exceeds maximum allowed size.");
                throw new InvalidBackupFileException();
            }

            // Create a new reader and set a mark at the beginning of the file
            reader = new BufferedReader(new FileReader(pfd.getFileDescriptor()));
            reader.mark((int)pfd.getStatSize()+1);

            // First pass: count the number of server, setting, version, and flag stanzas
            while( (line = reader.readLine()) != null ) {
                if( line.equals("[version]") )
                    numVersions++;
                else if( line.equals("[server]") )
                    numServers++;
                else if( line.equals("[setting]") )
                    numSettings++;
                else if( line.equals("[flag]") )
                    numFlags++;
            }

            // If there is not exactly 1 version stanza then the file is invalid
            if( numVersions != 1 ) {
                Log.e(TAG, "getBackupData(): File must contain exactly ONE version stanza (found " + numVersions + ").");
                throw new InvalidBackupFileException();
            }

            Log.i(TAG, "getBackupData(): Found " + numServers + " server(s).");
            Log.i(TAG, "getBackupData(): Found " + numSettings + " setting(s).");
            Log.i(TAG, "getBackupData(): Found " + numFlags + " flag(s).");

            // Correctly-sized arrays for the backup data
            servers = new ServerBackupRecord[numServers];
            settings = new SettingBackupRecord[numSettings];
            flags = new String[numFlags];

            // Version record
            VersionBackupRecord versionInfo = new VersionBackupRecord();

            // Counters
            int serverRecordNum = 0;
            int settingRecordNum = 0;
            int flagRecordNum = 0;
            int lineNumber = 0;
            int dataLine = 0;
            int x = 0;

            // Move the reader back to the beginning of the file
            reader.reset();

            // Second pass: do the heavy lifting
            while( (line = reader.readLine()) != null ) {
                lineNumber++;
                line = line.trim();

                if( line.isEmpty() || line.charAt(0) == '#' ) continue;

                dataLine++;

                // If this is the first line of backup data then make sure it's the version stanza
                if( dataLine == 1 ) {
                    if( line.equals("[version]") ) {
                        PARSING_SERVER = false;
                        PARSING_SETTING = false;
                        PARSING_VERSION = true;
                        PARSING_FLAG = false;
                    }
                    else {
                        reader.close();
                        Log.e(TAG, "getBackupData(): Backup data does not start with a version stanza");
                        throw new InvalidBackupFileException();
                    }
                }
                else if( line.equals("[server]") ) {
                    PARSING_SERVER = true;
                    PARSING_SETTING = false;
                    PARSING_VERSION = false;
                    PARSING_FLAG = false;
                    x = serverRecordNum++;
                    servers[x] = new ServerBackupRecord();
                }
                else if( line.equals("[setting]") ) {
                    PARSING_SERVER = false;
                    PARSING_SETTING = true;
                    PARSING_VERSION = false;
                    PARSING_FLAG = false;
                    x = settingRecordNum++;
                    settings[x] = new SettingBackupRecord();
                }
                else if( line.equals("[flag]") ) {
                    PARSING_SERVER = false;
                    PARSING_SETTING = false;
                    PARSING_VERSION = false;
                    PARSING_FLAG = true;
                    x = flagRecordNum++;
                    flags[x] = new String();
                }
                else {
                    String[] fields = line.split("=", 2);

                    if( PARSING_SERVER ) {
                        if( fields[0].equals("url") ) {
                            servers[x].setURL(fields[1]);
                        }
                        else if( fields[0].equals("port") ) {
                            servers[x].setPort(Integer.parseInt(fields[1]));
                        }
                        else if( fields[0].equals("timeout") ) {
                            servers[x].setTimeout(Integer.parseInt(fields[1]));
                        }
                        else if( fields[0].equals("listpos") ) {
                            servers[x].setListPos(Integer.parseInt(fields[1]));
                        }
                        else if( fields[0].equals("rcon") ) {
                            servers[x].setRCONPassword(fields[1]);
                        }
                        else if( fields[0].equals("nickname") ) {
                            servers[x].setName(fields[1]);
                        }
                        else if( fields[0].equals("enabled") ) {
                            servers[x].setEnabled(Integer.parseInt(fields[1]));
                        }
                        else {
                            Log.e(TAG, "getBackupData(): Unrecognized server key '" + fields[0] + "' on line " + lineNumber);
                            reader.close();
                            throw new InvalidBackupFileException();
                        }
                    }
                    else if( PARSING_SETTING ) {
                        if( fields[0].equals("type") ) {
                            settings[x].setType(fields[1]);
                        }
                        else if( fields[0].equals("id") ) {
                            settings[x].setID(fields[1]);
                        }
                        else if( fields[0].equals("value") ) {
                            settings[x].setValue(fields[1]);
                        }
                        else {
                            Log.e(TAG, "getBackupData(): Unrecognized setting key '" + fields[0] + "' on line " + lineNumber);
                            reader.close();
                            throw new InvalidBackupFileException();
                        }
                    }
                    else if( PARSING_VERSION ) {
                        if( fields[0].equals("app") ) {
                            versionInfo.setAppVersion(Integer.parseInt(fields[1]));
                        }
                        else if( fields[0].equals("file") ) {
                            versionInfo.setFileVersion(Integer.parseInt(fields[1]));
                        }
                        else {
                            Log.e(TAG, "getBackupData(): Unrecognized version key '" + fields[0] + "' on line " + lineNumber);
                            reader.close();
                            throw new InvalidBackupFileException();
                        }
                    }
                    else if( PARSING_FLAG ) {
                        if( fields[0].equals("name") ) {
                            flags[x] = fields[1];
                        }
                        else {
                            Log.e(TAG, "getBackupData(): Unrecognized flag key '" + fields[0] + "' on line " + lineNumber);
                            reader.close();
                            throw new InvalidBackupFileException();
                        }
                    }
                    else {
                        Log.e(TAG, "getBackupData(): Found data on line " + lineNumber + " but no parsing flag is set!");
                        reader.close();
                        throw new InvalidBackupFileException();
                    }
                }
            }

            reader.close();

            if( !versionInfo.isValid() ) {
                Log.e(TAG, "getBackupData(): Invalid version data [appVersion=" + versionInfo.getAppVersion() + "; fileVersion=" + versionInfo.getFileVersion() + "]");
                throw new InvalidBackupFileException();
            }

            Log.i(TAG, "getBackupData(): App version: " + versionInfo.getAppVersion());
            Log.i(TAG, "getBackupData(): File version: " + versionInfo.getFileVersion());

            // Validate all of the server records in the backup
            if( numServers > 0 ) {
                for( int i = 0; i < servers.length; i++ ) {
                    if( servers[i].isValid() ) {
                        Log.d(TAG, "getBackupData(): Server #" + i + " is valid.");
                    }
                    else {
                        Log.e(TAG, "getBackupData(): Server #" + i + " is invalid [url=" + servers[i].getURL() + "; port=" + servers[i].getPort() + "; timeout=" + servers[i].getTimeout() + "]");
                        throw new InvalidBackupFileException();
                    }
                }
            }

            // Validate all of the settings records in the backup
            if( numSettings > 0 ) {
                for( int i = 0; i < settings.length; i++ ) {
                    if( settings[i].isValid() ) {
                        Log.d(TAG, "getBackupData(): Setting #" + i + " is valid.");
                    }
                    else {
                        Log.e(TAG, "getBackupData(): Setting #" + i + " is invalid [name=" + settings[i].getID() + "; type=" + settings[i].getType() + "; value=" + settings[i].getValue() + "]");
                        throw new InvalidBackupFileException();
                    }
                }
            }

            // Validate all of the flags in the backup
            if( numFlags > 0 ) {
                for( int i = 0; i < flags.length; i++ ) {
                    if( flags[i].equals(Values.FILE_HIDE_CHAT_RELAY_NOTE)
                            || flags[i].equals(Values.FILE_HIDE_CONSOLE_RELAY_NOTE) ) {
                        Log.d(TAG, "getBackupData(): Flag #" + i + " is valid.");
                    }
                    else {
                        Log.e(TAG, "getBackupData(): Flag #" + i + " is invalid [name=" + flags[i] + "]");
                        throw new InvalidBackupFileException();
                    }
                }
            }

            pfd.close();
        }
        catch( InvalidBackupFileException ibfe ) {
            Log.e(TAG, "getBackupData(): Caught an exception:", ibfe);
            throw ibfe;
        }
        catch( Exception e ) {
            Log.e(TAG, "getBackupData(): Caught an exception:", e);
            return false;
        }

        return true;
    }

    public Bundle getSettingsData() throws InvalidBackupFileException {
        Bundle b = new Bundle();
        String type;
        String name;

        HashMap<String, String> map = new HashMap<>();

        map.put(DatabaseProvider.SETTINGS_RCON_DEFAULT_FONT_SIZE, Values.SETTING_RCON_DEFAULT_FONT_SIZE);
        map.put(DatabaseProvider.SETTINGS_DEFAULT_QUERY_PORT, Values.SETTING_DEFAULT_QUERY_PORT);
        map.put(DatabaseProvider.SETTINGS_DEFAULT_QUERY_TIMEOUT, Values.SETTING_DEFAULT_QUERY_TIMEOUT);
        map.put(DatabaseProvider.SETTINGS_DEFAULT_RELAY_PORT, Values.SETTING_DEFAULT_RELAY_PORT);
        map.put(DatabaseProvider.SETTINGS_RCON_WARN_UNSAFE, Values.SETTING_RCON_WARN_UNSAFE_COMMAND);
        map.put(DatabaseProvider.SETTINGS_RCON_SHOW_PASSWORDS, Values.SETTING_RCON_SHOW_PASSWORDS);
        map.put(DatabaseProvider.SETTINGS_RCON_SHOW_SUGGESTIONS, Values.SETTING_RCON_SHOW_SUGGESTIONS);
        map.put(DatabaseProvider.SETTINGS_RCON_ENABLE_HISTORY, Values.SETTING_RCON_ENABLE_HISTORY);
        map.put(DatabaseProvider.SETTINGS_RCON_VOLUME_BUTTONS, Values.SETTING_RCON_VOLUME_BUTTONS);
        map.put(DatabaseProvider.SETTINGS_RCON_INCLUDE_SM, Values.SETTING_RCON_INCLUDE_SM);
        map.put(DatabaseProvider.SETTINGS_SHOW_SERVER_IP, Values.SETTING_SHOW_SERVER_IP);
        map.put(DatabaseProvider.SETTINGS_SHOW_SERVER_MAP, Values.SETTING_SHOW_SERVER_MAP_NAME);
        map.put(DatabaseProvider.SETTINGS_SHOW_SERVER_PLAYERS, Values.SETTING_SHOW_SERVER_NUM_PLAYERS);
        map.put(DatabaseProvider.SETTINGS_SHOW_SERVER_GAME, Values.SETTING_SHOW_SERVER_GAME_INFO);
        map.put(DatabaseProvider.SETTINGS_SHOW_SERVER_TAGS, Values.SETTING_SHOW_SERVER_TAGS);
        map.put(DatabaseProvider.SETTINGS_SHOW_SERVER_PING, Values.SETTING_SHOW_SERVER_PING);
        map.put(DatabaseProvider.SETTINGS_USE_SERVER_NICKNAME, Values.SETTING_USE_SERVER_NICKNAME);
        map.put(DatabaseProvider.SETTINGS_VALIDATE_NEW_SERVERS, Values.SETTING_VALIDATE_NEW_SERVERS);
        map.put(DatabaseProvider.SETTINGS_DEFAULT_RELAY_HOST, Values.SETTING_DEFAULT_RELAY_HOST);
        map.put(DatabaseProvider.SETTINGS_DEFAULT_RELAY_PASSWORD, Values.SETTING_DEFAULT_RELAY_PASSWORD);
        map.put(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATION_LED, Values.SETTING_ENABLE_NOTIFICATION_LED);
        map.put(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATION_SOUND, Values.SETTING_ENABLE_NOTIFICATION_SOUND);
        map.put(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATION_VIBRATE, Values.SETTING_ENABLE_NOTIFICATION_VIBRATE);
        map.put(DatabaseProvider.SETTINGS_ENABLE_NOTIFICATIONS, Values.SETTING_ENABLE_NOTIFICATIONS);

        for( SettingBackupRecord s : settings ) {
            type = s.getType();
            name = s.getID();

            if( type.equals("string") ) {
                b.putString(map.get(name), s.getValue());
            }
            else if( type.equals("int") ) {
                b.putInt(map.get(name), Integer.parseInt(s.getValue()));
            }
            else if( type.equals("bool") ) {
                b.putBoolean(map.get(name), Boolean.parseBoolean(s.getValue()));
            }
            else {
                Log.e(TAG, "getSettingsData(): Setting '" + name + "' has invalid type '" + type + "'!");
                throw new InvalidBackupFileException();
            }
        }

        Log.d(TAG, "getSettingsData(): Created a new Bundle with settings from the backup file");

        return b;
    }

    public boolean restoreBackupData(Bundle newSettings) throws InvalidBackupFileException {
        try {
            Log.i(TAG, "restoreBackupData(): Sorting server list by position value");
            sortServersByPosition();

            Log.i(TAG, "restoreBackupData(): Purging server records from the database");
            database.deleteAllServers();
        }
        catch( InvalidBackupFileException ibfe ) {
            throw ibfe;
        }
        catch( Exception e ) {
            Log.e(TAG, "restoreBackupData(): Caught an exception:", e);
            return false;
        }

        Log.i(TAG, "restoreBackupData(): Inserting server records from backup into the database");

        long id = 0L;

        for( ServerBackupRecord s : servers ) {
            id = database.insertServer(s.getName(), s.getURL(), s.getPort(), s.getTimeout(), s.getRCONPassword());

            if( s.getEnabled() == 0 ) {
                Log.i(TAG, "restoreBackupData(): Server " + s.getURL() + ":" + s.getPort() + " is disabled.");
                database.disableServer(id);
            }
        }

        if( ! newSettings.isEmpty() ) {
            Log.i(TAG, "restoreBackupData(): Calling updateSettings() with Bundle " + newSettings);

            if( database.updateSettings(newSettings) ) {
                Log.i(TAG, "restoreBackupData(): Successfully updated settings in database");
            }
            else {
                Log.e(TAG, "restoreBackupData(): Failed to update settings in database!");
                return false;
            }
        }

        if( flags.length > 0 ) {
            File filesDir = c.getFilesDir();

            try {
                for( String s : flags ) {
                    File f = new File(filesDir, s);
                    if( ! f.exists() ) f.createNewFile();
                }
            }
            catch( Exception e ) {
                Log.e(TAG, "restoreBackupData(): Caught an exception:", e);
                return false;
            }
        }

        return true;
    }

    /*
     * Sorts the array of servers by the listpos value
     */
    public void sortServersByPosition() throws InvalidBackupFileException {
        ServerBackupRecord[] tmp = new ServerBackupRecord[servers.length];

        int x;    // The index in the new array

        for( ServerBackupRecord s : servers ) {
            x = s.getListPos() - 1;

            if( tmp[x] != null ) {
                // There's a duplicate listpos value in the backup file
                throw new InvalidBackupFileException();
            }
            else {
                tmp[x] = s;
            }
        }

        servers = tmp;
    }
}