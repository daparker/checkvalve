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
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeoutException;
import com.github.daparker.checkvalve.R;
import com.github.koraktor.steamcondenser.exceptions.RCONBanException;
import com.github.koraktor.steamcondenser.exceptions.RCONNoAuthException;
import com.github.koraktor.steamcondenser.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;

public class ChatUI extends Activity
{
    private final static String TAG = ChatUI.class.getSimpleName();

    private Animation fade_in;
    private Animation fade_out;
    private Button say_button;
    private Chat chatRunnable;
    private Context context;
    private EditText field_command;
    private NetworkEventReceiver receiverRunnable;
    private ProgressDialog p;
    private ScrollView layout;
    private SimpleDateFormat sdf;
    private String chatRelayIP;
    private String chatRelayPassword;
    private String chatRelayPort;
    private String command;
    private String gameServerIP;
    private String gameServerPort;
    private String rconPassword;
    private String rconServer;
    private String st;
    private TableLayout chat_table;
    private TableLayout.LayoutParams bottomRowParams;
    private TableRow bottomRow;
    private TableRow row;
    private TableRow spacerRow;
    private TableRow topRow;
    private TableRow.LayoutParams chatTextParams;
    private TableRow.LayoutParams notificationDateParams;
    private TableRow.LayoutParams notificationTextParams;
    private TextView msgPlayerName;
    private TextView msgPlayerSays;
    private TextView msgPlayerTeam;
    private TextView msgSayTeam;
    private TextView msgTimestamp;
    private TextView notificationDate;
    private TextView notificationText;
    private TextView sending;
    private TextView subtitle;
    private Thread chatThread;
    private Thread receiverThread;

    private boolean rconIsAuthenticated;
    private boolean rconPasswordDialogDismissed;
    private boolean scrollLock;
    private int maxRows;
    private int rowNum;
    private int rconPort;
    private int rconTimeout;
    private short[] engine;

    private SourceServer s;
    private GoldSrcServer g;

    private OnClickListener sayButtonListener = new OnClickListener()
    {
        public void onClick( View v )
        {
            /*
             * "Say" button was clicked
             */

            String message = field_command.getText().toString();

            if( message.length() == 0 )
                UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_empty_rcon_command);
            else
                sendCommand(message);
        }
    };

    private OnKeyListener enterKeyListener = new OnKeyListener()
    {
        public boolean onKey( View v, int k, KeyEvent e )
        {
            /*
             * "Enter" or "Done" key was pressed
             */

            if( (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (e.getAction() == KeyEvent.ACTION_UP) )
            {
                String message = field_command.getText().toString();

                if( message.length() == 0 )
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_empty_rcon_command);
                else
                    sendCommand(message);

                return true;
            }

            return false;
        }
    };

    private OnTouchListener touchListener = new OnTouchListener()
    {
        public boolean onTouch( View v, MotionEvent m )
        {
            /*
             * Screen was touched
             */
            scrollLock = (m.getAction() == MotionEvent.ACTION_UP)?false:true;
            return false;
        }
    };

    // Handler for the Chat Relay client thread
    private Handler chatClientHandler = new Handler()
    {
        public void handleMessage( Message msg )
        {
            /*
             * Message object "what" codes:
             * -2  =  Fatal exception (probably an IOException on the socket)
             * -1  =  Failed to connect to the chat relay (probably a SocketException)
             *  1  =  A heartbeat was received from the server
             *  3  =  Connection failure (includes error message as String object)
             *  4  =  Connection successful
             *  5  =  Chat message (includes ChatMessage object)
             *  
             *  NOTE: Codes >= 0 correspond to response types sent from the Chat Relay
             */

            if( p.isShowing() ) p.dismiss();

            switch( msg.what )
            {
                case -2:
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_connection_error);
                    getChatRelayDetails(chatRelayIP, chatRelayPort, null);
                    break;
                case -1:
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_chat_connect_failure);
                    getChatRelayDetails(chatRelayIP, chatRelayPort, null);
                    break;
                case 1:
                    break;
                case 3:
                    String errorMsg = (String)context.getText(R.string.msg_chat_connection_refused) + " "
                            + (String)msg.obj;
                    UserVisibleMessage.showMessage(ChatUI.this, errorMsg);
                    getChatRelayDetails(chatRelayIP, chatRelayPort, null);

                    break;
                case 4:
                    notificationText = (TextView)View.inflate(context, R.layout.chat_textview_notification_text, null);
                    notificationText.setText(R.string.msg_chat_connect_success);
                    notificationText.setTypeface(null, Typeface.BOLD);
                    notificationTextParams.span = 2;
                    notificationText.setLayoutParams(notificationTextParams);

                    notificationDate = (TextView)View.inflate(context, R.layout.chat_textview_notification_date, null);
                    notificationDate.setText(sdf.format(System.currentTimeMillis()));
                    notificationDateParams.span = 2;
                    notificationDate.setLayoutParams(notificationDateParams);

                    row = (TableRow)View.inflate(context, R.layout.chat_tablerow_notification, null);
                    row.addView(notificationText);
                    row.addView(notificationDate);
                    row.setId(rowNum);
                    chat_table.addView(row);

                    spacerRow = (TableRow)View.inflate(context, R.layout.chat_tablerow_spacer, null);
                    spacerRow.setLayoutParams(bottomRowParams);
                    spacerRow.setId(rowNum);
                    chat_table.addView(spacerRow, bottomRowParams);

                    rowNum++;

                    if( !scrollLock ) layout.fullScroll(View.FOCUS_DOWN);
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_chat_connect_success);

                    if( !rconPasswordDialogDismissed )
                    {
                        if( rconPassword.isEmpty() )
                            getRCONPassword();
                        else
                            rconAuthenticate();
                    }

                    break;
                case 5:
                    try
                    {
                        ChatMessage chatMsg = (ChatMessage)msg.obj;

                        st = (chatMsg.sayTeamFlag == (byte)0x00)?"Say":"Say Team";

                        msgTimestamp = (TextView)View.inflate(context, R.layout.chat_textview_top, null);
                        msgTimestamp.setText(chatMsg.messageTimestamp.substring(13));

                        msgPlayerName = (TextView)View.inflate(context, R.layout.chat_textview_top, null);
                        msgPlayerName.setText(chatMsg.playerName);
                        msgPlayerName.setTypeface(null, Typeface.BOLD);

                        msgPlayerTeam = (TextView)View.inflate(context, R.layout.chat_textview_top, null);
                        msgPlayerTeam.setText("(" + chatMsg.playerTeam + ")");

                        msgSayTeam = (TextView)View.inflate(context, R.layout.chat_textview_top, null);
                        msgSayTeam.setText(st + ":");

                        msgPlayerSays = (TextView)View.inflate(context, R.layout.chat_textview_bottom, null);
                        msgPlayerSays.setText(chatMsg.message);

                        topRow = (TableRow)View.inflate(context, R.layout.chat_tablerow_top, null);
                        topRow.setId(rowNum);
                        topRow.addView(msgTimestamp);
                        topRow.addView(msgPlayerName);
                        topRow.addView(msgPlayerTeam);
                        topRow.addView(msgSayTeam);

                        bottomRow = (TableRow)View.inflate(context, R.layout.chat_tablerow_bottom, null);
                        bottomRow.setId(rowNum);
                        bottomRow.addView(msgPlayerSays);
                        chatTextParams = (TableRow.LayoutParams)msgPlayerSays.getLayoutParams();
                        chatTextParams.span = 4;

                        msgPlayerSays.setLayoutParams(chatTextParams);
                        bottomRow.setLayoutParams(bottomRowParams);

                        chat_table.addView(topRow);
                        chat_table.addView(bottomRow, bottomRowParams);

                        if( !scrollLock )
                        {
                            while( ((chat_table.getChildCount()) / 2) > maxRows )
                            {
                                chat_table.removeViews(0, 2);
                                Log.d(TAG, "Removed 2 views from table");
                            }

                            layout.fullScroll(View.FOCUS_DOWN);
                        }
                        else
                        {
                            Log.d(TAG, "Scroll lock is active, not removing any views");
                        }

                        rowNum++;
                    }
                    catch( Exception e )
                    {
                        Log.w(TAG, "Caught an exception while reading chat message object:");
                        Log.w(TAG, e.toString());

                        StackTraceElement[] ste = e.getStackTrace();

                        for( int i = 0; i < ste.length; i++ )
                            Log.e(TAG, "    " + ste[i].toString());
                    }

                    break;
                case 255:
                    Log.d(TAG, "Handler received 255 (server closed connection)");
                    break;
                default:
                    Log.w(TAG, "Handler received an unexpected value (" + msg.what + ")");
                    break;
            }
        }
    };

    // Handler for the "Sending" pop-up thread
    private Handler popUpHandler = new Handler()
    {
        public void handleMessage( Message msg )
        {
            // sending.setVisibility(-1);
            runFadeOutAnimation(context, sending);

            switch( msg.what )
            {
                case 0:
                    break;
                case 1:
                    // Handle RCONNoAuthException
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_no_rcon_auth);
                    getRCONPassword();
                    break;
                case 2:
                    // Handle any other exception
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_rcon_general_error);
                    break;
            }
        }
    };

    // Handler for the network event receiver thread
    private Handler networkEventHandler = new Handler()
    {
        @Override
        public void handleMessage( Message msg )
        {
            /*
             * Message object "what" codes:
             * -2  =  Fatal exception in the NetworkEventReceiver thread
             * -1  =  No network connectivity
             *  0  =  Initial event from broadcast receiver (should be ignored)
             *  1  =  Network connection change
             */

            Log.d(TAG, "Received " + msg.what + " from NetworkEventReceiver");

            switch( msg.what )
            {
                case -2:
                    Log.e(TAG, "The network event receiver has aborted");
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_general_error);
                    finish();
                    break;
                case -1:
                    notificationText = (TextView)View.inflate(context, R.layout.chat_textview_notification_text, null);
                    notificationText.setText(R.string.msg_chat_connection_lost);
                    notificationText.setTypeface(null, Typeface.BOLD);
                    notificationTextParams.span = 2;
                    notificationText.setLayoutParams(notificationTextParams);

                    notificationDate = (TextView)View.inflate(context, R.layout.chat_textview_notification_date, null);
                    notificationDate.setText(sdf.format(System.currentTimeMillis()));
                    notificationDateParams.span = 2;
                    notificationDate.setLayoutParams(notificationDateParams);

                    row = (TableRow)View.inflate(context, R.layout.chat_tablerow_notification, null);
                    row.setId(rowNum);
                    row.addView(notificationText);
                    row.addView(notificationDate);
                    chat_table.addView(row);

                    spacerRow = (TableRow)View.inflate(context, R.layout.chat_tablerow_spacer, null);
                    spacerRow.setLayoutParams(bottomRowParams);
                    spacerRow.setId(rowNum);
                    chat_table.addView(spacerRow, bottomRowParams);

                    rowNum++;

                    shutdownChatRelayConnection();

                    if( !scrollLock ) layout.fullScroll(View.FOCUS_DOWN);
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_connection_lost);
                    break;
                case 0:
                    break;
                case 1:
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_network_change);

                    try
                    {
                        // Attempt a new connection to the chat relay
                        getChatRelayConnection();
                    }
                    catch( UnknownHostException u )
                    {
                        String errorMsg = (String)context.getText(R.string.msg_unknown_host) + " " + chatRelayIP;
                        UserVisibleMessage.showMessage(ChatUI.this, errorMsg);
                        getChatRelayDetails(chatRelayIP, chatRelayPort, chatRelayPassword);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // Handler for the RCON authentication thread
    private Handler rconAuthHandler = new Handler()
    {
        @Override
        public void handleMessage( Message msg )
        {
            runFadeOutAnimation(ChatUI.this, sending);

            if( msg.what == 0 )
            {
                int rconAuthStatus = checkRCON();
                
                switch( rconAuthStatus )
                {
                    case 1:
                        // Failed authentication
                        UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_no_rcon_auth);
                        getRCONPassword();
                        break;
                    case 2:
                        // RCONNoAuthException
                        UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_no_rcon_auth);
                        getRCONPassword();
                        break;
                    case 3:
                        // RCONBanException
                        UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_rcon_ban_exception);
                        break;
                    case 4:
                        // TimeoutException (happens if RCON password was already sent)
                        UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_rcon_timeout_exception);
                        break;
                    case 5:
                        // Any other exception
                        UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_rcon_general_error);
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chatui);

        Intent thisIntent = getIntent();

        context = ChatUI.this;
        sdf = new SimpleDateFormat("HH:mm:ss");
        rowNum = 0;
        maxRows = 1000;

        try
        {
            gameServerIP = InetAddress.getByName(thisIntent.getStringExtra("server")).getHostAddress();
            gameServerPort = Integer.toString(thisIntent.getIntExtra("port", 27015));
        }
        catch( UnknownHostException e )
        {
            String errorMsg = new String();
            
            errorMsg += (String)context.getText(R.string.msg_unknown_host) + " ";
            errorMsg += thisIntent.getStringExtra("server");

            UserVisibleMessage.showMessage(ChatUI.this, errorMsg);

            Log.w(TAG, "onCreate(): Unknown host " + thisIntent.getStringExtra("server"));

            finish();
        }
        catch( Exception e )
        {
            UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_general_error);

            Log.w(TAG, "onCreate(): Caught an exception:");
            Log.w(TAG, e.toString());

            StackTraceElement[] ste = e.getStackTrace();

            for( int i = 0; i < ste.length; i++ )
                Log.w(TAG, "    " + ste[i].toString());

            finish();
        }

        rconIsAuthenticated = false;
        rconPasswordDialogDismissed = false;
        rconServer = thisIntent.getStringExtra("server");
        rconPort = thisIntent.getIntExtra("port", 27015);
        rconPassword = thisIntent.getStringExtra("password");
        rconTimeout = thisIntent.getIntExtra("timeout", 2);

        engine = new short[1];

        fade_in = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        fade_out = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        scrollLock = false;

        setResult(1);

        bottomRowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        bottomRowParams.setMargins(0, 0, 0, 5);

        notificationTextParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
        notificationDateParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);

        chat_table = (TableLayout)findViewById(R.id.chat_table);

        layout = (ScrollView)findViewById(R.id.scrollview);
        layout.setOnTouchListener(touchListener);

        subtitle = (TextView)findViewById(R.id.subtitle);
        subtitle.setText(gameServerIP + ":" + gameServerPort);

        sending = (TextView)findViewById(R.id.sending);
        sending.setVisibility(-1);

        field_command = (EditText)findViewById(R.id.field_command);
        field_command.setOnKeyListener(enterKeyListener);

        say_button = (Button)findViewById(R.id.say_button);
        say_button.setOnClickListener(sayButtonListener);

        receiverRunnable = new NetworkEventReceiver(this, networkEventHandler);

        if( receiverRunnable == null )
        {
            Log.e(TAG, "onCreate(): NetworkEventReceiver object is null, cannot continue");
            UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_general_error);
            finish();
        }
        else
        {
            receiverThread = new Thread(receiverRunnable);

            if( receiverThread == null )
            {
                Log.e(TAG, "onCreate(): NetworkEventReceiver thread is null, cannot continue");
                UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_general_error);
                finish();
            }

            receiverThread.start();
            receiverRunnable.registerReceiver();
        }

        chatRelayIP = CheckValve.settings.getString(Values.SETTING_DEFAULT_RELAY_HOST);
        chatRelayPort = CheckValve.settings.getString(Values.SETTING_DEFAULT_RELAY_PORT);
        chatRelayPassword = CheckValve.settings.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD);
        
        getChatRelayDetails(chatRelayIP, chatRelayPort, chatRelayPassword);
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
    public void onDestroy()
    {
        super.onDestroy();
        shutdownNetworkEventReceiver();
        shutdownChatRelayConnection();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rcon_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.back:
                UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_chat_disconnected);
                finish();
                return true;
            case R.id.clear_console:
                chat_table.removeAllViews();
                chat_table.invalidate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult( int request, int result, Intent data )
    {
        if( request == Values.ACTIVITY_CHAT_RELAY_DETAILS_DIALOG )
        {
            if( result == 0 ) finish();
            if( result == 1 )
            {
                chatRelayIP = data.getStringExtra("server");
                chatRelayPort = data.getStringExtra("port");
                chatRelayPassword = data.getStringExtra("password");

                try
                {
                    getChatRelayConnection();
                }
                catch( UnknownHostException u )
                {
                    String errorMsg = (String)context.getText(R.string.msg_unknown_host) + " " + chatRelayIP;
                    UserVisibleMessage.showMessage(ChatUI.this, errorMsg);
                    getChatRelayDetails(chatRelayIP, chatRelayPort, null);
                }
            }
        }
        else if( request == Values.ACTIVITY_RCON_PASSWORD_DIALOG )
        {
            if( result == 0 )
            {
                rconPasswordDialogDismissed = true;
            }
            if( result == 1 )
            {
                rconPasswordDialogDismissed = false;
                rconPassword = data.getStringExtra("password");
                rconAuthenticate();
            }
        }

        return;
    }

    public void getRCONPassword()
    {
        Intent rconPasswordIntent = new Intent();
        rconPasswordIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.RconPassword");
        startActivityForResult(rconPasswordIntent, Values.ACTIVITY_RCON_PASSWORD_DIALOG);
    }

    public int checkRCON()
    {
        boolean authenticated = false;

        try
        {
            rconIsAuthenticated = false;

            if( engine[0] == Values.ENGINE_GOLDSRC )
            {
                g = new GoldSrcServer(InetAddress.getByName(rconServer), rconPort);
                authenticated = g.rconAuth(rconPassword);
                g.rconExec("status");
            }
            else
            {
                s = new SourceServer(InetAddress.getByName(rconServer), rconPort);
                authenticated = s.rconAuth(rconPassword);
            }

            if( ! authenticated )
                return 1;
            else
                rconIsAuthenticated = true;

            return 0;
        }
        catch( RCONNoAuthException e )
        {
            return 2;
        }
        catch( RCONBanException e )
        {
            return 3;
        }
        catch( TimeoutException e )
        {
            return 4;
        }
        catch( Exception e )
        {
            Log.w(TAG, e.toString());
            return 5;
        }
    }

    public void rconAuthenticate()
    {
        sending.setText(R.string.status_rcon_verifying_password);
        runFadeInAnimation(context, sending);

        new Thread(new ServerQuery(context, rconServer, rconPort, rconTimeout, engine, rconAuthHandler)).start();
    }

    public void sendCommand( String message )
    {
        if( !rconIsAuthenticated )
        {
            rconAuthenticate();
            sendCommand(message);
        }
        else
        {
            if( message.length() == 0 )
            {
                UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_empty_rcon_command);
            }
            else
            {
                command = "say " + message;
                field_command.setText("");
                sending.setText(R.string.status_rcon_sending);
                runFadeInAnimation(ChatUI.this, sending);

                if( engine[0] == 1 )
                    new Thread(new RconQuery(command, g, popUpHandler)).start();
                else
                    new Thread(new RconQuery(command, s, popUpHandler)).start();
            }
        }
    }

    public void getChatRelayDetails( String ip, String port, String pswd )
    {
        if( ip == null ) ip = "";
        if( port == null ) port = "";
        if( pswd == null ) pswd = "";

        Intent chatRelayDetailsIntent = new Intent();
        chatRelayDetailsIntent.setClassName("com.github.daparker.checkvalve", "com.github.daparker.checkvalve.ChatRelayDetails");
        chatRelayDetailsIntent.putExtra("server", ip);
        chatRelayDetailsIntent.putExtra("port", port);
        chatRelayDetailsIntent.putExtra("password", pswd);
        startActivityForResult(chatRelayDetailsIntent, Values.ACTIVITY_CHAT_RELAY_DETAILS_DIALOG);
    }

    public void getChatRelayConnection() throws UnknownHostException
    {
        // Kill the current connection
        shutdownChatRelayConnection();

        // Show the progress dialog
        p = ProgressDialog.show(this, "", context.getText(R.string.status_connecting), true, false);

        try
        {
            Log.d(TAG, "getChatRelayConnection(): Getting new instance of class " + Chat.class.getSimpleName());
            chatRunnable = new Chat(chatRelayIP, chatRelayPort, chatRelayPassword, gameServerIP, gameServerPort, chatClientHandler);

            if( chatRunnable != null )
            {
                Log.d(TAG, "getChatRelayConnection(): New object is " + chatRunnable.toString());
            }
            else
            {
                Log.d(TAG, "getChatRelayConnection(): Failed to get an instance of class " + Chat.class.getSimpleName());
                UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_chat_not_started);
                finish();
            }

            Log.d(TAG, "getChatRelayConnection(): Getting new Thread for runnable " + chatRunnable.toString());
            chatThread = new Thread(chatRunnable);

            if( chatThread != null )
            {
                Log.d(TAG, "getChatRelayConnection(): Calling start() on " + chatThread.toString());
                chatThread.start();

                if( chatThread.isAlive() )
                {
                    Log.d(TAG, "getChatRelayConnection(): Thread " + chatThread.toString() + " is running");
                }
                else
                {
                    Log.d(TAG, "getChatRelayConnection(): Failed to start thread " + chatThread.toString());
                    UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_chat_not_started);
                    finish();
                }
            }
            else
            {
                Log.d(TAG, "getChatRelayConnection(): Failed to get Thread for runnable " + chatRunnable.toString());
                UserVisibleMessage.showMessage(ChatUI.this, R.string.msg_chat_not_started);
                finish();
            }
        }
        catch( UnknownHostException uhe )
        {
            String errorMsg = (String)context.getText(R.string.msg_unknown_host) + " " + chatRelayIP;
            UserVisibleMessage.showMessage(ChatUI.this, errorMsg);
            getChatRelayDetails(chatRelayIP, chatRelayPort, null);
        }
    }

    public void runFadeInAnimation( Context c, View v )
    {
        v.startAnimation(fade_in);
    }

    public void runFadeOutAnimation( Context c, View v )
    {
        v.startAnimation(fade_out);
    }

    public void shutdownChatRelayConnection()
    {
        if( chatRunnable != null )
        {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat object is not null, calling shutDown()");
            chatRunnable.shutDown();
        }
        else
        {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat object is null");
        }

        if( chatThread != null )
        {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is not null");

            if( chatThread.isAlive() )
            {
                Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is alive, calling interrupt() on thread "
                        + chatThread.getId());
                
                chatThread.interrupt();
            }
            else
            {
                Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is not alive, not interrupting");
            }
        }
        else
        {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is null");
        }
    }

    public void shutdownNetworkEventReceiver()
    {
        if( receiverRunnable != null )
        {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver object is not null, calling unregisterReceiver()");
            receiverRunnable.unregisterReceiver();
            receiverRunnable.shutDown();
        }
        else
        {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver object is null");
        }

        if( receiverThread != null )
        {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is not null");

            if( receiverThread.isAlive() )
            {
                Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is alive, calling interrupt() on thread "
                        + receiverThread.getId());
                
                receiverThread.interrupt();
            }
            else
            {
                Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is not alive, not interrupting");
            }
        }
        else
        {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is null");
        }
    }
}