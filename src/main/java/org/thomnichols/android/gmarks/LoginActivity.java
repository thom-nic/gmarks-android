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

import org.thomnichols.android.gmarks.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class is deprecated; replaced with WebViewLoginActivity.
 */
@Deprecated
public class LoginActivity extends Activity {
	static final String TAG = "BOOKMARKS LOGIN";
	
	boolean loggedIn = false;
	ProgressDialog waitDialog = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i("TAG", "Created!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
		Button loginButton = (Button)findViewById(R.id.loginBtn);
		loginButton.setOnClickListener( this.loginClicked );
		
        // we already know the user needs to enter login info:
//		if ( false ) {
//	        this.waitDialog = ProgressDialog.show(this, "", 
//	                "Logging in. Please wait...", true);
//	        
//			new CheckLoginTask().execute();
//		}
    }
    
    class CheckLoginTask extends AsyncTask<Void, Void, Boolean> {
    	@Override protected Boolean doInBackground(Void... args) {
    		BookmarksQueryService gmarksSvc = BookmarksQueryService.getInstance();
    		GmarksProvider.DatabaseHelper db = new GmarksProvider.DatabaseHelper(LoginActivity.this);
    		gmarksSvc.setAuthCookies( db.restoreCookies() );
    		db.close();
    		return gmarksSvc.testAuth();
    	}
    	@Override protected void onPostExecute( Boolean alreadyLoggedIn ) {
    		if ( alreadyLoggedIn ) {
    			Log.d(TAG, "Already logged in");
    			Toast.makeText(LoginActivity.this, "Already logged in.", Toast.LENGTH_SHORT);
    			((Button)findViewById(R.id.loginBtn)).setText("Already logged in");
    			LoginActivity.this.loggedIn = true;
    			LoginActivity.this.finishActivity(Activity.RESULT_OK);
    		}
    		else Log.d(TAG,"Not logged in yet!");
    		
    		LoginActivity.this.waitDialog.dismiss();
    	}
    }
    
    class DoLoginTask extends AsyncTask<Void, Void, Boolean> {
    	String errorMsg = null;
    	@Override protected Boolean doInBackground(Void... args) {
			TextView username = (TextView)findViewById(R.id.user);
			TextView passwd = (TextView)findViewById(R.id.passwd);
    		try {
	    		BookmarksQueryService gmarksSvc = BookmarksQueryService.getInstance();
				gmarksSvc.login( username.getText().toString(),	passwd.getText().toString() );
				return true;
    		}
			catch ( Exception ex ) {
				Log.e(TAG, "Login error", ex);
				this.errorMsg = ex.getMessage();
				return false;
			}
    	}
    	@Override protected void onPostExecute( Boolean loginSuccess ) {
    		LoginActivity.this.waitDialog.dismiss();
    		if ( loginSuccess ) {
				Log.d(TAG,"Logged in!");
				GmarksProvider.DatabaseHelper db = new GmarksProvider.DatabaseHelper(LoginActivity.this);
				db.persistCookies( BookmarksQueryService.getInstance().cookieStore.getCookies() );
				db.close();
				Log.d(TAG,"Persisted cookies to DB!!");
				Toast.makeText(LoginActivity.this, "Logged in!", Toast.LENGTH_LONG);
				
//				startActivity(new Intent(Intent.ACTION_VIEW).setType(Bookmark.CONTENT_TYPE));
    			LoginActivity.this.finishActivity(Activity.RESULT_OK);
    		}
    		else {
    			Log.d(TAG,"Login failed!");
				((TextView)findViewById(R.id.errorMsg)).setText("Error:\n" + this.errorMsg );
    		}    		
    	}
    }

    private View.OnClickListener loginClicked = new View.OnClickListener() {
		public void onClick(View v) {
			if ( ! LoginActivity.this.loggedIn ) {
				Log.d(TAG, "Sending auth requests...");
		        LoginActivity.this.waitDialog = ProgressDialog.show(
		        		LoginActivity.this, "", 
		                "Logging in. Please wait...", true );
				new DoLoginTask().execute();
			}
			else startActivity(new Intent(Intent.ACTION_VIEW).setType(Bookmark.CONTENT_TYPE));
		}
	};

    protected void onDestroy() {
    	Log.i(TAG, "Destroyed!");
    	super.onDestroy();
    }
}
