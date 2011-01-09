package org.thomnichols.android.gmarks;

import java.util.List;

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
	
	static final String loginURL = "https://www.google.com/accounts/ServiceLogin";
	static final String targetURL = "https://www.google.com/bookmarks/";
	WebView webView = null;
	boolean loggedIn = false;
	CookieSyncManager cookieSyncManager;
	ProgressDialog waitDialog;
//	CookieManager cookieManager;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_login);

        this.cookieSyncManager = CookieSyncManager.createInstance(this);
//        this.cookieManager = CookieManager.getInstance();
        
        this.webView = (WebView)findViewById(R.id.loginWebView);
        this.webView.setWebViewClient(this.webClient);
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.loadUrl(targetURL);
    }
    
    protected void onResume() {
    	super.onResume();
    	cookieSyncManager.startSync();
    };
    
    protected void onPause() {
    	super.onPause();
    	cookieSyncManager.stopSync();
    };
    
    WebViewClient webClient = new WebViewClient() {
    	public void onPageFinished(WebView view, String url) {
    		Log.d(TAG, "PAGE LOADED ======= " + url );
    		cookieSyncManager.sync();
    		// dammit, there's no way to determine that this load is actually a redirect!
//    		if ( url.contains("ServiceLoginAuth") ) {
//    			
//    		}
    		
    		if ( url.startsWith(loginURL) ) {
    			if ( WebViewLoginActivity.this.waitDialog != null ) {
    				waitDialog.dismiss(); // let the user log in.
    				waitDialog = null;
    			}
    		}
    		
    		if ( url.startsWith(targetURL) ) {
    			// This prints out all of the cookies as one long string.  Big pain in my ass.
//    			Log.d(TAG,"Cookie: " + cookieManager.getCookie("https://www.google.com") );
    			cookieSyncManager.stopSync(); // let it close the DB.
    			try { Thread.sleep(1000); } catch ( InterruptedException ex ) {}
    			WebViewCookiesDB cookieDB = new WebViewCookiesDB(WebViewLoginActivity.this);
    			try {
    				List<Cookie> cookies = cookieDB.getCookies();
    				Log.d(TAG,"GOT cookies: " + cookies.size() );
    				boolean loggedIn = false; 
    				for ( Cookie c : cookies ) {
    					Log.d(TAG,"Cookie: " + c.getName() + "=" + c.getValue() );
    					if ( "SID".equals( c.getName() ) ) { 
    						loggedIn = true;
    						break;
    					}
    				}
    				if ( ! loggedIn ) return;

    				new GmarksProvider.DatabaseHelper(WebViewLoginActivity.this)
    					.persistCookies( cookies );
    				BookmarksQueryService.getInstance().setAuthCookies( cookies );
    				
	    			Log.d(TAG,"Restored " + cookies.size() + " cookies");
	    			Log.d(TAG,"Logged in!");
	    			
	    			Toast.makeText(WebViewLoginActivity.this, "Logged in!", Toast.LENGTH_LONG).show();
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
    				WebViewLoginActivity.this, "", "please wait...", true );
    	};
    	
    	public void onReceivedError(WebView view, int errorCode, 
    			String description, String failingUrl) {
    		Toast.makeText(WebViewLoginActivity.this, "Login failed: " + description, 
    				Toast.LENGTH_SHORT).show();
    		
    		view.loadUrl(targetURL);
    	}
    };

    void getCookies() {
    	String dbLocation = this.webView.getSettings().getDatabasePath();
    	Log.d(TAG, "DB path: " + dbLocation );
    }
}
