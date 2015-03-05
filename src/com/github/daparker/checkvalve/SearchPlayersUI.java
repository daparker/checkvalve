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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import com.github.daparker.checkvalve.R;

public class SearchPlayersUI extends Activity
{
    private ProgressDialog p;
    private SearchPlayers q;
    private DatabaseProvider database;
    private Cursor databaseCursor;
    private TableLayout search_results_table;
    private TableLayout message_table;
    private TableRow[] tableRows;
    private TableRow[] messageRows;
    private Context context;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        context = this;

        Intent thisIntent = this.getIntent();
        String search = thisIntent.getStringExtra("search");

        database = new DatabaseProvider(context);
        database.open();

        setContentView(R.layout.searchresults);

        search_results_table = (TableLayout)findViewById(R.id.search_results_table);
        message_table = (TableLayout)findViewById(R.id.message_table);

        searchPlayers(search);
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

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.playersearch_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.back:
                quit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void searchPlayers( String search )
    {
        // Show the progress dialog
        p = ProgressDialog.show(this, "", context.getText(R.string.status_searching_players), true, false);

        search_results_table.setVisibility(-1);
        search_results_table.removeAllViews();
        search_results_table.setVisibility(1);

        message_table.removeAllViews();

        if( !database.isOpen() ) database.open();

        databaseCursor = database.getAllServers();
        //databaseCursor.moveToFirst();

        //startManagingCursor(databaseCursor);

        tableRows = new TableRow[(databaseCursor.getCount() * 50)];
        messageRows = new TableRow[(databaseCursor.getCount() * 50)];

        if( databaseCursor != null ) databaseCursor.close();

        // Run the server queries in a new thread
        //q = new SearchPlayers(context, databaseCursor, tableRows, messageRows, progressHandler, search);
        q = new SearchPlayers(context, tableRows, messageRows, progressHandler, search);
        q.start();
    }

    // Handler for the player search thread
    Handler progressHandler = new Handler()
    {
        public void handleMessage( Message msg )
        {
            /*
             * Build and display the error messages table if there are errors to be displayed
             */
            if( messageRows[0] != null )
            {
                int m = 0;

                while( messageRows[m] != null )
                    message_table.addView(messageRows[m++]);

                message_table.setVisibility(1);
            }

            /*
             * Build and display the query results table
             */
            for( int i = 0; i < tableRows.length; i++ )
            {
                if( tableRows[i] != null )
                    search_results_table.addView(tableRows[i], new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }

            // Dismiss the progress dialog
            p.dismiss();
        }
    };

    public void quit()
    {
        if( database.isOpen() ) database.close();

        finish();
    }
}