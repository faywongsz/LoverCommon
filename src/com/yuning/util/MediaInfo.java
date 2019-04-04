package com.yuning.util;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Locale;

import com.yuning.lovercommon.R;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

public class MediaInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String SEPARATE_STR = ",,##";
	
	private long mId;
	private long mAlbumId;
	private String mTitle;
	private String mArtist;
	private String mAlbum;
	private String mURL;
	private long mDuration;
	
	public long getId() {
		return mId;
	}
	
	public long getAlbumId() {
		return mAlbumId;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getArtist() {
		return mArtist;
	}
	
	public String getAlbum() {
		return mAlbum;
	}
	
	public String getURL() {
		return mURL;
	}
	
	public long getDuration() {
		return mDuration;
	}
	
	public void setId(long id) {
		mId = id;
	}
	
	public void setAlbumId(long id) {
		mAlbumId = id;
	}
	
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public void setArtist(String artist) {
		mArtist = artist;
	}
	
	public void setAlbum(String album) {
		mAlbum = album;
	}
	
	public void setURL(String url) {
		mURL = url;
	}
	
	public void setDuration(long duration) {
		mDuration = duration;
	}
	
	public static String toTime(int duration) {
		int time = duration / 1000;
		int minute = time / 60;
		//int hour = minute / 60;
		int second = time % 60;
		
		return String.format(Locale.getDefault(), "%02d:%02d", minute, second);
	}
	
	@Override
	public String toString() {
		return mId + SEPARATE_STR
				+ mAlbumId + SEPARATE_STR
				+ mTitle + SEPARATE_STR
				+ mArtist + SEPARATE_STR
				+ mAlbum + SEPARATE_STR
				+ mURL + SEPARATE_STR
				+ mDuration;
	}

	public static MediaInfo genMediaInfo(String string) {
		MediaInfo info = null;
		if(string != null) {
			String[] datas = string.split(SEPARATE_STR);
			
			if(datas.length == 7) {
				info = new MediaInfo();
				info.setId(Long.parseLong(datas[0]));
				info.setAlbumId(Long.parseLong(datas[1]));
				info.setTitle(datas[2]);
				info.setArtist(datas[3]);
				info.setAlbum(datas[4]);
				info.setURL(datas[5]);
				info.setDuration(Long.parseLong(datas[6]));
			}
		}
		return info;
	}
	
	public static Bitmap getArtwork(Context context, long song_id, long album_id,  
            boolean allowdefault) {  
        if (album_id < 0) {  
            // This is something that is not in the database, so get the album art directly  
            // from the file.  
            if (song_id >= 0) {  
                Bitmap bm = getArtworkFromFile(context, song_id, -1);  
                if (bm != null) {  
                    return bm;  
                }  
            }  
            if (allowdefault) {  
                return getDefaultArtwork(context);  
            }  
            return null;  
        }  
        ContentResolver res = context.getContentResolver();  
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);  
        if (uri != null) {  
            InputStream in = null;  
            try {  
                in = res.openInputStream(uri);  
                return BitmapFactory.decodeStream(in, null, sBitmapOptions);  
            } catch (FileNotFoundException ex) {  
                // The album art thumbnail does not actually exist. Maybe the user deleted it, or  
                // maybe it never existed to begin with.  
                Bitmap bm = getArtworkFromFile(context, song_id, album_id);  
                if (bm != null) {  
                    if (bm.getConfig() == null) {  
                        bm = bm.copy(Bitmap.Config.RGB_565, false);  
                        if (bm == null && allowdefault) {  
                            return getDefaultArtwork(context);  
                        }  
                    }  
                } else if (allowdefault) {  
                    bm = getDefaultArtwork(context);  
                }  
                return bm;  
            } finally {  
                try {  
                    if (in != null) {  
                        in.close();  
                    }  
                } catch (IOException ex) {  
                }  
            }  
        }  
          
        return null;  
    }  
      
    private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {  
        Bitmap bm = null;  
        if (albumid < 0 && songid < 0) {  
            throw new IllegalArgumentException("Must specify an album or a song id");  
        }  
        try {  
            if (albumid < 0) {  
                Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");  
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");  
                if (pfd != null) {  
                    FileDescriptor fd = pfd.getFileDescriptor();  
                    bm = BitmapFactory.decodeFileDescriptor(fd);  
                }  
            } else {  
                Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);  
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");  
                if (pfd != null) {  
                    FileDescriptor fd = pfd.getFileDescriptor();  
                    bm = BitmapFactory.decodeFileDescriptor(fd);  
                }  
            }  
        } catch (FileNotFoundException ex) {  
   
        }  
        if (bm != null) {  
        }  
        return bm;  
    }  
      
    public static Bitmap getDefaultArtwork(Context context) {  
        BitmapFactory.Options opts = new BitmapFactory.Options();  
        opts.inPreferredConfig = Bitmap.Config.RGB_565;          
        return BitmapFactory.decodeStream(  
                context.getResources().openRawResource(R.drawable.music_album_cover_default), null, opts);                 
    }  
    
    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");  
    private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();  
}
