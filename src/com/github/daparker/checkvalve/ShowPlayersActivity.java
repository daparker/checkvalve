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
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableRow.LayoutParams;
import com.github.daparker.checkvalve.R;

public class ShowPlayersActivity extends Activity {
    private static final String TAG = ShowPlayersActivity.class.getSimpleName();

    private DatabaseProvider database;
    private TableLayout player_info_table;
    private Button x_button;
    ArrayList<PlayerRecord> playerList;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.showplayers);

        x_button = (Button)this.findViewById(R.id.showplayers_x_button);
        x_button.setOnClickListener(xButtonListener);

        player_info_table = (TableLayout)findViewById(R.id.showplayers_player_info_table);

        if( database == null )
            database = new DatabaseProvider(ShowPlayersActivity.this);

        playerList = getIntent().getParcelableArrayListExtra(Values.EXTRA_PLAYER_LIST);

        showPlayers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(ShowPlayersActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

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

    private OnClickListener xButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "X" button was clicked
             */
            v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            finish();
        }
    };

    private void showPlayers() {
        for( int i = 0; i < playerList.size(); i++ ) {
            PlayerRecord pr = playerList.get(i);

            TableRow row = new TableRow(ShowPlayersActivity.this);

            TextView playerName = new TextView(ShowPlayersActivity.this);
            TextView numKills = new TextView(ShowPlayersActivity.this);
            TextView connected = new TextView(ShowPlayersActivity.this);

            playerName.setText(pr.getName());
            playerName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f);
            playerName.setPadding(5, 0, 5, 0);
            playerName.setGravity(Gravity.LEFT);
            playerName.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            numKills.setText(Long.toString(pr.getKills()));
            numKills.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f);
            numKills.setPadding(5, 0, 5, 0);
            numKills.setGravity(Gravity.CENTER_HORIZONTAL);
            numKills.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            connected.setText(pr.getTime());
            connected.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f);
            connected.setPadding(5, 0, 5, 0);
            connected.setGravity(Gravity.CENTER_HORIZONTAL);
            connected.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            row.setId(pr.getIndex());
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            row.addView(playerName);
            row.addView(numKills);
            row.addView(connected);

            player_info_table.addView(row);
        }

        Log.d(TAG, "showPlayers(): Setting player_info_table to VISIBLE");
        player_info_table.setVisibility(View.VISIBLE);
    }
}