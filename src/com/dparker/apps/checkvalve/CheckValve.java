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
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class CheckValve extends Activity
{
	private ProgressDialog p;
	private ServerQuery s;
	private DatabaseProvider database;
	private Cursor databaseCursor;
	private TableLayout server_info_table;
	private TableLayout message_table;
	private TableRow[][] tableRows;
	private TableRow[] messageRows;
	private Context context;

	private long selectedServerRowId;

	// Define constants to be used as Intent request codes
	private static final short ADD_NEW_SERVER = 0;
	private static final short MANAGE_SERVERS = 1;
	private static final short UPDATE_SERVER  = 2;
	private static final short SHOW_PLAYERS   = 3;
	private static final short CONFIRM_DELETE = 4;
	private static final short RCON = 5;
	private static final short ABOUT = 6;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		context = this;
		
		database = new DatabaseProvider(this);
		database.open();
				
		selectedServerRowId = 0;

		setContentView(R.layout.main);

		server_info_table = (TableLayout)findViewById(R.id.server_info_table);
		message_table = (TableLayout)findViewById(R.id.message_table);
		
		message_table.setVisibility(-1);

       	queryServers();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		selectedServerRowId = 0;
		
		// Open the database
		if( ! database.isOpen() )
			database.open();
	}

	@Override
	public void onPause()
	{
		super.onPause();
    	
		// Close the database
		if( database.isOpen() )
		    database.close();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		databaseCursor = database.getAllServers();
		startManagingCursor(databaseCursor);
		
	    switch (item.getItemId()) {
	    	// "Quit" option was selected
	        case R.id.quit:
	            quit();
	            return true;
	        // "Add New Server" option was selected
	        case R.id.new_server:
	        	addNewServer();
	            return true;
	        // "Manage Server List" option was selected
	        case R.id.manage_servers:
	        	if( databaseCursor.getCount() == 0 )
	        		showMessage((String)context.getString(R.string.msg_empty_server_list));
	        	else
	        		manageServers();
	            return true;
	        // "Player Search" option was selected
	        case R.id.player_search:
	        	if( databaseCursor.getCount() == 0 )
	        		showMessage((String)context.getString(R.string.msg_empty_server_list));
	        	else
	        		playerSearch();
	            return true;
	        // "Refresh" option was selected
	        case R.id.refresh:
           		queryServers();
	            return true;
		    // "About" option was selected
	        case R.id.about:
           		about();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		selectedServerRowId = (long)v.getId();
		
		// Inflate the context menu
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		long id = selectedServerRowId;
		
		switch( item.getItemId() )
		{
			// "RCON" option was selected
			case R.id.rcon:
				rcon(id);
				return true;
			// "Show Players" option was selected
			case R.id.show_players:
				showPlayers(id);
				return true;
			// "Edit Server" option was selected
			case R.id.edit_server:
				updateServer(id);
				return true;
			// "Move Up" option was selected
			case R.id.move_up:
				moveServerUp(id);
				return true;
			// "Move Down" option was selected
			case R.id.move_down:
				moveServerDown(id);
				return true;
			// "Delete Server" option was selected
			case R.id.delete_server:
		    	deleteServer(id);
		    	return true;
		    // "Cancel" option was selected
			case R.id.cancel:
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
	
    public void onActivityResult(int request, int result, Intent data)
    {    	
    	if( request == ADD_NEW_SERVER )
    	{
    		if( result == 1 )
    			queryServers();
    	}
    	else if( request == MANAGE_SERVERS )
    	{
    		if( result == 1 )
    			queryServers();
    	}
    	else if( request == UPDATE_SERVER )
    	{
    		if( result == 1 )
    			queryServers();
    	}
    	else if( request == SHOW_PLAYERS )
    	{
    		switch( result )
    		{
    			case -1:
    				showMessage((String)this.getText(R.string.general_exception));
    				break;
    			case -2:
    				showMessage((String)this.getText(R.string.msg_no_players));
    				break;
    			case -3:
    				showMessage((String)this.getText(R.string.msg_no_players));
    				break;
    			default:
    				break;
    		}
    	}
    	else if( request == CONFIRM_DELETE )
    	{
    		if( result == 1 )
    			queryServers();
    	}
    	else if( request == RCON )
    	{
    		return;
    	}
    	else if( request == ABOUT )
    	{
    		return;
    	}
    }
    
    // Define the touch listener for table rows
    private OnTouchListener tableRowTouchListener = new OnTouchListener()
    {
    	public boolean onTouch(View v, MotionEvent m)
    	{    		
    		int id = v.getId();
    		int action = m.getAction();
    		int count = server_info_table.getChildCount();
    		
    		// Set the background color of each row to gray if the touch action is "UP"
    		if( action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL )
    		{
	    		for( int i = 0; i < count; i++ )
	    			if( server_info_table.getChildAt(i).getId() == id )
	    				server_info_table.getChildAt(i).setBackgroundResource(R.color.steam_gray);
    		}
    		// Set the background color of each row to blue if the touch action is "DOWN"
    		else
    		{
	    		for( int i = 0; i < count; i++ )
	    			if( server_info_table.getChildAt(i).getId() == id )
	    				server_info_table.getChildAt(i).setBackgroundResource(R.color.steam_blue);
    		}
    		    		
    		return false;
        }
    };
    
    // Define the focus change listener for table rows
    private OnFocusChangeListener tableRowFocusChangeListener = new OnFocusChangeListener()
    {
    	public void onFocusChange(View v, boolean f)
    	{    		
    		int id = v.getId();
    		int count = server_info_table.getChildCount();
    		
    		// If any row has focus then set its background color to blue
    		for( int i = 0; i < count; i++ )
    			if( server_info_table.getChildAt(i).getId() == id )
    				server_info_table.getChildAt(i).setBackgroundResource((f)?R.color.steam_blue:R.color.steam_gray);
        }
    };
    
	public void queryServers()
	{
        // Make sure the database is open
		if( ! database.isOpen() )
			database.open();
        
        // Clear the server info table
    	server_info_table.setVisibility(-1);
        server_info_table.removeAllViews();
        server_info_table.setVisibility(1);

        // Clear the messages table
        message_table.removeAllViews();

        databaseCursor = database.getAllServers();        
		databaseCursor.moveToFirst();
		
		startManagingCursor(databaseCursor);

		if( databaseCursor.getCount() == 0 )
		{
			// Show the "Add New Server" dialog
			addNewServer();
		}
		else
		{
			// Show the progress dialog
			p = ProgressDialog.show(this, "", context.getText(R.string.status_querying_servers), true, false);
        
			tableRows = new TableRow[databaseCursor.getCount()][6];
			messageRows = new TableRow[databaseCursor.getCount()];
		
			// Run the server queries in a new thread
			s = new ServerQuery(context, databaseCursor, tableRows, messageRows, progressHandler);
			s.start();
		}
	}
    
    // Handler for the server query thread
    Handler progressHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
        	/*
        	 * Build and display the messages table if there are errors
        	 * to be displayed
        	 */
        	if( messageRows[0] != null )
        	{
        		int m = 0;
        		
        		while( (m < messageRows.length) && (messageRows[m] != null) )
        			message_table.addView(messageRows[m++]);
        		
        		message_table.setVisibility(1);
        	}
            
        	/*
        	 * Build and display the query results table
        	 */
            for( int i = 0; i < tableRows.length; i++ )
            {
            	if( tableRows[i] != null )
            	{
            		TextView spacer = new TextView(context);
                	
                    spacer.setId(0);
                    spacer.setText("");
                    spacer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    spacer.setLayoutParams(new LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT));
                    
                    TableRow spacerRow = new TableRow(context);
                    spacerRow.setId(0);
                    spacerRow.setLayoutParams(new LayoutParams(
                            LayoutParams.FILL_PARENT,
                            LayoutParams.WRAP_CONTENT));
                    
                    spacerRow.addView(spacer);
                    
    	            registerForContextMenu(tableRows[i][0]);
    	            registerForContextMenu(tableRows[i][1]);
    	            registerForContextMenu(tableRows[i][2]);
    	            registerForContextMenu(tableRows[i][3]);
    	            registerForContextMenu(tableRows[i][4]);
    	            registerForContextMenu(tableRows[i][5]);
    	            
    	            tableRows[i][0].setOnTouchListener(tableRowTouchListener);
    	            tableRows[i][1].setOnTouchListener(tableRowTouchListener);
    	            tableRows[i][2].setOnTouchListener(tableRowTouchListener);
    	            tableRows[i][3].setOnTouchListener(tableRowTouchListener);
    	            tableRows[i][4].setOnTouchListener(tableRowTouchListener);
    	            tableRows[i][5].setOnTouchListener(tableRowTouchListener);

    	            tableRows[i][0].setFocusable(true);
    	            tableRows[i][0].setOnFocusChangeListener(tableRowFocusChangeListener);

    	            tableRows[i][1].setFocusable(false);
    	            tableRows[i][2].setFocusable(false);
    	            tableRows[i][3].setFocusable(false);
    	            tableRows[i][4].setFocusable(false);
    	            tableRows[i][5].setFocusable(false);
    	            
    	            server_info_table.addView(spacerRow, new TableLayout.LayoutParams(
    	                    LayoutParams.FILL_PARENT,
    	                    LayoutParams.WRAP_CONTENT));
    	            
    	            server_info_table.addView(tableRows[i][0], new TableLayout.LayoutParams(
    	                    LayoutParams.FILL_PARENT,
    	                    LayoutParams.WRAP_CONTENT));
    	
    	            server_info_table.addView(tableRows[i][1], new TableLayout.LayoutParams(
    	                    LayoutParams.FILL_PARENT,
    	                    LayoutParams.WRAP_CONTENT));
    	            
    	            server_info_table.addView(tableRows[i][2], new TableLayout.LayoutParams(
    	                    LayoutParams.FILL_PARENT,
    	                    LayoutParams.WRAP_CONTENT));
    	            
    	            server_info_table.addView(tableRows[i][3], new TableLayout.LayoutParams(
    	                    LayoutParams.FILL_PARENT,
    	                    LayoutParams.WRAP_CONTENT));
    	            
    	            server_info_table.addView(tableRows[i][4], new TableLayout.LayoutParams(
    	                    LayoutParams.FILL_PARENT,
    	                    LayoutParams.WRAP_CONTENT));

    	            server_info_table.addView(tableRows[i][5], new TableLayout.LayoutParams(
    	                    LayoutParams.FILL_PARENT,
    	                    LayoutParams.WRAP_CONTENT));
            	}
            }
            
            // Dismiss the progress dialog
            p.dismiss();
        }
    };
    
    public void quit()
    {
    	// Close the database
    	if( database.isOpen() )
    		database.close();
    	
    	// Finish this activity
    	finish();
    }
    
    public void addNewServer()
    {
		Intent addNewServerIntent = new Intent();
		addNewServerIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.AddNewServer");
		startActivityForResult(addNewServerIntent, ADD_NEW_SERVER);
    }
    
	public void manageServers()
	{
		Intent manageServersIntent = new Intent();
		manageServersIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.ManageServers");
		startActivityForResult(manageServersIntent, MANAGE_SERVERS);
	}
    
	public void showPlayers(long rowId)
	{
		Intent showPlayersIntent = new Intent();
		showPlayersIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.QueryPlayers");
		showPlayersIntent.putExtra("rowId", rowId);
		startActivityForResult(showPlayersIntent, SHOW_PLAYERS);
	}
	
    public void playerSearch()
    {
		Intent playerSearchIntent = new Intent();
		playerSearchIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.PlayerSearch");
		startActivity(playerSearchIntent);
    }
    
	public void updateServer(long rowId)
	{
		Intent updateServerIntent = new Intent();
		updateServerIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.UpdateServer");
		updateServerIntent.putExtra("rowId", rowId);		
		startActivityForResult(updateServerIntent, UPDATE_SERVER);
	}
	
	public void moveServerUp(long rowId)
	{
		if( database.moveServerUp(rowId) )
			queryServers();
		else
			showMessage((String)this.getString(R.string.msg_db_failure));
	}

	public void moveServerDown(long rowId)
	{	
		if( database.moveServerDown(rowId) )
			queryServers();
		else
			showMessage((String)this.getString(R.string.msg_db_failure));
	}
	
	public void deleteServer(long rowId)
	{
		Intent confirmDeleteIntent = new Intent();
		confirmDeleteIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.ConfirmDelete");
		confirmDeleteIntent.putExtra("rowId", rowId);		
		startActivityForResult(confirmDeleteIntent, CONFIRM_DELETE);
	}
	
	public void rcon(long rowId)
	{
		databaseCursor = database.getServer(rowId);
		
		String s = databaseCursor.getString(1);
		int p = databaseCursor.getInt(2);
		int t = databaseCursor.getInt(3);
		String r = databaseCursor.getString(5);
		
		Intent rconIntent = new Intent();
		rconIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.RconUI");
		rconIntent.putExtra("server", s);
		rconIntent.putExtra("port", p);
		rconIntent.putExtra("timeout", t);
		rconIntent.putExtra("password", r);
		startActivityForResult(rconIntent, RCON);
	}

	public void about()
	{
		Intent aboutIntent = new Intent();
		aboutIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.About");
		startActivityForResult(aboutIntent, ABOUT);
	}
	
	public void showMessage(String msg)
	{
		Intent messageBoxIntent = new Intent();
		messageBoxIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.MessageBox");
		messageBoxIntent.putExtra("messageText", msg);
		startActivity(messageBoxIntent);
	}
}