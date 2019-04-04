package com.yuning.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLinearLayout extends LinearLayout {

	public SquareLinearLayout(Context context) {
		super(context);
	}
	
	public SquareLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public SquareLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        /*final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }*/
        
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        /*final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }*/
        
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
        
        if(width < height) {
        	lp.setMargins(0, (height - width) / 2, 0, (height - width) / 2);
        	super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        } else {
        	lp.setMargins((width - height) / 2, 0, (width - height) / 2, 0);
        	super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        	
        }
        //setMeasuredDimension(getDefaultSize(0,widthMeasureSpec),getDefaultSize(0,heightMeasureSpec));
        //int childWidthSize = getMeasuredWidth();
        //int childHeightSize = getMeasuredHeight();
        // height is equal to  width
        //heightMeasureSpec = widthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize,childHeightSize);
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
}
