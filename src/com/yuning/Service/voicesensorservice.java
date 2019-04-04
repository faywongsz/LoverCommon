package com.yuning.Service;

import java.util.Random;

import com.yuning.activity.BaseActivity;
import com.yuning.util.RealDoubleFFT;
import com.yuning.util.sendThread;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;


public class voicesensorservice extends Service {  
	    private static final String TAG = "voicesensorservice" ;  
		byte[] mStop = {(byte) 0xff,(byte)0xff};	   
	    boolean started = false;
	    RecordAudio recordTask;
		int frequency = 8000;
		int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

		private RealDoubleFFT transformer;
		int blockSize = 256;
		
		private long lastUpdateTime; 
		private static final long UPTATE_INTERVAL_TIME = 20L; 		
	    
	    @Override  
	    public void onCreate() {  
	        Log.v(TAG, "ServiceDemo onCreate");  
	        super.onCreate();  
			transformer = new RealDoubleFFT(blockSize);	        
	    }  
	      
	    @Override  
	    public void onStart(Intent intent, int startId) {  
	        Log.v(TAG, "ServiceDemo onStart");  
	        super.onStart(intent, startId);  
			 
			if(!started){
				recordTask = new RecordAudio();			
				recordTask.execute();
				started = true;
			}


	    }  
	      
	    
	    @Override  
	    public void onDestroy() {  
	        Log.v(TAG, "onDestroy");  
	        if(recordTask != null){
				recordTask.cancel(true);
				recordTask = null;
				started = false;
				
	        }
			sendThread.sendData(mStop);
			/*restart record:wangfei added begin*/
			mHander.removeMessages(MSG_RESTART);
			mRestarted = false;
			/*restart record:wangfei added end*/
	    }
	    
	    @Override  
	    public int onStartCommand(Intent intent, int flags, int startId) {  
	        Log.v(TAG, "ServiceDemo onStartCommand");  
	        return super.onStartCommand(intent, flags, startId);  
	    }

		@Override
		public IBinder onBind(Intent intent) {
			if(!started){
				recordTask = new RecordAudio();			
				recordTask.execute();
				started = true;
			}	
			return null;
		}  
		
		/*restart record:wangfei added begin*/
		private static final int MSG_RESTART = 1;
		private int mRetryCount = 0;
		private boolean mRestarted = false;
		private TelephonyManager mTM;
		private Handler mHander = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
					case MSG_RESTART:
						Log.d(TAG,"MSG_RESTART");
						restartRecord();
				}
			}
			
		};
		/*restart record:wangfei added end*/
		
		private class RecordAudio extends AsyncTask<Void, double[], Void> {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					int bufferSize = AudioRecord.getMinBufferSize(frequency,
							channelConfiguration, audioEncoding);
					
					/*restart record:wangfei added begin*/
					mTM = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
					/*restart record:wangfei added end*/
					
					AudioRecord audioRecord = new AudioRecord(
							(mTM != null && mTM.getCallState() != TelephonyManager.CALL_STATE_IDLE) ? 
									MediaRecorder.AudioSource.VOICE_CALL : MediaRecorder.AudioSource.DEFAULT, frequency,
							channelConfiguration, audioEncoding, bufferSize);

					short[] buffer = new short[blockSize];
					double[] toTransform = new double[blockSize];

					audioRecord.startRecording();
			        Log.v(TAG, "doInBackground started=" + started);  
					while (started) {
						int bufferReadResult = audioRecord.read(buffer, 0,
								blockSize);

						for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
							toTransform[i] = (double) buffer[i] / 32768.0; // signed

						}

				       // Log.v(TAG, "doInBackground while");  
				        
						transformer.ft(toTransform);
						publishProgress(toTransform);
					}

					/*some phone can't open AudioRecord again:wangfei added begin*/
					//audioRecord.stop();
					audioRecord.release();
					audioRecord = null;
					/*some phone can't open AudioRecord again:wangfei added end*/
				} catch (Throwable t) {
					Log.e(TAG, "Recording Failed" + t);
				}

				return null;
			}
			private final int MAX_PWM_LEVEL = 25;
			private int mPwmLevel = 0;
			
			protected void onProgressUpdate(double[]... toTransform) {
				int maxVoicelevel = 0;
				int minVoicelevel = 10000000;			

			    long currentUpdateTime = SystemClock.elapsedRealtime();  
			    long timeInterval = currentUpdateTime - lastUpdateTime; 
			    if(timeInterval < UPTATE_INTERVAL_TIME) {  
			         return;  
			    }
			    
			    lastUpdateTime = currentUpdateTime;
			    
				
				for (int i = 0; i < toTransform[0].length; i++) {
					int x = i;
					int downy = (int) (100 - (toTransform[0][i] * 10));
					int upy = 100;
					int level = (int)(toTransform[0][i] * 10);
					if(maxVoicelevel < level){
						maxVoicelevel = level;
					}
					if(minVoicelevel > level){
						minVoicelevel = level;
					}				
				}


	      		int mode = 0;
	      		//maxVoicelevel -= 50;
	      		if(maxVoicelevel < 0){
	      			maxVoicelevel = 0;
	      		}
	      		

	    		/*if(maxVoicelevel > 2000){
	    			mode = MAX_PWM_LEVEL;
	    		}else if(maxVoicelevel > 1500){
	    		    mode = MAX_PWM_LEVEL - 1;	
	    		}else if(maxVoicelevel > 400){
	    			mode = MAX_PWM_LEVEL -3;
	    		}else if(maxVoicelevel > 150){
	    			mode = MAX_PWM_LEVEL -4;
	    		}else{
	    			mode = maxVoicelevel * 21 / 150;
	    		}*/	
	      		if(maxVoicelevel >= 9){
	      			mode = MAX_PWM_LEVEL;
	      		}else{
	      			//mode = maxVoicelevel * MAX_PWM_LEVEL / 100;
	      			mode = maxVoicelevel * 3;
	      			if(mode > MAX_PWM_LEVEL)
	      				mode = MAX_PWM_LEVEL;
	      		}
	      		Log.d(TAG,"maxVoicelevel="+maxVoicelevel+"    mode="+mode);
	      		
	      		/*restart record:wangfei added begin*/
	      		if(mode == 0) {
	      			if(mRetryCount++ > 10) {
	      				/*mRetryCount = 0;
		      			started = false;
		      			mHander.sendEmptyMessageDelayed(MSG_RESTART, 500);*/
	      				mode = getRandomLevel();
	      				Log.d(TAG,"restart, mode = " + mode);
	      			}
	      		} else {
	      			mRetryCount = 0;
	      		}
	      		/*restart record:wangfei added end*/
	    		
				
				if(mPwmLevel != mode && started){
					byte[] modeCMD = new byte[2];
					modeCMD[0] = 0x02;
					modeCMD[1] = (byte)mode;
					
					//BaseActivity.sendData(modeCMD);
					sendThread.sendData(modeCMD);
					//Log.d(TAG, "mode:" + mode);
					mPwmLevel = mode;
				}


			}
		}
		
		/*restart record:wangfei added begin*/
		private void restartRecord() {
			started = false;
	        if(recordTask != null){
				recordTask.cancel(true);
				recordTask = null;
				
	        }
			recordTask = new RecordAudio();			
			recordTask.execute();
			started = true;
			mRestarted = true;
			Log.d(TAG,"restartRecord");
		}
		
		private static final int RANDOM_MIN_LEVEL = 10;
		private static final int RANDOM_MAX_LEVEL = 25;
		private int getRandomLevel() {
			Random random = new Random();
			
			return random.nextInt(RANDOM_MAX_LEVEL) % (RANDOM_MAX_LEVEL - RANDOM_MIN_LEVEL + 1) + RANDOM_MIN_LEVEL;
		}
		/*restart record:wangfei added end*/
	

}
