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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

/*
 * Define the RconPassword class
 */
public class RconPassword extends Activity
{
	private EditText field_password;
	private Button submit_button;
	private Button cancel_button;

	private String message;
	private String password;
	private Intent returned;
	
	private Context context;
	
	private OnClickListener submitButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Submit" button was clicked
	    	 */
	    	
	        field_password = (EditText)findViewById(R.id.field_password);	        
	        password = field_password.getText().toString();

	        if( password.length() == 0 )
	        {
	        	message = (String)context.getText(R.string.msg_empty_rcon_password);
	        	showMessage(message);
	        }
	        else
	        {
	        	returned.putExtra("password", password);
	        	setResult(1, returned);
	        	finish();
	        }
	    }
	};

	private OnClickListener cancelButtonListener = new OnClickListener()
	{		
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Cancel" button was clicked
	    	 */
	    	
	    	setResult(0);
	    	finish();
	    }
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		context = this;
		returned = new Intent();
		
		setContentView(R.layout.rconpassword);
		
		submit_button = (Button)findViewById(R.id.submit_button);
		submit_button.setOnClickListener(submitButtonListener);
		
		cancel_button = (Button)findViewById(R.id.cancel_button);
		cancel_button.setOnClickListener(cancelButtonListener);
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
	
	public void showMessage(String msg)
	{
		Intent messageBoxIntent = new Intent();
		messageBoxIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.MessageBox");
		messageBoxIntent.putExtra("messageText", msg);
		startActivity(messageBoxIntent);
	}
}