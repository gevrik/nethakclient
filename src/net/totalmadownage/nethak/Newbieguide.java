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

public class Newbieguide extends Activity {
		
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
        webview.getSettings().setBuiltInZoomControls(true);

        final Activity activity = this;
        webview.setWebChromeClient(new WebChromeClient() {
        	@Override
          public void onProgressChanged(WebView view, int progress) {

            activity.setProgress(progress * 100);
          }
        });

        webview.setWebViewClient(new WebViewClient() {
        	@Override
          public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
          }
        });

        webview.loadUrl("http://totalmadownage.net/nethak/?page_id=510");
        
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
			Newbieguide.this.finish();
        	return true;
        	
        case MENU_REFRESH:
			webview.reload();
        	return true;
        	
        }
        return false;
    }
    
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//	    if (keyCode == KeyEvent.KEYCODE_BACK) {
//	        
//     		new AlertDialog.Builder(this)
//    		.setMessage("Are you sure you want to exit?")
//    		.setCancelable(false)
//    		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//    		           public void onClick(DialogInterface dialog, int id) {
//    		        	   Newbieguide.this.finish();
//    		           }
//    		       })
//    		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
//    		           public void onClick(DialogInterface dialog, int id) {
//    		                dialog.cancel();
//    		           }
//    		       }).show();
//	    	
//	        return true;
//	    }
//	    return super.onKeyDown(keyCode, event);
//	}
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            webview.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
        
}