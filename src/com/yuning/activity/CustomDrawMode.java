package com.yuning.activity;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.yuning.lovercommon.R;
import com.umeng.analytics.MobclickAgent;


public class CustomDrawMode extends BaseModeActivity {
	private static final String TAG = CustomDrawMode.class.getSimpleName();
	
	private static final int SHAKE_TIMES_COUNT = 50;
	private static final int MAX_SHAKE_LEVEL = 25;
	private static final long EACH_SHAKE_TIME = 300;
	private final int PLAY = 1;
	private final int STOP = 0;
	private ImageView mImageView;
	private Bitmap mBitmap;
	private Canvas mCanvas;
	private Paint mPaint;
	private int[] mTouchDatas;
	private int[] mShakeDatas;
	private int mImageWidth;
	private int mImageHeight;
	private int mCurrentPlaying = 0;
	private boolean mHasTouched = false;
	private boolean mIsTouching = false;
	private boolean mHasStartNew = false;
	private boolean mIsPlaying = false;
	private ImageView mBtnStartStop;
	private int COLOR_DRAW_BG = 0xffff5f95;
	private int COLOR_DRAW = 0xffa81b4b;
	private int COLOR_DRAW_PLAY = 0xffddec37;
	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PLAY:
				while(mShakeDatas[mCurrentPlaying] == -1){
                	mCurrentPlaying = (mCurrentPlaying + 1) % SHAKE_TIMES_COUNT;
                }
				byte level = (byte) getZhendongStrenthByTouchY(mShakeDatas[mCurrentPlaying]);
				Log.d(TAG,"play mCurrentPlaying="+mCurrentPlaying+"  level="+level);
				setFunLevelMode(level);
				if(mIsTouching == false && mHasStartNew == true){
					drawImage();
				}
				mHandler.sendEmptyMessageDelayed(PLAY, EACH_SHAKE_TIME);
                mCurrentPlaying = (mCurrentPlaying + 1) % SHAKE_TIMES_COUNT;
				break;
			case STOP:
				stopVibration();
				break;
			default:
				break;
			}
		}
	};
	
	private int getZhendongStrenthByTouchY(int touchY){
		return (mImageHeight - touchY) * MAX_SHAKE_LEVEL / mImageHeight;
	}
	
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.custom_draw_mode);
		initTitleBar(R.string.hand_draw_mode, 0, 0, null);
		findViews();
		init();
		setListeners();
	}

	private void findViews(){
		mImageView = (ImageView)findViewById(R.id.imageView);
		mBtnStartStop = (ImageView)findViewById(R.id.custom_draw_start_stop);
	}
	
	private void init(){
		mPaint = new Paint();
		mPaint.setStrokeWidth(6f);
		mPaint.setColor(COLOR_DRAW);
		mShakeDatas = new int[SHAKE_TIMES_COUNT];
	}
	
	private void resetTouchDatas(){
		for(int i = 0; i < mTouchDatas.length; i++){
			mTouchDatas[i] = -1;
		}
	}
	
	private void resetShakeDatas(){
		for(int i = 0; i < mShakeDatas.length; i++)
			mShakeDatas[i] = -1;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		mImageWidth = mImageView.getWidth();
		mImageHeight = mImageView.getHeight();
		if(mBitmap == null){
			mBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mImageView.setImageBitmap(mBitmap);
		}
		Log.d(TAG,"getWidth()="+mImageWidth+"  getHeight()="+mImageHeight);
		mTouchDatas = new int[mImageWidth];
		for(int i = 0; i < mImageWidth; i++) {
			mTouchDatas[i] = -1;
		}
		
		if(!hasFocus) {
			mHandler.removeMessages(PLAY);
			mHandler.sendEmptyMessage(STOP);
			mIsPlaying = false;
			mHasTouched = false;
			mBtnStartStop.setImageResource(R.drawable.hand_draw_play_selector);
		}
	}

	private void setListeners(){	
		mImageView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					mHasTouched = true;
					mIsTouching = true;
					mHasStartNew = false;
					resetTouchDatas();
					if(mIsPlaying) {
						mBtnStartStop.callOnClick();
					}
					break;
				case MotionEvent.ACTION_MOVE:
					int touchX = (int) event.getX();
					int touchY = (int) event.getY();
					if(touchX < 0) touchX = 0;
					if(touchX >= mImageWidth) touchX = mImageWidth - 1;
					if(touchY < 0) touchY = 0;
					if(touchY >= mImageHeight) touchY = mImageHeight - 1;
					Log.d(TAG,"touchX="+touchX+"  touchY="+touchY);
					mTouchDatas[touchX] = touchY;
					for(int i = touchX + 1; i < mImageWidth; i++){
						mTouchDatas[i] = -1;
					}
					drawImage();
					break;
				case MotionEvent.ACTION_UP:
					mIsTouching = false;
					//setShakeDatas();
					break;
				default:
					break;
				}
				return true;
			}
		});
	}
	
	private void setShakeDatas(){
		//mHandler.removeMessages(PLAY);
		//mHandler.sendEmptyMessage(STOP);
		resetShakeDatas();
		boolean hasData = false;
		for(int i = 0; i < mShakeDatas.length; i++){
			int touchDataIndex = mImageWidth * i / mShakeDatas.length;
			if(mTouchDatas[touchDataIndex] != -1){
				mShakeDatas[i] = mTouchDatas[touchDataIndex];
			}else{
				mShakeDatas[i] = getShakeDataByIndex(touchDataIndex);
			}
			if(mShakeDatas[i] != -1)
				hasData = true;
			Log.d(TAG,"touchDataIndex="+touchDataIndex+"  "+mShakeDatas[i]+"  ");
		}
		/*if touch data is very short begin*/
		if(hasData == false){
			int lastDataIndex = mImageWidth;
			for(int i = mTouchDatas.length - 1; i >= 0; i--){
				if(mTouchDatas[i] != -1){
					lastDataIndex = i;
					break;
				}
			}
			mShakeDatas[lastDataIndex * MAX_SHAKE_LEVEL / mImageWidth] = mTouchDatas[lastDataIndex];
		}
		/*if touch data is very short end*/
		
	}
	
	private int getShakeDataByIndex(int index){
		int data = -1;
		int firstDataIndex = -1;
		int lastDataIndex = mImageWidth;
		for(int i = 0; i < mTouchDatas.length; i++){
			if(mTouchDatas[i] != -1){
				firstDataIndex = i;
				break;
			}
		}
		for(int i = mTouchDatas.length - 1; i >= 0; i--){
			if(mTouchDatas[i] != -1){
				lastDataIndex = i;
				break;
			}
		}
		//Log.d(TAG,"firstDataIndex="+firstDataIndex+"  lastDataIndex="+lastDataIndex);
		if(index < firstDataIndex){
			return -1;
		}else if(index > lastDataIndex){
			return -1;
		}
		int leftIndex = index;
		int rightIndex = index;
		do{
			leftIndex--;
		}while(mTouchDatas[leftIndex] == -1);
		do{
			rightIndex++;
		}while(mTouchDatas[rightIndex] == -1);
		data = (mTouchDatas[leftIndex] + mTouchDatas[rightIndex]) / 2;
		return data;
	}
	
	private void drawImage(){
		mCanvas.drawColor(COLOR_DRAW_BG);
		int oldX = 0;
		while(oldX < mTouchDatas.length && mTouchDatas[oldX] < 0){
			oldX++;
		}
		int lastX = mImageWidth - 1;
		for(int i = mTouchDatas.length - 1; i >= 0; i--){
			if(mTouchDatas[i] != -1){
				lastX = i;
				break;
			}
		}
		for(int newX = oldX + 1; newX <= lastX; newX++){
			if(mTouchDatas[newX] == -1)
				continue;
			if(mIsTouching == false && (newX * SHAKE_TIMES_COUNT / mImageWidth) <= mCurrentPlaying){
				mPaint.setColor(COLOR_DRAW_PLAY);
			}else{
				mPaint.setColor(COLOR_DRAW);
			}
			mCanvas.drawLine(oldX, mTouchDatas[oldX], newX, mTouchDatas[newX], mPaint);
			oldX = newX;
		}
		mImageView.invalidate();
	}
	
	public void onResume(){
		super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
		if(mCanvas != null){
			mCanvas.drawColor(COLOR_DRAW_BG);
			mImageView.invalidate();
		}

		/*wangfei added begin*/
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		/*wangfei added end*/
		
		keepScreenOn(true);
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
		mHandler.removeMessages(PLAY);
		mHandler.sendEmptyMessage(STOP);
		mIsPlaying = false;
		mHasTouched = false;
		mBtnStartStop.setImageResource(R.drawable.hand_draw_play_selector);
		
		keepScreenOn(false);
		stopVibration();	
	}
	
	public void onBtnStart(View view){
		if(!isConnect()) {
			showOpenDeviceDialog();
			return;
		}
		
		mHandler.removeMessages(PLAY);
		mHandler.sendEmptyMessage(STOP);
		if(mIsPlaying == false){
			mHandler.removeMessages(STOP);
			mCurrentPlaying = 0;
			mHasStartNew = true;
			if(mHasTouched){
				setShakeDatas();
				mHandler.sendEmptyMessage(PLAY);
				mIsPlaying = true;
				((ImageView)view).setImageResource(R.drawable.hand_draw_stop_selector);
				saveLastTouchDatas();
			}else{
				boolean hasLastTouchDatas = getlastTouchDatas();
				if(hasLastTouchDatas){
					drawImage();
					setShakeDatas();
					mHandler.sendEmptyMessage(PLAY);
					mIsPlaying = true;
					((ImageView)view).setImageResource(R.drawable.hand_draw_stop_selector);
				}
			}
		}else{
			mIsPlaying = false;
			((ImageView)view).setImageResource(R.drawable.hand_draw_play_selector);
		}
	}
	
	public boolean getlastTouchDatas(){
		String lastDatas = getStrData(CUSTOMDRAW_MODE_DATA);
		if(lastDatas == null){
			Log.e(TAG,"no lastDatas");
			return false;
		}else{
			String strDatas[] = lastDatas.split(",");
			if(mTouchDatas == null)
				mTouchDatas = new int[lastDatas.length()];
			for(int i = 0; i < strDatas.length; i++){
				mTouchDatas[i] = Integer.parseInt(strDatas[i]);
			}
			return true;
		}
	}
	
	private void saveLastTouchDatas(){
		String datas = mTouchDatas[0] + "";
		for(int i = 1; i < mTouchDatas.length; i++){
			datas = datas + "," + mTouchDatas[i];
		}
		saveStrData(CUSTOMDRAW_MODE_DATA, datas);
	}
}

