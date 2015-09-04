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

package com.dparker.apps.checkvalve;

import java.io.File;
import android.app.Activity;
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
import com.dparker.apps.checkvalve.R;

public class SettingsActivity extends Activity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    private boolean rconShowPasswords;
    private boolean rconWarnUnsafeCommand;
    private boolean rconShowSuggestions;
    private boolean rconEnableHistory;
    private boolean showServerIP;
    private boolean showServerGameInfo;
    private boolean showServerMapName;
    private boolean showServerNumPlayers;
    private boolean showServerTags;
    private boolean validateNewServers;

    private Button saveButton;
    private Button cancelButton;
    private CheckBox checkbox_rcon_show_passwords;
    private CheckBox checkbox_rcon_warn_unsafe_command;
    private CheckBox checkbox_rcon_show_suggestions;
    private CheckBox checkbox_rcon_enable_history;
    private CheckBox checkbox_show_server_ip;
    private CheckBox checkbox_show_server_game_info;
    private CheckBox checkbox_show_server_map_name;
    private CheckBox checkbox_show_server_num_players;
    private CheckBox checkbox_show_server_tags;
    private CheckBox checkbox_validate_new_servers;
    private EditText field_default_query_port;
    private EditText field_default_query_timeout;
    private EditText field_default_relay_host;
    private EditText field_default_relay_port;
    private EditText field_default_relay_password;
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
    
    private OnClickListener saveButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Save" button was clicked
             */

            saveSettings();
            finish();
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Cancel" button was clicked
             */
            
            setResult(1);
            finish();
        }
    };

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
        this.setResult(1);

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
        
        checkbox_rcon_show_passwords = (CheckBox)findViewById(R.id.checkbox_rcon_show_passwords);
        checkbox_rcon_warn_unsafe_command = (CheckBox)findViewById(R.id.checkbox_rcon_warn_unsafe_command);
        checkbox_rcon_show_suggestions = (CheckBox)findViewById(R.id.checkbox_rcon_show_suggestions);
        checkbox_rcon_enable_history = (CheckBox)findViewById(R.id.checkbox_rcon_enable_history);
        checkbox_show_server_ip = (CheckBox)findViewById(R.id.checkbox_servers_show_ip);
        checkbox_show_server_game_info = (CheckBox)findViewById(R.id.checkbox_servers_show_game);
        checkbox_show_server_map_name = (CheckBox)findViewById(R.id.checkbox_servers_show_map);
        checkbox_show_server_num_players = (CheckBox)findViewById(R.id.checkbox_servers_show_players);
        checkbox_show_server_tags = (CheckBox)findViewById(R.id.checkbox_servers_show_tags);
        checkbox_validate_new_servers = (CheckBox)findViewById(R.id.checkbox_validate_new_servers);
        field_default_query_port = (EditText)findViewById(R.id.field_default_query_port);
        field_default_query_timeout = (EditText)findViewById(R.id.field_default_query_timeout);
        field_default_relay_host = (EditText)findViewById(R.id.field_default_relay_host);
        field_default_relay_port = (EditText)findViewById(R.id.field_default_relay_port);
        field_default_relay_password = (EditText)findViewById(R.id.field_default_relay_password);

        reset_do_not_show = (TextView)findViewById(R.id.reset_do_not_show);
        reset_do_not_show.setOnTouchListener(resetTouchListener);
        
        clear_saved_relays = (TextView)findViewById(R.id.clear_saved_relays);
        clear_saved_relays.setOnTouchListener(clearSavedRelaysTouchListener);
        
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

    public void showCurrentValues() {
        Bundle b = database.getSettingsAsBundle();

        Log.i(TAG, "showCurrentValues(): Applying values from Bundle " + b.toString());

        rconShowPasswords = b.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS);
        rconWarnUnsafeCommand = b.getBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND);
        rconShowSuggestions = b.getBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS);
        rconEnableHistory = b.getBoolean(Values.SETTING_RCON_ENABLE_HISTORY);
        showServerIP = b.getBoolean(Values.SETTING_SHOW_SERVER_IP);
        showServerGameInfo = b.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO);
        showServerMapName = b.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME);
        showServerNumPlayers = b.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS);
        showServerTags = b.getBoolean(Values.SETTING_SHOW_SERVER_TAGS);
        validateNewServers = b.getBoolean(Values.SETTING_VALIDATE_NEW_SERVERS);

        checkbox_rcon_show_passwords.setChecked(rconShowPasswords);
        checkbox_rcon_warn_unsafe_command.setChecked(rconWarnUnsafeCommand);
        checkbox_rcon_show_suggestions.setChecked(rconShowSuggestions);
        checkbox_rcon_enable_history.setChecked(rconEnableHistory);
        checkbox_show_server_ip.setChecked(showServerIP);
        checkbox_show_server_game_info.setChecked(showServerGameInfo);
        checkbox_show_server_map_name.setChecked(showServerMapName);
        checkbox_show_server_num_players.setChecked(showServerNumPlayers);
        checkbox_show_server_tags.setChecked(showServerTags);
        checkbox_validate_new_servers.setChecked(validateNewServers);

        field_default_query_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_PORT)));
        field_default_query_timeout.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT)));
        field_default_relay_host.setText(b.getString(Values.SETTING_DEFAULT_RELAY_HOST));
        field_default_relay_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_RELAY_PORT)));
        field_default_relay_password.setText(b.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD));
    }

    public void settingCheckboxHandler( View view ) {
        boolean checked = ((CheckBox)view).isChecked();

        Log.i(TAG, "settingCheckboxHandler(): View name=" + view.toString() + "; id=" + view.getId() + "; checked=" + checked);

        switch( view.getId() ) {
            case R.id.checkbox_rcon_show_passwords:
                rconShowPasswords = checked;
                break;
            case R.id.checkbox_rcon_warn_unsafe_command:
                rconWarnUnsafeCommand = checked;
                break;
            case R.id.checkbox_rcon_show_suggestions:
                rconShowSuggestions = checked;
                break;
            case R.id.checkbox_rcon_enable_history:
                rconEnableHistory = checked;
                break;
            case R.id.checkbox_servers_show_ip:
                showServerIP = checked;
                break;
            case R.id.checkbox_servers_show_game:
                showServerGameInfo = checked;
                break;
            case R.id.checkbox_servers_show_map:
                showServerMapName = checked;
                break;
            case R.id.checkbox_servers_show_players:
                showServerNumPlayers = checked;
                break;
            case R.id.checkbox_servers_show_tags:
                showServerTags = checked;
                break;
            case R.id.checkbox_validate_new_servers:
                validateNewServers = checked;
        }
    }

    public void saveSettings() {
        Bundle b = new Bundle();

        int defaultQueryPort;
        int defaultQueryTimeout;
        int defaultRelayPort;
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

            defaultRelayHost = field_default_relay_host.getText().toString();
            defaultRelayPassword = field_default_relay_password.getText().toString();

            b.putBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, rconShowPasswords);
            b.putBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, rconWarnUnsafeCommand);
            b.putBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, rconShowSuggestions);
            b.putBoolean(Values.SETTING_RCON_ENABLE_HISTORY, rconEnableHistory);
            b.putBoolean(Values.SETTING_SHOW_SERVER_IP, showServerIP);
            b.putBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, showServerGameInfo);
            b.putBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, showServerMapName);
            b.putBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, showServerNumPlayers);
            b.putBoolean(Values.SETTING_SHOW_SERVER_TAGS, showServerTags);
            b.putBoolean(Values.SETTING_VALIDATE_NEW_SERVERS, validateNewServers);
            b.putInt(Values.SETTING_DEFAULT_QUERY_PORT, defaultQueryPort);
            b.putInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, defaultQueryTimeout);
            b.putInt(Values.SETTING_DEFAULT_RELAY_PORT, defaultRelayPort);
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
}