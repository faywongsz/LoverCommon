package com.yuning.activity;

import com.yuning.lovercommon.R;
import com.yuning.ui.CustomDialog;
import com.yuning.util.sendThread;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class BaseModeActivity extends BaseActivity {
	private static final String TAG = BaseModeActivity.class.getSimpleName();
	
	private CustomDialog mOPenDevicesDialog;
	private boolean mbHasTitle = false;
	
	private View mTipsLayout;
	public static final int MAX_MASSAGE_LEVEL = 25;
	private byte[] mCMDStop = {(byte) 0xff,(byte)0xff};
	private byte[] mCMDClassicLevel = {0x01, 0x00};
	private byte[] mCMDFunLevel = {0x02, 0x00};
	private byte[] mCMDBasicLevel = {0x03, 0x00};
	private byte[] mCMDModeLevel = {0x05, 0x00};
	
	protected static final boolean WAIT_NO_RESPONSE = true;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		onCreate(savedInstanceState, true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy: stop");
		stopVibration();
	}
	
	public void onCreate(Bundle savedInstanceState, boolean hasTitle) {
		super.onCreate(savedInstanceState);
		mbHasTitle = hasTitle;
		if(mbHasTitle) requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	}

	protected void initTitleBar(int titleResId, int rightImageResId, int rightTextResId, View.OnClickListener listener) {
		if(mbHasTitle) getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);  
		
		View back = findViewById(R.id.title_back);
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		TextView title = (TextView) findViewById(R.id.title_name);
		title.setText(titleResId);
		
		if(rightImageResId != 0) {
			setRightImageItem(rightImageResId, listener);
		}
		if(rightTextResId != 0) {
			setRightButtonItem(rightTextResId, listener);
		}
	}
	
	protected void setRightImageItem(int resId, View.OnClickListener listener) {
		ImageView right = (ImageView) findViewById(R.id.title_right_image);
		right.setVisibility(View.VISIBLE);
		right.setImageResource(resId);
		right.setOnClickListener(listener);
	}
	
	protected void setRightButtonItem(int textredId, View.OnClickListener listener) {
		Button right = (Button) findViewById(R.id.title_right_button);
		if(textredId != 0) {
			right.setVisibility(View.VISIBLE);
			right.setText(textredId);
		} else {
			right.setVisibility(View.GONE);
			right.setText("");
		}
		right.setOnClickListener(listener);
	}
	
	protected void showTipsLayoutAnimation(int tipsRedId) {
		final int tipsHeight = getResources().getDimensionPixelSize(R.dimen.title_bar_height);
		if(mTipsLayout == null) {
			mTipsLayout = getLayoutInflater().inflate(R.layout.tips_layout, null);
			ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, tipsHeight);
			((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content)).addView(mTipsLayout, lp);
			//mTipsLayout.setTranslationY(-tipsHeight);
		}
		TextView textTips = (TextView) mTipsLayout.findViewById(R.id.text_tips);
		textTips.setText(tipsRedId);
		
		AnimationSet animationSet = new AnimationSet(true);
		
		TranslateAnimation animation_down = new TranslateAnimation(0, 0, -tipsHeight, 0);
		animation_down.setStartOffset(600);
		animation_down.setDuration(500);
		animation_down.setFillAfter(true);
		animationSet.addAnimation(animation_down);
		
		TranslateAnimation animation_up = new TranslateAnimation(0, 0, 0, -tipsHeight);
		animation_up.setStartOffset(3500);
		animation_up.setDuration(500);
		animation_up.setFillAfter(true);
		animation_up.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			@Override
			public void onAnimationEnd(Animation animation) {
				mTipsLayout.setVisibility(View.GONE);
			}
		});
		animationSet.addAnimation(animation_up);
		mTipsLayout.startAnimation(animationSet);
	}
	
	protected void showOpenDeviceDialog() {
    	if(mOPenDevicesDialog == null) {
    		mOPenDevicesDialog = new CustomDialog(this);
    	}
    	if(!mOPenDevicesDialog.getDialog().isShowing()) {
    		mOPenDevicesDialog.getDialog().show();
    	}
    	
    	mOPenDevicesDialog.setTitle(R.string.open_device_title);
    	mOPenDevicesDialog.setMessage(R.string.open_device_massage);
    	mOPenDevicesDialog.setYesButton(R.string.open_device_yes, new CustomDialog.OnClickListener() {
			@Override
			public void onClick(Dialog dialog) {
				mOPenDevicesDialog.getDialog().dismiss();
				finish();
			}
		});
    	mOPenDevicesDialog.setNoButton(R.string.open_device_no, new CustomDialog.OnClickListener() {
			@Override
			public void onClick(Dialog dialog) {
				mOPenDevicesDialog.getDialog().dismiss();
			}
		});
	}
	
	protected void keepScreenOn(boolean on) {
		if(on) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	protected boolean setClassicLevelMode(int level) {
		mCMDClassicLevel[1] = (byte) level;
		return sendData(mCMDClassicLevel);
	}
	
	protected void setFunLevelMode(int level) {
		mCMDFunLevel[1] = (byte) level;
		sendThread.sendData(mCMDFunLevel);
	}
	
	protected boolean setBasicLevelMode(int level) {
		mCMDBasicLevel[1] = (byte) level;
		return sendData(mCMDBasicLevel);
	}
	
	protected boolean setModeLevelMode(int level) {
		mCMDModeLevel[1] = (byte) level;
		return sendData(mCMDModeLevel);
	}
	
	protected void stopVibration() {
		sendThread.sendData(mCMDStop);
	}
}
