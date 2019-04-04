package com.yuning.activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;
import com.yuning.util.RealDoubleFFT;

public class SingMode extends BaseModeActivity{
	private static final String TAG = SingMode.class.getSimpleName();	
			
    private int frequency = 8000;
    private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	private RealDoubleFFT transformer;
	int blockSize = 256;
	
	private long lastUpdateTime; 
	private static final long UPTATE_INTERVAL_TIME = 20L; 	
	
	private RecordAudio recordTask;

	private ImageView mImageWave;
	private Bitmap bitmap;
	private Canvas canvas;
	private Paint paint;
	private boolean started = false;
	private int mWaveWidth, mWaveHeight;
	private static final int WAVE_HEIGHT = 60;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sing_mode);
		initTitleBar(R.string.sing_mode, 0, 0, null);

		transformer = new RealDoubleFFT(blockSize);

		mImageWave = (ImageView) this.findViewById(R.id.image_wave);
		ImageView animation = (ImageView) findViewById(R.id.sing_mode_animation);
		AnimationDrawable animationDrawable = (AnimationDrawable) animation.getDrawable();
		animationDrawable.start();

		final float desity = getResources().getDisplayMetrics().density;
		mWaveWidth = getResources().getDisplayMetrics().widthPixels;
		mWaveHeight = (int) (WAVE_HEIGHT * desity);
		
		bitmap = Bitmap.createBitmap(mWaveWidth, mWaveHeight, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setStrokeWidth(1 * desity);
		paint.setColor(0xffe31b22);
		mImageWave.setImageBitmap(bitmap);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
	
		started = true;
		recordTask = new RecordAudio();			
		recordTask.execute();
		
		keepScreenOn(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);

		if(recordTask != null){
			recordTask.cancel(true);
			recordTask = null;
			started = false;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "voice onPause send stop");
					stopVibration();
				}
			}, 500);
		}
		
		keepScreenOn(false);
	}
	   
	private class RecordAudio extends AsyncTask<Void, double[], Void> {
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
			bitmap.eraseColor(Color.TRANSPARENT);
			canvas.drawColor(Color.TRANSPARENT);
			/*draw begin*/
			int lastX = mWaveWidth / 4;
			int lastY = mWaveHeight / 2;
			canvas.drawLine(0,lastY-1,lastX,lastY,paint);//draw beginning line
			for(int i = 0; i < toTransform[0].length; i++){
				
				int level = (int)(toTransform[0][i] * 10);
				if(maxVoicelevel < level){
					maxVoicelevel = level;
				}
				if(minVoicelevel > level){
					minVoicelevel = level;
				}
				if((i+1)%8 == 0){
					int newX = i*(mWaveWidth/2)/toTransform[0].length-1+mWaveWidth / 4;
					int newY = (int) (mWaveHeight / 2 - (toTransform[0][i] * 10));
					if(newY < 0)
						newY = 0;
					if(newY > mWaveHeight)
						newY = mWaveHeight;
					canvas.drawLine(lastX, lastY, newX, newY, paint);//draw voice data line
					lastX = newX;
					lastY = newY;
				}
				
			}
			canvas.drawLine(lastX,lastY,lastX+8,mWaveHeight / 2,paint);//draw ending line
			lastX = lastX + 8;
			canvas.drawLine(lastX-1,mWaveHeight / 2-1,mWaveWidth,mWaveHeight / 2,paint);
			/*draw end*/
			
/*
			//draw line
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
				canvas.drawLine(2*x-1+100, downy, 2*x-1+100, upy, paint);
				
			}*/

      		int mode = 0;
      		//maxVoicelevel -= 50;
      		if(maxVoicelevel < 0){
      			maxVoicelevel = 0;
      		}
      		
    		/*if(maxVoicelevel > 2000){
    			mode = MAX_PWM_LEVEL;
    		}else if(maxVoicelevel > 1500){
    		    mode = MAX_PWM_LEVEL - 2;	
    		}else if(maxVoicelevel > 1000){
    			mode = MAX_PWM_LEVEL -4;
    		}else if(maxVoicelevel > 500){
    			mode = MAX_PWM_LEVEL - 7;
    		}else if(maxVoicelevel > 200){
    			mode = MAX_PWM_LEVEL /2;
    		}else if(maxVoicelevel > 100){
    			mode = MAX_PWM_LEVEL /2 -2;     			
    		}else{
    			mode = maxVoicelevel/10;
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
    		//mode = mode * 2;//add by guoliangliang 20150205
			if(mPwmLevel != mode){
				setFunLevelMode(mode);
					
				mPwmLevel = mode;
			}

			
			mImageWave.invalidate();
		}
	}
}
