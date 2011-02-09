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

import java.io.IOException;

import org.thomnichols.android.gmarks.GmarksProvider.DatabaseHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Synchronize bookmark lists
 */
public class ListsSync {
	static final String TAG = "BOOKMARK LIST SYNC";
	long lastSync = 0;
	long lastSyncAttempt = 0;
	SharedPreferences prefs;
	
	public ListsSync(Context ctx) {
		this.prefs = Prefs.get(ctx);
		this.lastSync = prefs.getLong(Prefs.KEY_LAST_LIST_SYNC, 0);
		this.lastSyncAttempt = prefs.getLong(Prefs.KEY_LAST_LIST_SYNC_ATTEMPT, 0);
	}
	
	public void synchronizeMyLists( DatabaseHelper db ) {
		BookmarksQueryService svc = BookmarksQueryService.getInstance();
		
		Long thisSync = System.currentTimeMillis();
		try {
			Iterable<BookmarkList> iter = svc.getMyBookmarks();
			for ( BookmarkList bl : iter ) {
				
			}
			
			prefs.edit().putLong(Prefs.KEY_LAST_LIST_SYNC, thisSync).commit();
		}
		catch ( IOException ex ) {
			Log.w(TAG,"Error syncing", ex);
		}
		prefs.edit().putLong(Prefs.KEY_LAST_LIST_SYNC_ATTEMPT, thisSync).commit();
	}
}
