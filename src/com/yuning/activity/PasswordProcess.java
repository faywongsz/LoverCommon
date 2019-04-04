package com.yuning.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.yuning.activity.Password.PasswordType;
import com.yuning.lovercommon.R;
import com.yuning.ui.DigitPasswordKeyPad;

public class PasswordProcess extends BaseModeActivity {
	private static String TAG = PasswordProcess.class.getSimpleName();
	
	private static final int ONE_MINUTE = 60 * 1000;
	private PasswordType mPasswordType;
	private String mTempPassword;
	private int mVerfiyCount = 0;
	private DigitPasswordKeyPad mPasswordKeyPad;
	private TextView mTextTips, mTextWrongTips;
	private ImageView mImageInput1, mImageInput2, mImageInput3, mImageInput4;
	
	private Handler mHandler = new Handler();
	private Runnable mOneMinuteRunnable = new Runnable() {
		@Override
		public void run() {
			saveIntData(PASSWORD_WRONG_INPUTS, 0);
			saveLongData(PASSWORD_WRONG_TIME, 0);
			
			mTextTips.setText(getTipsRedId());
			updateWrongInputStatus(true);
			updatePasswordStatus(0);
			mPasswordKeyPad.setEnabled(true);
		}
	};
	
	private DigitPasswordKeyPad.OnCallback mCallback = new DigitPasswordKeyPad.OnCallback() {
		@Override
		public void onKey(int length) {
			updatePasswordStatus(length);
		}
		@Override
		public void onFinish(String input) {
			mPasswordKeyPad.reset();
			mVerfiyCount++;
			updateFinishStatus(input);
		}
		@Override
		public void onDelete(int length) {
			updatePasswordStatus(length);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password_process);
		
		mPasswordType = (PasswordType) getIntent().getSerializableExtra(Password.EXTRA_PASSWORD);
		initTitleBar(getTitleRedId(), 0, 0, null);
		
		mPasswordKeyPad = (DigitPasswordKeyPad) findViewById(R.id.digit_password_keypad);
		mTextTips = (TextView) findViewById(R.id.password_tips);
		mTextWrongTips = (TextView) findViewById(R.id.password_wrong_tips);
		mImageInput1 = (ImageView) findViewById(R.id.password_input_1);
		mImageInput2 = (ImageView) findViewById(R.id.password_input_2);
		mImageInput3 = (ImageView) findViewById(R.id.password_input_3);
		mImageInput4 = (ImageView) findViewById(R.id.password_input_4);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(TAG);
		MobclickAgent.onResume(this);   
		
		mPasswordKeyPad.setOnCallback(mCallback);
		mTextTips.setText(getTipsRedId());
		
		long lastWrongTime = getLongData(PASSWORD_WRONG_TIME);
		if((lastWrongTime != 0) && (System.currentTimeMillis() - lastWrongTime >= ONE_MINUTE)) {
			saveIntData(PASSWORD_WRONG_INPUTS, 0);
			saveLongData(PASSWORD_WRONG_TIME, 0);
		}
		updateWrongInputStatus(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd(TAG);
		MobclickAgent.onPause(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacks(mOneMinuteRunnable);
	}

	private int getTitleRedId() {
		int titleId = R.string.password_set;
		switch(mPasswordType) {
			case OPEN :
				titleId = R.string.password_set;
				break;
			case CLOSE :
				titleId = R.string.password_close;
				break;
			case CHANGE :
				titleId = R.string.password_change;
				break;
			case ENTER :
			case LAUNCHER :
				titleId = R.string.password_input;
				break;
			default :
				break;
		}
		
		return titleId;
	}
	
	private int getTipsRedId() {
		int titleId = R.string.password_input_please;
		switch(mPasswordType) {
			case OPEN :
				titleId = R.string.password_input_please;
				break;
			case CLOSE :
				titleId = R.string.password_input_please;
				break;
			case CHANGE :
				titleId = R.string.password_input_old_please;
				break;
			case ENTER :
			case LAUNCHER :
				titleId = R.string.password_input_please;
				break;
			default:
				break;
		}
		
		return titleId;
	}
	
	private void updateFinishStatus(String input) {
		String password = getStrData(PASSWORD);
		int wrongInputs = getIntData(PASSWORD_WRONG_INPUTS);
		
		switch(mPasswordType) {
			case OPEN :
				if(mVerfiyCount == 1) {
					mTextWrongTips.setVisibility(View.INVISIBLE);
					mTextTips.setText(R.string.password_input_again_please);
					mTempPassword = input;
					updatePasswordStatus(0);
				} else if(mVerfiyCount == 2) {
					if(mTempPassword.equals(input)) {
						saveStrData(PASSWORD, mTempPassword);
						saveBoolData(PASSWORD_STATUS, true);
						finish();
					} else {
						mVerfiyCount = 0;
						mTextTips.setText(R.string.password_input_please);
						mTextWrongTips.setVisibility(View.VISIBLE);
						mTextWrongTips.setText(R.string.password_input_again_wrong_please);
						updatePasswordStatus(0);
					}
				}
				break;
			case CLOSE :
				if(password.equals(input)) {
					saveStrData(PASSWORD, "");
					saveBoolData(PASSWORD_STATUS, false);
					finish();
				} else {
					saveIntData(PASSWORD_WRONG_INPUTS, wrongInputs + 1);
					updateWrongInputStatus(false);
				}
				break;
			case CHANGE :
				if(mVerfiyCount == 1) {
					if(password.equals(input)) {
						mTextWrongTips.setVisibility(View.INVISIBLE);
						saveIntData(PASSWORD_WRONG_INPUTS, 0);
						mTextTips.setText(R.string.password_input_new_please);
						updatePasswordStatus(0);
					} else {
						mVerfiyCount = 0;
						saveIntData(PASSWORD_WRONG_INPUTS, wrongInputs + 1);
						updateWrongInputStatus(false);
					}
				} else if(mVerfiyCount == 2) {
					mTextWrongTips.setVisibility(View.INVISIBLE);
					mTextTips.setText(R.string.password_input_new_again_please);
					mTempPassword = input;
					updatePasswordStatus(0);
				} else if(mVerfiyCount == 3) {
					if(mTempPassword.equals(input)) {
						saveStrData(PASSWORD, mTempPassword);
						saveBoolData(PASSWORD_STATUS, true);
						finish();
					} else {
						mVerfiyCount = 1;
						mTextTips.setText(R.string.password_input_new_please);
						mTextWrongTips.setVisibility(View.VISIBLE);
						mTextWrongTips.setText(R.string.password_input_again_wrong_please);
						updatePasswordStatus(0);
					}
				}
				break;
			case ENTER :
				if(password.equals(input)) {
					saveIntData(PASSWORD_WRONG_INPUTS, 0);
					startActivity(new Intent(this, Password.class));
					finish();
				} else {
					saveIntData(PASSWORD_WRONG_INPUTS, wrongInputs + 1);
					updateWrongInputStatus(false);
				}
				break;
			case LAUNCHER :
				if(password.equals(input)) {
					saveIntData(PASSWORD_WRONG_INPUTS, 0);
					startActivity(new Intent(this, MainActivity.class));
					finish();
				} else {
					saveIntData(PASSWORD_WRONG_INPUTS, wrongInputs + 1);
					updateWrongInputStatus(false);
				}
				break;
			default :
				break;
		}
	}
	
	private void updateWrongInputStatus(boolean isBegin) {
		int wrongInputs = getIntData(PASSWORD_WRONG_INPUTS);
		
		if(wrongInputs >= Password.MAX_RETRY_COUNT) {
			mTextTips.setText(R.string.password_input_one_minute_retry);
			mTextWrongTips.setVisibility(View.VISIBLE);
			mTextWrongTips.setText(getString(R.string.password_input_error, wrongInputs));
			mPasswordKeyPad.setEnabled(false);
			if(!isBegin) {
				saveLongData(PASSWORD_WRONG_TIME, System.currentTimeMillis());
				mHandler.postDelayed(mOneMinuteRunnable, ONE_MINUTE);
			}
		} else if(wrongInputs > 0) {
			mTextWrongTips.setVisibility(View.VISIBLE);
			mTextWrongTips.setText(getString(R.string.password_input_error, wrongInputs));
		} else {
			mTextWrongTips.setVisibility(View.INVISIBLE);
		}
	}
	
	private void updatePasswordStatus(int len) {
		if(len  == 4) {
			mImageInput1.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput2.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput3.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput4.setImageResource(R.drawable.digit_keypad_password_input);
		} else if(len  == 3) {
			mImageInput1.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput2.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput3.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput4.setImageResource(R.drawable.digit_keypad_password);
		} else if(len  == 2) {
			mImageInput1.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput2.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput3.setImageResource(R.drawable.digit_keypad_password);
			mImageInput4.setImageResource(R.drawable.digit_keypad_password);
		} else if(len  == 1) {
			mImageInput1.setImageResource(R.drawable.digit_keypad_password_input);
			mImageInput2.setImageResource(R.drawable.digit_keypad_password);
			mImageInput3.setImageResource(R.drawable.digit_keypad_password);
			mImageInput4.setImageResource(R.drawable.digit_keypad_password);
		} else {
			mImageInput1.setImageResource(R.drawable.digit_keypad_password);
			mImageInput2.setImageResource(R.drawable.digit_keypad_password);
			mImageInput3.setImageResource(R.drawable.digit_keypad_password);
			mImageInput4.setImageResource(R.drawable.digit_keypad_password);
		}
	}

}
