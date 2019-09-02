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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

/*
 * Define the ChatRelayDetails class
 */
public class ChatRelayDetailsActivity extends Activity {
    private static final String TAG = ChatRelayDetailsActivity.class.getSimpleName();

    private AutoCompleteTextView field_server;
    private EditText field_port;
    private EditText field_password;
    private Button connectButton;
    private Button cancelButton;
    private Intent returned;
    private DatabaseProvider database;
    private String[] previousHosts;

    private OnClickListener connectButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Connect" button was clicked
             */

            int server_len = field_server.getText().toString().length();
            int port_len = field_port.getText().toString().length();

            if( (server_len == 0) || (port_len == 0) ) {
                UserVisibleMessage.showMessage(ChatRelayDetailsActivity.this, R.string.msg_empty_fields);
            }
            else {
                String server = field_server.getText().toString().trim();
                String port = field_port.getText().toString().trim();
                String password = field_password.getText().toString().trim();

                try {
                    int portInt = Integer.parseInt(port);

                    if( portInt < 1 || portInt > 65535 ) {
                        UserVisibleMessage.showMessage(ChatRelayDetailsActivity.this, R.string.msg_bad_port_value);
                        return;
                    }
                }
                catch( NumberFormatException e ) {
                    UserVisibleMessage.showMessage(ChatRelayDetailsActivity.this, R.string.msg_bad_port_value);
                    return;
                }

                boolean alreadySaved = false;

                Log.d(TAG, "Checking if host " + server + " is already saved.");

                for( int i = 0; i < previousHosts.length; i++ ) {
                    if( previousHosts[i].equals(server) ) {
                        alreadySaved = true;
                        Log.d(TAG, "Host " + server + " matches list element " + i + "; already saved.");
                        break;
                    }
                }

                if( !alreadySaved ) {
                    Log.d(TAG, "Saving host " + server + " to database.");
                    database.putRelayHost(server);
                }

                if( password.length() == 0 )
                    password = "";

                returned.putExtra(Values.EXTRA_SERVER, server);
                returned.putExtra(Values.EXTRA_PORT, port);
                returned.putExtra(Values.EXTRA_PASSWORD, password);
                setResult(1, returned);

                finish();
            }
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener() {
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
        this.setContentView(R.layout.chatrelaydetails);

        returned = new Intent();
        Intent thisIntent = getIntent();

        if( database == null )
            database = new DatabaseProvider(ChatRelayDetailsActivity.this);

        field_server = (AutoCompleteTextView) findViewById(R.id.chatrelaydetails_field_server);
        field_port = (EditText) findViewById(R.id.chatrelaydetails_field_port);
        field_password = (EditText) findViewById(R.id.chatrelaydetails_field_password);

        if( thisIntent.getStringExtra(Values.EXTRA_SERVER).length() != 0 )
            field_server.setText(thisIntent.getStringExtra(Values.EXTRA_SERVER));

        if( thisIntent.getStringExtra(Values.EXTRA_PORT).length() != 0 )
            field_port.setText(thisIntent.getStringExtra(Values.EXTRA_PORT));

        if( thisIntent.getStringExtra(Values.EXTRA_PASSWORD).length() != 0 )
            field_password.setText(thisIntent.getStringExtra(Values.EXTRA_PASSWORD));

        connectButton = (Button) findViewById(R.id.chatrelaydetails_connect_button);
        connectButton.setOnClickListener(connectButtonListener);

        cancelButton = (Button) findViewById(R.id.chatrelaydetails_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);

        previousHosts = database.getRelayHosts();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.autocomplete_textview_custom, previousHosts);

        field_server = (AutoCompleteTextView) findViewById(R.id.chatrelaydetails_field_server);
        field_server.setAdapter(adapter);
        field_server.setThreshold(1);
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

        if( database == null )
            database = new DatabaseProvider(ChatRelayDetailsActivity.this);

        connectButton = (Button) findViewById(R.id.chatrelaydetails_connect_button);
        connectButton.setOnClickListener(connectButtonListener);

        cancelButton = (Button) findViewById(R.id.chatrelaydetails_cancel_button);
        cancelButton.setOnClickListener(cancelButtonListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        return;
    }
}