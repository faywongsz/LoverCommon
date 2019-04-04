package com.bairuitech.helloanychat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.bairuitech.config.ConfigEntity;
import com.bairuitech.config.ConfigService;
import com.yuning.activity.BaseModeActivity;
import com.yuning.lovercommon.R;
import com.yuning.remote.RemoteModeAddInfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends BaseModeActivity implements AnyChatBaseEvent {
	// 视频配置界面标识
	public static final int ACTIVITY_ID_VIDEOCONFIG = 1;

	private ListView mRoleList;
	private EditText mEditIP;
	private EditText mEditPort;
	private EditText mEditName;
	private EditText mEditRoomID;
	private TextView mBottomConnMsg;
	private TextView mBottomBuildMsg;
	private Button mBtnStart;
	private Button mBtnLogout;
	private Button mBtnWaiting;
	private LinearLayout mWaitingLayout;
	private LinearLayout mProgressLayout;
	
	private TextView mTextMessage;
	private ImageView mImageOuter;
	private ImageView mImageOther;
	private int mConnectMessageCount;
	private StringBuilder mConnectMessageStr = new StringBuilder();

	private String mStrIP = "demo.anychat.cn";
	private String mStrName = "name";
	private int mSPort = 8906;
	private int mSRoomID = 1;
	private int mEnterRommID;

	private final int SHOWLOGINSTATEFLAG = 1; // 显示的按钮是登陆状态的标识
	private final int SHOWWAITINGSTATEFLAG = 2; // 显示的按钮是等待状态的标识
	private final int SHOWLOGOUTSTATEFLAG = 3; // 显示的按钮是登出状态的标识
	private final int LOCALVIDEOAUTOROTATION = 1; // 本地视频自动旋转控制

	private List<RoleInfo> mRoleInfoList = new ArrayList<RoleInfo>();
	private RoleListAdapter mAdapter;
	private int UserselfID;

	public AnyChatCoreSDK anyChatSDK;
	
	private static final int MESSAGE_CONNECT_MESSAGE = 1;
	private static final int MESSAGE_CONNECT_MESSAGE_INTEVAL = 600;
	private static final int MESSAGE_CONNECT_TIMEOUT = 2;
	private static final int MESSAGE_CONNECT_TIMEOUT_INTEVAL = 40000;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			
			switch(what) {
				case MESSAGE_CONNECT_MESSAGE :
					updateConnectMessage();
					break;
				case MESSAGE_CONNECT_TIMEOUT :
					Toast.makeText(MainActivity.this, R.string.remote_mode_connect_timeout, Toast.LENGTH_SHORT).show();
					finish();
					break;
			}
		}
		
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.anychat_activity_main);
		initTitleBar(R.string.remote_mode, 0, 0, null);
		
		mEnterRommID = getIntent().getIntExtra("id", 0);
		Log.d("RemoteMode", "mEnterRommID = " + mEnterRommID);
		if(Integer.toString(mEnterRommID).length() != RemoteModeAddInfo.ROOM_ID_LEN) {
			Toast.makeText(this, R.string.remote_mode_wrong_room_id, Toast.LENGTH_SHORT).show();
			finish();
		}
		
		InitSDK();
		InitLayout();
		// 读取登陆配置表
		readLoginDate();
		// 初始化登陆配置数据
		initLoginConfig();
		initWaitingTips();
		ApplyVideoConfig();
		// 注册广播
		registerBoradcastReceiver();
	}

	private void InitSDK() {
		if (anyChatSDK == null) {
			anyChatSDK = AnyChatCoreSDK.getInstance(this);
			anyChatSDK.SetBaseEvent(this);
			anyChatSDK.InitSDK(android.os.Build.VERSION.SDK_INT, 0);
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_AUTOROTATION,
					LOCALVIDEOAUTOROTATION);
		}
	}

	private void InitLayout() {
		mRoleList = (ListView) this.findViewById(R.id.roleListView);
		mEditIP = (EditText) this.findViewById(R.id.mainUIEditIP);
		mEditPort = (EditText) this.findViewById(R.id.mainUIEditPort);
		mEditName = (EditText) this.findViewById(R.id.main_et_name);
		mEditRoomID = (EditText) this.findViewById(R.id.mainUIEditRoomID);
		mBottomConnMsg = (TextView) this.findViewById(R.id.mainUIbottomConnMsg);
		mBottomBuildMsg = (TextView) this
				.findViewById(R.id.mainUIbottomBuildMsg);
		mBtnStart = (Button) this.findViewById(R.id.mainUIStartBtn);
		mBtnLogout = (Button) this.findViewById(R.id.mainUILogoutBtn);
		mBtnWaiting = (Button) this.findViewById(R.id.mainUIWaitingBtn);
		mWaitingLayout = (LinearLayout) this.findViewById(R.id.waitingLayout);

		mRoleList.setDivider(null);
		mBottomConnMsg.setText("No content to the server");
		// 初始化bottom_tips信息
		mBottomBuildMsg.setText(" V" + anyChatSDK.GetSDKMainVersion() + "."
				+ anyChatSDK.GetSDKSubVersion() + "  Build time: "
				+ anyChatSDK.GetSDKBuildTime());
		mBottomBuildMsg.setGravity(Gravity.CENTER_HORIZONTAL);
		mBtnStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkInputData()) {
					setBtnVisible(SHOWWAITINGSTATEFLAG);
					mSRoomID = Integer.parseInt(mEditRoomID.getText()
							.toString().trim());
					mStrName = mEditName.getText().toString().trim();
					mStrIP = mEditIP.getText().toString().trim();
					mSPort = Integer.parseInt(mEditPort.getText().toString()
							.trim());

					anyChatSDK.Connect(mStrIP, mSPort);
					anyChatSDK.Login(mStrName, "");
				}
			}
		});

		mBtnLogout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setBtnVisible(SHOWLOGINSTATEFLAG);
				anyChatSDK.LeaveRoom(-1);
				anyChatSDK.Logout();
				mRoleList.setAdapter(null);
				mBottomConnMsg.setText("No connnect to the server");
			}
		});
		
		mTextMessage = (TextView) findViewById(R.id.remote_mode_connect_message);
		mImageOuter = (ImageView) findViewById(R.id.remote_mode_default_icon_outer);
		mImageOther = (ImageView) findViewById(R.id.remote_mode_other_person);
		
		mImageOther.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mRoleInfoList.size() == 2) {
					onSelectItem(1);
				}
			}
		});
		
		RotateAnimation animation = new RotateAnimation(0.0f, 360.f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		animation.setDuration(1000);
		animation.setInterpolator(new LinearInterpolator());
		animation.setRepeatCount(Animation.INFINITE);
		mImageOuter.startAnimation(animation);
		
		updateConnectMessage();
	}

	private void initLoginConfig() {
		mEditIP.setText(mStrIP);
		mEditName.setText(mStrName);
		mEditPort.setText(String.valueOf(mSPort));
		mEditRoomID.setText(String.valueOf(mSRoomID));
	}

	// 读取登陆数据
	private void readLoginDate() {
		SharedPreferences preferences = getSharedPreferences("LoginInfo", 0);
		mStrIP = preferences.getString("UserIP", "demo.anychat.cn");
		mStrName = preferences.getString("UserName", "name");
		mSPort = preferences.getInt("UserPort", 8906);
		mSRoomID = mEnterRommID;//preferences.getInt("UserRoomID", -1);
		/*if(mSRoomID == -1) {
			mSRoomID = mEnterRommID;
		}*/
	}

	// 保存登陆相关数据
	private void saveLoginData() {
		SharedPreferences preferences = getSharedPreferences("LoginInfo", 0);
		Editor preferencesEditor = preferences.edit();
		preferencesEditor.putString("UserIP", mStrIP);
		preferencesEditor.putString("UserName", mStrName);
		preferencesEditor.putInt("UserPort", mSPort);
		//preferencesEditor.putInt("UserRoomID", mSRoomID);
		preferencesEditor.commit();
	}

	private boolean checkInputData() {
		String ip = mEditIP.getText().toString().trim();
		String port = mEditPort.getText().toString().trim();
		String name = mEditName.getText().toString().trim();
		String roomID = mEditRoomID.getText().toString().trim();

		if (ValueUtils.isStrEmpty(ip)) {
			mBottomConnMsg.setText("请输入IP");
			return false;
		} else if (ValueUtils.isStrEmpty(port)) {
			mBottomConnMsg.setText("请输入端口号");
			return false;
		} else if (ValueUtils.isStrEmpty(name)) {
			mBottomConnMsg.setText("请输入姓名");
			return false;
		} else if (ValueUtils.isStrEmpty(roomID)) {
			mBottomConnMsg.setText("请输入房间号");
			return false;
		} else {
			return true;
		}
	}

	// 控制登陆，等待和登出按钮状态
	private void setBtnVisible(int index) {
		if (index == SHOWLOGINSTATEFLAG) {
			mBtnStart.setVisibility(View.VISIBLE);
			mBtnLogout.setVisibility(View.GONE);
			mBtnWaiting.setVisibility(View.GONE);

			mProgressLayout.setVisibility(View.GONE);
		} else if (index == SHOWWAITINGSTATEFLAG) {
			mBtnStart.setVisibility(View.GONE);
			mBtnLogout.setVisibility(View.GONE);
			mBtnWaiting.setVisibility(View.VISIBLE);

			mProgressLayout.setVisibility(View.VISIBLE);
		} else if (index == SHOWLOGOUTSTATEFLAG) {
			mBtnStart.setVisibility(View.GONE);
			mBtnLogout.setVisibility(View.VISIBLE);
			mBtnWaiting.setVisibility(View.GONE);

			mProgressLayout.setVisibility(View.GONE);
		}
	}

	// init登陆等待状态UI
	private void initWaitingTips() {
		if (mProgressLayout == null) {
			mProgressLayout = new LinearLayout(this);
			mProgressLayout.setOrientation(LinearLayout.HORIZONTAL);
			mProgressLayout.setGravity(Gravity.CENTER_VERTICAL);
			mProgressLayout.setPadding(1, 1, 1, 1);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(5, 5, 5, 5);
			ProgressBar progressBar = new ProgressBar(this, null,
					android.R.attr.progressBarStyleLarge);
			mProgressLayout.addView(progressBar, params);
			mProgressLayout.setVisibility(View.GONE);
			mWaitingLayout.addView(mProgressLayout, new LayoutParams(params));
		}
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm.isActive()) {
			/*imm.hideSoftInputFromWindow(getCurrentFocus()
					.getApplicationWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);*/
		}
	}

	protected void onDestroy() {
		anyChatSDK.LeaveRoom(-1);
		anyChatSDK.Logout();
		anyChatSDK.Release();
		unregisterReceiver(mBroadcastReceiver);
		stopVibration();
		mHandler.removeMessages(MESSAGE_CONNECT_MESSAGE);
		super.onDestroy();
	}

	protected void onResume() {
		super.onResume();
		mBtnStart.postDelayed(new Runnable() {
			@Override
			public void run() {
				mHandler.sendEmptyMessageDelayed(MESSAGE_CONNECT_TIMEOUT, MESSAGE_CONNECT_TIMEOUT_INTEVAL);
				login();
			}
		}, 500);
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		anyChatSDK.SetBaseEvent(this);

		// 一种简便的方法，当断网的时候，返回到登录界面，不去刷新用户列表，下面广播已经清空了列表
		if (mBtnStart.getVisibility() != View.VISIBLE)
			updateUserList();
	}

	@Override
	public void OnAnyChatConnectMessage(boolean bSuccess) {
		if (!bSuccess) {
			setBtnVisible(SHOWLOGINSTATEFLAG);
			mBottomConnMsg.setText("连接服务器失败，自动重连，请稍后...");
			login();
		}
	}

	@Override
	public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
		if (dwErrorCode == 0) {
			saveLoginData();
			setBtnVisible(SHOWLOGOUTSTATEFLAG);
			hideKeyboard();

			mBottomConnMsg.setText("Connect to the server success.");
			int sHourseID = mSRoomID;/*Integer.valueOf(mEditRoomID.getEditableText()
					.toString());*/
			anyChatSDK.EnterRoom(sHourseID, "");

			UserselfID = dwUserId;
			// finish();
		} else {
			setBtnVisible(SHOWLOGINSTATEFLAG);
			mBottomConnMsg.setText("登录失败，errorCode：" + dwErrorCode);
			login();
		}
	}

	@Override
	public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {
		System.out.println("OnAnyChatEnterRoomMessage" + dwRoomId + "err:"
				+ dwErrorCode);
	}

	@Override
	public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId) {
		mBottomConnMsg.setText("进入房间成功！");
		updateUserList();
	}

	private void updateUserList() {
		mRoleInfoList.clear();
		int[] userID = anyChatSDK.GetOnlineUser();
		RoleInfo userselfInfo = new RoleInfo();
		userselfInfo.setName(anyChatSDK.GetUserName(UserselfID)
				+ "(自己) 【点击可设置】");
		userselfInfo.setUserID(String.valueOf(UserselfID));
		userselfInfo.setRoleIconID(getRoleRandomIconID());
		mRoleInfoList.add(userselfInfo);

		for (int index = 0; index < userID.length; ++index) {
			RoleInfo info = new RoleInfo();
			info.setName(anyChatSDK.GetUserName(userID[index]));
			info.setUserID(String.valueOf(userID[index]));
			info.setRoleIconID(getRoleRandomIconID());
			mRoleInfoList.add(info);
		}

		mAdapter = new RoleListAdapter(MainActivity.this, mRoleInfoList);
		mRoleList.setAdapter(mAdapter);
		mRoleList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if (arg2 == 0) {
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, VideoConfig.class);
					startActivityForResult(intent, ACTIVITY_ID_VIDEOCONFIG);
					return;
				}

				onSelectItem(arg2);
			}
		});
		
		if(mRoleInfoList.size() == 1) {
			updateConnectStatus(false);
		} else if (mRoleInfoList.size() == 2){
			updateConnectStatus(true);
		} else {
			Toast.makeText(MainActivity.this, R.string.remote_mode_disconnect_message, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	private void onSelectItem(int postion) {
		String strUserID = mRoleInfoList.get(postion).getUserID();
		Intent intent = new Intent();
		intent.putExtra("UserID", strUserID);
		intent.setClass(this, VideoActivity.class);
		startActivity(intent);
	}

	private int getRoleRandomIconID() {
		int number = new Random().nextInt(5) + 1;
		if (number == 1) {
			return R.drawable.anychat_role_1;
		} else if (number == 2) {
			return R.drawable.anychat_role_2;
		} else if (number == 3) {
			return R.drawable.anychat_role_3;
		} else if (number == 4) {
			return R.drawable.anychat_role_4;
		} else if (number == 5) {
			return R.drawable.anychat_role_5;
		}

		return R.drawable.anychat_role_1;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (resultCode == RESULT_OK && requestCode == ACTIVITY_ID_VIDEOCONFIG) {
			ApplyVideoConfig();
		}
	}

	// 根据配置文件配置视频参数
	private void ApplyVideoConfig() {
		ConfigEntity configEntity = ConfigService.LoadConfig(this);
		if (configEntity.mConfigMode == 1) // 自定义视频参数配置
		{
			// 设置本地视频编码的码率（如果码率为0，则表示使用质量优先模式）
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_BITRATECTRL,
					configEntity.mVideoBitrate);
//			if (configEntity.mVideoBitrate == 0) {
				// 设置本地视频编码的质量
				AnyChatCoreSDK.SetSDKOptionInt(
						AnyChatDefine.BRAC_SO_LOCALVIDEO_QUALITYCTRL,
						configEntity.mVideoQuality);
//			}
			// 设置本地视频编码的帧率
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_FPSCTRL,
					configEntity.mVideoFps);
			// 设置本地视频编码的关键帧间隔
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_GOPCTRL,
					configEntity.mVideoFps * 4);
			// 设置本地视频采集分辨率
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL,
					configEntity.mResolutionWidth);
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL,
					configEntity.mResolutionHeight);
			// 设置视频编码预设参数（值越大，编码质量越高，占用CPU资源也会越高）
			AnyChatCoreSDK.SetSDKOptionInt(
					AnyChatDefine.BRAC_SO_LOCALVIDEO_PRESETCTRL,
					configEntity.mVideoPreset);
		}
		// 让视频参数生效
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_APPLYPARAM,
				configEntity.mConfigMode);
		// P2P设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_NETWORK_P2PPOLITIC,
				configEntity.mEnableP2P);
		// 本地视频Overlay模式设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_OVERLAY,
				configEntity.mVideoOverlay);
		// 回音消除设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_AUDIO_ECHOCTRL,
				configEntity.mEnableAEC);
		// 平台硬件编码设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_CORESDK_USEHWCODEC,
				configEntity.mUseHWCodec);
		// 视频旋转模式设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_ROTATECTRL,
				configEntity.mVideoRotateMode);
		// 本地视频采集偏色修正设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_FIXCOLORDEVIA,
				configEntity.mFixColorDeviation);
		// 视频GPU渲染设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_VIDEOSHOW_GPUDIRECTRENDER,
				configEntity.mVideoShowGPURender);
		// 本地视频自动旋转设置
		AnyChatCoreSDK.SetSDKOptionInt(
				AnyChatDefine.BRAC_SO_LOCALVIDEO_AUTOROTATION,
				configEntity.mVideoAutoRotation);
	}

	@Override
	public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {
		if (bEnter) {
			RoleInfo info = new RoleInfo();
			info.setUserID(String.valueOf(dwUserId));
			info.setName(anyChatSDK.GetUserName(dwUserId));
			info.setRoleIconID(getRoleRandomIconID());
			mRoleInfoList.add(info);
			mAdapter.notifyDataSetChanged();
			
		} else {

			for (int i = 0; i < mRoleInfoList.size(); i++) {
				if (mRoleInfoList.get(i).getUserID().equals("" + dwUserId)) {
					mRoleInfoList.remove(i);
					mAdapter.notifyDataSetChanged();
				}
			}
		}
		
		if(mRoleInfoList.size() == 1) {
			updateConnectStatus(false);
		} else if (mRoleInfoList.size() == 2){
			updateConnectStatus(true);
		} else {
			Toast.makeText(MainActivity.this, R.string.remote_mode_disconnect_message, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	@Override
	public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
		setBtnVisible(SHOWLOGINSTATEFLAG);
		mRoleList.setAdapter(null);
		anyChatSDK.LeaveRoom(-1);
		anyChatSDK.Logout();
		mBottomConnMsg.setText("连接关闭，errorCode：" + dwErrorCode);
		login();
	}

	// 广播
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("VideoActivity")) {
				Toast.makeText(MainActivity.this, R.string.remote_mode_disconnect_message, Toast.LENGTH_SHORT)
						.show();
				setBtnVisible(SHOWLOGINSTATEFLAG);
				mRoleList.setAdapter(null);
				mBottomConnMsg.setText("No content to the server");
				anyChatSDK.LeaveRoom(-1);
				anyChatSDK.Logout();
				finish();
			}
		}
	};

	public void registerBoradcastReceiver() {
		IntentFilter myIntentFilter = new IntentFilter();
		myIntentFilter.addAction("VideoActivity");
		// 注册广播
		registerReceiver(mBroadcastReceiver, myIntentFilter);
	}
	
	private void updateConnectStatus(boolean isEnter) {
		if(isEnter) {
			mHandler.removeMessages(MESSAGE_CONNECT_TIMEOUT);
			mHandler.removeMessages(MESSAGE_CONNECT_MESSAGE);
			mTextMessage.setText(R.string.remote_mode_connect_message_enter);
			mImageOther.setVisibility(View.VISIBLE);
			mConnectMessageCount = 0;
		} else {
			mHandler.removeMessages(MESSAGE_CONNECT_TIMEOUT);
			mHandler.sendEmptyMessageDelayed(MESSAGE_CONNECT_TIMEOUT, MESSAGE_CONNECT_TIMEOUT_INTEVAL);
			updateConnectMessage();
			mImageOther.setVisibility(View.INVISIBLE);
		}
	}
	
	private void login() {
		mBtnStart.callOnClick();
	}
	
	private void updateConnectMessage() {
		mHandler.removeMessages(MESSAGE_CONNECT_MESSAGE);
		mHandler.sendEmptyMessageDelayed(MESSAGE_CONNECT_MESSAGE, MESSAGE_CONNECT_MESSAGE_INTEVAL);
		
		mConnectMessageStr.delete(0, mConnectMessageStr.length());
		mConnectMessageStr.append(getString(R.string.remote_mode_connect_message));
		for(int i = 0; i < mConnectMessageCount; i++) {
			mConnectMessageStr.append('.');
		}
		mTextMessage.setText(mConnectMessageStr);
		mConnectMessageCount++;
		mConnectMessageCount = mConnectMessageCount % 4;
	}
}
