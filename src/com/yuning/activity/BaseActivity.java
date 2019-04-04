package com.yuning.activity;

import android.app.Activity;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.yuning.Service.BluetoothLeService;
import com.yuning.config.config;
import com.yuning.config.config.bleUUID;
import com.yuning.util.sendThread;
import com.yuning.util.sysinfo;

public class BaseActivity extends Activity
{
    private static final String TAG = "BaseActivity";
    protected static final int GATT_WRITE_COMMON_TIMEOUT = 250; // Milliseconds	
    protected static BluetoothLeService mBluetoothLeService;
    protected static boolean mConnected = false;
    protected static boolean mNeedPair = false;    
    protected static BluetoothGattCharacteristic mNotifyCharacteristic;
    protected static BluetoothGattCharacteristic mSelecedCharacteristic;  
    
    public final static String BASIC_MODE_VOICE_TYPE = "basic_mode_voice_type";
    public static final String CLASSIC_MODE_INDEX = "classic_mode_index";
    public static final String CUSTOMDRAW_MODE_DATA = "customdraw_mode_data";
    public final static String UMENG_APK_VERSION = "umeng_apk_version";
    public static final String MUSIC_COUNT = "music_count";
    public static final String MUSIC_INFO_PREFIX = "music_info_";
    public static final String VIDEO_COUNT = "video_count";
    public static final String VIDEO_INFO_PREFIX = "video_info_";
    public static final String REMOTE_COUNT = "remot_count";
    public static final String REMOTE_INFO_PREFIX = "remote_info_";
    public final static String PASSWORD = "password";
    public final static String PASSWORD_STATUS = "password_status";
    public final static String PASSWORD_WRONG_INPUTS = "password_wrong_inputs";
    public final static String PASSWORD_WRONG_TIME = "password_wrong_time";
    
	public BaseActivity()
	{
		sendThread.getInstance().startThread();
	}

	protected boolean isConnect(){
		return mConnected;
	}
	
	protected void setConnectStatus(boolean status){
		 mConnected = status;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());        
    }
    
    @Override
	protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);        
    }   
    
	public static boolean sendData(byte[] bs){
		if(mConnected){
		    if (mSelecedCharacteristic != null) {
		    	mSelecedCharacteristic.setValue(bs);
		        if(mBluetoothLeService.writeCharacteristic(mSelecedCharacteristic)){ 	        	
	                 return mBluetoothLeService.waitIdle(GATT_WRITE_COMMON_TIMEOUT);
		        }  
		    }	
		}
	    return false;
	}
	
	protected boolean getBoolData(String s)
	{
		return getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getBoolean(s, false);
	}
	
	protected int getIntData(String s)
	{
		return getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getInt(s, 0);
	}
	
	protected long getLongData(String s)
	{
		return getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getLong(s, 0);
	}
	
	protected String getStrData(String s)
	{
		return getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getString(s, null);
	}

	protected String readBluetoothMAC()
	{
		return getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getString("BluetoothMAC", null);
	}

	protected void saveBluetoothMAC(String s)
	{
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putString("BluetoothMAC", s);
		editor.commit();
	}
	
/*montague 20150103 begin*/
	protected ArrayList <String> readBluetoothMACList()
	{
		ArrayList <String> list = new ArrayList <String>();
		
		int size = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getInt("BluetoothMACSize", 0);
		for(int i=0; i < size; i++){
			String macStr = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).getString("BluetoothMAC" + i, null);
			if(macStr != null){
				list.add(macStr);
			}
		}
		return list;
	}
	
	protected void BluetoothMACListAdd(String s)
	{
		ArrayList <String> oldList = readBluetoothMACList();

		for(String itemAdd:oldList){
			if(itemAdd.equals(s)){
				return;
			}
		}
			
		int size = oldList.size();
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putString("BluetoothMAC" + size, s);
		size +=1;
		editor.putInt("BluetoothMACSize" ,size);
		editor.commit();		
		
	}
	
	protected void BluetoothMACListClean()
	{
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putInt("BluetoothMACSize" ,0);
		editor.commit();		
	}	
	/*montague 20150103 end*/	
	
	protected void BluetoothHistoryClean() {
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putString("ZhiAiAiTalkModeStrings", "");
		editor.remove(CLASSIC_MODE_INDEX);
		editor.remove(CUSTOMDRAW_MODE_DATA);
		editor.commit();
		
		cleanSPMusicInfo();
		cleanSPVideoInfo();
		cleanSPRemoteInfo();
	}
	
	protected void cleanSPMusicInfo() {
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		int count = getIntData(MUSIC_COUNT);
		for(int i = 0; i < count; i++) {
			editor.remove(MUSIC_INFO_PREFIX + i);
		}
		editor.putInt(MUSIC_COUNT, 0);
		
		editor.commit();
	}
	
	protected void cleanSPVideoInfo() {
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		int count = getIntData(VIDEO_COUNT);
		for(int i = 0; i < count; i++) {
			editor.remove(VIDEO_INFO_PREFIX + i);
		}
		editor.putInt(VIDEO_COUNT, 0);
		
		editor.commit();
	}
	
	protected void cleanSPRemoteInfo() {
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		int count = getIntData(REMOTE_COUNT);
		for(int i = 0; i < count; i++) {
			editor.remove(REMOTE_INFO_PREFIX + i);
		}
		editor.putInt(REMOTE_COUNT, 0);
		
		editor.commit();
	}
	
	protected void saveBoolData(String s, boolean b)
	{
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putBoolean(s, b);
		editor.commit();
	}

	protected void saveStrData(String s, String s1)
	{
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putString(s, s1);
		editor.commit();
	}
	
	protected void saveIntData(String s, int i)
	{
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putInt(s, i);
		editor.commit();
	}
	
	protected void saveLongData(String s, long i)
	{
		android.content.SharedPreferences.Editor editor = getSharedPreferences(sysinfo.getInstance(this).getPackageName(), 0).edit();
		editor.putLong(s, i);
		editor.commit();
	}
	
    protected final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            	//setConnectStatus(true);
                //updateConnectionState(true);
                
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            	setConnectStatus(false);
                updateConnectionState(false);
                disconnectCallBack();
                updateBatteryLevel(config.BATTERY_NULL_LEVLE);//reset battery high level:wangfei added
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            	setConnectStatus(true);
            	updateConnectionState(true);
            	selectGattServices(mBluetoothLeService.getSupportedGattServices());
                /*force update firmware:wangfei added begin*/
                discoveredService();
                /*force update firmware:wangfei added end*/
            } else if(BluetoothLeService.ACTION_DATA_WRITE.equals(action)){
				int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,BluetoothGatt.GATT_SUCCESS);
				if (status == BluetoothGatt.GATT_SUCCESS) {
				    writeReponse(BluetoothLeService.EXTRA_DATA, true);
				}else{
					writeReponse(BluetoothLeService.EXTRA_DATA, false);
				}		
            } else if(BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)){
				byte [] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);

				if (uuidStr.equals(bleUUID.PAIR_CHAR_UUID)) {
		        	    switch(value[0]){
		        	        case 1:
		        	        	updatePairStatus(true);
		        	          break;
		        	        case 2:
		        	        	updatePairStatus(false);
		        	          break;

		        	}	
		        	    
		        	    if(value[0] >= 10){
		        	    	updatezhendongLevel(value[0] - 10);
		        	    }
		        }else if(uuidStr.equals(bleUUID.BATTERY_CHAR_UUID)){
		        	updateBatteryLevel(value[0]);
		        }
            }
        }
    };	

    
    private void selectGattServices(List<BluetoothGattService> gattServices) {    	
        if (gattServices == null) return;
        for (BluetoothGattService gattService : gattServices) {
            String seUuid = gattService.getUuid().toString();
       	    Log.d(TAG, "seUuid list item = " + seUuid); 
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
        	/* montague 20150103 begin */	
            if(seUuid.equals(bleUUID.SERVICEUUID)){
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String caUuid = null;                	
                    caUuid = gattCharacteristic.getUuid().toString();                
                    if(caUuid.equals(bleUUID.CHARACTERISTICUUID1)){
                	   mSelecedCharacteristic = gattCharacteristic;
                    }else if(caUuid.equals(bleUUID.CHARACTERISTICUUID3)){
                	   
                       //final int charaProp = gattCharacteristic.getProperties();
                  	   //Log.d(TAG, "charaProp = " + charaProp); 
                       //if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                      	   //Log.d(TAG, "PROPERTY_NOTIFY");                     	   
                           //mNotifyCharacteristic = gattCharacteristic;
                          // mBluetoothLeService.setCharacteristicNotification(
                        	//	   gattCharacteristic, true);
                       //}                   
                    }
                }
            }else if(seUuid.equals(bleUUID.BATTERY_SERVICE_UUID)){
           	    Log.d(TAG, "battery seUuid = " + seUuid); 
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String caUuid = null;    
                
                    caUuid = gattCharacteristic.getUuid().toString();   
               	    Log.d(TAG, "battery caUuid = " + caUuid);                         
                    if(caUuid.equals(bleUUID.BATTERY_CHAR_UUID)){
                   	    Log.d(TAG, "BATTERY_CHAR_UUID = " + bleUUID.BATTERY_CHAR_UUID);
                    	mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                        mBluetoothLeService.waitIdle(GATT_WRITE_COMMON_TIMEOUT);   
                    }
                }
            }else if(seUuid.equals(bleUUID.PAIR_SERVICE_UUID)){
           	    Log.d(TAG, "pair seUuid = " + seUuid); 
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String caUuid = null;    
                
                    caUuid = gattCharacteristic.getUuid().toString();   
               	    Log.d(TAG, "pair caUuid = " + caUuid);                         
                    if(caUuid.equals(bleUUID.PAIR_CHAR_UUID)){
                   	    Log.d(TAG, "PAIR_CHAR_UUID = " + bleUUID.PAIR_CHAR_UUID);
                    	mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    	mBluetoothLeService.waitIdle(GATT_WRITE_COMMON_TIMEOUT);   
                    }
                }
            }
        	/* montague 20150103 end */    
        }
 
       /* montague 20150104 begin*/
        runPairCmdThread(mNeedPair);
        /*montague 20150104 end*/  
        
    }

    protected void removeGattServices(List<BluetoothGattService> gattServices) {    	
        if (gattServices == null) return;
        for (BluetoothGattService gattService : gattServices) {
            String seUuid = gattService.getUuid().toString();
       	    Log.d(TAG, "seUuid list item = " + seUuid); 
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            if(seUuid.equals(bleUUID.BATTERY_SERVICE_UUID)){
           	    Log.d(TAG, "battery seUuid = " + seUuid); 
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String caUuid = null;    
                
                    caUuid = gattCharacteristic.getUuid().toString();   
               	    Log.d(TAG, "battery caUuid = " + caUuid);                         
                    if(caUuid.equals(bleUUID.BATTERY_CHAR_UUID)){
                   	    Log.d(TAG, "BATTERY_CHAR_UUID = " + bleUUID.BATTERY_CHAR_UUID);
                    	mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, false);
                        mBluetoothLeService.waitIdle(GATT_WRITE_COMMON_TIMEOUT);   
                    }
                }
            }else if(seUuid.equals(bleUUID.PAIR_SERVICE_UUID)){
           	    Log.d(TAG, "pair seUuid = " + seUuid); 
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String caUuid = null;    
                
                    caUuid = gattCharacteristic.getUuid().toString();   
               	    Log.d(TAG, "pair caUuid = " + caUuid);                         
                    if(caUuid.equals(bleUUID.PAIR_CHAR_UUID)){
                   	    Log.d(TAG, "PAIR_CHAR_UUID = " + bleUUID.PAIR_CHAR_UUID);
                    	mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, false);
                    	mBluetoothLeService.waitIdle(GATT_WRITE_COMMON_TIMEOUT);   
                    }
                }
            }
  
        }
         
    }
    

	
    protected static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);	
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);	        
        
        return intentFilter;
    }   
    
    protected void updateConnectionState(final boolean status) {
        runOnUiThread(new Runnable() {
			@Override
            public void run() {

                Log.d(TAG,"ConnectionState:" + status);  
            }
        });
    }    
    protected void disconnectCallBack() {
    	/*some phones can't find oad services when disconnect bt*/
    	if(mBluetoothLeService != null) {
    		mBluetoothLeService.close();
    	}
    	/*some phones can't find oad services when disconnect bt*/
    }
    

    protected void response(String data) {
        if (data != null) {
            Log.d(TAG,"current data:" + data);         
        }
    }

    private void writeReponse(String data, boolean successed){
        Log.d(TAG,"writeReponse data:" + data);    		
        if (data != null && successed) {   			
  
        }  		
	}
    
    protected void updatePairStatus(boolean status) {
        if(mNeedPair && status){
        	runPairCmdThread(false);			
        }
    }
    
    /* montague 20150104 begin*/    
    protected void runPairCmdThread(final boolean status){
         new Thread(){  
             public void run(){  
                 final int max_retry_count = 5;
               	 byte btcmd[] = {0x04,0x00};
               	 
                 if(!status){
                	 btcmd[1] = 0x01;
                 }

              	int i = 0;

              	while(true){
              		boolean sendresult = sendData(btcmd);              		
              		if(sendresult){
              			break;
              		}
              		if(i > max_retry_count){
              			mBluetoothLeService.disconnect();
              			break;
              		}
              		
                  	i ++;                     	
      				try {
      					Thread.sleep(1000);
      				} catch (InterruptedException e) {
      					e.printStackTrace();
      				}        		
              	}

                               
             
             }  
         }.start();
    }
    /*montague 20150104 end*/   
    
    
    protected void updatezhendongLevel(int level){
    	
    }
    
    protected void updateBatteryLevel(int level){
    	
    }    
    
    /*force update firmware:wangfei added begin*/
    protected void discoveredService() {
    	
    }
    /*force update firmware:wangfei added end*/
    
    protected boolean hasNewVersion(Activity activity) {
    	String apkVersion = sysinfo.getInstance(activity).getversionName();
    	String umengVersion = getStrData(UMENG_APK_VERSION);
    	
    	if(apkVersion != null && umengVersion != null) {
    		String[] apkVersionStrs = apkVersion.split("\\.");
    		String[] umengVersionStrs = umengVersion.split("\\.");
    		
    		int[] apkVersionInts = new int[apkVersionStrs.length];
    		int[] umengVersionInts = new int[umengVersionStrs.length];
    		
    		for(int i = 0; i < apkVersionInts.length && i < umengVersionInts.length; i++) {
    			try {
        			apkVersionInts[i] = Integer.parseInt(apkVersionStrs[i]);
        			umengVersionInts[i] = Integer.parseInt(umengVersionStrs[i]);
    			} catch (NumberFormatException e) {
    				return false;
    			}
    		}
    		
    		for(int i = 0; i < apkVersionInts.length && i < umengVersionInts.length; i++) {
    			if(apkVersionInts[i] < umengVersionInts[i]){
    				return true;
    			} else if(apkVersionInts[i] > umengVersionInts[i]) {
    				return false;
    			} else {
    				continue;
    			}
    		}
    	}
    	
    	return false;
    }
    
}
