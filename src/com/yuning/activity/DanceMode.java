package com.yuning.activity;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;
import com.yuning.util.ShakeListener;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;


public class DanceMode extends BaseModeActivity{
	private static final String TAG = DanceMode.class.getSimpleName();

    ShakeListener mShaker;
	
    private ImageView mImageLevel;
	private long lastUpdateShakeTime; 
	private static final long UPDATE_INTERVAL_TIME = 100L;
	private static final int MAX_LEVEL = 25;
	private static final int MAX_IMAGE_LEVEL = 9;
	private int mLastLevel;
	private int mCurrentZhendongLevel = 0;
	private int mZeroLevelCount = 0;
	
	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		setContentView(R.layout.dance_mode);
		initTitleBar(R.string.dance_mode, 0, 0, null);
		
		mImageLevel = (ImageView) findViewById(R.id.dance_mode_level);
		mImageLevel.setImageLevel(0);
		ImageView animation = (ImageView) findViewById(R.id.dance_mode_animation);
		AnimationDrawable animationDrawable = (AnimationDrawable) animation.getDrawable();
		animationDrawable.start();
        
        mShaker = new ShakeListener(this); 
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {  
        	//int mPwmLevel = 0;
            public void onShake(int level) {
    		    long currentUpdateTime = SystemClock.elapsedRealtime();  
            	//Log.d(TAG,"onShake()  currentUpdateTime = "+currentUpdateTime);
    		    long timeInterval  = currentUpdateTime - lastUpdateShakeTime; 
    		    if(timeInterval < UPDATE_INTERVAL_TIME) {  
    		         return;  
    		    }
    		    lastUpdateShakeTime = currentUpdateTime;
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
    		    Log.d(TAG, "level=" + level + "  mZeroLevelCount=" + mZeroLevelCount + "  mLastLevel=" + mLastLevel + "  mCurrentZhendongLevel=" + mCurrentZhendongLevel + "  currentUpdateTime=" + currentUpdateTime); 
                setFunLevelMode(mCurrentZhendongLevel);
                mImageLevel.setImageLevel((int) (MAX_IMAGE_LEVEL * ((float) mCurrentZhendongLevel / MAX_LEVEL)));
            } 
            	
        });  
		mShaker.stop();		
	}
	
	private void getCurrentLevelByInputLevel(int level){
		if(level >= 20){
			mCurrentZhendongLevel = MAX_LEVEL;
			mLastLevel = 50;
		}else{
			if(level > mLastLevel){
				mLastLevel = level;
				mCurrentZhendongLevel = mLastLevel / 2;
			}else{
				mLastLevel -= 5;
				if(mLastLevel < 0){
					mLastLevel = 0;
				}
				mCurrentZhendongLevel = mLastLevel / 2;
			}
		}
	}
	
	public void onResume()
	{
		super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
	
		mShaker.start();
		
		keepScreenOn(true);
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);
		
		keepScreenOn(false);
		stopVibration();

		mShaker.stop();	
	}
}

