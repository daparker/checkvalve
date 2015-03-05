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
 * Define the ConfirmDelete class
 */
public class ConfirmDelete extends Activity
{
    private DatabaseProvider database;
    private Button deleteButton;
    private Button cancelButton;
    private Intent thisIntent;

    private OnClickListener deleteButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Delete" button was clicked
             */

            if( deleteServer(thisIntent.getLongExtra("rowId", -1)) )
            {
                UserVisibleMessage.showMessage(ConfirmDelete.this, R.string.msg_server_deleted);
                setResult(1, thisIntent);
            }
            else
            {
                UserVisibleMessage.showMessage(ConfirmDelete.this, R.string.msg_db_failure);
            }

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

            if( database.isOpen() ) database.close();

            setResult(0, thisIntent);

            finish();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        database = new DatabaseProvider(this);
        database.open();

        thisIntent = getIntent();

        setResult(0, thisIntent);

        setContentView(R.layout.confirmdelete);

        deleteButton = (Button)findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(deleteButtonListener);

        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(cancelButtonListener);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if( database.isOpen() ) database.close();

        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if( !database.isOpen() ) database.open();
    }

    public boolean deleteServer( long rowId )
    {
        return database.deleteServer(rowId);
    }
}