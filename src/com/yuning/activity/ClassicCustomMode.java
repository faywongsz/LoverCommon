package com.yuning.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;
import com.yuning.util.sendThread;


public class ClassicCustomMode extends BaseActivity{
	private static final String TAG = "PinlvCustomMode";
	private ImageView mItemBtns[];	
	private ImageView mChoiceBtns[];	
	private int[] mSelectedImages;
	private final int PINLV_MODE_COUNT = 16;
	private int mZhendongFrameWidth = 0;
	private int mZhendongFrameHeight = 0;
	private static final byte[] mCMDstop= {(byte)0xff,(byte)0xff};

	private WakeLock mWakeLock = null;
	private ImageView backIcon;
	private ImageView mMoveItemIcon;
	private FrameLayout.LayoutParams mLpMoveItemIcon;
	private ImageView mMoveChoiceIcon;
	private FrameLayout.LayoutParams mLpMoveChoiceIcon;
	private FrameLayout mZhendongContent;

	private ImageView mZhendongCustom01;
	private ImageView mZhendongCustom02;
	private ImageView mZhendongCustom03;
	private ImageView mZhendongCustom04;

	private int mListStartY;
	private int mListEndY;
	
	private int mList1StartX;
	private int mList1EndX;
	
	private int mList2StartX;
	private int mList2EndX;
	
	private int mList3StartX;
	private int mList3EndX;
	
	private int mList4StartX;
	private int mList4EndX;
	
	private int mListChoices[];
	
	private long [] mTimesAll = new long[]{2000,3000,3000,2000,  1000,1000,5000,1000,  2000,1000,2000,3000,  1000,4000,6000,1000};
	private long [] mTimesSelected = new long[4];
	private int mCurrentPlaying = 0;
	private final int PLAY = 1;
	private final int STOP = 0;
	private boolean mIsPlaying = false;
	private ImageView mBtnStartStop;
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//super.handleMessage(msg);
			switch (msg.what) {
			case PLAY:
				byte[] sendcmd  = new byte[2];
                sendcmd[0] = (byte)0x01;
                //sendcmd[1] = (byte)mCurrentSelected;
                while(mListChoices[mCurrentPlaying] == -1){
                	mCurrentPlaying = (mCurrentPlaying + 1) % 4;
                }
				Log.d(TAG,"play mCurrentPlaying="+mCurrentPlaying);
                sendcmd[1] = (byte)mListChoices[mCurrentPlaying];
                sendThread.sendData(sendcmd);	
                mHandler.sendEmptyMessageDelayed(PLAY, mTimesSelected[mCurrentPlaying]);
                mCurrentPlaying = (mCurrentPlaying + 1) % 4;
				break;
			case STOP:
				Log.d(TAG,"stop mCurrentPlaying="+mCurrentPlaying);
				sendThread.sendData(mCMDstop);
				break;
			default:
				break;
			}
		}
		
	};
	
	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		setContentView(R.layout.classic_custom_mode);
		
		mListChoices = new int[]{-1,-1,-1,-1};
		mSelectedImages = new int[]{R.drawable.zhendong_custom_selected01,R.drawable.zhendong_custom_selected02,R.drawable.zhendong_custom_selected03,R.drawable.zhendong_custom_selected04,
				R.drawable.zhendong_custom_selected05,R.drawable.zhendong_custom_selected06,R.drawable.zhendong_custom_selected07,R.drawable.zhendong_custom_selected08,
				R.drawable.zhendong_custom_selected09,R.drawable.zhendong_custom_selected10,R.drawable.zhendong_custom_selected11,R.drawable.zhendong_custom_selected12,
				R.drawable.zhendong_custom_selected13,R.drawable.zhendong_custom_selected14,R.drawable.zhendong_custom_selected15,R.drawable.zhendong_custom_selected16};
		
		backIcon = (ImageView)findViewById(R.id.back_icon);
		backIcon.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				finish();
				return;
			}
		});
		mItemBtns = new ImageView[PINLV_MODE_COUNT];
		mItemBtns[0] = (ImageView)findViewById(R.id.zhendong_item_img_01);
		mItemBtns[1] = (ImageView)findViewById(R.id.zhendong_item_img_02);
		mItemBtns[2] = (ImageView)findViewById(R.id.zhendong_item_img_03);
		mItemBtns[3] = (ImageView)findViewById(R.id.zhendong_item_img_04);
		mItemBtns[4] = (ImageView)findViewById(R.id.zhendong_item_img_05);
		mItemBtns[5] = (ImageView)findViewById(R.id.zhendong_item_img_06);
		mItemBtns[6] = (ImageView)findViewById(R.id.zhendong_item_img_07);
		mItemBtns[7] = (ImageView)findViewById(R.id.zhendong_item_img_08);
		mItemBtns[8] = (ImageView)findViewById(R.id.zhendong_item_img_09);
		mItemBtns[9] = (ImageView)findViewById(R.id.zhendong_item_img_10);
		mItemBtns[10] = (ImageView)findViewById(R.id.zhendong_item_img_11);
		mItemBtns[11] = (ImageView)findViewById(R.id.zhendong_item_img_12);
		mItemBtns[12] = (ImageView)findViewById(R.id.zhendong_item_img_13);
		mItemBtns[13] = (ImageView)findViewById(R.id.zhendong_item_img_14);
		mItemBtns[14] = (ImageView)findViewById(R.id.zhendong_item_img_15);
		mItemBtns[15] = (ImageView)findViewById(R.id.zhendong_item_img_16);
		mBtnStartStop = (ImageView)findViewById(R.id.zhendong_custom_start);
		//mbtns[PINLV_MODE_COUNT] = (Button)findViewById(R.id.btn_keyboard_stop);	
		
		mChoiceBtns = new ImageView[4];
		mZhendongContent = (FrameLayout)findViewById(R.id.zhendong_content);
		mChoiceBtns[0] = mZhendongCustom01 = (ImageView)findViewById(R.id.zhendong_custom_list_01);
		mChoiceBtns[1] = mZhendongCustom02 = (ImageView)findViewById(R.id.zhendong_custom_list_02);
		mChoiceBtns[2] = mZhendongCustom03 = (ImageView)findViewById(R.id.zhendong_custom_list_03);
		mChoiceBtns[3] = mZhendongCustom04 = (ImageView)findViewById(R.id.zhendong_custom_list_04);
		
		mMoveItemIcon = (ImageView)findViewById(R.id.zhendong_move_item_icon);
		mLpMoveItemIcon = (FrameLayout.LayoutParams)mMoveItemIcon.getLayoutParams();
		mMoveChoiceIcon = (ImageView)findViewById(R.id.zhendong_move_choice_icon);
		mLpMoveChoiceIcon = (FrameLayout.LayoutParams)mMoveChoiceIcon.getLayoutParams();
		for (int i = 0; i < (PINLV_MODE_COUNT); i++){
			final int x = i;
			mItemBtns[i].setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					int action = event.getAction();
					int rateX = 11 + x % 4 * 51;
					int rateY = 32 + x / 4 * 151;
					int touchX = (int)event.getX() + mZhendongFrameWidth * rateX / 216;
					int touchY = (int)event.getY() + mZhendongFrameHeight * rateY / 834;
					if(action == MotionEvent.ACTION_DOWN){
						mMoveItemIcon.setImageDrawable(mItemBtns[x].getDrawable());
					}else if(action == MotionEvent.ACTION_MOVE){
						mLpMoveItemIcon.leftMargin = touchX - mLpMoveItemIcon.width / 2;
						mLpMoveItemIcon.rightMargin = mZhendongFrameWidth - mLpMoveItemIcon.leftMargin - mLpMoveItemIcon.width;
						mLpMoveItemIcon.topMargin = touchY - mLpMoveItemIcon.height / 2;
						mMoveItemIcon.setLayoutParams(mLpMoveItemIcon);
						mMoveItemIcon.setVisibility(View.VISIBLE);
						setlistImages(touchX, touchY, -1);
					}else if(action == MotionEvent.ACTION_UP){
						setlistImages(touchX, touchY, x);
						mMoveItemIcon.setVisibility(View.GONE);
						Log.d(TAG,"mListChoices[0]="+mListChoices[0]+"  mListChoices[1]="+mListChoices[1]+"  mListChoices[2]="+mListChoices[2]+"  mListChoices[3]="+mListChoices[3]);
						Log.d(TAG,"mTimesSelected[0]="+mTimesSelected[0]+"  mTimesSelected[1]="+mTimesSelected[1]+"  mTimesSelected[2]="+mTimesSelected[2]+"  mTimesSelected[3]="+mTimesSelected[3]);
					}
					return true;
				}
			});
		}
		for(int i = 0; i < mChoiceBtns.length; i ++){
			final int x = i;
			mChoiceBtns[i].setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					int action = event.getAction();
					int rateX = 96 + x * 195;
					int rateY = 700;
					int touchX = (int)event.getX() + mZhendongFrameWidth * rateX / 1080;
					int touchY = (int)event.getY() + mZhendongFrameHeight * rateY / 834;
					if(mListChoices[x] != -1){
						if(action == MotionEvent.ACTION_DOWN){
							mMoveChoiceIcon.setImageDrawable(mChoiceBtns[x].getDrawable());
						}else if(action == MotionEvent.ACTION_MOVE){
							mLpMoveChoiceIcon.leftMargin = touchX - mLpMoveChoiceIcon.width / 2;
							mLpMoveChoiceIcon.rightMargin = mZhendongFrameWidth - mLpMoveChoiceIcon.leftMargin - mLpMoveChoiceIcon.width;
							mLpMoveChoiceIcon.topMargin = touchY - mLpMoveChoiceIcon.height / 2;
							mMoveChoiceIcon.setLayoutParams(mLpMoveChoiceIcon);
							mMoveChoiceIcon.setVisibility(View.VISIBLE);
							mChoiceBtns[x].setImageResource(R.drawable.zhendong_custom_unselected);
							//deleteListImage(touchX, touchY, x);
						}else if(action == MotionEvent.ACTION_UP){
							deleteListImage(touchX, touchY, x);
							mMoveChoiceIcon.setVisibility(View.GONE);
						}
					}
					return true;
				}
			});
		}
	}
	
	private void deleteListImage(int touchX, int touchY ,int choice){
		Log.d(TAG,"deleteChoice, touchX="+touchX+"  touchY="+touchY+"  choice="+choice);
		boolean shouldDelete = false;
		if(touchY < mListStartY || touchY > mListEndY){
			shouldDelete = true;
			Log.d(TAG,"deleteChoice, shouldDelete = true,  mListStartY="+mListStartY+"  mListEndY="+mListEndY+"  touchY < mListStartY || touchY > mListEndY");
		}else{
			Log.d(TAG,"deleteChoice, mList1StartX="+mList1StartX+"  mList1EndX="+mList1EndX);
			Log.d(TAG,"deleteChoice, mList2StartX="+mList2StartX+"  mList2EndX="+mList2EndX);
			Log.d(TAG,"deleteChoice, mList3StartX="+mList3StartX+"  mList3EndX="+mList3EndX);
			Log.d(TAG,"deleteChoice, mList4StartX="+mList4StartX+"  mList4EndX="+mList4EndX);
			int startX = mList1StartX;
			int endX = mList1EndX;
			if(choice == 1){
				startX = mList2StartX;
				endX = mList2EndX;
			}else if(choice == 2){
				startX = mList3StartX;
				endX = mList3EndX;
			}else if(choice == 3){
				startX = mList4StartX;
				endX = mList4EndX;
			}
			if(touchX < startX || touchX > endX){
				shouldDelete = true;
				Log.d(TAG,"deleteChoice, shouldDelete = true,  touchX < startX || touchY > endX");
			}
		}
		if(shouldDelete){
			boolean hasAnotherChoice = false;
			for(int i = 0; i < mListChoices.length; i++){
				if(i == choice){
					continue;
				}
				if(mListChoices[i] != -1){
					hasAnotherChoice = true;
				}
			}
			if(hasAnotherChoice == false && mIsPlaying){
				mBtnStartStop.callOnClick();
			}
			mListChoices[choice] = -1;
		}else{
			mChoiceBtns[choice].setImageDrawable(mMoveChoiceIcon.getDrawable());;
		}
	}
	
	private void setlistImages(int touchX, int touchY, int choice){
		if(touchY > mListStartY && touchY < mListEndY){
			if(touchX >= mList1StartX && touchX <= mList1EndX){
				if(choice != -1){
					mListChoices[0] = choice;
					mTimesSelected[0] = mTimesAll[choice];
					mZhendongCustom01.setImageResource(mSelectedImages[choice]);
				}else{
					mZhendongCustom02.setImageResource(mListChoices[1] != -1 ? mSelectedImages[mListChoices[1]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom03.setImageResource(mListChoices[2] != -1 ? mSelectedImages[mListChoices[2]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom04.setImageResource(mListChoices[3] != -1 ? mSelectedImages[mListChoices[3]] : R.drawable.zhendong_custom_unselected);
				}
			}else if(touchX >= mList2StartX && touchX <= mList2EndX){
				if(choice != -1){
					mListChoices[1] = choice;
					mTimesSelected[1] = mTimesAll[choice];
					mZhendongCustom02.setImageResource(mSelectedImages[choice]);
				}else{
					mZhendongCustom01.setImageResource(mListChoices[0] != -1 ? mSelectedImages[mListChoices[0]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom03.setImageResource(mListChoices[2] != -1 ? mSelectedImages[mListChoices[2]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom04.setImageResource(mListChoices[3] != -1 ? mSelectedImages[mListChoices[3]] : R.drawable.zhendong_custom_unselected);
				}
			}else if(touchX >= mList3StartX && touchX <= mList3EndX){
				if(choice != -1){
					mListChoices[2] = choice;
					mTimesSelected[2] = mTimesAll[choice];
					mZhendongCustom03.setImageResource(mSelectedImages[choice]);
				}else{
					mZhendongCustom01.setImageResource(mListChoices[0] != -1 ? mSelectedImages[mListChoices[0]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom02.setImageResource(mListChoices[1] != -1 ? mSelectedImages[mListChoices[1]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom04.setImageResource(mListChoices[3] != -1 ? mSelectedImages[mListChoices[3]] : R.drawable.zhendong_custom_unselected);
				}
			}else if(touchX >= mList4StartX && touchX <= mList4EndX){
				if(choice != -1){
					mListChoices[3] = choice;
					mTimesSelected[3] = mTimesAll[choice];
					mZhendongCustom04.setImageResource(mSelectedImages[choice]);
				}else{
					mZhendongCustom01.setImageResource(mListChoices[0] != -1 ? mSelectedImages[mListChoices[0]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom02.setImageResource(mListChoices[1] != -1 ? mSelectedImages[mListChoices[1]] : R.drawable.zhendong_custom_unselected);
					mZhendongCustom03.setImageResource(mListChoices[2] != -1 ? mSelectedImages[mListChoices[2]] : R.drawable.zhendong_custom_unselected);
				}
			}else{
				resetlistImages();
			}
		}else{
			resetlistImages();
		}
	}
	
	private void resetlistImages(){
		mZhendongCustom01.setImageResource(mListChoices[0] != -1 ? mSelectedImages[mListChoices[0]] : R.drawable.zhendong_custom_unselected);
		mZhendongCustom02.setImageResource(mListChoices[1] != -1 ? mSelectedImages[mListChoices[1]] : R.drawable.zhendong_custom_unselected);
		mZhendongCustom03.setImageResource(mListChoices[2] != -1 ? mSelectedImages[mListChoices[2]] : R.drawable.zhendong_custom_unselected);
		mZhendongCustom04.setImageResource(mListChoices[3] != -1 ? mSelectedImages[mListChoices[3]] : R.drawable.zhendong_custom_unselected);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		if(hasFocus == true && mZhendongFrameWidth == 0){
			mZhendongFrameWidth = mZhendongContent.getWidth();
			mZhendongFrameHeight = mZhendongContent.getHeight();
			mLpMoveItemIcon.width = mLpMoveItemIcon.height = mZhendongFrameWidth * 41 / 216;
			mLpMoveChoiceIcon.width = mZhendongFrameWidth * 19 / 216;
			mLpMoveChoiceIcon.height = mZhendongFrameHeight * 97 / 834;
			
			mListStartY = mZhendongFrameHeight * 700 / 834;
			mListEndY = mZhendongFrameHeight * 797 / 834;
			Log.d(TAG,"mListStartY="+mListStartY+" mListEndY="+mListEndY);
			
			mList1StartX = mZhendongFrameWidth * (56 + 40 - 30) / 1080;
			mList1EndX = mList1StartX + mZhendongFrameWidth * (95 + 60) / 1080;
			Log.d(TAG,"mList1StartX="+mList1StartX+"  mList1EndX="+mList1EndX);
			
			mList2StartX = mList1StartX + mZhendongFrameWidth * 195 / 1080;
			mList2EndX = mList2StartX + mZhendongFrameWidth * (95 + 60) / 1080;
			Log.d(TAG,"mList2StartX="+mList2StartX+"  mList2EndX="+mList2EndX);
			
			mList3StartX = mList2StartX + mZhendongFrameWidth * 195 / 1080;
			mList3EndX = mList3StartX + mZhendongFrameWidth * (95 + 60) / 1080;
			Log.d(TAG,"mList3StartX="+mList3StartX+"  mList3EndX="+mList3EndX);
			
			mList4StartX = mList3StartX + mZhendongFrameWidth * 195 / 1080;
			mList4EndX = mList4StartX + mZhendongFrameWidth * (95 + 60) / 1080;
			Log.d(TAG,"mList4StartX="+mList4StartX+"  mList4EndX="+mList4EndX);
		}
	}

	public void onResume()
	{
		super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
			mWakeLock.setReferenceCounted(false);
			mWakeLock.acquire();
		}	
		
		if(!isConnect()){
			setResult(Activity.DEFAULT_KEYS_SEARCH_LOCAL, null);			
			Toast.makeText(this, getString(R.string.msg_please_connect), 0).show();				
			finish();
			return;
		}
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);
		
		mHandler.removeMessages(PLAY);
		sendThread.sendData(mCMDstop);
		mIsPlaying = false;
		mBtnStartStop.setImageResource(R.drawable.zhendong_custom_play_selector);
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
			setResult(Activity.DEFAULT_KEYS_SEARCH_LOCAL, null);
			finish();
		}
		return super.onKeyDown(KeyCode, keyevent);
	}

    public void onBtnClick_Array(View view){
		int id = view.getId();
		
		for (int i = 0; i < (PINLV_MODE_COUNT + 1); i++){
			if (mItemBtns[i].getId() == id){
				//if(i == PINLV_MODE_COUNT){
				//	sendThread.sendData(mCMDstop);
				//}else{	
                    byte[] sendcmd  = new byte[2];
                    sendcmd[0] = (byte)0x01;
                    sendcmd[1] = (byte)i;
                    sendThread.sendData(sendcmd);					
				//}
			    break;
			}
		}
		
	}
    
    public void onBtnStartPauseClick(View view){
    	mHandler.removeMessages(PLAY);
    	mHandler.removeMessages(STOP);
    	if(mIsPlaying){
    		mCurrentPlaying = 0;
    		mHandler.sendEmptyMessage(STOP);
    		((ImageView)view).setImageResource(R.drawable.zhendong_custom_play_selector);
    		mIsPlaying = !mIsPlaying;
    	}else{
    		boolean hasChoice = false;
    		for(int i = 0; i < mListChoices.length; i++){
    			if(mListChoices[i] != -1){
    				hasChoice = true;
    				break;
    			}
    		}
    		if(hasChoice){
    			mHandler.sendEmptyMessage(PLAY);
    			((ImageView)view).setImageResource(R.drawable.zhendong_custom_stop_selector);
    			mIsPlaying = !mIsPlaying;
    		}else{
    			Toast.makeText(this, R.string.zhendong_custom_toast, Toast.LENGTH_SHORT).show();
    		}
    	}
    }
}

