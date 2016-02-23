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

package com.github.daparker.checkvalve.backup;

import java.io.File;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.daparker.checkvalve.R;
import com.github.daparker.checkvalve.Values;

public class FileChooserActivity extends Activity {
    private LinearLayout file_list_layout;
    private TextView current_folder;
    private File storageDir;
    private ArrayList<String> breadcrumbs = new ArrayList<String>();
    
    final OnClickListener dismissButtonListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * Dismiss button was clicked
             */
            v.setBackgroundColor(FileChooserActivity.this.getResources().getColor(R.color.steam_blue));
            finish();
        }
    };
    
    final OnClickListener fileClickListener = new OnClickListener() {
        public void onClick( View v ) {
            /*
             * Filename was clicked
             */
            TextView t = (TextView)v;
            t.setBackgroundResource(R.color.steam_blue);
            String text = t.getText().toString().trim();
            File tag = (File)t.getTag();
            
            if( tag.isDirectory() ) {
                if( text.equals("../") ) {
                    // Remove the last entry from the breadcrumb trail
                    breadcrumbs.remove(breadcrumbs.size()-1);
                }
                else {
                    // Append this entry to the breadcrumb trail
                    breadcrumbs.add(tag.getName());
                }
                
                // Show the files in this folder
                showFiles(tag);
            }
            else {
                Intent i = new Intent();
                i.putExtra(Values.EXTRA_BACKUP_FILE, tag);
                setResult(Activity.RESULT_OK, i);
                finish();
            }
        }
    };

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        this.setResult(0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.file_chooser);
        
        file_list_layout = (LinearLayout)findViewById(R.id.file_list_layout);
        current_folder = (TextView)findViewById(R.id.current_folder); 
        
        this.findViewById(R.id.dismiss_button).setOnClickListener(dismissButtonListener);
        
        storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);        
        breadcrumbs.add(storageDir.getName());
        
        showFiles(storageDir);
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        return;
    }
    
    public void showFiles(File directory) {        
        StringBuilder sb = new StringBuilder(breadcrumbs.get(0));
        
        for( int i = 1; i < breadcrumbs.size(); i++ )
            sb.append(" > ").append(breadcrumbs.get(i));
        
        // Set the "Current Folder" text
        current_folder.setText(sb.toString());
        
        // Get a list of backup files and subfolders
        File[] fileList = directory.listFiles(new BackupFileFilter());
        
        // Clear the current list if one is showing
        file_list_layout.removeAllViews();
        
        // If this is not the Download directory then allow upward navigation
        if( ! directory.equals(storageDir) ) {
            TextView t = new TextView(FileChooserActivity.this);
            t.setTag(directory.getParentFile());
            t.setText("../");
            t.setTextSize(18);
            t.setPadding(10, 15, 10, 15);
            t.setOnClickListener(fileClickListener);
            file_list_layout.addView(t);
        }
        
        // If the file list is not empty then show the files
        if( fileList.length > 0 ) {
            for( File f : fileList ) {
                String filename = f.getName();
                
                if( f.isDirectory() )
                    filename = filename.concat("/");
                
                TextView t = new TextView(FileChooserActivity.this);
                t.setTag(f);
                t.setText(filename);
                t.setTextSize(18);
                t.setPadding(10, 15, 10, 15);
                t.setOnClickListener(fileClickListener);
                file_list_layout.addView(t);
            }
        }
        // If the file list is empty then show a message
        else {
            TextView t = new TextView(FileChooserActivity.this);
            t.setText(R.string.msg_backup_no_files);
            t.setTextSize(18);
            t.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, 100, 0, 0);
            file_list_layout.addView(t);
        }
    }
}