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
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import com.github.daparker.checkvalve.R;

public class ManageServers extends Activity
{
    private DatabaseProvider database;
    private TableLayout server_table;
    private Intent thisIntent;

    private OnClickListener editButtonListener = new OnClickListener()
    {
        public void onClick( View v )
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
        public void onClick( View v )
        {
            /*
             * "Delete" button was clicked
             */

            long rowId = (long)v.getId();

            Intent confirmDeleteIntent = new Intent();
            confirmDeleteIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ConfirmDelete");
            confirmDeleteIntent.putExtra("rowId", rowId);
            startActivityForResult(confirmDeleteIntent, Values.ACTIVITY_CONFIRM_DELETE);
        }
    };

    private OnClickListener moveUpButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Move Up" button was clicked
             */

            long rowId = (long)v.getId();

            if( database.moveServerUp(rowId) )
            {
                setResult(1, thisIntent);
                showServerList();
            }
            else
            {
                UserVisibleMessage.showMessage(ManageServers.this, R.string.msg_db_failure);
            }
        }
    };

    private OnClickListener moveDownButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Move Down" button was clicked
             */

            long rowId = (long)v.getId();

            if( database.moveServerDown(rowId) )
            {
                setResult(1, thisIntent);
                showServerList();
            }
            else
            {
                UserVisibleMessage.showMessage(ManageServers.this, R.string.msg_db_failure);
            }
        }
    };

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        thisIntent = getIntent();

        setResult(0, thisIntent);

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

        if( !database.isOpen() ) database.open();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if( database.isOpen() ) database.close();
    }

    public void onActivityResult( int request, int result, Intent data )
    {
        if( (request == Values.ACTIVITY_UPDATE_SERVER
                || request == Values.ACTIVITY_CONFIRM_DELETE)
                && result == 1 )
        {
            setResult(1, thisIntent);
            showServerList();
        }

        return;
    }

    public void showServerList()
    {
        if( !database.isOpen() ) database.open();

        server_table.setVisibility(View.GONE);
        server_table.removeAllViews();
        server_table.setVisibility(View.VISIBLE);

        Cursor databaseCursor = null;
        databaseCursor = database.getAllServers();
        databaseCursor.moveToFirst();
        
        /*
         * Loop through the servers in the database
         */
        for( int i = 0; i < databaseCursor.getCount(); i++ )
        {
            int rowId = (int)databaseCursor.getLong(0);
            String server = databaseCursor.getString(1);
            int port = databaseCursor.getInt(2);
            
            View v = View.inflate(ManageServers.this, R.layout.manage_servers_button_bar, null);
            v.setId(i);
            
            TextView serverName = (TextView)View.inflate(ManageServers.this, R.layout.manage_servers_servername, null);
            serverName.setText(server + ":" + port);
            serverName.setId(i);

            Button editButton = (Button)v.findViewById(R.id.edit_button);
            editButton.setId(rowId);
            editButton.setOnClickListener(editButtonListener);

            Button deleteButton = (Button)v.findViewById(R.id.delete_button);
            deleteButton.setId(rowId);
            deleteButton.setOnClickListener(deleteButtonListener);

            Button moveUpButton = (Button)v.findViewById(R.id.up_button);
            moveUpButton.setId(rowId);
            moveUpButton.setOnClickListener(moveUpButtonListener);

            Button moveDownButton = (Button)v.findViewById(R.id.down_button);
            moveDownButton.setId(rowId);
            moveDownButton.setOnClickListener(moveDownButtonListener);

            if( i != 0 )
            {
                TableRow dividerRow = (TableRow)View.inflate(ManageServers.this, R.layout.manage_servers_tablerow_h_divider, null);
                dividerRow.setId(i);
                server_table.addView(dividerRow);
            }
            
            TableRow serverRow = new TableRow(ManageServers.this);
            serverRow.setId(i);
            serverRow.addView(serverName);
            serverRow.addView(v);
            
            server_table.addView(serverRow);

            if( !databaseCursor.isLast() ) databaseCursor.moveToNext();
        }

        if( databaseCursor != null ) databaseCursor.close();
        
        int rowId = 100;
        String server = "someInordinatelyLongServerName.myHostingService.com";
        int port = 27015;
        
        View v = View.inflate(ManageServers.this, R.layout.manage_servers_button_bar, null);
        v.setId(rowId);
        
        TextView serverName = (TextView)View.inflate(ManageServers.this, R.layout.manage_servers_servername, null);
        serverName.setText(server + ":" + port);
        serverName.setId(rowId);

        Button editButton = (Button)v.findViewById(R.id.edit_button);
        editButton.setId(rowId);
        editButton.setOnClickListener(editButtonListener);

        Button deleteButton = (Button)v.findViewById(R.id.delete_button);
        deleteButton.setId(rowId);
        deleteButton.setOnClickListener(deleteButtonListener);

        Button moveUpButton = (Button)v.findViewById(R.id.up_button);
        moveUpButton.setId(rowId);
        moveUpButton.setOnClickListener(moveUpButtonListener);

        Button moveDownButton = (Button)v.findViewById(R.id.down_button);
        moveDownButton.setId(rowId);
        moveDownButton.setOnClickListener(moveDownButtonListener);

        TableRow dividerRow = (TableRow)View.inflate(ManageServers.this, R.layout.manage_servers_tablerow_h_divider, null);
        dividerRow.setId(rowId);
        server_table.addView(dividerRow);
        
        TableRow serverRow = new TableRow(ManageServers.this);
        serverRow.setId(rowId);
        serverRow.addView(serverName);
        serverRow.addView(v);
        
        server_table.addView(serverRow);
    }

    public void updateServer( long rowId )
    {
        Intent updateServerIntent = new Intent();
        updateServerIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.UpdateServer");
        updateServerIntent.putExtra("rowId", rowId);
        startActivityForResult(updateServerIntent, Values.ACTIVITY_UPDATE_SERVER);
    }

    public void showMessage( String msg )
    {
        Intent messageBoxIntent = new Intent();
        messageBoxIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.MessageBox");
        messageBoxIntent.putExtra("messageText", msg);
        startActivity(messageBoxIntent);
    }
}