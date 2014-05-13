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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/*
 * Define the AddNewServer class
 */
public class AddNewServer extends Activity
{
	private DatabaseProvider database;
	private EditText field_server;
	private EditText field_port;
	private EditText field_timeout;
	private EditText field_password;
	private Button addButton;
	private Button cancelButton;
	private Context context;
	private String message;

	private OnClickListener addButtonListener = new OnClickListener()
	{
	    public void onClick(View v)
	    {
	    	/*
	    	 * "Add" button was clicked
	    	 */
	    	
	        field_server = (EditText)findViewById(R.id.field_server);
	        field_port = (EditText)findViewById(R.id.field_port);
	        field_timeout = (EditText)findViewById(R.id.field_timeout);
	        field_password = (EditText)findViewById(R.id.field_password);
	        
	        int server_len = field_server.getText().toString().length();
	        int port_len = field_port.getText().toString().length();
	        int timeout_len = field_timeout.getText().toString().length();
	        
	        if( (server_len == 0) || (port_len == 0) || (timeout_len == 0) )
	        {
	    		message = (String)context.getText(R.string.msg_empty_fields);
	    		showMessage(message);
	        }
	        else
	        {
	        	String server = field_server.getText().toString();
		    	int port = Integer.parseInt(field_port.getText().toString());
		    	int timeout = Integer.parseInt(field_timeout.getText().toString());
		    	String password = field_password.getText().toString();

		    	if( password.length() == 0 ) { password = ""; }
		    	
		    	if( checkServer(server, port, timeout) )
		    	{
		    		long result = addServer(server, port, timeout, password);
		    		
		    		if( result > -1 )
		    		{
	    	    		message = (String)context.getString(R.string.msg_success);
	    	    		setResult(1);
		    		}
		    		else
		    		{
	    	    		message = (String)context.getString(R.string.msg_db_failure);
		    		}
        	        
		    		showMessage(message);
		    		
			        database.close();
			        finish();
		    	}
		    	else
		    	{
		    		message = (String)context.getText(R.string.msg_unknown_host) + server + "\n";
		    		showMessage(message);
		    	}
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
	    	
	    	if( database.isOpen() )
	    		database.close();
	    	
	    	finish();
	    }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		database = new DatabaseProvider(this);
		message = new String();
		context = this;
		
		setResult(0);

		setContentView(R.layout.addnewserver);
		
		addButton = (Button)findViewById(R.id.addServerButton);
		addButton.setOnClickListener(addButtonListener);
		
		cancelButton = (Button)findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(cancelButtonListener);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();	
		database.close();
		finish();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();

		database.open();
		
		addButton = (Button)findViewById(R.id.addServerButton);
		addButton.setOnClickListener(addButtonListener);
		
		cancelButton = (Button)findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(cancelButtonListener);
	}
	
	public boolean checkServer(String server, int port, int timeout)
	{		
        // A2S_INFO query string
        String queryString = "\u00FF\u00FF\u00FF\u00FF\u0054Source Engine Query\0";

        try
        {
    		// Create a UDP socket
        	DatagramSocket socket = new DatagramSocket();
	        socket.setSoTimeout( timeout*1000 );

	        // Byte buffers for packet data
	        byte[] bufferOut = queryString.getBytes("ISO8859_1");
	        byte[] bufferIn  = new byte[1400];

	        // UDP datagram packets
	        DatagramPacket packetOut = new DatagramPacket(bufferOut,bufferOut.length);
	        DatagramPacket packetIn = new DatagramPacket(bufferIn,bufferIn.length);

	        // Connect to the remote server
	        socket.connect(InetAddress.getByName(server),port);

	        // Send the query string to the server
	        socket.send( packetOut );

        	// Receive the response packet from the server
            socket.receive( packetIn );

            // Close the UDP socket
	        socket.close();
		}
        // Handle an unknown host exception
        catch( UnknownHostException uhe )
        {
        	return false;
        }
        // Handle any other exception
        catch( Exception e )
        {
        	return true;
        }

        return true;
	}
	
	public long addServer(String server, int port, int timeout, String password)
	{
    	return database.insertServer(server, port, timeout, password);
	}
	
	public void showMessage(String msg)
	{
		Intent messageBoxIntent = new Intent();
		messageBoxIntent.setClassName("com.dparker.apps.checkvalve","com.dparker.apps.checkvalve.MessageBox");
		messageBoxIntent.putExtra("messageText", msg);
		startActivity(messageBoxIntent);
	}
}