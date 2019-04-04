package com.yuning.activity;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.umeng.analytics.MobclickAgent;
import com.yuning.Service.voicesensorservice;
import com.yuning.lovercommon.R;
import com.yuning.util.LoadThumbnail;
import com.yuning.util.MediaInfo;
import com.yuning.util.MyHandler;
import com.yuning.util.sendThread;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;

public class VideoMode extends BaseModeActivity implements View.OnClickListener{
	private static final String TAG = VideoMode.class.getSimpleName();
	
	private static final int REQUEST_VIDEO_ADD = 1;
	private static final int DEFAULT_VIDEO_ID = -1;
	private VideoView mVideoView;
	private Uri mDefaultUri;
	
	private Boolean mIsBindService= false;
	private ImageView mImagePlayPause;
	private TextView mTextPlayTime;
	private TextView mTextDuration;
	private SeekBar mVideoSeekbar;
	private ListView mVideoList;
	private VideoAdapter mVideoAdapter;
	private LoadThumbnail mLoadThumbnail = null;
	private View mVideoBarLayout;
	private int mCurrentVideoIndex = 0;
	private boolean mbSeekProgress = false;
	private int mSeekPosition = 0;
	
	private static final byte[] mCMDstop= {(byte)0xff,(byte)0xff};

	private static final int HIDE_PALY_BUTTON = 1;
	private static final int HIDE_PALY_BUTTON_INTERVAL = 3000;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case HIDE_PALY_BUTTON : 
					mImagePlayPause.setVisibility(View.GONE);
					if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
						mVideoBarLayout.setVisibility(View.GONE);
					}
					break;
			}
		}
		
	};

	private MyHandler mMyHandler = new MyHandler() {
		@Override
		protected void OnTimerStart() {}

		@Override
		protected void OnTimer() {
			if(mVideoView == null){
				OnTimerStop();
			}else{
				if(mVideoView.isPlaying()) {
					updatePlayTime(mVideoView.getCurrentPosition());
					updateSeekbarProgress(mVideoView.getCurrentPosition(), mVideoView.getDuration());
				}
			}
		}

		@Override
		protected void OnTimerStop() {}

	};
    
	public void onCreate(Bundle bundle){
		super.onCreate(bundle, false);
		mVideoAdapter = new VideoAdapter(this);
		initView();
		
		initVideoAdaptorFromHistory();
		mCurrentVideoIndex = 0;
		updateVideoSource(mCurrentVideoIndex);
	}
	
	private void initView(){
		setContentView(R.layout.video_mode);
		
		initTitleBar(R.string.video_mode, R.drawable.music_title_add_selector, 0, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addVideo();
			}
		});
		
		mVideoView = (VideoView)findViewById(R.id.videoview);
		mImagePlayPause = (ImageView)findViewById(R.id.video_mode_play_pause);
		mTextPlayTime = (TextView) findViewById(R.id.video_play_time);
		mTextDuration = (TextView) findViewById(R.id.video_duration);
		mVideoBarLayout = findViewById(R.id.video_bar);
		
		mTextPlayTime.setText(MediaInfo.toTime(0));
		mTextDuration.setText(MediaInfo.toTime(0));
		findViewById(R.id.video_layout).setOnClickListener(this);
		findViewById(R.id.video_full_screen).setOnClickListener(this);
		mImagePlayPause.setOnClickListener(this);
		resetHideInterval();
		
		mVideoSeekbar = (SeekBar)findViewById(R.id.video_seekbar);
		mVideoSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(mbSeekProgress) {
					if(mVideoView != null) mVideoView.seekTo((int) (((long)mSeekPosition * mVideoView.getDuration()) / mVideoSeekbar.getMax()));
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
				if(mbSeekProgress) {
					mSeekPosition = progress;
					mTextPlayTime.setText(MediaInfo.toTime((int) (((long)mSeekPosition * mVideoView.getDuration()) / mVideoSeekbar.getMax())));
				}
			}
		});
		
		mVideoList = (ListView)findViewById(R.id.video_list);
		mVideoList.setAdapter(mVideoAdapter);
		mVideoList.setOnItemClickListener(new ListView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				// TODO Auto-generated method stub
				mCurrentVideoIndex = position;
				updateVideoSource(position);
				mImagePlayPause.callOnClick();
			}
			
		});

		mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {    
            public void onCompletion(MediaPlayer mp) {
            	if(mIsBindService){
				     unbindService(mConn);	
				     mIsBindService = false;
            	}
            	
            	mVideoView.pause();

			    mImagePlayPause.setImageResource(R.drawable.video_mode_play_selector);
			     
			    mMyHandler.stopTimer();
			    //mBtnPlayStop.callOnClick();
			    /* start next */
			    startPlay();
			     
            }    
        }); 		
			
	}
	
	private MediaInfo getDefaultMedia() {
		MediaInfo video = new MediaInfo();
		mDefaultUri = Uri.parse("android.resource://" + getPackageName() + "/" +R.raw.default_video);
		video.setId(DEFAULT_VIDEO_ID);
		video.setTitle(getString(R.string.video_mode_default_name));
		video.setDuration(15000);
		
		return video;
	}

	private void initVideoAdaptorFromHistory(){
		mVideoAdapter.clearVideos();
		
		MediaInfo defaultVideo = getDefaultMedia();
		addVideoAdaptor(0, defaultVideo);
		
		int count = getIntData(VIDEO_COUNT);
		int addCount = 1;
		for(int i = 0; i < count; i++){
			String infoStr = getStrData(VIDEO_INFO_PREFIX + i);
			MediaInfo info = MediaInfo.genMediaInfo(infoStr);
			if(info != null) {
				addVideoAdaptor(addCount++, info);
			}
		}
	}
	
	private void addVideoAdaptor(int index, MediaInfo info){
		mVideoAdapter.addVideo(index, info);
		mVideoAdapter.notifyDataSetChanged();
	}
	
	private boolean isExistVideoInfo(MediaInfo info) {
		for(int i = 0; i < mVideoAdapter.getCount(); i++){
			MediaInfo video = (MediaInfo) mVideoAdapter.getItem(i);
			if(info.getId() == video.getId()){
				return true;
			}
		}
		
		return false;
	}
	
	private int addVideoInfos(ArrayList<MediaInfo> infos) {
		int firstAdd = 0;
		
		cleanVideoInfo();
		
		addVideoAdaptor(0, getDefaultMedia());
		
		int count = mVideoAdapter.getCount();
		for(int i = 0; infos != null && i < infos.size(); i++) {
			MediaInfo info = infos.get(i);
			if(!isExistVideoInfo(info)) {
				saveStrData(VIDEO_INFO_PREFIX + count, info.toString());
				addVideoAdaptor(count, info);
				
				if(firstAdd == 0) {
					firstAdd = count;
				}
				count++;
			}
		}
		saveIntData(VIDEO_COUNT, count);
		
		return firstAdd;
	}
	
	private void cleanVideoInfo() {
		cleanSPVideoInfo();
		mVideoAdapter.clearVideos();
		
		mTextPlayTime.setText(MediaInfo.toTime(0));
		mTextDuration.setText(MediaInfo.toTime(0));
		mVideoSeekbar.setProgress(0);
	}
    
	public void onResume()
	{
		super.onResume();
		updatePlayTime(mVideoView.getCurrentPosition() <= 0 ? 0 : mVideoView.getCurrentPosition());
		updateSeekbarProgress(mVideoView.getCurrentPosition(), (int) ((MediaInfo)mVideoAdapter.getItem(mCurrentVideoIndex)).getDuration());
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);		
		
		keepScreenOn(true);
	}
	
	public void onPause(){
		super.onPause();
		Log.d(TAG,"onPause()");
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);		
		if(mVideoView.isPlaying()){
			mImagePlayPause.callOnClick();
			//mVideoView.pause();
			//mBtnPlayStop.setImageResource(R.drawable.video_mode_play_selector);
		}
		/*modified by wangfei begin*/
		if(!mIsBindService) sendThread.sendData(mCMDstop);	
		/*modified by wangfei end*/
		if(mIsBindService){
			unbindService(mConn);
			mIsBindService = false;
		}
		mMyHandler.stopTimer();
		//updatePlayTime(0);
		
		keepScreenOn(false);
	}
    
    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        	
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        	
        }
    };	

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.video_mode_play_pause :
				if(!isConnect()) {
					showOpenDeviceDialog();
					return;
				}
				onPlayPause();
				resetHideInterval();
				break;
			case R.id.video_full_screen : 
				handleFullScreen();
				break;
			case R.id.video_layout :
				if(mImagePlayPause.getVisibility() == View.GONE) {
					mImagePlayPause.setVisibility(View.VISIBLE);
				}
				if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
					if(mVideoBarLayout.getVisibility() == View.GONE) {
						mVideoBarLayout.setVisibility(View.VISIBLE);
					}
				}
				resetHideInterval();
				break;
		}
	}
	
	private void startPlay(){
		mVideoView.start();

		/*modified by wangfei begin*/
		//Intent service = new Intent(videomode.this,voicesensorservice.class);
		//videomode.this.bindService(service, mConn,  Context.BIND_AUTO_CREATE);
		//mIsBindService = true;
		if(mVisualizer != null) mVisualizer.setEnabled(true);
		check_zero_num = 0;
		/*modified by wangfei end*/
		mImagePlayPause.setImageResource(R.drawable.video_mode_pause_selector);
		updatePlayTime(0);
		mMyHandler.startTimer();
	}
	
	private void stopPlay(){
		mVideoView.pause();
		/*modified by wangfei begin*/
		if(!mIsBindService) sendThread.sendData(mCMDstop);	
		if(mIsBindService) {
			unbindService(mConn);
			mIsBindService = false;
		}
		if(mVisualizer != null) mVisualizer.setEnabled(false);
		/*modified by wangfei end*/
		mImagePlayPause.setImageResource(R.drawable.video_mode_play_selector);
		Log.d(TAG,"onBtnStart call mMyHandler.stopTimer()");
		mMyHandler.stopTimer();
	}
	
	
    public void	onPlayPause(){
    	if(mVideoView.isPlaying()){
    		stopPlay();
    	}else{
    		startPlay();
    	}
    	mMyHandler.setTimer(250);
	}
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
        switch (requestCode) {
            case REQUEST_VIDEO_ADD:     
            if (resultCode == RESULT_OK) {
    			@SuppressWarnings("unchecked")
				ArrayList<MediaInfo> infos = (ArrayList<MediaInfo>) data.getSerializableExtra(VideoAdd.INTENT_DATA);
    			int firstAdd = addVideoInfos(infos);
    			updateVideoSource(firstAdd);
            }
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }   
    
    public void	addVideo(){
		Intent intent = new Intent(this, VideoAdd.class);
		intent.putExtra(VideoAdd.INTENT_DATA, (Serializable)mVideoAdapter.getVideos());
		startActivityForResult(intent, REQUEST_VIDEO_ADD);
         
	} 

	void updateVideoSource(int index){
		//Uri uri = ((Video)mVideoAdapter.getItem(index)).getUri();
		//mVideoView.setVideoURI(uri);
		String path = ((MediaInfo)mVideoAdapter.getItem(index)).getURL();
		if(index == 0){
			mVideoView.setVideoURI(mDefaultUri);
		}else{
			mVideoView.setVideoPath(path);
		}
		/*modified by wangfei begin*/
		if(mVisualizer != null) mVisualizer.setEnabled(false);
		setupVisualizerFxAndUI();
		/*modified by wangfei end*/
		mCurrentVideoIndex = index;
		mTextDuration.setText(MediaInfo.toTime((int) ((MediaInfo)mVideoAdapter.getItem(index)).getDuration()));
		//mImagePlayPause.callOnClick();
	}

	private void updatePlayTime(int time) {
		if(!mbSeekProgress) {
			mTextPlayTime.setText(MediaInfo.toTime(time));
		}
	}
	
	private void updateSeekbarProgress(int position, int duration) {
		int progress = duration <= 0 ? 0 : (int)(((long)position * mVideoSeekbar.getMax()) / duration);
		if(!mbSeekProgress) {
			mVideoSeekbar.setProgress(progress);
		}
	}
	
	private void resetHideInterval() {
		mHandler.removeMessages(HIDE_PALY_BUTTON);
		mHandler.sendEmptyMessageDelayed(HIDE_PALY_BUTTON, HIDE_PALY_BUTTON_INTERVAL);
	}
	
	private class VideoAdapter extends BaseAdapter{
		
		private class ViewHolder {
			TextView mTitle;
			ImageView mImage;
			TextView mDuration;
		}
		
		Context mContext;
    	LayoutInflater mInflater;	
    	private ArrayList <MediaInfo> mVideos;
		public VideoAdapter(Context context){
			mContext = context;
			mVideos = new ArrayList<MediaInfo>();
			mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		public void addVideo(int index, MediaInfo video){
			mVideos.add(index, video);
		}
		
		public ArrayList <MediaInfo> getVideos(){
			return mVideos;
		}
		
		public void clearVideos() {
			mVideos.clear();
		}
		
		@Override
		public int getCount() {
			return mVideos.size();
		}

		@Override
		public Object getItem(int index) {
			return mVideos.get(index);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			ViewHolder holder = null;
			MediaInfo info = mVideos.get(position);
			
			if(convertView == null){
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.video_mode_list_item, null);
				
				holder.mTitle = (TextView) convertView.findViewById(R.id.item_title);
				holder.mImage = (ImageView) convertView.findViewById(R.id.item_img);
				holder.mDuration = (TextView) convertView.findViewById(R.id.item_duration);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.mTitle.setText(info.getTitle());
			holder.mDuration.setText(MediaInfo.toTime((int) info.getDuration()));
			holder.mImage.setTag(info);
			if(mLoadThumbnail == null) {
				mLoadThumbnail = new LoadThumbnail(getApplicationContext());
			}
			mLoadThumbnail.loadThumbnail(holder.mImage, info, new LoadThumbnail.OnLoadFinished() {
				@Override
				public void onLoadFinished(ImageView imageView, MediaInfo info,
						Bitmap bitmap) {
					ImageView image = (ImageView) mVideoList.findViewWithTag(info);
					image.setImageBitmap(bitmap);
				}
			});
			
			return convertView;
		}
		
	}
	
	public void handleFullScreen(){
         if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
             setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
         }else if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
             setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
         }		 
	}
	
	void onScreenchanged(){
        boolean isPlaying = mVideoView.isPlaying();	
        int CurrentPosion = mVideoView.getCurrentPosition();
		
		initView();
		updateVideoSource(mCurrentVideoIndex);
		mVideoView.seekTo(CurrentPosion);
		
		if(isPlaying){
			startPlay();
			
		}
		updatePlayTime(CurrentPosion);
		updateSeekbarProgress(CurrentPosion, (int) ((MediaInfo)mVideoAdapter.getItem(mCurrentVideoIndex)).getDuration());
	}

	@Override 
	public void onConfigurationChanged(Configuration newConfig) 
	{ 
		super.onConfigurationChanged(newConfig); 
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) { 
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
			
		} 
		else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { 
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);
		} 
		onScreenchanged();
	} 
	
	/*modified by wangfei begin*/
	private final int MAX_PWM_LEVEL = 25;
	private int PINPU_MAX_COUNT = 5;
	private int pinpu_count = 0;
    private int pinpu_value = 0;
    private int check_zero_num = 0;
	private Visualizer mVisualizer;
	
	private void setupVisualizerFxAndUI(){  
        
        final int maxCR = Visualizer.getMaxCaptureRate();  
        
        mVisualizer = new Visualizer(mVideoView.getAudioSessionId());  
        //mVisualizer.setCaptureSize(mVisualizer.getCaptureSize()); 
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);        
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
                    	if(mIsBindService) {
                    		return;
                    	}
                    	//Log.d(TAG, "fft" + fft.toString());
                    	int tempValue = 0;
                    	for(int i = 0; i < fft.length; i++){
                    		tempValue += Math.abs(fft[i]);
                    	}
                    	
                    	//tempValue = tempValue/fft.length;
                    	pinpu_count ++;
                    	pinpu_value += tempValue;
                    	
                  	
                    	if(pinpu_count >= PINPU_MAX_COUNT){
                           	Log.d(TAG, "tempValue" + tempValue);   
                        	
                           	if(pinpu_value == 0) {
                        		if(!mIsBindService && check_zero_num == 5) {
                            		Intent service = new Intent(VideoMode.this,voicesensorservice.class);
                            		VideoMode.this.bindService(service, mConn,  Context.BIND_AUTO_CREATE);
                            		mIsBindService = true;
                            		check_zero_num = 0;
                        		} else if(check_zero_num > 0) {
                        			tempValue = 400;//avoid toy no vibration
                        		}
                        		check_zero_num++;
                        	}
                           	
                    		int mode = 0;
                    		if(tempValue > 2500){
                    			mode = MAX_PWM_LEVEL;
                    		}else if(tempValue > 2000){
                    		    mode = MAX_PWM_LEVEL * 3/4;	
                    		}else if(tempValue > 1500){
                    			mode = MAX_PWM_LEVEL/2;
                    		}else if(tempValue > 1000){
                    			mode = MAX_PWM_LEVEL/4;
                    		}else if(tempValue > 500){
                    			mode = MAX_PWM_LEVEL/10 + 1;
                    		}else{
                    			mode = 1;
                    		}
                    		if(mVideoView.isPlaying()){
                    			setFunLevelMode(mode);
                    		}

                           	Log.d(TAG, "mode=" + mode);                      		
                    		pinpu_count = 0;
                    		pinpu_value = 0;
                    	}

                        //mVisualizerView.updateVisualizer(fft);  
                    }  
                }, maxCR / 2, false, true);  
    } 
	/*modified by wangfei end*/
}
