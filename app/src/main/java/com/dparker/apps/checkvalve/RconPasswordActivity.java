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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;

/*
 * Define the RconPassword class
 */
public class RconPasswordActivity extends Activity {
    private EditText field_password;
    private Intent returned;

    private final OnClickListener submitButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Submit" button was clicked
             */
            String password = field_password.getText().toString().trim();

            if( password.isEmpty() ) {
                UserVisibleMessage.showMessage(RconPasswordActivity.this, R.string.msg_empty_rcon_password);
            }
            else {
                returned.putExtra(Values.EXTRA_PASSWORD, password);
                setResult(1, returned);
                finish();
            }
        }
    };

    private final OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Cancel" button was clicked
             */

            setResult(0);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.rconpassword);

        Bundle settings = Values.getSettings(RconPasswordActivity.this);

        Button submit_button = findViewById(R.id.rconpassword_submit_button);
        submit_button.setOnClickListener(submitButtonListener);

        Button cancel_button = findViewById(R.id.rconpassword_cancel_button);
        cancel_button.setOnClickListener(cancelButtonListener);

        field_password = findViewById(R.id.rconpassword_field_password);

        if( settings.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS) ) {
            ((CheckBox) findViewById(R.id.rconpassword_checkbox_show_password)).setChecked(true);
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        else {
            ((CheckBox) findViewById(R.id.rconpassword_checkbox_show_password)).setChecked(false);
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

        returned = new Intent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void showPasswordCheckboxHandler(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        if( checked )
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        else
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}