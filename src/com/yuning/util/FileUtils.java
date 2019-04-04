package com.yuning.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class FileUtils {
    public static String getPath(Context context, Uri uri, boolean audioOrvideo) {
 
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.Video.Media.DATA };
            Cursor cursor = null;
 
            try {
            	String document_id = uri.getLastPathSegment();
            	long id = Long.parseLong(document_id.substring(document_id.indexOf(":") + 1));
                cursor = context.getContentResolver().query(
                		ContentUris.withAppendedId(audioOrvideo ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), 
                		projection,null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
 
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
 
        return null;
    }
}
