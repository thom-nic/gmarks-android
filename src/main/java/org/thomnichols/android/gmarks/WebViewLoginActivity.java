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
import java.util.List;
import java.util.Set;

import org.apache.http.cookie.Cookie;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebViewLoginActivity extends Activity {
	static final String TAG = "GMARKS WEBVIEW LOGIN";
	
	static final String KEY_PAUSED_FOR_TWO_FACTOR_AUTH = "gmarks.webview.paused_for_two_factor_auth";
	static final String KEY_PAUSED_AT_URL = "gmarks.webview.paused_at_current_url";
	
	static final String loginURL = "https://www.google.com/accounts/ServiceLogin";
	static final String twoFactorAuthURL = "https://www.google.com/accounts/SmsAuth";
//	static final String checkCookieURL = "https://www.google.com/accounts/CheckCookie";
	static final String loginParams = "?service=bookmarks&passive=true"
		+ "&continue=https://www.google.com/bookmarks/l"
		+ "&followup=https://www.google.com/bookmarks/l";
	static final String targetURL = "https://www.google.com/bookmarks/";
	WebView webView = null;
	boolean loggedIn = false;
	boolean resumingTwoFactorAuth = false;
	private String resumeAtURL = loginURL + loginParams; 
	CookieSyncManager cookieSyncManager;
	ProgressDialog waitDialog;
//	CookieManager cookieManager;
	static final Set<String> requiredCookies = new HashSet<String>();
	static {
		requiredCookies.add("SID");
		requiredCookies.add("LSID");
		requiredCookies.add("HSID");
		requiredCookies.add("SSID");
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_login);

        this.cookieSyncManager = CookieSyncManager.createInstance(this);
//        this.cookieManager = CookieManager.getInstance();
        
        this.webView = (WebView)findViewById(R.id.loginWebView);
        this.webView.setWebViewClient(this.webClient);
        this.webView.getSettings().setJavaScriptEnabled(true);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	WebViewCookiesDB cookieDB = new WebViewCookiesDB(WebViewLoginActivity.this);
		try {
			if ( ! resumingTwoFactorAuth ) cookieDB.deleteCookie("SID");
		} finally { cookieDB.close(); }
		final String currentURL = webView.getUrl();
		if ( resumingTwoFactorAuth && currentURL != null && 
				currentURL.startsWith(twoFactorAuthURL) ) return;
		this.webView.loadUrl( this.resumeAtURL );
    }
    
    protected void onPause() {
    	super.onPause();
    	if ( this.waitDialog != null ) this.waitDialog.dismiss();
    };
    
    protected void onSaveInstanceState(Bundle outState) {
    	final String currentURL = webView.getUrl(); 
    	if ( currentURL.startsWith(twoFactorAuthURL) ) {
    		outState.putBoolean(KEY_PAUSED_FOR_TWO_FACTOR_AUTH, true);
    		outState.putString(KEY_PAUSED_AT_URL, currentURL );
    	}
    	super.onSaveInstanceState(outState);
    };
    
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	this.resumingTwoFactorAuth = savedInstanceState.getBoolean(
    			KEY_PAUSED_FOR_TWO_FACTOR_AUTH, false );
    	if ( this.resumingTwoFactorAuth && savedInstanceState.containsKey(KEY_PAUSED_AT_URL) )
    		this.resumeAtURL = savedInstanceState.getString( KEY_PAUSED_AT_URL );
    };
    
    WebViewClient webClient = new WebViewClient() {
    	public void onPageFinished(WebView view, String url) {
    		Log.d(TAG, "PAGE LOADED ======= " + url );
    		cookieSyncManager.sync();
    		
    		if ( url.startsWith(loginURL) || url.startsWith(twoFactorAuthURL) ) {
    			if ( WebViewLoginActivity.this.waitDialog != null ) {
    				try {
    					waitDialog.dismiss(); // let the user log in.
    				} catch ( IllegalArgumentException ex ) {}
    				waitDialog = null;
    			}
    		}
    		
    		if ( url.startsWith(targetURL) ) {
    			// This prints out all of the cookies as one long string.  Big pain in my ass.
//    			Log.d(TAG,"Cookie: " + cookieManager.getCookie("https://www.google.com") );
    			WebViewCookiesDB cookieDB = new WebViewCookiesDB(WebViewLoginActivity.this);
    			try {
    				List<Cookie> cookies = null; 
    				boolean loggedIn = false; 
    				long timeout = System.currentTimeMillis() + 10000;
    				while ( ! loggedIn ) { 
        				cookies = cookieDB.getCookies();
    					try { Thread.sleep(1500); } catch ( InterruptedException ex ) {}
	    				Log.d(TAG,"GOT cookies: " + cookies.size() );
	    				Set<String> cookieNames = new HashSet<String>();
	    				for ( Cookie c : cookies ) cookieNames.add(c.getName());
	    				
	    				if ( cookieNames.containsAll(requiredCookies) ) loggedIn = true;
	    				
	    				if ( System.currentTimeMillis() > timeout ) {
	    					Log.w(TAG,"Timeout looking for SID cookie!");
	    					break;
	    				}
    				}

    				int loginMsg = R.string.login_failed_msg;
    				if ( loggedIn ) {
	    				new GmarksProvider.DatabaseHelper(WebViewLoginActivity.this)
	    					.persistCookies( cookies );
	    				BookmarksQueryService.getInstance().setAuthCookies( cookies );	    				
		    			Log.d(TAG,"Logged in!");
		    			loginMsg = R.string.login_success_msg; 
    				}
	    			
	    			Toast.makeText(WebViewLoginActivity.this, loginMsg, Toast.LENGTH_LONG).show();
	    			WebViewLoginActivity.this.finishActivity(RESULT_OK);
	    			WebViewLoginActivity.this.finish();
    			}
    			finally { cookieDB.close(); }
    		}
    		else if ( ! url.startsWith("https://www.google.com/") ) {
    			Log.w(TAG, "Somehow we got redirected to a different domain! " + url);
    			view.loadUrl(targetURL);
    		}
    	}
    	
    	public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
    		if ( WebViewLoginActivity.this.waitDialog != null ) return; 
    		WebViewLoginActivity.this.waitDialog = ProgressDialog.show(
    				WebViewLoginActivity.this, "", 
    				getText(R.string.please_wait_msg), true );
    	};
    	
    	public void onReceivedError(WebView view, int errorCode, 
    			String description, String failingUrl) {
    		Toast.makeText( WebViewLoginActivity.this, 
    				getString(R.string.login_failed_detail_msg, description), 
    				Toast.LENGTH_SHORT ).show();
    		
    		view.loadUrl(targetURL);
    	}
    };

    void getCookies() {
    	String dbLocation = this.webView.getSettings().getDatabasePath();
    	Log.d(TAG, "DB path: " + dbLocation );
    }
}
