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
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class LabelPreference extends Preference implements OnActivityResultListener {
	static final String TAG = "LABEL PREF";
	
	String label = null;

	public LabelPreference(Context context) { super(context); }
	
	public LabelPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public LabelPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);		
		this.setSummary(this.label != null ? this.label : "(none)");
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		Log.d(TAG,"SetInitialValue: " + restorePersistedValue +" " + defaultValue);
		super.onSetInitialValue(restorePersistedValue, defaultValue);
		this.setLabel(restorePersistedValue ? getPersistedString(this.label) : null );
	}
	
	@Override
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);
		// this method is probably package protected, and the method isn't even
		// defined in the stubs that we're compiling against.  WTS.
//		preferenceManager.registerOnActivityResultListener(this);
	}
	
	protected void onClick() {
		// start label picker...
		Log.d(TAG, "Starting label picker...");
		Intent intent = new Intent(Intent.ACTION_PICK).setType(Label.CONTENT_ITEM_TYPE);
		// another method that isn't exposed...  WTS Android.
//		getPreferenceManager().getActivity().startActivityForResult(intent, 1);
		((Activity)getContext()).startActivityForResult(intent, 1);		
	}
	
	
	public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "ACTIVITY RESULT: " + resultCode);
    	if ( resultCode != Activity.RESULT_OK ) return false;
    	String label = data.getStringExtra("label");
		Log.i(TAG, "GOT LABEL RESULT: " + label);
    	
    	this.setLabel(label);
		return true;
	}
	
	String getLabel() { return this.label; }
	
	void setLabel( String val ) {
		this.label = val;
		
		this.persistString(val);
		this.setSummary(label != null ? label : "(none)" );
		this.callChangeListener(val);
		notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
        Log.d("GMARK LABEL PREF","Sent pref update!");
	}

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
         
        Log.d(TAG,"Restoring instance state: " + state);
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setLabel(myState.label);
    }
    
    private static class SavedState extends BaseSavedState {
        String label;
        
        public SavedState(Parcel source) {
            super(source);
            label = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(label);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            
			public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}