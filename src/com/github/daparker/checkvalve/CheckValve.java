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
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import com.github.daparker.checkvalve.R;

public class CheckValve extends Activity
{
    private static final String TAG = CheckValve.class.getSimpleName();

    private ProgressDialog p;
    private DatabaseProvider database;
    private TableLayout server_info_table;
    private TableLayout message_table;
    private TableRow[][] tableRows;
    private TableRow[] messageRows;

    public static Bundle settings;

    private long selectedServerRowId;

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting CheckValve.");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        if( database == null )
            database = new DatabaseProvider(CheckValve.this);

        selectedServerRowId = 0;
        server_info_table = (TableLayout)findViewById(R.id.server_info_table);
        message_table = (TableLayout)findViewById(R.id.message_table);
        message_table.setVisibility(-1);

        getSettings();
        queryServers();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        selectedServerRowId = 0;

        if( database == null )
            database = new DatabaseProvider(CheckValve.this);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // Dismiss the progress dialog to avoid a leaked window
        if( p != null )
            p.dismiss();

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

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
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
                // "About" option was selected
                settings();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo )
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        selectedServerRowId = (long)v.getId();

        // Inflate the context menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected( MenuItem item )
    {
        long id = selectedServerRowId;

        switch( item.getItemId() )
        {
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

    public void onActivityResult( int request, int result, Intent data )
    {
        if( database == null )
            database = new DatabaseProvider(CheckValve.this);

        switch( request )
        {
            case Values.ACTIVITY_RCON:
            case Values.ACTIVITY_CHAT:
            case Values.ACTIVITY_ABOUT:
            case Values.ACTIVITY_SHOW_PLAYERS:
                break;

            case Values.ACTIVITY_ADD_NEW_SERVER:
            case Values.ACTIVITY_MANAGE_SERVERS:
            case Values.ACTIVITY_UPDATE_SERVER:
            case Values.ACTIVITY_CONFIRM_DELETE:
                if( result == 1 ) queryServers();
                break;

            case Values.ACTIVITY_SETTINGS:
                if( result == -1 )
                {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_db_failure);
                }
                else if( result == 0 )
                {
                    getSettings();
                    refreshView();
                }
                break;
            default:
                break;
        }
    }

    // Define the touch listener for table rows
    private OnTouchListener tableRowTouchListener = new OnTouchListener()
    {
        public boolean onTouch( View v, MotionEvent m )
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
        public void onFocusChange( View v, boolean f )
        {
            int id = v.getId();
            int count = server_info_table.getChildCount();

            // If any row has focus then set its background color to blue
            for( int i = 0; i < count; i++ )
                if( server_info_table.getChildAt(i).getId() == id )
                    server_info_table.getChildAt(i).setBackgroundResource((f)?R.color.steam_blue:R.color.steam_gray);
        }
    };

    //@SuppressWarnings("deprecation")
    public void queryServers()
    {
        // Clear the server info table
        server_info_table.setVisibility(-1);
        server_info_table.removeAllViews();
        server_info_table.setVisibility(1);

        // Clear the messages table
        message_table.removeAllViews();

        int count = (int)database.getServerCount();

        if( count == 0 )
        {
            addNewServer();
        }
        else
        {
            // Show the progress dialog
            p = ProgressDialog.show(this, "", getText(R.string.status_querying_servers), true, false);

            // Define the TableRow arrays to hold the display data
            tableRows = new TableRow[count][6];
            messageRows = new TableRow[count];

            // Run the server queries in a separate thread
            new Thread(new ServerQuery(CheckValve.this, tableRows, messageRows, progressHandler)).start();
        }
    }

    // Handler for the server query thread
    Handler progressHandler = new Handler()
    {
        public void handleMessage( Message msg )
        {
            String tag = new String();

            /*
             * Build and display the messages table if there are errors to be displayed
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
                    TextView spacer = new TextView(CheckValve.this);

                    spacer.setId(0);
                    spacer.setText("");
                    spacer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    spacer.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    TableRow spacerRow = new TableRow(CheckValve.this);
                    spacerRow.setId(0);
                    spacerRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    spacerRow.addView(spacer);

                    tableRows[i][0].setOnFocusChangeListener(tableRowFocusChangeListener);

                    for( int j = 0; j < tableRows[i].length; j++ )
                    {
                        registerForContextMenu(tableRows[i][j]);
                        tableRows[i][j].setOnTouchListener(tableRowTouchListener);
                        tableRows[i][j].setFocusable(false);

                        tag = (String)tableRows[i][j].getTag();

                        if( tag.equals(Values.TAG_SERVER_IP) )
                            tableRows[i][j].setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_IP))?View.VISIBLE:View.GONE);
                        else if( tag.equals(Values.TAG_SERVER_GAME) )
                            tableRows[i][j].setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO))?View.VISIBLE:View.GONE);
                        else if( tag.equals(Values.TAG_SERVER_MAP) )
                            tableRows[i][j].setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME))?View.VISIBLE:View.GONE);
                        else if( tag.equals(Values.TAG_SERVER_PLAYERS) )
                            tableRows[i][j].setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS))?View.VISIBLE:View.GONE);
                        else if( tag.equals(Values.TAG_SERVER_TAGS) )
                            tableRows[i][j].setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_TAGS))?View.VISIBLE:View.GONE);
                    }

                    server_info_table.addView(spacerRow, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    server_info_table.addView(tableRows[i][0], new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    server_info_table.addView(tableRows[i][1], new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    server_info_table.addView(tableRows[i][2], new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    server_info_table.addView(tableRows[i][3], new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    server_info_table.addView(tableRows[i][4], new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    server_info_table.addView(tableRows[i][5], new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                }
            }

            // Dismiss the progress dialog
            p.dismiss();
        }
    };

    public void refreshView()
    {
        String tag = new String();

        for( int i = 0; i < server_info_table.getChildCount(); i++ )
        {
            server_info_table.getChildAt(i).setVisibility(View.VISIBLE);
            tag = (String)server_info_table.getChildAt(i).getTag();

            if( tag != null )
            {
                if( tag.equals(Values.TAG_SERVER_IP) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_IP))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_GAME) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_GAME_INFO))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_MAP) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_MAP_NAME))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_PLAYERS) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_NUM_PLAYERS))?View.VISIBLE:View.GONE);
                else if( tag.equals(Values.TAG_SERVER_TAGS) )
                    server_info_table.getChildAt(i).setVisibility((settings.getBoolean(Values.SETTING_SHOW_SERVER_TAGS))?View.VISIBLE:View.GONE);
            }
        }

        server_info_table.invalidate();
    }

    public void getSettings()
    {
        settings = database.getSettingsAsBundle();
    }

    public void quit()
    {
        finish();
    }

    public void addNewServer()
    {
        Intent addNewServerIntent = new Intent();
        addNewServerIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.AddServerActivity");
        startActivityForResult(addNewServerIntent, Values.ACTIVITY_ADD_NEW_SERVER);
    }

    public void manageServers()
    {
        Intent manageServersIntent = new Intent();
        manageServersIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ManageServersActivity");
        startActivityForResult(manageServersIntent, Values.ACTIVITY_MANAGE_SERVERS);
    }

    public void showPlayers( final long rowId )
    {
        final ServerRecord sr = database.getServer(rowId);
        final String server = sr.getServerName();
        final int port = sr.getServerPort();
        final int timeout = sr.getServerTimeout();

        final Handler playerQueryHandler = new Handler()
        {
            @SuppressWarnings("unchecked")
            public void handleMessage( Message msg )
            {
                p.dismiss();

                if( msg.what == -2 )
                {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_no_players);
                }
                else if( msg.what == -1 )
                {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                }
                else
                {
                    if( msg.obj != null )
                    {
                        ArrayList<PlayerRecord> playerList = (ArrayList<PlayerRecord>)msg.obj;

                        Intent showPlayersIntent = new Intent();
                        showPlayersIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ShowPlayersActivity");
                        showPlayersIntent.putParcelableArrayListExtra(Values.EXTRA_PLAYER_LIST, playerList);
                        startActivityForResult(showPlayersIntent, Values.ACTIVITY_SHOW_PLAYERS);
                    }
                    else
                    {
                        Log.d(TAG, "handleMessage(): Object 'msg.obj' is null");
                        UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                        finish();
                    }
                }
            }
        };

        final Handler challengeResponseHandler = new Handler()
        {
            public void handleMessage( Message msg )
            {
                Log.d(TAG, "handleMessage(): msg=" + msg.toString());

                if( msg.what != 0 )
                {
                    p.dismiss();
                    Log.d(TAG, "handleMessage(): msg.what=" + msg.what);
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_general_error);
                }
                else
                {
                    Bundle b = (Bundle)msg.obj;
                    byte[] challengeResponse = b.getByteArray(Values.EXTRA_CHALLENGE_RESPONSE);

                    if( challengeResponse == null )
                    {
                        p.dismiss();
                        String message = new String();
                        message += CheckValve.this.getText(R.string.msg_no_challenge_response);
                        message += " " + server + ":" + port;
                        UserVisibleMessage.showMessage(CheckValve.this, message);
                    }
                    else
                    {
                        new Thread(new QueryPlayers(CheckValve.this, rowId, challengeResponse, playerQueryHandler)).start();
                    }
                }
            }
        };

        p = ProgressDialog.show(this, "", getText(R.string.status_querying_servers), true, false);

        new Thread(new ChallengeResponseQuery(server, port, timeout, rowId, challengeResponseHandler)).start();
    }

    public void playerSearch()
    {
        Intent playerSearchIntent = new Intent();
        playerSearchIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.PlayerSearchActivity");
        startActivity(playerSearchIntent);
    }

    public void updateServer( final long rowId )
    {
        Intent updateServerIntent = new Intent();
        updateServerIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.EditServerActivity");
        updateServerIntent.putExtra(Values.EXTRA_ROW_ID, rowId);
        startActivityForResult(updateServerIntent, Values.ACTIVITY_UPDATE_SERVER);
    }

    @SuppressLint("InlinedApi")
    public void deleteServer( final long rowId )
    {
        AlertDialog.Builder alertDialogBuilder;

        if( android.os.Build.VERSION.SDK_INT >= 11 )
            alertDialogBuilder = new AlertDialog.Builder(CheckValve.this, AlertDialog.THEME_HOLO_DARK);
        else
            alertDialogBuilder = new AlertDialog.Builder(CheckValve.this);

        alertDialogBuilder.setTitle(R.string.title_confirm_delete);
        alertDialogBuilder.setMessage(R.string.msg_delete_server);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int id )
            {
                /*
                 *  "Delete" button was clicked
                 */
                if( database.deleteServer(rowId) )
                {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_server_deleted);
                    queryServers();
                }
                else
                {
                    UserVisibleMessage.showMessage(CheckValve.this, R.string.msg_db_failure);
                }
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int id )
            {
                /*
                 * "Cancel" button was clicked
                 */
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void rcon( final long rowId )
    {
        ServerRecord sr = database.getServer(rowId);

        String s = sr.getServerName();
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

    public void chat( final long rowId )
    {
        ServerRecord sr = database.getServer(rowId);

        String s = sr.getServerName();
        String r = sr.getServerRCONPassword();
        int p = sr.getServerPort();
        int t = sr.getServerTimeout();

        Intent chatIntent = new Intent();
        chatIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ChatViewerActivity");
        chatIntent.putExtra(Values.EXTRA_SERVER, s);
        chatIntent.putExtra(Values.EXTRA_PORT, p);
        chatIntent.putExtra(Values.EXTRA_TIMEOUT, t);
        chatIntent.putExtra(Values.EXTRA_PASSWORD, r);
        startActivityForResult(chatIntent, Values.ACTIVITY_CHAT);
    }

    public void about()
    {
        Intent aboutIntent = new Intent();
        aboutIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.AboutActivity");
        startActivityForResult(aboutIntent, Values.ACTIVITY_ABOUT);
    }

    public void settings()
    {
        Intent settingsIntent = new Intent();
        settingsIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.SettingsActivity");
        startActivityForResult(settingsIntent, Values.ACTIVITY_SETTINGS);
    }
}