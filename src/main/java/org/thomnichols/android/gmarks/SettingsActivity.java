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

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	static final String TAG = "GMARKS SETTINGS";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    protected void onResume() {
        super.onResume();        
        Preference labelPref = findPreference(Prefs.KEY_BROWSER_SYNC_LABEL);
        labelPref.setEnabled( ((CheckBoxPreference) 
        		findPreference(Prefs.KEY_BROWSER_SYNC_ENABLED)).isChecked() );
        
        CheckBoxPreference backgroundEnabledPref = 
        	(CheckBoxPreference) findPreference(Prefs.KEY_BACKGROUND_SYNC_ENABLED);

        ListPreference intervalPref = (ListPreference)findPreference(Prefs.KEY_SYNC_INTERVAL);
        int currentSetting = intervalPref.findIndexOfValue( intervalPref.getValue() ); 
        intervalPref.setSummary( currentSetting < 0 ? getText(R.string.pref_not_set_label) : 
        	intervalPref.getEntries()[currentSetting] );
        intervalPref.setEnabled( backgroundEnabledPref.isChecked() );
        
        labelPref.setOnPreferenceChangeListener(this);
        backgroundEnabledPref.setOnPreferenceChangeListener(this);
        intervalPref.setOnPreferenceChangeListener(this);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref ) {
    	String key = pref.getKey();
    	if ( Prefs.KEY_BROWSER_SYNC_ENABLED.equals(key) )
    		findPreference(Prefs.KEY_BROWSER_SYNC_LABEL).setEnabled(
    				((CheckBoxPreference)pref).isChecked() );

    	if ( Prefs.KEY_BACKGROUND_SYNC_ENABLED.equals(key) )
    		findPreference(Prefs.KEY_SYNC_INTERVAL).setEnabled(
    				((CheckBoxPreference)pref).isChecked() );
    	
    	if ( Prefs.KEY_BROWSER_SYNC_LABEL.equals(key) ) {
    		// label picker listens for its own click...
    	}
    	
		return super.onPreferenceTreeClick(prefScreen, pref);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "ACTIVITY RESULT: " + resultCode);
    	((LabelPreference)findPreference(Prefs.KEY_BROWSER_SYNC_LABEL)).onActivityResult(
    			requestCode, resultCode, data);
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
	        intervalPref.setSummary( intervalPref.getEntries()[currentSetting] );
		}
		else if ( Prefs.KEY_BROWSER_SYNC_LABEL.equals(key) ) {
			// label picker handles itself.
		}
		return true;
	}
}
