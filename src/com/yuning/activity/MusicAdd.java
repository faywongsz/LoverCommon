package com.yuning.activity;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.ListView;
import android.widget.TextView;

import com.yuning.lovercommon.R;
import com.yuning.util.MediaInfo;

public class MusicAdd extends BaseModeActivity implements AdapterView.OnItemClickListener{
	private static final String TAG = MusicAdd.class.getSimpleName();
	
	public static final String INTENT_DATA = "data";
	
	private ListView mList;
	private InfoAdapter mMusicAdapter;
	private ArrayList<MediaInfo> mSelectedInfos;
	private ArrayList<MediaInfo> mMusicInfos;
	private boolean[] mMusicChecked = null;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_add);
		
		mSelectedInfos = (ArrayList<MediaInfo>) getIntent().getSerializableExtra(MusicAdd.INTENT_DATA);
		
		initTitleBar(R.string.music_add, 0, R.string.music_add_done, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayList<MediaInfo> data = new ArrayList<MediaInfo>();
				Intent intent = new Intent();
				int count = 0;
				for(int i = 0; i < mMusicChecked.length; i++) {
					if(mMusicChecked[i]) {
						data.add(count++, mMusicInfos.get(i));
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
		mList = (ListView) findViewById(R.id.music_add_list);
		mMusicInfos = getMusicInfos();
		mMusicChecked = new boolean[mMusicInfos.size()];
		for(int i = 0; mSelectedInfos != null && i < mSelectedInfos.size(); i++) {
			for(int j = 0; mMusicInfos != null && j < mMusicInfos.size(); j++) {
				if(mSelectedInfos.get(i).getId() == mMusicInfos.get(j).getId()) {
					mMusicChecked[j] = true;
					break;
				}
			}
		}
		
		mMusicAdapter = new InfoAdapter(getApplicationContext(), mMusicInfos);
		mList.setAdapter(mMusicAdapter);
		mList.setOnItemClickListener(this);
	}
	
	private ArrayList<MediaInfo> getMusicInfos() {
		ArrayList<MediaInfo> infos = new ArrayList<MediaInfo>();
		
		Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, 
				null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		while(cursor.moveToNext()) {
			MediaInfo info = new MediaInfo();
			
			long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
			long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
			String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)); 
			String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)); 
			String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)); 
			String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)); 
			long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)); 
			
			info.setId(id);
			info.setAlbumId(albumId);
			info.setTitle(title);
			info.setArtist(artist);
			info.setAlbum(album);
			info.setURL(url);
			info.setDuration(duration);
			
			infos.add(info);
		}
		
		return infos;
	}
	
	private class ViewHolder {
		TextView mTitle;
		TextView mArtist;
		CheckBox mCheckBox;
	}
	private class InfoAdapter extends BaseAdapter {

		private Context mContext;
		private ArrayList<MediaInfo> mInfos;
		
		public InfoAdapter(Context context, ArrayList<MediaInfo> infos) {
			mContext = context;
			mInfos = infos;
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
				convertView = LayoutInflater.from(mContext).inflate(R.layout.music_add_item, null);
				
				holder.mTitle = (TextView) convertView.findViewById(R.id.item_title);
				holder.mArtist = (TextView) convertView.findViewById(R.id.item_artist);
				holder.mCheckBox = (CheckBox) convertView.findViewById(R.id.check_add);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.mTitle.setText(info.getTitle());
			holder.mArtist.setText(info.getArtist());
			holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mMusicChecked[position] = isChecked;
				}
			});
			holder.mCheckBox.setChecked(mMusicChecked[position]);
			
			return convertView;
		}
		
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		boolean checked = mMusicChecked[position];
		CheckBox checkBox = (CheckBox) view.findViewById(R.id.check_add);
		checkBox.setChecked(!checked);
		mMusicChecked[position] = !checked;
	}
}
