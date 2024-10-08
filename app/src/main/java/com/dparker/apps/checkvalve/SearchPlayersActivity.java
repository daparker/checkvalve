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
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;

import androidx.annotation.NonNull;

public class SearchPlayersActivity extends Activity {
    private ProgressDialog p;
    private DatabaseProvider database;
    private TableLayout search_results_table;
    private TableLayout message_table;
    private TableRow[] tableRows;
    private TableRow[] messageRows;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( ViewConfiguration.get(this).hasPermanentMenuKey() )
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        if( database == null )
            database = new DatabaseProvider(SearchPlayersActivity.this);

        Intent thisIntent = this.getIntent();
        String search = thisIntent.getStringExtra(Values.EXTRA_SEARCH);

        setContentView(R.layout.searchresults);

        search_results_table = findViewById(R.id.searchresults_main_table);
        message_table = findViewById(R.id.searchresults_message_table);

        searchPlayers(search);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(SearchPlayersActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if( database != null ) {
            database.close();
            database = null;
        }

        finish();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.playersearch_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( item.getItemId() == R.id.back ) {
            quit();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void searchPlayers(String search) {
        // Show the progress dialog
        p = ProgressDialog.show(this, "", this.getText(R.string.status_searching_players), true, false);

        search_results_table.removeAllViews();
        message_table.removeAllViews();

        int count = (int) database.getServerCount();

        tableRows = new TableRow[(count * 50)];
        messageRows = new TableRow[(count * 50)];

        // Run the server queries in a new thread
        SearchPlayers q = new SearchPlayers(SearchPlayersActivity.this, tableRows, messageRows, progressHandler, search);
        q.start();
    }

    // Handler for the player search thread
    Handler progressHandler = new Handler(Looper.myLooper()) {
        public void handleMessage(@NonNull Message msg) {
            /*
             * Build and display the error messages table if there are errors to be displayed
             */
            if( messageRows[0] != null ) {
                int m = 0;

                while( messageRows[m] != null )
                    message_table.addView(messageRows[m++]);

                message_table.setVisibility(View.VISIBLE);
            }

            /*
             * Build and display the query results table
             */
            for( int i = 0; i < tableRows.length; i++ ) {
                if( tableRows[i] != null ) {
                    search_results_table.addView(
                            tableRows[i],
                            new TableLayout.LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT));
                }
            }

            // Dismiss the progress dialog
            p.dismiss();
        }
    };

    public void quit() {
        finish();
    }
}