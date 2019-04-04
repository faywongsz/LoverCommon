package com.yuning.activity;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;


public class ClassicMode extends BaseModeActivity implements View.OnClickListener{
	private static final String TAG = ClassicMode.class.getSimpleName();
	
	private ImageView mImagePlayStop;
	private AnimationDrawable mWaveAnimation;
	private boolean mbPlaying;
	private int mSelectedIndex = 1;
	private ViewGroup mSelectedItemLayout;
	
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.classic_mode);
		initTitleBar(R.string.classic_mode, 0, 0, null);
		
		mImagePlayStop = (ImageView) findViewById(R.id.classic_mode_play_stop);
		mImagePlayStop.setOnClickListener(this);
		
		mWaveAnimation = (AnimationDrawable) ((ImageView)findViewById(R.id.classic_mode_wave)).getBackground();
		
		int index = getIntData(CLASSIC_MODE_INDEX);
		mSelectedIndex = (index == 0 ? 1 : index);
		View selectedView = findViewById(R.id.zhendong_item_img_01 + mSelectedIndex - 1);
		updateSelectedStatus(selectedView == null ? findViewById(R.id.zhendong_item_img_01) : selectedView);
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(TAG);
		MobclickAgent.onResume(this);
		
		keepScreenOn(true);
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
		
		mbPlaying = false;
		mImagePlayStop.setImageResource(R.drawable.classic_mode_play_selector);
		keepScreenOn(false);
		stopVibration();
	}

	@Override
	public void onClick(View v) {
		if(!isConnect()) {
			showOpenDeviceDialog();
			return;
		}
		
		switch(v.getId()) {
			case R.id.classic_mode_play_stop :
				if(mbPlaying) {
					mbPlaying = false;
					mImagePlayStop.setImageResource(R.drawable.classic_mode_play_selector);
					setClassicLevelMode(0);
					mWaveAnimation.stop();
				} else {
					mbPlaying = true;
					mImagePlayStop.setImageResource(R.drawable.classic_mode_stop_selector);
					setClassicLevelMode(mSelectedIndex);
					mWaveAnimation.start();
					Log.d(TAG, "classic mode = " + (mSelectedIndex));
				}
				break;
		}
	}
	
    public void onBtnClick_Array(View view){
		try{
			int index = Integer.parseInt((String) view.getTag()) ;
			if(index > 0) {
				mSelectedIndex = index;
				updateSelectedStatus(view);
				if(mbPlaying) {
					setClassicLevelMode(mSelectedIndex);
				}
				saveIntData(CLASSIC_MODE_INDEX, mSelectedIndex);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
    
    private void updateSelectedStatus(View view) {
		if(mSelectedItemLayout != null) {
			mSelectedItemLayout.setBackground(null);
		}
		mSelectedItemLayout = (ViewGroup) view.getParent();
		mSelectedItemLayout.setBackgroundResource(R.drawable.zhendong_item_selected_bg);
    }

}

