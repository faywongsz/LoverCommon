package com.yuning.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.iflytek.asr.AsrService.Asr;
import com.iflytek.asr.AsrService.AsrRecord;
import com.iflytek.asr.AsrService.RecognitionResult;
import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

public class BasicVoiceMode extends BaseModeActivity implements View.OnTouchListener, Asr.OnCallback {
	private static final String TAG = BasicVoiceMode.class.getSimpleName();
	
	private ImageView mImageRecord, mImageVoiceAnim, mImageVoiceBG, mImageAnimation;
	private TextView mTextLevel, mTextTips;
	private AnimationDrawable mAnimationDrawable, mVoiceAnimationDrawable;
	private int LEVEL_COUNT = 6;
	private int mCurrentLevel = 0;
    
	public static final String ASR_PATH = Environment.getExternalStorageDirectory().getPath() + "/asr/";
	private String[] mAiStrings;
	public static final String[] mAsrFileName={"grm.irf",
			                "iFlyDefG",
			                "ivAM.irf",
			                "ivCMNParam.irf",
			                "ivDTree.irf",
			                "ivFM.irf",
			                "ivMHTab.irf",
			                "ivModel.irf",
			                "ivNumb.irf",
			                "VoiceTagGrm.irf"};
	
	private static final int MSG_HIDE_TIPS_INTERVAL = 15000;
	//private static final int MSG_HIDE_LEVLE_INTERVAL = 5000;
	private static final int MSG_HIDE_TIPS = 1;
	private static final int MSG_HIDE_LEVLE = 2;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			
			switch(what) {
				case MSG_HIDE_TIPS :
					mTextTips.startAnimation(getAlphaAnimation(false));
					break;
				case MSG_HIDE_LEVLE :
					mTextLevel.startAnimation(getAlphaAnimation(false));
					break;
			}
		}
		
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_voice_mode);
		initTitleBar(R.string.basic_voice_mode, R.drawable.base_mode_base_selector, 0, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(BasicVoiceMode.this, BasicMode.class));
				finish();
			}
		});
		initViews();
		saveBoolData(BASIC_MODE_VOICE_TYPE, true);
		
        TypedArray orderTypes = getResources().obtainTypedArray(R.array.asr_orders_array);
        mAiStrings = new String[orderTypes.length()];
        for(int i = 0; i < orderTypes.length(); i++) {
        	mAiStrings[i] = orderTypes.getString(i);
        }
        orderTypes.recycle();
        
		Thread thread=new Thread(new Runnable() {  
            @Override  
            public void run() {  
        		CopyAssets("asr".toString(), ASR_PATH);
            	mMsgHandler.sendEmptyMessage(MSG_HANDLE_COPY_END);
            }  
        });  

        if(isAsrFileExist()){
        	Log.d(TAG, "asr file is exist");
        	initAitalk();
        }else{
        	Log.d(TAG, "asr file not exist");   
            thread.start(); 
        }
        
        updateStatus(false, mCurrentLevel);
	}
	
	private void initViews() {
		mImageRecord = (ImageView) findViewById(R.id.basic_mode_voice_record);
		mImageVoiceAnim = (ImageView) findViewById(R.id.basic_mode_voice_animation);
		mImageVoiceBG = (ImageView) findViewById(R.id.basic_mode_voice_bg);
		mImageAnimation = (ImageView) findViewById(R.id.base_mode_animation);
		mTextLevel = (TextView) findViewById(R.id.base_mode_level);
		mTextTips = (TextView) findViewById(R.id.base_mode_voice_tips);
		
		mImageRecord.setOnTouchListener(this);
		
		mVoiceAnimationDrawable = (AnimationDrawable) mImageVoiceAnim.getDrawable();
		
		mHandler.sendEmptyMessageDelayed(MSG_HIDE_TIPS, MSG_HIDE_TIPS_INTERVAL);
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(TAG);
		MobclickAgent.onResume(this);
		
		Asr.mNeedStop = false;
		
		keepScreenOn(true);
	}
	
	public void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
		
		Asr.mNeedStop = true;
		keepScreenOn(false);
		pause();
	}

    @Override
    protected void onDestroy() {
    	Asr.mNeedStop = true;
    	/*wangfei added begin*/
    	//Asr.JniDestroy();
    	/*wangfei added end*/
    	super.onDestroy();
    }
    
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(!isConnect()) {
			showOpenDeviceDialog();
			return true;
		}
		
		switch(event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN :
				mImageRecord.setPressed(true);
				startRecord();
				updateVoiceAnimation(true);
				break;
			case MotionEvent.ACTION_MOVE :
				break;
			case MotionEvent.ACTION_UP :
			case MotionEvent.ACTION_CANCEL :
				mImageRecord.setPressed(false);
				AsrRecord.ForceEndRecord();
				updateVoiceAnimation(false);
				break;
		}
		return true;
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
    
    private void updateLevel(int level){
    	if(setBasicLevelMode(level) || WAIT_NO_RESPONSE) {
    		mCurrentLevel = level;
    		updateStatus(false, mCurrentLevel);
    		Log.d(TAG, "level = " + level);
    	}
    }
    
    private void updateStatus(boolean pause, int level) {
    	if(pause) {
    		mTextLevel.setText(getString(R.string.basic_mode_levle, level) + getString(R.string.basic_voice_pause));
    	} else if(level == LEVEL_COUNT) {
    		mTextLevel.setText(getString(R.string.basic_mode_levle, level) + getString(R.string.basic_voice_max));
    	} else {
    		mTextLevel.setText(getString(R.string.basic_mode_levle, level));
    	}
    	
    	//mTextLevel.clearAnimation();
    	//mTextLevel.startAnimation(getAlphaAnimation(true));
    	//mHandler.removeMessages(MSG_HIDE_LEVLE);
    	//mHandler.sendEmptyMessageDelayed(MSG_HIDE_LEVLE, MSG_HIDE_LEVLE_INTERVAL);
    	
    	if(mAnimationDrawable != null && mAnimationDrawable.isRunning()) {
    		mAnimationDrawable.stop();
    	}
    	mAnimationDrawable = BasicMode.getAnimationDrawable(getApplicationContext(), pause ? 0 : level);
    	mImageAnimation.setImageDrawable(mAnimationDrawable);
    	mAnimationDrawable.start();
    }
    
    private void updateVoiceAnimation(boolean start) {
    	if(start) {
    		mImageVoiceAnim.setVisibility(View.VISIBLE);
    		mImageVoiceBG.setVisibility(View.INVISIBLE);
    		
    		mVoiceAnimationDrawable.start();
    	} else {
    		mImageVoiceAnim.setVisibility(View.INVISIBLE);
    		mImageVoiceBG.setVisibility(View.VISIBLE);
    		
    		mVoiceAnimationDrawable.stop();
    	}
    }
    
    private Animation getAlphaAnimation(final boolean visible) {
    	Animation  animation = new AlphaAnimation(visible ? 0.0f : 1.0f, visible ? 1.0f : 0.0f);
    	animation.setDuration(1000);
    	animation.setFillAfter(true);
    	
    	return animation;
    }
    
    private void pause() {
    	setBasicLevelMode(0);
    	updateStatus(true, mCurrentLevel);
    }

	@Override
	public void onCallback() {
		if (Asr.mResult.size() == 0){
			Log.d(TAG,"Asr.mResult.size() == 0");
			return;
		}
		RecognitionResult result = Asr.mResult.get(0);
	
		int nSlots = result.mSlotList.size();
		String resMsg = "";
		for (int i = 0; i < nSlots; i ++){
			resMsg += result.mSlotList.get(i).mItemTexts[0];
		}
		
		if(resMsg.length() > 0){
			Log.d(TAG,"resMsg="+resMsg);
			if(resMsg.contains(mAiStrings[0]) || resMsg.contains(mAiStrings[1])){
				updateLevel(getLevel(true, mCurrentLevel));
			}else if(resMsg.contains(mAiStrings[2]) || resMsg.contains(mAiStrings[3])){
				updateLevel(getLevel(false, mCurrentLevel));
			}else if(resMsg.contains(mAiStrings[4])){
				pause();
			}else if(resMsg.contains(mAiStrings[5])){
				updateLevel(mCurrentLevel == 0 ? 1 : mCurrentLevel);
			}
				
		}else{
			Log.d(TAG,"resMsg.length()<=0");
		}
		String infoSting = "resMsg:" + resMsg + "  mPwmLevel:" + mCurrentLevel;
		Log.d(TAG,infoSting);
	}
	
    boolean isAsrFileExist(){
    	for(int i = 0;i < mAsrFileName.length; i++){
    		String filename = ASR_PATH + mAsrFileName[i];
    		File file = new File(filename);
    		Log.d(TAG, filename);
    	   	if(!file.exists()){
        		Log.e(TAG, "not exists");
        		return false;
        	}
    	}
 
    	return true;
    }
    
    void initAitalk() {
        //result_info.setText("init OK");
    	/*wangfei added begin*/
        //Asr.JniCreate();
    	/*wangfei added end*/
		Asr.JniBeginLexicon("menu", false);
    	for(int i = 0; i < mAiStrings.length; i++){
    		Asr.JniAddLexiconItem(mAiStrings[i], i);
    	}
    	Asr.JniEndLexicon();
    	mMsgHandler.sendEmptyMessage(MSG_HANDLE_START_REG);
    }    
    
	private void onClickStart() {
		//1. 设置识别场景
		
		Asr.startRecoThread(null, this);
		Log.d(TAG,"Asr ready");
	}
	
	public void startRecord(){
		Log.d(TAG,"startRecord()");
		if(!AsrRecord.isThreadRun){
			Asr.reStart();			
		}
	}
	
    final int MSG_HANDLE_COPY_END = 100;
    final int MSG_HANDLE_START_REG = 101;    
	private Handler mMsgHandler = new Handler() { 
		@Override
		public void handleMessage(Message msg)
		{ 
			switch (msg.what)  {
			 case MSG_HANDLE_COPY_END:
				 initAitalk();
				 break;
			 case MSG_HANDLE_START_REG:
				 onClickStart();
				 break;
			
			}
		}
	};
		
    private void CopyAssets(String assetDir, String dir) {
    	  String[] files;
      	  try {
      	   files = this.getResources().getAssets().list(assetDir);
      	  } catch (IOException e1) {
      	   return;
      	  }
      	  File mWorkingPath = new File(dir);
      	  // if this directory does not exists, make one.
      	  if (!mWorkingPath.exists()) {
      	   if (!mWorkingPath.mkdirs()) {
      	    Log.e("--CopyAssets--", "cannot create directory.");
      	   }
      	  }
      	  for (int i = 0; i < files.length; i++) {
      	   try {
      	    String fileName = files[i];
      	    /*
      	    // we make sure file name not contains '.' to be a folder.
      	    if (!fileName.contains(".")) {
      	     if (0 == assetDir.length()) {
      	      CopyAssets(fileName, dir + fileName + "/");
      	     } else {
      	      CopyAssets(assetDir + "/" + fileName, dir + fileName
      	        + "/");
      	     }
      	     continue;
      	    }
      	    */
      	    File outFile = new File(mWorkingPath, fileName);
      	    if (outFile.exists())
      	     outFile.delete();
      	    InputStream in = null;
      	    if (0 != assetDir.length())
      	     in = getAssets().open(assetDir + "/" + fileName);
      	    else
      	     in = getAssets().open(fileName);
      	    OutputStream out = new FileOutputStream(outFile);
      	    // Transfer bytes from in to out
      	    byte[] buf = new byte[1024];
      	    int len;
      	    while ((len = in.read(buf)) > 0) {
      	     out.write(buf, 0, len);
      	    }
      	    in.close();
      	    out.close();
      	   } catch (FileNotFoundException e) {
      	    e.printStackTrace();
      	   } catch (IOException e) {
      	    e.printStackTrace();
      	   }
      	  }
    }
    
    @Override
	protected void disconnectCallBack() {
    	finish();
	}

}
