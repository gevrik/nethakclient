package net.totalmadownage.nethak;

import net.totalmadownage.nethak.ServerListDialog.ConnectReady;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Community extends Activity {
	/** Called when the activity is first created. */
	
	protected static final int MENU_CONNECT = 1;
	
	WebView webview;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.website);
	
    webview = (WebView) findViewById(R.id.webview); 
    webview.setWebViewClient(new HelloWebViewClient()); 
    webview.getSettings().setJavaScriptEnabled(true); 
    webview.loadUrl("http://kults.genesismuds.com/nethak/index.php");
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