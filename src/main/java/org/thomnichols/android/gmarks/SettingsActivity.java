package org.thomnichols.android.gmarks;

import java.awt.Desktop.Action;

import org.thomnichols.android.gmarks.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
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
        
        ListPreference intervalPref = (ListPreference)findPreference(KEY_SYNC_INTERVAL);
        int currentSetting = intervalPref.findIndexOfValue( intervalPref.getValue() ); 
        intervalPref.setSummary( currentSetting < 0 ? "(None)" : 
        	intervalPref.getEntries()[currentSetting] );
        intervalPref.setEnabled( ((CheckBoxPreference) 
        		findPreference(KEY_BACKGROUND_SYNC_ENABLED)).isChecked() );
        
        labelPref.setOnPreferenceChangeListener(this);
        intervalPref.setOnPreferenceChangeListener(this);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref ) {
    	if ( KEY_BROWSER_SYNC_ENABLED.equals(pref.getKey()) )
    		findPreference(KEY_BROWSER_SYNC_LABEL).setEnabled(
    				((CheckBoxPreference)pref).isChecked() );

    	if ( KEY_BACKGROUND_SYNC_ENABLED.equals(pref.getKey()) )
    		findPreference(KEY_SYNC_INTERVAL).setEnabled(
    				((CheckBoxPreference)pref).isChecked() );
    	
    	if ( KEY_BROWSER_SYNC_LABEL.equals(pref.getKey()) ) {
    		// start label picker...
    		Intent intent = new Intent(Intent.ACTION_PICK).setType(Label.CONTENT_TYPE);
    		startActivityForResult(intent, RESULT_OK);
    	}
    	
    	super.onPreferenceTreeClick(prefScreen, pref);
		return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "ACTIVITY RESULT: " + resultCode);
//    	if ( resultCode != RESULT_OK ) return;
    	String label = data.getStringExtra("label");
		Log.i(TAG, "GOT LABEL RESULT: " + label);
    	
    	LabelPreference pref = (LabelPreference)findPreference(KEY_BROWSER_SYNC_LABEL);
    	pref.setValue(label);
    	pref.setSummary( label );
    	super.onActivityResult(requestCode, resultCode, data);
    }

	public boolean onPreferenceChange(Preference pref, Object newVal) {
		
		Log.d(TAG, "GOT PREF CHANGE: " + pref.getKey() + "=" + newVal);
		if ( KEY_SYNC_INTERVAL.equals(pref.getKey()) ) {
			ListPreference intervalPref = (ListPreference)pref;
	        int currentSetting = intervalPref.findIndexOfValue( intervalPref.getValue() );
	        intervalPref.setSummary( intervalPref.getEntries()[currentSetting] );
		}
		else if ( KEY_BROWSER_SYNC_LABEL.equals(pref.getKey()) ) {
			pref.setSummary( ((LabelPreference)pref).getValue());
		}
		return true;
	}
}
