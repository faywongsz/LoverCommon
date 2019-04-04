package com.yuning.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;
import com.yuning.util.ShakeListener;
import com.yuning.util.sendThread;
import com.yuning.view.ShakeProgressBar;


public class yaoyiyaomode extends BaseActivity{
	private static final String TAG = "yaoyiyaomode";

	byte[] mStop = {(byte) 0xff,(byte)0xff};
	
    ShakeListener mShaker;
	private WakeLock mWakeLock = null;
	
	private long lastUpdateTime; 
	private long lastUpdateContentImgTime;
	private static final long UPTATE_INTERVAL_TIME = 100L; 	
	private static final long UPDATE_CONTENT_IMG_TIME = 300L;
	private static final int MAX_LEVEL = 25;
	private ShakeProgressBar mProgress;
	private ImageView backIcon;
	private ImageView mContentImg;
	private int mLastLevel;
	private int mCurrentDisplayLevel = 0;
	private int mCurrentZhendongLevel = 0;
	private int mZeroLevelCount = 0;
	
	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		setContentView(R.layout.yaoyiyao_mode);
		backIcon = (ImageView)findViewById(R.id.back_icon);
		backIcon.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				mShaker.stop();
				finish();
				return;
			}
		});
		mContentImg = (ImageView)findViewById(R.id.yaoyiyao_content_img);
        mProgress = (ShakeProgressBar)findViewById(R.id.progress);
        mShaker = new ShakeListener(this); 
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {  
            public void onShake(int level) {
    		    long currentUpdateTime = SystemClock.elapsedRealtime();  
    		    long timeInterval = currentUpdateTime - lastUpdateTime; 
    		    if(timeInterval < UPTATE_INTERVAL_TIME) {  
    		         return;  
    		    }
    		    lastUpdateTime = currentUpdateTime;
    		    if(level == mLastLevel){
    		    	return;
    		    }
    		  //modify by guoliangliang 20150313 begin
    		    if(level == 0){
    		    	mZeroLevelCount++;
    		    	if(mZeroLevelCount == 3){
    					Log.d(TAG,"mZeroLevelCount = 3"+"  currentUpdateTime="+currentUpdateTime);
    		    		mZeroLevelCount = 0;
    					mCurrentZhendongLevel = 0;
    					mCurrentDisplayLevel = 0;
    					mLastLevel = 0;
    		    	}else{
    		    		getCurrentLevelByInputLevel(level);
    		    	}
    		    }else{
    		    	mZeroLevelCount = 0;
    		    	getCurrentLevelByInputLevel(level);
    		    }
    		    //getCurrentLevelByInputLevel(level);
    		    //modify by guoliangliang 20150313 end
    		    Log.d(TAG, "level=" + level + "  mZeroLevelCount=" + mZeroLevelCount + "  mLastLevel=" + mLastLevel + "  mCurrentDisplayLevel=" + mCurrentDisplayLevel + "  mCurrentZhendongLevel=" + mCurrentZhendongLevel + "  currentUpdateTime=" + currentUpdateTime); 
				mProgress.updateLevel(mCurrentDisplayLevel);
				timeInterval = currentUpdateTime - lastUpdateContentImgTime;
				if(level != 0 && timeInterval >= UPDATE_CONTENT_IMG_TIME){
					mContentImg.setPressed(!mContentImg.isPressed());
					lastUpdateContentImgTime = currentUpdateTime;
				}
    			    byte[] modeCMD = new byte[2];
    				modeCMD[0] = 0x02;
    				modeCMD[1] = (byte)mCurrentZhendongLevel;
    				sendThread.sendData(modeCMD);
				
            } 
            	
        });  
		mShaker.stop();		
	}
	
	private void getCurrentLevelByInputLevel(int level){
		if(level >= 30){
			mCurrentDisplayLevel = MAX_LEVEL;
			mCurrentZhendongLevel = MAX_LEVEL;
			mLastLevel = 50;
		}else{
			if(level > mLastLevel){
				mLastLevel = level;
				mCurrentDisplayLevel = mLastLevel / 2;
				mCurrentZhendongLevel = mLastLevel / 2;
			}else{
				mLastLevel -= 5;
				if(mLastLevel < 0){
					mLastLevel = 0;
				}
				mCurrentDisplayLevel = mLastLevel / 2;
				mCurrentZhendongLevel = mLastLevel / 2;
			}
		}
	}
	
	public void onResume()
	{
		super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
		
		if(!isConnect()){
			setResult(Activity.DEFAULT_KEYS_SEARCH_LOCAL, null);			
			Toast.makeText(this, getString(R.string.msg_please_connect), 0).show();				
			finish();
			return;
		}	
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
			mWakeLock.setReferenceCounted(false);
			mWakeLock.acquire();
		}	
		mShaker.start();
		//mButton.setText(R.string.yaoyiyao_start);
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);		
		
		//modified by wangfei
		sendThread.sendData(mStop);
		//sendData(mStop);	
		
		//handle.removeMessages(HANDLE_SEND_DATA);
		//handle.removeMessages(HANDLE_STOP);
		mShaker.stop();
		
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
			mWakeLock = null;
		}		
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		return false;
	}

	public boolean onKeyDown(int KeyCode, KeyEvent keyevent)
	{
		if (KeyCode == KeyEvent.KEYCODE_BACK)
		{
			mShaker.stop();
			setResult(Activity.DEFAULT_KEYS_SEARCH_LOCAL, null);
			finish();
		}
		return super.onKeyDown(KeyCode, keyevent);
	}
}

