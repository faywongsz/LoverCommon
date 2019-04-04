package com.yuning.Service;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicPlayService extends Service {
	private static final String TAG = MusicPlayService.class.getSimpleName();

    public class LocalBinder extends Binder {
    	public MusicPlayService getService() {
            return MusicPlayService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder(); 
    
    private MediaPlayer mMediaPlayer;
    
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
		super.onCreate();
		mMediaPlayer = new MediaPlayer();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand()");
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
		mMediaPlayer.release();
		mMediaPlayer = null;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind()");
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "onUnbind()");
		return super.onUnbind(intent);
	}

	public void setDataSource(String path) {
		try {
			if(mMediaPlayer != null) mMediaPlayer.setDataSource(path);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void prepare() {
		try {
			if(mMediaPlayer != null) mMediaPlayer.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public boolean isPlaying() {
    	if(mMediaPlayer != null) {
    		return mMediaPlayer.isPlaying();
    	} else {
    		return false;
    	}
    }
    
    public void stop() {
    	if(mMediaPlayer != null) mMediaPlayer.stop();
    }
    
    public void pause() {
    	if(mMediaPlayer != null) mMediaPlayer.pause();
    }
    
    public void start() {
    	if(mMediaPlayer != null) mMediaPlayer.start();
    }
    
    public void reset() {
    	if(mMediaPlayer != null) mMediaPlayer.reset();
    }
    
    public int getCurrentPosition() {
    	if(mMediaPlayer != null) {
    		return mMediaPlayer.getCurrentPosition();
    	} else {
    		return 0;
    	}
    }
    
    public int getDuration() {
    	if(mMediaPlayer != null)  {
    		return mMediaPlayer.getDuration();
    	} else {
    		return 0;
    	}
    }
    
    public void seekTo(int pos) {
    	if(mMediaPlayer != null) mMediaPlayer.seekTo(pos);
    }
    
    public int getAudioSessionId() {
    	if(mMediaPlayer != null) {
    		return mMediaPlayer.getAudioSessionId();
    	} else {
    		return 0;
    	}
    }
    
    public void setLooping(boolean looping) {
    	if(mMediaPlayer != null) mMediaPlayer.setLooping(looping);
    }
    
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
    	if(mMediaPlayer != null) mMediaPlayer.setOnCompletionListener(listener);
    }
}
