/*
 * Copyright 2010-2019 by David A. Parker <parker.david.a@gmail.com>
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
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;

/*
 * Define the RconPassword class
 */
public class RconPasswordActivity extends Activity {
    private static Bundle settings;
    
    private EditText field_password;
    private Button submit_button;
    private Button cancel_button;

    private String password;
    private Intent returned;

    private OnClickListener submitButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Submit" button was clicked
             */
            password = field_password.getText().toString().trim();

            if( password.length() == 0 ) {
                UserVisibleMessage.showMessage(RconPasswordActivity.this, R.string.msg_empty_rcon_password);
            }
            else {
                returned.putExtra(Values.EXTRA_PASSWORD, password);
                setResult(1, returned);
                finish();
            }
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Cancel" button was clicked
             */

            setResult(0);
            finish();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.rconpassword);

        settings = Values.getSettings(RconPasswordActivity.this);
        
        returned = new Intent();

        submit_button = (Button)findViewById(R.id.rconpassword_submit_button);
        submit_button.setOnClickListener(submitButtonListener);

        cancel_button = (Button)findViewById(R.id.rconpassword_cancel_button);
        cancel_button.setOnClickListener(cancelButtonListener);

        field_password = (EditText)findViewById(R.id.rconpassword_field_password);

        if( settings.getBoolean(Values.SETTING_RCON_SHOW_PASSWORDS) == true ) {
            ((CheckBox)findViewById(R.id.rconpassword_checkbox_show_password)).setChecked(true);
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        else {
            ((CheckBox)findViewById(R.id.rconpassword_checkbox_show_password)).setChecked(false);
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
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

    public void showPasswordCheckboxHandler( View view ) {
        boolean checked = ((CheckBox)view).isChecked();

        if( checked )
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        else
            field_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }
}