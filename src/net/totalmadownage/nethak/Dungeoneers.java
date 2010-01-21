package net.totalmadownage.nethak;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.Selection;
import android.text.SpannableString;
import android.text.method.BaseKeyListener;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.*;
import android.view.View.OnKeyListener;

import android.view.View.OnClickListener;

import net.totalmadownage.nethak.ConnectionDialog.ServerAdd;
import net.totalmadownage.nethak.ServerListDialog.ConnectReady;
import net.totalmadownage.nethak.TelnetConnectionThread.TelnetThreadListener;
import net.totalmadownage.nethak.R;
import net.totalmadownage.nethak.R.id;
import net.totalmadownage.nethak.R.layout;

public class Dungeoneers extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
	
	//protected static final int MENU_CONNECT = 1;
	protected static final int MENU_DISCONNECT = 2;
	protected static final int MENU_OPTIONS = 3;
	protected static final int MENU_HELP = 4;
	protected static final int MENU_TOGGLE = 5;
	protected PowerManager.WakeLock mWakeLock;
	
    Spinner   spinner1;
    Button    button1;
    Spinner		spinnermodify;
    Button		buttonmodify;

	protected static final int HISTORY_BUFFER_SIZE = 20;
	
	//private SpannableString inputSpanBuffer= new SpannableString("");
	private String inputBuffer;
	private List<String> viewBufferFull;//= new ArrayList<String>();
	private List<Integer> viewBufferFullColor;//= new ArrayList<Integer>();
	private List<Integer> viewBufferFullBGColor;//= new ArrayList<Integer>();
	private List<String> commandHistory;

	private ArrayList<ServerInfo> ServerListing;
	
	public static int MAX_TEXT_LINES = 200;
	public static int LINE_REMOVAL_AMOUNT = 20;

	private float scrollstart = 0;
	private float scrolly = 0;
	private boolean scrolllocked = false; //false follows scroll true stays
	
	String Hostname = "";
	int Port = 7666;
	private SendStack sendData;
	private ThreadListener tlisten;
	//private SendStack recvData = new SendStack(50);
	
	//Selection cmdbuftext;
	
	int current_mode = 0;
	
	private int historypos = 0;

    Thread connectionThread = null;
    
    public class TempData
    {
    	public Thread con;
    	public SendStack stack;
    	public List<String> viewBufferFull;//= new ArrayList<String>();
    	public List<String> commandHistory;//= new ArrayList<String>();
    	public List<Integer> viewBufferFullColor;//= new ArrayList<Integer>();
    	public List<Integer> viewBufferFullBGColor;//= new ArrayList<Integer>();
    }
    
    
    @Override 
    protected void onDestroy()
    {
    	//SavePrefs();
		sendData.push("quit\r\n");
    	this.mWakeLock.release();
    	super.onDestroy();
    }
    
    
    public Object onRetainNonConfigurationInstance()
    {
    	TempData temp = new TempData();
    		temp.con = connectionThread;
    		temp.stack = sendData;
    		temp.viewBufferFull = viewBufferFull;
    		temp.commandHistory = commandHistory;
    		temp.viewBufferFullColor = viewBufferFullColor;
    		temp.viewBufferFullBGColor = viewBufferFullBGColor;
		return temp;
    	
    }
    
    @Override
	protected void onSaveInstanceState(Bundle outState)
    {
    	outState.putString("INPUT_TEXT", inputBuffer);
    	//SavePrefs();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.add(0, MENU_CONNECT, 0, "Connect");
        menu.add(0, MENU_DISCONNECT, 0, "Community");
        menu.add(0, MENU_HELP, 0, "Help");
        menu.add(0, MENU_TOGGLE, 0, "UI");
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	if(this.connectionThread == null)
    	{
    		//menu.findItem(MENU_CONNECT).setEnabled(true);
    		//menu.findItem(MENU_DISCONNECT).setEnabled(false);
    	}
    	else if(this.connectionThread.isAlive())
    	{
    		//menu.findItem(MENU_CONNECT).setEnabled(false);
    		//menu.findItem(MENU_DISCONNECT).setEnabled(true);
    	}
    	else
    	{
    		//menu.findItem(MENU_CONNECT).setEnabled(true);
    		//menu.findItem(MENU_DISCONNECT).setEnabled(false);
    	}
		return true;
    }
	
	private void SavePrefs()
	{
		SharedPreferences.Editor edit = getPreferences(0).edit();
		
		edit.putInt("SERVER_COUNT", ServerListing.size() - 1);
		
		for(int x=1; x< ServerListing.size(); x++)
		{
			ServerInfo s = ServerListing.get(x);
			edit.putString("SERVER_NAME" + x, s.ServerName);
			edit.putString("SERVER_IP" + x, s.IP);
			edit.putInt("SERVER_PORT" + x, s.Port);
		}
		edit.commit();
	}

	private void LoadPrefs()
	{
		SharedPreferences prefs = getPreferences(0);
		
		int servers = prefs.getInt("SERVER_COUNT", 0);

		if(servers > 0)
		{
			for(int x=1; x< servers+1; x++)
			{
				ServerListing.add(new ServerInfo(prefs.getString("SERVER_NAME"+x,"0"),
						prefs.getString("SERVER_IP"+x,"0"),
						prefs.getInt("SERVER_PORT"+x,0)));
			}
		}
		else
		{
			//SETUP DEFAULT SERVERS
			ServerListing.add(new ServerInfo("NetHak Beta", "gevrik.ath.cx", 4000));
		}
		
	}
    @Override
    public boolean onTouchEvent(MotionEvent motion)
    {
		TextView textview = (TextView)findViewById(R.id.MainText);
    	switch(motion.getAction())
    	{
    	case MotionEvent.ACTION_DOWN:
    		scrollstart = textview.getScrollY();
    		scrolly = motion.getY();
    		break;
    	case MotionEvent.ACTION_MOVE:
			
			float distance = (scrolly - motion.getY());
			float max_scroll = (textview.getLineCount() * textview.getLineHeight()) - textview.getHeight();
			
			if(distance + scrollstart > max_scroll)
			{
				
			}
			else if(distance + scrollstart <= 0)
			{
				
			}
			else
			{
				textview.scrollTo(0, Math.round(distance + scrollstart));
				
				if(textview.getScrollY() >= (max_scroll-5))
				{
					scrolllocked = false;
				}
				else
				{
					scrolllocked = true;
				}
			}
    		scrollstart = textview.getScrollY();
    		scrolly = motion.getY();
			break;
    	default:
    		break;
    	}
		return false;
    }
    
	private class OnReadyListener implements ServerListDialog.ConnectReady {

		//@Override
		public void connect(int pos) {
			// TODO Auto-generated method stub
			if(pos !=1)
			{
				Hostname = ServerListing.get(pos).IP;
				Port = ServerListing.get(pos).Port;
				
				tlisten = new ThreadListener();
	            connectionThread = new Thread(new TelnetConnectionThread("gevrik.ath.cx",4000,sendData,tlisten));
	            connectionThread.start(); 
			}
			else
			{
            	ConnectionDialog dialog = new ConnectionDialog(Dungeoneers.this, new ServerAddListener());
                dialog.show();
			}
		}

		//@Override
		public void delete(int pos) {
			
			ServerListing.remove(pos);

        	ConnectReady cready = new OnReadyListener();
        	ServerListDialog dialog = new ServerListDialog(Dungeoneers.this, cready, ServerListing);
            dialog.show();
		}

		//@Override
		public void modify(int pos) {
			
        	ConnectionDialog dialog = new ConnectionDialog(Dungeoneers.this, pos,  new ServerAddListener(),ServerListing.get(pos).ServerName,ServerListing.get(pos).IP,ServerListing.get(pos).Port);
            dialog.show();
			
		}

   } 
	
	private class ThreadListener implements TelnetConnectionThread.TelnetThreadListener
	{
		//@Override
		public void dataReady(Message m) {
	        Dungeoneers.this.TCUpdateHandler.sendMessage(m);
		}
	}
	
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        /*
        case MENU_CONNECT:
            if(connectionThread == null)
            {
            	ConnectReady cready = new OnReadyListener();
            	ServerListDialog dialog = new ServerListDialog(this, cready, ServerListing);
                dialog.show();
            }
            else if(!this.connectionThread.isAlive())
            {
            	ConnectReady cready = new OnReadyListener();
            	ServerListDialog dialog = new ServerListDialog(this, cready, ServerListing);
                dialog.show();
            }
            return true;
            */
        case MENU_DISCONNECT:
			Intent f = new Intent(this, Community.class);
			startActivity(f);
        	return true;
        case MENU_HELP:
			Intent e = new Intent(this, Website.class);
			startActivity(e);
        	return true;
        case MENU_TOGGLE:
        	View cmd = (View)findViewById(R.id.CommandBox);
        	View cmdkey = (View)findViewById(R.id.KeyboardBox);
        	int visibility = cmd.getVisibility();
        	int visibilitykey = cmdkey.getVisibility();
        		if (visibility == 4 && visibilitykey == 4)
        		{
        			cmd.setVisibility(0);
        			//cmdkey.setVisibility(0);
        		}
        		else
        		{
        			cmd.setVisibility(4);
        			cmdkey.setVisibility(4);
        		}
        	return true;
        }
        return false;
    }

    private class CommandListener implements OnKeyListener
    {

		//@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub

	    	if(event.getAction() == KeyEvent.ACTION_DOWN)
			{
				switch (keyCode)
				{
				case KeyEvent.KEYCODE_DPAD_DOWN:
				{
					EditText cmd = (EditText)findViewById(R.id.cmdText);
					if(historypos > 0)
					{
						historypos --;
					}
					if(historypos == 0)
					{
						cmd.setText("");
					}
					else
					{
						cmd.setText(commandHistory.get(commandHistory.size() - historypos));
						Spannable cmdbuffer = (Spannable)cmd.getText();
						Selection.moveToRightEdge(cmdbuffer, cmd.getLayout());
					}
					return true;
				}
				case KeyEvent.KEYCODE_DPAD_UP:
				{
					EditText cmd = (EditText)findViewById(R.id.cmdText);
					if(historypos < commandHistory.size())
					{
						historypos++;
					}
					if(historypos == 0)
					{
						cmd.setText("");
					}
					else
					{
						cmd.setText(commandHistory.get(commandHistory.size() - historypos));
						Spannable cmdbuffer = (Spannable)cmd.getText();
						Selection.moveToRightEdge(cmdbuffer, cmd.getLayout());
					}
					return true;
				}
				case KeyEvent.KEYCODE_BACK:
				{
					openNewGameDialog();
					//sendData.push("quit\r\n");
					//Dungeoneers.this.finish();
				//	return false;
				}
				case KeyEvent.KEYCODE_ENTER:
					EditText cmd = (EditText)findViewById(R.id.cmdText);
					sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);
					
					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");
					
					scrolllocked = false;
					
					TextView textview = (TextView)findViewById(R.id.MainText);
					textview.scrollTo(0, (textview.getLineCount() * textview.getLineHeight()) - textview.getHeight());
					return true;
				
				}
			}
			return false;
		}
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    
        //setTheme(R.style.MyTheme);
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        //setContentView(R.layout.splashscreen);
        setContentView(R.layout.main);
        
        if (!Prefs.getScreen(getBaseContext())) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE); 
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag"); 
        this.mWakeLock.acquire();
        
        EditText cmd = (EditText)findViewById(R.id.cmdText);
        if (Prefs.getKeyboard(getBaseContext())) {
        cmd.setInputType(InputType.TYPE_NULL);
        }
        TextView textview = (TextView)findViewById(R.id.MainText);             
        
        if(savedInstanceState == null)
        {
        
        	viewBufferFull= new ArrayList<String>();
        	commandHistory= new ArrayList<String>();
        	viewBufferFullColor= new ArrayList<Integer>();
        	viewBufferFullBGColor= new ArrayList<Integer>();

        	scrolllocked = false;

            if(connectionThread == null)
            {
            
           	
        		tlisten = new ThreadListener();
        		sendData = new SendStack(50);
                connectionThread = new Thread(new TelnetConnectionThread("kults.genesismuds.com",7666,sendData,tlisten));
                connectionThread.start(); 
            	
            }
            else if(!this.connectionThread.isAlive())
            {
            	//ConnectReady cready = new OnReadyListener();
            	//ServerListDialog dialog = new ServerListDialog(this, cready, ServerListing);
                //dialog.show();
            	
        		tlisten = new ThreadListener();
        		sendData = new SendStack(50);
                connectionThread = new Thread(new TelnetConnectionThread("kults.genesismuds.com",7666,sendData,tlisten));
                connectionThread.start(); 
            	
            }
        	
        }
        else
        {
        	viewBufferFull= (List<String>)((TempData)this.getLastNonConfigurationInstance()).viewBufferFull;
        	commandHistory= (List<String>)((TempData)this.getLastNonConfigurationInstance()).commandHistory;
        	viewBufferFullColor= (List<Integer>)((TempData)this.getLastNonConfigurationInstance()).viewBufferFullColor;
        	viewBufferFullBGColor= (List<Integer>)((TempData)this.getLastNonConfigurationInstance()).viewBufferFullBGColor;
        	
        	connectionThread = (Thread)((TempData)this.getLastNonConfigurationInstance()).con;
        	sendData = (SendStack)((TempData)this.getLastNonConfigurationInstance()).stack;
        	
        	tlisten = new ThreadListener();
        	sendData.push(tlisten);
        	
        	inputBuffer = (String)savedInstanceState.getString("INPUT_TEXT");
        	
        	scrolllocked = false;

            if(connectionThread == null)
            {
            	//ConnectReady cready = new OnReadyListener();
                connectionThread = new Thread(new TelnetConnectionThread("gevrik.ath.cx",4000,sendData,tlisten));
                connectionThread.start(); 
            }
            else if(!this.connectionThread.isAlive())
            {
            	//ConnectReady cready = new OnReadyListener();
                connectionThread = new Thread(new TelnetConnectionThread("gevrik.ath.cx",4000,sendData,tlisten));
                connectionThread.start(); 
            }
        	
        }


		cmd.setText(inputBuffer);
		cmd.setOnKeyListener(new CommandListener());
        refreshView();
		textview.scrollTo(0, (textview.getLineCount() * textview.getLineHeight()) - textview.getHeight());
		
	     // Set up click listeners for all the buttons
        View northButton = findViewById(R.id.north_button);
        northButton.setOnClickListener(this);
        View eastButton = findViewById(R.id.east_button);
        eastButton.setOnClickListener(this);
        View southButton = findViewById(R.id.south_button);
        southButton.setOnClickListener(this);
        View westButton = findViewById(R.id.west_button);
        westButton.setOnClickListener(this);
        View upButton = findViewById(R.id.up_button);
        upButton.setOnClickListener(this);
        View downButton = findViewById(R.id.down_button);
        downButton.setOnClickListener(this);
        View lookButton = findViewById(R.id.look_button);
        lookButton.setOnClickListener(this);
        View enterButton = findViewById(R.id.enter_button);
        enterButton.setOnClickListener(this);
        
        View commandButton = findViewById(R.id.command_button);
        commandButton.setOnClickListener(this);

        View toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener(this);        
        View uinumbButton = findViewById(R.id.uinumb_button);
        uinumbButton.setOnClickListener(this);
        View uibackButton = findViewById(R.id.uiback_button);
        uibackButton.setOnClickListener(this);
        
        View qButton = findViewById(R.id.key_q);
        qButton.setOnClickListener(this);
        View wButton = findViewById(R.id.key_w);
        wButton.setOnClickListener(this);
        View eButton = findViewById(R.id.key_e);
        eButton.setOnClickListener(this);
        View rButton = findViewById(R.id.key_r);
        rButton.setOnClickListener(this);
        View tButton = findViewById(R.id.key_t);
        tButton.setOnClickListener(this);
        View yButton = findViewById(R.id.key_y);
        yButton.setOnClickListener(this);
        View uButton = findViewById(R.id.key_u);
        uButton.setOnClickListener(this);
        View iButton = findViewById(R.id.key_i);
        iButton.setOnClickListener(this);
        View oButton = findViewById(R.id.key_o);
        oButton.setOnClickListener(this);
        View pButton = findViewById(R.id.key_p);
        pButton.setOnClickListener(this);

        View aButton = findViewById(R.id.key_a);
        aButton.setOnClickListener(this);
        View sButton = findViewById(R.id.key_s);
        sButton.setOnClickListener(this);
        View dButton = findViewById(R.id.key_d);
        dButton.setOnClickListener(this);
        View fButton = findViewById(R.id.key_f);
        fButton.setOnClickListener(this);
        View gButton = findViewById(R.id.key_g);
        gButton.setOnClickListener(this);
        View hButton = findViewById(R.id.key_h);
        hButton.setOnClickListener(this);
        View jButton = findViewById(R.id.key_j);
        jButton.setOnClickListener(this);
        View kButton = findViewById(R.id.key_k);
        kButton.setOnClickListener(this);
        View lButton = findViewById(R.id.key_l);
        lButton.setOnClickListener(this);
        View qmlsButton = findViewById(R.id.key_qm_ls);
        qmlsButton.setOnClickListener(this);

        View zButton = findViewById(R.id.key_z);
        zButton.setOnClickListener(this);
        View xButton = findViewById(R.id.key_x);
        xButton.setOnClickListener(this);
        View cButton = findViewById(R.id.key_c);
        cButton.setOnClickListener(this);
        View vButton = findViewById(R.id.key_v);
        vButton.setOnClickListener(this);
        View bButton = findViewById(R.id.key_b);
        bButton.setOnClickListener(this);
        View nButton = findViewById(R.id.key_n);
        nButton.setOnClickListener(this);
        View mButton = findViewById(R.id.key_m);
        mButton.setOnClickListener(this);
        View dotButton = findViewById(R.id.key_dot);
        dotButton.setOnClickListener(this);
        View slashlsButton = findViewById(R.id.key_slash);
        slashlsButton.setOnClickListener(this);
        View commalsButton = findViewById(R.id.key_comma);
        commalsButton.setOnClickListener(this);
        
        View delButton = findViewById(R.id.key_del);
        delButton.setOnClickListener(this);
        View spacelsButton = findViewById(R.id.key_spacels);
        spacelsButton.setOnClickListener(this); 
        View enterlsButton = findViewById(R.id.key_enterls);
        enterlsButton.setOnClickListener(this); 
        
        View oneButton = findViewById(R.id.key_1);
        oneButton.setOnClickListener(this);
        View twoButton = findViewById(R.id.key_2);
        twoButton.setOnClickListener(this);
        View threeButton = findViewById(R.id.key_3);
        threeButton.setOnClickListener(this);
        View fourButton = findViewById(R.id.key_4);
        fourButton.setOnClickListener(this);
        View fiveButton = findViewById(R.id.key_5);
        fiveButton.setOnClickListener(this);
        View sixButton = findViewById(R.id.key_6);
        sixButton.setOnClickListener(this);
        View sevenButton = findViewById(R.id.key_7);
        sevenButton.setOnClickListener(this);
        View eightButton = findViewById(R.id.key_8);
        eightButton.setOnClickListener(this);
        View nineButton = findViewById(R.id.key_9);
        nineButton.setOnClickListener(this);
        View tenButton = findViewById(R.id.key_0);
        tenButton.setOnClickListener(this);
                
        View bsButton = findViewById(R.id.key_bs);
        bsButton.setOnClickListener(this);
        View enButton = findViewById(R.id.key_en);
        enButton.setOnClickListener(this); 
        View spaceButton = findViewById(R.id.key_space);
        spaceButton.setOnClickListener(this); 
        
        View oneaButton = findViewById(R.id.one_button);
        oneaButton.setOnClickListener(this);
        View twoaButton = findViewById(R.id.two_button);
        twoaButton.setOnClickListener(this);
        View threeaButton = findViewById(R.id.three_button);
        threeaButton.setOnClickListener(this);
        View fouraButton = findViewById(R.id.four_button);
        fouraButton.setOnClickListener(this);
        View fiveaButton = findViewById(R.id.five_button);
        fiveaButton.setOnClickListener(this);
        View sixaButton = findViewById(R.id.six_button);
        sixaButton.setOnClickListener(this);
        View sevenaButton = findViewById(R.id.seven_button);
        sevenaButton.setOnClickListener(this);
        View eightaButton = findViewById(R.id.eight_button);
        eightaButton.setOnClickListener(this);
        View nineaButton = findViewById(R.id.nine_button);
        nineaButton.setOnClickListener(this);
        View nullaButton = findViewById(R.id.null_button);
        nullaButton.setOnClickListener(this);
        View spaceaButton = findViewById(R.id.space_button);
        spaceaButton.setOnClickListener(this);
        View nbbsButton = findViewById(R.id.nbbs_button);
        nbbsButton.setOnClickListener(this);
               
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clicker()); 

    	spinnermodify = (Spinner) 
     	findViewById  (R.id.spinnermodify);  
    	
    	buttonmodify  = (Button)
    	findViewById (R.id.buttonmodify);                                               
        
    	ArrayAdapter<String> adaptermodify = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraymodify);        
    	adaptermodify.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinnermodify.setAdapter(adaptermodify);         
    	buttonmodify.setOnClickListener(  new clickermodify()); 
    	
    	
    }

      
    private static final String[] arraycategories = {        "info", "nodes", "talk", "combat",
        "travel", "building", "interact", "shopping", "banking", "organization", "group" }; 
    
    private static final String[] arraydirs = {        "north ", "east ", "south ", "west ",
        "up ", "down " }; 

    private static final String[] arraydirsconnect = {        "north connect ", "east connect ", "south connect ", "west connect ",
        "up connect ", "down connect " }; 
    
    private static final String[] arraydirskeycode = {        "north keycode ", "east keycode ", "south keycode ", "west keycode ",
        "up keycode ", "down keycode " }; 
    
    private static final String[] arraydirsdoor = {        "north door", "east door", "south door", "west door",
        "up door", "down door" }; 
    
    private static final String[] arrayorgs = {        "turing", "moderns" }; 
    
    private static final String[] arraysystems = {        "berlin ", "seattle ", "straylight " };
    /*
    private static final String[] arraysystems = {        "ascension ", "beijing ", "berlin ", "bosat ", "cairo ", "chiba ", "chicago ",
    	"dakar ", "delhi ", "denver ", "havana ", "hongkong ", "honolulu ", "london ", "losangeles ", "madrid ", "melbourne ", "moscow ",
    	"nairobi ", "newash ", "panamacity ", "paris ", "rio ", "rome ", "saltlake ", "stockholm ", "seattle ", "straylight " }; 
    */    
    class  clicker implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
    	       
		    	View cmda = (View)findViewById(R.id.CommandBox);
	        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
	        	View cmdnum = (View)findViewById(R.id.NumberBox);
	        	View cmdspin = (View)findViewById(R.id.spinner1);
	        	View cmdconfirm = (View)findViewById(R.id.button1);

	        	cmda.setVisibility(4);
	        	cmdkb.setVisibility(0);
	        	cmdnum.setVisibility(4);
	        	cmdspin.setVisibility(4);
	        	cmdconfirm.setVisibility(4);
    	       
    	         cmd.setText(cmd.getText() + s);    
    				Spannable cmdbufferh = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferh, cmd.getLayout());
    	}                                         

    }  
    
    class  clickerkeynum implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
    	       
		    	View cmda = (View)findViewById(R.id.CommandBox);
	        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
	        	View cmdnum = (View)findViewById(R.id.NumberBox);
	        	View cmdnumblock = (View)findViewById(R.id.NumblockBox);
	        	View cmdspin = (View)findViewById(R.id.spinner1);
	        	View cmdconfirm = (View)findViewById(R.id.button1);

	        	cmda.setVisibility(4);
	        	cmdkb.setVisibility(4);
	        	cmdnum.setVisibility(4);
	        	cmdnumblock.setVisibility(0);
	        	cmdspin.setVisibility(4);
	        	cmdconfirm.setVisibility(4);
    	       
    	         cmd.setText(cmd.getText() + s);    
    				Spannable cmdbufferh = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferh, cmd.getLayout());
    	}                                         

    } 
 
    class  clickercategories implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 

    	       if (s == "info") {
    	    	   startInfo();				
    	       }
    	       else if (s == "nodes") {
    	    	   startNodes();				
    	       }  
    	       else if (s == "talk") {
    	    	   startTalk();				
    	       }  
    	       else if (s == "combat") {
    	    	   startCombat();				
    	       }  
    	       else if (s == "travel") {
    	    	   startTravel();				
    	       }  
    	       else if (s == "building") {
    	    	   startConstruct();				
    	       }  
    	       else if (s == "interact") {
    	    	   startInteract();				
    	       }  
    	       else if (s == "shopping") {
    	    	   startShopping();				
    	       }  
    	       else if (s == "banking") {
    	    	   startBank();				
    	       }
    	       else if (s == "organization") {
    	    	   startOrganization();				
    	       } 
    	       else if (s == "group") {
    	    	   startGroup();				
    	       } 
    	       else openCommonDialog();
    	}                                         

    } 

    private static final String[] arraycatinfo = {        "score", "who", "skills", "systems",
        "regions", "organizations", "affected", "commands", "equipment", "inventory", "compare ", "showorg ", "showsystem " }; 
        
	private void startInfo() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatinfo);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatinfo()); 
		
	}

    class  clickercatinfo implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "compare ")
    	         {
    	        	cmd.setText(cmd.getText() + s); 
    	        	Spannable cmdbufferw = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
    	         }
    	         else if (s == "showorg ")
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
    	        	 startOrgs();
    	         }
    	         else if (s == "showsystem ")
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
    	        	 startSystem();
    	         }
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");
    	         }
    	         
    	}                                         

    } 

     
    private static final String[] arraycatnodes = {        "look", "analyze", "search", "dig",
        "exits", "glance", "examine ", "drop ", "unlock ", "scan " }; 
    
	private void startNodes() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatnodes);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatnodes()); 
		
	}

    class  clickercatnodes implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "examine " || s == "drop ")
    	         {
    	        	cmd.setText(cmd.getText() + s); 
    	        	Spannable cmdbufferw = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
    	         }
    	         else if (s == "unlock ")
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
    	        	 startDirsNum();
    	         }
    	         else if (s == "scan ")
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
    	        	 startDirs();
    	         }
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");
    	         }
    	         
    	}                                         

    } 


    
    private static final String[] arraycattalk = {        "say job", "say ", "tell ", "reply ",
        "gchat ", "schat ", "yell ", "afk", "bio", "gtell ", "orgtalk ", "ooc " }; 

	private void startTalk() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycattalk);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercattalk()); 
		
	}
	
    class  clickercattalk implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "afk" || s == "bio" || s == "say job")
    	         {
    	        	     	        	 
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");
    	        	 
    	         }
     	         else
    	         {
     	        	cmd.setText(cmd.getText() + s); 
    	        	Spannable cmdbufferw = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
    	         }
    	         
    	}                                         

    } 

    
    private static final String[] arraycatcombat = {        "kill virus", "kill ICE", "kill program", "kill ",
        "blast ", "consider ", "flee ", "rest", "sleep", "wake", "sit", "stand", "setblaster ", "shove " }; 
    
	private void startCombat() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatcombat);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatcombat()); 
		
	}
	
    class  clickercatcombat implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "kill " || s == "consider " || s == "setblaster " || s == "shove ")
    	         {

    	        	 cmd.setText(cmd.getText() + s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				
    	         }
    	         else if (s == "blast " || s == "flee ")
    	         {

   	        	    cmd.setText(cmd.getText() + s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				
     				startDirs();
     				
    	         }

    	         
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");

    	         }
    	         
    	}                                         

    } 

    private static final String[] arraycattravel = {        "home", "enter", "connect " }; 
    
	private void startTravel() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycattravel);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercattravel()); 
		
	}
	
    class  clickercattravel implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "connect ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startSystem();
    	         }
    	         
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");

    	         }
    	         
    	}                                         

    } 
    
    /*
    <array name="constructcommands">
    <item>construct [dir]</item>
    <item>modify [type]</item>
    <item>bridge [dir] connect [id]</item>
    <item>bridge [dir] door</item>
    <item>bridge [dir] keycode [code]</item>
    <item>node description [editor]</item>
    </array>
    */
    
    private static final String[] arraycatconstruct = {        "construct ", "modify ", "bridge connect", "bridge door",
    															"bridge keycode", "nodedescription" };    
    
    
	private void startConstruct() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatconstruct);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatconstruct()); 
		
	}
	
    class  clickercatconstruct implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "construct ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startDirs();
    	         }
    	         else if (s == "modify ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startModify();
    	         }    
    	         else if (s == "bridge connect")
    	         {

    	        	cmd.setText("bridge "); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startDirsConnect();
    	         }  
    	         else if (s == "bridge door")
    	         {

    	        	cmd.setText("bridge "); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startDirsDoor();
    	         }  
    	         else if (s == "bridge keycode")
    	         {

    	        	cmd.setText("bridge "); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startDirsKeycode();
    	         }  
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");

    	         }
    	         
    	}                                         

    } 

     
    private static final String[] arraycatinteract = {        "give package program", "give virus dataminer", "give ", "wield ",
		"wear ", "remove ", "get ", "open ", "close ", "lock ", "unlock ", "drag ", "empty ", "fill ", "activate ", "teach ", "visible" };  

	private void startInteract() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatinteract);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatinteract()); 
		
	}
	
    class  clickercatinteract implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "give " || s == "wield " || s == "drag " || s == "wear " || s == "remove " || s == "get " || s == "empty " || s == "fill " || s == "activate " || s == "teach ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());

    	         }
    	         else if (s == "open " || s == "close ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startDirs();
    	         }    
    	         else if (s == "lock " || s == "unlock ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startDirsNum();
    	         }  
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");

    	         }
    	         
    	}                                         

    } 
    
    /*
<array name="shopcommands">
<item>List</item>
<item>Buy [object]</item>
<item>Sell [object]</item>
<item>Repair [object]</item>
<item>Value [object]</item>
<item>Buyskill</item>
<item>Buyskill [skill]</item>
</array>
*/
    
    private static final String[] arraycatshopping = {        "list", "buy ", "sell ", "repair ",
		"value ", "buyskill", "buyskill [skill]" }; 
    
	private void startShopping() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatshopping);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatshopping()); 
		
	}
	
    class  clickercatshopping implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "buy " || s == "sell " || s == "repair " || s == "value ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());

    	         }
    	         else if (s == "buyskill [skill]")
    	         {

    	        	cmd.setText("buyskill "); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startBuyskill();
    	         }    
    	         else if (s == "lock " || s == "unlock ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());
     				startDirsNum();
    	         }  
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");

    	         }
    	         
    	}                                         

    } 
    

    private static final String[] arraycatbuyskill = {        "aid", "backstab", "blades", "blasters",
		"codeblade", "codeblaster", "codecomlink", "codecontainer", "codedef", "codeshield", "codeutil",
		"damboost", "disguise", "dodge", "dualwield", "firstaid", "hide", "peek", "picklock",
		"poisonmod", "postguard", "propaganda", "quicktalk", "reinforcements", "second attack",
		"steal", "throw"}; 
    
	private void startBuyskill() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatbuyskill);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercommand()); 
		
	}
	
	/*
	<array name="bankingcommands">
	<item>Balance</item>
	<item>Deposit [amount]</item>
	<item>Withdraw [amount]</item>
	<item>Transfer [a] [p]</item>
	</array>
    */
	
    private static final String[] arraycatbank = { "balance", "deposit ", "withdraw ", "transfer " }; 
    
	private void startBank() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatbank);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatbank()); 
		
	}
	
    class  clickercatbank implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "deposit " || s == "withdraw " || s == "transfer ")
    	         {

    	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());

    	         }
    	         else
    	         {
    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");

    	         }
    	         
    	}                                         

    } 

    
    private static final String[] arraycatorganization = { "donate ", "demote ", "empower ", "induct ", "withdraw ", "war ", "outcast ",
    	"setwages ", "enlist", "overthrow" }; 
    
	private void startOrganization() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatorganization);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatorganization()); 
		
	}
	
    class  clickercatorganization implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "enlist" || s == "overthrow")
    	         {

    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");
    	        	 
    	         }
    	         else
    	         {

     	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());

    	         }
    	         
    	}                                         

    } 
	
    private static final String[] arraycatgroup = { "follow ", "group" }; 
    
	private void startGroup() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycatgroup);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercatgroup()); 
		
	}
	
    class  clickercatgroup implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
   	           	         
    	         if (s == "group")
    	         {

    	        	 cmd.setText(cmd.getText() + s); 
    	        	 sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");
    	        	 
    	         }
    	         else
    	         {

     	        	cmd.setText(s); 
     	        	Spannable cmdbufferw = (Spannable)cmd.getText();
     				Selection.moveToRightEdge(cmdbufferw, cmd.getLayout());

    	         }
    	         
    	}                                         

    } 
    
	private void startDirsKeycode() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirskeycode);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickerkeynum()); 
		
	}
	
	private void startDirsDoor() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirsdoor);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercommand()); 
		
	}
    
	private void startDirsConnect() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirsconnect);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickerkeynum()); 
		
	}
    
	private void startDirsNum() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickerkeynum()); 
		
	}
	
	private void startModify() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraymodify);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercommand()); 
		
	}

	private void startDirs() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercommand()); 
		
	}

	
	private void startOrgs() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayorgs);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercommand()); 
		
	}
	
	private void startSystem() {
		
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraysystems);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickerkeynum()); 
		
	}
    
    class  clickercommand implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinner1.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
    	       
		    	View cmda = (View)findViewById(R.id.CommandBox);
	        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
	        	View cmdnum = (View)findViewById(R.id.NumberBox);
	        	View cmdspin = (View)findViewById(R.id.spinner1);
	        	View cmdconfirm = (View)findViewById(R.id.button1);

	        	cmda.setVisibility(0);
	        	cmdkb.setVisibility(4);
	        	cmdnum.setVisibility(4);
	        	cmdspin.setVisibility(4);
	        	cmdconfirm.setVisibility(4);
    	       
    	         cmd.setText(cmd.getText() + s);    
    				
					sendData.push(cmd.getText() + "\r\n");
					addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmd.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmd.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmd.getText().toString());
					}
					cmd.setText("");
    	         
    	}                                         

    } 
    
    private static final String[] arraymodify = {        "database", "terminal", "subserver", "agent",
        "trade", "supply", "pawn", "coding", "firewall", "bank", "employment" }; 
    
    class  clickermodify implements  Button.OnClickListener

    { 
    	public   void  onClick(View   v)
                  {        
    	       String       s = (String) spinnermodify.getSelectedItem(); 
    	       EditText cmd = (EditText)findViewById(R.id.cmdText);
    	       
		    	View cmda = (View)findViewById(R.id.CommandBox);
	        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
	        	View cmdspin = (View)findViewById(R.id.spinnermodify);
	        	View cmdconfirm = (View)findViewById(R.id.buttonmodify);

	        	cmda.setVisibility(0);
	        	cmdkb.setVisibility(4);
	        	cmdspin.setVisibility(4);
	        	cmdconfirm.setVisibility(4);
    	       
    	         cmd.setText(cmd.getText() + s);    
    				Spannable cmdbufferh = (Spannable)cmd.getText();
    				Selection.moveToRightEdge(cmdbufferh, cmd.getLayout());
    	}                                         

    }  
    
	public void onClick(View v) {
		
	     Vibrator vb = ( Vibrator )getApplication().getSystemService( Service.VIBRATOR_SERVICE ); 
		EditText cmdkeyboard = (EditText)findViewById(R.id.cmdText);
		
		switch (v.getId()) {

		
		/*
		case R.id.key_num:
				    	
        	View cmdnum = (View)findViewById(R.id.NumberBox);
        	int visibilitynum = cmdnum.getVisibility();
        		if (visibilitynum == 0)
        		{
        			cmdnum.setVisibility(4);
        		}
        		else
        		{
        			cmdnum.setVisibility(0);
        		}
			
        		if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
			
		break;
	*/
		
		case R.id.one_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "1");
			Spannable cmdbufferoneb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferoneb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.two_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "2");
			Spannable cmdbuffertwob = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffertwob, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.three_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "3");
			Spannable cmdbufferthreeb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferthreeb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.four_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "4");
			Spannable cmdbufferfourb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferfourb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.five_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "5");
			Spannable cmdbufferfiveb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferfiveb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.six_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "6");
			Spannable cmdbuffersixb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffersixb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.seven_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "7");
			Spannable cmdbuffersevenb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffersevenb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.eight_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "8");
			Spannable cmdbuffereightb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffereightb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.nine_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "9");
			Spannable cmdbuffernineb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffernineb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.null_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + "0");
			Spannable cmdbuffernullb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffernullb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.space_button:
			cmdkeyboard.setText(cmdkeyboard.getText() + " ");
			Spannable cmdbufferspaceb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferspaceb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.nbbs_button:
			CharSequence texta = cmdkeyboard.getText();
			if (!(texta instanceof Editable)) {
		          cmdkeyboard.setText(texta, BufferType.EDITABLE);
		        }
		        Editable editablea = (Editable)cmdkeyboard.getText();

		        if (texta.length() > 0) {
		          editablea.delete(texta.length() - 1, texta.length());
		        }
			
		        if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.key_q:
			cmdkeyboard.setText(cmdkeyboard.getText() + "q");
			Spannable cmdbufferq = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferq, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;		

		case R.id.key_w:
			cmdkeyboard.setText(cmdkeyboard.getText() + "w");
			Spannable cmdbufferw = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferw, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_e:
			cmdkeyboard.setText(cmdkeyboard.getText() + "e");
			Spannable cmdbuffere = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffere, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_r:
			cmdkeyboard.setText(cmdkeyboard.getText() + "r");
			Spannable cmdbufferr = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferr, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_t:
			cmdkeyboard.setText(cmdkeyboard.getText() + "t");
			Spannable cmdbuffert = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffert, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_y:
			cmdkeyboard.setText(cmdkeyboard.getText() + "y");
			Spannable cmdbuffery = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffery, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_u:
			cmdkeyboard.setText(cmdkeyboard.getText() + "u");
			Spannable cmdbufferu = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferu, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_i:
			cmdkeyboard.setText(cmdkeyboard.getText() + "i");
			Spannable cmdbufferi = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferi, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_o:
			cmdkeyboard.setText(cmdkeyboard.getText() + "o");
			Spannable cmdbuffero = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffero, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_p:
			cmdkeyboard.setText(cmdkeyboard.getText() + "p");
			Spannable cmdbufferp = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferp, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_a:
			cmdkeyboard.setText(cmdkeyboard.getText() + "a");
			Spannable cmdbuffera = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffera, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;		

		case R.id.key_s:
			cmdkeyboard.setText(cmdkeyboard.getText() + "s");
			Spannable cmdbuffers = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffers, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_d:
			cmdkeyboard.setText(cmdkeyboard.getText() + "d");
			Spannable cmdbufferd = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferd, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_f:
			cmdkeyboard.setText(cmdkeyboard.getText() + "f");
			Spannable cmdbufferf = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferf, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_g:
			cmdkeyboard.setText(cmdkeyboard.getText() + "g");
			Spannable cmdbufferg = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferg, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_h:
			cmdkeyboard.setText(cmdkeyboard.getText() + "h");
			Spannable cmdbufferh = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferh, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_j:
			cmdkeyboard.setText(cmdkeyboard.getText() + "j");
			Spannable cmdbufferj = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferj, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_k:
			cmdkeyboard.setText(cmdkeyboard.getText() + "k");
			Spannable cmdbufferk = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferk, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_l:
			cmdkeyboard.setText(cmdkeyboard.getText() + "l");
			Spannable cmdbufferl = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferl, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_qm_ls:
			cmdkeyboard.setText(cmdkeyboard.getText() + "l");
			Spannable cmdbufferqmls = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferqmls, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		
		case R.id.key_z:
			cmdkeyboard.setText(cmdkeyboard.getText() + "z");
			Spannable cmdbufferz = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferz, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_x:
			cmdkeyboard.setText(cmdkeyboard.getText() + "x");
			Spannable cmdbufferx = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferx, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_c:
			cmdkeyboard.setText(cmdkeyboard.getText() + "c");
			Spannable cmdbufferc = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferc, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_v:
			cmdkeyboard.setText(cmdkeyboard.getText() + "v");
			Spannable cmdbufferv = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferv, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_m:
			cmdkeyboard.setText(cmdkeyboard.getText() + "m");
			Spannable cmdbufferm = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferm, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_b:
			cmdkeyboard.setText(cmdkeyboard.getText() + "b");
			Spannable cmdbufferb = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferb, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_n:
			cmdkeyboard.setText(cmdkeyboard.getText() + "n");
			Spannable cmdbuffern = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffern, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_dot:
			cmdkeyboard.setText(cmdkeyboard.getText() + ".");
			Spannable cmdbufferdot = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferdot, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_slash:
			cmdkeyboard.setText(cmdkeyboard.getText() + "/");
			Spannable cmdbufferslash = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferslash, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_comma:
			cmdkeyboard.setText(cmdkeyboard.getText() + ",");
			Spannable cmdbuffercomma = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffercomma, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;		
		
		
		case R.id.uinumb_button:
	    	View cmdaui = (View)findViewById(R.id.CommandBox);
        	View cmdkbui = (View)findViewById(R.id.KeyboardBox);
        	View cmdnumberui = (View)findViewById(R.id.NumberBox);
        	View cmdnumblockui = (View)findViewById(R.id.NumblockBox);
        	View cmdspinui = (View)findViewById(R.id.spinner1);
        	View cmdconfirmui = (View)findViewById(R.id.button1);
        	View cmduibackbuttonui = (View)findViewById(R.id.uiback_button);
        	        	
        			cmdaui.setVisibility(4);
        			cmdkbui.setVisibility(4);
        			cmdnumberui.setVisibility(4);
        			cmdnumblockui.setVisibility(0);
        			cmdspinui.setVisibility(4);
        			cmdconfirmui.setVisibility(4);
        			cmduibackbuttonui.setVisibility(4);
 
        		
        		if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;		

		case R.id.key_space:
			cmdkeyboard.setText(cmdkeyboard.getText() + " ");
			Spannable cmdbuffersp = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffersp, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_spacels:
			cmdkeyboard.setText(cmdkeyboard.getText() + " ");
			Spannable cmdbufferspls = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferspls, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.key_del:
			cmdkeyboard.setText(cmdkeyboard.getText() + " ");
			Spannable cmdbufferdel = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferdel, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_enterls:
			cmdkeyboard.setText(cmdkeyboard.getText() + " ");
			Spannable cmdbufferenls = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferenls, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		
		case R.id.key_1:
			cmdkeyboard.setText(cmdkeyboard.getText() + "1");
			Spannable cmdbufferone = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferone, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_2:
			cmdkeyboard.setText(cmdkeyboard.getText() + "2");
			Spannable cmdbuffertwo = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffertwo, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_3:
			cmdkeyboard.setText(cmdkeyboard.getText() + "3");
			Spannable cmdbufferthree = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferthree, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		
		case R.id.key_4:
			cmdkeyboard.setText(cmdkeyboard.getText() + "4");
			Spannable cmdbufferfour = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferfour, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_5:
			cmdkeyboard.setText(cmdkeyboard.getText() + "5");
			Spannable cmdbufferfive = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferfive, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_6:
			cmdkeyboard.setText(cmdkeyboard.getText() + "6");
			Spannable cmdbuffersix = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffersix, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_7:
			cmdkeyboard.setText(cmdkeyboard.getText() + "7");
			Spannable cmdbufferseven = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferseven, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_8:
			cmdkeyboard.setText(cmdkeyboard.getText() + "8");
			Spannable cmdbuffereight = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffereight, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_9:
			cmdkeyboard.setText(cmdkeyboard.getText() + "9");
			Spannable cmdbuffernine = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbuffernine, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.key_0:
			cmdkeyboard.setText(cmdkeyboard.getText() + "0");
			Spannable cmdbufferten = (Spannable)cmdkeyboard.getText();
			Selection.moveToRightEdge(cmdbufferten, cmdkeyboard.getLayout());
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		
		
		
		case R.id.key_bs:		
			
			CharSequence text = cmdkeyboard.getText();
			if (!(text instanceof Editable)) {
		          cmdkeyboard.setText(text, BufferType.EDITABLE);
		        }
		        Editable editable = (Editable)cmdkeyboard.getText();

		        if (text.length() > 0) {
		          editable.delete(text.length() - 1, text.length());
		        }
			
		        if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 

		break;
		
		case R.id.key_en:
			EditText cmdenterx = (EditText)findViewById(R.id.cmdText);
			sendData.push(cmdenterx.getText() + "\r\n");
			addText(cmdenterx.getText() + "\n", Color.WHITE, Color.BLACK);

			historypos = 0;

			if(commandHistory.size() > 1)
			{
				if(!(cmdenterx.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
				{
					commandHistory.add(cmdenterx.getText().toString());
					if(commandHistory.size() > HISTORY_BUFFER_SIZE)
					{
						commandHistory.remove(0);
					}
				}
			}
			else
			{
				commandHistory.add(cmdenterx.getText().toString());
			}
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
			cmdenterx.setText("");
		break;
		
		
		case R.id.north_button:
			EditText cmd = (EditText)findViewById(R.id.cmdText);
			cmd.setText("north");
			sendData.push(cmd.getText() + "\r\n");
			addText(cmd.getText() + "\n", Color.WHITE, Color.BLACK);
			cmd.setText("");
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.east_button:
			EditText cmde = (EditText)findViewById(R.id.cmdText);
			cmde.setText("east");
			sendData.push(cmde.getText() + "\r\n");
			addText(cmde.getText() + "\n", Color.WHITE, Color.BLACK);
			cmde.setText("");
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;

		case R.id.west_button:
		EditText cmdw = (EditText)findViewById(R.id.cmdText);
		cmdw.setText("west");
		sendData.push(cmdw.getText() + "\r\n");
		addText(cmdw.getText() + "\n", Color.WHITE, Color.BLACK);
		cmdw.setText("");
		if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;

		case R.id.south_button:
			EditText cmds = (EditText)findViewById(R.id.cmdText);
			cmds.setText("south");
			sendData.push(cmds.getText() + "\r\n");
			addText(cmds.getText() + "\n", Color.WHITE, Color.BLACK);
			cmds.setText("");
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.up_button:
			EditText cmdup = (EditText)findViewById(R.id.cmdText);
			cmdup.setText("up");
			sendData.push(cmdup.getText() + "\r\n");
			addText(cmdup.getText() + "\n", Color.WHITE, Color.BLACK);
			cmdup.setText("");
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.down_button:
			EditText cmddown = (EditText)findViewById(R.id.cmdText);
			cmddown.setText("down");
			sendData.push(cmddown.getText() + "\r\n");
			addText(cmddown.getText() + "\n", Color.WHITE, Color.BLACK);
			cmddown.setText("");
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.look_button:
			EditText cmdlook = (EditText)findViewById(R.id.cmdText);
			cmdlook.setText("look");
			sendData.push(cmdlook.getText() + "\r\n");
			addText(cmdlook.getText() + "\n", Color.WHITE, Color.BLACK);
			cmdlook.setText("");
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.enter_button:
			EditText cmdenter = (EditText)findViewById(R.id.cmdText);
			sendData.push(cmdenter.getText() + "\r\n");
			addText(cmdenter.getText() + "\n", Color.WHITE, Color.BLACK);

			historypos = 0;

			if(commandHistory.size() > 1)
			{
				if(!(cmdenter.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
				{
					commandHistory.add(cmdenter.getText().toString());
					if(commandHistory.size() > HISTORY_BUFFER_SIZE)
					{
						commandHistory.remove(0);
					}
				}
			}
			else
			{
				commandHistory.add(cmdenter.getText().toString());
			}
			//cmdenter.setText("");
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;	
		
		case R.id.toggle_button:
	    	View cmda = (View)findViewById(R.id.CommandBox);
        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
        	View cmdnumber = (View)findViewById(R.id.NumberBox);
        	View cmdnumblock = (View)findViewById(R.id.NumblockBox);
        	View cmdspin = (View)findViewById(R.id.spinner1);
        	View cmdconfirm = (View)findViewById(R.id.button1);
        	View cmduibackbutton = (View)findViewById(R.id.uiback_button);
        	int visibilitya = cmda.getVisibility();
        	
        		if (visibilitya == 0)
        		{
        			cmda.setVisibility(4);
        			cmdkb.setVisibility(4);
        			cmdnumber.setVisibility(4);
        			cmdnumblock.setVisibility(0);
        			cmdspin.setVisibility(4);
        			cmdconfirm.setVisibility(4);
        			cmduibackbutton.setVisibility(4);
        		}
        		else
        		{
        			cmda.setVisibility(0);
        			cmdkb.setVisibility(4);
        			cmdnumber.setVisibility(4);
        			cmdnumblock.setVisibility(4);
        			cmdspin.setVisibility(4);
        			cmdconfirm.setVisibility(4);
        			cmduibackbutton.setVisibility(4);
        		}
        		
        		if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;
		
		case R.id.uiback_button:

			openCommonDialog();
        		
        		if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
		break;


			case R.id.command_button:
			openCommonDialog();
        	View cmduibackbuttona = (View)findViewById(R.id.uiback_button);
        	
        	cmduibackbuttona.setVisibility(0);
        	
			if (Prefs.getVibration(getBaseContext())) vb.vibrate( new long[]{0,35,0,0}, -1 ); 
			break;

		}
		}
 
	
	//private static final String TAG = "Dungeoneers" ;
	/** Ask the user what difficulty level they want */
	private void openNewGameDialog() {
		
		new AlertDialog.Builder(this)
		.setMessage("Are you sure you want to exit?")
		.setCancelable(false)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   Dungeoneers.this.finish();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       }).show();
		
	}

	
	private void openCommonDialog() {
			
    	spinner1 = (Spinner) 
     	findViewById  (R.id.spinner1);  
    	
    	button1    = (Button)
    	findViewById (R.id.button1);                                               
        
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraycategories);        
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
    	spinner1.setAdapter(adapter);         
    	button1.setOnClickListener(  new clickercategories()); 
    	
    	View cmda = (View)findViewById(R.id.CommandBox);
    	View cmdkb = (View)findViewById(R.id.KeyboardBox);
    	View cmdnum = (View)findViewById(R.id.NumberBox);
    	View cmdspin = (View)findViewById(R.id.spinner1);
    	View cmdconfirm = (View)findViewById(R.id.button1);

    	cmda.setVisibility(4);
    	cmdkb.setVisibility(4);
    	cmdnum.setVisibility(4);
    	cmdspin.setVisibility(0);
    	cmdconfirm.setVisibility(0);
		
		}
	
		/** Start a new game with the given difficulty level */
		private void startCommand(int i) {
		//Log.d(TAG, "clicked on " + i);
			if (i == 0) {
				
					new AlertDialog.Builder(this)
					.setTitle("Info Commands")
					.setItems(R.array.infocommands,
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialoginterface,
					int i) {
					startCommanda(i);
					}
					})
					.show();			
					
			}
			else if (i == 1) {

				new AlertDialog.Builder(this)
				.setTitle("Node Commands")
				.setItems(R.array.nodescommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandb(i);
				}
				})
				.show();
				
			}
			else if (i == 2) {
				new AlertDialog.Builder(this)
				.setTitle("Talk Commands")
				.setItems(R.array.talkcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandc(i);
				}
				})
				.show();
			}
			else if (i == 3) {
				new AlertDialog.Builder(this)
				.setTitle("Combat Commands")
				.setItems(R.array.combatcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandcombat(i);
				}
				})
				.show();
			}
			else if (i == 4) {
				new AlertDialog.Builder(this)
				.setTitle("Travel Commands")
				.setItems(R.array.travelcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandtravel(i);
				}
				})
				.show();
			}	
			else if (i == 5) {
				new AlertDialog.Builder(this)
				.setTitle("Construct Commands")
				.setItems(R.array.constructcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandconstruct(i);
				}
				})
				.show();
			}	
			else if (i == 6) {
				new AlertDialog.Builder(this)
				.setTitle("Interact Commands")
				.setItems(R.array.interactcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandinteract(i);
				}
				})
				.show();
			}	
			else if (i == 7) {
				new AlertDialog.Builder(this)
				.setTitle("Shop Commands")
				.setItems(R.array.shopcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandshop(i);
				}
				})
				.show();
			}	
			else if (i == 8) {
				new AlertDialog.Builder(this)
				.setTitle("Bank Commands")
				.setItems(R.array.bankingcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandsbanking(i);
				}
				})
				.show();
			}	
			else if (i == 9) {
				new AlertDialog.Builder(this)
				.setTitle("Organization Commands")
				.setItems(R.array.organizationcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandsorganization(i);
				}
				})
				.show();
			}	
			else if (i == 10) {
				new AlertDialog.Builder(this)
				.setTitle("Group Commands")
				.setItems(R.array.groupcommands,
				new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialoginterface,
				int i) {
				startCommandsgroup(i);
				}
				})
				.show();
			}				
			else sendData.push(i + "\r\n");
			
			}	
		
		private void startCommanda(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("score");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("who");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 3) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("systems");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 4) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("regions");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}
				else if (i == 5) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("organizations");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}	
				else if (i == 6) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("affected");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}
				
				else if (i == 7) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("commands");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}				
				
				else if (i == 8) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("equipment");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}
				
				else if (i == 9) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("inventory");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}
				
				else if (i == 10) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("compare ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
				}
				
				else if (i == 11) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("showorg ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayorgs);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickercommand()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
						
				}
				
				else if (i == 12) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("showsystem ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraysystems);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickercommand()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
						
				}
				
				else sendData.push(i + "\r\n");
				}	
		
		private void startCommandb(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("look");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("analyze");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 2) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("search");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 3) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("dig");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}
				
				else if (i == 4) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("exits");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}				
				
				else if (i == 5) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("glance");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}
							
				else if (i == 6) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("examine ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
				}
				
				else if (i == 7) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("drop ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
				}

				else if (i == 8) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("unlock ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickerkeynum()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
						
				}
				
				else if (i == 9) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("scan ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickercommand()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
						
				}
				
				else sendData.push(i + "\r\n");
				}	

		private void startCommandc(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("say hi");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("say job");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 2) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("say ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
															
				}
				else if (i == 3) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("gchat ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					}
													
				else if (i == 4) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("schat ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());				
					}
				
				else if (i == 5) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("Afk");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				
				else if (i == 6) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bio");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				
				else if (i == 7) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("gtell ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 8) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("orgtalk ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 9) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("ooc ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else sendData.push(i + "\r\n");
				}

		private void startCommandtravel(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("home");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("enter");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 2) {
					
					/*
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("connect ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
					// add system list
					
					new AlertDialog.Builder(this)
					//.setContentView(R.layout.custom_dialog)
					.setTitle("Choose City Grid")
					.setInverseBackgroundForced(true)
					.setItems(R.array.commoncommands,
					
					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialoginterface,
					int i) {
					startCitygrids(i);
					}
					})
					.show();
					*/
										
						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("connect ", BufferType.SPANNABLE);
						Spannable cmdbuffer = (Spannable)cmdscore.getText();
						Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
											
				    	spinner1 = (Spinner) 
				     	findViewById  (R.id.spinner1);  
				    	
				    	button1    = (Button)
				    	findViewById (R.id.button1);                                               
				        
				    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraysystems);        
				    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
				    	spinner1.setAdapter(adapter);         
				    	button1.setOnClickListener(  new clickerkeynum()); 
				    	
				    	View cmda = (View)findViewById(R.id.CommandBox);
			        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
			        	View cmdnum = (View)findViewById(R.id.NumberBox);
			        	View cmdspin = (View)findViewById(R.id.spinner1);
			        	View cmdconfirm = (View)findViewById(R.id.button1);

			        	cmda.setVisibility(4);
			        	cmdkb.setVisibility(4);
			        	cmdnum.setVisibility(4);
			        	cmdspin.setVisibility(0);
			        	cmdconfirm.setVisibility(0);
					
				}
				
				else sendData.push(i + "\r\n");
				}
		
			
		
		private void startCommandcombat(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("kill virus");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				
				if (i == 1) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("kill ICE");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				
				if (i == 2) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("kill program");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				
				else if (i == 3) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("kill ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 4) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("blast ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clicker()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);

					
				}
				
				else if (i == 5) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("consider ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
				}
					
					else if (i == 6) {
						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("flee ", BufferType.SPANNABLE);
						Spannable cmdbuffer = (Spannable)cmdscore.getText();
						Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
											
				    	spinner1 = (Spinner) 
				     	findViewById  (R.id.spinner1);  
				    	
				    	button1    = (Button)
				    	findViewById (R.id.button1);                                               
				        
				    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
				    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
				    	spinner1.setAdapter(adapter);         
				    	button1.setOnClickListener(  new clickercommand()); 
				    	
				    	View cmda = (View)findViewById(R.id.CommandBox);
			        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
			        	View cmdnum = (View)findViewById(R.id.NumberBox);
			        	View cmdspin = (View)findViewById(R.id.spinner1);
			        	View cmdconfirm = (View)findViewById(R.id.button1);

			        	cmda.setVisibility(4);
			        	cmdkb.setVisibility(4);
			        	cmdnum.setVisibility(4);
			        	cmdspin.setVisibility(0);
			        	cmdconfirm.setVisibility(0);
						
					}
				
					else if (i == 7) {

						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("rest");
						sendData.push(cmdscore.getText() + "\r\n");
						addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

						historypos = 0;

						if(commandHistory.size() > 1)
						{
							if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
							{
								commandHistory.add(cmdscore.getText().toString());
								if(commandHistory.size() > HISTORY_BUFFER_SIZE)
								{
									commandHistory.remove(0);
								}
							}
						}
						else
						{
							commandHistory.add(cmdscore.getText().toString());
						}
						cmdscore.setText("");
						
					}
				
					else if (i == 8) {

						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("sleep");
						sendData.push(cmdscore.getText() + "\r\n");
						addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

						historypos = 0;

						if(commandHistory.size() > 1)
						{
							if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
							{
								commandHistory.add(cmdscore.getText().toString());
								if(commandHistory.size() > HISTORY_BUFFER_SIZE)
								{
									commandHistory.remove(0);
								}
							}
						}
						else
						{
							commandHistory.add(cmdscore.getText().toString());
						}
						cmdscore.setText("");
						
					}
					else if (i == 9) {

						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("wake");
						sendData.push(cmdscore.getText() + "\r\n");
						addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

						historypos = 0;

						if(commandHistory.size() > 1)
						{
							if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
							{
								commandHistory.add(cmdscore.getText().toString());
								if(commandHistory.size() > HISTORY_BUFFER_SIZE)
								{
									commandHistory.remove(0);
								}
							}
						}
						else
						{
							commandHistory.add(cmdscore.getText().toString());
						}
						cmdscore.setText("");
						
					}
					else if (i == 10) {

						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("sit");
						sendData.push(cmdscore.getText() + "\r\n");
						addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

						historypos = 0;

						if(commandHistory.size() > 1)
						{
							if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
							{
								commandHistory.add(cmdscore.getText().toString());
								if(commandHistory.size() > HISTORY_BUFFER_SIZE)
								{
									commandHistory.remove(0);
								}
							}
						}
						else
						{
							commandHistory.add(cmdscore.getText().toString());
						}
						cmdscore.setText("");
						
					}
				
				
					else if (i == 11) {

						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("stand");
						sendData.push(cmdscore.getText() + "\r\n");
						addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

						historypos = 0;

						if(commandHistory.size() > 1)
						{
							if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
							{
								commandHistory.add(cmdscore.getText().toString());
								if(commandHistory.size() > HISTORY_BUFFER_SIZE)
								{
									commandHistory.remove(0);
								}
							}
						}
						else
						{
							commandHistory.add(cmdscore.getText().toString());
						}
						cmdscore.setText("");
						
					}
				
					else if (i == 12) {
						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("setblaster ", BufferType.SPANNABLE);
						Spannable cmdbuffer = (Spannable)cmdscore.getText();
						Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
					}
				
					else if (i == 13) {
						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("shove ", BufferType.SPANNABLE);
						Spannable cmdbuffer = (Spannable)cmdscore.getText();
						Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
					}
				
				else sendData.push(i + "\r\n");
				}
		
		private void startCommandconstruct(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("construct ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickercommand()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
		        	
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraymodify);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickercommand()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
					
				}
				else if (i == 2) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bridge ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirsconnect);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickerkeynum()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
		        	
					
				}
				else if (i == 3) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bridge ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirsdoor);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickercommand()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
				}
				else if (i == 4) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bridge ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirskeycode);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickerkeynum()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
				}
				else if (i == 5) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("nodedescription");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
				}
				else sendData.push(i + "\r\n");
				}		
	
		private void startCommandinteract(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("give package program");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				
				else if (i == 1) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("give ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 2) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("hold ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 3) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("get ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 4) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("close ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 5) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bridge ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
										
			    	spinner1 = (Spinner) 
			     	findViewById  (R.id.spinner1);  
			    	
			    	button1    = (Button)
			    	findViewById (R.id.button1);                                               
			        
			    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraydirs);        
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);                   
			    	spinner1.setAdapter(adapter);         
			    	button1.setOnClickListener(  new clickerkeynum()); 
			    	
			    	View cmda = (View)findViewById(R.id.CommandBox);
		        	View cmdkb = (View)findViewById(R.id.KeyboardBox);
		        	View cmdnum = (View)findViewById(R.id.NumberBox);
		        	View cmdspin = (View)findViewById(R.id.spinner1);
		        	View cmdconfirm = (View)findViewById(R.id.button1);

		        	cmda.setVisibility(4);
		        	cmdkb.setVisibility(4);
		        	cmdnum.setVisibility(4);
		        	cmdspin.setVisibility(0);
		        	cmdconfirm.setVisibility(0);
					
				}
				
				else if (i == 6) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("drag ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 7) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("empty ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 8) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("fill ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 9) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("activate ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}

				else if (i == 10) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("teach ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 11) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("visible");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}				
				
				else sendData.push(i + "\r\n");
				}
		
		private void startCommandshop(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("list");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("buy ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 2) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("sell ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 3) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("repair ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 4) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("value ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 5) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("buyskill");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				
				
				
				else if (i == 6) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("buyskill ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else sendData.push(i + "\r\n");
				}
		


		private void startCommandsbanking(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bank balance");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bank deposit ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 2) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bank withdraw ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 3) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bank transfer ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
			
				else sendData.push(i + "\r\n");
				}
		

		private void startCommandsorganization(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("donate ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("demote ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 2) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("empower ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 3) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("induct ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 4) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("withdraw ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 5) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("war ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 6) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("outcast ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 7) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("outcast ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				else if (i == 8) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("enlist");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
				else if (i == 9) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("overthrow");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");
					
				}
			
				else sendData.push(i + "\r\n");
				}		

		private void startCommandsgroup(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {

					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("follow ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
					
				}
				else if (i == 1) {

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("group");
					sendData.push(cmdscore.getText() + "\r\n");
					addText(cmdscore.getText() + "\n", Color.WHITE, Color.BLACK);

					historypos = 0;

					if(commandHistory.size() > 1)
					{
						if(!(cmdscore.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
						{
							commandHistory.add(cmdscore.getText().toString());
							if(commandHistory.size() > HISTORY_BUFFER_SIZE)
							{
								commandHistory.remove(0);
							}
						}
					}
					else
					{
						commandHistory.add(cmdscore.getText().toString());
					}
					cmdscore.setText("");

					
				}
				else sendData.push(i + "\r\n");
				}
		
		
    public void errormessage(String text)
    {
    	addText(text,Color.RED,Color.BLUE);
    }
    
    public void addText(String text, int color, int bgcolor)
    {

    	TextView textview = (TextView)findViewById(R.id.MainText);
    	
    	if(!scrolllocked)
    	{
    		int viewBufferSize = viewBufferFull.size();
    		int viewBufferLines = textview.getLineCount();
    		
    		if(viewBufferLines > MAX_TEXT_LINES)
    		{
	    		int x = 0;
	    		int lines = 0;
	    		for(x =0; (x<viewBufferSize) && (lines <= viewBufferLines - (MAX_TEXT_LINES - LINE_REMOVAL_AMOUNT));x++)
	    		{
	    			if(viewBufferFull.get(x).contains("\n"))
	    			{
	    				lines++;
	    			}
	    		}
	    		
				viewBufferFull = viewBufferFull.subList(x, viewBufferFull.size() - 1);
				viewBufferFullColor = viewBufferFullColor.subList(x, viewBufferFullColor.size() - 1);
				viewBufferFullBGColor = viewBufferFullBGColor.subList(x, viewBufferFullBGColor.size() - 1);
				refreshView();
				
				textview.scrollTo(0, (textview.getLineCount() * textview.getLineHeight()) - textview.getHeight());
    		}
    	}
    	
		viewBufferFull.add(text);
		viewBufferFullColor.add(color);
		viewBufferFullBGColor.add(bgcolor);
		
    	SpannableString viewSpanBuffer= new SpannableString(text);
    	if(color != Color.WHITE)
    		viewSpanBuffer.setSpan(new ForegroundColorSpan(color), 0, viewSpanBuffer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    	if(bgcolor != Color.BLACK)
    		viewSpanBuffer.setSpan(new BackgroundColorSpan(bgcolor), 0, viewSpanBuffer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    		
        textview.append(viewSpanBuffer);

		
    }
    
    public void refreshView()
    {
    	TextView textview = (TextView)findViewById(R.id.MainText);

    	textview.setText("");
    	
    	for(int x = 0; x < viewBufferFull.size();x++)
    	{
        	SpannableString viewSpanBuffer= new SpannableString(viewBufferFull.get(x));
        	if(viewBufferFullColor.get(x) != Color.WHITE)
        		viewSpanBuffer.setSpan(new ForegroundColorSpan(viewBufferFullColor.get(x)), 0, viewSpanBuffer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	if(viewBufferFullBGColor.get(x) != Color.BLACK)
        		viewSpanBuffer.setSpan(new BackgroundColorSpan(viewBufferFullBGColor.get(x)), 0, viewSpanBuffer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	textview.append(viewSpanBuffer);
    	}
    	
    	new Thread(){
    		public void run()
    		{
    			try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	       	Message m = new Message();
    	       	m.what = TelnetConnectionThread.TEXT_SCROLL;
    			TCUpdateHandler.sendMessage(m);
    		}
    	};
		textview.scrollTo(0, (textview.getLineCount() * textview.getLineHeight()) - textview.getHeight());
        

    }
    
    public Handler TCUpdateHandler = new Handler(){
    
    	public void handleMessage(Message msg){
    		switch(msg.what)
    		{
    		case TelnetConnectionThread.TEXT_UPDATE:
				{
					addText(msg.getData().getString("text"),msg.getData().getInt("color"),msg.getData().getInt("bgcolor"));
					TextView textview = (TextView)findViewById(R.id.MainText);
					
					if(!scrolllocked)
						textview.scrollTo(0, (textview.getLineCount() * textview.getLineHeight()) - textview.getHeight());
					
					break;
				}
    		case TelnetConnectionThread.TEXT_SENT:
				{
					break;
				}
    		case TelnetConnectionThread.TEXT_SCROLL:
    			{
					TextView textview = (TextView)findViewById(R.id.MainText);
					textview.scrollTo(0, (textview.getLineCount() * textview.getLineHeight()) - textview.getHeight());
    				break;
    			}
    		default:
    			break;
    		}
            super.handleMessage(msg); 
    	}
    };
	
   
	public class ServerInfo{
		String ServerName;
		String IP;
		int Port;
		
		public ServerInfo(String name, String ip, int port)
		{
			ServerName = name;
			IP = ip;
			Port = port;
		}
		
		@Override
		public String toString()
		{
			return ServerName;
		}
	}

	private class ServerAddListener implements ConnectionDialog.ServerAdd {


		//@Override
		public void Add(String Name,String H, int p) {
			// TODO Auto-generated method stub
			
			ServerListing.add(new ServerInfo(Name,H,p));

        	ConnectReady cready = new OnReadyListener();
        	ServerListDialog dialog = new ServerListDialog(Dungeoneers.this, cready, ServerListing);
            dialog.show();
		}

		//@Override
		public void Modify(int pos, String name, String H, int p) {
			// TODO Auto-generated method stub

			ServerListing.get(pos).ServerName = name;
			ServerListing.get(pos).IP = H;
			ServerListing.get(pos).Port = p;

        	ConnectReady cready = new OnReadyListener();
        	ServerListDialog dialog = new ServerListDialog(Dungeoneers.this, cready, ServerListing);
            dialog.show();
		}

   }
	
}
