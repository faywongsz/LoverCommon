package com.yuning.activity;

import com.yuning.util.sysinfo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LauncherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		boolean hasPassword = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0)
				.getBoolean(Password.PASSWORD_STATUS, false);
		if(hasPassword) {
			Intent intent = new Intent(this, PasswordProcess.class);
			intent.putExtra(Password.EXTRA_PASSWORD, Password.PasswordType.LAUNCHER);
			startActivity(intent);
		} else {
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		finish();
	}

}
