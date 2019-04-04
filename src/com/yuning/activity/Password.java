package com.yuning.activity;

import com.umeng.analytics.MobclickAgent;
import com.yuning.lovercommon.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class Password extends BaseModeActivity {
	private static String TAG = Password.class.getSimpleName();
	
	private static final String SP_PASSWORD_TIPS_ANIMATION = "password_tips_animation";
	
	private TextView mTextPassStatus, mTextPassChange;
	
	public static final int MAX_RETRY_COUNT = 6;
	public static final String EXTRA_PASSWORD = "extra_password";
	public static enum PasswordType {
		OPEN,
		CLOSE,
		CHANGE,
		ENTER,
		LAUNCHER
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password);
		initTitleBar(R.string.password_settings, 0, 0, null);
		
		mTextPassStatus = (TextView) findViewById(R.id.password_status);
		mTextPassChange = (TextView) findViewById(R.id.password_change);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(TAG);
		MobclickAgent.onResume(this);   
		
		if(getBoolData(PASSWORD_STATUS)) {
			mTextPassStatus.setText(R.string.password_close);
			mTextPassChange.setTextColor(getResources().getColor(R.color.password_enalbe));
		} else {
			mTextPassStatus.setText(R.string.password_open);
			mTextPassChange.setTextColor(getResources().getColor(R.color.password_disable));
		}
		
		if(isConnect() && !getBoolData(SP_PASSWORD_TIPS_ANIMATION)) {
			showTipsLayoutAnimation(R.string.password_tips);
			saveBoolData(SP_PASSWORD_TIPS_ANIMATION, true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
	}
	
    public void	onPasswordStatus(View view){
    	Intent intent = getProcessIntent();
		if(getBoolData(PASSWORD_STATUS)) {
			intent.putExtra(EXTRA_PASSWORD, PasswordType.CLOSE);
		} else {
			intent.putExtra(EXTRA_PASSWORD, PasswordType.OPEN);
		}
		startActivity(intent);
	}
    
    public void	onPasswordChange(View view){
		if(getBoolData(PASSWORD_STATUS)) {
			Intent intent = getProcessIntent();
			intent.putExtra(EXTRA_PASSWORD, PasswordType.CHANGE);
			startActivity(intent);
		} 
	}
    
    private Intent getProcessIntent() {
    	Intent intent = new Intent(this, PasswordProcess.class);
    	return intent;
    }
}
