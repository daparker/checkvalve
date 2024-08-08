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

package com.dparker.apps.checkvalve;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

/*
 * Define the AddNewServer class
 */
@SuppressLint("HandlerLeak")
public class AddServerActivity extends Activity {
    private static final String TAG = AddServerActivity.class.getSimpleName();
    private Bundle settings;

    private DatabaseProvider database;
    private Button addButton;
    private Button cancelButton;

    private ProgressDialog p;

    private EditText field_nickname;
    private EditText field_server;
    private EditText field_port;
    private EditText field_timeout;
    private EditText field_rcon_password;

    private final OnClickListener addButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Add" button was clicked
             */

            int server_len = field_server.getText().toString().length();
            int port_len = field_port.getText().toString().length();
            int timeout_len = field_timeout.getText().toString().length();
            int password_len = field_rcon_password.length();
            int nickname_len = field_nickname.length();

            final String server;
            final String password;
            final String nickname;
            final int port;
            final int timeout;

            if( (server_len == 0) || (port_len == 0) || (timeout_len == 0) ) {
                UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_empty_fields);
            }
            else {
                server = field_server.getText().toString().trim();
                password = (password_len > 0) ? field_rcon_password.getText().toString().trim() : "";
                nickname = (nickname_len > 0) ? field_nickname.getText().toString().trim() : "";

                if( ! nickname.isEmpty() ) {
                    if( database.serverNicknameExists(nickname) ) {
                        Log.w(TAG, "addButtonListener: Server nickname '" + nickname + "' is a duplicate!");
                        UserVisibleMessage.showMessage(AddServerActivity.this, "The server nickname is already in use.");
                        return;
                    }
                }

                try {
                    port = Integer.parseInt(field_port.getText().toString().trim());

                    if( port < 1 || port > 65535 ) {
                        UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_bad_port_value);
                        return;
                    }
                }
                catch( NumberFormatException e ) {
                    UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_bad_port_value);
                    return;
                }

                try {
                    timeout = Integer.parseInt(field_timeout.getText().toString().trim());

                    if( timeout < 0 ) {
                        UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_bad_timeout_value);
                        return;
                    }
                }
                catch( NumberFormatException e ) {
                    UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_bad_timeout_value);
                    return;
                }

                Handler checkServerHandler = new Handler() {
                    String errorMsg = "";

                    public void handleMessage(Message msg) {
                        if( p.isShowing() ) p.dismiss();

                        switch( msg.what ) {
                            case 0:
                                if( (database.insertServer(nickname, server, port, timeout, password)) > -1 ) {
                                    UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_success);
                                    setResult(1);
                                }
                                else {
                                    errorMsg = "Database insert failed! [db=" + database.toString() + "]";
                                    errorMsg += "[params=" + server + "," + port + "," + timeout + "," + password + "]";
                                    Log.w(TAG, errorMsg);

                                    UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_db_failure);
                                }

                                finish();
                                break;
                            case 1:
                                errorMsg = String.format(AddServerActivity.this.getString(R.string.msg_unknown_host), server);
                                UserVisibleMessage.showMessage(AddServerActivity.this, errorMsg);
                                break;
                            case 2:
                            case 3:
                            case 4:
                                errorMsg = String.format(AddServerActivity.this.getString(R.string.msg_unable_to_connect_to), server);
                                UserVisibleMessage.showMessage(AddServerActivity.this, errorMsg);
                                break;
                            //case 5:
                            //    UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_server_validation_failed);
                            //    break;
                            default:
                                UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_server_validation_failed);
                                break;
                        }
                    }
                };

                if( settings.getBoolean(Values.SETTING_VALIDATE_NEW_SERVERS) ) {
                    // Show the progress dialog
                    p = ProgressDialog.show(AddServerActivity.this, "", AddServerActivity.this.getText(R.string.status_verifying_server), true, false);

                    // Run the server query in a separate thread
                    new Thread(new ServerCheck(server, port, timeout, checkServerHandler)).start();
                }
                else {
                    if( (database.insertServer(nickname, server, port, timeout, password)) > -1 ) {
                        UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_success);
                        setResult(1);
                    }
                    else {
                        String errorMsg = "Database insert failed! [db=" + database.toString() + "]";
                        errorMsg += "[params=" + server + "," + port + "," + timeout + "," + password + "]";
                        Log.w(TAG, errorMsg);

                        UserVisibleMessage.showMessage(AddServerActivity.this, R.string.msg_db_failure);
                    }

                    finish();
                }
            }
        }
    };

    private final OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Cancel" button was clicked
             */

            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.addnewserver);

        settings = Values.getSettings(AddServerActivity.this);

        if( database == null ) database = new DatabaseProvider(AddServerActivity.this);

        addButton = findViewById(R.id.addnewserver_add_button);
        addButton.setOnClickListener(addButtonListener);

        cancelButton = findViewById(R.id.addnewserver_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);

        field_server = findViewById(R.id.addnewserver_field_server);
        field_port = findViewById(R.id.addnewserver_field_port);
        field_timeout = findViewById(R.id.addnewserver_field_timeout);
        field_rcon_password = findViewById(R.id.addnewserver_field_rcon_password);
        field_nickname = findViewById(R.id.addnewserver_field_nickname);

        field_port.setText(Integer.toString(settings.getInt(Values.SETTING_DEFAULT_QUERY_PORT)));
        field_timeout.setText(Integer.toString(settings.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT)));

        if( settings.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS) ) {
            ((CheckBox) findViewById(R.id.addnewserver_checkbox_show_password)).setChecked(true);
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        else {
            ((CheckBox) findViewById(R.id.addnewserver_checkbox_show_password)).setChecked(false);
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
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
    protected void onResume() {
        super.onResume();

        addButton = findViewById(R.id.addnewserver_add_button);
        addButton.setOnClickListener(addButtonListener);

        cancelButton = findViewById(R.id.addnewserver_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);

        if( database == null ) database = new DatabaseProvider(AddServerActivity.this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void showPasswordCheckboxHandler(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        if( checked )
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        else
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }
}