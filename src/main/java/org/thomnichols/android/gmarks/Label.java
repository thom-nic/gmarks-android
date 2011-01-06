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
		public static final String DEFAULT_SORT_ORDER = "label ASC";

		public static final String TITLE = "label";	
		public static final String COUNT = "_count";
	}

}
