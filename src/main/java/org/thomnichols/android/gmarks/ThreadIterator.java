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
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thomnichols.android.gmarks.BookmarksQueryService.AuthException;

import android.util.Log;

public abstract class ThreadIterator<T> implements Iterator<T>, Iterable<T> {
	static final String TAG = "BOOKMARK THREAD ITERATOR";
	protected final BookmarksQueryService bookmarksQueryService;
	private static final String uriTemplate = "https://www.google.com/bookmarks/api/threadsearch?fo=%s&g=Time&nr=25&start=";
	private final String uriBase;
	
	private JSONObject currentBatch = null;
	private int currentQueryIndex = 0;	
	private int totalItems = -1; 
	private int sectionIndex = 0;
	protected int currentItemIndex = 0;
	protected JSONArray currentSection = null;
	
	public ThreadIterator(BookmarksQueryService bookmarksQueryService, String threadParam ) 
			throws AuthException, IOException { 
		this.bookmarksQueryService = bookmarksQueryService;
		this.bookmarksQueryService.getXtParam(); // ensures we're logged in & have the default thread ID
		this.uriBase = String.format(uriTemplate, threadParam);
	}

	private boolean getNextSection() {
		if ( currentBatch == null ) return false;
		try {
			JSONArray allSections = currentBatch.getJSONArray( "threadTitles" );
			if ( this.sectionIndex >= allSections.length() )
				return false;

			this.currentSection = allSections.getJSONObject( sectionIndex++ )
				.getJSONArray("sectionContent");
			this.currentItemIndex = 0;
			this.currentQueryIndex += currentSection.length();
			return true;
		}
		catch ( JSONException ex ) {
			Log.w(TAG, "JSON iterator error", ex );
		}
		return false;
	}
	
	private boolean queryNext() throws IteratorException {
		try {
			this.currentBatch = this.bookmarksQueryService.queryJSON(
					uriBase + currentQueryIndex );
			this.currentItemIndex = 0;
			this.sectionIndex = 0;

			if ( this.totalItems < 0 ) this.totalItems = currentBatch.getInt("nr");
			
			JSONArray sectionList = currentBatch.getJSONArray("threadTitles");
			if ( sectionList.length() < 1 ) { 
				Log.w(TAG, "JSON response has 0 items!");
				return false;
			}

			this.currentSection = sectionList.getJSONObject(sectionIndex++).getJSONArray("sectionContent");
			this.currentQueryIndex += currentSection.length();
			return this.currentSection.length() > 0;
		}
		catch ( IOException ex ) {
			Log.w(TAG,"IO error in query all bookmarks", ex );
			throw new IteratorException(ex);
//				return false;
		}
		catch ( JSONException ex ) {
			Log.w(TAG,"JSON error in query all bookmarks", ex ); 
			throw new IteratorException(ex);
//				return false;
		}
	}
	
	public boolean hasNext() throws IteratorException {
		return this.currentSection != null && this.currentItemIndex < this.currentSection.length()
			|| getNextSection() || queryNext();
	}

	public void remove() { throw new UnsupportedOperationException(); }

	public Iterator<T> iterator() { return this; }
}
