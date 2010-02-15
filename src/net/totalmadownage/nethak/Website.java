package net.totalmadownage.nethak;



import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class Website extends Activity {
		
	protected static final int MENU_EXIT = 1;
	protected static final int MENU_REFRESH = 2;
	
	WebView webview;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	//getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags
    	(WindowManager.LayoutParams.FLAG_FULLSCREEN,
    	WindowManager.LayoutParams.FLAG_FULLSCREEN );

        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        webview = new WebView(this);
        setContentView(webview);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        webview.getSettings().setJavaScriptEnabled(true);

        final Activity activity = this;
        webview.setWebChromeClient(new WebChromeClient() {
          public void onProgressChanged(WebView view, int progress) {

            activity.setProgress(progress * 100);
          }
        });

        webview.setWebViewClient(new WebViewClient() {
          public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
          }
        });

        webview.loadUrl("http://kults.genesismuds.com/nethak/helps/index.html");
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_EXIT, 0, "EXIT");
        menu.add(0, MENU_REFRESH, 0, "REFRESH");
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {

		return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        case MENU_EXIT:
			Website.this.finish();
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

//import android.app.Activity;
//import android.content.pm.ActivityInfo;
//import android.os.Bundle;
//import android.view.KeyEvent;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.Window;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//
//public class Website extends Activity {
//	/** Called when the activity is first created. */
//	
//	WebView webview;
//	
//	protected static final int MENU_CONNECT = 1;
//	protected static final int MENU_HELP = 2;
//	
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//	super.onCreate(savedInstanceState);
//	setContentView(R.layout.website);
//	
//	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//	getWindow().requestFeature(Window.FEATURE_PROGRESS);
//	
//    webview = (WebView) findViewById(R.id.webview); 
//    webview.setWebViewClient(new HelloWebViewClient()); 
//    webview.getSettings().setJavaScriptEnabled(true); 
//    webview.loadUrl("http://kults.genesismuds.com/nethak/helps/index.html");
//    //webview.loadUrl("http://www.google.com");
//	
//	}
//	
//	private class HelloWebViewClient extends WebViewClient {
//	    @Override
//	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
//	        view.loadUrl(url);
//	        return true;
//	    }
//	}
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(0, MENU_CONNECT, 0, "EXIT");
//        menu.add(0, MENU_HELP, 0, "REFRESH");
//        return true;
//    }
//    
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu)
//    {
//
//		return true;
//    }
//
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//        case MENU_CONNECT:
//			Website.this.finish();
//        	return true;
//
//        case MENU_HELP:
//			webview.reload();
//        	return true;
//        	
//        }
//        return false;
//    }
//	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//	    if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
//	        webview.goBack();
//	        return true;
//	    }
//	    return super.onKeyDown(keyCode, event);
//	}
//	
//}