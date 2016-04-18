/*
 * Copyright 2010-2016 by David A. Parker <parker.david.a@gmail.com>
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
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.content.Context;
import android.content.Intent;
import com.dparker.apps.checkvalve.R;

public class PlayerSearchActivity extends Activity {
    private EditText field_playersearch;
    private Button search_button;
    private Button cancel_button;

    private OnClickListener searchButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Search" button was clicked
             */

            field_playersearch = (EditText)findViewById(R.id.playersearch_field_search_for);

            // Explicitly hide the soft keyboard because sometimes it doesn't close
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(field_playersearch.getWindowToken(), 0);

            int search_string_len = field_playersearch.getText().toString().length();

            if( search_string_len == 0 ) {
                UserVisibleMessage.showMessage(PlayerSearchActivity.this, R.string.msg_empty_fields);
            }
            else {
                String search = field_playersearch.getText().toString();
                searchPlayers(search);
            }
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Cancel" button was clicked
             */

            finish();
        }
    };

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.playersearch);

        field_playersearch = (EditText)findViewById(R.id.playersearch_field_search_for);

        search_button = (Button)findViewById(R.id.playersearch_search_button);
        cancel_button = (Button)findViewById(R.id.playersearch_cancel_button);

        search_button.setOnClickListener(searchButtonListener);
        cancel_button.setOnClickListener(cancelButtonListener);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void searchPlayers( String s ) {
        Intent searchPlayersIntent = new Intent();
        searchPlayersIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.SearchPlayersActivity");
        searchPlayersIntent.putExtra(Values.EXTRA_SEARCH, s);
        startActivity(searchPlayersIntent);
    }
}