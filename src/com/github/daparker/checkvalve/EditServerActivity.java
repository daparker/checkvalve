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

public class EditServerActivity extends Activity
{
    private static final String TAG = EditServerActivity.class.getSimpleName();

    private DatabaseProvider database;
    private EditText field_server;
    private EditText field_port;
    private EditText field_timeout;
    private EditText field_rcon_password;
    private Button saveButton;
    private Button cancelButton;
    private String errorMsg;
    private long rowId;

    private OnClickListener saveButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * Handle button click here
             */

            int server_len = field_server.getText().toString().length();
            int port_len = field_port.getText().toString().length();
            int timeout_len = field_timeout.getText().toString().length();

            if( (server_len == 0) || (port_len == 0) || (timeout_len == 0) )
            {
                UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_empty_fields);
            }
            else
            {
                String server = field_server.getText().toString();
                int port = Integer.parseInt(field_port.getText().toString());
                int timeout = Integer.parseInt(field_timeout.getText().toString());
                String password = field_rcon_password.getText().toString();

                if( password.length() == 0 ) password = "";

                if( database.updateServer(rowId, server, port, timeout, password) )
                {
                    UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_success);
                }
                else
                {
                    errorMsg = "Database insert failed! [db=" + database.toString() + "]";
                    errorMsg += "[params=" + server + "," + port + "," + timeout + "," + password + "]";
                    Log.w(TAG, errorMsg);

                    UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_db_failure);
                }

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

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.editserver);

        if( database == null )
            database = new DatabaseProvider(EditServerActivity.this);

        Intent thisIntent = getIntent();
        rowId = thisIntent.getLongExtra("rowId", -1);

        if( rowId == -1 )
        {
            UserVisibleMessage.showMessage(EditServerActivity.this, R.string.msg_db_failure);
            finish();
        }

        ServerRecord sr = database.getServer(rowId);

        saveButton = (Button)findViewById(R.id.saveButton);
        saveButton.setOnClickListener(saveButtonListener);

        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(cancelButtonListener);

        field_server = (EditText)findViewById(R.id.field_server);
        field_port = (EditText)findViewById(R.id.field_port);
        field_timeout = (EditText)findViewById(R.id.field_timeout);
        field_rcon_password = (EditText)findViewById(R.id.field_rcon_password);

        field_server.setText(sr.getServerName());
        field_port.setText(Integer.toString(sr.getServerPort()));
        field_timeout.setText(Integer.toString(sr.getServerTimeout()));
        field_rcon_password.setText(sr.getServerRCONPassword());

        if( CheckValve.settings.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS) == true )
        {
            ((CheckBox)findViewById(R.id.checkbox_show_password)).setChecked(true);
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        else
        {
            ((CheckBox)findViewById(R.id.checkbox_show_password)).setChecked(false);
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(EditServerActivity.this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if( database != null )
        {
            database.close();
            database = null;
        }
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged(newConfig);
        return;
    }

    public void showPasswordCheckboxHandler( View view )
    {
        boolean checked = ((CheckBox)view).isChecked();

        if( checked )
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        else
            field_rcon_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }
}