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
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class DebugConsoleActivity extends Activity {

    private final OnClickListener dismissButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Dismiss" button was clicked
             */
            v.setBackgroundColor(getResources().getColor(R.color.steam_blue));
            finish();
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( ViewConfiguration.get(this).hasPermanentMenuKey() )
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent thisIntent = this.getIntent();
        String debugText = thisIntent.getStringExtra(Values.EXTRA_DEBUG_TEXT);
        setResult(0, thisIntent);

        setContentView(R.layout.debug_console);

        Button dismiss_button = findViewById(R.id.debugconsole_dismiss_button);
        dismiss_button.setOnClickListener(dismissButtonListener);

        TextView debug_console = findViewById(R.id.debugconsole_textview);
        debug_console.setText(debugText);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}