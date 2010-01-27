package net.totalmadownage.nethak;

import  android.content.Context;
import android.content.SharedPreferences.Editor;
import  android.os.Bundle;
import  android.preference.PreferenceActivity;
import  android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity {
   // Option names and default values
   private static final String OPT_SCREEN = "screen" ;
   private static final boolean OPT_SCREEN_DEF = false;
   private static final String OPT_KEYBOARD = "keyboard" ;
   private static final boolean OPT_KEYBOARD_DEF = true;
   private static final String OPT_VIBRATION = "vibration" ;
   private static final boolean OPT_VIBRATION_DEF = true;
   private static final String OPT_DROPDOWNS = "dropdown" ;
   private static final boolean OPT_DROPDOWNS_DEF = false;
   private static final String OPT_WELCOMEMSG = "welcomemsgflag" ;
   private static final boolean OPT_WELCOMEMSG_DEF = true;
   private static final String VALUE_LASTVERSIONWMSHOWN = "versionwmshown";
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       addPreferencesFromResource(R.xml.settings);
   }
   /** Get the current value of the screen option */
   public static boolean getScreen(Context context) {
       return PreferenceManager.getDefaultSharedPreferences(context)
             .getBoolean(OPT_SCREEN, OPT_SCREEN_DEF);
   }
   /** Get the current value of the keyboard option */
   public static boolean getKeyboard(Context context) {
       return PreferenceManager.getDefaultSharedPreferences(context)
             .getBoolean(OPT_KEYBOARD, OPT_KEYBOARD_DEF);
   }
   /** Get the current value of the screen option */
   public static boolean getVibration(Context context) {
       return PreferenceManager.getDefaultSharedPreferences(context)
             .getBoolean(OPT_VIBRATION, OPT_VIBRATION_DEF);
   }
   /** Get the current value of the drop down size option : true = large, false = small */
   public static boolean getDropdowns(Context context) {
       return PreferenceManager.getDefaultSharedPreferences(context)
             .getBoolean(OPT_DROPDOWNS, OPT_DROPDOWNS_DEF);
   }
   
   /** Get the current value of the always show welcome message flag */
   public static boolean getAlwaysShowWelcomeFlag(Context context) {
       return PreferenceManager.getDefaultSharedPreferences(context)
             .getBoolean(OPT_WELCOMEMSG, OPT_WELCOMEMSG_DEF);
   }
   public static int getLastVersionWelcomeMessageShown(Context context)
   {
	   return PreferenceManager.getDefaultSharedPreferences(context)
	   .getInt(VALUE_LASTVERSIONWMSHOWN, 0);
   }
   public static void setLastVersionWelcomeMessageShown(Context context, int a_version)
   {
	   Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
	   editor.putInt(VALUE_LASTVERSIONWMSHOWN, a_version);
	   editor.commit();
   }
}
