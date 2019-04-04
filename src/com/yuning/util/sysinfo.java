package com.yuning.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public class sysinfo
{

	private int miVCode;
	private int miscreenHeight;
	private int miscreenWidth;
	private String msVName;
	private String mspackageName;
	private static sysinfo mInstance = null;

	public static sysinfo getInstance(Activity activity) {
		if(mInstance == null) {
			mInstance = new sysinfo(activity);
		}
		
		return mInstance;
	}
	
	private sysinfo(Activity activity)
	{
		PackageManager packagemanager;
		try
		{
			packagemanager = activity.getPackageManager();
			PackageInfo packageinfo = packagemanager.getPackageInfo(activity.getPackageName(), 0);
			mspackageName = packageinfo.packageName;
			miVCode = packageinfo.versionCode;
			msVName = packageinfo.versionName;
			Display display = activity.getWindowManager().getDefaultDisplay();
			miscreenHeight = display.getHeight();
			miscreenWidth = display.getWidth();
		}
		catch (PackageManager.NameNotFoundException namenotfoundexception)
		{
			
		}
	}

	public String getPackageName()
	{
		return mspackageName;
	}

	public int getscreenHeight()
	{
		return miscreenHeight;
	}

	public int getscreenWidth()
	{
		return miscreenWidth;
	}

	public int getversionCode()
	{
		return miVCode;
	}

	public String getversionName()
	{
		return msVName;
	}
	
	/*wangfei added begin*/
	public static boolean isExeceptionProduct(Context context) {
		int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
		
		String model = Build.MODEL;
		String manufacturer = Build.MANUFACTURER;
		return (model.equals("MX4 Pro") && manufacturer.equals("Meizu") && screenHeight >= 2560)
				|| (model.equals("MX4") && manufacturer.equals("Meizu") && screenHeight >= 1920);
	}
	/*wangfei added end*/
	
	public static boolean isNetworkAvailable(Context context) {

		boolean available = false;

		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (cm != null) {
			try {
				NetworkInfo info = cm.getActiveNetworkInfo();
				if (info != null && info.isAvailable()) {
					available = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return available;
	}
}
