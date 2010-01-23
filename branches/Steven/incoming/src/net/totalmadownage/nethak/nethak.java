package net.totalmadownage.nethak;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.View.OnClickListener;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class nethak extends Activity implements OnClickListener {
	/** Called when the activity is first created. */
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN );
	setContentView(R.layout.mainmenu);
	
    if (Prefs.getScreen(getBaseContext())) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    else
    {
  	  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
	
	View newButton = findViewById(R.id.new_button);
	newButton.setOnClickListener(this);
	View settingsButton = findViewById(R.id.settings_button);
	settingsButton.setOnClickListener(this);
	View helpButton = findViewById(R.id.help_button);
	helpButton.setOnClickListener(this);
	View helpuiButton = findViewById(R.id.uihelp_button);
	helpuiButton.setOnClickListener(this);
	View websiteButton = findViewById(R.id.website_button);
	websiteButton.setOnClickListener(this);
	View exitButton = findViewById(R.id.exit_button);
	exitButton.setOnClickListener(this);
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		//case R.id.about_button:
		//Intent i = new Intent(this, About.class);
		//startActivity(i);
		//break;
		case R.id.settings_button:
			   startActivity(new Intent(this, Prefs.class));
			   break;
		case R.id.help_button:
			Intent f = new Intent(this, Website.class);
			startActivity(f);
		break;
		case R.id.uihelp_button:
			   Intent i = new Intent(this, About.class);
			   startActivity(i);
			   break;
		case R.id.website_button:
			Intent e = new Intent(this, Community.class);
			startActivity(e);
		break;
		// More buttons go here (if any) ...
		case R.id.new_button:
			startGame();
			break;
		case R.id.exit_button:
			finish();
			break;
		}
}
   	   
	   /** Start a new game with the given difficulty level */
	   private void startGame() {

		   Intent intent = new Intent(nethak.this, Dungeoneers.class);
		   startActivity(intent);
		   }
	   
}