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

public class Label {
	public static final String AUTHORITY = "org.thomnichols.gmarks";

	public static final Uri CONTENT_URI
 		= Uri.parse("content://" + AUTHORITY + "/labels" );

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.bookmark_label";
	
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.bookmark_label";

	private String title;
	private int count;
	
	public Label(String title, int count) {
		this.title = title;
		this.count = count;
	}
	
	public String getTitle() { return title; }
	public int getCount() { return count; }
	
	public static final class Columns implements BaseColumns {
		public static final String SORT_ALPHA = "label ASC";
		public static final String SORT_COUNT = "_count DESC";
		public static final String DEFAULT_SORT_ORDER = SORT_ALPHA;

		public static final String TITLE = "label";	
		public static final String COUNT = "_count";
	}

}
