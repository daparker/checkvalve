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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

public class UpdateServer extends Activity
{
	private DatabaseProvider database;
	private Cursor databaseCursor;
	private EditText field_server;
	private EditText field_port;
	private EditText field_timeout;
	private EditText field_password;
	private Button saveButton;
	private Button cancelButton;
	private Context context;

	private long rowId;
	
	private OnClickListener saveButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * Handle button click here
	    	 */
	    	
	        int server_len = field_server.getText().toString().length();
	        int port_len = field_port.getText().toString().length();
	        int timeout_len = field_timeout.getText().toString().length();
	        
	        if( (server_len == 0) || (port_len == 0) || (timeout_len == 0) )
	        {
	        	showMessage((String)context.getText(R.string.msg_empty_fields));
	        }
	        else
	        {
	        	String server = field_server.getText().toString();	        	
		    	int port = Integer.parseInt(field_port.getText().toString());
		    	int timeout = Integer.parseInt(field_timeout.getText().toString());
		    	String password = field_password.getText().toString();
		    	
		    	if( password.length() == 0 ) { password = ""; }
		    	
		    	if( updateServer(rowId, server, port, timeout, password) )
		    	{
		    		showMessage((String)context.getText(R.string.msg_success));
		    		setResult(1);
		    	}
		    	else
		    	{
		    		showMessage((String)context.getText(R.string.msg_db_failure));
		    	}
		    	
		    	finish();
	        }
	    }
	};

	private OnClickListener cancelButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	database.close();
	    	finish();
	    }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.updateserver);
		
		context = this;
		
		setResult(0);
		
		Intent thisIntent = getIntent();
		rowId = thisIntent.getLongExtra("rowId", -1);
		
		if( rowId == -1 )
		{
			showMessage((String)context.getText(R.string.msg_db_failure));
			finish();
		}
		
		saveButton = (Button)findViewById(R.id.saveButton);
		saveButton.setOnClickListener(saveButtonListener);
		
		cancelButton = (Button)findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(cancelButtonListener);
		
		field_server = (EditText)findViewById(R.id.field_server);
        field_port = (EditText)findViewById(R.id.field_port);
        field_timeout = (EditText)findViewById(R.id.field_timeout);
        field_password = (EditText)findViewById(R.id.field_password);
        
		database = new DatabaseProvider(this);
		database.open();
		
		databaseCursor = database.getServer(rowId);
		
		if( databaseCursor == null )
		{
			showMessage((String)context.getText(R.string.msg_db_failure));
			finish();
		}
				
		field_server.setText(databaseCursor.getString(1));
		field_port.setText(Integer.toString(databaseCursor.getInt(2)));
		field_timeout.setText(Integer.toString(databaseCursor.getInt(3)));
		field_password.setText(databaseCursor.getString(5));
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();	
		database.close();
		finish();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		if( ! database.isOpen() )
		    database.open();
	}
		
	public boolean updateServer(long rowId, String server, int port, int timeout, String password)
	{
    	return database.updateServer(rowId, server, port, timeout, password);
	}
	
	public void showMessage(String msg)
	{
		Intent messageBoxIntent = new Intent();
		messageBoxIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.MessageBox");
		messageBoxIntent.putExtra("messageText", msg);
		startActivity(messageBoxIntent);
	}
}