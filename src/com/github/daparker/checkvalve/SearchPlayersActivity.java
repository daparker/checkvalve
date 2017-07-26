/*
 * Copyright 2010-2017 by David A. Parker <parker.david.a@gmail.com>
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
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableRow.LayoutParams;
import android.content.Intent;
import android.content.res.Configuration;
import com.github.daparker.checkvalve.R;

public class SearchPlayersActivity extends Activity {
    private ProgressDialog p;
    private SearchPlayers q;
    private DatabaseProvider database;
    private TableLayout search_results_table;
    private TableLayout message_table;

    @SuppressLint("NewApi")
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        if( android.os.Build.VERSION.SDK_INT < 11 ) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        else if( android.os.Build.VERSION.SDK_INT >= 14 ) {
            if( ViewConfiguration.get(this).hasPermanentMenuKey() )
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        if( database == null )
            database = new DatabaseProvider(SearchPlayersActivity.this);

        Intent thisIntent = this.getIntent();
        String search = thisIntent.getStringExtra(Values.EXTRA_SEARCH);

        setContentView(R.layout.searchresults);

        search_results_table = (TableLayout)findViewById(R.id.searchresults_main_table);
        message_table = (TableLayout)findViewById(R.id.searchresults_message_table);

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
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.playersearch_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.back:
                quit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void searchPlayers( String search ) {
        // Show the progress dialog
        p = ProgressDialog.show(this, "", this.getText(R.string.status_searching_players), true, false);

        search_results_table.removeAllViews();
        message_table.removeAllViews();

        // Run the server queries in a new thread
        q = new SearchPlayers(SearchPlayersActivity.this, progressHandler, search);
        q.start();
    }

    // Handler for the player search thread
    Handler progressHandler = new Handler() {
        public void handleMessage( Message msg ) {
            message_table.setVisibility(View.GONE);
            search_results_table.setVisibility(View.GONE);
            
            // A negative "what" code indicates the server query thread failed
            if( msg.what < 0 ) {
                p.dismiss();
                UserVisibleMessage.showMessage(SearchPlayersActivity.this, R.string.msg_general_error);
                return;
            }
            
            Bundle b = (Bundle)msg.obj;
            ArrayList<String> messages = b.getStringArrayList(Values.MESSAGES);
            ArrayList<String> players = b.getStringArrayList(Values.PLAYER_INFO);
            
            /*
             * Build and display the error messages table
             */
            if( ! messages.isEmpty() ) {
                for( int m = 0; m < messages.size(); m++ ) {
                    TextView errorMessage = new TextView(SearchPlayersActivity.this);

                    errorMessage.setId(Integer.MAX_VALUE);
                    errorMessage.setText(messages.get(m));
                    errorMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    errorMessage.setPadding(3, 0, 3, 0);
                    errorMessage.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    // Create a TableRow and give it an ID
                    TableRow messageRow = new TableRow(SearchPlayersActivity.this);
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
            if( ! players.isEmpty() ) {
                for( int i = 0; i < players.size(); i++ ) {
                    TextView searchResult = new TextView(SearchPlayersActivity.this);
                    searchResult.setId(0);
                    searchResult.setText(Html.fromHtml(players.get(i)));
                    searchResult.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                    searchResult.setPadding(5, 0, 5, 0);
                    searchResult.setGravity(Gravity.LEFT);
                    searchResult.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                    TableRow row = new TableRow(SearchPlayersActivity.this);
                    row.setId(0);
                    row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    row.addView(searchResult);

                    search_results_table.addView(row);
                }
            }
            else {
                TextView searchResult = new TextView(SearchPlayersActivity.this);
                searchResult.setId(0);
                searchResult.setText(getString(R.string.msg_no_search_results));
                searchResult.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
                searchResult.setPadding(5, 0, 5, 0);
                searchResult.setGravity(Gravity.LEFT);
                searchResult.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

                TableRow row = new TableRow(SearchPlayersActivity.this);
                row.setId(0);
                row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                row.addView(searchResult);

                search_results_table.addView(row);
            }

            search_results_table.setVisibility(View.VISIBLE);
            
            // Dismiss the progress dialog
            p.dismiss();
        }
    };

    public void quit() {
        finish();
    }
}