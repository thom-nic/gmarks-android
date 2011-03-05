/* This file is part of GMarks. Copyright 2010, 2011 Thom Nichols
 *
 * GMarks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GMarks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GMarks.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thomnichols.android.gmarks;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements 
		Preference.OnPreferenceChangeListener,
		Preference.OnPreferenceClickListener {
	
	static final String TAG = "GMARKS SETTINGS";
	
	static final String MAILTO_URI = "mailto:tmnichols@gmail.com";
	static final String MAILTO_SUBJECT = "GMarks for Android feedback";
	
	static final String KEY_FULL_SYNC_ACTION = "dummy_full_sync_action";
	static final String KEY_LOGOUT_ACTION = "dummy_logout_action";
	static final String KEY_SEND_FEEDBACK_ACTION = "dummy_send_feedback_action";
	
	static final int START_EMAIL_ACTIVITY = 0x2;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    protected void onResume() {
        super.onResume();        
        Preference labelPref = findPreference(Prefs.KEY_BROWSER_SYNC_LABEL);
        labelPref.setOnPreferenceChangeListener(this);
        
        CheckBoxPreference backgroundEnabledPref = 
        	(CheckBoxPreference) findPreference(Prefs.KEY_BACKGROUND_SYNC_ENABLED);
        backgroundEnabledPref.setOnPreferenceChangeListener(this);

        ListPreference intervalPref = (ListPreference)findPreference(Prefs.KEY_SYNC_INTERVAL);
        int currentSetting = intervalPref.findIndexOfValue( intervalPref.getValue() ); 
        intervalPref.setSummary( currentSetting < 0 ? getText(R.string.pref_not_set_label) : 
        	intervalPref.getEntries()[currentSetting] );
        intervalPref.setOnPreferenceChangeListener(this);
        
        findPreference(KEY_FULL_SYNC_ACTION).setOnPreferenceClickListener(this);
        findPreference(KEY_LOGOUT_ACTION).setOnPreferenceClickListener(this);
        findPreference(KEY_SEND_FEEDBACK_ACTION).setOnPreferenceClickListener(this);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "ACTIVITY RESULT: " + requestCode + "/" + resultCode);
    	if ( requestCode == LabelPreference.START_LABEL_CHOOSER_ACTIVITY ) {
    		Preference p = findPreference(Prefs.KEY_BROWSER_SYNC_LABEL);
    		if ( p instanceof LabelPreference ) 
    			((LabelPreference)p).onActivityResult(requestCode, resultCode, data);
    	}
    	else if ( requestCode == START_EMAIL_ACTIVITY && resultCode == Activity.RESULT_OK ) {
    		Toast.makeText(this, R.string.feedback_thanks_msg, Toast.LENGTH_SHORT);
    	}
    }

	public boolean onPreferenceChange(Preference pref, Object newVal) {
		
		String key = pref.getKey();
		Log.d(TAG, "GOT PREF CHANGE: " + key + "=" + newVal);
		if ( Prefs.KEY_BACKGROUND_SYNC_ENABLED.equals(key) ||
				Prefs.KEY_SYNC_INTERVAL.equals(key)) {
			// start the Background sync service if it's not already started:
			Intent action = new Intent(this, BackgroundService.class);
			action.setAction( Intent.ACTION_CONFIGURATION_CHANGED );
			action.putExtra("key", key);
			startService(action);
			// notify the service that settings have changed.  
			// If there is nothing to do, it will shut back down.
		}
		if ( Prefs.KEY_SYNC_INTERVAL.equals(key) ) {  // change the 'summary' field
			ListPreference intervalPref = (ListPreference)pref;
	        int currentSetting = intervalPref.findIndexOfValue( (String)newVal );
	        Log.d(TAG,"New interval idx: " + currentSetting);
	        intervalPref.setSummary( intervalPref.getEntries()[currentSetting] );
		}

		return true;
	}

	public boolean onPreferenceClick(Preference pref) {
		if ( KEY_FULL_SYNC_ACTION.equals( pref.getKey() ) ) {
    		Log.d(TAG,"Performing full sync...");
    		new RemoteSyncTask(this, true).execute();
    	}
		else if ( KEY_LOGOUT_ACTION.equals( pref.getKey() ) ) {
			new GmarksProvider.DatabaseHelper(this).clearCookies();
			BookmarksQueryService.getInstance().clearAuthCookies();
			Toast.makeText(this, R.string.logged_out_msg, Toast.LENGTH_LONG).show();
		}
		else if ( KEY_SEND_FEEDBACK_ACTION.equals( pref.getKey() ) ) {
			try {
				startActivityForResult(
						new Intent(Intent.ACTION_SENDTO,Uri.parse(MAILTO_URI))
						.putExtra(Intent.EXTRA_SUBJECT, MAILTO_SUBJECT), 
						START_EMAIL_ACTIVITY );
			}
			catch ( ActivityNotFoundException ex ) {
				Toast.makeText( this, 
						R.string.error_cant_send_email_msg, 
						Toast.LENGTH_LONG).show();
			}
		}
    	return false;
	}
}
