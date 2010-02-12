package net.totalmadownage.nethak;

import net.totalmadownage.nethak.ServerListDialog.ConnectReady;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Community extends Activity {
	/** Called when the activity is first created. */
	
	protected static final int MENU_CONNECT = 1;
	protected static final int MENU_REFRESH = 2;
		
	WebView webview;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	
	requestWindowFeature(Window.FEATURE_NO_TITLE);
	
	setContentView(R.layout.website);
	
	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	
    webview = (WebView) findViewById(R.id.webview); 
    webview.setWebViewClient(new HelloWebViewClient()); 
    webview.getSettings().setJavaScriptEnabled(true); 
    webview.loadUrl("http://nethak.totalmadownage.net");
    //webview.loadUrl("http://www.google.com");
	
	}
	
	private class HelloWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
	        return true;
	    }
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_CONNECT, 0, "EXIT");
        menu.add(0, MENU_REFRESH, 0, "REFRESH");
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {

		return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_CONNECT:
			Community.this.finish();
        	return true;
        case MENU_REFRESH:
			webview.reload();
        	return true;	
        }
        return false;
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
	        webview.goBack();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
}