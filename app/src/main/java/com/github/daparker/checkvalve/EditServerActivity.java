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

package com.github.daparker.checkvalve;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;

public class EditServerActivity extends Activity {
    private static final String TAG = EditServerActivity.class.getSimpleName();
    private static Bundle settings;

    private DatabaseProvider database;
    private ServerRecord sr;
    private EditText field_server;
    private EditText field_port;
    private EditText field_timeout;
    private EditText field_rcon_password;
    private EditText field_nickname;
    private Button saveButton;
    private Button cancelButton;
    private String errorMsg;
    private long rowId;

    private OnClickListener saveButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * Handle button click here
             */

            int server_len = field_server.getText().toString().length();
            int port_len = field_port.getText().toString().length();
            int timeout_len = field_timeout.getText().toString().length();
            
            String server;
            String password;
            String nickname;
            int port;
            int timeout;

            if( (server_len == 0) || (port_len == 0) || (timeout_len == 0) ) {
                UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_empty_fields);
            }
            else {
                server = field_server.getText().toString().trim();
                password = field_rcon_password.getText().toString().trim();
                nickname = field_nickname.getText().toString().trim();
                
                if( password.length() == 0 ) password = "";
                
                if( nickname.length() == 0 ) {
                    nickname = "";
                }
                else {
                	// Fix for GitHub issue #10:
                	// Only check the nickname if the value of that field has changed
                	if( ! nickname.equals(sr.getServerNickname()) ) {
	                    if( database.serverNicknameExists(nickname) ) {
	                        Log.w(TAG, "saveButtonListener: Server nickname '" + nickname + "' is a duplicate!");
	                        UserVisibleMessage.showMessage(EditServerActivity.this, "The server nickname is already in use.");
	                        return;
	                    }
                	}
                }
                
                try {
                    port = Integer.parseInt(field_port.getText().toString().trim());
                    
                    if( port < 1 || port > 65535 ) {
                        UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_bad_port_value);
                        return;
                    }
                }
                catch( NumberFormatException e ) {
                    UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_bad_port_value);
                    return;
                }
                
                try {
                    timeout = Integer.parseInt(field_timeout.getText().toString().trim());
                    
                    if( timeout < 0 ) {
                        UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_bad_timeout_value);
                        return;
                    }
                }
                catch( NumberFormatException e ) {
                    UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_bad_timeout_value);
                    return;
                }

                if( database.updateServer(rowId, nickname, server, port, timeout, password) ) {
                    UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_success);
                }
                else {
                    errorMsg = "Database insert failed! [db=" + database.toString() + "]";
                    errorMsg += "[params=" + server + "," + port + "," + timeout + "," + password + "]";
                    Log.w(TAG, errorMsg);

                    UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_db_failure);
                }

                finish();
            }
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Cancel" button was clicked
             */

            finish();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.editserver);

        if( database == null )
            database = new DatabaseProvider(EditServerActivity.this);

        settings = Values.getSettings(EditServerActivity.this);
        
        Intent thisIntent = getIntent();
        rowId = thisIntent.getLongExtra(Values.EXTRA_ROW_ID, -1);

        if( rowId == -1 ) {
            UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_db_failure);
            finish();
        }

        sr = database.getServer(rowId);

        saveButton = (Button)findViewById(R.id.editserver_save_button);
        saveButton.setOnClickListener(saveButtonListener);

        cancelButton = (Button)findViewById(R.id.editserver_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);

        field_server = (EditText)findViewById(R.id.editserver_field_server);
        field_port = (EditText)findViewById(R.id.editserver_field_port);
        field_timeout = (EditText)findViewById(R.id.editserver_field_timeout);
        field_rcon_password = (EditText)findViewById(R.id.editserver_field_rcon_password);
        field_nickname = (EditText)findViewById(R.id.editserver_field_nickname);

        field_server.setText(sr.getServerURL());
        field_port.setText(Integer.toString(sr.getServerPort()));
        field_timeout.setText(Integer.toString(sr.getServerTimeout()));
        field_rcon_password.setText(sr.getServerRCONPassword());
        field_nickname.setText(sr.getServerNickname());

        if( settings.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS) == true ) {
            ((CheckBox)findViewById(R.id.editserver_checkbox_show_password)).setChecked(true);
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        else {
            ((CheckBox)findViewById(R.id.editserver_checkbox_show_password)).setChecked(false);
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(EditServerActivity.this);
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

    public void showPasswordCheckboxHandler( View view ) {
        boolean checked = ((CheckBox)view).isChecked();

        if( checked )
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        else
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }
}