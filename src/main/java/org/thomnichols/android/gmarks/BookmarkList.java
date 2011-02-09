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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Model class for bookmark lists
 * @author tnichols
 *
 */
public class BookmarkList {
	static final String TABLE_NAME = "bookmark_list";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + Bookmark.AUTHORITY + "/bookmark_lists" );
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.bookmark_list";
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.bookmark_list";

	public static final String LISTS_PRIVATE = "Mine";
	public static final String LISTS_SHARED = "Shared";
	public static final String LISTS_PUBLIC = "Pub";
	
	private String threadId;
	private String title;
	private String description;
	private Long createdDate;
	private Long modifiedDate;
	private Long _id = null;
	private Boolean ownedByUser;
	private Boolean shared;
	private Boolean published;
	
	public BookmarkList( String title, String description ) {
		this.title = title;
		this.description = description;
	}
	
	public BookmarkList( String threadId, String title, String description, 
			Long created, Long modified,
			Boolean ownedByUser, Boolean shared, Boolean published ) {
		this.threadId = threadId;
		this.title = title;
		this.description = description;
		this.createdDate = created;
		this.modifiedDate = modified;
		this.ownedByUser = ownedByUser;
		this.shared = shared;
		this.published = published;
	}
	
	public Long get_id() { return _id; }
	public void set_id(Long id) { _id = id; }

	public String getThreadId() { return threadId; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public Long getCreatedDate() { return createdDate; }
	public Long getModifiedDate() { return modifiedDate; }
	public Boolean isOwnedByUser() { return ownedByUser; }
	public Boolean isShared() { return shared; }
	public Boolean isPublished() { return published; }

	static final class Columns implements BaseColumns {
		public static final String SORT_MODIFIED = "modified DESC";
		public static final String SORT_TITLE = "title ASC";
		public static final String DEFAULT_SORT_ORDER = SORT_MODIFIED;
		
		public static final String THREAD_ID = "thread_id";
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String CREATED_DATE = "created";
		public static final String MODIFIED_DATE = "modified";
		public static final String OWNED = "owned";
		public static final String SHARED = "shared";
		public static final String PUBLISHED = "published";
	}
}
