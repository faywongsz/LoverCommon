package com.yuning.view;

import com.yuning.lovercommon.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class ShakeProgressBar extends View {
	private Drawable mDrawable;
	private int mMaxLevel = 25;
	private int mLevel = 0;
	public ShakeProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	private void init(Context context){
		mDrawable = context.getResources().getDrawable(R.drawable.shake_progress);
	}
	
	public void updateLevel(int level){
		/*if(level >= 0 && level < mMaxLevel){
			mLevel = level - (level + 1) / 5;
		}*/
		mLevel = level;
		invalidate();
	}
	  
    @Override  
    protected void onDraw(Canvas canvas) {  
        super.onDraw(canvas);  
        canvas.save();
        canvas.clipRect(0, 0, getWidth() * mLevel / mMaxLevel, getHeight());
        mDrawable.setBounds(0, 0, getWidth(), getHeight());
        mDrawable.draw(canvas);
        canvas.restore();
    }  
}
