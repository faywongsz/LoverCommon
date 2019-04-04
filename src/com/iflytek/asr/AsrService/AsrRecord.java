package com.iflytek.asr.AsrService;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * ASR AudioRecord manager
 * @author zhangyun
 *
 */
public class AsrRecord {
	private static final String TAG = "AsrService(record)";
	private static final int BUFF_SIZE = 64 * 320;           //Receive data buffer size
	private static final int FRAME_BUFF = 16 * 320;          //A frame buffer size
	private static final int SAMPLE_RATE = 16000;             //Sample rate
	private static final int READ_DELAY = 10;                //Read delay time
	private static final long WAIT_TEIMOUT = 5000;           //Time out 
	private static final int BUFF_IGNORE = 4 * 320;          //Ignore audio data when begin record
	
	
	private static AudioRecord mRecord = null;
	private static final ReentrantLock ReadThreadLock = new ReentrantLock();
	
	
	//寮�惎褰曢煶
	public static int  startRecord(){
		Log.d(TAG,"startRecord ");
		mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 
				SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				 AudioFormat.ENCODING_PCM_16BIT, BUFF_SIZE);
		if (null == mRecord){
			Log.d(TAG,"AudioRecord create failed");
			return -1;
		}
		if (mRecord.getState() == AudioRecord.STATE_UNINITIALIZED ){
			Log.d(TAG,"startRecord state uninitialized");
			return -1;
		}else{
			mRecord.startRecording();
			
		}

		class ThreadRecord implements Runnable{			
			

			@Override
			public void run() {
				boolean isReadRunable = false;				
				try {
					isReadRunable = ReadThreadLock.tryLock(WAIT_TEIMOUT, TimeUnit.MILLISECONDS);
					if (!isReadRunable){
						Log.e(TAG, "ThreadRecord tryLock  is unavailable");
						return ;
					}
					byte [] mBuff = new byte[BUFF_SIZE];
					mRecord.read(mBuff,0,BUFF_IGNORE );
					Log.d(TAG," ignore audio data ...");
					while (true){
						
						try {
							Thread.sleep(READ_DELAY);
						} catch (InterruptedException e) {
							Log.d(TAG,e.toString());		
							break;
						}
						if (mRecord == null){
							Log.d(TAG,"ThreadRecord mRecord null ");		
							break;
						}
						if(Asr.mNeedStop){
							break;
						}						
						if (mRecord.getState() == AudioRecord.STATE_UNINITIALIZED
								|| AudioRecord.RECORDSTATE_STOPPED == mRecord.getRecordingState()){
							Log.d(TAG,"ThreadRecord mRecord uninitialized or stopped");		
							break;
						}
						
						if(mRecordForceStop){
							Log.d(TAG,"ThreadRecord mRecord force stop");								
							int ret = Asr.JniEndData();
							if (0 != ret){
								Log.e(TAG, "ThreadRecord force stop recode error!");
								break;
							}
							mRecordForceStop = false;
						}else{
							Log.d(TAG,"ThreadRecord begin read.......");	
							int ret = 0;
							try {
								ret = mRecord.read(mBuff,0,FRAME_BUFF);
							}catch (Exception e){
								Log.e(TAG,e.toString());
							}
							
							if (ret > 0){
								//鍚戣瘑鍒紩鎿庨�鍏ユ暟鎹�
								ret = Asr.appendData(mBuff,ret);
								if (0 != ret){
									Log.e(TAG, "ThreadRecord append data to ASR error!");
									break;
								}
								
							}else{
								Log.e(TAG, "ThreadRecord read data error!");
								break;
							}						
						}	
					}	//end of while
					
					try {
						Log.d(TAG,"ThreadRecord finish and release");	
						if (null != mRecord){
							mRecord.stop();
							mRecord.release();
						}					
					}catch (Exception e){
						Log.d(TAG,e.toString());
					}
					mBuff = null;
					
				} catch (InterruptedException e1) {
					Log.e(TAG,e1.toString());
				}finally{
					if (isReadRunable){
						ReadThreadLock.unlock();
					}
				}	
				
				Log.d(TAG,"ThreadRecord stop");	
				isThreadRun = false;
				
			}
		
		};
		Thread mThreadRecord = (new Thread(new ThreadRecord()));
		mThreadRecord.start();
		isThreadRun = true;
		
		mRecordForceStop = false;
		
		Log.d(TAG,"ThreadRecord run");		
		
		return 0;
	}

	//鍋滄褰曢煶
	public static void stopRecord() {
		if (null != mRecord && mRecord.getState() == AudioRecord.STATE_INITIALIZED
				|| AudioRecord.RECORDSTATE_RECORDING == mRecord.getRecordingState()){
			Log.d(TAG,"stopRecord ");		
			try {
				mRecord.stop();
			}catch (Exception e){
				Log.d(TAG,e.toString());
			}
		}else{
			Log.d(TAG,"stopRecord  error state ");
		}
	}	

	private static boolean mRecordForceStop = false;
	public static void ForceEndRecord() {
		mRecordForceStop = true;
	}	
    public static boolean isThreadRun  = false;

	
}
