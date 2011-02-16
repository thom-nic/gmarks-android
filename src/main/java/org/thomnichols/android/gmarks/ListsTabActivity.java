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

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class ListsTabActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lists_tab_view);

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, ListsListActivity.class)
			.putExtra(BookmarkList.PARAM_CATEGORY, BookmarkList.LISTS_PRIVATE);
		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("private").setIndicator("Private",
				res.getDrawable(R.xml.private_lists_tab)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, ListsListActivity.class)
				.putExtra(BookmarkList.PARAM_CATEGORY, BookmarkList.LISTS_SHARED);
		spec = tabHost.newTabSpec("shared").setIndicator("Shared",
				res.getDrawable(R.xml.private_lists_tab)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, ListsListActivity.class)
				.putExtra(BookmarkList.PARAM_CATEGORY, BookmarkList.LISTS_PUBLIC);
		spec = tabHost.newTabSpec("public").setIndicator("Public",
				res.getDrawable(R.xml.private_lists_tab)).setContent(intent);
		tabHost.addTab(spec);

		// TODO remember which tab was selected last in Prefs
		tabHost.setCurrentTab(0);
	}
}
