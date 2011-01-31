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

import java.util.HashSet;
import java.util.Set;

import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class Bookmark {

	public static final String AUTHORITY = "org.thomnichols.gmarks";

	public static final Uri CONTENT_URI
 		= Uri.parse("content://" + AUTHORITY + "/bookmarks" );

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.bookmark";
	
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.bookmark";

	private String googleId;
	private String threadId;
	private String title;
	private String url;
	private String host;
	private String faviconURL;
	private String description;
	private long createdDate;
	private long modifiedDate;
	private Long _id = null;
	
	private Set<String> labels;
	
	public Bookmark() {}
	
	public Bookmark( String googleId, String threadId, String title, String url, String host, 
			String description, long created, long modified ) {
		this.googleId = googleId;
		this.threadId = threadId;
		this.title = title;
		this.url = url;
		this.host = host;
		this.description = description;
		this.createdDate = created;
		this.modifiedDate = modified;
		this.labels = new HashSet<String>();
	}
	
	public void parseLabels( String labelString ) {
		String[] labels = labelString.split(",");
		for ( String label : labels ) {
			label = label.trim();
			if ( label.length() < 1 ) continue; 
			this.getLabels().add(label.trim());
		}
	}
	
	public String getAllLabels() {
		return TextUtils.join( ", ", this.getLabels() );
	}
	
	public String getGoogleId() { return this.googleId; }
	
	public Long get_id() {
		return _id;
	}
	
	public void set_id(Long id) {
		_id = id;
	}

	public String getThreadId() {
		return threadId;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}

	public String getHost() {
		return host;
	}

	public String getFaviconURL() {
		return faviconURL;
	}

	public void setFaviconURL(String url) {
		this.faviconURL = url;
	}

	public String getDescription() {
		return description;
	}

	public long getCreatedDate() {
		return createdDate;
	}

	public long getModifiedDate() {
		return modifiedDate;
	}
	
	public void setLabels(Set<String> l) { this.labels = l; }

	public Set<String> getLabels() {
		return labels;
	}
	
	@Override
	public int hashCode() {
		return this.url != null ? this.url.hashCode() : super.hashCode();
	}

	public static final class Columns implements BaseColumns {
		public static final String SORT_MODIFIED = "modified DESC";
		public static final String SORT_TITLE = "title ASC";
		public static final String DEFAULT_SORT_ORDER = SORT_MODIFIED;
		
		public static final String GOOGLEID = "google_id";
		public static final String THREAD_ID = "thread_id";
		public static final String TITLE = "title";
		public static final String URL = "url";
		public static final String HOST = "host";
		public static final String FAVICON = "favicon_url";
		public static final String DESCRIPTION = "description";
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";
		public static final String LABELS = "labels";
	}
}
