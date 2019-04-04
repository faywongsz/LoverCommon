package com.yuning.ui;

import com.yuning.lovercommon.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class MusicCover extends ImageView {

	private static Animation mAnimation = null;
	private static Bitmap mForegroundCover = null;
	private static int COVER_EDGE = 2;
	private static int DEGRESS_INTERVAL = 1;
	
	private Bitmap mCoverImage;
	private boolean mbRotate = false;
	private int mRotateDegress = 0;
	private boolean mbDefault = false;
	private float mDenstiy;
	
	public MusicCover(Context context) {
		this(context, null, 0);
	}


	public MusicCover(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MusicCover(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setScaleType(ScaleType.CENTER_INSIDE);
		mDenstiy = getResources().getDisplayMetrics().density;
	}

	@Override
	protected void onDraw(Canvas canvas) {
	    Drawable drawable = getDrawable() ;  
        if (drawable == null) { 
            return; 
        } 
        if (getWidth() == 0 || getHeight() == 0) { 
            return; 
        } 
        if (drawable.getClass() == NinePatchDrawable.class) {
        	return; 
        }
            
        Bitmap b = ((BitmapDrawable) drawable).getBitmap(); 
        Bitmap bitmap = b.copy(Bitmap.Config.ARGB_8888, true); 
        
		final int width = getWidth() - getPaddingLeft() - getPaddingRight();
		final int height = getHeight() - getPaddingTop() - getPaddingBottom();
		final int diameter = width < height ? width : height;
		
		if(mCoverImage == null) {
			mCoverImage = getCoverImageBitmap(bitmap, diameter);
		}
		canvas.save();
		if(mbRotate) {
			mRotateDegress += DEGRESS_INTERVAL;
			mRotateDegress %= 360;
		}
		canvas.rotate(mRotateDegress, getWidth() / 2, getHeight() / 2);
		canvas.drawBitmap(mCoverImage, (width - diameter) / 2 + getPaddingLeft(), 
				(height - diameter) / 2 + getPaddingTop(), null);
		canvas.restore();
		if(mbRotate) {
			invalidate();
		}
	}

	public void setCoverImageBitmap(Bitmap bitmap, boolean isDefault) {
		setImageBitmap(bitmap);
		mbDefault = isDefault;
	}
	
	public Bitmap getCoverImageBitmap(Bitmap bitmap, int diameter) {
		final int bmpWidth = bitmap.getWidth(); 
		final int bmpHeight = bitmap.getHeight(); 
        int squareWidth = 0, squareHeight = 0; 
        int x = 0, y = 0; 
        Bitmap squareBitmap, scaledSrcBmp, outputBmp; 
        if (bmpHeight > bmpWidth) {// 高大于宽 
            squareWidth = squareHeight = bmpWidth; 
            x = 0; 
            y = (bmpHeight - bmpWidth) / 2; 
            // 截取正方形图片 
            squareBitmap = Bitmap.createBitmap(bitmap, x, y, squareWidth, squareHeight); 
        } else if (bmpHeight < bmpWidth) {// 宽大于高 
            squareWidth = squareHeight = bmpHeight; 
            x = (bmpWidth - bmpHeight) / 2; 
            y = 0; 
            squareBitmap = Bitmap.createBitmap(bitmap, x, y, squareWidth,squareHeight); 
        } else { 
            squareBitmap = bitmap; 
        } 
		
        if (squareBitmap.getWidth() != diameter || squareBitmap.getHeight() != diameter) { 
            scaledSrcBmp = Bitmap.createScaledBitmap(squareBitmap, diameter,diameter, true);
        } else { 
            scaledSrcBmp = squareBitmap; 
        }
        
        outputBmp = Bitmap.createBitmap(scaledSrcBmp.getWidth(), 
                scaledSrcBmp.getHeight(),  
                Config.ARGB_8888); 
        Canvas canvas = new Canvas(outputBmp); 
   
        Paint paint = new Paint(); 
        Rect rect = new Rect(0, 0, scaledSrcBmp.getWidth(),scaledSrcBmp.getHeight()); 
   
        paint.setAntiAlias(true); 
        paint.setFilterBitmap(true); 
        paint.setDither(true); 
        canvas.drawARGB(0, 0, 0, 0); 
        canvas.drawCircle(scaledSrcBmp.getWidth() / 2, 
                scaledSrcBmp.getHeight() / 2,  
                mbDefault ? scaledSrcBmp.getWidth() / 2 : (scaledSrcBmp.getWidth() / 2 - COVER_EDGE * mDenstiy), 
                paint); 
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN)); 
        
        canvas.drawBitmap(scaledSrcBmp, rect, rect, paint); 
        if(!mbDefault) {
            paint.setXfermode(null);
            canvas.drawBitmap(getForegroundCover(getContext(), rect.width(), rect.height()), rect, rect, paint);
        }
        
        bitmap.recycle();
        bitmap = null; 
        squareBitmap.recycle();
        squareBitmap = null; 
        scaledSrcBmp.recycle();
        scaledSrcBmp = null; 
        
        return outputBmp;
	}
	
	public static Bitmap getForegroundCover(Context context, int width, int height) {
		if(mForegroundCover == null) {
			mForegroundCover = Bitmap.createBitmap(width, height,  Config.ARGB_8888); 
	        Canvas canvas = new Canvas(mForegroundCover); 
	        
	        Paint paint = new Paint(); 
	        paint.setAntiAlias(true); 
	        paint.setFilterBitmap(true); 
	        paint.setDither(true); 
	        canvas.drawARGB(0, 0, 0, 0); 
	        
	        Bitmap cover = BitmapFactory.decodeResource(context.getResources(), R.drawable.music_album_cover_foreground);
	        Rect src = new Rect(0, 0, cover.getWidth(), cover.getHeight());
	        Rect dst = new Rect(0, 0, width, height); 
	        
	        canvas.drawBitmap(cover, src, dst, paint); 
	        
	        cover.recycle();
	        cover = null;
		}
		
		return mForegroundCover;
	}

	public void startRotate() {
		mbRotate = true;
		invalidate();
	}
	
	public void pauseRotate() {
		mbRotate = false;
		invalidate();
	}
	
	public void stopRotate() {
		mbRotate = false;
		mRotateDegress = 0;
		invalidate();
	}
	
	public static Animation getRotateAnimation() {
		if(mAnimation == null) {
			mAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			mAnimation.setInterpolator(new LinearInterpolator());
			mAnimation.setDuration(6000);
			mAnimation.setFillAfter(true);
			mAnimation.setRepeatCount(Animation.INFINITE);
			mAnimation.setRepeatMode(Animation.RESTART);
		}
		
		return mAnimation;
	}
}
