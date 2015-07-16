/*
 * Copyright 2010-2015 by David A. Parker <parker.david.a@gmail.com>
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
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.TextView;

public class MessageBox extends Activity
{	
	private TextView messagebox;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// Get the Intent that launched this Activity
		Intent thisIntent = getIntent();
		
		// Get the message text that was passed in from the caller
		String messageText = thisIntent.getStringExtra("messageText");

		// Handler for displaying and closing this window
		Handler handler = new Handler();

		// Time to wait before closing the message box window (in milliseconds)
		int delay = 2000;
		
		setContentView(R.layout.messagebox);
		
		messagebox = (TextView)findViewById(R.id.messagebox);
		messagebox.setPadding(10,10,10,10);
		messagebox.setText(messageText);
	    		
		// Close this window once the delay interval is reached
	    handler.postDelayed(new Runnable()
	    {
	    	public void run()
	    	{
	    		finish();
	    	} 
	    }, delay); 
	}
}

