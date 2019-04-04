package com.yuning.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public class MusicCoverWorkspace extends ViewGroup {
	private static final String TAG = MusicCoverWorkspace.class.getSimpleName();
	
	private static final boolean DEBUG_UI = true;
	
    private int mCurrentScreen;
    private boolean mFirstLayout = true;
    
    private Scroller mScroller;    
    private VelocityTracker mVelocityTracker;    
    
    private int mScrollX = 0;    
       
    private float mLastMotionX;    
       
    private static final int SNAP_VELOCITY = 1000;    
       
    private final static int TOUCH_STATE_REST = 0;    
    private final static int TOUCH_STATE_SCROLLING = 1;    
      
    private int mTouchState = TOUCH_STATE_REST;    
     
    private int mTouchSlop = 0;
    
    private WorkspaceOvershootInterpolator mScrollInterpolator;
    
    private onPageChangedListener mOnPageChangedListener;

    private static final float BASELINE_FLING_VELOCITY = 2500.f;
    private static final float FLING_VELOCITY_INFLUENCE = 0.4f;
    
    private static class WorkspaceOvershootInterpolator implements Interpolator {
        private static final float DEFAULT_TENSION = 1.3f;
        private float mTension;

        public WorkspaceOvershootInterpolator() {
            mTension = DEFAULT_TENSION;
        }
        
        public void setDistance(int distance) {
            mTension = distance > 0 ? DEFAULT_TENSION / distance : DEFAULT_TENSION;
        }

        public void disableSettle() {
            mTension = 0.f;
        }

        public float getInterpolation(float t) {
            // _o(t) = t * t * ((tension + 1) * t + tension)
            // o(t) = _o(t - 1) + 1
            t -= 1.0f;
            return t * t * ((mTension + 1) * t + mTension) + 1.0f;
        }
    }
    
	public MusicCoverWorkspace(Context context) {
		this(context, null);
	}

	public MusicCoverWorkspace(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public MusicCoverWorkspace(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		
		mScrollInterpolator = new WorkspaceOvershootInterpolator();
		mScroller = new Scroller(context, mScrollInterpolator); 
		   
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	@Override
	public void addView(View child) {
        if (!(child instanceof MusicCover)) {
            throw new IllegalArgumentException("MusicCoverWorkspace can only have MusicCover children.");
        }
		super.addView(child);
	}

	@Override
	public void addView(View child, int index) {
        if (!(child instanceof MusicCover)) {
            throw new IllegalArgumentException("MusicCoverWorkspace can only have MusicCover children.");
        }
		super.addView(child, index);
	}

	@Override
	public void addView(View child, int width, int height) {
        if (!(child instanceof MusicCover)) {
            throw new IllegalArgumentException("MusicCoverWorkspace can only have MusicCover children.");
        }
		super.addView(child, width, height);
	}

	@Override
	public void addView(View child, LayoutParams params) {
        if (!(child instanceof MusicCover)) {
            throw new IllegalArgumentException("MusicCoverWorkspace can only have MusicCover children.");
        }
		super.addView(child, params);
	}

	@Override
	public void addView(View child, int index, LayoutParams params) {
        if (!(child instanceof MusicCover)) {
            throw new IllegalArgumentException("MusicCoverWorkspace can only have MusicCover children.");
        }
		super.addView(child, index, params);
	}

	public int getCurrentScreen(){
		return mCurrentScreen;
	}
    
    @Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mVelocityTracker == null) {    
			mVelocityTracker = VelocityTracker.obtain();    
		}    
		mVelocityTracker.addMovement(ev);    
		
    	final int action = ev.getAction();    
    	
    	if ((action == MotionEvent.ACTION_MOVE)    
    			&& (mTouchState != TOUCH_STATE_REST)) {    
    		return true;    
    	}    
    	   
    	final float x = ev.getX();    
    	   
    	switch (action) {    
    		case MotionEvent.ACTION_MOVE:    
    	   
		    	final int xDiff = (int) Math.abs(x - mLastMotionX);    
		    	   
		    	boolean xMoved = xDiff > mTouchSlop;    
		    	   
		    	if (xMoved) {    
			    	// Scroll if the user moved far enough along the X axis    
			    	mTouchState = TOUCH_STATE_SCROLLING;    
		    	}    
		    	break;    
    	   
    		case MotionEvent.ACTION_DOWN:    
		    	// Remember location of down touch    
		    	mLastMotionX = x;    
		    	   
		    	/*   
		    	* 如果被flinged和用户触动屏幕上启动 
		    	*/   
		    	mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;    
		    	break;    
    	   
	    	case MotionEvent.ACTION_CANCEL:    
	    	case MotionEvent.ACTION_UP:    
		    	// Release the drag    
		    	mTouchState = TOUCH_STATE_REST;    
		    	break;    
    	}    
    	   
    	/*   
    	* 我们唯一的一次想拦截移动事件是我们在拖动模式。  
    	*/   
    	return mTouchState != TOUCH_STATE_REST; 
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mVelocityTracker == null) {    
			mVelocityTracker = VelocityTracker.obtain();    
		}    
		mVelocityTracker.addMovement(event);    
			   
		final int action = event.getAction();    
		final float x = event.getX();    
			   
		switch (action) {    
			case MotionEvent.ACTION_DOWN:    
				if(DEBUG_UI) Log.i(TAG, "event : down");    
				/*   
				* 如果被flinged和用户的触摸,停止fling.结束   
				*/   
				if (!mScroller.isFinished()) {    
					mScroller.abortAnimation();    
				}    
			   
				mLastMotionX = x;    
				break;    
			case MotionEvent.ACTION_MOVE:    
				// WristBandLogUtils.i(TAG,"event : move");    
				// if (mTouchState == TOUCH_STATE_SCROLLING) {    
				// Scroll to follow the motion event    
				final int deltaX = (int) (mLastMotionX - x);    
				mLastMotionX = x;    
				   
				if(DEBUG_UI) Log.i(TAG, "event : move, deltaX " + deltaX + ", mScrollX " + mScrollX);    
				   
				if (deltaX < 0) {    
					if (mScrollX > 0) {    
						scrollBy(Math.max(-mScrollX, deltaX), 0);    
					} else{
						scrollBy(deltaX >> 1, 0);
					}
				} else if (deltaX > 0) {    
					final int availableToScroll = getChildAt(getChildCount() - 1).getRight()- mScrollX - getWidth();    
					if (availableToScroll > 0) {    
						scrollBy(Math.min(availableToScroll, deltaX), 0);    
					}else{
						scrollBy(deltaX >> 1, 0);
					}
				}    
				   
				break;    
			case MotionEvent.ACTION_UP:    
				if(DEBUG_UI) Log.i(TAG, "event : up");    
				// if (mTouchState == TOUCH_STATE_SCROLLING) {    
				final VelocityTracker velocityTracker = mVelocityTracker;    
				velocityTracker.computeCurrentVelocity(1000);    
				int velocityX = (int) velocityTracker.getXVelocity();    
				   
				if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {    
					// Fling hard enough to move left    
					snapToScreen(mCurrentScreen - 1, velocityX, true);    
				} else if (velocityX < -SNAP_VELOCITY&& mCurrentScreen < getChildCount() - 1) {    
					// Fling hard enough to move right    
					snapToScreen(mCurrentScreen + 1, velocityX, true);    
				} else {    
					snapToDestination();    
				}    
				   
				if (mVelocityTracker != null) {    
					mVelocityTracker.recycle();    
					mVelocityTracker = null;    
				}    
				// }    
				mTouchState = TOUCH_STATE_REST;    
				break;    
			case MotionEvent.ACTION_CANCEL:    
				if(DEBUG_UI) Log.i(TAG, "event : cancel");    
				mTouchState = TOUCH_STATE_REST;   
				break;
		}   
		
		mScrollX = getScrollX();    
		return true; 
	}

	private void snapToDestination() {    
		final int screenWidth = getWidth();    
		final int whichScreen = (mScrollX + (screenWidth / 2)) / screenWidth;   
		if(DEBUG_UI) Log.i(TAG, "from des");    
		snapToScreen(whichScreen, 0, false);    
	}    
		   
	public void snapToScreen(int whichScreen, int velocity, boolean settle) {     
		if(DEBUG_UI) Log.i(TAG, "snap To Screen " + whichScreen);    
		mCurrentScreen = whichScreen;  
		
		if(mOnPageChangedListener != null) mOnPageChangedListener.onPageChanged(mCurrentScreen);

		final int newX = whichScreen * getWidth();    
		final int delta = newX - mScrollX;    
        final int screenDelta = Math.max(1, Math.abs(whichScreen - mCurrentScreen));
        int duration = (screenDelta + 1) * 100;

        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        
        if (settle) {
            mScrollInterpolator.setDistance(screenDelta);
        } else {
            mScrollInterpolator.disableSettle();
        }
        
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration += (duration / (velocity / BASELINE_FLING_VELOCITY))
                    * FLING_VELOCITY_INFLUENCE;
        } else {
            duration += 100;
        }
        
		mScroller.startScroll(mScrollX, 0, delta, 0, duration);     
		invalidate();    
	}    
		   
	public void setToScreen(int whichScreen) {    
		if(DEBUG_UI) Log.i(TAG, "set To Screen " + whichScreen);    
		mCurrentScreen = whichScreen;    
		if(mOnPageChangedListener != null) mOnPageChangedListener.onPageChanged(mCurrentScreen);
		final int newX = whichScreen * getWidth();    
		mScroller.startScroll(newX, 0, 0, 0, 10);     
		invalidate();    
	}    

	
	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {    
			mScrollX = mScroller.getCurrX();    
			scrollTo(mScrollX, 0);    
			postInvalidate();    
		} 
	}

	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }
        
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }
        if(DEBUG_UI) Log.d(TAG, "width = " + width + " height = " + height);
        
        // The children are given the same width and height as the workspace
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }


        if (mFirstLayout) {
            setHorizontalScrollBarEnabled(false);
            scrollTo(mCurrentScreen * width, 0);
            setHorizontalScrollBarEnabled(true);
            mFirstLayout = false;
        }
    }
    
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
	}
	
	public void setonPageChangedListener(onPageChangedListener listener) {
		mOnPageChangedListener = listener;
	}
	
	public interface onPageChangedListener {
		public void onPageChanged(int index);
	}
}

