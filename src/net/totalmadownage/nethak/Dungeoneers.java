package net.totalmadownage.nethak;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.text.Editable;
import android.text.Spannable;
import android.text.Selection;
import android.text.SpannableString;
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
	protected PowerManager.WakeLock mWakeLock;
	

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
	int Port = 23;
	private SendStack sendData;
	private ThreadListener tlisten;
	//private SendStack recvData = new SendStack(50);
	
	//Selection cmdbuftext;
	
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
		//sendData.push("quit\r\n");
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
					//Dungeoneers.this.finish();
					//return false;
			    	//SavePrefs();
					sendData.push("quit\r\n");
					Dungeoneers.this.finish();
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
             
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        //setContentView(R.layout.splashscreen);
        setContentView(R.layout.main);
        
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE); 
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag"); 
        this.mWakeLock.acquire();
        
        EditText cmd = (EditText)findViewById(R.id.cmdText);
        TextView textview = (TextView)findViewById(R.id.MainText);
        Selection cmdbuftext;
        
		//tlisten = new ThreadListener();
        //connectionThread = new Thread(new TelnetConnectionThread(Hostname,Port,sendData,tlisten));
        //connectionThread.start(); 

        
        //ServerListing = new ArrayList<ServerInfo>();
        
        //LoadPrefs();
        
        if(savedInstanceState == null)
        {
        
        	viewBufferFull= new ArrayList<String>();
        	commandHistory= new ArrayList<String>();
        	viewBufferFullColor= new ArrayList<Integer>();
        	viewBufferFullBGColor= new ArrayList<Integer>();

        	scrolllocked = false;

            if(connectionThread == null)
            {
            
            	//ConnectReady cready = new OnReadyListener();
            	//ServerListDialog dialog = new ServerListDialog(this, cready, ServerListing);
                //dialog.show();
            	
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
        
    }
    
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.north_button:
			EditText cmd = (EditText)findViewById(R.id.cmdText);
			cmd.setText("north");
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
		break;
		
		case R.id.east_button:
			EditText cmde = (EditText)findViewById(R.id.cmdText);
			cmde.setText("east");
			sendData.push(cmde.getText() + "\r\n");
			addText(cmde.getText() + "\n", Color.WHITE, Color.BLACK);

			historypos = 0;

			if(commandHistory.size() > 1)
			{
				if(!(cmde.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
				{
					commandHistory.add(cmde.getText().toString());
					if(commandHistory.size() > HISTORY_BUFFER_SIZE)
					{
						commandHistory.remove(0);
					}
				}
			}
			else
			{
				commandHistory.add(cmde.getText().toString());
			}
			cmde.setText("");
		break;

		case R.id.west_button:
		EditText cmdw = (EditText)findViewById(R.id.cmdText);
		cmdw.setText("west");
		sendData.push(cmdw.getText() + "\r\n");
		addText(cmdw.getText() + "\n", Color.WHITE, Color.BLACK);

		historypos = 0;

		if(commandHistory.size() > 1)
		{
			if(!(cmdw.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
			{
				commandHistory.add(cmdw.getText().toString());
				if(commandHistory.size() > HISTORY_BUFFER_SIZE)
				{
					commandHistory.remove(0);
				}
			}
		}
		else
		{
			commandHistory.add(cmdw.getText().toString());
		}
		cmdw.setText("");		
		break;

		case R.id.south_button:
			EditText cmds = (EditText)findViewById(R.id.cmdText);
			cmds.setText("south");
			sendData.push(cmds.getText() + "\r\n");
			addText(cmds.getText() + "\n", Color.WHITE, Color.BLACK);

			historypos = 0;

			if(commandHistory.size() > 1)
			{
				if(!(cmds.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
				{
					commandHistory.add(cmds.getText().toString());
					if(commandHistory.size() > HISTORY_BUFFER_SIZE)
					{
						commandHistory.remove(0);
					}
				}
			}
			else
			{
				commandHistory.add(cmds.getText().toString());
			}
			cmds.setText("");
		break;
		
		case R.id.up_button:
			EditText cmdup = (EditText)findViewById(R.id.cmdText);
			cmdup.setText("up");
			sendData.push(cmdup.getText() + "\r\n");
			addText(cmdup.getText() + "\n", Color.WHITE, Color.BLACK);

			historypos = 0;

			if(commandHistory.size() > 1)
			{
				if(!(cmdup.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
				{
					commandHistory.add(cmdup.getText().toString());
					if(commandHistory.size() > HISTORY_BUFFER_SIZE)
					{
						commandHistory.remove(0);
					}
				}
			}
			else
			{
				commandHistory.add(cmdup.getText().toString());
			}
			cmdup.setText("");
		break;
		
		case R.id.down_button:
			EditText cmddown = (EditText)findViewById(R.id.cmdText);
			cmddown.setText("down");
			sendData.push(cmddown.getText() + "\r\n");
			addText(cmddown.getText() + "\n", Color.WHITE, Color.BLACK);

			historypos = 0;

			if(commandHistory.size() > 1)
			{
				if(!(cmddown.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
				{
					commandHistory.add(cmddown.getText().toString());
					if(commandHistory.size() > HISTORY_BUFFER_SIZE)
					{
						commandHistory.remove(0);
					}
				}
			}
			else
			{
				commandHistory.add(cmddown.getText().toString());
			}
			cmddown.setText("");
		break;
		
		case R.id.look_button:
			EditText cmdlook = (EditText)findViewById(R.id.cmdText);
			cmdlook.setText("look");
			sendData.push(cmdlook.getText() + "\r\n");
			addText(cmdlook.getText() + "\n", Color.WHITE, Color.BLACK);

			historypos = 0;

			if(commandHistory.size() > 1)
			{
				if(!(cmdlook.getText().toString().compareTo(commandHistory.get(commandHistory.size()-1)) == 0))
				{
					commandHistory.add(cmdlook.getText().toString());
					if(commandHistory.size() > HISTORY_BUFFER_SIZE)
					{
						commandHistory.remove(0);
					}
				}
			}
			else
			{
				commandHistory.add(cmdlook.getText().toString());
			}
			cmdlook.setText("");
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
		break;	


			case R.id.command_button:
			openCommonDialog();
			break;

		}
		}
 
	
	//private static final String TAG = "Dungeoneers" ;
	/** Ask the user what difficulty level they want */
	private void openNewGameDialog() {
	new AlertDialog.Builder(this)
	.setTitle("Numbers")
	.setItems(R.array.numbers,
	new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialoginterface,
	int i) {
	startGame(i);
	}
	})
	.show();
	}
	/** Start a new game with the given difficulty level */
	private void startGame(int i) {
	//Log.d(TAG, "clicked on " + i);
	
		sendData.push(i + "\r\n");
	}	
	
	private void openCommonDialog() {
		new AlertDialog.Builder(this)
		.setTitle("Choose category")
		.setItems(R.array.commoncommands,
		new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialoginterface,
		int i) {
		startCommand(i);
		}
		})
		.show();
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
				else if (i == 2) {
					
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
				else if (i == 3) {
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
				else if (i == 4) {
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
				else if (i == 5) {
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
				
				else if (i == 6) {
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
				
				else if (i == 7) {
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
				
				else if (i == 8) {
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
				
				else if (i == 9) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("compare ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
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
					cmdscore.setText("glance ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
				}
				
				else if (i == 7) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("examine ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
				}
				
				else if (i == 8) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("drop ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
				}
				
				else if (i == 9) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("bury ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
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

					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("connect turing 34");
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
					cmdscore.setText("connect moderns 60");
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
					cmdscore.setText("connect moderns 86");
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
					cmdscore.setText("connect turing");
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
					cmdscore.setText("connect moderns");
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
					cmdscore.setText("connect chatsubo");
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
					cmdscore.setText("connect turing ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 9) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("connect moderns ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 10) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("connect chatsubo ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 11) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("connect ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else sendData.push(i + "\r\n");
				}
		
		
		
		
		private void startCommandcombat(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
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
				
				else if (i == 1) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("kill ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 2) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("blast ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
				}
				
				else if (i == 3) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("consider ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
				}
					
					else if (i == 4) {
						EditText cmdscore = (EditText)findViewById(R.id.cmdText);
						cmdscore.setText("flee ", BufferType.SPANNABLE);
						Spannable cmdbuffer = (Spannable)cmdscore.getText();
						Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
						
					}
									
				else sendData.push(i + "\r\n");
				}
		
		private void startCommandconstruct(int i) {
			//Log.d(TAG, "clicked on " + i);
				if (i == 0) {
					
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("construct north");
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
					cmdscore.setText("construct east");
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
					cmdscore.setText("construct south");
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
					cmdscore.setText("construct west");
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
					cmdscore.setText("construct up");
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
					cmdscore.setText("construct down");
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
					cmdscore.setText("modify terminal");
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
					cmdscore.setText("modify database");
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
					cmdscore.setText("modify inside");
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
					cmdscore.setText("modify subserver");
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
					cmdscore.setText("modify datamine");
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
					cmdscore.setText("modify info");
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
					cmdscore.setText("modify mail");
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
				else if (i == 13) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify bank");
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
				else if (i == 14) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify logout");
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
				else if (i == 15) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify trade");
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
				else if (i == 16) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify supply");
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
				else if (i == 17) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify pawn");
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
				else if (i == 18) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify firewall");
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
				else if (i == 19) {
					EditText cmdscore = (EditText)findViewById(R.id.cmdText);
					cmdscore.setText("modify employment");
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
					cmdscore.setText("give package ice");
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
					cmdscore.setText("lock ", BufferType.SPANNABLE);
					Spannable cmdbuffer = (Spannable)cmdscore.getText();
					Selection.moveToRightEdge(cmdbuffer, cmdscore.getLayout());
					
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