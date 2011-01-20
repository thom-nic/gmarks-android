package org.thomnichols.android.gmarks;

import android.content.Intent;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

public class LiveFolder extends LabelsListActivity {
	static final String TAG = "GMARKS LIVE FOLDER PICKER";

    public static final Uri CONTENT_URI = 
    	Uri.parse("content://" + Bookmark.AUTHORITY + "/live_folders/bookmarks" );

    public static final Uri BOOKMARK_URI = Uri.parse( Bookmark.CONTENT_URI + "/#" );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final Intent intent = getIntent();
        final String action = intent.getAction();

    	Log.d(TAG,"CREATE" + intent);
    	
        if ( ! LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action) ) {
            setResult(RESULT_CANCELED);
            finish();
            Log.d(TAG,"Cancelled");
        }
        super.onCreate(savedInstanceState);
        setTitle(R.string.live_folder_title);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.d(TAG,"RESUME: " + getIntent() );
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
//    	super.onListItemClick(l, v, position, id);
    	// user has selected a label
    	String labelText = ((CursorWrapper)l.getItemAtPosition(position))
    		.getString(COLUMN_INDEX_TITLE);

    	Uri target = CONTENT_URI.buildUpon()
    		.appendQueryParameter("label_id", ""+id).build();
    	Log.d(TAG, "Setting result " + target);
        setFolder( target, labelText );
        finish();
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	return false;
    }
    
    @Override
    public void onClick(View v) {
//    	super.onClick(v);
    	if ( v.getId() == R.id.allListItems ) {
    		Log.d(TAG, "User selected all items" );
    		setFolder(CONTENT_URI, "Recent Items");
    	}
		finish();
    }
        
    protected void setFolder(Uri targetURI, String title) {
        final Intent liveFolderIntent = new Intent();
        if ( targetURI == null ) targetURI = BOOKMARK_URI;
        Log.d(TAG, "Setting live folder for URI: " + targetURI);

        liveFolderIntent.setData(targetURI);
        liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME,title);
        liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON,
                Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_folder));
        liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
                LiveFolders.DISPLAY_MODE_LIST);
        liveFolderIntent.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT,
                new Intent(Intent.ACTION_VIEW, Bookmark.CONTENT_URI) );

		setResult(RESULT_OK, liveFolderIntent);
    }
}
