package com.yuning.activity;

import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateStatus;
import com.umeng.update.UpdateResponse;
import com.yuning.lovercommon.R;
import com.yuning.ui.CustomDialog;
import com.yuning.util.sysinfo;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SetMode extends BaseModeActivity {
	private static String TAG = "SetMode";
	private ImageView mImageNewVersionTips;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set_mode);
		initTitleBar(R.string.set_mode, 0, 0, null);
		findViews();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);    
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);
	}

	private void findViews(){
    	TextView versionText = (TextView) findViewById(R.id.set_mode_apk_version);
    	
        PackageManager pm = this.getPackageManager();
        PackageInfo pi;
    	try {
    		pi = pm.getPackageInfo(this.getPackageName(), 0);
    	    String version = pi.versionName;
    	    versionText.setText(getString(R.string.set_mode_apk_version, version));
    	} catch (NameNotFoundException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	
    	mImageNewVersionTips = (ImageView) findViewById(R.id.settings_update_tips);
    	mImageNewVersionTips.setVisibility(hasNewVersion(this) ? View.VISIBLE : View.INVISIBLE);
	}
	
    public void	onPassword(View view){
    	if(!isConnect()) {
    		showOpenDeviceDialog();
    		return;
    	}
    	
		if(getBoolData(PASSWORD_STATUS)) {
			Intent intent = new Intent(this, PasswordProcess.class);
			intent.putExtra(Password.EXTRA_PASSWORD, Password.PasswordType.ENTER);
			startActivity(intent);	
		} else {
			Intent intent = new Intent(this, Password.class);
			startActivity(intent);	
		}
	}
    
    public void	onSelectFW(View view){
    	if(!isConnect()) {
    		showOpenDeviceDialog();
    		return;
    	}
    	
		Intent intent = new Intent(this, FwUpdateActivity.class);
		startActivity(intent);	
		//finish();
	}
    
    public void	onAPPcheck(View view){
    	if(sysinfo.isNetworkAvailable(getApplicationContext())) {
        	android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        	LayoutInflater inflater = getLayoutInflater();
        	View subView = inflater.inflate(R.layout.set_mode_app_check, null);
        	builder.setView(subView);
        	builder.setCancelable(false);
        	
        	final android.app.AlertDialog dialog = builder.create();
        	dialog.show();
        	
        	UmengUpdateAgent.setUpdateAutoPopup(false);
        	UmengUpdateAgent.setUpdateListener(new UmengUpdateListener() {
        	    @Override
        	    public void onUpdateReturned(int updateStatus,UpdateResponse updateInfo) {
        	    	Log.d(TAG, "updateStatus = " + updateStatus);
        	        switch (updateStatus) {
    	    	        case UpdateStatus.Yes: // has update
    	    	            UmengUpdateAgent.showUpdateDialog(getApplicationContext(), updateInfo);
    	    	            saveStrData(UMENG_APK_VERSION, updateInfo.version);
    	    	            mImageNewVersionTips.setVisibility(hasNewVersion(SetMode.this) ? View.VISIBLE : View.INVISIBLE);
    	    	            dialog.dismiss();
    	    	            break;
    	    	        case UpdateStatus.No: // has no update
    	    	        	saveStrData(UMENG_APK_VERSION, "0.0");
    	    	        	mImageNewVersionTips.setVisibility(hasNewVersion(SetMode.this) ? View.VISIBLE : View.INVISIBLE);
    	    	        case UpdateStatus.NoneWifi: // none wifi
    	    	        case UpdateStatus.Timeout: // time out
    	    	            Toast.makeText(SetMode.this, R.string.set_mode_latest_version, Toast.LENGTH_SHORT).show();
    	    	            dialog.dismiss();
    	    	            break;
        	        }
        	    }
        	});
        	UmengUpdateAgent.update(this);
    	} else {
			CustomDialog dialog = new CustomDialog(SetMode.this);
			
			dialog.setTitle(R.string.dialog_tips);
			dialog.setMessage(R.string.set_mode_app_check_tips);
			dialog.setYesButton(R.string.yes, null);
    	}
	}
    
    public void	onDeviceClear(View view){
    	if(!isConnect()) {
    		showOpenDeviceDialog();
    		return;
    	}
    	
		CustomDialog dialog = new CustomDialog(this);
		
		dialog.setTitle(R.string.set_mode_clear_connection_history);
		dialog.setMessage(R.string.set_mode_clear_msg);
		dialog.setYesButton(R.string.set_mode_clear_ok, new CustomDialog.OnClickListener() {
			@Override
			public void onClick(Dialog dialog) {
				BluetoothMACListClean();
				mBluetoothLeService.disconnect();
				dialog.dismiss();
			}
		});
		dialog.setNoButton(R.string.set_mode_clear_cancel, null);
	}
    
    public void onHistoryClear(View view){
    	if(!isConnect()) {
    		showOpenDeviceDialog();
    		return;
    	}
    	
		CustomDialog dialog = new CustomDialog(this);
		
		dialog.setTitle(R.string.set_mode_clear_data_history);
		dialog.setMessage(R.string.set_mode_clear_use_msg);
		dialog.setYesButton(R.string.set_mode_clear_ok, new CustomDialog.OnClickListener() {
			@Override
			public void onClick(Dialog dialog) {
				BluetoothHistoryClean();
				dialog.dismiss();
			}
		});
		dialog.setNoButton(R.string.set_mode_clear_cancel, null);
    }
    
}
