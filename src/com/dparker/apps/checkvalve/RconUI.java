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

package com.dparker.apps.checkvalve;

import android.app.Activity;
import android.app.ProgressDialog;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;

import java.net.*;
import java.util.concurrent.TimeoutException;

import com.github.koraktor.steamcondenser.exceptions.RCONBanException;
import com.github.koraktor.steamcondenser.exceptions.RCONNoAuthException;
import com.github.koraktor.steamcondenser.steam.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.steam.servers.SourceServer;

public class RconUI extends Activity
{	
	private Animation fade_in;
	private Animation fade_out;
	
	private ProgressDialog p;
	private ServerQuery q;
	
	private TextView rcon_console;
	private TextView sending;
	private EditText field_command;
	private Button send_button;
	
	private String message;
	private String password;
	private String command;
	private String [] response;
	private String server;
	private int port;
	private int timeout;
	private short [] engine;
	
	private SourceServer s;
	private GoldSrcServer g;
	
	private Context context;
	
	private final static short RCON_PASSWORD_DIALOG = 0;
	private final static short SHOW_BAD_PASSWORD_MESSAGE = 1;
	
	private OnClickListener sendButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Send" button was clicked
	    	 */

	    	sendCommand();
	    }
	};

	private OnKeyListener enterKeyListener = new OnKeyListener()
	{
		public boolean onKey(View v, int k, KeyEvent e)
		{
			/*
			 * "Enter" or "Done" key was pressed
			 */
			
			if( (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (e.getAction() == KeyEvent.ACTION_UP) )
			{
				sendCommand();
				return true;
			}
			
			return false;
		}
	};
	
    // Handler for the server query thread
    private Handler progressHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
        	boolean authenticated = false;
        	
    		try
    		{
    			if( engine[0] == 1 )
    			{
    				g = new GoldSrcServer(InetAddress.getByName(server), port);
    				authenticated = g.rconAuth(password);
    				g.rconExec("status");
    			}
    			else
    			{
    				s = new SourceServer(InetAddress.getByName(server), port);
    				authenticated = s.rconAuth(password);
    			}
    			
    			if( ! authenticated ) { showBadPasswordMessage(); }
    		}
        	catch(RCONNoAuthException e)
        	{
        		// Handle RCONNoAuthException
        		showBadPasswordMessage();
        	}
        	catch(RCONBanException e)
        	{
        		// Handle RCONNoAuthException
        		rcon_console.setText((String)context.getText(R.string.msg_rcon_ban_exception));
        	}
        	catch(TimeoutException e)
        	{
        		// Ignore a TimeoutException (happens if RCON password was already sent)
        		//return;
        		rcon_console.setText((String)context.getText(R.string.msg_rcon_timeout_exception));
        	}
        	catch(Exception e)
        	{
        		// Handle any other exception
        		message = (String)context.getString(R.string.msg_rcon_general_error);
        		showMessage(message);
        	}
        	
        	// Dismiss the progress dialog
            p.dismiss();
        }
    };
    
    // Handler for the "Sending" pop-up thread
    private Handler popUpHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
        	//sending.setVisibility(-1);
        	runFadeOutAnimation(context, sending);

        	switch( msg.what )
        	{
        		case 0:
            		rcon_console.append("> " + command + "\n\n");
            		rcon_console.append(response[0] + "\n\n");
            		scrollToBottomOfText();
            		break;
        		case 1:
    	        	// Handle RCONNoAuthException
    	        	showBadPasswordMessage();
    	        	break;
        		case 2:
    	        	// Handle any other exception
    	        	message = (String)context.getString(R.string.msg_rcon_general_error);
    	        	showMessage(message);
    	        	break;
        	}
        }
    };
    
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	
		context = this;
	
		Intent thisIntent = getIntent();
		
		server = thisIntent.getStringExtra("server");
		port = thisIntent.getIntExtra("port", 27015);
		timeout = thisIntent.getIntExtra("timeout", 2);
		password = thisIntent.getStringExtra("password");
		engine = new short[1];
		response = new String[1];
    
		fade_in = AnimationUtils.loadAnimation(context, R.anim.fade_in);
		fade_out = AnimationUtils.loadAnimation(context, R.anim.fade_out);
		
		setResult(1);
		
		setContentView(R.layout.rcon);

		rcon_console = (TextView)findViewById(R.id.rcon_console);
		sending = (TextView)findViewById(R.id.sending);
		field_command = (EditText)findViewById(R.id.field_command);
		send_button = (Button)findViewById(R.id.send_button);
		
		send_button.setOnClickListener(sendButtonListener);
		rcon_console.setMovementMethod(ScrollingMovementMethod.getInstance());
		rcon_console.setHorizontallyScrolling(true);
		field_command.setOnKeyListener(enterKeyListener);
		
		sending.setVisibility(-1);
		
		if( password.length() == 0 )
			getPassword();
		else
			getServerType();
	}
    
	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.rcon_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.back:
	            finish();
	            return true;
	        case R.id.clear_console:
	        	rcon_console.setText("");
	        	rcon_console.scrollTo(0, 0);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
    public void onActivityResult(int request, int result, Intent data)
    {    	
    	if( request == RCON_PASSWORD_DIALOG )
    	{
    		if( result == 0 )
    			finish();
    		
    		if( result == 1 )
    		{
	    		password = data.getStringExtra("password");
	    		getServerType();
    		}
    	}
    	else if( request == SHOW_BAD_PASSWORD_MESSAGE )
    	{
    		getPassword();
    	}
    }

    public void sendCommand()
    {
		command = field_command.getText().toString();

        if( command.length() == 0 )
        {
        	// Handle empty command field here
        	message = (String)context.getText(R.string.msg_empty_rcon_command);
        	showMessage(message);
        }
        else
        {
        	field_command.setText("");
        		        	
        	runFadeInAnimation(context, sending);
        	
    		if( engine[0] == 1 )
    			q = new ServerQuery(context, command, response, null, g, popUpHandler);
    		else
    			q = new ServerQuery(context, command, response, s, null, popUpHandler);
        		
    		q.start();
        }
    }
    
	public void getServerType()
	{
		// Show the progress dialog
		p = ProgressDialog.show(this, "", context.getText(R.string.status_connecting), true, false);
        
		// Run the server queries in a new thread
		q = new ServerQuery(context, server, port, timeout, engine, progressHandler);
		q.start();
	}
    
    public void scrollToBottomOfText()
    {
    	/*
    	 * I can't take credit for this. This is based on a little trick I found at:
    	 * http://groups.google.com/group/android-developers/browse_thread/thread/8752156cca1e3742
    	 */
    	int lineCount = rcon_console.getLineCount();
    	int lineHeight = rcon_console.getLineHeight();
    	int viewHeight = rcon_console.getHeight();

    	int difference = (lineCount * lineHeight ) - viewHeight;

    	if( difference < 1 ) { return; }

    	rcon_console.scrollTo(0, difference);
    }
    
    public void runFadeInAnimation(Context c, View v)
    {
    	v.startAnimation(fade_in);
    }

    public void runFadeOutAnimation(Context c, View v)
    {
    	v.startAnimation(fade_out);
    }
    
    public void getPassword()
    {
		Intent rconPasswordIntent = new Intent();
		rconPasswordIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.RconPassword");
		startActivityForResult(rconPasswordIntent, RCON_PASSWORD_DIALOG);
    }
    
	public void showBadPasswordMessage()
	{
		String msg = (String)context.getText(R.string.msg_no_rcon_auth);
		
		Intent messageBoxIntent = new Intent();
		messageBoxIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.MessageBox");
		messageBoxIntent.putExtra("messageText", msg);
		startActivityForResult(messageBoxIntent, SHOW_BAD_PASSWORD_MESSAGE);
	}
	
	public void showMessage(String msg)
	{
		Intent messageBoxIntent = new Intent();
		messageBoxIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.MessageBox");
		messageBoxIntent.putExtra("messageText", msg);
		startActivity(messageBoxIntent);
	}
}