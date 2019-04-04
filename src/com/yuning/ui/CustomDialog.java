package com.yuning.ui;

import com.yuning.lovercommon.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class CustomDialog {
	private Button mButtonYes, mButtonNo;
	private OnClickListener mYesListener, mNoListener;
	private Dialog mDialog;
	
	public interface OnClickListener {
		public void onClick(Dialog dialog);
	}
	
	public CustomDialog(Context context) {
		this(context, R.layout.custom_dialog);
	}
	
	public CustomDialog(Context context, int layoutId) {
		AlertDialog dialog = new AlertDialog.Builder(context).create();
		dialog.setView(LayoutInflater.from(context).inflate(layoutId, null));
		dialog.show();
		dialog.setContentView(layoutId);
		mDialog = dialog;
		
		WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
		lp.width = context.getResources().getDisplayMetrics().widthPixels;
		mDialog.getWindow().setAttributes(lp);
		
		mButtonYes = (Button) dialog.findViewById(R.id.dialog_yes);
		if(mButtonYes != null) mButtonYes.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mYesListener != null) {
					mYesListener.onClick(mDialog);
				} else {
					mDialog.dismiss();
				}
			}
		});
		mButtonNo = (Button) dialog.findViewById(R.id.dialog_no);
		if(mButtonNo != null) mButtonNo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mNoListener != null) {
					mNoListener.onClick(mDialog);
				} else {
					mDialog.dismiss();
				}
			}
		});
	}
	
	public void setTitle(int redId) {
		((TextView)findViewById(R.id.dialog_title)).setText(redId);
	}
	
	public void setMessage(int redId) {
		((TextView)findViewById(R.id.dialog_message)).setText(redId);
	}
	
	public void setYesButton(int redId, final OnClickListener listener) {
		mButtonYes.setText(redId);
		findViewById(R.id.dialog_yes_layout).setVisibility(View.VISIBLE);
		mYesListener = listener;
	}
	
	public void setNoButton(int redId, final OnClickListener listener) {
		mButtonNo.setText(redId);
		findViewById(R.id.dialog_no_layout).setVisibility(View.VISIBLE);
		mNoListener = listener;
	}
	
	public Dialog getDialog() {
		return mDialog;
	}
	
	public View findViewById(int id) {
		return mDialog.findViewById(id);
	}
}
