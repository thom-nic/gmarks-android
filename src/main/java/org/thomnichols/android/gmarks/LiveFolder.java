package org.thomnichols.android.gmarks;

import org.thomnichols.android.gmarks.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

public class LiveFolder extends Activity {

    public static final Uri CONTENT_URI = 
    	Uri.parse("content://" + Bookmark.AUTHORITY + "/live_folders/bookmarks" );

    public static final Uri BOOKMARK_URI = Uri.parse( Bookmark.CONTENT_URI + "/#" );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        // TODO present dialog from which a user can choose the folder title
        
        if ( LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action) ) {
            // Build the live folder intent.
            final Intent liveFolderIntent = new Intent();

            liveFolderIntent.setData(CONTENT_URI);
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME,"GMarks Recent Items");
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_folder));
            liveFolderIntent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
                    LiveFolders.DISPLAY_MODE_LIST);
            liveFolderIntent.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT,
                    new Intent(Intent.ACTION_VIEW, BOOKMARK_URI) );

            // The result of this activity should be a live folder intent.
            setResult(RESULT_OK, liveFolderIntent);
        } 
        else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }
}
