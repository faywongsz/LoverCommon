package com.yuning.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;

public class BasicMode extends BaseModeActivity implements View.OnClickListener {
	private static final String TAG = BasicMode.class.getSimpleName();
	
	private static final String SP_BASE_MODE_TIPS = "base_mode_tips";
	private static final String SP_TIPS_ANIMATION = "tips_animation";
	private ImageView mImagePrev, mImageNext, mImagePlayPause, mImageAnimation;
	private TextView mTextLevel;
	private AnimationDrawable mAnimationDrawable;
	private boolean mbPlaying = false;
	private int LEVEL_COUNT = 6;
	private int mCurrentLevel = 0;

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.basic_mode);
		boolean noTips = getBoolData(SP_BASE_MODE_TIPS);
		initTitleBar(R.string.basic_mode, noTips ? R.drawable.base_mode_voice_selector : R.drawable.base_mode_voice_tips_selector, 0, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveBoolData(SP_BASE_MODE_TIPS, true);
				startActivity(new Intent(BasicMode.this, BasicVoiceMode.class));
				finish();
			}
		});
		init();
		saveBoolData(BASIC_MODE_VOICE_TYPE, false);
		updateStatus(mCurrentLevel);
	}
	
	private void init() {
		mImagePrev = (ImageView)findViewById(R.id.base_mode_prev_level);
		mImageNext = (ImageView) findViewById(R.id.base_mode_next_level);
		mImagePlayPause = (ImageView) findViewById(R.id.base_mode_play_pause);
		mImageAnimation = (ImageView) findViewById(R.id.base_mode_animation);
		mTextLevel = (TextView) findViewById(R.id.base_mode_level);
		mImagePrev.setOnClickListener(this);
		mImageNext.setOnClickListener(this);
		mImagePlayPause.setOnClickListener(this);
		mImagePlayPause.setVisibility(View.INVISIBLE);
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(TAG);
		MobclickAgent.onResume(this);
		
		keepScreenOn(true);
		
		if(isConnect() && !getBoolData(SP_TIPS_ANIMATION)) {
			showTipsLayoutAnimation(R.string.tips_title);
			saveBoolData(SP_TIPS_ANIMATION, true);
		}
	}
	
	public void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
		
		keepScreenOn(false);
		pasue();
	}

	@Override
	public void onClick(View v) {
		if(!isConnect()) {
			showOpenDeviceDialog();
			return;
		}
		
		switch(v.getId()) {
			case R.id.base_mode_prev_level :
				updateLevel(getLevel(false, mCurrentLevel));
				break;
			case R.id.base_mode_next_level :
				updateLevel(getLevel(true, mCurrentLevel));
				break;
			case R.id.base_mode_play_pause :
				if(mbPlaying)  {
					pasue();
				} else {
					updateLevel(mCurrentLevel);
				}
				break;
		}
	}
	
	private int getLevel(boolean isNext, int level) {
		int result = 0;
		if(isNext) {
			if(level < LEVEL_COUNT) {
				result = level + 1;
			}
		} else {
			if(level > 0) {
				result = level - 1;
			}
		}
		
		return result;
	}
    
    private void updateLevel(int level) {
    	if(setBasicLevelMode(level) || WAIT_NO_RESPONSE) {
    		mCurrentLevel = level;
    		updateStatus(mCurrentLevel);
    		Log.d(TAG, "level = " + level);
    	}
    }
    
    private void updateStatus(int level) {
    	mTextLevel.setText(getString(R.string.basic_mode_levle, level));
    	if(level == LEVEL_COUNT) {
    		mImageNext.setEnabled(false);
    		mbPlaying = true;
    		mImagePlayPause.setImageResource(R.drawable.base_mode_pause_selector);
    		mImagePlayPause.setVisibility(View.VISIBLE);
    	} else if(level == 0) {
    		mImagePrev.setEnabled(false);
    		mbPlaying = false;
    		mImagePlayPause.setImageResource(R.drawable.base_mode_play_selector);
    		mImagePlayPause.setVisibility(View.INVISIBLE);
    	} else {
    		mImagePrev.setEnabled(true);
    		mImageNext.setEnabled(true);
    		mbPlaying = true;
    		mImagePlayPause.setImageResource(R.drawable.base_mode_pause_selector);
    		mImagePlayPause.setVisibility(View.VISIBLE);
    	}
    	
    	updateAnimation(level);
    }
    
    private void pasue() {
		setBasicLevelMode(0);
		mbPlaying = false;
		mImagePlayPause.setImageResource(R.drawable.base_mode_play_selector);
    	updateAnimation(0);
    }
    
    @Override
	protected void disconnectCallBack() {
    	finish();
	}
    
    private void updateAnimation(int level) {
    	if(mAnimationDrawable != null && mAnimationDrawable.isRunning()) {
    		mAnimationDrawable.stop();
    	}
    	mAnimationDrawable = getAnimationDrawable(getApplicationContext(), level);
    	mImageAnimation.setImageDrawable(mAnimationDrawable);
    	mAnimationDrawable.start();
    }

	public static AnimationDrawable getAnimationDrawable(Context context, int level) {
    	AnimationDrawable frameAnimation = new AnimationDrawable();
    	
    	if(level > 0) {
    		int duration = 0;
    		switch(level) {
    			case 1 :
    				duration = 110;
    				break;
    			case 2 :
    				duration = 90;
    				break;
    			case 3 :
    				duration = 70;
    				break;
    			case 4 :
    				duration = 50;
    				break;
    			case 5 :
    				duration = 40;
    				break;
    			case 6 :
    				duration = 30;
    				break;
    		}
    		frameAnimation.addFrame(context.getResources().getDrawable(R.drawable.base_mode_animation_1), duration);
    		frameAnimation.addFrame(context.getResources().getDrawable(R.drawable.base_mode_animation_2), duration);
    		frameAnimation.addFrame(context.getResources().getDrawable(R.drawable.base_mode_animation_3), duration);
    		frameAnimation.addFrame(context.getResources().getDrawable(R.drawable.base_mode_animation_4), duration);
    		frameAnimation.addFrame(context.getResources().getDrawable(R.drawable.base_mode_animation_5), duration);
    		frameAnimation.setOneShot(false);
    	} else {
    		frameAnimation.addFrame(context.getResources().getDrawable(R.drawable.base_mode_animation_1), 0);
    		frameAnimation.setOneShot(true);
    	}
    	
    	
    	return frameAnimation;
    }
}
