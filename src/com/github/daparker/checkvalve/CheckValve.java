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

package com.github.daparker.checkvalve;

import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import com.github.daparker.checkvalve.R;

@SuppressLint("HandlerLeak")
public class CheckValve extends Activity {
    private static final String TAG = CheckValve.class.getSimpleName();

    private static boolean debugMode = false;
    public static Bundle settings;

    private ProgressDialog p;
    private DatabaseProvider database;
    private TableLayout server_info_table;
    private TableLayout message_table;
    private QueryDebugLog debugLog;
    private Intent serviceIntent;
    private long selectedServerRowId;

    @SuppressLint({ "InlinedApi", "NewApi" })
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        Log.i(TAG, "Starting CheckValve.");

        super.onCreate(savedInstanceState);
        
        if( android.os.Build.VERSION.SDK_INT < 11 ) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            UserVisibleMessage.showNote(
                    CheckValve.this,
                    Values.FILE_HIDE_ANDROID_VERSION_NOTE,
                    R.string.note_android_version_support);
        }
        else if( android.os.Build.VERSION.SDK_INT >= 14 ) {
            if( ViewConfiguration.get(this).hasPermanentMenuKey() )
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        setContentView(R.layout.main);

        if( database == null )
            database = new DatabaseProvider(CheckValve.this);

        selectedServerRowId = 0;
        server_info_table = (TableLayout)findViewById(R.id.checkvalve_server_info_table);
        message_table = (TableLayout)findViewById(R.id.checkvalve_message_table);
        message_table.setVisibility(View.INVISIBLE);
        
        this.findViewById(R.id.checkvalve_debug_button).setOnClickListener(debugButtonListener);
        
        TextView titleBar = (TextView)findViewById(R.id.checkvalve_title);
        titleBar.setOnLongClickListener(titleBarClickListener);
        
        getSettings();
        queryServers();
    }

    @Override
    public void onResume() {
        super.onResume();

        selectedServerRowId = 0;

        if( database == null )
            database = new DatabaseProvider(CheckValve.this);
        
        getSettings();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Dismiss the progress dialog to avoid a leaked window
        if( p != null ) p.dismiss();

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

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        int menuResId;
        
        if( debugMode == true )
            menuResId = R.menu.main_menu_debug;
        else
            menuResId = R.menu.main_menu;
        
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.main_menu, menu);
        inflater.inflate(menuResId, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.quit:
                // "Quit" option was selected
                quit();
                break;

            case R.id.new_server:
                // "Add New Server" option was selected
                addNewServer();
                break;

            case R.id.manage_servers:
                // "Manage Server List" option was selected
                if( database.getServerCount() == 0 )
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_empty_server_list);
                else
                    manageServers();

                break;

            case R.id.player_search:
                // "Player Search" option was selected
                if( database.getServerCount() == 0 )
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_empty_server_list);
                else
                    playerSearch();

                break;

            case R.id.refresh:
                // "Refresh" option was selected
                queryServers();
                break;

            case R.id.about:
                // "About" option was selected
                about();
                break;

            case R.id.settings:
                // "Settings" option was selected
                settings();
                break;
                
            case R.id.debug:
                // "Debug" option was clicked
                showDebugConsole();
                break;
                
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu(menu, v, menuInfo);

        selectedServerRowId = (long)v.getId();

        // Inflate the context menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        long id = selectedServerRowId;

        switch( item.getItemId() ) {
            case R.id.rcon:
                // "RCON" option was selected
                rcon(id);
                break;
            case R.id.view_chat:
                // "View Chat" option was selected
                chat(id);
                break;
            case R.id.show_players:
                // "Show Players" option was selected
                showPlayers(id);
                break;
            case R.id.edit_server:
                // "Edit Server" option was selected
                updateServer(id);
                break;
            case R.id.delete_server:
                // "Delete Server" option was selected
                deleteServer(id);
                break;
            case R.id.cancel:
                // "Cancel" option was selected
                break;
            default:
                return super.onContextItemSelected(item);
        }

        return true;
    }

    public void onActivityResult( int request, int result, Intent data ) {
        if( database == null ) database = new DatabaseProvider(CheckValve.this);

        switch( request ) {
            case Values.ACTIVITY_RCON:
            case Values.ACTIVITY_CHAT:
            case Values.ACTIVITY_ABOUT:
            case Values.ACTIVITY_SHOW_PLAYERS:
            case Values.ACTIVITY_DEBUG_CONSOLE:
            case Values.ACTIVITY_SHOW_NOTE:
                break;

            case Values.ACTIVITY_ADD_NEW_SERVER:
            case Values.ACTIVITY_MANAGE_SERVERS:
            case Values.ACTIVITY_UPDATE_SERVER:
            case Values.ACTIVITY_CONFIRM_DELETE:
                if( result == 1 ) queryServers();
                break;

            case Values.ACTIVITY_SETTINGS:
                if( result == -1 ) {
                    // Database error
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_db_failure);
                }
                else if( result == 0 ) {
                    // "Cancel" was clicked
                }
                else if( result == 1 ) {
                    // Success
                    getSettings();

                    if( data.getBooleanExtra(Values.EXTRA_QUERY_SERVERS, false) ) {
                        // Re-query all servers
                        queryServers();
                    }
                    else {
                        if( data.getBooleanExtra(Values.EXTRA_REFRESH_SERVERS, false) ) {
                            // Refresh the existing view
                            refreshView();
                        }
                    }
                    
                    // Background service is only supported on Honeycomb and above
                    if( android.os.Build.VERSION.SDK_INT >= 11 ) {
                        if( data.getBooleanExtra(Values.EXTRA_RESTART_SERVICE, false) ) {
                            restartService();
                        }
                    }
                }
                else {
                    // Some other error
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                }
                break;
            default:
                break;
        }
    }

    // Define the touch listener for table rows
    private OnTouchListener tableRowTouchListener = new OnTouchListener() {
        public boolean onTouch( View v, MotionEvent m ) {
            int id = v.getId();
            int action = m.getAction();
            int count = server_info_table.getChildCount();

            // Set the background color of each row to gray if the touch action is "UP"
            if( action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ) {
                for( int i = 0; i < count; i++ )
                    if( server_info_table.getChildAt(i).getId() == id )
                        server_info_table.getChildAt(i).setBackgroundResource(R.color.steam_gray);
            }
            // Set the background color of each row to blue if the touch action is "DOWN"
            else {
                for( int i = 0; i < count; i++ )
                    if( server_info_table.getChildAt(i).getId() == id )
                        server_info_table.getChildAt(i).setBackgroundResource(R.color.steam_blue);
            }

            return false;
        }
    };

    // Define the focus change listener for table rows
    private OnFocusChangeListener tableRowFocusChangeListener = new OnFocusChangeListener() {
        public void onFocusChange( View v, boolean f ) {
            int id = v.getId();
            int count = server_info_table.getChildCount();

            // If any row has focus then set its background color to blue
            for( int i = 0; i < count; i++ )
                if( server_info_table.getChildAt(i).getId() == id )
                    server_info_table.getChildAt(i).setBackgroundResource((f)?R.color.steam_blue:R.color.steam_gray);
        }
    };

    private OnLongClickListener titleBarClickListener = new OnLongClickListener() {
        public boolean onLongClick( View v ) {
            toggleDebugMode();
            return true;
        }
    };
    
    private OnClickListener debugButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            v.setBackgroundColor(CheckValve.this.getResources().getColor(R.color.steam_blue));
            showDebugConsole();
            v.setBackgroundColor(CheckValve.this.getResources().getColor(android.R.color.transparent));
        }
    };
    
    //@SuppressWarnings("deprecation")
    public void queryServers() {
    	if( debugMode )
    	    debugLog = new QueryDebugLog();
    	
        // Clear the server info table
        server_info_table.setVisibility(View.INVISIBLE);
        server_info_table.removeAllViews();
        server_info_table.setVisibility(View.VISIBLE);

        // Clear the messages table
        message_table.removeAllViews();

        int count = (int)database.getServerCount();

        if( count == 0 ) {
            addNewServer();
        }
        else {
            // Show the progress dialog
            p = ProgressDialog.show(this, "", getText(R.string.status_querying_servers), true, false);

            // Run the server queries in a separate thread
            new Thread(new ServerQuery(CheckValve.this, progressHandler, debugMode, debugLog)).start();
        }
    }

    // Handler for the server query thread
    Handler progressHandler = new Handler() {
        public void handleMessage( Message msg ) {            
            message_table.setVisibility(View.GONE);
            server_info_table.setVisibility(View.GONE);
            
            // A negative "what" code indicates the server query thread failed
            if( msg.what < 0 ) {
                p.dismiss();
                UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                return;
            }
            
            Bundle b = (Bundle)msg.obj;            
            ArrayList<String> messages = b.getStringArrayList(Values.MESSAGES);
            ServerInfo[] serverInfo = (ServerInfo[])b.getParcelableArray(Values.SERVER_INFO);
            
            /*
             * Build and display the messages table if there are errors to be displayed
             */
            if( ! messages.isEmpty() ) {
                int m = 0;

                for( m = 0; m < messages.size(); m++ ) {
                    TextView errorMessage = new TextView(CheckValve.this);

                    errorMessage.setId(Integer.MAX_VALUE);
                    errorMessage.setText(messages.get(m));
                    errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    errorMessage.setPadding(3, 0, 3, 0);
                    errorMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    // Create a TableRow and give it an ID
                    TableRow messageRow = new TableRow(CheckValve.this);
                    messageRow.setId(Integer.MAX_VALUE);
                    messageRow.setBackgroundResource(R.color.translucent_red);
                    messageRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    messageRow.addView(errorMessage);
                    
                    message_table.addView(messageRow);
                }

                message_table.setVisibility(View.VISIBLE);
            }

            /*
             * Build and display the query results table
             */
            for( int i = 0; i < serverInfo.length; i++ ) {
                if( serverInfo[i] != null ) {
                    String serverNickname = serverInfo[i].getNickame();
                    String serverName = serverInfo[i].getName();
                    TextView serverLabel = new TextView(CheckValve.this);
                    TextView serverValue = new TextView(CheckValve.this);
                    serverLabel.setId(i * 200);
                    serverLabel.setText(CheckValve.this.getText(R.string.label_server));
                    serverLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    serverLabel.setPadding(3, 0, 3, 0);
                    serverLabel.setTypeface(null, Typeface.BOLD);
                    serverLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    serverValue.setId(i * 300);
                    
                    if( settings.getBoolean(Values.SETTING_USE_SERVER_NICKNAME) ) {
                        serverValue.setText((serverNickname.length() > 0)?serverNickname:serverName);
                    }
                    else {
                        serverValue.setText(serverName);
                    }
                    
                    serverValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    serverValue.setPadding(3, 0, 3, 0);
                    serverValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    
                    String serverIP = serverInfo[i].getAddr();
                    int serverPort = serverInfo[i].getPort();
                    TextView ipLabel = new TextView(CheckValve.this);
                    TextView ipValue = new TextView(CheckValve.this);
                    ipLabel.setId(i * 400);
                    ipLabel.setText(CheckValve.this.getText(R.string.label_ip));
                    ipLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    ipLabel.setPadding(3, 0, 3, 0);
                    ipLabel.setTypeface(null, Typeface.BOLD);
                    ipLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    ipValue.setId(i * 500);
                    ipValue.setText(serverIP + ":" + Integer.toString(serverPort));
                    ipValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    ipValue.setPadding(3, 0, 3, 0);
                    ipValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    
                    String serverGame = serverInfo[i].getGame();
                    String gameVersion = serverInfo[i].getVersion();
                    TextView gameLabel = new TextView(CheckValve.this);
                    TextView gameValue = new TextView(CheckValve.this);
                    gameLabel.setId(i * 600);
                    gameLabel.setText(CheckValve.this.getText(R.string.label_game));
                    gameLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    gameLabel.setPadding(3, 0, 3, 0);
                    gameLabel.setTypeface(null, Typeface.BOLD);
                    gameLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    gameValue.setId(i * 700);
                    gameValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    gameValue.setPadding(3, 0, 3, 0);
                    gameValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    if( gameVersion.length() > 0 )
                        gameValue.setText(serverGame + " [" + gameVersion + "]");
                    else
                        gameValue.setText(serverGame);
                    
                    String serverMap = serverInfo[i].getMap();
                    TextView mapLabel = new TextView(CheckValve.this);
                    TextView mapValue = new TextView(CheckValve.this);
                    mapLabel.setId(i * 800);
                    mapLabel.setText(CheckValve.this.getText(R.string.label_map));
                    mapLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    mapLabel.setPadding(3, 0, 3, 0);
                    mapLabel.setTypeface(null, Typeface.BOLD);
                    mapLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    mapValue.setId(i * 900);
                    mapValue.setText(serverMap);
                    mapValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    mapValue.setPadding(3, 0, 3, 0);
                    mapValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    
                    int serverNumPlayers = serverInfo[i].getNumPlayers();
                    int serverMaxPlayers = serverInfo[i].getMaxPlayers();
                    TextView playersLabel = new TextView(CheckValve.this);
                    TextView playersValue = new TextView(CheckValve.this);
                    playersLabel.setId(i * 1000);
                    playersLabel.setText(CheckValve.this.getText(R.string.label_players));
                    playersLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    playersLabel.setPadding(3, 0, 3, 0);
                    playersLabel.setTypeface(null, Typeface.BOLD);
                    playersLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    playersValue.setId(i * 1100);
                    playersValue.setText(serverNumPlayers + "/" + serverMaxPlayers);
                    playersValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    playersValue.setPadding(3, 0, 3, 0);
                    playersValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    
                    String serverTags = serverInfo[i].getTags();
                    TextView tagsLabel = new TextView(CheckValve.this);
                    TextView tagsValue = new TextView(CheckValve.this);
                    tagsLabel.setId(i * 1200);
                    tagsLabel.setText(CheckValve.this.getText(R.string.label_tags));
                    tagsLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    tagsLabel.setPadding(3, 0, 3, 0);
                    tagsLabel.setTypeface(null, Typeface.BOLD);
                    tagsLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    tagsValue.setId(i * 1300);
                    tagsValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    tagsValue.setPadding(3, 0, 3, 0);
                    tagsValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    if( serverTags.length() > 0 )
                        tagsValue.setText(serverTags);
                    else
                        tagsValue.setText(CheckValve.this.getText(R.string.msg_no_tags));
                    
                    long serverPing = serverInfo[i].getPing();
                    TextView pingLabel = new TextView(CheckValve.this);
                    TextView pingValue = new TextView(CheckValve.this);
                    pingLabel.setId(i * 1400);
                    pingLabel.setText(CheckValve.this.getText(R.string.label_ping));
                    pingLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    pingLabel.setPadding(3, 0, 3, 0);
                    pingLabel.setTypeface(null, Typeface.BOLD);
                    pingLabel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    pingValue.setId(i * 1500);
                    pingValue.setText(serverPing + " ms");
                    pingValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    pingValue.setPadding(3, 0, 3, 0);
                    pingValue.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    
                    int serverRowId = (int)serverInfo[i].getRowId();

                    TextView spacer = new TextView(CheckValve.this);
                    spacer.setId(i * 100);
                    spacer.setText("");
                    spacer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    spacer.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    TableRow spacerRow = new TableRow(CheckValve.this);
                    spacerRow.setId(0);
                    spacerRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    spacerRow.addView(spacer);

                    // Create TableRows
                    TableRow serverRow = new TableRow(CheckValve.this);
                    serverRow.setId(serverRowId);
                    serverRow.setTag(Values.TAG_SERVER_NAME);
                    serverRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    serverRow.setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_NAME))?View.VISIBLE:View.GONE);
                    serverRow.setOnFocusChangeListener(tableRowFocusChangeListener);
                    serverRow.setOnTouchListener(tableRowTouchListener);
                    serverRow.setFocusable(false);
                    serverRow.addView(serverLabel);
                    serverRow.addView(serverValue);
                    registerForContextMenu(serverRow);

                    TableRow ipRow = new TableRow(CheckValve.this);
                    ipRow.setId(serverRowId);
                    ipRow.setTag(Values.TAG_SERVER_IP);
                    ipRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    ipRow.setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_IP))?View.VISIBLE:View.GONE);
                    ipRow.setOnTouchListener(tableRowTouchListener);
                    ipRow.setFocusable(false);
                    ipRow.addView(ipLabel);
                    ipRow.addView(ipValue);
                    registerForContextMenu(ipRow);

                    TableRow gameRow = new TableRow(CheckValve.this);
                    gameRow.setId(serverRowId);
                    gameRow.setTag(Values.TAG_SERVER_GAME);
                    gameRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    gameRow.setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO))?View.VISIBLE:View.GONE);
                    gameRow.setOnTouchListener(tableRowTouchListener);
                    gameRow.setFocusable(false);
                    gameRow.addView(gameLabel);
                    gameRow.addView(gameValue);
                    registerForContextMenu(gameRow);

                    TableRow mapRow = new TableRow(CheckValve.this);
                    mapRow.setId(serverRowId);
                    mapRow.setTag(Values.TAG_SERVER_MAP);
                    mapRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    mapRow.setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME))?View.VISIBLE:View.GONE);
                    mapRow.setOnTouchListener(tableRowTouchListener);
                    mapRow.setFocusable(false);
                    mapRow.addView(mapLabel);
                    mapRow.addView(mapValue);
                    registerForContextMenu(mapRow);

                    TableRow playersRow = new TableRow(CheckValve.this);
                    playersRow.setId(serverRowId);
                    playersRow.setTag(Values.TAG_SERVER_PLAYERS);
                    playersRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    playersRow.setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS))?View.VISIBLE:View.GONE);
                    playersRow.setOnTouchListener(tableRowTouchListener);
                    playersRow.setFocusable(false);
                    playersRow.addView(playersLabel);
                    playersRow.addView(playersValue);
                    registerForContextMenu(playersRow);

                    TableRow tagsRow = new TableRow(CheckValve.this);
                    tagsRow.setId(serverRowId);
                    tagsRow.setTag(Values.TAG_SERVER_TAGS);
                    tagsRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    tagsRow.setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_TAGS))?View.VISIBLE:View.GONE);
                    tagsRow.setOnTouchListener(tableRowTouchListener);
                    tagsRow.setFocusable(false);
                    tagsRow.addView(tagsLabel);
                    tagsRow.addView(tagsValue);
                    registerForContextMenu(tagsRow);
                    
                    TableRow pingRow = new TableRow(CheckValve.this);
                    pingRow.setId(serverRowId);
                    pingRow.setTag(Values.TAG_SERVER_PING);
                    pingRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    pingRow.setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_PING))?View.VISIBLE:View.GONE);
                    pingRow.setOnTouchListener(tableRowTouchListener);
                    pingRow.setFocusable(false);
                    pingRow.addView(pingLabel);
                    pingRow.addView(pingValue);
                    registerForContextMenu(pingRow);
                    
                    // Add these rows to the server info table
                    server_info_table.addView(
                            spacerRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    /*
                    server_info_table.addView(
                            nicknameRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    */
                    server_info_table.addView(
                            serverRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    server_info_table.addView(
                            ipRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    server_info_table.addView(
                            gameRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    server_info_table.addView(
                            mapRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    server_info_table.addView(
                            playersRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    server_info_table.addView(
                            tagsRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                    server_info_table.addView(
                            pingRow,
                            new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    );
                }
            }

            server_info_table.setVisibility(View.VISIBLE);
            
            // Dismiss the progress dialog
            p.dismiss();
        }
    };

    public void refreshView() {
        String tag = new String();

        for( int i = 0; i < server_info_table.getChildCount(); i++ ) {
            server_info_table.getChildAt(i).setVisibility(View.VISIBLE);
            tag = (String)server_info_table.getChildAt(i).getTag();

            if( tag != null ) {
                if( tag.equals(Values.TAG_SERVER_NAME) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_NAME))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_IP) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_IP))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_GAME) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_MAP) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_PLAYERS) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_TAGS) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_TAGS))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_PING) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_PING))?View.VISIBLE:View.GONE);
            }
        }

        server_info_table.invalidate();
    }

    public void getSettings() {
        settings = database.getSettingsAsBundle();
        
        // Background service is only supported on Honeycomb and above
        if( android.os.Build.VERSION.SDK_INT >= 11 ) {
            checkServiceState();
        }
    }

    public void checkServiceState() {
        serviceIntent = new Intent(this, BackgroundQueryService.class);
        
        Log.d(TAG, "Checking state of background query service.");
        
        // Start the service if it's not running but should be
        if( ! BackgroundQueryService.isRunning() ) {
            if( settings.getBoolean(Values.SETTING_ENABLE_NOTIFICATIONS) == true ) {
                Log.i(TAG, "Starting background query service.");
                startService(serviceIntent);
            }
        }
        // Stop the service if it is running but shouldn't be
        else {
            if( settings.getBoolean(Values.SETTING_ENABLE_NOTIFICATIONS) == false ) {
                Log.i(TAG, "Stopping background query service.");
                stopService(serviceIntent);
            }
        }
    }
    
    public void restartService() {
        serviceIntent = new Intent(this, BackgroundQueryService.class);
        
        if( BackgroundQueryService.isRunning() ) {
            Log.i(TAG, "Stopping background query service.");
            stopService(serviceIntent);
        }
        
        if( settings.getBoolean(Values.SETTING_ENABLE_NOTIFICATIONS) == true ) {
            Log.i(TAG, "Starting background query service.");
            startService(serviceIntent);
        }
        else {
            Log.i(TAG, "Background service is disabled in settings, not restarting.");
        }
    }
    
    public void quit() {
        finish();
    }

    public void addNewServer() {
        Intent addNewServerIntent = new Intent();
        addNewServerIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.AddServerActivity");
        startActivityForResult(addNewServerIntent, Values.ACTIVITY_ADD_NEW_SERVER);
    }

    public void manageServers() {
        Intent manageServersIntent = new Intent();
        manageServersIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ManageServersActivity");
        startActivityForResult(manageServersIntent, Values.ACTIVITY_MANAGE_SERVERS);
    }

    public void showPlayers( final long rowId ) {
        final ServerRecord sr = database.getServer(rowId);
        final String server = sr.getServerURL();
        final int port = sr.getServerPort();
        final int timeout = sr.getServerTimeout();

        final Handler playerQueryHandler = new Handler() {
            @SuppressWarnings("unchecked")
            public void handleMessage( Message msg ) {
                p.dismiss();

                if( msg.what == -2 ) {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_no_players);
                }
                else if( msg.what == -1 ) {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                }
                else {
                    if( msg.obj != null ) {
                        ArrayList<PlayerRecord> playerList = (ArrayList<PlayerRecord>)msg.obj;

                        Intent showPlayersIntent = new Intent();
                        showPlayersIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ShowPlayersActivity");
                        showPlayersIntent.putParcelableArrayListExtra(Values.EXTRA_PLAYER_LIST, playerList);
                        startActivityForResult(showPlayersIntent, Values.ACTIVITY_SHOW_PLAYERS);
                    }
                    else {
                        Log.d(TAG, "handleMessage(): Object 'msg.obj' is null");
                        UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                        finish();
                    }
                }
            }
        };

        final Handler challengeResponseHandler = new Handler() {
            public void handleMessage( Message msg ) {
                Log.d(TAG, "handleMessage(): msg=" + msg.toString());

                if( msg.what != 0 ) {
                    p.dismiss();
                    Log.d(TAG, "handleMessage(): msg.what=" + msg.what);
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                }
                else {
                    Bundle b = (Bundle)msg.obj;
                    byte[] challengeResponse = b.getByteArray(Values.EXTRA_CHALLENGE_RESPONSE);

                    if( challengeResponse == null ) {
                        p.dismiss();
                        String host = server + ":" + Integer.toString(port);
                        String message = String.format(CheckValve.this.getString(R.string.msg_no_challenge_response), host);
                        UserVisibleMessage.showMessage(CheckValve.this, message);
                    }
                    else {
                        new Thread(new QueryPlayers(CheckValve.this, rowId, challengeResponse, playerQueryHandler)).start();
                    }
                }
            }
        };

        p = ProgressDialog.show(this, "", getText(R.string.status_querying_servers), true, false);

        new Thread(new ChallengeResponseQuery(server, port, timeout, rowId, challengeResponseHandler)).start();
    }

    public void playerSearch() {
        Intent playerSearchIntent = new Intent();
        playerSearchIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.PlayerSearchActivity");
        startActivity(playerSearchIntent);
    }

    public void updateServer( final long rowId ) {
        Intent updateServerIntent = new Intent();
        updateServerIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.EditServerActivity");
        updateServerIntent.putExtra(Values.EXTRA_ROW_ID, rowId);
        startActivityForResult(updateServerIntent, Values.ACTIVITY_UPDATE_SERVER);
    }

    @SuppressLint({ "InlinedApi", "NewApi" })
    public void deleteServer( final long rowId ) {
        AlertDialog.Builder alertDialogBuilder;

        if( android.os.Build.VERSION.SDK_INT >= 11 )
            alertDialogBuilder = new AlertDialog.Builder(CheckValve.this, AlertDialog.THEME_HOLO_DARK);
        else
            alertDialogBuilder = new AlertDialog.Builder(CheckValve.this);

        alertDialogBuilder.setTitle(R.string.title_confirm_delete);
        alertDialogBuilder.setMessage(R.string.msg_delete_server);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int id ) {
                /*
                 *  "Delete" button was clicked
                 */
                if( database.deleteServer(rowId) ) {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_server_deleted);
                    queryServers();
                }
                else {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_db_failure);
                }
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int id ) {
                /*
                 * "Cancel" button was clicked
                 */
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void rcon( final long rowId ) {
        ServerRecord sr = database.getServer(rowId);

        String s = sr.getServerURL();
        String r = sr.getServerRCONPassword();
        int p = sr.getServerPort();
        int t = sr.getServerTimeout();

        Intent rconIntent = new Intent();
        rconIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.RconActivity");
        rconIntent.putExtra(Values.EXTRA_SERVER, s);
        rconIntent.putExtra(Values.EXTRA_PORT, p);
        rconIntent.putExtra(Values.EXTRA_TIMEOUT, t);
        rconIntent.putExtra(Values.EXTRA_PASSWORD, r);
        startActivityForResult(rconIntent, Values.ACTIVITY_RCON);
    }

    public void chat( final long rowId ) {
        ServerRecord sr = database.getServer(rowId);

        String s = sr.getServerURL();
        String r = sr.getServerRCONPassword();
        String n = sr.getServerNickname();
        int p = sr.getServerPort();
        int t = sr.getServerTimeout();

        Intent chatIntent = new Intent();
        chatIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ChatViewerActivity");
        chatIntent.putExtra(Values.EXTRA_SERVER, s);
        chatIntent.putExtra(Values.EXTRA_PORT, p);
        chatIntent.putExtra(Values.EXTRA_TIMEOUT, t);
        chatIntent.putExtra(Values.EXTRA_PASSWORD, r);
        
        if( n != null && n.length() > 0 ) {
            chatIntent.putExtra(Values.EXTRA_NICKNAME, n);
        }
        startActivityForResult(chatIntent, Values.ACTIVITY_CHAT);
    }

    public void about() {
        Intent aboutIntent = new Intent();
        aboutIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.AboutActivity");
        startActivityForResult(aboutIntent, Values.ACTIVITY_ABOUT);
    }

    public void settings() {
        Intent settingsIntent = new Intent();
        settingsIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.SettingsActivity");
        startActivityForResult(settingsIntent, Values.ACTIVITY_SETTINGS);
    }
    
    public void showDebugConsole() {
    	if( debugLog != null ) {
            Intent debugConsoleIntent = new Intent();
            debugConsoleIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.DebugConsoleActivity");
            debugConsoleIntent.putExtra("debugText", debugLog.getString());
            startActivityForResult(debugConsoleIntent, Values.ACTIVITY_DEBUG_CONSOLE);
    	}
    	else {
    		UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_debug_log_empty);
    	}
    }
    
    @SuppressLint("NewApi")
    private void toggleDebugMode() {
        if( debugMode == false ) {
            debugMode = true;
            
            UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_debug_mode_enabled);
            
            if( android.os.Build.VERSION.SDK_INT >= 11 )
                invalidateOptionsMenu();
            else
                this.findViewById(R.id.checkvalve_debug_button_layout).setVisibility(View.VISIBLE);
        }
        else {
            debugMode = false;
            
            UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_debug_mode_disabled);
            
            if( android.os.Build.VERSION.SDK_INT >= 11 )
                invalidateOptionsMenu();
            else
                this.findViewById(R.id.checkvalve_debug_button_layout).setVisibility(View.GONE);
        }
    }
}