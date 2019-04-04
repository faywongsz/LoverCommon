package com.yuning.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.InputFilter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.asr.AsrService.Asr;
import com.iflytek.asr.AsrService.AsrRecord;
import com.iflytek.asr.AsrService.RecognitionResult;
import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;
import com.yuning.util.RealDoubleFFT;
import com.yuning.util.sendThread;
import com.yuning.util.sysinfo;

public class Aitalkmode extends BaseActivity {
	private String TAG = "Aitalkmode";
	private WakeLock mWakeLock = null;

	private static final byte[] mCMDStop={(byte)0xff,(byte)0xff};
	private static final int MAX_PWM_LEVEL = 7;
	private static final int MAX_MODE_LEVEL = 10;//change mode count by guoliangliang 20150202
	private static final int AI_STRING_MAX_LENGTH = 10;
	private static final int AI_STRING_STOP_MAX_lENGTH = 6;
	private final String mAiTalkSplitString = "                    ";
	//private static String[] mModeStrings;//delete by guoliangliang 20150202
	private int mPwmLevel = 0;//change default level by guoliangliang 20150202
	private int mModeLevel = 0;//change default mode by guoliangliang 20150202
		
	private ProgressDialog progressDialog; 
	private ImageView mBtnStart;
	private TextView mAiStringLess;
	private TextView mAiStringNext;
	private TextView mAiStringMore;
	private TextView mAiStringPrev;
	private TextView mAiStringStop;
	private String[] mAiStrings;
	private TextView mCurrentModeText;
	private TextView mCurrentLevelText;
	private TextView mAiTextViews[];
	private ImageView mAiBtnLess;
	private ImageView mAiBtnNext;
	private ImageView mAiBtnMore;
	private ImageView mAiBtnPrev;
	private ImageView backIcon;
	private Animation mAiBtnLessAnim;
	private Animation mAiBtnNextAnim;
	private Animation mAiBtnMoreAnim;
	private Animation mAiBtnPrevAnim;
	private Animation mAiBtnStopAnim;
	private ImageView mVoiceLine;
	private ImageView mVoiceLineAnim;
	//private Bitmap mBitmap;
	//private Canvas mCanvas;
	private Paint mPaint;
	//RecordAudio mRecordTask;
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int blockSize = 256;
	boolean started = false;
	private long lastUpdateTime;
	private static final long UPTATE_INTERVAL_TIME = 20L;
	private RealDoubleFFT transformer;
	
	private View.OnTouchListener mBtnStartOnTouchListener = new View.OnTouchListener() {
		
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			// TODO Auto-generated method stub	    	
	        final int action = event.getActionMasked();
	        switch (action) {
	            case MotionEvent.ACTION_DOWN:
	                Log.i(TAG,"ACTION_DOWN");
	                ((ImageView)view).setImageResource(R.drawable.ai_talk_start_pressed);
	                //mRecordTask = new RecordAudio();
	                //mRecordTask.execute();
	                started = true;
	                //mVoiceLine.setImageBitmap(mBitmap);
	                /*wangfei added begin*/
	                //mVoiceLine.setVisibility(View.INVISIBLE);
	                //mVoiceLineAnim.setVisibility(View.VISIBLE);
	                startVoiceLineAnimation();
	                /*wangfei added end*/
	                startRecord();
	                break;
	            case MotionEvent.ACTION_MOVE:
	                //Log.i(TAG, action + ": ACTION_MOVE");
	                break;
	            case MotionEvent.ACTION_UP:
	                Log.i(TAG,"ACTION_UP");
	                //mRecordTask.cancel(true);
	                started = false;
	                ((ImageView)view).setImageResource(R.drawable.ai_talk_start_normal);
	                AsrRecord.ForceEndRecord();
	                /*wangfei added begin*/
	                //mVoiceLineAnim.setVisibility(View.INVISIBLE);
	                //mVoiceLine.setVisibility(View.VISIBLE);
	                stopVoiceLineAnimation();
	                /*wangfei added end*/
	                break;
	            default:
	                break;
	        }
			return true;
		}
	};
	
	private View.OnLongClickListener mBtnCustomOnLongClickListener = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View view) {
			// TODO Auto-generated method stub
			int viewId = view.getId();
			if(viewId == mAiBtnLess.getId()){
				Log.d(TAG,"mAiBtnLess long click");
				editAiTalkString(0);
			}else if(viewId == mAiBtnNext.getId()){
				Log.d(TAG,"mAiBtnNext long click");
				editAiTalkString(1);
			}else if(viewId == mAiBtnMore.getId()){
				Log.d(TAG,"mAiBtnMore long click");
				editAiTalkString(2);
			}else if(viewId == mAiBtnPrev.getId()){
				Log.d(TAG,"mAiBtnPrev long click");
				editAiTalkString(3);
			}else if(viewId == mAiStringStop.getId()){
				Log.d(TAG,"mAiStringStop long click");
				editAiTalkString(4);
			}
			return true;
		}
	};
	
	private void editAiTalkString(final int i){
		final Context context = this;
		final EditText editText = new EditText(this);
		int maxLength;
		if(i == 4){
			maxLength = AI_STRING_STOP_MAX_lENGTH;
		}else{
			maxLength = AI_STRING_MAX_LENGTH;
		}
		editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
		editText.setText(mAiStrings[i]);
		editText.setSelection(mAiStrings[i].length());
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.ai_talk_edit_dialog_title, maxLength))
				.setView(editText)
				.setNegativeButton(R.string.ai_talk_edit_dialog_cancel, null);
		builder.setPositiveButton(R.string.ai_talk_edit_dialog_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				String inputString = editText.getText().toString().trim();
				if(inputString.length() < 1){
					Toast.makeText(Aitalkmode.this, getString(R.string.ai_talk_edit_dialog_too_short), 0).show();
				}else{
					mAiStrings[i] = inputString;
					//modify by guoliangliang 20150508 begin
					if(i == 0 || i == 2){
						mAiTextViews[i].setText(getVerticalString(inputString));
					}else{
						mAiTextViews[i].setText(inputString);
					}
					//modify by guoliangliang 20150508 end
					saveAiTalkStrings();
					updateAiOrders();
				}
			}
		});
		builder.show();
	}
    
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aitalkmode);
        findViews();
        initTextViews();
        setListeners();
        initStrings();
        initAnimations();
        initVoiceLine();

        /*wangfei added begin*/
        new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
		        sendPwmMsg((byte)mPwmLevel);
		        sendModeMsg((byte)mModeLevel);
			}
		}, 200);
        //sendPwmMsg((byte)mPwmLevel);
        //sendModeMsg((byte)mModeLevel);
        /*wangfei added begin*/

        
		Thread thread=new Thread(new Runnable()  
        {  
            @Override  
            public void run()  
            {  
        		CopyAssets("asr".toString(), "/sdcard/asr");
            	mMsgHandler.sendEmptyMessage(MSG_HANDLE_COPY_END);
            }  
        });  

        if(isAsrFileExist()){
        	Log.d(TAG, "asr file is exist");
        	initAitalk();
        }else{
        	Log.d(TAG, "asr file not exist");   
            progressDialog = ProgressDialog.show(this, "Loading...", "Please wait...", true, false);         	
            thread.start(); 
        }
    }
    
    private void findViews(){
    	backIcon = (ImageView)findViewById(R.id.back_icon);
    	mAiStringLess = (TextView)findViewById(R.id.ai_talk_less);
        mAiStringNext = (TextView)findViewById(R.id.ai_talk_next);
        mAiStringMore = (TextView)findViewById(R.id.ai_talk_more);
        mAiStringPrev = (TextView)findViewById(R.id.ai_talk_prev);
        mAiStringStop = (TextView)findViewById(R.id.ai_talk_stop);
        mCurrentModeText = (TextView)findViewById(R.id.ai_talk_current_mode);
        mCurrentLevelText = (TextView)findViewById(R.id.ai_talk_current_level);
        mBtnStart = (ImageView)findViewById(R.id.ai_talk_start);
        mAiBtnLess = (ImageView)findViewById(R.id.ai_talk_less_btn);
        mAiBtnNext = (ImageView)findViewById(R.id.ai_talk_next_btn);
        mAiBtnMore = (ImageView)findViewById(R.id.ai_talk_more_btn);
        mAiBtnPrev = (ImageView)findViewById(R.id.ai_talk_prev_btn);
        mVoiceLine = (ImageView)findViewById(R.id.ai_talk_voice_line);
        mVoiceLineAnim = (ImageView)findViewById(R.id.ai_talk_voice_line_anim);
    }
    
    private void setListeners(){
    	backIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				finish();
				return;
			}
		});
    	mBtnStart.setOnTouchListener(mBtnStartOnTouchListener);
    	mAiBtnLess.setOnLongClickListener(mBtnCustomOnLongClickListener);
    	mAiBtnNext.setOnLongClickListener(mBtnCustomOnLongClickListener);
    	mAiBtnMore.setOnLongClickListener(mBtnCustomOnLongClickListener);
    	mAiBtnPrev.setOnLongClickListener(mBtnCustomOnLongClickListener);
    	mAiStringStop.setOnLongClickListener(mBtnCustomOnLongClickListener);
    }
    
    private void initStrings(){
    	/*mModeStrings = new String[MAX_MODE_LEVEL];
        mModeStrings[0] = getString(R.string.zhendong_item_01);
        mModeStrings[1] = getString(R.string.zhendong_item_02);
        mModeStrings[2] = getString(R.string.zhendong_item_03);
        mModeStrings[3] = getString(R.string.zhendong_item_04);
        mModeStrings[4] = getString(R.string.zhendong_item_05);
        mModeStrings[5] = getString(R.string.zhendong_item_06);
        mModeStrings[6] = getString(R.string.zhendong_item_07);
        mModeStrings[7] = getString(R.string.zhendong_item_08);
        mModeStrings[8] = getString(R.string.zhendong_item_09);
        mModeStrings[9] = getString(R.string.zhendong_item_10);
        mModeStrings[10] = getString(R.string.zhendong_item_11);
        mModeStrings[11] = getString(R.string.zhendong_item_12);
        mModeStrings[12] = getString(R.string.zhendong_item_13);
        mModeStrings[13] = getString(R.string.zhendong_item_14);
        mModeStrings[14] = getString(R.string.zhendong_item_15);
        mModeStrings[15] = getString(R.string.zhendong_item_16);*/
        mAiStrings = new String[5];
        getAiTalkStrings();
    	for(int i = 0; i < mAiTextViews.length; i ++){
    		//modify by guoliangliang 20150507 begin
    		if(i == 0 || i == 2){
    			mAiTextViews[i].setText(getVerticalString(mAiStrings[i]));
    		}else{
    			mAiTextViews[i].setText(mAiStrings[i]);
    		}
    		//modify by guoliangliang 20150507 end
    	}
    }
    
    //add by guoliangliang 20150507 begin
    private String getVerticalString(String str){
    	String newStr = "";
    	if(str.length() == 1){
    		newStr = str;
    	}else{
    		for(int i = 0; i < str.length(); i++){
    			newStr += str.substring(i, i + 1);
    			if(i < str.length() - 1){
    				newStr += "\n";
    			}
    		}
    	}
    	return newStr;
    }
    //add by guoliangliang 20150507 end
    
	private void initAnimations(){
    	mAiBtnLessAnim = new AlphaAnimation(1, 1);
    	mAiBtnLessAnim.setDuration(200);
    	mAiBtnLessAnim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
				mAiBtnLess.setPressed(true);
			}
			@Override
			public void onAnimationRepeat(Animation arg0) {}
			@Override
			public void onAnimationEnd(Animation arg0) {
				mAiBtnLess.setPressed(false);
			}
		});
    	
    	mAiBtnNextAnim = new AlphaAnimation(1, 1);
    	mAiBtnNextAnim.setDuration(200);
    	mAiBtnNextAnim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
				mAiBtnNext.setPressed(true);
			}
			@Override
			public void onAnimationRepeat(Animation arg0) {}
			@Override
			public void onAnimationEnd(Animation arg0) {
				mAiBtnNext.setPressed(false);
			}
		});
    	
    	mAiBtnMoreAnim = new AlphaAnimation(1, 1);
    	mAiBtnMoreAnim.setDuration(200);
    	mAiBtnMoreAnim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
				mAiBtnMore.setPressed(true);
			}
			@Override
			public void onAnimationRepeat(Animation arg0) {}
			@Override
			public void onAnimationEnd(Animation arg0) {
				mAiBtnMore.setPressed(false);
			}
		});
    	
    	mAiBtnPrevAnim = new AlphaAnimation(1, 1);
    	mAiBtnPrevAnim.setDuration(200);
    	mAiBtnPrevAnim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {
				mAiBtnPrev.setPressed(true);
			}
			@Override
			public void onAnimationRepeat(Animation arg0) {}
			@Override
			public void onAnimationEnd(Animation arg0) {
				mAiBtnPrev.setPressed(false);
			}
		});
    	
    	mAiBtnStopAnim = new AlphaAnimation(1, 1);
    	mAiBtnStopAnim.setDuration(200);
    	mAiBtnStopAnim.setAnimationListener(new AnimationListener() {
			public void onAnimationStart(Animation arg0) {
				mAiStringStop.setTextColor(0xffffff00);
			}
			public void onAnimationRepeat(Animation arg0) {}
			public void onAnimationEnd(Animation arg0) {
				mAiStringStop.setTextColor(0xffd71820);
			}
		});
    }
    
    private void initVoiceLine(){
    	//mBitmap = Bitmap.createBitmap((int) 712, (int) 200, Bitmap.Config.ARGB_8888);
    	//mCanvas = new Canvas(mBitmap);
    	mPaint = new Paint();
		mPaint.setStrokeWidth(3f);
		mPaint.setColor(0xff800033);
    	//mCanvas.drawColor(Color.BLACK);
    	//mCanvas.drawLine(0,99,712,100,mPaint);
		//mVoiceLine.setImageResource(R.drawable.ai_talk_voice_line);
		//mRecordTask = new RecordAudio();
		transformer = new RealDoubleFFT(blockSize);
    }
    
    private void getAiTalkStrings(){
    	String lastStrings = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getString("ZhiAiAiTalkModeStrings", "");
    	if(lastStrings.equals("")){
    		mAiStrings[0] = getString(R.string.ai_talk_less);
        	mAiStrings[1] = getString(R.string.ai_talk_next);
        	mAiStrings[2] = getString(R.string.ai_talk_more);
        	mAiStrings[3] = getString(R.string.ai_talk_prev);
        	mAiStrings[4] = getString(R.string.ai_talk_stop);
        	saveAiTalkStrings();
    	}else{
    		String strStrings[] = lastStrings.split(mAiTalkSplitString);
    		for(int i = 0; i < strStrings.length; i++){
    			mAiStrings[i] = strStrings[i];
    		}
    	}
    }
    
    private void saveAiTalkStrings(){
    	String lastStrings = mAiStrings[0];
    	for(int i = 1; i < mAiStrings.length; i++){
    		lastStrings = lastStrings + mAiTalkSplitString + mAiStrings[i];
    	}
    	android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putString("ZhiAiAiTalkModeStrings", lastStrings);
		editor.commit();
    }
    
    private void initTextViews(){
    	mAiTextViews = new TextView[5];
    	mAiTextViews[0] = mAiStringLess;
    	mAiTextViews[1] = mAiStringNext;
    	mAiTextViews[2] = mAiStringMore;
    	mAiTextViews[3] = mAiStringPrev;
    	mAiTextViews[4] = mAiStringStop;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();  
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);	
		
		sendThread.sendData(mCMDStop);
    	Asr.mNeedStop = true;
		
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
			mWakeLock = null;
		}
		//mVoiceLine.setImageResource(R.drawable.ai_talk_voice_line);
		/*wangfei added begin*/
		//mVoiceLineAnim.setVisibility(View.INVISIBLE);
		//mVoiceLine.setVisibility(View.VISIBLE);
		stopVoiceLineAnimation();
		/*wangfei added end*/
		//mRecordTask.cancel(true);
		started = false;
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	Asr.mNeedStop = false;
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
		
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
			mWakeLock.setReferenceCounted(false);
			mWakeLock.acquire();
		}	
    }
    
    boolean isAsrFileExist(){
    	for(int i = 0;i<mAsrFileName.length; i++){
    		String filename = "/sdcard/asr/" + mAsrFileName[i];
    		File file = new File(filename);
    		Log.d(TAG, filename);
    	   	if(!file.exists()){
        		Log.e(TAG, "not exists");
        		return false;
        	}
    	}
 
    	return true;
    }
    void initAitalk()
    {
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
    
    private void updateAiOrders(){
    	Asr.JniBeginLexicon("menu", false);
    	for(int i = 0; i < mAiStrings.length; i++){
    		Asr.JniAddLexiconItem(mAiStrings[i], i);
    	}
    	Asr.JniEndLexicon();
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
				 progressDialog.dismiss();
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
    protected void onDestroy() {
    	Asr.mNeedStop = true;
    	/*wangfei added begin*/
    	//Asr.JniDestroy();
    	/*wangfei added end*/
    	super.onDestroy();
    }
    

	public void onResult(){
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
			if(resMsg.contains(mAiStrings[0])){
				if(mModeLevel == 0) {
					if(mPwmLevel > 0){
						mPwmLevel--;
					}
				} else {
					mModeLevel = 0;
					mPwmLevel = 0;
				}
				mAiBtnLess.startAnimation(mAiBtnLessAnim);
				sendPwmMsg((byte)mPwmLevel);
				sendModeMsg((byte)mModeLevel);
			}else if(resMsg.contains(mAiStrings[1])){
				if(mModeLevel > 0){
					mModeLevel--;
				}
				mAiBtnNext.startAnimation(mAiBtnNextAnim);
				sendModeMsg((byte)mModeLevel);
			}else if(resMsg.contains(mAiStrings[2])){
				if(mModeLevel == 0) {
					if(mPwmLevel < MAX_PWM_LEVEL - 1){
						mPwmLevel++;
					}
				} else {
					mModeLevel = 0;
					mPwmLevel = MAX_PWM_LEVEL - 1;
				}
				mAiBtnMore.startAnimation(mAiBtnMoreAnim);
				sendPwmMsg((byte)mPwmLevel);
				sendModeMsg((byte)mModeLevel);
			}else if(resMsg.contains(mAiStrings[3])){
				if(mModeLevel < MAX_MODE_LEVEL - 1){
					mModeLevel++;
				}
				mAiBtnPrev.startAnimation(mAiBtnPrevAnim);
				sendModeMsg((byte)mModeLevel);
			}else if(resMsg.contains(mAiStrings[4])){
				sendData(mCMDStop);
				mAiStringStop.startAnimation(mAiBtnStopAnim);
			}
				
		}else{
			Log.d(TAG,"resMsg.length()<=0");
		}
		String infoSting = "resMsg:" + resMsg + "  mPwmLevel:" + mPwmLevel + "  mModeLevel:" + mModeLevel;
		Log.d(TAG,infoSting);
	}
	
	
	//private void onClickStop() {
		//停止识别
		//Asr.JniStop();
		//Asr.JniStart("menu");	
		//AnimationDrawable animationDrawable = (AnimationDrawable) mVoiceAnimation.getDrawable();  
        //animationDrawable.stop();
	//}


	private void onClickStart() {
		//1. 设置识别场景
		
		Asr.startRecoThread(this);
		Log.d(TAG,"Asr ready");
	}
	//private boolean isRecoreStart = false;
	public void startRecord(){
		Log.d(TAG,"startRecord()");
		if(!AsrRecord.isThreadRun){
			Asr.reStart();			
		}
	}
	
    private void sendPwmMsg(byte level){
		byte[] sendcmd  = new byte[2];
		sendcmd[0] = (byte)0x03;
		sendcmd[1] = level;
		sendData(sendcmd);
		mCurrentLevelText.setText(getString(R.string.ai_talk_current_level) + level);
    }
    private void sendModeMsg(byte mode){
    	byte[] sendcmd  = new byte[2];
		sendcmd[0] = (byte)0x05;//change by guoliangliang 20150202
		sendcmd[1] = mode;
		sendData(sendcmd);
		mCurrentModeText.setText(getString(R.string.ai_talk_current_mode) + /*mModeStrings[*/mode/*]*/);
    }

/*	private class RecordAudio extends AsyncTask<Void, double[], Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				int bufferSize = AudioRecord.getMinBufferSize(frequency,
						channelConfiguration, audioEncoding);

				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, frequency,
						channelConfiguration, audioEncoding, bufferSize);

				short[] buffer = new short[blockSize];
				double[] toTransform = new double[blockSize];

				audioRecord.startRecording();

				while (started) {
					int bufferReadResult = audioRecord.read(buffer, 0,
							blockSize);

					for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
						toTransform[i] = (double) buffer[i] / 32768.0; // signed
						// 16
						// bit
					}

					transformer.ft(toTransform);
					publishProgress(toTransform);
				}

				audioRecord.stop();
			} catch (Throwable t) {
				Log.e("AudioRecord", "Recording Failed");
			}

			return null;
		}
		private final int MAX_PWM_LEVEL = 25;
		private int mPwmLevel = 0;
		
		protected void onProgressUpdate(double[]... toTransform) {

		    long currentUpdateTime = SystemClock.elapsedRealtime();  
		    long timeInterval = currentUpdateTime - lastUpdateTime; 
		    if(timeInterval < UPTATE_INTERVAL_TIME) {  
		         return;  
		    }
		    
			int maxVoicelevel = 0;
			int minVoicelevel = 10000000;			
			mCanvas.drawColor(Color.BLACK);
//绘制开始
			int lastX = 100;
			int lastY = 100;
			mCanvas.drawLine(0,lastY-1,lastX,lastY,mPaint);//绘制头
			for(int i = 0; i < toTransform[0].length; i++){
				
				int level = (int)(toTransform[0][i] * 10);
				if(maxVoicelevel < level){
					maxVoicelevel = level;
				}
				if(minVoicelevel > level){
					minVoicelevel = level;
				}
				if((i+1)%8 == 0){
					int newX = 2*i-1+100;
					int newY = (int) (100 - (toTransform[0][i] * 10));
					if(newY < 0)
						newY = 0;
					if(newY > 200)
						newY = 200;
					mCanvas.drawLine(lastX, lastY, newX, newY, mPaint);//绘制曲线
					lastX = newX;
					lastY = newY;
				}
				
			}
			mCanvas.drawLine(lastX,lastY,lastX+8,100,mPaint);//绘制尾
			lastX = lastX + 8;
			mCanvas.drawLine(lastX-1,99,712,100,mPaint);
//绘制结束

			mVoiceLine.invalidate();
		}
	}*/
    
    /*wangfei added begin*/
    private void startVoiceLineAnimation() {
        mVoiceLine.setVisibility(View.INVISIBLE);
        mVoiceLineAnim.setVisibility(View.VISIBLE);
        
        AnimationDrawable animationDrawable = (AnimationDrawable) mVoiceLineAnim.getDrawable();
        if(animationDrawable != null) animationDrawable.start();
    }
    
    private void stopVoiceLineAnimation() {
        mVoiceLineAnim.setVisibility(View.INVISIBLE);
        mVoiceLine.setVisibility(View.VISIBLE);
        
        AnimationDrawable animationDrawable = (AnimationDrawable) mVoiceLineAnim.getDrawable();
        if(animationDrawable != null && animationDrawable.isRunning()) {
        	animationDrawable.stop();
        }
    }
    /*wangfei added end*/
}