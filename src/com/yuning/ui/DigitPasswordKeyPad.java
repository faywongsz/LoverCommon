package com.yuning.ui;

import com.yuning.lovercommon.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class DigitPasswordKeyPad extends LinearLayout {

	private Button mBtnKeypad1, mBtnKeypad2, mBtnKeypad3, mBtnKeypad4, mBtnKeypad5; 
	private Button mBtnKeypad6, mBtnKeypad7, mBtnKeypad8, mBtnKeypad9, mBtnKeypad0;
	private ImageButton mBtnKeypadDelete; 
	
	private OnCallback mOnCallback;
	private StringBuilder mInputString = new StringBuilder();
	private static final int MAX_INPUT_LEN = 4;
	
	public interface OnCallback {
		public void onKey(int length);
		public void onDelete(int length);
		public void onFinish(String input);
	}
	
	private View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch(v.getId()) {
				case R.id.digit_keypad_1 :
					mInputString.append(1);
					updateStatus(); 
					break;
				case R.id.digit_keypad_2 :
					mInputString.append(2);
					updateStatus(); 
					break;
				case R.id.digit_keypad_3 :
					mInputString.append(3);
					updateStatus(); 
					break;
				case R.id.digit_keypad_4 :
					mInputString.append(4);
					updateStatus(); 
					break;
				case R.id.digit_keypad_5 :
					mInputString.append(5);
					updateStatus();  
					break;
				case R.id.digit_keypad_6 :
					mInputString.append(6);
					updateStatus(); 
					break;
				case R.id.digit_keypad_7 :
					mInputString.append(7);
					updateStatus(); 
					break;
				case R.id.digit_keypad_8 :
					mInputString.append(8);
					updateStatus(); 
					break;
				case R.id.digit_keypad_9 :
					mInputString.append(9);
					updateStatus(); 
					break;
				case R.id.digit_keypad_0 :
					mInputString.append(0);
					updateStatus(); 
					break;
				case R.id.digit_keypad_delete :
					if(mInputString.length() > 0) {
						mInputString.deleteCharAt(mInputString.length() - 1);
						mOnCallback.onDelete(mInputString.length());
					}
					break;
			}
		}
	};
	
	public DigitPasswordKeyPad(Context context) {
		this(context, null);
	}

	public DigitPasswordKeyPad(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DigitPasswordKeyPad(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {
		View keypad = LayoutInflater.from(getContext()).inflate(R.layout.digit_password_keypad, this, true);
		
		mBtnKeypad1 = (Button) keypad.findViewById(R.id.digit_keypad_1);
		mBtnKeypad2 = (Button) keypad.findViewById(R.id.digit_keypad_2);
		mBtnKeypad3 = (Button) keypad.findViewById(R.id.digit_keypad_3);
		mBtnKeypad4 = (Button) keypad.findViewById(R.id.digit_keypad_4);
		mBtnKeypad5 = (Button) keypad.findViewById(R.id.digit_keypad_5);
		mBtnKeypad6 = (Button) keypad.findViewById(R.id.digit_keypad_6);
		mBtnKeypad7 = (Button) keypad.findViewById(R.id.digit_keypad_7);
		mBtnKeypad8 = (Button) keypad.findViewById(R.id.digit_keypad_8);
		mBtnKeypad9 = (Button) keypad.findViewById(R.id.digit_keypad_9);
		mBtnKeypad0 = (Button) keypad.findViewById(R.id.digit_keypad_0);
		mBtnKeypadDelete = (ImageButton) keypad.findViewById(R.id.digit_keypad_delete);
		
		mBtnKeypad1.setOnClickListener(mOnClickListener);
		mBtnKeypad2.setOnClickListener(mOnClickListener);
		mBtnKeypad3.setOnClickListener(mOnClickListener);
		mBtnKeypad4.setOnClickListener(mOnClickListener);
		mBtnKeypad5.setOnClickListener(mOnClickListener);
		mBtnKeypad6.setOnClickListener(mOnClickListener);
		mBtnKeypad7.setOnClickListener(mOnClickListener);
		mBtnKeypad8.setOnClickListener(mOnClickListener);
		mBtnKeypad9.setOnClickListener(mOnClickListener);
		mBtnKeypad0.setOnClickListener(mOnClickListener);
		mBtnKeypadDelete.setOnClickListener(mOnClickListener);
	}
	
	private void updateStatus() {
		if(mOnCallback != null) {
			mOnCallback.onKey(mInputString.length());
			if(mInputString.length() == MAX_INPUT_LEN) {
				mOnCallback.onFinish(mInputString.toString());
			} 
		}
	}
	
	public void setOnCallback(OnCallback listener) {
		mOnCallback = listener;
	}
	
	public void reset() {
		mInputString.delete(0, mInputString.length());
	}
	
	public void setEnabled(boolean enable) {
		mBtnKeypad1.setEnabled(enable);
		mBtnKeypad2.setEnabled(enable);
		mBtnKeypad3.setEnabled(enable);
		mBtnKeypad4.setEnabled(enable);
		mBtnKeypad5.setEnabled(enable);
		mBtnKeypad6.setEnabled(enable);
		mBtnKeypad7.setEnabled(enable);
		mBtnKeypad8.setEnabled(enable);
		mBtnKeypad9.setEnabled(enable);
		mBtnKeypad0.setEnabled(enable);
		mBtnKeypadDelete.setEnabled(enable);
	}
}
