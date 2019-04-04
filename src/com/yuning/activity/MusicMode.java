package com.yuning.activity;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.yuning.Service.MusicPlayService;
import com.yuning.lovercommon.R;
import com.yuning.ui.MusicCover;
import com.yuning.ui.MusicCoverWorkspace;
import com.yuning.util.MediaInfo;

public class MusicMode extends BaseModeActivity implements View.OnClickListener, MusicCoverWorkspace.onPageChangedListener{
	private static final String TAG = MusicMode.class.getSimpleName();
	
	private static final String MUSIC_TIPS = "music_tips";
	private static final int REQUEST_MUSIC_ADD = 1;
	
	private MusicPlayService mPlayService;
	private Visualizer mVisualizer;
	private static final int PINPU_MAX_COUNT = 5;
	private int mPinpuCount = 0;
    //private int mPinpuValue = 0;
    
	private MusicCoverWorkspace mWorkspace;
	private MusicCover mCurrentCover;
	//private View mMusicSeekbarLayout;
	private SeekBar mMusicSeekBar;
	private ImageView mImageMusicOper;
	private ImageView mImageMusicPrev;
	private ImageView mImageMusicNext;
	private TextView mTextTitle;
	private TextView mTextArtist;
	private TextView mTextPlayTime;
	private TextView mTextDuration;
	private MusicOperState mOperState = MusicOperState.MUSIC_PAUSE;
	private ArrayList<MediaInfo> mMusicInfos = new ArrayList<MediaInfo>();
	private int mMusicCount = 0;
	private int mMusicIndex = -1;
	private boolean mbSeekProgress = false;
	private int mSeekPosition = 0;
	
	private static final int UPDATE_TIME = 1;
	private static final int UPDATE_TIME_INTERVAL = 50;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch(what) {
				case UPDATE_TIME:
					if(mPlayService != null && mPlayService.isPlaying()) {
						updatePlayTime(mPlayService.getCurrentPosition());
					}
					sendEmptyMessageDelayed(UPDATE_TIME, UPDATE_TIME_INTERVAL);
					break;
			}
		}
	};
	
	private enum MusicOperState {
		MUSIC_ADD,
		MUSIC_PLAY,
		MUSIC_PAUSE,
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mPlayService = null;
		}
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mPlayService = ((MusicPlayService.LocalBinder)service).getService();
			selectMusic(mMusicIndex);
		}
	};
	
	private PhoneStateChangedReceiver mPhoneStateChangedReceiver; 
	private boolean mbPlayingBeforePhone = false;
    public class PhoneStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
             Log.d(TAG, "action = " + action);
 
             if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            	 if(mPlayService != null && mPlayService.isPlaying()) {
            		 mbPlayingBeforePhone = true;
            		 pauseMusic();
            	 }
             } else {
            	 TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            	 
            	 if(tm != null) {
            		 int state = tm.getCallState();
            		 Log.d(TAG, "state = " + state);
            		 
            		 switch(state) {
            		 	case TelephonyManager.CALL_STATE_RINGING :
            		 	case TelephonyManager.CALL_STATE_OFFHOOK :
            		 		if(mPlayService != null && mPlayService.isPlaying()) {
            		 			mbPlayingBeforePhone = true;
            		 			pauseMusic();
            		 		}
            		 		break;
            		 	case TelephonyManager.CALL_STATE_IDLE :
            		 		if(mbPlayingBeforePhone) {
            		 			mbPlayingBeforePhone = false;
            		 			playMusic();
            		 		}
            		 		break;
            		 }
            	 }
             }
        }
    }
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_mode);
		
		getMusicInfos();
		
		initTitleBar(R.string.music_mode, R.drawable.music_title_add_selector, 0, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addMusic();
			}
		});
		initViews();
		
		mWorkspace.setToScreen(0);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC); 
		bindService(new Intent(this, MusicPlayService.class), mConnection, Context.BIND_AUTO_CREATE);
		
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
		intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		mPhoneStateChangedReceiver = new PhoneStateChangedReceiver();
		registerReceiver(mPhoneStateChangedReceiver, intentFilter);
	}
	
	private void initViews() {
		mWorkspace = (MusicCoverWorkspace) findViewById(R.id.music_cover_workspace);
		mWorkspace.setonPageChangedListener(this);
		//mMusicSeekbarLayout = findViewById(R.id.music_seekbar_layout);
		mMusicSeekBar = (SeekBar) findViewById(R.id.music_seekbar);
		mMusicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(mbSeekProgress) {
					if(mPlayService != null) {
						mPlayService.seekTo(mSeekPosition);
					}
					mbSeekProgress = false;
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mbSeekProgress = true;
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(mPlayService != null && mbSeekProgress) {
					mSeekPosition = progress;
					mTextPlayTime.setText(MediaInfo.toTime(mSeekPosition));
				}
			}
		});
		
		mTextTitle = (TextView) findViewById(R.id.music_title);
		mTextArtist = (TextView) findViewById(R.id.music_artist);
		mTextPlayTime = (TextView) findViewById(R.id.music_play_time);
		mTextDuration = (TextView) findViewById(R.id.music_duration);
		mImageMusicOper = (ImageView) findViewById(R.id.music_operation);
		mImageMusicPrev = (ImageView) findViewById(R.id.music_prev);
		mImageMusicNext = (ImageView) findViewById(R.id.music_next);
		mImageMusicOper.setOnClickListener(this);
		mImageMusicPrev.setOnClickListener(this);
		mImageMusicNext.setOnClickListener(this);
		
		mWorkspace.removeAllViews();
		if(mMusicCount > 0) {
			for(int i = 0; i < mMusicCount; i++) {
				addMusicCover(i);
			}
		} else {
			addMusicCover(-1);
			//mMusicSeekbarLayout.setVisibility(View.INVISIBLE);
			resetPlayTime();
		}
		
		final View tipLayout = findViewById(R.id.music_tips);
		tipLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tipLayout.setVisibility(View.GONE);
				saveBoolData(MUSIC_TIPS, true);
			}
		});
		if(!getBoolData(MUSIC_TIPS)) {
			tipLayout.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);    
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mVisualizer != null) {
			mVisualizer.setEnabled(false);
		}
		unbindService(mConnection);
		mHandler = null;
		
		unregisterReceiver(mPhoneStateChangedReceiver);
	}

	@Override
	public void onClick(View v) {
		if(!isConnect()) {
			showOpenDeviceDialog();
			return;
		}
		
		switch(v.getId()) {
			case R.id.music_operation :
				switch(mOperState) {
					case MUSIC_ADD :
						//addMusic();
						break;
					case MUSIC_PAUSE:
						playMusic();
						break;
					case MUSIC_PLAY:
						pauseMusic();
						break;
					default:
						break;
				}
				break;
			case R.id.music_prev :
				prevMusic();
				break;
			case R.id.music_next :
				nextMusic();
				break;
		}
	}
	
	@Override
	public void onPageChanged(int index) {
		if(index != mMusicIndex) {
			updateMusicInfo(index);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_MUSIC_ADD) {
			@SuppressWarnings("unchecked")
			ArrayList<MediaInfo> infos = (ArrayList<MediaInfo>) data.getSerializableExtra(MusicAdd.INTENT_DATA);
			int firstAdd = addMusicInfos(infos);
			if(firstAdd != -1) {
				//mWorkspace.setToScreen(firstAdd);
			}
			mWorkspace.setToScreen(0);
			if(infos.size() == 0) {
				pauseMusic();
				if(mPlayService != null) {
					mPlayService.reset();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void addMusic() {
		Intent intent = new Intent(this, MusicAdd.class);
		intent.putExtra(MusicAdd.INTENT_DATA, (Serializable)mMusicInfos);
		startActivityForResult(intent, REQUEST_MUSIC_ADD);
	}
	
	private void getMusicInfos() {
		int count = 0;
		mMusicCount = getIntData(MUSIC_COUNT);
		for(int i = 0; i < mMusicCount; i++) {
			String infoStr = getStrData(MUSIC_INFO_PREFIX + i);
			MediaInfo info = MediaInfo.genMediaInfo(infoStr);
			if(info != null) {
				mMusicInfos.add(count++, info);
			}
		}
		mMusicCount = count;
	}
	
	private int addMusicInfos(ArrayList<MediaInfo> infos) {
		int firstAdd = -1;
		
		cleanMusicInfo();
		
		for(int i = 0; infos != null && i < infos.size(); i++) {
			MediaInfo info = infos.get(i);
			if(!isExistMusicInfo(info)) {
				saveStrData(MUSIC_INFO_PREFIX + mMusicCount, info.toString());
				mMusicInfos.add(mMusicCount, info);
				
				if(mMusicCount == 0) {
					mWorkspace.removeAllViews();
					//mMusicSeekbarLayout.setVisibility(View.VISIBLE);
				}
				addMusicCover(mMusicCount);
				
				if(firstAdd == -1) {
					firstAdd = mMusicCount;
				}
				mMusicCount++;
			}
		}
		saveIntData(MUSIC_COUNT, mMusicCount);
		
		return firstAdd;
	}
	
	private boolean isExistMusicInfo(MediaInfo info) {
		for(MediaInfo item : mMusicInfos) {
			if(item.getId() == info.getId()) {
				return true;
			}
		}
		
		return false;
	}
	
	private void updateMusicInfo(int index) {
		if(mMusicCount == 0) {
			setMusicOperationState(MusicOperState.MUSIC_ADD);
			mTextTitle.setText("");
			mTextArtist.setText("");
			mMusicIndex = -1;
		} else {
			MediaInfo info = mMusicInfos.get(index);
			mTextTitle.setText(info.getTitle());
			mTextArtist.setText(info.getArtist());
			mTextPlayTime.setText(MediaInfo.toTime(0));
			mTextDuration.setText(MediaInfo.toTime((int) info.getDuration()));
			
			if(mOperState == MusicOperState.MUSIC_ADD) {
				mOperState = MusicOperState.MUSIC_PAUSE;
			}
			setMusicOperationState(mOperState);
			
			mMusicIndex = index;
			selectMusic(mMusicIndex);
		}
	}
	
	private void setMusicOperationState(MusicOperState state) {
		mOperState = state;
		switch(state) {
			case MUSIC_ADD:
				//mImageMusicOper.setImageResource(R.drawable.music_add_selector);
				break;
			case MUSIC_PLAY:
				mImageMusicOper.setImageResource(R.drawable.music_pause_selector);
				break;
			case MUSIC_PAUSE:
				mImageMusicOper.setImageResource(R.drawable.music_play_selector);
				break;
			default:
				break;
				
		}
	}
	
	private MusicCover inflateMusicCover(MediaInfo info) {
		LayoutInflater inflater = getLayoutInflater();
		MusicCover cover = (MusicCover) inflater.inflate(R.layout.music_cover, null);
		
		Bitmap bitmap; 
		if(info == null) {
			bitmap = MediaInfo.getDefaultArtwork(getApplicationContext());
			cover.setCoverImageBitmap(bitmap, true);
		} else {
			bitmap = MediaInfo.getArtwork(getApplicationContext(), info.getId(), info.getAlbumId(), false);
			if(bitmap == null) {
				cover.setCoverImageBitmap(MediaInfo.getDefaultArtwork(getApplicationContext()),true);
			} else {
				cover.setCoverImageBitmap(bitmap, false);
			}
		}
		
		return cover;
	}
	
	private void addMusicCover(int index) {
		MusicCover cover = inflateMusicCover(index == -1 ? null : mMusicInfos.get(index));
		mWorkspace.addView(cover, index);
	}
	
	private void selectMusic(int index) {
		if(mOperState != MusicOperState.MUSIC_ADD && index >= 0) {
			String path = mMusicInfos.get(index).getURL();
			
			if(mPlayService != null) {
				mPlayService.reset();
				mPlayService.setDataSource(path);
				mPlayService.prepare();
				mPlayService.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						//todo
					}
				});
				
				if(mVisualizer == null) {
					setupVisualizerFx();
				}
				
				if(mCurrentCover != null) {
					mCurrentCover.stopRotate();
				}
				mCurrentCover = (MusicCover) mWorkspace.getChildAt(index);
				mMusicSeekBar.setProgress(0);
				mMusicSeekBar.setMax(mPlayService.getDuration());
				mHandler.removeMessages(UPDATE_TIME);
				
				if(mOperState == MusicOperState.MUSIC_PLAY) {
					playMusic();
				}
			}
		}
	}
	
	private void cleanMusicInfo() {
		cleanSPMusicInfo();
		mMusicCount = 0;
		mMusicIndex = -1;
		mMusicInfos.clear();
		
		mWorkspace.removeAllViews();
		addMusicCover(-1);
		//mMusicSeekbarLayout.setVisibility(View.INVISIBLE);
		resetPlayTime();
		mMusicSeekBar.setProgress(0);
	}
	
	private void playMusic() {
		if(mPlayService != null && mMusicCount > 0) {
			mPlayService.start();
			mPlayService.setLooping(true);
			if(mVisualizer != null) {
				mVisualizer.setEnabled(true);
			}
			setMusicOperationState(MusicOperState.MUSIC_PLAY);
			if(mCurrentCover != null) {
				mCurrentCover.startRotate();
			}
			mHandler.sendEmptyMessageDelayed(UPDATE_TIME, UPDATE_TIME_INTERVAL);
			setFunLevelMode(0);
		}
	}
	
	private void pauseMusic() {
		if(mPlayService != null && mPlayService.isPlaying()) {
			mPlayService.pause();
			if(mVisualizer != null) {
				mVisualizer.setEnabled(false);
			}
			setMusicOperationState(MusicOperState.MUSIC_PAUSE);
			if(mCurrentCover != null) {
				mCurrentCover.pauseRotate();
			}
			mHandler.removeMessages(UPDATE_TIME);
			setFunLevelMode(0);
		}
	}
	
	private void prevMusic() {
		if(mMusicIndex > 0) {
			updateMusicInfo(--mMusicIndex);
			mWorkspace.setToScreen(mMusicIndex);
		}
	}
	
	private void nextMusic() {
		if(mMusicIndex < mMusicCount - 1) {
			updateMusicInfo(++mMusicIndex);
			mWorkspace.setToScreen(mMusicIndex);
		}
	}
	
	private void updatePlayTime(int time) {
		if(!mbSeekProgress) {
			mMusicSeekBar.setProgress(time);
			mTextPlayTime.setText(MediaInfo.toTime(time));
		}
	}
	
	private void resetPlayTime() {
		mTextPlayTime.setText(MediaInfo.toTime(0));
		mTextDuration.setText(MediaInfo.toTime(0));
		mMusicSeekBar.setMax(0);
	}

	private void setupVisualizerFx(){  
        
        final int maxCR = Visualizer.getMaxCaptureRate();  
          
        mVisualizer = new Visualizer(mPlayService.getAudioSessionId());  
        mVisualizer.setCaptureSize(64); 
        //mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);        
        mVisualizer.setDataCaptureListener(  
                new Visualizer.OnDataCaptureListener()  
                {  
                    public void onWaveFormDataCapture(Visualizer visualizer,  
                            byte[] bytes, int samplingRate)  
                    {  
                       // mVisualizerView.updateVisualizer(bytes);  
                    }  
  
                    public void onFftDataCapture(Visualizer visualizer,  
                            byte[] fft, int samplingRate)  
                    {  
                    	//Log.d(TAG, "fft" + fft.toString());
                    	int tempValue = 0;
                    	for(int i = 0; i < fft.length; i++){
                    		tempValue += Math.abs(fft[i]);
                    	}
                    	//tempValue = tempValue/fft.length;
                    	mPinpuCount ++;
                    	//mPinpuValue += tempValue;
                    	
                  	
                    	if(mPinpuCount >= PINPU_MAX_COUNT){
                           	Log.d(TAG, "tempValue" + tempValue);   
                    		
                    		int mode = 0;
                    		if(tempValue > 2500){
                    			mode = MAX_MASSAGE_LEVEL;
                    		}else if(tempValue > 2000){
                    		    mode = MAX_MASSAGE_LEVEL * 3/4;	
                    		}else if(tempValue > 1500){
                    			mode = MAX_MASSAGE_LEVEL/2;
                    		}else if(tempValue > 1000){
                    			mode = MAX_MASSAGE_LEVEL/4;
                    		}else if(tempValue > 500){
                    			mode = MAX_MASSAGE_LEVEL/10 + 1;
                    		}else{
                    			mode = 1;
                    		}
                    		if(mPlayService != null && mPlayService.isPlaying()){
                    			setFunLevelMode(mode);
                    		}

                           	Log.d(TAG, "mode=" + mode);                      		
                    		mPinpuCount = 0;
                    		//mPinpuValue = 0;
                    	}

                        //mVisualizerView.updateVisualizer(fft);  
                    }  
                }, maxCR / 2, false, true);  
    }
}
