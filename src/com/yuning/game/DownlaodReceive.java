package com.yuning.game;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jayqqaa12.abase.kit.network.NetworkKit;
import com.yuning.game.engine.DownloadEngine;

public class DownlaodReceive extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		
		if(NetworkKit.isConnectingToInternet()){
			DownloadEngine.me.remuseAllTask();
		}
		
	}

}
