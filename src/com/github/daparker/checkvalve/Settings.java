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

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;

/*
 * Define the AddNewServer class
 */
public class Settings extends Activity
{
    private static final String TAG = Settings.class.getSimpleName();
    
    private boolean rconShowPasswords;
    private boolean rconWarnUnsafeCommand;
    private boolean rconShowSuggestions;
    private boolean showServerIP;
    private boolean showServerGameInfo;
    private boolean showServerMapName;
    private boolean showServerNumPlayers;
    private boolean showServerTags;
    
    private Button saveButton;
    private Button cancelButton;
    private CheckBox checkbox_rcon_show_passwords;
    private CheckBox checkbox_rcon_warn_unsafe_command;
    private CheckBox checkbox_rcon_show_suggestions;
    private CheckBox checkbox_show_server_ip;
    private CheckBox checkbox_show_server_game_info;
    private CheckBox checkbox_show_server_map_name;
    private CheckBox checkbox_show_server_num_players;
    private CheckBox checkbox_show_server_tags;
    private EditText field_default_query_port;
    private EditText field_default_query_timeout;
    private EditText field_default_relay_host;
    private EditText field_default_relay_port;
    private EditText field_default_relay_password;
    
    private DatabaseProvider db;
    
    private OnClickListener saveButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Save" button was clicked
             */

            saveSettings();
            
            if( db.isOpen() )
                db.close();
            
            finish();
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Cancel" button was clicked
             */
            if( db.isOpen() )
                db.close();
            
            finish();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.settings);
        
        setResult(1);

        db = new DatabaseProvider(Settings.this);
        db.open();
        
        saveButton = (Button)findViewById(R.id.settings_save_button);
        saveButton.setOnClickListener(saveButtonListener);
        cancelButton = (Button)findViewById(R.id.settings_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);
        checkbox_rcon_show_passwords = (CheckBox)findViewById(R.id.checkbox_rcon_show_passwords);
        checkbox_rcon_warn_unsafe_command = (CheckBox)findViewById(R.id.checkbox_rcon_warn_unsafe_command);
        checkbox_rcon_show_suggestions = (CheckBox)findViewById(R.id.checkbox_rcon_show_suggestions);
        checkbox_show_server_ip = (CheckBox)findViewById(R.id.checkbox_servers_show_ip);
        checkbox_show_server_game_info = (CheckBox)findViewById(R.id.checkbox_servers_show_game);
        checkbox_show_server_map_name = (CheckBox)findViewById(R.id.checkbox_servers_show_map);
        checkbox_show_server_num_players = (CheckBox)findViewById(R.id.checkbox_servers_show_players);
        checkbox_show_server_tags = (CheckBox)findViewById(R.id.checkbox_servers_show_tags);
        field_default_query_port = (EditText)findViewById(R.id.field_default_query_port);
        field_default_query_timeout = (EditText)findViewById(R.id.field_default_query_timeout);
        field_default_relay_host = (EditText)findViewById(R.id.field_default_relay_host);
        field_default_relay_port = (EditText)findViewById(R.id.field_default_relay_port);
        field_default_relay_password = (EditText)findViewById(R.id.field_default_relay_password);
        
        showCurrentValues();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        
        if( db.isOpen() )
            db.close();
        
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        if( ! db.isOpen() )
            db.open();
        
        saveButton = (Button)findViewById(R.id.settings_save_button);
        saveButton.setOnClickListener(saveButtonListener);
        cancelButton = (Button)findViewById(R.id.settings_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);
    }
    
    public void showCurrentValues()
    {        
        Bundle b = db.getSettingsAsBundle();
        
        Log.i(TAG, "showCurrentValues(): Applying values from Bundle " + b.toString());
        
        rconShowPasswords = b.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS);
        rconWarnUnsafeCommand = b.getBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND);
        rconShowSuggestions = b.getBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS);
        showServerIP = b.getBoolean(Values.SETTING_SHOW_SERVER_IP);
        showServerGameInfo = b.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO);
        showServerMapName = b.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME);
        showServerNumPlayers = b.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS);
        showServerTags = b.getBoolean(Values.SETTING_SHOW_SERVER_TAGS);
        
        checkbox_rcon_show_passwords.setChecked(rconShowPasswords);
        checkbox_rcon_warn_unsafe_command.setChecked(rconWarnUnsafeCommand);
        checkbox_rcon_show_suggestions.setChecked(rconShowSuggestions);
        checkbox_show_server_ip.setChecked(showServerIP);
        checkbox_show_server_game_info.setChecked(showServerGameInfo);
        checkbox_show_server_map_name.setChecked(showServerMapName);
        checkbox_show_server_num_players.setChecked(showServerNumPlayers);
        checkbox_show_server_tags.setChecked(showServerTags);
        
        field_default_query_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_PORT)));
        field_default_query_timeout.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT)));
        field_default_relay_host.setText(b.getString(Values.SETTING_DEFAULT_RELAY_HOST));
        field_default_relay_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_RELAY_PORT)));
        field_default_relay_password.setText(b.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD));
    }
    
    public void settingCheckboxHandler(View view)
    {
        boolean checked = ((CheckBox) view).isChecked();

        Log.i(TAG, "settingCheckboxHandler(): View name=" + view.toString() + "; id=" + view.getId() + "; checked=" + checked);
        
        switch( view.getId() )
        {
            case R.id.checkbox_rcon_show_passwords:
                rconShowPasswords = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set rconShowPasswords = " + rconShowPasswords);
                break;
            case R.id.checkbox_rcon_warn_unsafe_command:
                rconWarnUnsafeCommand = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set rconWarnUnsafeCommand = " + rconWarnUnsafeCommand);
                break;
            case R.id.checkbox_rcon_show_suggestions:
                rconShowSuggestions = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set rconShowSuggestions = " + rconShowSuggestions);
                break;
            case R.id.checkbox_servers_show_ip:
                showServerIP = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set showServerIP = " + showServerIP);
                break;
            case R.id.checkbox_servers_show_game:
                showServerGameInfo = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set showServerGameInfo = " + showServerGameInfo);
                break;
            case R.id.checkbox_servers_show_map:
                showServerMapName = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set showServerMapName = " + showServerMapName);
                break;
            case R.id.checkbox_servers_show_players:
                showServerNumPlayers = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set showServerNumPlayers = " + showServerNumPlayers);
                break;
            case R.id.checkbox_servers_show_tags:
                showServerTags = checked;
                Log.i(TAG, "settingCheckboxHandler(): Set showServerTags = " + showServerTags);
                break;
        }
        
        Log.d(TAG, "settingCheckboxHandler(): rconWarnUnsafeCommand = " + rconWarnUnsafeCommand);
        Log.d(TAG, "settingCheckboxHandler(): rconShowPasswords = " + rconShowPasswords);
        Log.d(TAG, "settingCheckboxHandler(): showServerIP = " + showServerIP);
        Log.d(TAG, "settingCheckboxHandler(): showServerGameInfo = " + showServerGameInfo);
        Log.d(TAG, "settingCheckboxHandler(): showServerMapName = " + showServerMapName);
        Log.d(TAG, "settingCheckboxHandler(): showServerNumPlayers = " + showServerNumPlayers);
        Log.d(TAG, "settingCheckboxHandler(): showServerTags = " + showServerTags);
    }
    
    public void saveSettings()
    {   
        Bundle b = new Bundle();

        int defaultQueryPort;
        int defaultQueryTimeout;
        int defaultRelayPort;
        String defaultRelayHost;
        String defaultRelayPassword;
        
        Log.d(TAG, "saveSettings(): rconWarnUnsafeCommand = " + rconWarnUnsafeCommand);
        Log.d(TAG, "saveSettings(): rconShowPasswords = " + rconShowPasswords);
        Log.d(TAG, "saveSettings(): showServerIP = " + showServerIP);
        Log.d(TAG, "saveSettings(): showServerGameInfo = " + showServerGameInfo);
        Log.d(TAG, "saveSettings(): showServerMapName = " + showServerMapName);
        Log.d(TAG, "saveSettings(): showServerNumPlayers = " + showServerNumPlayers);
        Log.d(TAG, "saveSettings(): showServerTags = " + showServerTags);
        
        try
        {
            try
            {
                defaultQueryPort = Integer.parseInt(field_default_query_port.getText().toString());
            }
            catch( NumberFormatException nfe )
            {
                defaultQueryPort = 27015;
            }
            
            try
            {
                defaultQueryTimeout = Integer.parseInt(field_default_query_timeout.getText().toString());
            }
            catch( NumberFormatException nfe )
            {
                defaultQueryTimeout = 1;
            }
            
            try
            {
                defaultRelayPort = Integer.parseInt(field_default_relay_port.getText().toString());
            }
            catch( NumberFormatException nfe )
            {
                defaultRelayPort = 1;
            }
            
            defaultRelayHost = field_default_relay_host.getText().toString();
            defaultRelayPassword = field_default_relay_password.getText().toString();
            
            b.putBoolean(Values.SETTING_RCON_SHOW_PASSWORDS, rconShowPasswords);
            b.putBoolean(Values.SETTING_RCON_WARN_UNSAFE_COMMAND, rconWarnUnsafeCommand);
            b.putBoolean(Values.SETTING_RCON_SHOW_SUGGESTIONS, rconShowSuggestions);
            b.putBoolean(Values.SETTING_SHOW_SERVER_IP, showServerIP);
            b.putBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO, showServerGameInfo);
            b.putBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME, showServerMapName);
            b.putBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS, showServerNumPlayers);
            b.putBoolean(Values.SETTING_SHOW_SERVER_TAGS, showServerTags);
            b.putInt(Values.SETTING_DEFAULT_QUERY_PORT, defaultQueryPort);
            b.putInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, defaultQueryTimeout);
            b.putInt(Values.SETTING_DEFAULT_RELAY_PORT, defaultRelayPort);
            b.putString(Values.SETTING_DEFAULT_RELAY_HOST, defaultRelayHost);
            b.putString(Values.SETTING_DEFAULT_RELAY_PASSWORD, defaultRelayPassword);
            
            Log.i(TAG, "saveSettings(): Calling updateSettings() with Bundle " + b.toString());
            if( db.updateSettings(b) )
            {
            	Log.i(TAG, "Success!");
                setResult(0);
            }
            else
            {
                Log.e(TAG, "saveSettings(): Failed to update settings in database.");
                setResult(1);
            }
    
            db.close();
        }
        catch( Exception e )
        {
            Log.e(TAG, "Caught an exception while saving settings:");
            Log.e(TAG, e.toString());
            
            StackTraceElement[] ste = e.getStackTrace();

            for( int i = 0; i < ste.length; i++ )
                Log.e(TAG, "    " + ste[i].toString());
            
            setResult(1);
        }
    }
}