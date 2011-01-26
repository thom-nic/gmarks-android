package org.thomnichols.android.gmarks;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class Prefs {

	static final String KEY_SYNC_INTERVAL = "background_sync_interval";
	static final String KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled";
	static final String KEY_BROWSER_SYNC_ENABLED = "browser_sync_enabled";
	static final String KEY_BROWSER_SYNC_LABEL = "browser_sync_label";

	static final String KEY_LABELS_SORT_PREF = "labels_sort_by";
	static final String KEY_BOOKMARKS_SORT_PREF = "bookmarks_sort_by";
	
	static final String PREF_LAST_SYNC = "last_sync";
	static final String PREF_LAST_SYNC_ATTEMPT = "last_sync_attempt";
	static final String PREF_LAST_BROWSER_SYNC = "last_browser_sync";

	static final String DEFAULT_SYNC_INTERVAL = "60"; // 1 hour in minutes
	static final boolean DEFAULT_SYNC_ENABLED = false;
	static final boolean DEFAULT_BROWSER_SYNC_ENABLED = false;
	static final String DEFAULT_BROWSER_SYNC_LABEL = null;
	
	public static SharedPreferences get(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx);
	}
	
	public static SharedPreferences.Editor edit(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).edit();
	}
	
	private Prefs() {}
}
