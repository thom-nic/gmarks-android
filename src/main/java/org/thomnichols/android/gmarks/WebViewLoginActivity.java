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
	static final String checkCookieURL = "https://www.google.com/accounts/CheckCookie";
	static final String loginParams = "?service=bookmarks"
		+ "&continue=https://www.google.com/bookmarks/l"
		+ "&followup=https://www.google.com/bookmarks/l";
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
//        this.webView.loadUrl(targetURL);
      this.webView.loadUrl(checkCookieURL + loginParams );
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	WebViewCookiesDB cookieDB = new WebViewCookiesDB(WebViewLoginActivity.this);
		try {
			cookieDB.deleteCookie("SID");
		} finally { cookieDB.close(); }
    }
    
    protected void onPause() {
    	super.onPause();
    	this.waitDialog.dismiss();
    };
    
    WebViewClient webClient = new WebViewClient() {
    	public void onPageFinished(WebView view, String url) {
    		Log.d(TAG, "PAGE LOADED ======= " + url );
    		cookieSyncManager.sync();
    		
    		if ( url.startsWith(loginURL) ) {
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
    					try { Thread.sleep(1000); } catch ( InterruptedException ex ) {}
	    				Log.d(TAG,"GOT cookies: " + cookies.size() );
	    				for ( Cookie c : cookies ) {
	//    					Log.d(TAG,"Cookie: " + c.getName() + "=" + c.getValue() );
	    					if ( "SID".equals( c.getName() ) ) { 
	    						loggedIn = true;
	    						break;
	    					}
	    				}
	    				if ( System.currentTimeMillis() > timeout ) {
	    					Log.w(TAG,"Timeout looking for SID cookie!");
	    					break;
	    				}
    				}

    				String loginMsg = "Login error!";
    				if ( loggedIn ) {
	    				new GmarksProvider.DatabaseHelper(WebViewLoginActivity.this)
	    					.persistCookies( cookies );
	    				BookmarksQueryService.getInstance().setAuthCookies( cookies );	    				
		    			Log.d(TAG,"Logged in!");
		    			loginMsg = "Logged in!"; 
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
