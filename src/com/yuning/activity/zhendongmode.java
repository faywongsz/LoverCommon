package com.yuning.activity;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

public class zhendongmode extends BaseActivity{
	private  String zhendong[];
	private Button[] mbtns;
	private static final String mCMDstart= "pwm|";
	private static final String mCMDstop= "pwm|stop"; 
	
	private void sendMsg(String s)
	{
		//if (SendData(s.getBytes()) >= 0){
			
		//}
	}

	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		setContentView(R.layout.zhendong_mode);
		mbtns = new Button[7];
		mbtns[0] = (Button)findViewById(R.id.btn_keyboard_1);
		mbtns[1] = (Button)findViewById(R.id.btn_keyboard_2);
		mbtns[2] = (Button)findViewById(R.id.btn_keyboard_3);
		mbtns[3] = (Button)findViewById(R.id.btn_keyboard_4);
		mbtns[4] = (Button)findViewById(R.id.btn_keyboard_5);
		mbtns[5] = (Button)findViewById(R.id.btn_keyboard_6);
		mbtns[6] = (Button)findViewById(R.id.btn_keyboard_7);		
		zhendong = new String[6];
		zhendong[0] = "25;2000;0;500;";
		zhendong[1] = "20;2000;0;500;";
		zhendong[2] = "18;2000;0;500";		
		zhendong[3] = "15;2000;0;500";
		zhendong[4] = "10;2000;0;500";	
		zhendong[5] = "6;2000;0;500";			
		
	}
	
	public void onResume()
	{
		super.onResume();
		MobclickAgent.onPageStart( "zhendongmode" );
		MobclickAgent.onResume(this);
		
		if(!isConnect()){
			setResult(Activity.DEFAULT_KEYS_SEARCH_LOCAL, null);			
			Toast.makeText(this, getString(R.string.msg_please_connect), 0).show();				
			finish();
			return;
		}
	}
	
	public void onPause(){
		sendMsg(mCMDstop);
		super.onPause();
		MobclickAgent.onPageEnd( "zhendongmode" );
		MobclickAgent.onPause(this);
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{			
		return false;
	}

	public boolean onKeyDown(int KeyCode, KeyEvent keyevent)
	{
		if (KeyCode == KeyEvent.KEYCODE_BACK)
		{
			setResult(Activity.DEFAULT_KEYS_SEARCH_LOCAL, null);
			finish();
		}
		return super.onKeyDown(KeyCode, keyevent);
	}
	
	
    public void	onBtnStart(View view){

	}
    
    public void	onBtnClick_Array(View view){
		int id = view.getId();
		
		for (int i = 0; i < 7; i++){
			if (mbtns[i].getId() == id){
				if(i == 6){
					sendMsg(mCMDstop);
				}else{
					sendMsg(mCMDstart + zhendong[i]);					
				}
			    break;
			}
		}
		
	}    
}
