package com.yuning.remote;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.yuning.activity.BaseModeActivity;
import com.yuning.lovercommon.R;
import com.yuning.ui.CustomDialog;

public class RemoteModeAddFinish extends BaseModeActivity implements View.OnClickListener{
	private static final String TAG = RemoteModeAddFinish.class.getSimpleName();

	private Button mBtnSkip, mBtnOk;
	private EditText mEditName;
	private CustomDialog mTipsDialog;
	
	private InputMethodManager mIMM;
	
	private int mLauncherCode;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.remote_mode_add_finish);
		initTitleBar(R.string.remote_mode_add_info_title, 0, 0, null);
		
		mLauncherCode = getIntent().getIntExtra(RemoteModeAddInfo.ID_DATA, 0);
		if(Integer.toString(mLauncherCode).length() != RemoteModeAddInfo.ROOM_ID_LEN) {
			Toast.makeText(this, R.string.remote_mode_dialog_len_massage, Toast.LENGTH_SHORT).show();
			finish();
		}
		
		mIMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		
		mBtnSkip = (Button) findViewById(R.id.remote_mode_skip_btn);
		mBtnOk = (Button) findViewById(R.id.remote_mode_ok_btn);
		mEditName = (EditText) findViewById(R.id.remote_mode_name_edit);
		
		mBtnSkip.setOnClickListener(this);
		mBtnOk.setOnClickListener(this);
		
		findViewById(R.id.main_layout).setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mIMM.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
			}
		});
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(TAG);
		MobclickAgent.onResume(this);
		
		mEditName.setFocusable(true);
		mEditName.setFocusableInTouchMode(true);
		mEditName.requestFocus();
		
		mEditName.postDelayed(new Runnable() {
			@Override
			public void run() {
				mIMM.showSoftInput(mEditName, 0);
			}
		}, 500);
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
	}

	@Override
	public void onClick(View v) {
		int count = getIntData(REMOTE_COUNT);
		RemoteModeMain.RemoteInfo info = null;
		
		switch(v.getId()) {
			case R.id.remote_mode_skip_btn :
				info = new RemoteModeMain.RemoteInfo(mLauncherCode, getString(R.string.remote_mode_link_prefix) + " " + (count + 1));
				break;
			case R.id.remote_mode_ok_btn :
				String name = mEditName.getText().toString();
				if(name == null || name.equals("")) {
					if(mTipsDialog == null) {
						mTipsDialog = new CustomDialog(RemoteModeAddFinish.this);
					}
					if(!mTipsDialog.getDialog().isShowing()) {
						mTipsDialog.getDialog().show();
					}
					
					mTipsDialog.setTitle(R.string.remote_mode_dialog_tips);
					mTipsDialog.setMessage(R.string.remote_mode_null_name_tips);
					mTipsDialog.setYesButton(R.string.yes, null);
					return;
				}
				info = new RemoteModeMain.RemoteInfo(mLauncherCode, name);
				break;
		}
		
		if(info != null) {
			saveStrData(REMOTE_INFO_PREFIX + count, info.toString());
			saveIntData(REMOTE_COUNT, count + 1);
			Log.d(TAG, "info = " + info + ", count = " + (count + 1));
			
			Intent intent = new Intent(this, RemoteModeMain.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
	}
}
