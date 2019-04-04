package com.yuning.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer.TrackInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekbar extends SeekBar {
	private static final String TAG = "VerticalSeekbar";
	private Drawable mThumb;
	private AudioManager mAudioManager;
	
	public interface OnSeekBarChangeListener{
		void onProgressChanged(VerticalSeekbar verticalSeekBar, int progress, boolean fromUser);
		void onStartTrackingTouch(VerticalSeekbar verticalSeekBar);
		void onStopTrackingTouch(VerticalSeekbar verticalSeekBar);
	}
	
	private OnSeekBarChangeListener mOnSeekBarChangeListener;

	public VerticalSeekbar(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public VerticalSeekbar(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.seekBarStyle);
		// TODO Auto-generated constructor stub
	}

	public VerticalSeekbar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mAudioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
		// TODO Auto-generated constructor stub
	}
	
	public void setOnSeekBarChangeListener(OnSeekBarChangeListener l){
		mOnSeekBarChangeListener = l;
	}
	
	void onStartTrackingTouch(){
		if(mOnSeekBarChangeListener != null){
			mOnSeekBarChangeListener.onStartTrackingTouch(this);
		}
	}
	
	void onStopTrackingTouch(){
		if(mOnSeekBarChangeListener != null){
			mOnSeekBarChangeListener.onStopTrackingTouch(this);
		}
	}
	
	void onProgressRefresh(float scale, boolean fromUser){
		Drawable thumb = mThumb;
		if(thumb != null){
			setThumbPos(getHeight(), thumb, scale, Integer.MIN_VALUE);
			invalidate();
		}
		if (mOnSeekBarChangeListener != null){
			mOnSeekBarChangeListener.onProgressChanged(this, getProgress(), fromUser);
		}
	}
	
	private void setThumbPos(int w, Drawable thumb, float scale, int gap) {
		int available = w + getPaddingLeft() - getPaddingRight();
		int thumbWidth = thumb.getIntrinsicWidth();
		int thumbHeight = thumb.getIntrinsicHeight();
		available -= thumbWidth;
		available += getThumbOffset() * 2;
		int thumbPos = (int) (scale * available);
		int topBound, bottomBound;
		if (gap == Integer.MIN_VALUE) {
			Rect oldBounds = thumb.getBounds();
			topBound = oldBounds.top;
			bottomBound = oldBounds.bottom;
		}else{
			topBound = gap;
			bottomBound = gap + thumbHeight;
		}
		thumb.setBounds(thumbPos, topBound, thumbPos + thumbWidth, bottomBound);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		canvas.rotate(-90);
		canvas.translate(-getHeight(), 0);
		super.onDraw(canvas);
	}

	@Override
	protected synchronized void onMeasure(int widthMeasureSpec,
			int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}
	
	@Override
	public void setThumb(Drawable thumb) {
		// TODO Auto-generated method stub
		mThumb = thumb;
		super.setThumb(thumb);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(h, w, oldh, oldw);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if(!isEnabled()){
			return false;
		}
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				setPressed(true);
				onStartTrackingTouch();
				trackTouchEvent(event);
				onSizeChanged(getWidth(), getHeight(), 0, 0);
				break;
			case MotionEvent.ACTION_MOVE:
				trackTouchEvent(event);
				attemptClaimDrag();
				onSizeChanged(getWidth(), getHeight(), 0, 0);
				break;
			case MotionEvent.ACTION_UP:
				trackTouchEvent(event);
				onStopTrackingTouch();
				setPressed(false);
				onSizeChanged(getWidth(), getHeight(), 0, 0);
				break;
			case MotionEvent.ACTION_CANCEL:
				break;
	
			default:
				break;
		}
		return true;
	}
	
	private void trackTouchEvent(MotionEvent event) {
		final int Height = getHeight();
		final int available = Height - getPaddingBottom() - getPaddingTop();
		int Y = (int) event.getY();
		float scale;
		float progress = 0;
		if (Y > Height - getPaddingBottom()) {
			scale = 0.0f;
		}else if(Y < getPaddingTop()){
			scale = 1.0f;
		}else{
			scale = (float) (Height - getPaddingBottom() - Y) / (float) available;
		}
		final int max = getMax();
		progress = scale * max;
		setProgress((int) progress);
		Log.d(TAG,"progress="+progress);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)progress, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
	}
	
	public void updateProgress(int progress){
		setProgress(progress);
		onSizeChanged(getWidth(), getHeight(), 0, 0);
	}
	
	private void attemptClaimDrag() {
		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
	}

}
