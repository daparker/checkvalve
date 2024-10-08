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
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.dparker.apps.checkvalve.exceptions.InvalidDataTypeException;

import java.io.File;

public class SettingsActivity extends Activity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    private boolean enableNotificationLED;
    private boolean enableNotificationSound;
    private boolean enableNotificationVibrate;
    private boolean enableNotifications;
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
    private boolean useServerNickname;
    private boolean validateNewServers;
    private boolean refreshServers;
    private boolean queryServers;

    private CheckBox checkbox_enable_notification_led;
    private CheckBox checkbox_enable_notification_sound;
    private CheckBox checkbox_enable_notification_vibrate;
    private CheckBox checkbox_enable_notifications;
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
    private CheckBox checkbox_use_server_nickname;
    private CheckBox checkbox_validate_new_servers;
    private EditText field_default_query_port;
    private EditText field_default_query_timeout;
    private EditText field_default_relay_host;
    private EditText field_default_relay_port;
    private EditText field_default_relay_password;
    private EditText field_background_query_frequency;
    private TextView field_default_rcon_font_size;

    private DatabaseProvider database;

    private final OnTouchListener buttonTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent m) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            }
            else {
                v.setBackgroundColor(getResources().getColor(R.color.steam_gray));
            }

            return false;
        }
    };

    private final OnTouchListener resetTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent m) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                /*
                 * Delete marker files for 'do not show' settings
                 */

                File filesDir = SettingsActivity.this.getFilesDir();
                File f1 = new File(filesDir, Values.FILE_HIDE_CHAT_RELAY_NOTE);
                File f2 = new File(filesDir, Values.FILE_HIDE_CONSOLE_RELAY_NOTE);
                File f3 = new File(filesDir, Values.FILE_HIDE_ANDROID_VERSION_NOTE);

                f1.delete();
                f2.delete();
                f3.delete();

                UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_options_reset);
            }

            return false;
        }
    };

    private final OnTouchListener clearSavedRelaysTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent m) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                database.deleteRelayHosts();
                UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_cleared_relay_list);
            }

            return false;
        }
    };

    private final OnTouchListener createBackupTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent m) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                createBackup();
            }

            return false;
        }
    };

    private final OnTouchListener restoreBackupTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent m) {
            if( m.getAction() == MotionEvent.ACTION_DOWN ) {
                restoreBackup();
            }

            return false;
        }
    };

    private final OnClickListener saveButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Save" button was clicked
             */

            Intent resultIntent = new Intent();

            try {
                int defaultQueryPort = Integer.parseInt(field_default_query_port.getText().toString().trim());

                if( defaultQueryPort < 1 || defaultQueryPort > 65535 ) {
                    UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_bad_default_query_port_value);
                    return;
                }
            }
            catch( NumberFormatException e ) {
                UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_bad_default_query_port_value);
                return;
            }

            try {
                int defaultRelayPort = Integer.parseInt(field_default_relay_port.getText().toString().trim());

                if( defaultRelayPort < 1 || defaultRelayPort > 65535 ) {
                    UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_bad_default_relay_port_value);
                    return;
                }
            }
            catch( NumberFormatException e ) {
                UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_bad_default_relay_port_value);
                return;
            }

            try {
                int currentFreq = database.getIntSetting(DatabaseProvider.SETTINGS_BACKGROUND_QUERY_FREQUENCY);
                int newFreq = Integer.parseInt(field_background_query_frequency.getText().toString().trim());

                if( (currentFreq != newFreq) && checkbox_enable_notifications.isChecked() ) {
                    resultIntent.putExtra(Values.EXTRA_RESTART_SERVICE, true);
                }
            }
            catch( InvalidDataTypeException idte ) {
                Log.e(TAG, "saveButtonListener(): Caught an exception:", idte);
            }
            catch( NumberFormatException nfe ) {
                UserVisibleMessage.showMessage(SettingsActivity.this, R.string.msg_bad_freq_value);
                return;
            }

            if( queryServers ) {
                resultIntent.putExtra(Values.EXTRA_QUERY_SERVERS, true);
            }
            else {
                if( refreshServers ) {
                    resultIntent.putExtra(Values.EXTRA_REFRESH_SERVERS, true);
                }
            }

            // Result codes:
            // -1 = Database error
            //  0 = Canceled
            //  1 = Success
            //  2 = Some other error
            int result = saveSettings();

            setResult(result, resultIntent);
            finish();
        }
    };

    private final OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Cancel" button was clicked
             */

            // Return 0
            setResult(0);
            finish();
        }
    };

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( ViewConfiguration.get(this).hasPermanentMenuKey() )
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.setContentView(R.layout.settings);
        this.setResult(0);

        // Flag to main Activity to refresh the server list if necessary
        refreshServers = false;

        // Flag to main Activity to re-query all servers
        queryServers = false;

        if( database == null )
            database = new DatabaseProvider(SettingsActivity.this);

        Button cancelButton = findViewById(R.id.settings_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);
        cancelButton.setOnTouchListener(buttonTouchListener);
        cancelButton.setFocusable(true);
        cancelButton.setFocusableInTouchMode(true);

        Button saveButton = findViewById(R.id.settings_save_button);
        saveButton.setOnClickListener(saveButtonListener);
        saveButton.setOnTouchListener(buttonTouchListener);

        Button plusButton = findViewById(R.id.settings_rcon_font_size_plus);
        Button minusButton = findViewById(R.id.settings_rcon_font_size_minus);

        checkbox_rcon_show_passwords = findViewById(R.id.settings_checkbox_rcon_show_passwords);
        checkbox_rcon_warn_unsafe_command = findViewById(R.id.settings_checkbox_rcon_warn_unsafe_command);
        checkbox_rcon_show_suggestions = findViewById(R.id.settings_checkbox_rcon_show_suggestions);
        checkbox_rcon_enable_history = findViewById(R.id.settings_checkbox_rcon_enable_history);
        checkbox_rcon_volume_buttons = findViewById(R.id.settings_checkbox_rcon_volume_buttons);
        checkbox_rcon_include_sm = findViewById(R.id.settings_checkbox_rcon_include_sm);
        checkbox_show_server_name = findViewById(R.id.settings_checkbox_servers_show_name);
        checkbox_show_server_ip = findViewById(R.id.settings_checkbox_servers_show_ip);
        checkbox_show_server_game_info = findViewById(R.id.settings_checkbox_servers_show_game);
        checkbox_show_server_map_name = findViewById(R.id.settings_checkbox_servers_show_map);
        checkbox_show_server_num_players = findViewById(R.id.settings_checkbox_servers_show_players);
        checkbox_show_server_tags = findViewById(R.id.settings_checkbox_servers_show_tags);
        checkbox_show_server_ping = findViewById(R.id.settings_checkbox_servers_show_ping);
        checkbox_use_server_nickname = findViewById(R.id.settings_checkbox_servers_use_nickname);
        checkbox_validate_new_servers = findViewById(R.id.settings_checkbox_validate_new_servers);
        checkbox_enable_notification_led = findViewById(R.id.settings_checkbox_notification_led);
        checkbox_enable_notification_sound = findViewById(R.id.settings_checkbox_notification_sound);
        checkbox_enable_notification_vibrate = findViewById(R.id.settings_checkbox_notification_vibrate);
        checkbox_enable_notifications = findViewById(R.id.settings_checkbox_notifications);

        field_default_query_port = findViewById(R.id.settings_field_default_query_port);
        field_default_query_timeout = findViewById(R.id.settings_field_default_query_timeout);
        field_default_relay_host = findViewById(R.id.settings_field_default_relay_host);
        field_default_relay_port = findViewById(R.id.settings_field_default_relay_port);
        field_default_relay_password = findViewById(R.id.settings_field_default_relay_password);
        field_background_query_frequency = findViewById(R.id.settings_field_background_query_frequency);
        field_default_rcon_font_size = findViewById(R.id.settings_field_default_rcon_font_size);

        TextView reset_do_not_show = findViewById(R.id.settings_reset_do_not_show);
        reset_do_not_show.setOnTouchListener(resetTouchListener);

        TextView clear_saved_relays = findViewById(R.id.settings_clear_saved_relays);
        clear_saved_relays.setOnTouchListener(clearSavedRelaysTouchListener);

        findViewById(R.id.settings_create_backup).setOnTouchListener(createBackupTouchListener);
        findViewById(R.id.settings_restore_backup).setOnTouchListener(restoreBackupTouchListener);

        plusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int size = Integer.parseInt(field_default_rcon_font_size.getText().toString());

                if( size < 18 ) {
                    size++;
                    field_default_rcon_font_size.setText("" + size);
                }
                Log.d(TAG, "plusButton clicked; view ID = " + v.getId() + "; size = " + size);
            }
        });

        minusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int size = Integer.parseInt(field_default_rcon_font_size.getText().toString());

                if( size > 6 ) {
                    size--;
                    field_default_rcon_font_size.setText("" + size);
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onActivityResult(int request, int result, Intent data) {
        switch( request ) {
            case Values.ACTIVITY_CREATE_BACKUP:
                break;
            case Values.ACTIVITY_RESTORE_BACKUP:
                showCurrentValues();
                queryServers = true;
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
        useServerNickname = b.getBoolean(Values.SETTING_USE_SERVER_NICKNAME);
        validateNewServers = b.getBoolean(Values.SETTING_VALIDATE_NEW_SERVERS);
        enableNotificationLED = b.getBoolean(Values.SETTING_ENABLE_NOTIFICATION_LED);
        enableNotificationSound = b.getBoolean(Values.SETTING_ENABLE_NOTIFICATION_SOUND);
        enableNotificationVibrate = b.getBoolean(Values.SETTING_ENABLE_NOTIFICATION_VIBRATE);
        enableNotifications = b.getBoolean(Values.SETTING_ENABLE_NOTIFICATIONS);

        checkbox_rcon_show_passwords.setChecked(rconShowPasswords);
        checkbox_rcon_warn_unsafe_command.setChecked(rconWarnUnsafeCommand);
        checkbox_rcon_show_suggestions.setChecked(rconShowSuggestions);
        checkbox_rcon_enable_history.setChecked(rconEnableHistory);
        checkbox_rcon_volume_buttons.setChecked(rconVolumeButtons);
        checkbox_rcon_include_sm.setChecked(rconIncludeSM);
        checkbox_rcon_include_sm.setEnabled(rconShowSuggestions);
        checkbox_show_server_name.setChecked(showServerName);
        checkbox_show_server_ip.setChecked(showServerIP);
        checkbox_show_server_game_info.setChecked(showServerGameInfo);
        checkbox_show_server_map_name.setChecked(showServerMapName);
        checkbox_show_server_num_players.setChecked(showServerNumPlayers);
        checkbox_show_server_tags.setChecked(showServerTags);
        checkbox_show_server_ping.setChecked(showServerPing);
        checkbox_use_server_nickname.setChecked(useServerNickname);
        checkbox_use_server_nickname.setEnabled(showServerName);
        checkbox_validate_new_servers.setChecked(validateNewServers);
        checkbox_enable_notification_led.setChecked(enableNotificationLED);
        checkbox_enable_notification_led.setEnabled(enableNotifications);
        checkbox_enable_notification_sound.setChecked(enableNotificationSound);
        checkbox_enable_notification_sound.setEnabled(enableNotifications);
        checkbox_enable_notification_vibrate.setChecked(enableNotificationVibrate);
        checkbox_enable_notification_vibrate.setEnabled(enableNotifications);
        checkbox_enable_notifications.setChecked(enableNotifications);

        field_default_query_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_PORT)));
        field_default_query_timeout.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT)));
        field_default_relay_host.setText(b.getString(Values.SETTING_DEFAULT_RELAY_HOST));
        field_default_relay_port.setText(Integer.toString(b.getInt(Values.SETTING_DEFAULT_RELAY_PORT)));
        field_default_relay_password.setText(b.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD));
        field_default_rcon_font_size.setText(Integer.toString(b.getInt(Values.SETTING_RCON_DEFAULT_FONT_SIZE)));
        field_background_query_frequency.setText(Integer.toString(b.getInt(Values.SETTING_BACKGROUND_QUERY_FREQUENCY)));
        field_background_query_frequency.setEnabled(enableNotifications);
    }

    public void settingCheckboxHandler(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        if( view.getId() == R.id.settings_checkbox_rcon_show_passwords ) {
            rconShowPasswords = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_rcon_warn_unsafe_command ) {
            rconWarnUnsafeCommand = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_rcon_show_suggestions ) {
            rconShowSuggestions = checked;
            checkbox_rcon_include_sm.setEnabled(rconShowSuggestions);
        }
        else if( view.getId() == R.id.settings_checkbox_rcon_enable_history ) {
            rconEnableHistory = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_rcon_volume_buttons ) {
            rconVolumeButtons = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_rcon_include_sm ) {
            rconIncludeSM = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_show_name ) {
            showServerName = checked;
            checkbox_use_server_nickname.setEnabled(checked);
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_show_ip ) {
            showServerIP = checked;
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_show_game ) {
            showServerGameInfo = checked;
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_show_map ) {
            showServerMapName = checked;
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_show_players ) {
            showServerNumPlayers = checked;
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_show_tags ) {
            showServerTags = checked;
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_show_ping ) {
            showServerPing = checked;
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_servers_use_nickname ) {
            useServerNickname = checked;
            refreshServers = true;
        }
        else if( view.getId() == R.id.settings_checkbox_validate_new_servers ) {
            validateNewServers = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_notification_led ) {
            enableNotificationLED = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_notification_sound ) {
            enableNotificationSound = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_notification_vibrate ) {
            enableNotificationVibrate = checked;
        }
        else if( view.getId() == R.id.settings_checkbox_notifications ) {
            enableNotifications = checked;
            checkbox_enable_notification_led.setEnabled(checked);
            checkbox_enable_notification_sound.setEnabled(checked);
            checkbox_enable_notification_vibrate.setEnabled(checked);
            field_background_query_frequency.setEnabled(checked);
        }
    }

    private int saveSettings() {
        Bundle b = new Bundle();

        int defaultQueryPort;
        int defaultQueryTimeout;
        int defaultRelayPort;
        int defaultRconFontSize;
        int backgroundQueryFrequency;
        String defaultRelayHost;
        String defaultRelayPassword;

        int result;

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

            try {
                backgroundQueryFrequency = Integer.parseInt(field_background_query_frequency.getText().toString());
            }
            catch( NumberFormatException nfe ) {
                backgroundQueryFrequency = 5;
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
            b.putBoolean(Values.SETTING_USE_SERVER_NICKNAME, useServerNickname);
            b.putBoolean(Values.SETTING_VALIDATE_NEW_SERVERS, validateNewServers);
            b.putInt(Values.SETTING_DEFAULT_QUERY_PORT, defaultQueryPort);
            b.putInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT, defaultQueryTimeout);
            b.putInt(Values.SETTING_DEFAULT_RELAY_PORT, defaultRelayPort);
            b.putInt(Values.SETTING_RCON_DEFAULT_FONT_SIZE, defaultRconFontSize);
            b.putString(Values.SETTING_DEFAULT_RELAY_HOST, defaultRelayHost);
            b.putString(Values.SETTING_DEFAULT_RELAY_PASSWORD, defaultRelayPassword);
            b.putBoolean(Values.SETTING_ENABLE_NOTIFICATION_LED, enableNotificationLED);
            b.putBoolean(Values.SETTING_ENABLE_NOTIFICATION_SOUND, enableNotificationSound);
            b.putBoolean(Values.SETTING_ENABLE_NOTIFICATION_VIBRATE, enableNotificationVibrate);
            b.putBoolean(Values.SETTING_ENABLE_NOTIFICATIONS, enableNotifications);
            b.putInt(Values.SETTING_BACKGROUND_QUERY_FREQUENCY, backgroundQueryFrequency);

            Log.i(TAG, "saveSettings(): Calling updateSettings() with Bundle " + b);

            if( database.updateSettings(b) ) {
                Log.i(TAG, "Success!");
                result = 1;
            }
            else {
                Log.e(TAG, "saveSettings(): Failed to update settings in database.");
                result = -1;
            }
        }
        catch( Exception e ) {
            Log.e(TAG, "Caught an exception while saving settings:", e);
            result = 2;
        }

        return result;
    }

    public void createBackup() {
        Intent backupIntent = new Intent();
        backupIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.CreateBackupActivity");
        startActivityForResult(backupIntent, Values.ACTIVITY_CREATE_BACKUP);
    }

    public void restoreBackup() {
        Intent restoreIntent = new Intent();
        restoreIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.RestoreBackupActivity");
        startActivityForResult(restoreIntent, Values.ACTIVITY_RESTORE_BACKUP);
    }
}