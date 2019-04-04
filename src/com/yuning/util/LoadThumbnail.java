package com.yuning.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.widget.ImageView;

import com.yuning.lovercommon.R;

public class LoadThumbnail {
	private static final String TAG = LoadThumbnail.class.getSimpleName();
	
	private Context mContext;
	private HashMap<Long, LoadThumbnailTask> mTaskMap = new HashMap<Long, LoadThumbnailTask>();  
	private HashMap<Long, SoftReference<Bitmap>> mImageCaches = new HashMap<Long, SoftReference<Bitmap>>(); 
	
	public interface OnLoadFinished {
		public void onLoadFinished(ImageView imageView, MediaInfo info, Bitmap bitmap);
	}
	
	public LoadThumbnail(Context context) {
		mContext = context;
	}
	
    public void loadThumbnail(ImageView imageView, MediaInfo info, OnLoadFinished load){  
        SoftReference<Bitmap> currBitmap = mImageCaches.get(info.getId());  
        Bitmap softRefBitmap = null;  
        if(currBitmap != null){  
            softRefBitmap = currBitmap.get();  
        }  

        if(currBitmap != null && imageView != null && softRefBitmap != null 
        		&& info.getId() == ((MediaInfo)imageView.getTag()).getId()){  
        	imageView.setImageBitmap(softRefBitmap);  
        }else if(info != null && !isTasksContains(info.getId())){  
        	LoadThumbnailTask task = new LoadThumbnailTask(imageView, info, load);  
            if(imageView != null){  
                task.execute();  
                mTaskMap.put(info.getId(), task);  
            }  
        }  
    }  
      
    private boolean isTasksContains(long id){  
        boolean b = false;  
        if(mTaskMap != null && mTaskMap.get(id) != null){  
            b = true;  
        }  
        return b;  
    }  
      
    private void removeTaskFormMap(long id){  
        if(mTaskMap != null && mTaskMap.get(id) != null){  
        	mTaskMap.remove(id);  
        }  
    }  
    
    private class LoadThumbnailTask extends AsyncTask<String, Void, Bitmap> {
    	private ImageView mImageView;  
    	private MediaInfo mInfo;
    	private OnLoadFinished mLoad;
    	
    	public LoadThumbnailTask(ImageView imageView, MediaInfo info, OnLoadFinished load) {
    		mImageView = imageView;
    		mInfo = info;
    		mLoad = load;
    		Log.d(TAG, info.toString());
    	}
    	
    	@Override
    	protected Bitmap doInBackground(String... params) {
    		Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(mInfo.getURL(), Images.Thumbnails.MICRO_KIND);
    		/*bitmap = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), info.getId(),
    				Images.Thumbnails.MICRO_KIND, mOptions);*/
    		
    		if(bitmap == null) {
    			bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.video_mode_default_thumbnail);
    		}
    		mImageCaches.put(mInfo.getId(), new SoftReference<Bitmap>(bitmap));
    		
    		return bitmap;
    	}

    	@Override
    	protected void onPostExecute(Bitmap result) {
    		if(result != null) {
    			if(mLoad != null) {
    				mLoad.onLoadFinished(mImageView, mInfo, result);
        			removeTaskFormMap(mInfo.getId());
    			}
    		}
    	}
    	
    }
}

