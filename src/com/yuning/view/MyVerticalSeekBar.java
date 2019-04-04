package com.yuning.view;

import com.yuning.lovercommon.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MyVerticalSeekBar extends View {
	private static final String TAG = MyVerticalSeekBar.class.getSimpleName();
	
	private int mWidth, mHeight;
	private int mProgress = 0;
	private int mMax = 10;
	private Drawable mThumb;
	private Drawable mProgressDrawable;
	private int mProgressWidth = 1;
	
	public interface OnSeekBarChangeListener{
		void onProgressChanged(int progress);
	}
	
	private OnSeekBarChangeListener mOnSeekBarChangeListener;
	
	public MyVerticalSeekBar(Context context) {
		this(context, null);
	}

	public MyVerticalSeekBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public MyVerticalSeekBar(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Seekbar, defStyleAttr, 0);
        
        mThumb = a.getDrawable(R.styleable.Seekbar_thumb);
        Drawable drawable = a.getDrawable(R.styleable.Seekbar_progressDrawable);
        if(drawable instanceof LayerDrawable) {
        	mProgressDrawable = ((LayerDrawable) drawable).findDrawableByLayerId(android.R.id.background);
        }
        
        mProgressWidth *= context.getResources().getDisplayMetrics().density;
        
        a.recycle();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(mProgressDrawable != null) {
			canvas.save();
			mProgressDrawable.draw(canvas);
			canvas.restore();
		}
		
		if(mThumb != null) {
			canvas.save();
			mThumb.draw(canvas);
			canvas.restore();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		mWidth = MeasureSpec.getSize(widthMeasureSpec);
		//final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		
		mHeight = MeasureSpec.getSize(heightMeasureSpec);
		//final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		
        /*if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("MyVerticalSeekBar cannot have UNSPECIFIED dimensions");
        }*/
        Log.d(TAG, "width = " + mWidth + " height = " + mHeight);
		
		setMeasuredDimension(mWidth, mHeight);
		
		updateThumbPos(mWidth, mHeight);
		updateProgressDrawableBounds(mWidth, mHeight);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				setPressed(true);
				trackTouchEvent(event);
				break;
			case MotionEvent.ACTION_MOVE:
				trackTouchEvent(event);
				break;
			case MotionEvent.ACTION_UP:
				trackTouchEvent(event);
				setPressed(false);
				break;
			case MotionEvent.ACTION_CANCEL:
				break;
	
			default:
				break;
		}
		return true;
	}
	
	public void setMax(int max) {
		mMax = max;
	}
	
	public int getMax() {
		return mMax;
	}
	
	public int getProgress() {
		return mProgress;
	}
	
	public void setProgress(int progress) {
		mProgress = progress;
		
        int max = getMax();
        float scale = max > 0 ? (float) getProgress() / (float) max : 0;
        
		setThumbPos(mHeight, mThumb, scale, Integer.MIN_VALUE);
		invalidate();
		
		if(mOnSeekBarChangeListener != null) {
			mOnSeekBarChangeListener.onProgressChanged(progress);
		}
		Log.d(TAG,"progress="+progress);
	}
	
	public void setOnSeekBarChangeListener(OnSeekBarChangeListener l){
		mOnSeekBarChangeListener = l;
	}
	
	private void trackTouchEvent(MotionEvent event) {
		final int available = mHeight - getPaddingBottom() - getPaddingTop();
		int y = (int) event.getY();
		float scale;
		float progress = 0;
		if (y > mHeight - getPaddingBottom()) {
			scale = 0.0f;
		}else if(y < getPaddingTop()){
			scale = 1.0f;
		}else{
			scale = (float) (mHeight - getPaddingBottom() - y) / (float) available;
		}
		final int max = getMax();
		progress = scale * max;
		setProgress((int) progress);
	}
	
	private void updateProgressDrawableBounds(int w, int h) {
		Drawable progress = mProgressDrawable;
        int drawableWidth = progress == null ? 0 : mProgressWidth;
        int trackWidth = w - getPaddingLeft() - getPaddingRight();
        
        if (progress != null) {
        	progress.setCallback(this);
        }
        
        if(drawableWidth > trackWidth) {
        	if(progress != null) {
        		progress.setBounds(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        	}
        } else {
        	int gap = (trackWidth - drawableWidth) / 2;
        	if(progress != null) {
        		progress.setBounds(gap, getPaddingTop(), gap + mProgressWidth, h - getPaddingBottom());
        	}
        }
        
	}
	
    private void updateThumbPos(int w, int h) {
        Drawable thumb = mThumb;
        int thumbWidth = thumb == null ? 0 : thumb.getIntrinsicWidth();
        // The max height does not incorporate padding, whereas the height
        // parameter does
        int trackWidth = w - getPaddingLeft() - getPaddingRight();
        
        int max = getMax();
        float scale = max > 0 ? (float) getProgress() / (float) max : 0;
        
        if (thumbWidth > trackWidth) {
            if (thumb != null) {
                setThumbPos(h, thumb, scale, 0);
            }
        } else {
            int gap = (trackWidth - thumbWidth) / 2;
            if (thumb != null) {
                setThumbPos(h, thumb, scale, gap);
            }
        }
    }
    
    private void setThumbPos(int h, Drawable thumb, float scale, int gap) {
		final int thumb_w = thumb.getIntrinsicWidth();
		final int thumb_h = thumb.getIntrinsicHeight();
		final int available = h - getPaddingBottom() - getPaddingTop() - thumb_h;
		final int bottom = (int) (h - getPaddingBottom() - scale * available);
		
		int leftBound, rightBound;
		if(gap == Integer.MIN_VALUE) {
			Rect oldBounds = thumb.getBounds();
			leftBound = oldBounds.left;
			rightBound = oldBounds.right;
		} else {
			leftBound = gap;
			rightBound = gap + thumb_w;
		}
		
		thumb.setBounds(leftBound, bottom - thumb_h, rightBound, bottom);
    }
}
