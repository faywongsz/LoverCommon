/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuning.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.http.Header;
import org.apache.http.HttpStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.asr.AsrService.Asr;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;
import com.umeng.update.UmengUpdateAgent;
import com.umeng.update.UmengUpdateListener;
import com.umeng.update.UpdateResponse;
import com.umeng.update.UpdateStatus;
import com.yuning.Service.BluetoothLeService;
import com.yuning.config.config;
import com.yuning.game.GameMode;
import com.yuning.lovercommon.R;
import com.yuning.remote.RemoteModeMain;
import com.yuning.ui.ArcMenu;
import com.yuning.ui.CustomDialog;
import com.yuning.util.ForceUpdateFirmware;
import com.yuning.util.ShakeListener;
import com.yuning.util.sendThread;
import com.yuning.util.sysinfo;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class MainActivity extends BaseActivity {
    private final static String TAG = MainActivity.class.getSimpleName();	
    private final static String []PROJECT_NAME = {"F525"};
    private final static String UMENG_LAST_UPDATE_TIME = "umeng_last_update_time";
    private final static long UMENG_UPDATE_TIME_INTERVAL = 3 * 24 * 60 * 60 * 1000;//3 days
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    public static boolean isServiceBind = false;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 5000;

    private final int REQ_DEVICE_ACT = 3;
    
    LeDeviceListAdapter mLeDeviceListAdapter;
    
    private String mDeviceAddress;
    private ArrayList <String> mHistoryAddress = new ArrayList<String>();
    private ArrayList <String> mScanListAddress = new ArrayList<String>();
    //private ArrayList <String> mRefuseAddress = new ArrayList<String>();
    private GridView gatt_device_list;
    private ProgressBar mProgressBar;
    private ImageView mScanSearched;
    private Button mConnectedFlag;
    //private Button mScanButton;
    private TextView mConnectStateBar;
    private ImageView mImageNewTips;
    //private ImageView mConnectStateLogo;
    private ShakeListener mShaker;
    private long lastUpdateTime;
    private static final long UPTATE_INTERVAL_TIME = 20L; 	
    private final static int CONNECT_TIMEOUT_HANDLE = 100;
    private final static int CONNECT_TIMEOUT_DELAY = 15000;
    private final static int CONNECT_TIMEOUT_SUCCESS = 300;
    private final static int CONNECT_TIMEOUT_SUCCESS_DELAY = 3000;
    private String mConnectSuccessText;
    //private int mConnectLogoVisible;
    private int mDisconnectClickCount = 0;
    private boolean mIsConnecting = false;
    private ImageView mbattLevel;
    //private Boolean mBatteryIsLow = false;
    //private static final byte BATTERY_LOW_LEVEL = 1; /* montague 20150126 */
    private View mShoppingLayout;
    
    /*reset bt connect:wangfei added begin*/
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				final int previousstate = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
				Log.d(TAG, "state = " + state + ", previousstate = " + previousstate);
				switch(state) {
					case BluetoothAdapter.STATE_OFF :
						startOpenBTActivity();
						break;
				}
			}
		}
    	
    };
	/*reset bt connect:wangfei added end*/
    /*add anims by guoliangliang 20150203 begin*/
    /*private final float mAnimTimeRate = 1.5f;
    private FrameLayout mFrameAnim;
    private ImageView mImageRainbow1;
    private AnimationSet mAnimRainbow11;
    private AnimationSet mAnimRainbow12;
    private AnimationSet mAnimRainbow13;
    
    private ImageView mImageRainbow2;
    private AnimationSet mAnimRainbow21;
    private AnimationSet mAnimRainbow22;
    
    private LinearLayout mRainbow3Parent;
    private ImageView mImageRainbow3;
    private AnimationSet mAnimRainbow31;
    private AnimationSet mAnimRainbow32;
    
    private ImageView mImageText1Red;
    private ImageView mImageText1White;
    private AnimationSet mAnimText1RedNull;
    private AnimationSet mAnimText1WhiteNull;
    private AnimationSet mAnimText1Red1;
    private AnimationSet mAnimText1White1;
    private AnimationSet mAnimText1Red2;
    private AnimationSet mAnimText1White2;
    private AnimationSet mAnimText1Red3;
    private AnimationSet mAnimText1White3;
    private AnimationSet mAnimText1Red4;
    private AnimationSet mAnimText1White4;
    private AnimationSet mAnimText1Red5Null;
    private AnimationSet mAnimText1White5Null;
    private AnimationSet mAnimText1Red6;
    private AnimationSet mAnimText1White6;
    
    private ImageView mImageText2Red;
    private ImageView mImageText2White;
    private FrameLayout mFrameText2;
    private AnimationSet mAnimFrameText2Null;
    private AnimationSet mAnimText2Red1;
    private AnimationSet mAnimText2Red2;
    private AnimationSet mAnimText2White1;
    private AnimationSet mAnimText2White2;
    
    private ImageView mImageText3Red;
    private ImageView mImageText3White;
    private AlphaAnimation mAnimText3RedNull;
    private AlphaAnimation mAnimText3WhiteNull;
    private AnimationSet mAnimText3Red1;
    private AnimationSet mAnimText3White1;
    
    private ImageView mImagePinkCircle;
    private AlphaAnimation mAnimPinkCircleAlpha;
    private ScaleAnimation mAnimPinkCircleScale;
    
    private ImageView mImageLogo;
    private AlphaAnimation mAnimLogoAlpha1;
    private AlphaAnimation mAnimLogoAlpha2;*/
    /*add anims by guoliangliang 20150203 end*/
    
    /*any chat:wangfei added begin*/
    private static final boolean HAVE_ANYCHAT = true;
    /*any chat:wangfei added end*/
    
    private Handler mConnectHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		switch(msg.what){
    		   case CONNECT_TIMEOUT_HANDLE:
    	        	Log.d(TAG,  "connectDevice timeout error");
    	        	disConnectCurrenDevice();
    	        	updateConnectionState(false);
    				/*Toast.makeText(MainActivity.this, 
    						getString(R.string.msg_device_bluetooth_error), 0).show(); */
    			   break;
    		   case CONNECT_TIMEOUT_SUCCESS:
    			   if(mDisconnectClickCount < 1 && isConnect()){
	    			   Log.d(TAG,"mConnectStateBar change text to beautiful");
	    			   mConnectSuccessText = getResources().getString(R.string.main_connect_state_success_beautiful);
	    			   mConnectStateBar.setText(mConnectSuccessText);
	    			   //mConnectLogoVisible = View.VISIBLE;
	    			   //mConnectStateLogo.setVisibility(View.VISIBLE);
    			   }
    			   break;
    		}
    	}
    };
    
    byte[] mStop = {(byte) 0xff,(byte)0xff};	
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        mConnectSuccessText = getResources().getString(R.string.main_connect_state_success);
        //mConnectLogoVisible = View.GONE;
        mShaker = new ShakeListener(this);
        mShaker.setOnShakeListener(new ShakeListener.OnShakeListener() {  
        	//int mPwmLevel = 0;
            public void onShake(int level) {
    		    long currentUpdateTime = SystemClock.elapsedRealtime();  
    		    long timeInterval = currentUpdateTime - lastUpdateTime; 
    		    if(timeInterval < UPTATE_INTERVAL_TIME) {  
    		         return;  
    		    }
    		    lastUpdateTime = currentUpdateTime;
    			if(/*mPwmLevel != level*/ level > 15){
    				if(isConnect() == false && mScanning == false){
    					Log.d(TAG,"onShake,isConnect() == false && mScanning == false, scanLeDevice(true)");
    					dismissMultiDevicesDialog();
    					mLeDeviceListAdapter.clear();
    					scanLeDevice(true);
    				}
    			}
				
            } 
            	
        });
        //mShaker.start();
		MobclickAgent.setDebugMode(true);
		//MobclickAgent.openActivityDurationTrack(false);        
		MobclickAgent.updateOnlineConfig(this);
				
        setContentView(R.layout.main_activity);
        
       // getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        
        mbattLevel = (ImageView)findViewById(R.id.bat_level);
        
        mProgressBar = (ProgressBar)findViewById(R.id.gatt_scan_progress);       
        mScanSearched = (ImageView)findViewById(R.id.gatt_scan_searched);
        mConnectedFlag = (Button)findViewById(R.id.gatt_connect_flag);
        mConnectStateBar = (TextView)findViewById(R.id.main_connect_state);
        mImageNewTips = (ImageView) findViewById(R.id.main_settings_tips);
        //mConnectStateLogo = (ImageView)findViewById(R.id.main_connect_state_logo);
        mShoppingLayout = findViewById(R.id.shopping_layout);
        gatt_device_list = (GridView)findViewById(R.id.gatt_device_list);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        gatt_device_list.setAdapter(mLeDeviceListAdapter);
 
		gatt_device_list.setOnItemClickListener(new GridView.OnItemClickListener(){ 
		       public void onItemClick(AdapterView adapterView, View view,int postion, long id) 
		       { 
                    final String deviceAddress = mLeDeviceListAdapter.getDeviceAddress(postion);
                    if(!isConnect()){
                    	mDeviceAddress = deviceAddress; 
                    	if(isHistroyDevice(mDeviceAddress)){
                			android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
                			dialog.setTitle(getString(R.string.connect_device_confirm_connect_title));
                			dialog.setMessage(getString(R.string.connect_device_confirm_connect_info));
                			dialog.setPositiveButton(R.string.confirm_connect, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						mNeedPair = false;
                						connectDevice(mDeviceAddress);
                						
                					}
                				});
                				
                			dialog.setNegativeButton(R.string.confirm_cancel, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						mDeviceAddress = null;
                					}
                				});
                			dialog.create().show();                    		
                    	}else{
                			android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
                			dialog.setTitle(getString(R.string.connect_device_confirm_pair_title));
                			dialog.setMessage(getString(R.string.connect_device_confirm_pair_info));
                			dialog.setPositiveButton(R.string.connect_device_confirm_pair_ok, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						mNeedPair = true;
                						connectDevice(mDeviceAddress);
                					}
                			});
                			
                			dialog.setNegativeButton(R.string.connect_device_confirm_pair_cancel, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						mDeviceAddress = null;
                					}
                				});
                			dialog.setOnKeyListener(new OnKeyListener() {  
                	        	  
                	            @Override  
                	            public boolean onKey(DialogInterface dialog, int keyCode,  
                	                    KeyEvent event) {  
                	                if (keyCode == KeyEvent.KEYCODE_BACK) {   /* && event.getRepeatCount() == 0 */

                	                }  
                	                return false;  
                	            }  
                	        });
                			dialog.create().show();
                    	}
                                           	
                    }else{
                    	if(mNeedPair){
                			android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
                			dialog.setTitle(getString(R.string.connect_device_cancel_pair_title));
                			dialog.setMessage(getString(R.string.connect_device_cancel_pair_info));
                			dialog.setPositiveButton(R.string.connect_device_cancel_pair_ok, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						disConnectCurrenDevice();
                					}
                			});
                				
                			dialog.setNegativeButton(R.string.connect_device_cancel_pair_cancel, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						
                					}
                				});
                			dialog.create().show();                     		
                    	}else{
                			android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(MainActivity.this);
                			dialog.setTitle(getString(R.string.connect_device_confirm_disconnect_title));
                			dialog.setMessage(getString(R.string.connect_device_confirm_disconnect_info));
                			dialog.setPositiveButton(R.string.confirm_disconnect, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						disConnectCurrenDevice();
                					}
                			});
                				
                			dialog.setNegativeButton(R.string.confirm_cancel, new android.content.DialogInterface.OnClickListener() {
                					public void onClick(DialogInterface dialoginterface, int i)
                					{
                						
                					}
                				});
                			dialog.create().show();                       		
                    	}
                     	
                    }

		       }	
		   } );

        

		/* umeng push */
		PushAgent mPushAgent = PushAgent.getInstance(this);
		mPushAgent.enable();	
		PushAgent.getInstance(this).onAppStart();	
		//umeng auto update
		final long lastUpdateTime = getLongData(UMENG_LAST_UPDATE_TIME);
		final long now = System.currentTimeMillis();
		if((now - lastUpdateTime) > UMENG_UPDATE_TIME_INTERVAL) {
	    	UmengUpdateAgent.setUpdateAutoPopup(false);
	    	UmengUpdateAgent.setUpdateListener(new UmengUpdateListener() {
	    	    @Override
	    	    public void onUpdateReturned(int updateStatus,UpdateResponse updateInfo) {
	    	    	Log.d(TAG, "updateStatus = " + updateStatus);
	    	        switch (updateStatus) {
		    	        case UpdateStatus.Yes: // has update
		    	            UmengUpdateAgent.showUpdateDialog(getApplicationContext(), updateInfo);
		    	            saveLongData(UMENG_LAST_UPDATE_TIME, now);
		    	            saveStrData(UMENG_APK_VERSION, updateInfo.version);
		    	            mImageNewTips.setVisibility(hasNewVersion(MainActivity.this) ? View.VISIBLE : View.INVISIBLE);
		    	            break;
		    	        case UpdateStatus.No: // has no update
		    	            saveStrData(UMENG_APK_VERSION, "0.0");
		    	            mImageNewTips.setVisibility(hasNewVersion(MainActivity.this) ? View.VISIBLE : View.INVISIBLE);
		    	        case UpdateStatus.NoneWifi: // none wifi
		    	        case UpdateStatus.Timeout: // time out
		    	            break;
	    	        }
	    	    }
	    	});
	    	UmengUpdateAgent.update(this);
		}
		
		/*wangfei added begin*/
		Thread thread=new Thread(new Runnable()  {  
            @Override  
            public void run() {  
        		CopyAssets("asr".toString(), BasicVoiceMode.ASR_PATH);
        		MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Asr.JniCreate();
					}
				});
            }  
        });  

        if(isAsrFileExist()){
        	Log.d(TAG, "asr file is exist");
        	Asr.JniCreate();
        }else{
        	Log.d(TAG, "asr file not exist");   
            thread.start(); 
        }
		/*wangfei added end*/	
		
		config.BATTERY_LEVEL = config.BATTERY_NULL_LEVLE;
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);  
        
		mShoppingLayout.setVisibility(View.INVISIBLE);
		postShopping();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
		MobclickAgent.onPageStart( TAG );
		MobclickAgent.onResume(this);
		Log.d(TAG,"onResume()");
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        
		mDisconnectClickCount = 0;
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            	startOpenBTActivity();
                /*reset bt connect:wangfei added begin*/
                return;
                /*reset bt connect:wangfei added begin*/
            }
        }

        
        if(!isConnect()){
            scanLeDevice(true);
        }
        mShaker.start();
        
		/*force update firmware:wangfei added begin*/
		startForceUpdateFirmware();
		/*force update firmware:wangfei added end*/
		mImageNewTips.setVisibility(hasNewVersion(this) ? View.VISIBLE : View.INVISIBLE);
		updateBatteryLevel(config.BATTERY_LEVEL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
		MobclickAgent.onPageEnd( TAG );
		MobclickAgent.onPause(this);
		Log.d(TAG,"onPause()");
		mShaker.stop();
        scanLeDevice(false);
        dismissMultiDevicesDialog();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy(){
    	/*wangfei added begin*/
    	if(!sysinfo.isExeceptionProduct(getApplicationContext())) {
    		Asr.JniDestroy();
    	} else {
    		System.exit(0);
    	}
    	/*wangfei added end*/
    	super.onDestroy();
    	
    	/*
    	if(isConnect()){
    		saveBluetoothMAC(mDeviceAddress);
    	}else{
    		saveBluetoothMAC(null);
    	}

    	
		String tempmac = readBluetoothMAC();
		Log.d(TAG, "init address:" + tempmac);  
		*/
    	
    	if(isServiceBind){
        	unbindService(mServiceConnection);
        	//isServiceBind = false;
    	}
    	mBluetoothLeService = null;
    	setConnectStatus(false);
    	
	   /*force update firmware:wangfei added begin*/
	   if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())) {
		   ForceUpdateFirmware.getInstance(this).stop();
	   }
	   /*force update firmware:wangfei added end*/
    }
    
    public boolean onKeyDown(int KeyCode, KeyEvent keyevent)
	{
        if (KeyCode == KeyEvent.KEYCODE_BACK){
			CustomDialog dialog = new CustomDialog(this);
			
			dialog.setTitle(R.string.back_dialog_title);
			dialog.setMessage(R.string.back_dialog_mgs);
			dialog.setYesButton(R.string.back_dialog_ok, new CustomDialog.OnClickListener() {
				@Override
				public void onClick(Dialog dialog) {
					finish();
				}
			});
			dialog.setNoButton(R.string.back_dialog_cancle, null);
			return true;
		}
		return super.onKeyDown(KeyCode, keyevent);
	}
    
    private Runnable mScanRunnable = new Runnable() {
		@Override
		public void run() {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            //updateDeviceStatus();
            connectDeviceAfterSearch();
		}
	};

    private void scanLeDevice(final boolean enable) {
        if (enable) {
        	if(!mBluetoothAdapter.isEnabled()) {
        		startOpenBTActivity();
                return;
        	}
        	
            mHandler.postDelayed(mScanRunnable, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
        	mLeDeviceListAdapter.clear();
        	mHandler.removeCallbacks(mScanRunnable);
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        updateDeviceStatus();
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(device != null){
                    	if(device.getName() != null){
                            if(/*device.getName().equals(PROJECT_NAME)*/isDeviceInProject(device.getName())
                            		/*|| device.getName().equals("Keyfobdemo")*/){
                            	Log.i(TAG, "rssi=" + rssi + "scanRecord=" + scanRecord.toString());
                            	mLeDeviceListAdapter.addDevice(device.getAddress());
                            	mLeDeviceListAdapter.notifyDataSetChanged();

                                String deviceAddr = device.getAddress();
                                if(isHistroyDevice(deviceAddr)){
/*                            		mDeviceAddress = deviceAddr;
                            		mNeedPair = false;                            		
                            		connectDevice(mDeviceAddress);                            		*/
                                }else{
                                    Log.d(TAG, "mScanListAddress add:" + deviceAddr); 
                                	mScanListAddress.add(deviceAddr);
                                }

                            }
                    	}
 
                    }

                }
            });
        }
    };
    
    private boolean isDeviceInProject(String deviceName){
    	Log.d(TAG,"deviceName="+deviceName);
    	for(int i = 0; i < PROJECT_NAME.length; i ++){
    		if(deviceName.equals(PROJECT_NAME[i])){
    			return true;
    		}
    	}
    	return false;
    }
    
    private void connectDevice(String deviceAddress){
    	Log.d(TAG,"connectDevice "+deviceAddress);
    	boolean connectResult = false;
        //scanLeDevice(false);
                
        //Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        //bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);  
        //isServiceBind = true;
        
        if (mBluetoothLeService != null) {
            connectResult = mBluetoothLeService.connect(deviceAddress);
        } 
        
        
        if(connectResult){
        	mIsConnecting = true;
        	//modified by wangfei
        	mConnectHandler.sendEmptyMessageDelayed(CONNECT_TIMEOUT_HANDLE,
        			CONNECT_TIMEOUT_DELAY);
        	updateDeviceStatus();
        }else{
        	Log.d(TAG,  "connectDevice result error");
        	mIsConnecting = false;
        	mConnectHandler.removeMessages(CONNECT_TIMEOUT_HANDLE);
        	updateConnectionState(false);
        }        
    }
    
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

      
    private class LeDeviceListAdapter extends BaseAdapter{
    	Context mContext;
    	LayoutInflater mInflater;	
    	//private ArrayList <BluetoothDevice> mLeDevices;
    	private ArrayList <String> mLeDevicesAddress; 	
 
    	
    	String mConnedAddress = null;
    
    	    	
    	public LeDeviceListAdapter(Context context){
    		mContext = context;
    		mLeDevicesAddress = new ArrayList<String>();
    		mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	public void addDevice(String deviceAddress){
    		if(!mLeDevicesAddress.contains(deviceAddress)){ 			
    			mLeDevicesAddress.add(deviceAddress);
    		}
    	}
    	
    	public void setConnectedDeviceByAddree(String address){
    		mConnedAddress = address;
    	}

    	
    	public void clear(){
    		mLeDevicesAddress.clear();
    	}
    	
    	public void sortByHistory() {
    		Log.i(TAG, "sort begin: " + mLeDevicesAddress.toString());
    		Collections.sort(mLeDevicesAddress, new Comparator<String>() {
				@Override
				public int compare(String lhs, String rhs) {
					if(isHistroyDevice(lhs) && !isHistroyDevice(rhs)) {
						return -1;
					}
					return 0;
				}
			});
    		Log.i(TAG, "sort after: " + mLeDevicesAddress.toString());
    	}
    	
    	public String getDeviceAddress(int postion){
    		return mLeDevicesAddress.get(postion);
    	}
		@Override
		public int getCount() {

			return mLeDevicesAddress.size();
		}

		@Override
		public Object getItem(int index) {

			return mLeDevicesAddress.get(index);
		}

		@Override
		public long getItemId(int i) {

			return i;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			String address = mLeDevicesAddress.get(position);
			
            if(convertView == null){
            	convertView = mInflater.inflate(R.layout.listitem_device, null);
            }
            
            TextView deviceAddr = (TextView)convertView.findViewById(R.id.device_address);
            TextView deviceName = (TextView)convertView.findViewById(R.id.device_name); 
			
			/*if(mConnedAddress == null){
				deviceName.setText(R.string.menu_disconnect); 
			}else if(mConnedAddress.equals(address)){
			    if(mNeedPair){
                    deviceName.setText(getString(R.string.menu_pair));
            	}else{
                    deviceName.setText(getString(R.string.menu_connect));
            	}
			}else{
                deviceName.setText(R.string.menu_disconnect);
            }*/
            StringBuilder stringBuidler = new StringBuilder();
            stringBuidler.append(getResources().getString(R.string.device_toy) + " " + (position + 1));
            stringBuidler.append(" (" 
            		+ (isHistroyDevice(address) ? getResources().getString(R.string.device_has_been_connected) : getResources().getString(R.string.device_has_not_been_connected)) 
            				+ ")");
            deviceName.setText(stringBuidler);
            deviceAddr.setText(address);
			return convertView;
		}
		
    	
    }
    

    @Override
    protected void updateConnectionState(final boolean connectStatus) {
    	super.updateConnectionState(connectStatus);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	if(connectStatus){
            		mIsConnecting = false;
            		mConnectHandler.removeMessages(CONNECT_TIMEOUT_HANDLE);
            		
            		mLeDeviceListAdapter.setConnectedDeviceByAddree(mDeviceAddress);
            		if(mDeviceAddress != null){
                    	mLeDeviceListAdapter.addDevice(mDeviceAddress);
                    	mLeDeviceListAdapter.notifyDataSetChanged();
            		}

            	}else{
            		mDeviceAddress = null;
					mNeedPair = false;
            		mLeDeviceListAdapter.setConnectedDeviceByAddree(null);
					stopDeviceActivity();
					if(mIsConnecting){
			        	Log.d(TAG,  "connectDevice status error");
						Toast.makeText(MainActivity.this, 
								getString(R.string.msg_device_bluetooth_error), 0).show(); 
						mIsConnecting = false;
						mConnectHandler.removeMessages(CONNECT_TIMEOUT_HANDLE);
					}
					/*force update firmware:wangfei added begin*/
					if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())) {
						ForceUpdateFirmware.getInstance(MainActivity.this).stop();
					}
					/*force update firmware:wangfei added end*/
					dismissMultiDevicesDialog();
            	}            	
            	updateDeviceStatus();
            	
                Log.d(TAG,"updateConnectionState ConnectionState:" +connectStatus);  
            }
        });
    }    
    
    @Override    
    protected void disconnectCallBack() {

    }
    @Override    
    protected void response(String data) {
    	super.response(data);
        if (data != null) {
            Log.d(TAG,"current data:" + data);         
        }
    }    
    
    
    private void updateDeviceStatus(){
        Log.d(TAG, "updateDeviceStatus mNeedPair:" +mNeedPair + "isConnect :" + isConnect() + " mIsConnecting :" + mIsConnecting);   	
    	//mScanSearched.setImageResource(isConnect() ? R.drawable.index_foot_scan_connected : R.drawable.index_foot_scan_refresh);
        mScanSearched.setImageResource(R.drawable.index_foot_scan_connected);
        if(isConnect() && mNeedPair == false) {
        	mConnectedFlag.setBackgroundResource(R.drawable.index_foot_connected_selector);
        	mConnectedFlag.setText(R.string.close_device);
        } else {
        	mConnectedFlag.setBackgroundResource(R.drawable.index_foot_disconnected);
        	mConnectedFlag.setText("");
        }
        
    	if(isConnect()){
        	if(mNeedPair){
        		mConnectStateBar.setText(R.string.main_connect_state_long_press_to_confirm);
        		//mConnectStateLogo.setVisibility(View.GONE);
            	mScanSearched.setVisibility(View.INVISIBLE);
            	mProgressBar.setVisibility(View.VISIBLE); 
        	}else{
        		mConnectStateBar.setText(mConnectSuccessText);
        		//mConnectStateLogo.setVisibility(mConnectLogoVisible);
        		mConnectHandler.sendEmptyMessageDelayed(CONNECT_TIMEOUT_SUCCESS, CONNECT_TIMEOUT_SUCCESS_DELAY);
            	mProgressBar.setVisibility(View.INVISIBLE);  
            	mScanSearched.setVisibility(View.VISIBLE);
        	}
        }else{
        	mConnectStateBar.setText(mIsConnecting ? R.string.connecting_device : (mScanning ? R.string.scanning_device : R.string.main_connect_state_shake));
        	//mConnectStateLogo.setVisibility(View.GONE);
        	if (mScanning || mIsConnecting) {
            	//mScanButton.setText(R.string.menu_stop);
            	mProgressBar.setVisibility(View.VISIBLE);
            	mScanSearched.setVisibility(View.INVISIBLE);
            } else {
            	//mScanButton.setText(R.string.menu_scan); 
            	mProgressBar.setVisibility(View.INVISIBLE);
            	mScanSearched.setVisibility(View.VISIBLE);
            }  
        	mDisconnectClickCount = 0;
        }    	
        
        mLeDeviceListAdapter.notifyDataSetChanged();
        
    }
    
    private void connectDeviceAfterSearch(){
    	int deviceCount = mLeDeviceListAdapter.getCount();
        Log.d(TAG,"updateDeviceStatus,deviceCount="+deviceCount+"  mScanning="+mScanning);
        if(mScanning == false && isConnect() == false){
	        if(deviceCount <= 0){
	        	Toast.makeText(this, getString(R.string.main_connect_no_toy), 0).show();
	        	mProgressBar.setVisibility(View.INVISIBLE);  
            	mScanSearched.setVisibility(View.VISIBLE);
            	updateDeviceStatus();
	        }else if(deviceCount > 1){
	        	//Toast.makeText(this, getString(R.string.main_connect_close_others), 0).show();
	        	mProgressBar.setVisibility(View.INVISIBLE);  
            	mScanSearched.setVisibility(View.VISIBLE);
            	showMultiDevicesDialog();
	        }else{
	        	String deviceAddress = mLeDeviceListAdapter.getDeviceAddress(0);
	        	if(isHistroyDevice(deviceAddress)){
	        		Log.d(TAG,deviceAddress+" is histroyDevice");
	        		mDeviceAddress = deviceAddress;
	        		mNeedPair = false;
					connectDevice(deviceAddress);
	        	}else{
	        		Log.d(TAG,deviceAddress+" is not histroyDevice");
	        		mDeviceAddress = deviceAddress;
	        		mNeedPair = true;
					connectDevice(deviceAddress);
	        	}
	        }
        }
    }
    
	public void onBtnScan(View view){
		if(isConnect()){
			if(mNeedPair){

			}else{
				//mConnectSuccessText = getResources().getString(R.string.main_connect_state_success);
				//disConnectCurrenDevice();				
			}
		}else{
			if(mScanning){
                scanLeDevice(false);
			}else{
		    	mLeDeviceListAdapter.clear();
		    	scanLeDevice(true);
			}   
		}
	}	  
	
	public void disconnectDevice(View view){
		if(isConnect() && mNeedPair == false){
			mDisconnectClickCount++;
			if(mDisconnectClickCount == 1){
				mConnectStateBar.setText(R.string.main_connect_state_click_again_to_disconnect);
				//mConnectStateLogo.setVisibility(View.GONE);
			}else if(mDisconnectClickCount == 2){
				mConnectSuccessText = getResources().getString(R.string.main_connect_state_success);
				//mConnectLogoVisible = View.GONE;
				//disConnectCurrenDevice();
				sendData(new byte[] {(byte) 0xFD});
				mConnectStateBar.setText(R.string.main_connect_state_closing_device);
		        mConnectedFlag.setBackgroundResource(R.drawable.index_foot_disconnected);
		        mConnectedFlag.setText("");
	            mProgressBar.setVisibility(View.VISIBLE);
	            mScanSearched.setVisibility(View.INVISIBLE);
				//mDisconnectClickCount = 0;
			}
		}
		Log.d(TAG, "mDisconnectClickCount = " + mDisconnectClickCount);
	}
	
	private boolean isHistroyDevice(String address){
        mHistoryAddress = readBluetoothMACList(); /* montague 20150103 */		
		if(mHistoryAddress != null){
			int size = mHistoryAddress.size();
			for(int i=0; i<size ;i++){
				if(mHistoryAddress.get(i).equals(address)){
					return true;
				}			
			}
		}
		return false;
	}
	
    @Override
    protected void updatePairStatus(final boolean status) {
    	super.updatePairStatus(status);

        Log.d(TAG,"PairStatus:" +status);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

			if(mNeedPair){
				if(status){ 					
					BluetoothMACListAdd(mDeviceAddress);
					mNeedPair = false;
					//Toast.makeText(MainActivity.this, getString(R.string.msg_device_confirm_pair), 0).show(); 
					updateDeviceStatus();		
					/*force update firmware:wangfei added begin*/
					startForceUpdateFirmware();
					/*force update firmware:wangfei added end*/
				}else{
					disConnectCurrenDevice();
					Toast.makeText(MainActivity.this, 
							getString(R.string.msg_device_cancel_pair), 0).show();
					updateDeviceStatus();							
				}
			}

            }
        });
    }   	
        
	@Override
    protected void updateBatteryLevel(int level){
		Log.d(TAG,"updateBatteryLevel  level="+level);
		config.BATTERY_LEVEL = level;
		if(config.BATTERY_LEVEL == config.BATTERY_EXTREMELY_LOW_EXLEVLE){
			mbattLevel.setVisibility(View.VISIBLE);
			mbattLevel.setImageResource(R.drawable.battery_level_extremely_low);
		} else if(config.BATTERY_LEVEL == config.BATTERY_LOW_LEVLE) {
			mbattLevel.setVisibility(View.VISIBLE);
			mbattLevel.setImageResource(R.drawable.battery_level_low);			
		} else if(config.BATTERY_LEVEL == config.BATTERY_HIGH_LEVLE) {
			mbattLevel.setVisibility(View.VISIBLE);
			mbattLevel.setImageResource(R.drawable.battery_level_high);			
		} else {
			mbattLevel.setVisibility(View.GONE);		
		}
    }       

	private void disConnectCurrenDevice(){
        if(mBluetoothLeService != null){
		    removeGattServices(mBluetoothLeService.getSupportedGattServices());
			
		    mBluetoothLeService.disconnect();			
		}

		mDeviceAddress = null;
	}
	
	
	private void entryActivity(Class<?> classname){
	    //if(isConnect() && !mNeedPair){
	    	
	    	sendThread.sendData(mStop);
			Intent intent = new Intent(this, classname);
			startActivityForResult(intent, REQ_DEVICE_ACT);	
		//}else if(mNeedPair){
		//	Toast.makeText(this, getString(R.string.msg_please_pair), 0).show();			
		//}else{
		//	Toast.makeText(this, getString(R.string.msg_please_connect), 0).show();			
		//}	
	}
	
	private void stopDeviceActivity() {
		finishActivity(REQ_DEVICE_ACT);
	}
	
	public void onBtnEnterSet(View view){
		entryActivity(SetMode.class);	
	}
	
	public void onEnterRemoteMode(View view){
		/*any chat:wangfei added begin*/
		if(HAVE_ANYCHAT) {
			entryActivity(RemoteModeMain.class);	
		} else {
			entryActivity(VoicectrlYuanchengMode.class);
		}
		/*any chat:wangfei added end*/
	}
	
	public void onEnterBasicMode(View view) {
		if(getBoolData(BASIC_MODE_VOICE_TYPE)) {
			entryActivity(BasicVoiceMode.class);
		} else {
			entryActivity(BasicMode.class);
		}
	}
	
	public void onEnterClassicMode(View view) {
		entryActivity(ClassicMode.class);
	}

	public void onEnterEntertainmentMode(View view){
		showTipsWindow(view, mEntertainmentMode);
	}
	
	public void onEnterPersonalMode(View view){
		showTipsWindow(view, mPersonalMode);
	}
	
	public void onEnterShopping(View view){
		startActivity(new Intent(this, ShoppingActivity.class));
	}
	
	private void startOpenBTActivity() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}
	
	/*wangfei added begin*/	
    boolean isAsrFileExist(){
    	for(int i = 0;i<BasicVoiceMode.mAsrFileName.length; i++){
    		String filename = BasicVoiceMode.ASR_PATH + Aitalkmode.mAsrFileName[i];
    		File file = new File(filename);
    		Log.d(TAG, filename);
    	   	if(!file.exists()){
        		Log.e(TAG, "not exists");
        		return false;
        	}
    	}
 
    	return true;
    }
    
    private void CopyAssets(String assetDir, String dir) {
    	  String[] files;
    	  try {
    	   files = this.getResources().getAssets().list(assetDir);
    	  } catch (IOException e1) {
    	   return;
    	  }
    	  File mWorkingPath = new File(dir);
    	  // if this directory does not exists, make one.
    	  if (!mWorkingPath.exists()) {
    	   if (!mWorkingPath.mkdirs()) {
    	    Log.e("--CopyAssets--", "cannot create directory.");
    	   }
    	  }
    	  for (int i = 0; i < files.length; i++) {
    	   try {
    	    String fileName = files[i];
    	    /*
    	    // we make sure file name not contains '.' to be a folder.
    	    if (!fileName.contains(".")) {
    	     if (0 == assetDir.length()) {
    	      CopyAssets(fileName, dir + fileName + "/");
    	     } else {
    	      CopyAssets(assetDir + "/" + fileName, dir + fileName
    	        + "/");
    	     }
    	     continue;
    	    }
    	    */
    	    File outFile = new File(mWorkingPath, fileName);
    	    if (outFile.exists())
    	     outFile.delete();
    	    InputStream in = null;
    	    if (0 != assetDir.length())
    	     in = getAssets().open(assetDir + "/" + fileName);
    	    else
    	     in = getAssets().open(fileName);
    	    OutputStream out = new FileOutputStream(outFile);
    	    // Transfer bytes from in to out
    	    byte[] buf = new byte[1024];
    	    int len;
    	    while ((len = in.read(buf)) > 0) {
    	     out.write(buf, 0, len);
    	    }
    	    in.close();
    	    out.close();
    	   } catch (FileNotFoundException e) {
    	    e.printStackTrace();
    	   } catch (IOException e) {
    	    e.printStackTrace();
    	   }
    	 }
    }
    /*wangfei added end*/	
    
    /*force update firmware:wangfei added begin*/
    private void startForceUpdateFirmware() {
    	if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())) {
    		if((BluetoothAdapter.checkBluetoothAddress(mDeviceAddress)) && ForceUpdateFirmware.isTargetDevice(
    				   mBluetoothAdapter.getRemoteDevice(mDeviceAddress).getName())) {
    			mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						ForceUpdateFirmware.getInstance(MainActivity.this).start();
					}
				}, 1000);
    		}
    	}
    }
    
    protected void discoveredService() {
    	if(!mNeedPair) {
    		startForceUpdateFirmware();
    	}
    }
    /*force update firmware:wangfei added end*/
    
    private Dialog mMultiDevicesDialog;
    private void showMultiDevicesDialog() {
    	if(mMultiDevicesDialog == null) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		mMultiDevicesDialog = builder.create();
    	}
    	if(!mMultiDevicesDialog.isShowing()) {
    		mMultiDevicesDialog.show();
    	}
    	
    	mMultiDevicesDialog.setContentView(R.layout.multi_devices_dialog);
    	mMultiDevicesDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				updateDeviceStatus();
			}
		});
    	ListView listView = (ListView) mMultiDevicesDialog.findViewById(R.id.dialog_list);
    	mLeDeviceListAdapter.sortByHistory();
    	listView.setAdapter(mLeDeviceListAdapter);
    	listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mMultiDevicesDialog.dismiss();
        		mDeviceAddress = mLeDeviceListAdapter.getDeviceAddress(position);
        		mNeedPair = !isHistroyDevice(mDeviceAddress);
				connectDevice(mDeviceAddress);
			}
		});
    }
    
    private void dismissMultiDevicesDialog() {
    	if(mMultiDevicesDialog != null && mMultiDevicesDialog.isShowing()) {
    		mMultiDevicesDialog.dismiss();
    	}
    }
    
    private ArcMenu mArcMenu;
    private int mArcHintSize;
    private static final TipsHolder[] mEntertainmentMode = {new TipsHolder(R.drawable.arc_menu_item_1, R.string.music_title, MusicMode.class), 
    	new TipsHolder(R.drawable.arc_menu_item_2, R.string.video_title, VideoMode.class), 
    	new TipsHolder(R.drawable.arc_menu_item_3, R.string.game_title, GameMode.class)};
    private static final TipsHolder[] mPersonalMode = {new TipsHolder(R.drawable.arc_menu_item_1, R.string.dance_title, DanceMode.class), 
    	new TipsHolder(R.drawable.arc_menu_item_2, R.string.handdraw_title, CustomDrawMode.class), 
    	new TipsHolder(R.drawable.arc_menu_item_3, R.string.sing_title, SingMode.class)};
    
    private static class TipsHolder {
    	int itemDrawable;
    	int itemTitle;
    	Class<?> enterActivity;
    	
    	public TipsHolder(int itemDrawable, int itemTitle, Class<?> enterActivity) {
    		this.itemDrawable = itemDrawable;
    		this.itemTitle = itemTitle;
    		this.enterActivity = enterActivity;
    	}
    }
    
    private WindowManager.LayoutParams getTipsLayoutParams(View view) {
		int[] location = new int[2];
		view.getLocationOnScreen(location);
		
		WindowManager.LayoutParams params = new WindowManager.LayoutParams();
		params.type = WindowManager.LayoutParams.TYPE_APPLICATION;
		params.dimAmount = 0.5f;
		params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		params.format = PixelFormat.TRANSPARENT;
		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = location[0] + view.getWidth() / 2 - mArcHintSize / 2;
		params.y = location[1] + view.getHeight() / 2 + mArcHintSize / 2 - mArcMenu.getSize();
		params.width = WindowManager.LayoutParams.WRAP_CONTENT;
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;
		params.setTitle("ArcMenu");
		
		return params;
    }
    
    private void showTipsWindow(View view, TipsHolder[] items) {
    	final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    	
		if(mArcMenu == null) {
			mArcMenu = new ArcMenu(getApplicationContext());
			//mArcHintSize = getResources().getDimensionPixelSize(R.dimen.arc_menu_child_size);
			mArcHintSize = view.getWidth() < view.getHeight() ? view.getWidth() : view.getHeight();
			mArcMenu.setHintViewSize(mArcHintSize);
			mArcMenu.setOnCallbackListener(new ArcMenu.OnCallbackListener() {
				@Override
				public void onDismiss() {
					wm.removeView(mArcMenu);
				}
				@Override
				public void onClick(View v) {
					Class<?> activity = (Class<?>) v.getTag();
					entryActivity(activity);
				}
			});
		}
		initArcMenu(mArcMenu, items);
		
		wm.addView(mArcMenu, getTipsLayoutParams(view));
    }
    
    private void initArcMenu(ArcMenu menu, TipsHolder[] items) {
    	menu.clearItems();
        final int itemCount = items.length;
        for (int i = 0; i < itemCount; i++) {
        	Button item = (Button) getLayoutInflater().inflate(R.layout.arc_menu_item, null);
            item.setBackgroundResource(items[i].itemDrawable);
            item.setText(items[i].itemTitle);
            item.setTag(items[i].enterActivity);

            menu.addItem(item);
        }
    }
    
    private static final String SHOPPING_URL = "http://115.29.197.230:8888/channel?channel=";
    private static final String SHOPPING_CHANNEL = "test";
    private AsyncHttpClient mHttpClent = new AsyncHttpClient();
    private TextHttpResponseHandler mHttpRspHandler = new TextHttpResponseHandler() {
		@Override
		public void onSuccess(int result, Header[] mHeader, String data) {
			if(result == HttpStatus.SC_OK) {
				JSONObject object = parseJSONData(data); 
				if(object != null && object.getIntValue("open") == 1) {
					mShoppingLayout.setVisibility(View.VISIBLE);
				}
			}
		}
		
		@Override
		public void onFailure(int result, Header[] mHeader, String data, Throwable throable) {
			
		}
	};
	
    private void postShopping() {
    	mHttpClent.get(SHOPPING_URL + SHOPPING_CHANNEL, mHttpRspHandler);
    }
    
	public static JSONObject parseJSONData(String json) {
		if(json == null) {
			return null;
		}
		JSONObject localJSONObject = JSON.parseObject(json);
		if(localJSONObject == null) {
			return null;
		}
		JSONObject localJSONObject2 = localJSONObject.getJSONObject("data");
       return localJSONObject2;
	}	

}
