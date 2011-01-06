package org.thomnichols.android.gmarks;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;

public class LabelPreference extends Preference {
	public LabelPreference(Context context) { super(context); }
	
	public LabelPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public LabelPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	String value = null;
	String getValue() { return this.value; }
	void setValue( String val ) {
		this.value = val;
		this.callChangeListener(val);
		Log.d("GMARK LABEL PREF","Sent pref update!");
	}
}