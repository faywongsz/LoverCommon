package com.yuning.activity;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.yuning.lovercommon.R;
import com.yuning.util.LoadThumbnail;
import com.yuning.util.MediaInfo;

public class VideoAdd extends BaseModeActivity implements AdapterView.OnItemClickListener{
	private static final String TAG = VideoAdd.class.getSimpleName();
	
	public static final String INTENT_DATA = "data";
	
	private ListView mList;
	private InfoAdapter mVideoAdapter;
	private ArrayList<MediaInfo> mSelectedInfos;
	private ArrayList<MediaInfo> mVideoInfos;
	private boolean[] mVideoChecked = null;
	private LoadThumbnail mLoadThumbnail = null;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_add);
		
		mSelectedInfos = (ArrayList<MediaInfo>) getIntent().getSerializableExtra(MusicAdd.INTENT_DATA);
		
		initTitleBar(R.string.video_add, 0, R.string.video_add_done, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayList<MediaInfo> data = new ArrayList<MediaInfo>();
				Intent intent = new Intent();
				int count = 0;
				for(int i = 0; i < mVideoChecked.length; i++) {
					if(mVideoChecked[i]) {
						data.add(count++, mVideoInfos.get(i));
					}
				}
				
				intent.putExtra(INTENT_DATA, (Serializable)data);
				Log.d(TAG, "count = " + count);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});
		initView();
	}
	
	private void initView() {
		mList = (ListView) findViewById(R.id.video_add_list);
		mVideoInfos = getVideoInfos();
		mVideoChecked = new boolean[mVideoInfos.size()];
		for(int i = 0; mSelectedInfos != null && i < mSelectedInfos.size(); i++) {
			for(int j = 0; mVideoInfos != null && j < mVideoInfos.size(); j++) {
				if(mSelectedInfos.get(i).getId() == mVideoInfos.get(j).getId()) {
					mVideoChecked[j] = true;
					break;
				}
			}
		}
		
		mVideoAdapter = new InfoAdapter(getApplicationContext(), mVideoInfos);
		mList.setAdapter(mVideoAdapter);
		mList.setOnItemClickListener(this);
	}
	
	private ArrayList<MediaInfo> getVideoInfos() {
		ArrayList<MediaInfo> infos = new ArrayList<MediaInfo>();
		
		Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, 
				null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
		while(cursor.moveToNext()) {
			MediaInfo info = new MediaInfo();
			
			long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID)); 
			String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE)); 
			String url = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)); 
			long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION)); 
			
			info.setId(id);
			info.setTitle(title);
			info.setURL(url);
			info.setDuration(duration);
			
			infos.add(info);
		}
		
		return infos;
	}
	
	private class ViewHolder {
		TextView mTitle;
		ImageView mImage;
		TextView mDuration;
		CheckBox mCheckBox;
	}
	
	private class InfoAdapter extends BaseAdapter {

		private Context mContext;
		private ArrayList<MediaInfo> mInfos;
		BitmapFactory.Options mOptions;
		
		public InfoAdapter(Context context, ArrayList<MediaInfo> infos) {
			mContext = context;
			mInfos = infos;
			mOptions = new BitmapFactory.Options();
			mOptions.inDither = false;
			mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
		}

		@Override
		public int getCount() {
			return mInfos.size();
		}

		@Override
		public Object getItem(int position) {
			return mInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			MediaInfo info = mInfos.get(position);
			
			if(convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(mContext).inflate(R.layout.video_add_item, null);
				
				holder.mTitle = (TextView) convertView.findViewById(R.id.item_title);
				holder.mImage = (ImageView) convertView.findViewById(R.id.item_img);
				holder.mDuration = (TextView) convertView.findViewById(R.id.item_duration);
				holder.mCheckBox = (CheckBox) convertView.findViewById(R.id.check_add);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.mTitle.setText(info.getTitle());
			holder.mDuration.setText(MediaInfo.toTime((int) info.getDuration()));
			holder.mImage.setTag(info);
			if(mLoadThumbnail == null) {
				mLoadThumbnail = new LoadThumbnail(getApplicationContext());
			}
			mLoadThumbnail.loadThumbnail(holder.mImage, info, new LoadThumbnail.OnLoadFinished() {
				@Override
				public void onLoadFinished(ImageView imageView, MediaInfo info,
						Bitmap bitmap) {
					ImageView image = (ImageView) mList.findViewWithTag(info);
					image.setImageBitmap(bitmap);
				}
			});
			
			holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mVideoChecked[position] = isChecked;
				}
			});
			holder.mCheckBox.setChecked(mVideoChecked[position]);
			
			return convertView;
		}
		
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		boolean checked = mVideoChecked[position];
		CheckBox checkBox = (CheckBox) view.findViewById(R.id.check_add);
		checkBox.setChecked(!checked);
		mVideoChecked[position] = !checked;
	}
	

}
