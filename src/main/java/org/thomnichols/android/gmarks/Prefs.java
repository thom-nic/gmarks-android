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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class Prefs {

	static final String KEY_SYNC_INTERVAL = "background_sync_interval";
	static final String KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled";
	static final String KEY_BROWSER_SYNC_ENABLED = "browser_sync_enabled";
	static final String KEY_BROWSER_SYNC_LABEL = "browser_sync_label";
	static final String KEY_SYNC_NOTIFICATION = "sync_notifications";

	static final String KEY_LABELS_SORT_PREF = "labels_sort_by";
	static final String KEY_BOOKMARKS_SORT_PREF = "bookmarks_sort_by";
	
	static final String PREF_LAST_SYNC = "last_sync";
	static final String PREF_LAST_SYNC_ATTEMPT = "last_sync_attempt";
	static final String PREF_LAST_BROWSER_SYNC = "last_browser_sync";

	static final String PREF_HIDDEN_LABEL_IDS = "hidden_label_ids";

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
