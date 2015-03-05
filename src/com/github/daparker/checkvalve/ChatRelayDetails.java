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
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;

/*
 * Define the ChatRelayDetails class
 */
public class ChatRelayDetails extends Activity
{
    private EditText field_server;
    private EditText field_port;
    private EditText field_password;
    private Button connectButton;
    private Button cancelButton;
    private Intent returned;

    private OnClickListener connectButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Connect" button was clicked
             */

            int server_len = field_server.getText().toString().length();
            int port_len = field_port.getText().toString().length();

            if( (server_len == 0) || (port_len == 0) )
            {
                UserVisibleMessage.showMessage(ChatRelayDetails.this, R.string.msg_empty_fields);
            }
            else
            {
                String server = field_server.getText().toString();
                String port = field_port.getText().toString();
                String password = field_password.getText().toString();

                if( password.length() == 0 ) password = "";

                returned.putExtra("server", server);
                returned.putExtra("port", port);
                returned.putExtra("password", password);
                setResult(1, returned);
                finish();
            }
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Cancel" button was clicked
             */

            finish();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chatrelaydetails);

        returned = new Intent();
        field_server = (EditText)findViewById(R.id.field_server);
        field_port = (EditText)findViewById(R.id.field_port);
        field_password = (EditText)findViewById(R.id.field_password);

        Intent thisIntent = getIntent();

        if( !thisIntent.getStringExtra("server").isEmpty() ) field_server.setText(thisIntent.getStringExtra("server"));

        if( !thisIntent.getStringExtra("port").isEmpty() ) field_port.setText(thisIntent.getStringExtra("port"));

        if( !thisIntent.getStringExtra("password").isEmpty() )
            field_password.setText(thisIntent.getStringExtra("password"));

        setResult(0);

        connectButton = (Button)findViewById(R.id.connectButton);
        connectButton.setOnClickListener(connectButtonListener);

        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(cancelButtonListener);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        connectButton = (Button)findViewById(R.id.connectButton);
        connectButton.setOnClickListener(connectButtonListener);

        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(cancelButtonListener);
    }
}