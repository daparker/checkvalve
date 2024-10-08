/*
 * Copyright 2010-2024 by David A. Parker <parker.david.a@gmail.com>
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

//import android.widget.Switch;

@SuppressLint("NewApi")
public class ManageServersActivity extends Activity {
    private DatabaseProvider database;
    private TableLayout server_table;
    private Intent thisIntent;

    private final OnClickListener xButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "X" button was clicked
             */
            v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            finish();
        }
    };

    private final OnClickListener editButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Edit" button was clicked
             */
            v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            long rowId = v.getId();
            updateServer(rowId);
        }
    };

    private final OnClickListener deleteButtonListener = new OnClickListener() {
        @SuppressLint("InlinedApi")
        public void onClick(View v) {
            /*
             * "Delete" button was clicked
             */
            v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            final long rowId = v.getId();

            AlertDialog.Builder alertDialogBuilder;

            alertDialogBuilder = new AlertDialog.Builder(ManageServersActivity.this, AlertDialog.THEME_HOLO_DARK);
            alertDialogBuilder.setTitle(R.string.title_confirm_delete);
            alertDialogBuilder.setMessage(R.string.msg_delete_server);
            alertDialogBuilder.setCancelable(false);

            alertDialogBuilder.setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    /*
                     *  "Delete" button was clicked
                     */
                    if( database.deleteServer(rowId) ) {
                        setResult(1, thisIntent);
                        showServerList();
                        UserVisibleMessage.showMessage(ManageServersActivity.this, R.string.msg_server_deleted);
                    }
                    else {
                        UserVisibleMessage.showMessage(ManageServersActivity.this, R.string.msg_db_failure);
                    }
                }
            });

            alertDialogBuilder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    /*
                     * "Cancel" button was clicked
                     */
                    dialog.cancel();
                    showServerList();
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    };

    private final OnClickListener moveUpButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Move Up" button was clicked
             */
            v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            long rowId = v.getId();

            if( database.moveServerUp(rowId) ) {
                setResult(1, thisIntent);
                showServerList();
            }
            else {
                UserVisibleMessage.showMessage(ManageServersActivity.this, R.string.msg_db_failure);
            }
        }
    };

    private final OnClickListener moveDownButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Move Down" button was clicked
             */
            v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            long rowId = v.getId();

            if( database.moveServerDown(rowId) ) {
                setResult(1, thisIntent);
                showServerList();
            }
            else {
                UserVisibleMessage.showMessage(ManageServersActivity.this, R.string.msg_db_failure);
            }
        }
    };

    private final OnCheckedChangeListener toggleListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
            long rowId = buttonView.getId();

            if( checked ) {
                if( database.enableServer(rowId) ) {
                    setResult(1, thisIntent);
                    showServerList();
                }
                else {
                    UserVisibleMessage.showMessage(ManageServersActivity.this, R.string.msg_db_failure);
                }
            }
            else {
                if( database.disableServer(rowId) ) {
                    setResult(1, thisIntent);
                    showServerList();
                }
                else {
                    UserVisibleMessage.showMessage(ManageServersActivity.this, R.string.msg_db_failure);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( ViewConfiguration.get(this).hasPermanentMenuKey() )
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        thisIntent = this.getIntent();
        setResult(0, thisIntent);

        if( database == null )
            database = new DatabaseProvider(ManageServersActivity.this);

        setContentView(R.layout.manageservers);

        Button x_button = findViewById(R.id.manageservers_x_button);
        x_button.setOnClickListener(xButtonListener);
        server_table = findViewById(R.id.manageservers_server_table);

        showServerList();
    }

    @Override
    public void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(ManageServersActivity.this);

        showServerList();
    }

    @Override
    public void onPause() {
        super.onPause();

        if( database != null ) {
            database.close();
            database = null;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onActivityResult(int request, int result, Intent data) {
        if( database == null ) database = new DatabaseProvider(ManageServersActivity.this);

        if( request == Values.ACTIVITY_UPDATE_SERVER
                || request == Values.ACTIVITY_CONFIRM_DELETE
                || request == Values.ACTIVITY_ADD_NEW_SERVER ) {
            if( result == 1 )
                setResult(1, thisIntent);

            showServerList();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manageservers_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( item.getItemId() == R.id.back ) {
            finish();
            return true;
        }
        else if( item.getItemId() == R.id.add_server ) {
            addNewServer();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void showServerList() {
        ServerRecord[] serverList = database.getAllServers();

        server_table.removeAllViews();

        /*
         * Loop through the servers in the database
         */
        for( int i = 0; i < serverList.length; i++ ) {
            ServerRecord sr = serverList[i];

            int rowId = (int) sr.getServerRowID();
            int port = sr.getServerPort();

            // Use the nickname as the preferred server name in the list
            String server = sr.getServerNickname();

            // If there's no nickname then use the URL as the server name 
            if( server.isEmpty() ) {
                server = sr.getServerURL() + ":" + port;
            }

            View buttonBar = View.inflate(ManageServersActivity.this, R.layout.manageservers_button_bar, null);
            buttonBar.setId(i);

            TextView serverName = (TextView) View.inflate(ManageServersActivity.this, R.layout.manageservers_servername, null);
            serverName.setText(server);
            serverName.setId(i);

            Button editButton = buttonBar.findViewById(R.id.buttonbar_edit_button);
            editButton.setId(rowId);
            editButton.setOnClickListener(editButtonListener);

            Button deleteButton = buttonBar.findViewById(R.id.buttonbar_delete_button);
            deleteButton.setId(rowId);
            deleteButton.setOnClickListener(deleteButtonListener);

            Button moveUpButton = buttonBar.findViewById(R.id.buttonbar_up_button);
            moveUpButton.setId(rowId);
            moveUpButton.setOnClickListener(moveUpButtonListener);

            Button moveDownButton = buttonBar.findViewById(R.id.buttonbar_down_button);
            moveDownButton.setId(rowId);
            moveDownButton.setOnClickListener(moveDownButtonListener);

            CheckBox toggle = buttonBar.findViewById(R.id.buttonbar_toggle);
            toggle.setId(rowId);

            if( sr.isEnabled() ) {
                toggle.setChecked(true);
                serverName.setTextColor(Color.WHITE);
            }
            else {
                toggle.setChecked(false);
                serverName.setTextColor(Color.GRAY);
            }

            toggle.setOnCheckedChangeListener(toggleListener);

            TableRow serverRow = new TableRow(ManageServersActivity.this);
            serverRow.setId(i);
            serverRow.addView(serverName);
            serverRow.addView(buttonBar);

            TableLayout.LayoutParams params = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);

            params.setMargins(0, 50, 0, 0);
            serverRow.setLayoutParams(params);
            serverRow.setGravity(Gravity.START | Gravity.BOTTOM);

            server_table.addView(serverRow, params);

            TableRow dividerRow = (TableRow) View.inflate(ManageServersActivity.this, R.layout.manageservers_tablerow_h_divider, null);
            dividerRow.setId(i);
            server_table.addView(dividerRow);
        }
    }

    public void updateServer(long rowId) {
        Intent updateServerIntent = new Intent();
        updateServerIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.EditServerActivity");
        updateServerIntent.putExtra(Values.EXTRA_ROW_ID, rowId);
        startActivityForResult(updateServerIntent, Values.ACTIVITY_UPDATE_SERVER);
    }

    public void addNewServer() {
        Intent addNewServerIntent = new Intent();
        addNewServerIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.AddServerActivity");
        startActivityForResult(addNewServerIntent, Values.ACTIVITY_ADD_NEW_SERVER);
    }
}