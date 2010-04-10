package net.totalmadownage.nethak;

import java.util.ArrayList;

import net.totalmadownage.nethak.Dungeoneers.ServerInfo;
import net.totalmadownage.nethak.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ServerListDialog extends Dialog {

	ConnectReady cready;
	ArrayList<ServerInfo> itemlist;
	int contextSelect = 0;

	
	
	    
	public ServerListDialog(Context context, ConnectReady cfunc, ArrayList<ServerInfo> sinfo) {
		super(context);
		this.cready = cfunc;
		itemlist = sinfo;
	}
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        
		setContentView(R.layout.serverlist);
		setTitle("Choose A Server");
		
		ListView list = (ListView)findViewById(R.id.ListView01);

		list.setAdapter(new ArrayAdapter<ServerInfo>(this.getContext(), R.layout.test, itemlist));
		list.setTextFilterEnabled(true);

		list.setOnItemClickListener(new ItemSelectedListener());
		//list.setOnItemLongClickListener(new ItemLongSelectedListener());
		
		//setupAlerts();
	}

	public interface ConnectReady{
			public void connect(int pos);
			public void modify(int pos);
			public void delete(int pos);
	}
	
	private class ItemSelectedListener implements OnItemClickListener
	{

		//@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

			cready.connect(position);
			
			ServerListDialog.this.dismiss();
			
		}
	}
	
	public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.setHeaderTitle("Primary context");
	}
	
}
