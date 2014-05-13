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

package com.dparker.apps.checkvalve;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;

public class ManageServers extends Activity
{
	private DatabaseProvider database;
	private Cursor databaseCursor;
	private TableLayout server_table;	
    private Intent thisIntent;
    private Context context;
    
	private static final short UPDATE_SERVER = 1;
	private static final short CONFIRM_DELETE = 2;
	
	private OnClickListener editButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Edit" button was clicked
	    	 */
	    
	    	long rowId = (long)v.getId();
	    	updateServer(rowId);
	    }
	};
	
	private OnClickListener deleteButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Delete" button was clicked
	    	 */
	    	
	    	long rowId = (long)v.getId();

			Intent confirmDeleteIntent = new Intent();
			confirmDeleteIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.ConfirmDelete");
			confirmDeleteIntent.putExtra("rowId", rowId);		
			startActivityForResult(confirmDeleteIntent, CONFIRM_DELETE);
	    }
	};

	private OnClickListener moveUpButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Move Up" button was clicked
	    	 */
	    
	    	long rowId = (long)v.getId();

	    	if( database.moveServerUp(rowId) )
	    	{
	    		setResult(1,thisIntent);
	    		showServerList();
	    	}
	    	else
	    	{
	    		showMessage((String)context.getString(R.string.msg_db_failure));
	    	}
	    }
	};
	
	private OnClickListener moveDownButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Move Down" button was clicked
	    	 */
	    
	    	long rowId = (long)v.getId();

	    	if( database.moveServerDown(rowId) )
	    	{
	    		setResult(1,thisIntent);
	    		showServerList();
	    	}
	    	else
	    	{
	    		showMessage((String)context.getString(R.string.msg_db_failure));
	    	}
	    }
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		thisIntent = getIntent();
		
		setResult(0,thisIntent);

		context = this;
		
		database = new DatabaseProvider(this);
		database.open();
		
		setContentView(R.layout.manageservers);

		server_table = (TableLayout)findViewById(R.id.serverTable);
		
		showServerList();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		if( ! database.isOpen() )
		    database.open();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		
		if( database.isOpen() )
		    database.close();
	}
	
    public void onActivityResult(int request, int result, Intent data)
    {
    	if( request == UPDATE_SERVER )
    	{
    		if( result == 1 )
    		{
    			setResult(1,thisIntent);
    			showServerList();
    		}
    	}
    	else if( request == CONFIRM_DELETE )
    	{
    		if( result == 1 )
    		{
    			setResult(1,thisIntent);
    			showServerList();
    		}
    	}
    	
    	return;
    }
    
	public void showServerList()
	{
		if( ! database.isOpen() )
			database.open();
		
        server_table.setVisibility(-1);
        server_table.removeAllViews();
        server_table.setVisibility(1);
        
		databaseCursor = database.getAllServers();
		databaseCursor.moveToFirst();
		
		startManagingCursor(databaseCursor);
				
		/*
		 * Loop through the servers in the database
		 */
		for( int i = 0; i < databaseCursor.getCount(); i++ )
		{
		    int rowId = (int)databaseCursor.getLong(0);
			String server = databaseCursor.getString(1);
			int port = databaseCursor.getInt(2);
		    
		    TextView serverName = new TextView(this);
		    
		    Button editButton = new Button(this);
		    Button deleteButton = new Button(this);
		    Button moveUpButton = new Button(this);
		    Button moveDownButton = new Button(this);
		    
			editButton.setOnClickListener(editButtonListener);
			deleteButton.setOnClickListener(deleteButtonListener);
			moveUpButton.setOnClickListener(moveUpButtonListener);
			moveDownButton.setOnClickListener(moveDownButtonListener);
			
            serverName.setText(server + ":" + port);
            serverName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
            serverName.setPadding(5,0,0,0);
            serverName.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            serverName.setLayoutParams(new LayoutParams(
            		LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));

            editButton.setId(rowId);
            editButton.setText(this.getText(R.string.button_edit));
            editButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8.0f);
            editButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            editButton.setTypeface(null, Typeface.BOLD);
            editButton.setLayoutParams(new LayoutParams(65,50));

            deleteButton.setId(rowId);
            deleteButton.setText(this.getText(R.string.button_delete));
            deleteButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8.0f);
            deleteButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            deleteButton.setTypeface(null, Typeface.BOLD);
            deleteButton.setLayoutParams(new LayoutParams(85,50));

            moveUpButton.setId(rowId);
            moveUpButton.setText(this.getText(R.string.button_move_up));
            moveUpButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8.0f);
            moveUpButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            moveUpButton.setTypeface(null, Typeface.BOLD);
            moveUpButton.setLayoutParams(new LayoutParams(65,50));

            moveDownButton.setId(rowId);
            moveDownButton.setText(this.getText(R.string.button_move_down));
            moveDownButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8.0f);
            moveDownButton.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            moveDownButton.setTypeface(null, Typeface.BOLD);
            moveDownButton.setLayoutParams(new LayoutParams(75,50));
            
            TableRow serverRow = new TableRow(this);
            serverRow.setId(i);
            serverRow.setLayoutParams(new LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));   

            serverRow.addView(serverName);
            serverRow.addView(editButton);
            serverRow.addView(deleteButton);
            serverRow.addView(moveUpButton);
            serverRow.addView(moveDownButton);
            
            server_table.addView(serverRow);
            
            if( ! databaseCursor.isLast() )
            	databaseCursor.moveToNext();
		}
	}

	public void updateServer(long rowId)
	{
		Intent updateServerIntent = new Intent();
		updateServerIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.UpdateServer");
		updateServerIntent.putExtra("rowId", rowId);		
		startActivityForResult(updateServerIntent, UPDATE_SERVER);
	}

	public void showMessage(String msg)
	{
		Intent messageBoxIntent = new Intent();
		messageBoxIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.MessageBox");
		messageBoxIntent.putExtra("messageText", msg);
		startActivity(messageBoxIntent);
	}
}