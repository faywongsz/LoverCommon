package com.yuning.activity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.yuning.Service.voicesensorservice;
import com.yuning.lovercommon.R;
import com.yuning.util.RealDoubleFFT;

public class VoicectrlYuanchengMode extends BaseActivity{
	private static final String TAG = "VoicectrlYuanchengMode";	
	byte[] mStop = {(byte) 0xff,(byte)0xff};
    private WakeLock mWakeLock = null;
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private RealDoubleFFT transformer;
	int blockSize = 256;
	private ImageView backIcon;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG,"onCreate");
		setContentView(R.layout.voicectrl_yuancheng_mode);

		transformer = new RealDoubleFFT(blockSize);

		backIcon = (ImageView)findViewById(R.id.back_icon);
		backIcon.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				finish();
				return;
			}
		});
		 /*wangfei added begin*/
		findViewById(R.id.voice_ctr_weixin_icon).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setClassName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				startSecureActivity(intent);
			}
		});
		findViewById(R.id.voice_ctr_qq_icon).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setClassName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.SplashActivity");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				startSecureActivity(intent);
			}
		});
		findViewById(R.id.voice_ctr_call_icon).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_DIAL);
				startSecureActivity(intent);
			}
		});
		 /*wangfei added end*/
		startYuanchengControl();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG,"onResume");
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
			mWakeLock.setReferenceCounted(false);
			mWakeLock.acquire();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG,"onPause");
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG,"onDestroy");
		unbindService(mConn);
	}
	
	private void startYuanchengControl(){
		Intent service = new Intent(VoicectrlYuanchengMode.this,voicesensorservice.class);
		VoicectrlYuanchengMode.this.bindService(service, mConn, Context.BIND_AUTO_CREATE);
	}

    private ServiceConnection mConn = new ServiceConnection() {
        
        @Override
        public void onServiceDisconnected(ComponentName name) {}
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {}
    };	
     
    @Override    
    protected void response(String data) {
    	super.response(data);
        if (data != null) {
            Log.d(TAG,"current data:" + data);         
        }
    }    
    
    /*wangfei added begin*/
    private void startSecureActivity(Intent intent) {
    	try {
    		startActivity(intent);
    	}catch(ActivityNotFoundException e) {
    		Toast.makeText(this, R.string.app_not_installed, Toast.LENGTH_SHORT).show();
    	}
    }
    /*wangfei added end*/
	
}
