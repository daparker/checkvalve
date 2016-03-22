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

import java.io.File;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import com.github.daparker.checkvalve.R;

public class SettingsActivity extends Activity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    private boolean rconShowPasswords;
    private boolean rconWarnUnsafeCommand;
    private boolean rconShowSuggestions;
    private boolean rconEnableHistory;
    private boolean rconVolumeButtons;
    private boolean rconIncludeSM;
    private boolean showServerName;
    private boolean showServerIP;
    private boolean showServerGameInfo;
    private boolean showServerMapName;
    private boolean showServerNumPlayers;
    private boolean showServerTags;
    private boolean showServerPing;
    private boolean useServerAlias;
    private boolean validateNewServers;
    private boolean refreshServers;

    private Button saveButton;
    private Button cancelButton;
    private Button plusButton;
    private Button minusButton;
    private CheckBox checkbox_rcon_show_passwords;
    private CheckBox checkbox_rcon_warn_unsafe_command;
    private CheckBox checkbox_rcon_show_suggestions;
    private CheckBox checkbox_rcon_enable_history;
    private CheckBox checkbox_rcon_volume_buttons;
    private CheckBox checkbox_rcon_include_sm;
    private CheckBox checkbox_show_server_name;
    private CheckBox checkbox_show_server_ip;
    private CheckBox checkbox_show_server_game_info;
    private CheckBox checkbox_show_server_map_name;
    private CheckBox checkbox_show_server_num_players;
    private CheckBox checkbox_show_server_tags;
    private CheckBox checkbox_show_server_ping;
    private CheckBox checkbox_use_server_alias;
    private CheckBox checkbox_validate_new_servers;
    private EditText field_default_query_port;
    private EditText field_default_query_timeout;
    private EditText field_default_relay_host;
    private EditText field_default_relay_port;
    private EditText field_default_relay_password;
    private TextView field_default_rcon_font_size;
    private TextView reset_do_not_show;
    private TextView clear_saved_relays;

    private DatabaseProvider database;

    private OnTouchListener buttonTouchListener = new OnTouchListener() {
        public boolean onTouch( View v, MotionEvent m ) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            }
            else {
                v.setBackgroundColor(getResources().getColor(R.color.steam_gray));
            }

            return false;
        }
    };
    
    private OnTouchListener resetTouchListener = new OnTouchListener() {
        public boolean onTouch( View v, MotionEvent m ) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                /*
                 * Delete marker files for 'do not show' settings
                 */
                	
                File filesDir = SettingsActivity.this.getFilesDir();
                File f1 = new File(filesDir, Values.FILE_HIDE_CHAT_RELAY_NOTE);
                File f2 = new File(filesDir, Values.FILE_HIDE_CONSOLE_RELAY_NOTE);
                		    	
                f1.delete();
                f2.delete();
                		    	
                UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_options_reset);
            }
            	    	
            return false;
        }
    };
    
    private OnTouchListener clearSavedRelaysTouchListener = new OnTouchListener() {
        public boolean onTouch( View v, MotionEvent m ) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                database.deleteRelayHosts();
                UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_cleared_relay_list);
            }
            
            return false;
        }
    };
    
    private OnTouchListener createBackupTouchListener = new OnTouchListener() {
        public boolean onTouch( View v, MotionEvent m ) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                createBackup();
            }
            
            return false;
        }
    };
    
    private OnTouchListener restoreBackupTouchListener = new OnTouchListener() {
        public boolean onTouch( View v, MotionEvent m ) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                restoreBackup();
            }
            
            return false;
        }
    };
    
    private OnClickListener saveButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Save" button was clicked
             */

            // Refresh the server list if the "User server alias" option has been toggled
            if( checkbox_use_server_alias.isChecked() != CheckValve.settings.getBoolean(Values.SETTING_USE_SERVER_ALIAS) )
                refreshServers = true;
            
            saveSettings();
            setResult((refreshServers)?1:0);
            finish();
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Cancel" button was clicked
             */
            
            setResult((refreshServers)?1:0);
            finish();
        }
    };

    @SuppressLint("NewApi")
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        if( android.os.Build.VERSION.SDK_INT < 11 ) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        else if( android.os.Build.VERSION.SDK_INT >= 14 ) {
            if( ViewConfiguration.get(this).hasPermanentMenuKey() )
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        
        this.setContentView(R.layout.settings);
        this.setResult(0);
        
        // Flag to main Activity to refresh the server list if necessary
        refreshServers = false;
        
        if( database == null )
            database = new DatabaseProvider(SettingsActivity.this);

        cancelButton = (Button)findViewById(R.id.settings_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);
        cancelButton.setOnTouchListener(buttonTouchListener);
        cancelButton.setFocusable(true);
        cancelButton.setFocusableInTouchMode(true);

        saveButton = (Button)findViewById(R.id.settings_save_button);
        saveButton.setOnClickListener(saveButtonListener);
        saveButton.setOnTouchListener(buttonTouchListener);
        
        plusButton = (Button)findViewById(R.id.settings_rcon_font_size_plus);
        minusButton = (Button)findViewById(R.id.settings_rcon_font_size_minus);
        
        checkbox_rcon_show_passwords = (CheckBox)findViewById(R.id.settings_checkbox_rcon_show_passwords);
        checkbox_rcon_warn_unsafe_command = (CheckBox)findViewById(R.id.settings_checkbox_rcon_warn_unsafe_command);
        checkbox_rcon_show_suggestions = (CheckBox)findViewById(R.id.settings_checkbox_rcon_show_suggestions);
        checkbox_rcon_enable_history = (CheckBox)findViewById(R.id.settings_checkbox_rcon_enable_history);
        checkbox_rcon_volume_buttons = (CheckBox)findViewById(R.id.settings_checkbox_rcon_volume_buttons);
        checkbox_rcon_include_sm = (CheckBox)findViewById(R.id.settings_checkbox_rcon_include_sm);
        checkbox_show_server_name = (CheckBox)findViewById(R.id.settings_checkbox_servers_show_name);
        checkbox_show_server_ip = (CheckBox)findViewById(R.id.settings_checkbox_servers_show_ip);
        checkbox_show_server_game_info = (CheckBox)findViewById(R.id.settings_checkbox_servers_show_game);
        checkbox_show_server_map_name = (CheckBox)findViewById(R.id.settings_checkbox_servers_show_map);
        checkbox_show_server_num_players = (CheckBox)findViewById(R.id.settings_checkbox_servers_show_players);
        checkbox_show_server_tags = (CheckBox)findViewById(R.id.settings_checkbox_servers_show_tags);
        checkbox_show_server_ping = (CheckBox)findViewById(R.id.settings_checkbox_servers_show_ping);
        checkbox_use_server_alias = (CheckBox)findViewById(R.id.settings_checkbox_servers_use_alias);
        checkbox_validate_new_servers = (CheckBox)findViewById(R.id.settings_checkbox_validate_new_servers);
        field_default_query_port = (EditText)findViewById(R.id.settings_field_default_query_port);
        field_default_query_timeout = (EditText)findViewById(R.id.settings_field_default_query_timeout);
        field_default_relay_host = (EditText)findViewById(R.id.settings_field_default_relay_host);
        field_default_relay_port = (EditText)findViewById(R.id.settings_field_default_relay_port);
        field_default_relay_password = (EditText)findViewById(R.id.settings_field_default_relay_password);
        field_default_rcon_font_size = (TextView)findViewById(R.id.settings_field_default_rcon_font_size);

        reset_do_not_show = (TextView)findViewById(R.id.settings_reset_do_not_show);
        reset_do_not_show.setOnTouchListener(resetTouchListener);
        
        clear_saved_relays = (TextView)findViewById(R.id.settings_clear_saved_relays);
        clear_saved_relays.setOnTouchListener(clearSavedRelaysTouchListener);
        
        findViewById(R.id.settings_create_backup).setOnTouchListener(createBackupTouchListener);
        findViewById(R.id.settings_restore_backup).setOnTouchListener(restoreBackupTouchListener);
        
        plusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int size = Integer.parseInt(field_default_rcon_font_size.getText().toString());
                
                if( size < 18 ) {
                    size++;
                    field_default_rcon_font_size.setText( "" + size);
                }
                Log.d(TAG, "plusButton clicked; view ID = " + v.getId() + "; size = " + size);
            }
        });
        
        minusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int size = Integer.parseInt(field_default_rcon_font_size.getText().toString());
                
                if( size > 6 ) {
                    size--;
                    field_default_rcon_font_size.setText( "" + size);
                }
                Log.d(TAG, "minusButton clicked; view ID = " + v.getId() + "; size = " + size);
            }
        });
        
        cancelButton.requestFocus();

        showCurrentValues();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(SettingsActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if( database != null ) {
            database.close();
            database = null;
        }
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }

    public void onActivityResult( int request, int result, Intent data ) {
        switch( request ) {
            case Values.ACTIVITY_CREATE_BACKUP:
                break;
            case Values.ACTIVITY_RESTORE_BACKUP:
                showCurrentValues();
                refreshServers = true;
                break;
            default:
                break;
        }
    }
    
    public void showCurrentValues() {
        if( database == null )
            database = new DatabaseProvider(SettingsActivity.this);
        
        Bundle b = database.getSettingsAsBundle();

        Log.i(TAG, "showCurrentValues(): Applying values from Bundle " + b.toString());

        rconShowPasswords = b.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS);
        rconWarnUnsafeCommand = b.getBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND);
        rconShowSuggestions = b.getBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS);
        rconEnableHistory = b.getBoolean(Values.SETTING_RCON_ENABLE_HISTORY);
        rconVolumeButtons = b.getBoolean(Values.SETTING_RCON_VOLUME_BUTTONS);
        rconIncludeSM = b.getBoolean(Values.SETTING_RCON_INCLUDE_SM);
        showServerName = b.getBoolean(Values.SETTING_SHOW_SERVER_NAME);
        showServerIP = b.getBoolean(Values.SETTING_SHOW_SERVER_IP);
        showServerGameInfo = b.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO);
        showServerMapName = b.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME);
        showServerNumPlayers = b.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS);
        showServerTags = b.getBoolean(Values.SETTING_SHOW_SERVER_TAGS);
        showServerPing = b.getBoolean(Values.SETTING_SHOW_SERVER_PING);
        useServerAlias = b.getBoolean(Values.SETTING_USE_SERVER_ALIAS);
        validateNewServers = b.getBoolean(Values.SETTING_VALIDATE_NEW_SERVERS);

        checkbox_rcon_show_passwords.setChecked(rconShowPasswords);
        checkbox_rcon_warn_unsafe_command.setChecked(rconWarnUnsafeCommand);
        checkbox_rcon_show_suggestions.setChecked(rconShowSuggestions);
        checkbox_rcon_enable_history.setChecked(rconEnableHistory);
        checkbox_rcon_volume_buttons.setChecked(rconVolumeButtons);
        checkbox_rcon_include_sm.setChecked(rconIncludeSM);
        checkbox_show_server_name.setChecked(showServerName);
        checkbox_show_server_ip.setChecked(showServerIP);
        checkbox_show_server_game_info.setChecked(showServerGameInfo);
        checkbox_show_server_map_name.setChecked(showServerMapName);
        checkbox_show_server_num_players.setChecked(showServerNumPlayers);
        checkbox_show_server_tags.setChecked(showServerTags);
        checkbox_show_server_ping.setChecked(showServerPing);
        checkbox_use_server_alias.setChecked(useServerAlias);
        checkbox_validate_new_servers.setChecked(validateNewServers);

        field_default_query_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_PORT)));
        field_default_query_timeout.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT)));
        field_default_relay_host.setText(b.getString(Values.SETTING_DEFAULT_RELAY_HOST));
        field_default_relay_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_RELAY_PORT)));
        field_default_relay_password.setText(b.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD));
        field_default_rcon_font_size.setText(Integer.toString(b.getInt(Values.SETTING_RCON_DEFAULT_FONT_SIZE)));
    }

    public void settingCheckboxHandler( View view ) {
        boolean checked = ((CheckBox)view).isChecked();

        Log.i(TAG, "settingCheckboxHandler(): View name=" + view.toString() + "; id=" + view.getId() + "; checked=" + checked);

        switch( view.getId() ) {
            case R.id.settings_checkbox_rcon_show_passwords:
                rconShowPasswords = checked;
                break;
            case R.id.settings_checkbox_rcon_warn_unsafe_command:
                rconWarnUnsafeCommand = checked;
                break;
            case R.id.settings_checkbox_rcon_show_suggestions:
                rconShowSuggestions = checked;
                checkbox_rcon_include_sm.setEnabled(rconShowSuggestions);
                break;
            case R.id.settings_checkbox_rcon_enable_history:
                rconEnableHistory = checked;
                break;
            case R.id.settings_checkbox_rcon_volume_buttons:
                rconVolumeButtons = checked;
                break;
            case R.id.settings_checkbox_rcon_include_sm:
                rconIncludeSM = checked;
                break;
            case R.id.settings_checkbox_servers_show_name:
                showServerName = checked;
                checkbox_use_server_alias.setEnabled(showServerName);
                break;
            case R.id.settings_checkbox_servers_show_ip:
                showServerIP = checked;
                break;
            case R.id.settings_checkbox_servers_show_game:
                showServerGameInfo = checked;
                break;
            case R.id.settings_checkbox_servers_show_map:
                showServerMapName = checked;
                break;
            case R.id.settings_checkbox_servers_show_players:
                showServerNumPlayers = checked;
                break;
            case R.id.settings_checkbox_servers_show_tags:
                showServerTags = checked;
                break;
            case R.id.settings_checkbox_servers_show_ping:
                showServerPing = checked;
                break;
            case R.id.settings_checkbox_servers_use_alias:
                useServerAlias = checked;
                break;
            case R.id.settings_checkbox_validate_new_servers:
                validateNewServers = checked;
        }
    }

    public void saveSettings() {
        Bundle b = new Bundle();

        int defaultQueryPort;
        int defaultQueryTimeout;
        int defaultRelayPort;
        int defaultRconFontSize;
        String defaultRelayHost;
        String defaultRelayPassword;

        try {
            try {
                defaultQueryPort = Integer.parseInt(field_default_query_port.getText().toString());
            }
            catch( NumberFormatException nfe ) {
                defaultQueryPort = 27015;
            }

            try {
                defaultQueryTimeout = Integer.parseInt(field_default_query_timeout.getText().toString());
            }
            catch( NumberFormatException nfe ) {
                defaultQueryTimeout = 1;
            }

            try {
                defaultRelayPort = Integer.parseInt(field_default_relay_port.getText().toString());
            }
            catch( NumberFormatException nfe ) {
                defaultRelayPort = 1;
            }
            
            try {
                defaultRconFontSize = Integer.parseInt(field_default_rcon_font_size.getText().toString());
            }
            catch( NumberFormatException nfe ) {
                defaultRconFontSize = 9;
            }

            defaultRelayHost = field_default_relay_host.getText().toString();
            defaultRelayPassword = field_default_relay_password.getText().toString();

            b.putBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, rconShowPasswords);
            b.putBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, rconWarnUnsafeCommand);
            b.putBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, rconShowSuggestions);
            b.putBoolean(Values.SETTING_RCON_ENABLE_HISTORY, rconEnableHistory);
            b.putBoolean(Values.SETTING_RCON_VOLUME_BUTTONS, rconVolumeButtons);
            b.putBoolean(Values.SETTING_RCON_INCLUDE_SM, rconIncludeSM);
            b.putBoolean(Values.SETTING_SHOW_SERVER_NAME, showServerName);
            b.putBoolean(Values.SETTING_SHOW_SERVER_IP, showServerIP);
            b.putBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, showServerGameInfo);
            b.putBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, showServerMapName);
            b.putBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, showServerNumPlayers);
            b.putBoolean(Values.SETTING_SHOW_SERVER_TAGS, showServerTags);
            b.putBoolean(Values.SETTING_SHOW_SERVER_PING, showServerPing);
            b.putBoolean(Values.SETTING_USE_SERVER_ALIAS, useServerAlias);
            b.putBoolean(Values.SETTING_VALIDATE_NEW_SERVERS, validateNewServers);
            b.putInt(Values.SETTING_DEFAULT_QUERY_PORT, defaultQueryPort);
            b.putInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, defaultQueryTimeout);
            b.putInt(Values.SETTING_DEFAULT_RELAY_PORT, defaultRelayPort);
            b.putInt(Values.SETTING_RCON_DEFAULT_FONT_SIZE, defaultRconFontSize);
            b.putString(Values.SETTING_DEFAULT_RELAY_HOST, defaultRelayHost);
            b.putString(Values.SETTING_DEFAULT_RELAY_PASSWORD, defaultRelayPassword);

            Log.i(TAG, "saveSettings(): Calling updateSettings() with Bundle " + b.toString());

            if( database.updateSettings(b) ) {
                Log.i(TAG, "Success!");
                setResult(0);
            }
            else {
                Log.e(TAG, "saveSettings(): Failed to update settings in database.");
                setResult(1);
            }
        }
        catch( Exception e ) {
            Log.e(TAG, "Caught an exception while saving settings:", e);
            setResult(1);
        }
    }
    
    public void createBackup() {
        Intent backupIntent = new Intent();
        backupIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.CreateBackupActivity");
        startActivityForResult(backupIntent, Values.ACTIVITY_CREATE_BACKUP);
    }
    
    public void restoreBackup() {
        Intent restoreIntent = new Intent();
        restoreIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.RestoreBackupActivity");
        startActivityForResult(restoreIntent, Values.ACTIVITY_RESTORE_BACKUP);
    }
}