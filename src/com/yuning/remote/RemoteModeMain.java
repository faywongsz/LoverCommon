package com.yuning.remote;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.yuning.activity.BaseModeActivity;
import com.yuning.lovercommon.R;
import com.yuning.ui.CustomDialog;
import com.yuning.util.sysinfo;

public class RemoteModeMain extends BaseModeActivity implements View.OnClickListener{
	private static final String TAG = RemoteModeMain.class.getSimpleName();

	private static final String SP_REMOTE_TIPS = "remote_tips";
	private static final int MAX_LIST_ITEMS = 5;
	
	private ListView mListView;
	private ListAdapter mListAdapter;
	private Button mBtnAdd;
	
	private boolean mbEditing;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.remote_mode);
		initTitleBar(R.string.remote_mode, 0, 0, null);
		
		mListView = (ListView) findViewById(R.id.remote_mode_link_list);
		mBtnAdd = (Button) findViewById(R.id.remote_mode_add_btn);
		
		mListAdapter = new ListAdapter(getApplicationContext());
		mListView.setAdapter(mListAdapter);
		mBtnAdd.setOnClickListener(this);
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		
		showTipsDialog();
		initRemoteInfos();
		updateRightButtonItem();
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.remote_mode_add_btn :
				if(mListAdapter.getCount() < MAX_LIST_ITEMS) {
					startActivity(new Intent(this, RemoteModeAddInfo.class));
				} else {
					Toast.makeText(this, R.string.remote_mode_link_max, Toast.LENGTH_SHORT).show();
				}
				break;
		}
	}
	
	private void showTipsDialog() {
		if(!getBoolData(SP_REMOTE_TIPS)) {
			CustomDialog dialog = new CustomDialog(this);
			
			dialog.setTitle(R.string.dialog_tips);
			dialog.setMessage(R.string.remote_mode_tips_title);
			((TextView)dialog.findViewById(R.id.dialog_message)).setGravity(Gravity.START);
			dialog.setYesButton(R.string.yes, null);
			
			saveBoolData(SP_REMOTE_TIPS, true);
		}
	}
	
	private void updateRightButtonItem() {
		if(mListAdapter.getCount() > 0) {
			if(mbEditing) {
				setRightButtonItem(R.string.remote_mode_finish, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mbEditing = false;
						updateRightButtonItem();
						mListAdapter.notifyDataSetChanged();
					}
				});
			} else {
				setRightButtonItem(R.string.remote_mode_edit, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mbEditing = true;
						updateRightButtonItem();
						mListAdapter.notifyDataSetChanged();
					}
				});
			}
		} else {
			setRightButtonItem(0, null);
			mbEditing = false;
		}
	}
	
	private void initRemoteInfos() {
		mListAdapter.clearInfos();
		
		int count = getIntData(REMOTE_COUNT);
		int addCount = 0;
		for(int i = 0; i < count && i < MAX_LIST_ITEMS; i++){
			String infoStr = getStrData(REMOTE_INFO_PREFIX + i);
			RemoteInfo info = RemoteInfo.genRemoteInfo(infoStr);
			if(info != null) {
				mListAdapter.addInfo(addCount++, info);
			}
		}
		mListAdapter.notifyDataSetChanged();
	}
	
	private void deleteRemoteInfo(int index) {
		cleanSPRemoteInfo();
		
		ArrayList<RemoteInfo> infos = mListAdapter.getInfos();
		infos.remove(index);
		
		for(int i = 0; i < infos.size(); i++) {
			RemoteInfo info = infos.get(i);
			saveStrData(REMOTE_INFO_PREFIX + i, info.toString());
		}
		saveIntData(REMOTE_COUNT, infos.size());
		
		updateRightButtonItem();
		mListAdapter.notifyDataSetChanged();
	}
	
	public static class RemoteInfo {
		private static final String SEPARATE_STR = ",,##";
		
		private int mId;
		private String mName;
		
		public RemoteInfo(int id, String name) {
			mId = id;
			mName = name;
		}
		
		public int getId() {
			return mId;
		}
		
		public String getName() {
			return mName;
		}
		
		@Override
		public String toString() {
			return mId + SEPARATE_STR
					+ mName;
		}
		
		public static RemoteInfo genRemoteInfo(String string) {
			RemoteInfo info = null;
			if(string != null) {
				String[] datas = string.split(SEPARATE_STR);
				
				if(datas.length == 2) {
					info = new RemoteInfo(Integer.parseInt(datas[0]), datas[1]);
				}
			}
			return info;
		}
	}
	
	private class ListAdapter extends BaseAdapter {
		
		private class ViewHolder {
			Button mButtonTitle;
			ImageView mImageDelete;
		}

		private Context mContext;
		private ArrayList <RemoteInfo> mInfos;
		
		public ListAdapter(Context context) {
			mContext = context;
			mInfos = new ArrayList<RemoteInfo>();
		}
		
		public void addInfo(int index, RemoteInfo info) {
			mInfos.add(index, info);
		}
		
		public ArrayList <RemoteInfo> getInfos() {
			return mInfos;
		}
		
		public void clearInfos() {
			mInfos.clear();
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
			final RemoteInfo info = mInfos.get(position);
			
			if(convertView == null){
				holder = new ViewHolder();
				convertView = LayoutInflater.from(mContext).inflate(R.layout.remote_mode_link_list_item, null);
				
				holder.mButtonTitle = (Button) convertView.findViewById(R.id.remote_mode_list_btn);
				holder.mImageDelete = (ImageView) convertView.findViewById(R.id.remote_mode_list_delete);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.mButtonTitle.setText(info.getName());
			holder.mImageDelete.setVisibility(mbEditing ? View.VISIBLE : View.GONE);
			
			holder.mButtonTitle.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!mbEditing) {
						if(sysinfo.isNetworkAvailable(getApplicationContext())) {
							Intent intent = new Intent(RemoteModeMain.this, com.bairuitech.helloanychat.MainActivity.class);
							intent.putExtra("id", info.getId());
							startActivity(intent);
						} else {
							CustomDialog dialog = new CustomDialog(RemoteModeMain.this);
							
							dialog.setTitle(R.string.dialog_tips);
							dialog.setMessage(R.string.remote_mode_null_connection_tips);
							dialog.setYesButton(R.string.yes, null);
						}

					}
				}
			});
			holder.mImageDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteRemoteInfo(position);
				}
			});
			
			return convertView;
		}
		
	}
}
