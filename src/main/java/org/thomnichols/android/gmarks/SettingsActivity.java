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
	
	static final String KEY_BROWSER_SYNC_ENABLED = "browser_sync_enabled";
	static final String KEY_BROWSER_SYNC_LABEL = "browser_sync_label";
	static final String KEY_SYNC_INTERVAL = "background_sync_interval";
	static final String KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    protected void onResume() {
        super.onResume();        
        Preference labelPref = findPreference(KEY_BROWSER_SYNC_LABEL);
        labelPref.setEnabled( ((CheckBoxPreference) 
        		findPreference(KEY_BROWSER_SYNC_ENABLED)).isChecked() );
        
        CheckBoxPreference backgroundEnabledPref = 
        	(CheckBoxPreference) findPreference(KEY_BACKGROUND_SYNC_ENABLED);

        
        ListPreference intervalPref = (ListPreference)findPreference(KEY_SYNC_INTERVAL);
        int currentSetting = intervalPref.findIndexOfValue( intervalPref.getValue() ); 
        intervalPref.setSummary( currentSetting < 0 ? "(None)" : 
        	intervalPref.getEntries()[currentSetting] );
        intervalPref.setEnabled( backgroundEnabledPref.isChecked() );
        
        labelPref.setOnPreferenceChangeListener(this);
        backgroundEnabledPref.setOnPreferenceChangeListener(this);
        intervalPref.setOnPreferenceChangeListener(this);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref ) {
    	String key = pref.getKey();
    	if ( KEY_BROWSER_SYNC_ENABLED.equals(key) )
    		findPreference(KEY_BROWSER_SYNC_LABEL).setEnabled(
    				((CheckBoxPreference)pref).isChecked() );

    	if ( KEY_BACKGROUND_SYNC_ENABLED.equals(key) )
    		findPreference(KEY_SYNC_INTERVAL).setEnabled(
    				((CheckBoxPreference)pref).isChecked() );
    	
    	if ( KEY_BROWSER_SYNC_LABEL.equals(key) ) {
    		// label picker listens for its own click...
    	}
    	
		return super.onPreferenceTreeClick(prefScreen, pref);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "ACTIVITY RESULT: " + resultCode);
    	((LabelPreference)findPreference(KEY_BROWSER_SYNC_LABEL)).onActivityResult(
    			requestCode, resultCode, data);
    }

	public boolean onPreferenceChange(Preference pref, Object newVal) {
		
		String key = pref.getKey();
		Log.d(TAG, "GOT PREF CHANGE: " + key + "=" + newVal);
		if ( KEY_BACKGROUND_SYNC_ENABLED.equals(key) ||
				KEY_SYNC_INTERVAL.equals(key)) {
			// start the Background sync service if it's not already started:
			Intent action = new Intent(this, BackgroundService.class);
			action.setAction( Intent.ACTION_CONFIGURATION_CHANGED );
			action.putExtra("key", key);
			startService(action);
			// notify the service that settings have changed.  
			// If there is nothing to do, it will shut back down.
		}
		if ( KEY_SYNC_INTERVAL.equals(key) ) {  // change the 'summary' field
			ListPreference intervalPref = (ListPreference)pref;
	        int currentSetting = intervalPref.findIndexOfValue( (String)newVal );
	        intervalPref.setSummary( intervalPref.getEntries()[currentSetting] );
		}
		else if ( KEY_BROWSER_SYNC_LABEL.equals(key) ) {
			// label picker handles itself.
		}
		return true;
	}
}
