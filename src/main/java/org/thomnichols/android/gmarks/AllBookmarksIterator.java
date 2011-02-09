/* This file is part of GMarks. Copyright 2011 Thom Nichols
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thomnichols.android.gmarks.BookmarksQueryService.AuthException;

import android.util.Log;

class AllBookmarksIterator extends ThreadIterator<Bookmark> {
	
	private static final String TAG = "BOOKMARKS ITERATOR";
	private static final String THREAD_PARAM = "Starred";
	
	public AllBookmarksIterator(BookmarksQueryService svc) throws AuthException, IOException {
		super(svc, THREAD_PARAM );
	}
	
	public Bookmark next() throws IteratorException {
		try {
			JSONObject item = this.currentSection.getJSONObject(currentItemIndex++);
//				Log.v(TAG,item.toString());
			Bookmark bookmark = new Bookmark( 
					item.getString("elementId"),
					item.getString("threadId"),
					item.getString("title"), 
					item.getString("url"),
					item.getString("host"),
					item.getString("description"),
					item.getLong("timestamp"),
					item.getLong("modifiedTimestamp") );

			if ( item.has("labels") ) {
				JSONArray labelJSON = item.getJSONArray("labels");

				for ( int i=0; i< labelJSON.length(); i++ )
					bookmark.getLabels().add(labelJSON.getString(i));
			}
			if ( item.has("faviconUrl") )
				bookmark.setFaviconURL(item.getString("faviconUrl"));
			
			return bookmark;
		}
		catch ( JSONException ex ) {
			Log.w(TAG, "Error parsing bookmark from JSON", ex);
			throw new IteratorException( "Error parsing bookmark from JSON", ex);
		}
	}
}