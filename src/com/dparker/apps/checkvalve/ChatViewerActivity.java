/*
 * Copyright 2010-2016 by David A. Parker <parker.david.a@gmail.com>
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
import android.view.ViewConfiguration;
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
import android.content.res.Configuration;
import android.graphics.Typeface;
import java.io.File;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.dparker.apps.checkvalve.R;
import com.github.koraktor.steamcondenser.servers.GoldSrcServer;
import com.github.koraktor.steamcondenser.servers.SourceServer;

@SuppressLint("HandlerLeak")
public class ChatViewerActivity extends Activity {
    private final static String TAG = ChatViewerActivity.class.getSimpleName();

    private Animation fade_in;
    private Animation fade_out;
    private Button say_button;
    private Chat chatRunnable;
    private EditText message_field;
    private NetworkEventReceiver receiverRunnable;
    private ProgressDialog p;
    private ScrollView layout;
    private SimpleDateFormat sdf;
    private String chatRelayIP;
    private String chatRelayPassword;
    private String chatRelayPort;
    private String command;
    private String gameServerIP;
    private String message;
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
    private DatabaseProvider database;

    private boolean rconIsAuthenticated;
    private boolean rconPasswordDialogDismissed;
    private boolean scrollLock;
    private int maxRows;
    private int rowNum;
    private int rconPort;
    private int rconTimeout;
    private int engine;

    private SourceServer s;
    private GoldSrcServer g;

    private OnClickListener sayButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * "Say" button was clicked
             */

            message = message_field.getText().toString();

            if( message.length() == 0 )
                UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_empty_rcon_command);
            else
                sendCommand();
        }
    };

    private OnKeyListener enterKeyListener = new OnKeyListener() {
        public boolean onKey( View v, int k, KeyEvent e ) {
            /*
             * "Enter" or "Done" key was pressed
             */

            if( (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (e.getAction() == KeyEvent.ACTION_UP) ) {
                message = message_field.getText().toString();

                if( message.length() == 0 )
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_empty_rcon_command);
                else
                    sendCommand();

                return true;
            }

            return false;
        }
    };

    private OnTouchListener touchListener = new OnTouchListener() {
        public boolean onTouch( View v, MotionEvent m ) {
            /*
             * Screen was touched
             */
            scrollLock = (m.getAction() == MotionEvent.ACTION_UP)?false:true;
            return false;
        }
    };

    // Handler for the Chat Relay client thread
    private Handler chatClientHandler = new Handler() {
        public void handleMessage( Message msg ) {            
            /*
             * Message object "what" codes:
             * -2   = An exception during shutdown (maybe normal)
             * -1   = Failed to connect to the chat relay (probably a SocketException)
             *  1   = A heartbeat was received from the server
             *  3   = Connection failure (includes error message as String object)
             *  4   = Connection successful
             *  5   = Chat message (includes ChatMessage object)
             *  255 = Disconnected (probably due to shutdown)
             *  
             *  Values 1-5 correspond to response types sent from the Chat Relay
             */

            if( p.isShowing() ) p.dismiss();

            switch( msg.what ) {
                case -2:
                    finish();
                    break;
                    
                case -1:
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_chat_connect_failure);
                    getChatRelayDetails(chatRelayIP, chatRelayPort, null);
                    break;
                    
                case 1:
                    break;
                    
                case 3:
                    String errorMsg = (String)ChatViewerActivity.this.getText(R.string.msg_chat_connection_refused) + " " + (String)msg.obj;
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, errorMsg);
                    getChatRelayDetails(chatRelayIP, chatRelayPort, null);
                    break;
                    
                case 4:
                    notificationText = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_notification_text, null);
                    notificationText.setText(R.string.msg_chat_connect_success);
                    notificationText.setTypeface(null, Typeface.BOLD);
                    notificationTextParams.span = 2;
                    notificationText.setLayoutParams(notificationTextParams);

                    notificationDate = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_notification_date, null);
                    notificationDate.setText(sdf.format(System.currentTimeMillis()));
                    notificationDateParams.span = 2;
                    notificationDate.setLayoutParams(notificationDateParams);

                    row = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_notification, null);
                    row.addView(notificationText);
                    row.addView(notificationDate);
                    row.setId(rowNum);
                    chat_table.addView(row);

                    spacerRow = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_spacer, null);
                    spacerRow.setLayoutParams(bottomRowParams);
                    spacerRow.setId(rowNum);
                    chat_table.addView(spacerRow, bottomRowParams);

                    rowNum++;

                    if( !scrollLock ) layout.fullScroll(View.FOCUS_DOWN);
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_chat_connect_success);

                    if( !rconPasswordDialogDismissed ) rconAuthenticate();

                    break;
                    
                case 5:
                    try {
                        ChatMessage chatMsg = (ChatMessage)msg.obj;

                        st = (chatMsg.sayTeamFlag == (byte)0x00)?"":"(Say Team)";

                        msgTimestamp = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_top, null);
                        msgTimestamp.setText(chatMsg.messageTimestamp.substring(13));

                        msgPlayerName = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_top, null);
                        msgPlayerName.setText(chatMsg.playerName);
                        msgPlayerName.setTypeface(null, Typeface.BOLD);

                        msgPlayerTeam = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_top, null);
                        msgPlayerTeam.setText("(" + chatMsg.playerTeam + ")");

                        msgSayTeam = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_top, null);
                        msgSayTeam.setText(st);

                        msgPlayerSays = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_bottom, null);
                        msgPlayerSays.setText(chatMsg.message);

                        topRow = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_top, null);
                        topRow.setId(rowNum);
                        topRow.addView(msgTimestamp);
                        topRow.addView(msgPlayerName);
                        topRow.addView(msgPlayerTeam);
                        topRow.addView(msgSayTeam);

                        bottomRow = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_bottom, null);
                        bottomRow.setId(rowNum);
                        bottomRow.addView(msgPlayerSays);
                        chatTextParams = (TableRow.LayoutParams)msgPlayerSays.getLayoutParams();
                        chatTextParams.span = 4;

                        msgPlayerSays.setLayoutParams(chatTextParams);
                        bottomRow.setLayoutParams(bottomRowParams);

                        chat_table.addView(topRow);
                        chat_table.addView(bottomRow, bottomRowParams);

                        if( !scrollLock ) {
                            while( ((chat_table.getChildCount()) / 2) > maxRows ) {
                                chat_table.removeViews(0, 2);
                                Log.d(TAG, "Removed 2 views from table");
                            }

                            layout.fullScroll(View.FOCUS_DOWN);
                        }
                        else {
                            Log.d(TAG, "Scroll lock is active, not removing any views");
                        }

                        rowNum++;
                    }
                    catch( Exception e ) {
                        Log.w(TAG, "Caught an exception while reading chat message object:", e);
                    }

                    break;
                    
                case 255:
                    Log.d(TAG, "Handler received 255 (server closed connection)");
                    
                    notificationText = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_notification_text, null);
                    notificationText.setText(R.string.msg_chat_disconnected);
                    notificationText.setTypeface(null, Typeface.BOLD);
                    notificationTextParams.span = 2;
                    notificationText.setLayoutParams(notificationTextParams);

                    notificationDate = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_notification_date, null);
                    notificationDate.setText(sdf.format(System.currentTimeMillis()));
                    notificationDateParams.span = 2;
                    notificationDate.setLayoutParams(notificationDateParams);

                    row = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_notification, null);
                    row.addView(notificationText);
                    row.addView(notificationDate);
                    row.setId(rowNum);
                    chat_table.addView(row);

                    spacerRow = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_spacer, null);
                    spacerRow.setLayoutParams(bottomRowParams);
                    spacerRow.setId(rowNum);
                    chat_table.addView(spacerRow, bottomRowParams);

                    rowNum++;
                    
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_chat_disconnected);
                    break;
                    
                default:
                    Log.w(TAG, "Handler received an unexpected value (" + msg.what + ")");
                    break;
            }
        }
    };

    // Handler for the "Sending" pop-up thread
    private Handler popUpHandler = new Handler() {
        public void handleMessage( Message msg ) {
            // sending.setVisibility(View.GONE);
            runFadeOutAnimation(ChatViewerActivity.this, sending);

            switch( msg.what ) {
                case 0:
                    break;
                case 1:
                    // Handle RCONNoAuthException
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_no_rcon_auth);
                    getRCONPassword();
                    break;
                case 2:
                    // Handle any other exception
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_rcon_general_error);
                    break;
            }
        }
    };

    // Handler for the network event receiver thread
    private Handler networkEventHandler = new Handler() {
        @Override
        public void handleMessage( Message msg ) {            
            /*
             * Message object "what" codes:
             * -2  =  Fatal exception in the NetworkEventReceiver thread
             * -1  =  No network connectivity
             *  0  =  Initial event from broadcast receiver (should be ignored)
             *  1  =  Network connection change
             */

            Log.d(TAG, "Received " + msg.what + " from NetworkEventReceiver");

            switch( msg.what ) {
                case -2:
                    Log.e(TAG, "The network event receiver has aborted");
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_general_error);
                    finish();
                    break;
                    
                case -1:
                    notificationText = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_notification_text, null);
                    notificationText.setText(R.string.msg_chat_connection_lost);
                    notificationText.setTypeface(null, Typeface.BOLD);
                    notificationTextParams.span = 2;
                    notificationText.setLayoutParams(notificationTextParams);

                    notificationDate = (TextView)View.inflate(ChatViewerActivity.this, R.layout.chat_textview_notification_date, null);
                    notificationDate.setText(sdf.format(System.currentTimeMillis()));
                    notificationDateParams.span = 2;
                    notificationDate.setLayoutParams(notificationDateParams);

                    row = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_notification, null);
                    row.setId(rowNum);
                    row.addView(notificationText);
                    row.addView(notificationDate);
                    chat_table.addView(row);

                    spacerRow = (TableRow)View.inflate(ChatViewerActivity.this, R.layout.chat_tablerow_spacer, null);
                    spacerRow.setLayoutParams(bottomRowParams);
                    spacerRow.setId(rowNum);
                    chat_table.addView(spacerRow, bottomRowParams);

                    rowNum++;

                    shutdownChatRelayConnection();

                    if( !scrollLock ) layout.fullScroll(View.FOCUS_DOWN);
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_connection_lost);
                    break;
                    
                case 0:
                    break;
                    
                case 1:
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_network_change);

                    try {
                        // Attempt a new connection to the chat relay
                        getChatRelayConnection();
                    }
                    catch( UnknownHostException u ) {
                        String errorMsg = String.format(ChatViewerActivity.this.getString(R.string.msg_unknown_host), chatRelayIP);
                        UserVisibleMessage.showMessage(ChatViewerActivity.this, errorMsg);
                        getChatRelayDetails(chatRelayIP, chatRelayPort, chatRelayPassword);
                    }
                    break;
                    
                default:
                    break;
            }
        }
    };

    // Handler for the EngineQuery thread
    private Handler engineQueryHandler = new Handler() {
        @Override
        public void handleMessage( Message msg ) {
            engine = msg.what;

            if( engine == -1 ) {
                p.dismiss();
                UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_rcon_general_error);
            }
            else {
                try {
                    if( engine == Values.ENGINE_GOLDSRC ) {
                        g = (GoldSrcServer)msg.obj;
                        new Thread(new RconAuth(rconPassword, g, rconAuthHandler)).start();
                    }
                    else {
                        s = (SourceServer)msg.obj;
                        new Thread(new RconAuth(rconPassword, s, rconAuthHandler)).start();
                    }
                }
                catch( Exception e ) {
                    p.dismiss();
                    Log.w(TAG, "checkRCON(): Caught an exception:", e);
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_rcon_general_error);
                }
            }
        }
    };

    private Handler rconAuthHandler = new Handler() {
        @Override
        public void handleMessage( Message msg ) {
            p.dismiss();

            switch( msg.what ) {
                case 0:
                    if( engine == Values.ENGINE_GOLDSRC )
                        g = (GoldSrcServer)msg.obj;
                    else
                        s = (SourceServer)msg.obj;

                    rconIsAuthenticated = true;

                    if( message != null )
                        if( message.length() > 0 )
                            sendCommand();

                    break;
                    
                case 1:
                    // RCONNoAuthException
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_no_rcon_auth);
                    getRCONPassword();
                    break;
                    
                case 2:
                    // RCONBanException
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_rcon_ban_exception);
                    break;
                    
                case 3:
                    // SteamCondenserException
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_rcon_general_error);
                    break;
                    
                case 4:
                    // TimeoutException (happens if RCON password was already sent)
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_rcon_timeout_exception);
                    break;
                    
                case 5:
                    // Any other exception
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_rcon_general_error);
                    break;
                    
                default:
                    break;
            }
        }
    };

    @SuppressLint({ "InlinedApi", "NewApi" })
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if( android.os.Build.VERSION.SDK_INT >= 14 ) {
            if( ViewConfiguration.get(this).hasPermanentMenuKey() )
                requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        
        this.setContentView(R.layout.chatui);

        Intent thisIntent = getIntent();

        try {
            gameServerIP = InetAddress.getByName(thisIntent.getStringExtra(Values.EXTRA_SERVER)).getHostAddress();
            gameServerPort = Integer.toString(thisIntent.getIntExtra(Values.EXTRA_PORT, 27015));
        }
        catch( UnknownHostException e ) {
            String errorMsg = String.format(ChatViewerActivity.this.getString(R.string.msg_unknown_host),
                    thisIntent.getStringExtra(Values.EXTRA_SERVER));

            UserVisibleMessage.showMessage(ChatViewerActivity.this, errorMsg);

            Log.w(TAG, "onCreate(): Unknown host " + thisIntent.getStringExtra(Values.EXTRA_SERVER));

            finish();
        }
        catch( Exception e ) {
            UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_general_error);
            Log.w(TAG, "onCreate(): Caught an exception:", e);
            finish();
        }

        if( database == null )
            database = new DatabaseProvider(ChatViewerActivity.this);
                
        rconIsAuthenticated = false;
        rconPasswordDialogDismissed = false;
        rconServer = thisIntent.getStringExtra(Values.EXTRA_SERVER);
        rconPort = thisIntent.getIntExtra(Values.EXTRA_PORT, 27015);
        rconPassword = thisIntent.getStringExtra(Values.EXTRA_PASSWORD);
        rconTimeout = thisIntent.getIntExtra(Values.EXTRA_TIMEOUT, 1);

        fade_in = AnimationUtils.loadAnimation(ChatViewerActivity.this, R.anim.fade_in);
        fade_out = AnimationUtils.loadAnimation(ChatViewerActivity.this, R.anim.fade_out);
        scrollLock = false;

        setResult(1);

        bottomRowParams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        
        bottomRowParams.setMargins(0, 0, 0, 5);

        notificationTextParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT);
        
        notificationDateParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT);

        chat_table = (TableLayout)findViewById(R.id.chatui_chat_table);

        layout = (ScrollView)findViewById(R.id.chatui_scrollview);
        layout.setOnTouchListener(touchListener);

        subtitle = (TextView)findViewById(R.id.chatui_subtitle);
        
        if( thisIntent.getStringExtra(Values.EXTRA_NICKNAME) != null ) {
            subtitle.setText(thisIntent.getStringExtra(Values.EXTRA_NICKNAME));
        }
        else {
            subtitle.setText(gameServerIP + ":" + gameServerPort);
        }

        sending = (TextView)findViewById(R.id.chatui_sending);
        sending.setVisibility(View.GONE);

        message_field = (EditText)findViewById(R.id.chatui_message_field);
        message_field.setOnKeyListener(enterKeyListener);

        say_button = (Button)findViewById(R.id.chatui_say_button);
        say_button.setOnClickListener(sayButtonListener);

        receiverRunnable = new NetworkEventReceiver(this, networkEventHandler);

        if( receiverRunnable == null ) {
            Log.e(TAG, "onCreate(): NetworkEventReceiver object is null, cannot continue");
            UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_general_error);
            finish();
        }
        else {
            receiverThread = new Thread(receiverRunnable);

            if( receiverThread == null ) {
                Log.e(TAG, "onCreate(): NetworkEventReceiver thread is null, cannot continue");
                UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_general_error);
                finish();
            }

            receiverThread.start();
        }

        sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        rowNum = 0;
        maxRows = 1000;

        Bundle settings = database.getSettingsAsBundle();
        chatRelayIP = settings.getString(Values.SETTING_DEFAULT_RELAY_HOST);
        chatRelayPort = Integer.valueOf(settings.getInt(Values.SETTING_DEFAULT_RELAY_PORT)).toString();
        chatRelayPassword = settings.getString(Values.SETTING_DEFAULT_RELAY_PASSWORD);

        //showNote();
        getChatRelayDetails(chatRelayIP, chatRelayPort, chatRelayPassword);
        
        UserVisibleMessage.showNote(
                ChatViewerActivity.this,
                Values.FILE_HIDE_CHAT_RELAY_NOTE,
                R.string.note_chat_relay_required);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if( database == null )
            database = new DatabaseProvider(ChatViewerActivity.this);
    }

    @Override
    public void onPause() {
        super.onPause();
        
        if( database != null ) {
            database.close();
            database = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdownNetworkEventReceiver();
        shutdownChatRelayConnection();
        if( g != null ) g.disconnect();
        if( s != null ) s.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rcon_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.back:
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

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }

    public void onActivityResult( int request, int result, Intent data ) {
        if( request == Values.ACTIVITY_CHAT_RELAY_DETAILS_DIALOG ) {
            if( result == 0 ) finish();
            if( result == 1 ) {
                chatRelayIP = data.getStringExtra(Values.EXTRA_SERVER);
                chatRelayPort = data.getStringExtra(Values.EXTRA_PORT);
                chatRelayPassword = data.getStringExtra(Values.EXTRA_PASSWORD);

                try {
                    getChatRelayConnection();
                }
                catch( UnknownHostException u ) {
                    String errorMsg = String.format(ChatViewerActivity.this.getString(R.string.msg_unknown_host), chatRelayIP);
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, errorMsg);
                    getChatRelayDetails(chatRelayIP, chatRelayPort, null);
                }
            }
        }
        else if( request == Values.ACTIVITY_RCON_PASSWORD_DIALOG ) {
            if( result == 0 ) {
                rconPasswordDialogDismissed = true;
            }
            if( result == 1 ) {
                rconPasswordDialogDismissed = false;
                rconPassword = data.getStringExtra(Values.EXTRA_PASSWORD);
                rconAuthenticate();
            }
        }
        else if( request == Values.ACTIVITY_SHOW_NOTE ) {
            getChatRelayDetails(chatRelayIP, chatRelayPort, chatRelayPassword);
        }

        return;
    }

    public void getRCONPassword() {
        Intent rconPasswordIntent = new Intent();
        rconPasswordIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.RconPasswordActivity");
        startActivityForResult(rconPasswordIntent, Values.ACTIVITY_RCON_PASSWORD_DIALOG);
    }

    public void rconAuthenticate() {
        if( rconPassword.length() == 0 ) {
            getRCONPassword();
            return;
        }

        p = ProgressDialog.show(this, "", ChatViewerActivity.this.getText(R.string.status_rcon_verifying_password), true, false);

        // Start the authentication process by getting the engine type
        new Thread(new EngineQuery(rconServer, rconPort, rconTimeout, engineQueryHandler)).start();
    }

    public void sendCommand() {
        if( !rconIsAuthenticated ) {
            rconAuthenticate();
            return;
        }

        command = "say " + message;
        message_field.setText("");
        message = new String();
        sending.setText(R.string.status_rcon_sending);
        runFadeInAnimation(ChatViewerActivity.this, sending);

        if( engine == Values.ENGINE_GOLDSRC )
            new Thread(new RconQuery(command, g, popUpHandler)).start();
        else
            new Thread(new RconQuery(command, s, popUpHandler)).start();
    }

    public void getChatRelayDetails( String ip, String port, String pswd ) {
        if( ip == null ) ip = "";
        if( port == null ) port = "";
        if( pswd == null ) pswd = "";

        Intent chatRelayDetailsIntent = new Intent();
        chatRelayDetailsIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.ChatRelayDetailsActivity");
        chatRelayDetailsIntent.putExtra(Values.EXTRA_SERVER, ip);
        chatRelayDetailsIntent.putExtra(Values.EXTRA_PORT, port);
        chatRelayDetailsIntent.putExtra(Values.EXTRA_PASSWORD, pswd);
        startActivityForResult(chatRelayDetailsIntent, Values.ACTIVITY_CHAT_RELAY_DETAILS_DIALOG);
    }

    public void getChatRelayConnection() throws UnknownHostException {
        // Kill the current connection
        shutdownChatRelayConnection();

        // Show the progress dialog
        p = ProgressDialog.show(this, "", ChatViewerActivity.this.getText(R.string.status_connecting), true, false);

        try {
            Log.d(TAG, "getChatRelayConnection(): Getting new instance of class " + Chat.class.getSimpleName());
            chatRunnable = new Chat(chatRelayIP, chatRelayPort, chatRelayPassword, gameServerIP, gameServerPort, chatClientHandler);

            if( chatRunnable != null ) {
                Log.d(TAG, "getChatRelayConnection(): New object is " + chatRunnable.toString());
            }
            else {
                Log.d(TAG, "getChatRelayConnection(): Failed to get an instance of class " + Chat.class.getSimpleName());
                UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_chat_not_started);
                finish();
            }

            Log.d(TAG, "getChatRelayConnection(): Getting new Thread for runnable " + chatRunnable.toString());
            chatThread = new Thread(chatRunnable);

            if( chatThread != null ) {
                Log.d(TAG, "getChatRelayConnection(): Calling start() on " + chatThread.toString());
                chatThread.start();

                if( chatThread.isAlive() ) {
                    Log.d(TAG, "getChatRelayConnection(): Thread " + chatThread.toString() + " is running");
                }
                else {
                    Log.d(TAG, "getChatRelayConnection(): Failed to start thread " + chatThread.toString());
                    UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_chat_not_started);
                    finish();
                }
            }
            else {
                Log.d(TAG, "getChatRelayConnection(): Failed to get Thread for runnable " + chatRunnable.toString());
                UserVisibleMessage.showMessage(ChatViewerActivity.this, R.string.msg_chat_not_started);
                finish();
            }
        }
        catch( UnknownHostException uhe ) {
            String errorMsg = String.format(ChatViewerActivity.this.getString(R.string.msg_unknown_host), chatRelayIP);
            UserVisibleMessage.showMessage(ChatViewerActivity.this, errorMsg);
            getChatRelayDetails(chatRelayIP, chatRelayPort, null);
        }
    }

    public void runFadeInAnimation( Context c, View v ) {
        v.startAnimation(fade_in);
    }

    public void runFadeOutAnimation( Context c, View v ) {
        v.startAnimation(fade_out);
    }

    public void shutdownChatRelayConnection() {
        if( chatRunnable != null ) {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat object is not null, calling shutDown()");
            chatRunnable.shutDown();
        }
        else {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat object is null");
        }

        if( chatThread != null ) {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is not null");

            if( chatThread.isAlive() ) {
                Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is alive, calling interrupt() on thread " + chatThread.getId());
                chatThread.interrupt();
            }
            else {
                Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is not alive, not interrupting");
            }
        }
        else {
            Log.d(TAG, "shutdownChatRelayConnection(): Chat thread is null");
        }        
    }

    public void shutdownNetworkEventReceiver() {
        if( receiverRunnable != null ) {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver object is not null, calling unregisterReceiver()");
            receiverRunnable.unregisterReceiver();
            receiverRunnable.shutDown();
        }
        else {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver object is null");
        }

        if( receiverThread != null ) {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is not null");

            if( receiverThread.isAlive() ) {
                Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is alive, calling interrupt() on thread " + receiverThread.getId());
                receiverThread.interrupt();
            }
            else {
                Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is not alive, not interrupting");
            }
        }
        else {
            Log.d(TAG, "shutdownNetworkEventReceiver(): NetworkEventReceiver thread is null");
        }
    }
    
    public void showNote() {
    	File f = new File(ChatViewerActivity.this.getFilesDir(), Values.FILE_HIDE_CHAT_RELAY_NOTE);
    	
    	if( f.exists() ) {
            getChatRelayDetails(chatRelayIP, chatRelayPort, chatRelayPassword);
            return;
    	}
    	
        Intent showNoteIntent = new Intent();
        showNoteIntent.setClassName("com.dparker.apps.checkvalve", "com.dparker.apps.checkvalve.ShowNoteActivity");
        showNoteIntent.putExtra(Values.EXTRA_NOTE_ID, R.string.note_chat_relay_required);
        showNoteIntent.putExtra(Values.EXTRA_FILE_NAME, Values.FILE_HIDE_CHAT_RELAY_NOTE);
        startActivityForResult(showNoteIntent, Values.ACTIVITY_SHOW_NOTE);
    }
}
