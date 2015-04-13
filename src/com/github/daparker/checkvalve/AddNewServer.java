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
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;

/*
 * Define the AddNewServer class
 */
public class AddNewServer extends Activity
{
    private static final String TAG = AddNewServer.class.getSimpleName();

    private DatabaseProvider database;
    private Button addButton;
    private Button cancelButton;
    private Context context;

    private ProgressDialog p;

    private EditText field_server;
    private EditText field_port;
    private EditText field_timeout;
    private EditText field_rcon_password;

    private OnClickListener addButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Add" button was clicked
             */

            int server_len = field_server.getText().toString().length();
            int port_len = field_port.getText().toString().length();
            int timeout_len = field_timeout.getText().toString().length();
            int password_len = field_rcon_password.length();

            if( (server_len == 0) || (port_len == 0) || (timeout_len == 0) )
            {
                UserVisibleMessage.showMessage(AddNewServer.this, R.string.msg_empty_fields);
            }
            else
            {
                final String server = field_server.getText().toString();
                final int port = Integer.parseInt(field_port.getText().toString());
                final int timeout = Integer.parseInt(field_timeout.getText().toString());
                final String password = (password_len > 0)?field_rcon_password.getText().toString():"";

                Handler checkServerHandler = new Handler()
                {
                    String errorMsg = "";

                    public void handleMessage( Message msg )
                    {
                        if( p.isShowing() ) p.dismiss();

                        switch( msg.what )
                        {
                            case 0:
                                if( (database.insertServer(server, port, timeout, password)) > -1 )
                                {
                                    UserVisibleMessage.showMessage(AddNewServer.this, R.string.msg_success);
                                    setResult(1);
                                }
                                else
                                {
                                    errorMsg = "Database insert failed! [db=" + database.toString() + "]";
                                    errorMsg += "[params=" + server + "," + port + "," + timeout + "," + password + "]";
                                    Log.w(TAG, errorMsg);

                                    UserVisibleMessage.showMessage(AddNewServer.this, R.string.msg_db_failure);
                                }

                                finish();
                                break;
                            case 1:
                                errorMsg = (String)context.getText(R.string.msg_unknown_host) + " " + server;
                                UserVisibleMessage.showMessage(AddNewServer.this, errorMsg);
                                break;
                            case 2:
                            case 3:
                            case 4:
                                errorMsg = (String)context.getText(R.string.msg_unable_to_connect_to) + " " + server;
                                UserVisibleMessage.showMessage(AddNewServer.this, errorMsg);
                                break;
                            case 5:
                                UserVisibleMessage.showMessage(AddNewServer.this, R.string.msg_server_validation_failed);
                                break;
                            default:
                                UserVisibleMessage.showMessage(AddNewServer.this, R.string.msg_server_validation_failed);
                                break;
                        }
                    }
                };

                if( CheckValve.settings.getBoolean(Values.SETTING_VALIDATE_NEW_SERVERS) == true )
                {
                    // Show the progress dialog
                    p = ProgressDialog.show(AddNewServer.this, "", context.getText(R.string.status_verifying_server), true, false);
    
                    // Run the server query in a separate thread
                    new Thread(new ServerCheck(server, port, timeout, checkServerHandler)).start();
                }
                else
                {
                    if( (database.insertServer(server, port, timeout, password)) > -1 )
                    {
                        UserVisibleMessage.showMessage(AddNewServer.this, R.string.msg_success);
                        setResult(1);
                    }
                    else
                    {
                        String errorMsg = new String();
                        errorMsg = "Database insert failed! [db=" + database.toString() + "]";
                        errorMsg += "[params=" + server + "," + port + "," + timeout + "," + password + "]";
                        Log.w(TAG, errorMsg);

                        UserVisibleMessage.showMessage(AddNewServer.this, R.string.msg_db_failure);
                    }

                    finish();
                }
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
        
        if( database == null )
            database = new DatabaseProvider(AddNewServer.this);
        
        context = AddNewServer.this;

        setResult(0);

        setContentView(R.layout.addnewserver);

        addButton = (Button)findViewById(R.id.addServerButton);
        addButton.setOnClickListener(addButtonListener);

        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(cancelButtonListener);

        field_server = (EditText)findViewById(R.id.field_server);
        field_port = (EditText)findViewById(R.id.field_port);
        field_timeout = (EditText)findViewById(R.id.field_timeout);
        field_rcon_password = (EditText)findViewById(R.id.field_rcon_password);
        
        field_port.setText(Integer.toString(CheckValve.settings.getInt(Values.SETTING_DEFAULT_QUERY_PORT)));
        field_timeout.setText(Integer.toString(CheckValve.settings.getInt(Values.SETTING_DEFAULT_QUERY_TIMEOUT)));

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
    protected void onResume()
    {
        super.onResume();

        addButton = (Button)findViewById(R.id.addServerButton);
        addButton.setOnClickListener(addButtonListener);

        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(cancelButtonListener);
        
        if( database == null )
            database = new DatabaseProvider(AddNewServer.this);
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