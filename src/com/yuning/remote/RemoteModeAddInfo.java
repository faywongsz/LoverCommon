package com.yuning.remote;

import java.util.Random;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXTextObject;
import com.umeng.analytics.MobclickAgent;
import com.yuning.activity.BaseModeActivity;
import com.yuning.lovercommon.R;
import com.yuning.ui.CustomDialog;

public class RemoteModeAddInfo extends BaseModeActivity implements View.OnClickListener {
	private static final String TAG = RemoteModeAddInfo.class.getSimpleName();
	
	public static final String ID_DATA = "id_data";
	public static final int ROOM_ID_LEN = 8;//max value is 9
	
	private static final int MIN_PHONE_NUMBER = 5;
	
	private TextView mTextLauncherCode;
	private EditText mEditInvitationCode;
	private ImageView mImageSendMessage;
	
	private CustomDialog mTipsDialog, mSendDialog;
	private ProgressDialog mSendingDialog;
	private EditText mEditPhoneNumber;
	private Button mBtnSend;
	
	private InputMethodManager mIMM;
	
	private int mLauncherCode;
	
    private String SENT_SMS_ACTION = "SENT_SMS_ACTION";  
    //private String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
	private BroadcastReceiver mSendMessageReceiver = new BroadcastReceiver() {
	    @Override  
	    public void onReceive(Context context, Intent intent) {  
	        //判断短信是否发送成功  
	        switch (getResultCode()) {  
	        	case Activity.RESULT_OK:  
	        		Toast.makeText(RemoteModeAddInfo.this, R.string.remote_mode_send_success, Toast.LENGTH_SHORT).show();  
	        		mSendingDialog.dismiss();
	        		break;  
	        	default:  
	        		Toast.makeText(RemoteModeAddInfo.this, R.string.remote_mode_send_failed, Toast.LENGTH_SHORT).show();  
	        		mSendingDialog.dismiss();
	        		break;  
	        }  
	    }
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.remote_mode_add_info);
		initTitleBar(R.string.remote_mode_add_info_title, 0, R.string.remote_mode_next, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RemoteModeAddInfo.this, RemoteModeAddFinish.class);
				String str = mEditInvitationCode.getText().toString();
				if(str == null || str.length() == 0) {
					intent.putExtra(ID_DATA, mLauncherCode);
					startActivity(intent);
				} else {
					if(str.length() != ROOM_ID_LEN) {
						if(mTipsDialog == null) {
							mTipsDialog = new CustomDialog(RemoteModeAddInfo.this);
						}
						if(!mTipsDialog.getDialog().isShowing()) {
							mTipsDialog.getDialog().show();
						}
						
						mTipsDialog.setTitle(R.string.remote_mode_dialog_tips);
						mTipsDialog.setMessage(R.string.remote_mode_dialog_len_massage);
						mTipsDialog.setYesButton(R.string.yes, null);
						
					} else {
						try {
							int code = Integer.parseInt(str);
							intent.putExtra(ID_DATA, code);
							startActivity(intent);
						} catch(NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		
		mIMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		
		mTextLauncherCode = (TextView) findViewById(R.id.remote_mode_launcher_text);
		mEditInvitationCode = (EditText) findViewById(R.id.remote_mode_invitation_edit);
		mImageSendMessage = (ImageView) findViewById(R.id.remote_mode_send_sms);
		ImageView imageSendWeixin = (ImageView) findViewById(R.id.remote_mode_send_weixin);
		
		mLauncherCode = getRandomRoomId();
		mTextLauncherCode.setText(String.valueOf(mLauncherCode));
		mImageSendMessage.setOnClickListener(this);
		imageSendWeixin.setOnClickListener(this);
		
		findViewById(R.id.main_layout).setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return mIMM.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
			}
		});
		
		registerReceiver(mSendMessageReceiver, new IntentFilter(SENT_SMS_ACTION));
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(TAG);
		MobclickAgent.onResume(this);
	}
	
	public void onPause(){
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(mSendMessageReceiver);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.remote_mode_send_sms :
				showSendDialog();
				break;
			case R.id.remote_mode_send_weixin :
				sendWeixin();
				break;
			case R.id.dialog_send_btn :
				String number = mEditPhoneNumber.getText().toString();
				if(number == null || number.length() < MIN_PHONE_NUMBER) {
					Toast.makeText(this, R.string.remote_mode_send_wrong_number, Toast.LENGTH_SHORT).show();
				} else {
					sentMessage(number, getString(R.string.remote_mode_message_content, mLauncherCode));
					mSendDialog.getDialog().dismiss();
					mSendingDialog = new ProgressDialog(this);
					mSendingDialog.setMessage(getString(R.string.remote_mode_sending));
					mSendingDialog.setCancelable(false);
					mSendingDialog.show();
				}
				break;
		}
	}
	
	private int getRandomRoomId() {
		int min = (int) Math.pow(10, ROOM_ID_LEN - 1);
		int max = (int) (Math.pow(10, ROOM_ID_LEN) - 1);
		Random random = new Random();
		
		return (min + random.nextInt(max - min));
	}
	
	private void showSendDialog() {
    	if(mSendDialog == null) {
    		mSendDialog = new CustomDialog(this, R.layout.remote_mode_send_dialog);
    	}
    	if(!mSendDialog.getDialog().isShowing()) {
    		mSendDialog.getDialog().show();
    	}
    	
    	mBtnSend = (Button) mSendDialog.findViewById(R.id.dialog_send_btn);
    	mBtnSend.setOnClickListener(this);
    	
    	mEditPhoneNumber = (EditText) mSendDialog.findViewById(R.id.remote_mode_send_edit);
    	mEditPhoneNumber.setFocusable(true);
    	mEditPhoneNumber.setFocusableInTouchMode(true);
    	mEditPhoneNumber.requestFocus();
    	mSendDialog.getDialog().getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    	mIMM.showSoftInput(mEditPhoneNumber, 0);
	}
	
	private void sentMessage(String number, String message) {
		SmsManager sms = SmsManager.getDefault(); 
		
	    // create the sentIntent parameter  
	    Intent sentIntent = new Intent(SENT_SMS_ACTION);  
	    PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, sentIntent, 0);  
	 
	    // create the deilverIntent parameter  
	    /*Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);  
	    PendingIntent deliverPI = PendingIntent.getBroadcast(this, 0,  
	        deliverIntent, 0); */
	    
	    sms.sendTextMessage(number, null, message, sentPI, null);  
	}
	
	private void sendWeixin() {
		// wx967daebe835fbeac是你在微信开发平台注册应用的AppID, 这里需要替换成你注册的AppID
		String appID = "wxbe37f9c576008aff";
		//String appSecret = "4ee37e162058e6661f78198101bb1159";
		
		IWXAPI api = WXAPIFactory.createWXAPI(this, appID, true);
		api.registerApp(appID);
		
		if(!api.isWXAppInstalled()) {
			Toast.makeText(getApplicationContext(), R.string.remote_mode_weixin_not_installed, Toast.LENGTH_SHORT).show();
			return;
		}
		
		String text = getString(R.string.remote_mode_message_content, mLauncherCode);
		// 初始化一个WXTextObject对象
		WXTextObject textObj = new WXTextObject();
		textObj.text = text;

		// 用WXTextObject对象初始化一个WXMediaMessage对象
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = textObj;
		// 发送文本类型的消息时，title字段不起作用
		// msg.title = "Will be ignored";
		msg.description = text;

		// 构造一个Req
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("text"); // transaction字段用于唯一标识一个请求
		req.message = msg;
		req.scene = SendMessageToWX.Req.WXSceneSession;
		
		// 调用api接口发送数据到微信
		api.sendReq(req);
	}
	
	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}
}
