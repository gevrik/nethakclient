package net.totalmadownage.nethak;

import  android.content.Context;
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
}
