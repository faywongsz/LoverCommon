package com.yuning.game;

import com.yuning.util.sendThread;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class GameService extends Service {
	private static final String TAG = GameService.class.getSimpleName();
	private static final String ACTION_GAME = "com.yuning.game.GAME";
	private static final String EXTRA_LEVEL = "game_level";

	private byte[] modeCMD = {0x02, 0x00};
	private byte[] stopCMD = {(byte) 0xff,(byte)0xff};	   
	
	private BroadcastReceiver mRecevier = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(ACTION_GAME)) {
				int level = intent.getIntExtra(EXTRA_LEVEL, 0);
				modeCMD[0] = 0x02;
				modeCMD[1] = (byte)level;
				Log.d(TAG, "level = " + level);
				
				sendThread.sendData(modeCMD);
			}
		}
		
	};
	@Override
	public void onCreate() {
		super.onCreate();
		IntentFilter filter = new IntentFilter(ACTION_GAME);
		registerReceiver(mRecevier, filter);
		Log.d(TAG, "onCreate");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mRecevier);
		sendThread.sendData(stopCMD);
		Log.d(TAG, "onDestroy");
	}
}
