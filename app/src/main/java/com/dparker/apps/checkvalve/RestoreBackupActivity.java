/*
 * Copyright 2010-2024 by David A. Parker <parker.david.a@gmail.com>
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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.dparker.apps.checkvalve.backup.BackupParser;
import com.dparker.apps.checkvalve.exceptions.InvalidBackupFileException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class RestoreBackupActivity extends Activity {
    private static final String TAG = RestoreBackupActivity.class.getSimpleName();

    private EditText field_backup_file;
    private TableLayout file_details_table;
    private ProgressDialog p;
    private Uri chosenUri;

    private DatabaseProvider database;

    private final OnClickListener restoreButtonListener = new OnClickListener() {
        @SuppressLint({"InlinedApi", "NewApi"})
        public void onClick(View v) {
            /*
             * "Restore" button was clicked
             */

            if(field_backup_file.getText().toString().isEmpty()) {
                UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_empty_fields);
            }
            else {
                AlertDialog.Builder alertDialogBuilder;

                alertDialogBuilder = new AlertDialog.Builder(RestoreBackupActivity.this, AlertDialog.THEME_HOLO_DARK);
                alertDialogBuilder.setTitle(R.string.title_confirm_restore);
                alertDialogBuilder.setMessage(R.string.msg_restore_backup);
                alertDialogBuilder.setCancelable(false);

                alertDialogBuilder.setPositiveButton(R.string.button_restore, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        /*
                         *  "Restore" button was clicked
                         */
                        restoreBackupFile();
                    }
                });

                alertDialogBuilder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        /*
                         * "Cancel" button was clicked
                         */
                        dialog.cancel();
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
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
             * "Cancel" button was clicked
             */

            runFileChooser();
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler backupParserHandler = new Handler(Looper.myLooper()) {
        public void handleMessage(@NonNull Message msg) {
            if( p.isShowing() )
                p.dismiss();

            switch( msg.what ) {
                case 0:
                    // Data was restored successfully
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_restored);
                    finish();
                    break;
                case 1:
                    // Error during file parsing
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                    break;
                case 2:
                    // Invalid backup file
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_invalid);
                    break;
                case 3:
                    // Some other error
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                    break;
                default:
                    // This should never happen
                    Log.e(TAG, "backupWriterHandler: handleMessage(): Invalid Message what value " + msg.what);
                    UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
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

        this.setContentView(R.layout.restore_backup);
        this.setResult(1);

        if( database == null )
            database = new DatabaseProvider(RestoreBackupActivity.this);

        // Set button listeners
        this.findViewById(R.id.restorebackup_cancel_button).setOnClickListener(cancelButtonListener);
        this.findViewById(R.id.restorebackup_restore_button).setOnClickListener(restoreButtonListener);
        this.findViewById(R.id.restorebackup_chooser_button).setOnClickListener(chooserButtonListener);

        field_backup_file = findViewById(R.id.restorebackup_field_backup_file);
        field_backup_file.setEnabled(false);

        TextView help_text = findViewById(R.id.restorebackup_help_text);
        help_text.setText(this.getString(R.string.help_text_restore_backup));

        file_details_table = findViewById(R.id.restorebackup_file_details_table);
        file_details_table.setColumnShrinkable(0, false);
        file_details_table.setColumnShrinkable(1, true);

        runFileChooser();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( database == null )
            database = new DatabaseProvider(RestoreBackupActivity.this);
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onActivityResult(int request, int result, Intent data) {
        switch( request ) {
            case Values.ACTIVITY_FILE_CHOOSER:
                if( result == Activity.RESULT_OK ) {
                    File selectedFile = (File) data.getSerializableExtra(Values.EXTRA_BACKUP_FILE);
                    String filename = selectedFile.getName();
                    field_backup_file.setText(filename);

                    try {
                        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(selectedFile, ParcelFileDescriptor.MODE_READ_ONLY);
                        if( ! showFileDetails(filename, pfd) ) {
                            this.findViewById(R.id.restorebackup_file_details_layout).setVisibility(View.GONE);
                            UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                        }
                    }
                    catch( InvalidBackupFileException ibfe ) {
                        UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_invalid);
                    }
                    catch( Exception e ) {
                        Log.e(TAG, "onActivityResult(): Caught an exception:", e);
                        UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                    }
                }
                break;
            case Values.ACTIVITY_OPEN_DOCUMENT:
                if( result == Activity.RESULT_OK ) {
                    chosenUri = data.getData();
                    Log.d(TAG, "onActivityResult(): Chosen URI is " + chosenUri.toString());
                    String[] segments = chosenUri.getLastPathSegment().split(":");
                    String f = segments[segments.length-1];
                    field_backup_file.setText(f);

                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(chosenUri, "r");
                        if( ! showFileDetails(f, pfd) ) {
                            this.findViewById(R.id.restorebackup_file_details_layout).setVisibility(View.GONE);
                            UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                        }
                    }
                    catch( InvalidBackupFileException ibfe ) {
                        UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_backup_file_invalid);
                    }
                    catch( Exception e ) {
                        Log.e(TAG, "onActivityResult(): Caught an exception:", e);
                        UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                    }
                }
                break;
            default:
                break;
        }
    }

    public void restoreBackupFile() {
        try {
            // Show the progress dialog
            p = ProgressDialog.show(RestoreBackupActivity.this, "", getText(R.string.status_restoring_backup), true, false);

            BackupParser b = new BackupParser(RestoreBackupActivity.this, chosenUri, backupParserHandler);
            Thread t = new Thread(b);
            t.start();
        }
        catch( Exception e ) {
            Log.e(TAG, "restoreBackupFile(): Caught an exception:", e);
            UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
        }
    }

    public void runFileChooser() {
        String filename = "";
        Uri uri;

        if( chosenUri == null ) {
            uri = Uri.fromFile(new File(Environment.DIRECTORY_DOWNLOADS));
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
            }
            catch( Exception e ) {
                Log.e(TAG, "runFileChooser(): Caught an exception:", e);
                UserVisibleMessage.showMessage(RestoreBackupActivity.this, R.string.msg_general_error);
                return;
            }
        }

        Intent createDocument = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        createDocument.addCategory(Intent.CATEGORY_OPENABLE);
        createDocument.setType("text/plain");
        createDocument.putExtra(Intent.EXTRA_TITLE, filename);

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
            createDocument.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);

        startActivityForResult(createDocument, Values.ACTIVITY_OPEN_DOCUMENT);
    }
    
    public boolean showFileDetails(String fileName, ParcelFileDescriptor pfd) throws InvalidBackupFileException {
        String appVersion = "";
        String fileDate = "";
        String line;

        boolean valid = false;

        try {
            file_details_table.removeAllViews();

            Log.d(TAG, "showFileDetails(): Reading file " + fileName);
            BufferedReader r = new BufferedReader(new FileReader(pfd.getFileDescriptor()));

            int lineNum = 0;

            while( (line = r.readLine()) != null ) {
                lineNum++;

                if( line.startsWith("## Version: ") ) {
                    Log.d(TAG, "showFileDetails(): Found version header on line " + lineNum);
                    appVersion = line.replaceFirst("## Version: ", "").trim();
                    Log.i(TAG, "showFileDetails(): Backup file is from app version " + appVersion);
                    continue;
                }

                if( line.startsWith("## Created: ") ) {
                    Log.d(TAG, "showFileDetails(): Found date header on line " + lineNum);
                    fileDate = line.replaceFirst("## Created: ", "").trim();
                    Log.i(TAG, "showFileDetails(): Backup file was created " + fileDate);
                    continue;
                }

                if( (!appVersion.isEmpty()) && (!fileDate.isEmpty()) ) {
                    valid = true;
                    break;
                }
            }

            r.close();

            if( !valid ) {
                Log.e(TAG, "showFileDetails(): Backup file is not valid!");
                throw new InvalidBackupFileException();
            }

            long sizeInKB = pfd.getStatSize() / 1024;

            TextView versionLabel = new TextView(RestoreBackupActivity.this);
            versionLabel.setText(R.string.label_backup_app_version);
            versionLabel.setTextColor(Color.WHITE);
            versionLabel.setTextSize(14);
            versionLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            versionLabel.setPadding(0, 0, 25, 0);

            TextView versionText = new TextView(RestoreBackupActivity.this);
            versionText.setText(appVersion);
            versionText.setTextColor(Color.WHITE);
            versionText.setTextSize(14);

            TextView dateLabel = new TextView(RestoreBackupActivity.this);
            dateLabel.setText(R.string.label_backup_date);
            dateLabel.setTextColor(Color.WHITE);
            dateLabel.setTextSize(14);
            dateLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            dateLabel.setPadding(0, 0, 25, 0);

            TextView dateText = new TextView(RestoreBackupActivity.this);
            dateText.setText(fileDate);
            dateText.setTextColor(Color.WHITE);
            dateText.setTextSize(14);

            TextView sizeLabel = new TextView(RestoreBackupActivity.this);
            sizeLabel.setText(R.string.label_backup_size);
            sizeLabel.setTextColor(Color.WHITE);
            sizeLabel.setTextSize(14);
            sizeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            sizeLabel.setPadding(0, 0, 25, 0);

            TextView sizeText = new TextView(RestoreBackupActivity.this);
            sizeText.setText(Long.valueOf(sizeInKB).toString() + " KB");
            sizeText.setTextColor(Color.WHITE);
            sizeText.setTextSize(14);

            TableRow versionRow = new TableRow(RestoreBackupActivity.this);
            //versionRow.addView(versionLabel, new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 0.2f));
            versionRow.addView(versionLabel);
            versionRow.addView(versionText, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f));

            TableRow dateRow = new TableRow(RestoreBackupActivity.this);
            //dateRow.addView(dateLabel, new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 0.2f));
            dateRow.addView(dateLabel);
            dateRow.addView(dateText, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f));

            TableRow sizeRow = new TableRow(RestoreBackupActivity.this);
            sizeRow.addView(sizeLabel);
            //sizeRow.addView(sizeLabel, new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 0.2f));
            sizeRow.addView(sizeText, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f));

            file_details_table.addView(versionRow);
            file_details_table.addView(dateRow);
            file_details_table.addView(sizeRow);

            this.findViewById(R.id.restorebackup_file_details_layout).setVisibility(View.VISIBLE);

            return true;
        }
        catch( InvalidBackupFileException ibfe ) {
            Log.d(TAG, "showFileDetails(): Caught an exception:", ibfe);
            throw ibfe;
        }
        catch( Exception e ) {
            Log.d(TAG, "showFileDetails(): Caught an exception:", e);
            return false;
        }
    }
}