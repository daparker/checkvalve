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

package com.dparker.apps.checkvalve;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.dparker.apps.checkvalve.backup.BackupWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateBackupActivity extends Activity {
    private static final String TAG = CreateBackupActivity.class.getSimpleName();

    private boolean includeServers;
    private boolean includeSettings;
    private EditText field_save_as;
    private Uri chosenUri;

    private DatabaseProvider database;

    private final OnClickListener saveButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Save" button was clicked
             */

            if( field_save_as.getText().toString().isEmpty() ) {
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_empty_fields);
            }
            else {
                saveBackupFile();
            }
        }
    };

    private final OnClickListener cancelButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "Cancel" button was clicked
             */

            setResult(1);
            finish();
        }
    };

    private final OnClickListener chooserButtonListener = new OnClickListener() {
        public void onClick(View v) {
            /*
             * "..." button was clicked
             */

            runFileChooser();
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler backupWriterHandler = new Handler() {
        public void handleMessage(Message msg) {
        switch( msg.what ) {
            case 0:
                // File was written successfully
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_backup_file_created);
                finish();
                break;
            case 1:
                // Database error
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_db_failure);
                break;
            case 2:
                // Exception
                Exception e = (Exception) msg.obj;
                Log.d(TAG, "Caught an exception:", e);
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_general_error);
                break;
            default:
                // Something else went wrong
                Log.e(TAG, "backupWriterHandler: handleMessage(): Invalid Message what value " + msg.what);
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_general_error);
                break;
        }
        }
    };

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( ViewConfiguration.get(this).hasPermanentMenuKey() )
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.setContentView(R.layout.create_backup);
        this.setResult(1);

        if( database == null )
            database = new DatabaseProvider(CreateBackupActivity.this);

        this.findViewById(R.id.createbackup_cancel_button).setOnClickListener(cancelButtonListener);
        this.findViewById(R.id.createbackup_save_button).setOnClickListener(saveButtonListener);
        this.findViewById(R.id.createbackup_chooser_button).setOnClickListener(chooserButtonListener);

        field_save_as = findViewById(R.id.createbackup_field_save_as);
        field_save_as.setEnabled(false);

        TextView help_text = findViewById(R.id.createbackup_help_text);
        help_text.setText(String.format(this.getString(R.string.help_text_create_backup), Environment.DIRECTORY_DOWNLOADS));

        includeServers = true;
        includeSettings = true;

        runFileChooser();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(CreateBackupActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if( database != null ) {
            database.close();
            database = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void checkboxHandler(View view) {
        boolean checkState = ((CheckBox) view).isChecked();

        Log.i(TAG, "checkboxHandler(): View name=" + view + "; id=" + view.getId() + "; checked=" + checkState);

        if( view.getId() == R.id.createbackup_checkbox_include_servers )
            includeServers = checkState;
        else
            includeSettings = checkState;
    }

    public void saveBackupFile() {
        try {
            Log.d(TAG, "saveBackupFile(): Backup file URI is " + chosenUri.toString());
            BackupWriter b = new BackupWriter(CreateBackupActivity.this, chosenUri, includeServers, includeSettings, backupWriterHandler);
            Thread t = new Thread(b);
            t.start();
        }
        catch (Exception e) {
            Log.i(TAG, "saveBackupFile(): Deleting file " + chosenUri.toString());
            deleteBackupFile();
            Log.e(TAG, "saveBackupFile(): Caught an exception:", e);
            UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_general_error);
        }
    }

    public void runFileChooser() {
        String filename;
        Uri uri;

        if( chosenUri == null ) {
            // Default backup file name
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            String timestamp = sdf.format(new Date());

            filename = "checkvalve_" + timestamp;
            uri = Uri.fromFile(new File(Environment.DIRECTORY_DOWNLOADS, filename));
        }
        else {
            try {
                String s = chosenUri.getLastPathSegment();

                if( s.contains("/") ) {
                    filename = s.substring(s.lastIndexOf('/'));
                }
                else {
                    filename = s.split(":")[1];
                }

                uri = chosenUri;

                Log.i(TAG, "Deleting file " + chosenUri.toString());
                deleteBackupFile();
            }
            catch( Exception e ) {
                Log.e(TAG, "runFileChooser(): Caught an exception:", e);
                UserVisibleMessage.showMessage(CreateBackupActivity.this, R.string.msg_general_error);
                return;
            }
        }

        Intent createDocument = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        createDocument.addCategory(Intent.CATEGORY_OPENABLE);
        createDocument.setType("text/plain");
        createDocument.putExtra(Intent.EXTRA_TITLE, filename);

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
            createDocument.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);

        startActivityForResult(createDocument, Values.ACTIVITY_CREATE_DOCUMENT);
    }

    public void deleteBackupFile() {
        try {
            DocumentsContract.deleteDocument(getContentResolver(), chosenUri);
            Log.d(TAG, "deleteBackupFile(): Deleted URI " + chosenUri.toString());
        }
        catch( FileNotFoundException e ) {
            Log.e(TAG, "deleteBackupFile(): Caught an exception:", e);
            Log.d(TAG, "deleteBackupFile(): URI " + chosenUri.toString() + " does not exist");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == Values.ACTIVITY_CREATE_DOCUMENT ) {
            if( resultCode == Activity.RESULT_OK ) {
                chosenUri = data.getData();
                Log.d(TAG, "Chosen URI is " + chosenUri.toString());
                String f = chosenUri.getLastPathSegment().split(":")[1];
                field_save_as.setText(f);
            }
            else {
                UserVisibleMessage.showMessage(CreateBackupActivity.this, "Canceled.");
            }
        }
    }
}