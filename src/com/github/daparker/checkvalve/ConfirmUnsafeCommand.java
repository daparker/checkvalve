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
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import com.github.daparker.checkvalve.R;

/*
 * Define the ConfirmUnsafeCommand class
 */
public class ConfirmUnsafeCommand extends Activity
{
    private Button sendButton;
    private Button cancelButton;
    private Intent thisIntent;

    private OnClickListener sendButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Send" button was clicked
             */

            setResult(1, thisIntent);
            finish();
        }
    };

    private OnClickListener cancelButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Cancel" button was clicked
             */

            setResult(0, thisIntent);
            finish();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        thisIntent = getIntent();

        setResult(0, thisIntent);

        setContentView(R.layout.confirmunsafecommand);

        sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(sendButtonListener);

        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(cancelButtonListener);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }
}
