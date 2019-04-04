package com.yuning.util;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.yuning.Service.BluetoothLeService;
import com.yuning.activity.FwUpdateActivity;
import com.yuning.lovercommon.R;
import com.yuning.config.GattInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
/*force update firmware:wangfei added begin*/
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;

public class ForceUpdateFirmware {
	private static final String TAG = "ForceUpdateFirmware";
	public static final String ASSET_FIRMWARE_PATH = "Firmware";
	private static final String TARGET_DEVICE = "F520B-LT";
	private static final byte[] FIRMWARE_PROBLEM = {1, 1};
	private static final byte[] FIRMWARE_TARGET = {1, 2};
	
	private static String msCurrentDevice = "";
	private static ForceUpdateFirmware sInstance = null;
	private static boolean mbShowingDialog = false;
	
	private Context mContext;
	// BLE
	private BluetoothGattService mOadService;
	private List<BluetoothGattCharacteristic> mCharListOad;
	private BluetoothGattCharacteristic mCharIdentify = null;
	private BluetoothLeService mLeService;
	private IntentFilter mIntentFilter;
	private boolean mServiceOk = false;
	private boolean mStarting = false;
	
	private static final int GATT_WRITE_TIMEOUT = 250; // Milliseconds
	  
	 private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

		    if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
					byte [] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
					String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
					
			    if (uuidStr.equals(mCharIdentify.getUuid().toString())) {
			    	// Image info notification
			      Log.d(TAG, "Image info Notification ");
			      for(int i=0; i<value.length; i++){
			    	  Log.d(TAG,"value["+ i + "]=" + value[i]);
			      }
			      
			      short version = Conversion.buildUint16(value[1], value[0]);
			      byte[] targimgVersion = Conversion.buildFWVersion(version);
			      short targProduct = Conversion.buildProductType(version);
			      byte[] fileimgVersion = getFileFirmwareVersion(ASSET_FIRMWARE_PATH + "/AppA.bin");
			      short fileProduct = getFileProductType(ASSET_FIRMWARE_PATH + "/AppA.bin");
			      if(fileimgVersion == null || fileProduct == -1) {
			    	  fileimgVersion = getFileFirmwareVersion(ASSET_FIRMWARE_PATH + "/AppB.bin");
			    	  fileProduct = getFileProductType(ASSET_FIRMWARE_PATH + "/AppB.bin");
			      }
			      Log.i(TAG, "fileimgVersion = " + fileimgVersion + "targimgVersion = " + targimgVersion);
			      
			      if((targProduct == fileProduct) && (compareVersion(targimgVersion, FIRMWARE_PROBLEM) == 0) 
			    		  && (compareVersion(fileimgVersion, FIRMWARE_TARGET) == 0)
			    		  && (compareVersion(fileimgVersion, targimgVersion) > 0) && !mbShowingDialog) {
			    	  try {
				    	  new AlertDialog.Builder(mContext)
				    	  	.setTitle(R.string.force_update_firmware)
				    	  	.setMessage(R.string.force_update_firmware_message)
				    	  	.setCancelable(false)
				    	  	.setPositiveButton(R.string.force_update, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Intent intent = new Intent(mContext, FwUpdateActivity.class);
									intent.putExtra("is_force", true);
									mContext.startActivity(intent);
									dialog.dismiss();
								}
							})
				    	  	.setOnKeyListener(new DialogInterface.OnKeyListener() {
								@Override
								public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
									if (keyCode==KeyEvent.KEYCODE_BACK) {
										return true;
									}
									return false;
								}
							}).show();
				    	  mbShowingDialog = true;
			    	  } catch (Exception e) {
			    		  
			    	  }
			      }
			    }
			    
			} 
		}
	};
		
	public static ForceUpdateFirmware getInstance(Activity context) {
		if(sInstance == null) {
			sInstance = new ForceUpdateFirmware(context);
		}
		
		return sInstance;
	}
	
	public ForceUpdateFirmware(Activity context) {
		mContext = context;
		mLeService = BluetoothLeService.getInstance();
	}
	
	public void start() {
		Log.d(TAG, "start");
		//if(mLeService == null) {
			mLeService = BluetoothLeService.getInstance();
			if(mLeService == null) {
				return;
			}
		//}
		
		if(mStarting) {
			Log.w(TAG, "is starting!!!");
			return;
		}
		mStarting = true;
		checkOad();
	    if(mOadService != null){
	        // Characteristics list
	        mCharListOad = mOadService.getCharacteristics();
	        //mCharListCc = mConnControlService.getCharacteristics();

	        mServiceOk = mCharListOad.size() == 2; // && mCharListCc.size() >= 3;
	        if (mServiceOk) {
	          mCharIdentify = mCharListOad.get(0);
	          //mCharConnReq = mCharListCc.get(1);
	        }    	
	    }    
	    
	    if(mOadService == null){
	    	Log.w(TAG, "mOadService is null!");
	    	mStarting = false;
	    	return;
	    }  
	    
	    
	    if (mServiceOk) {
	    	initIntentFilter();
	    	mContext.registerReceiver(mGattUpdateReceiver, mIntentFilter);
	    	// Read target image info
	    	getTargetImageInfo();
	  		// Connection interval is too low by default
		  	//setConnectionParameters();
	    } else {
	    	//null
	    	mStarting = false;
	    	Log.w(TAG, "mServiceOk is not ok!");
	    }
	}
	
	public void stop() {
		Log.d(TAG, "stop");
	  	if(mServiceOk){
	  	  	mContext.unregisterReceiver(mGattUpdateReceiver);
	  	    mServiceOk = false;
	  	}
	  	mStarting = false;
	  	mbShowingDialog = false;
	}
	
	private void getTargetImageInfo() {
	    // Enable notification
	    boolean ok = enableNotification(mCharIdentify, true);
	    // Prepare data for request (try image A and B respectively, only one of
	    // them will give a notification with the image info)
	    if (ok)
	      ok = writeCharacteristic(mCharIdentify, (byte) 0);
	    if (ok)
	      ok = writeCharacteristic(mCharIdentify, (byte) 1);
	    if (!ok) {
	    	//null
	    }
	}
	
	private boolean writeCharacteristic(BluetoothGattCharacteristic c, byte v) {
		    boolean ok = mLeService.writeCharacteristic(c, v);
		    if (ok)
		      ok = mLeService.waitIdle(GATT_WRITE_TIMEOUT);
		    return ok;
	}
		  
	private boolean enableNotification(BluetoothGattCharacteristic c, boolean enable) {
		    boolean ok = mLeService.setCharacteristicNotification(c, enable);
		    if (ok)
		      ok = mLeService.waitIdle(GATT_WRITE_TIMEOUT);   
		    return ok;
	}  
	
	private void initIntentFilter() {
		  	mIntentFilter = new IntentFilter();
		  	mIntentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
		  	mIntentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
	}
		
	private void checkOad() {
		List<BluetoothGattService> serviceList = mLeService.getSupportedGattServices();	  
		mOadService = null;
		for (int i = 0; (serviceList != null) && (i < serviceList.size())
		    && (mOadService == null); i++) {
			BluetoothGattService srv = serviceList.get(i);
			if (srv.getUuid().equals(GattInfo.OAD_SERVICE_UUID)) {
				mOadService = srv;
			}			
		}
	}
	
	private byte[] getFileFirmwareVersion(String path) {
		try {
			byte[] buffer = new byte[6];
			InputStream stream = mContext.getAssets().open(path);
			stream.read(buffer, 0, buffer.length);
			stream.close();
			short version = Conversion.buildUint16(buffer[5], buffer[4]);
			return Conversion.buildFWVersion(version);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	private short getFileProductType(String path) {
		try {
			byte[] buffer = new byte[6];
			InputStream stream = mContext.getAssets().open(path);
			stream.read(buffer, 0, buffer.length);
			stream.close();
			short version = Conversion.buildUint16(buffer[5], buffer[4]);
			return Conversion.buildProductType(version);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		
	}
	
	private int compareVersion(byte[] fileVersion, byte[] targetVersion) {
		if(fileVersion == null || fileVersion.length != 2
				|| targetVersion == null || targetVersion.length != 2) {
			return -1;
		}
		
		if(fileVersion[0] > targetVersion[0]) {
			return 1;
		} else if(fileVersion[0] == targetVersion[0]) {
			if(fileVersion[1] > targetVersion[1]) {
				return 1;
			} else if(fileVersion[1] == targetVersion[1]) {
				return 0;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}
	
	public static boolean isForceUpdateFirmware(Context context) {
		try {
			String files[] = context.getAssets().list(ASSET_FIRMWARE_PATH);
			boolean isAbin = false, isBbin = false;
			for(int i = 0; i < files.length; i++) {
				if(files[i].equals("AppA.bin")) {
					isAbin = true;
				} else if(files[i].equals("AppB.bin")) {
					isBbin = true;
				}	
			}
			return isAbin && isBbin;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean isTargetDevice(String device) {
		return device.equals(TARGET_DEVICE);
	}
	
	public static void setTargetDevice(String device) {
		msCurrentDevice = device;
	}
	
}
/*force update firmware:wangfei added end*/