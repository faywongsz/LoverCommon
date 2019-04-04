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

package com.yuning.Service;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import com.yuning.activity.MainActivity;
import com.yuning.lovercommon.R;
import com.yuning.util.Conversion;
import com.yuning.config.GattInfo;
import com.yuning.config.config;
import com.yuning.config.config.bleUUID;



/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
	private static BluetoothLeService mThis = null;
	private volatile boolean mBusy = false; // Write/read pending response	
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int OPTION_TIMEOUT = 2000;

    private AlertDialog.Builder mDialogBuilder = null;
    private Dialog mDialog = null;
    
    public final static String ACTION_GATT_CONNECTED =
            "com.yuning.lovercommon.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.yuning.lovercommon.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.yuning.lovercommon.ACTION_GATT_SERVICES_DISCOVERED";
    
    public final static String ACTION_DATA_READ = "com.yuning.lovercommon.ACTION_DATA_READ";
	public final static String ACTION_DATA_NOTIFY = "com.yuning.lovercommon.ACTION_DATA_NOTIFY";
	public final static String ACTION_DATA_WRITE = "com.yuning.lovercommon.ACTION_DATA_WRITE";
	
    public final static String EXTRA_DATA = "com.yuning.lovercommon.EXTRA_DATA";
	public final static String EXTRA_UUID = "com.yuning.lovercommon.EXTRA_UUID";
	public final static String EXTRA_STATUS = "com.yuning.lovercommon.EXTRA_STATUS";
	public final static String EXTRA_ADDRESS = "com.yuning.lovercommon.EXTRA_ADDRESS";
    
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
						close();
						break;
				}
			}
		}
    	
    };
	/*reset bt connect:wangfei added end*/
	
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
               
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                System.out.println("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
    		broadcastUpdate(ACTION_DATA_READ, characteristic, status); 
			Log.d(TAG, "onCharacteristicRead rsp");	    		
			optionResponse();			
        }
        
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
		    BluetoothGattCharacteristic characteristic, int status) {
			broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
			Log.d(TAG, "onCharacteristicWrite rsp");				
			optionResponse();
		}
		
        @Override
        public void onReadRemoteRssi(android.bluetooth.BluetoothGatt gatt, int rssi, int status){
        	Log.d(TAG, "current Rssi=" + rssi + "status=" + status);
			optionResponse();
        }
       
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_NOTIFY, characteristic,
				    BluetoothGatt.GATT_SUCCESS);    
			Log.d(TAG, "onCharacteristicChanged rsp\n mConnectionState=" + mConnectionState);				
			optionResponse();	

			if(mConnectionState == STATE_CONNECTED){
				if(characteristic.getUuid().toString().equals(bleUUID.BATTERY_CHAR_UUID)){
			        final byte[] data = characteristic.getValue();	        
			        Log.d(TAG, "battery level:" + data[0]);
			        Handler handler = new Handler(Looper.getMainLooper()); 
			        handler.post(new Runnable() {
						@Override
						public void run() {
							showLowBatteryDialog(data[0]);
						}
					});
				}
			}
/* montague 20150103 begin */
			/*
			if(mConnectionState == STATE_CONNECTED){
				if(characteristic.getUuid().toString().equals(bleUUID.BATTERY_CHAR_UUID)){
			        final byte[] data = characteristic.getValue();	        
			        Log.d(TAG, "battery level:" + data[0]);
			        if(data[0] == BATTERY_LOW_LEVEL){
				        Handler handler = new Handler(Looper.getMainLooper()); 
				        handler .post(new Runnable() { 
				        	@Override public void run() { 
				        		feedbackDialog();
				        		} });

			        }
				}
			}*/
			//if(characteristic.getUuid().toString().equals(bleUUID.PAIR_CHAR_UUID)){
		    //    final byte[] data = characteristic.getValue();	        
		    //    Log.d(TAG, "key status" + data[0]);
			//}
/* montague 20150103 end */
			
        }
        
		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
		    BluetoothGattDescriptor descriptor, int status) {
			Log.d(TAG, "onDescriptorRead rsp");				
			optionResponse();
		}        
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
		    BluetoothGattDescriptor descriptor, int status) {
			Log.i(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
			optionResponse();
		}		
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);      
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml

            // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            //intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        }

        sendBroadcast(intent);
         
    }

	private void broadcastUpdate(final String action,
		    final BluetoothGattCharacteristic characteristic, final int status) {
			final Intent intent = new Intent(action);
			intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
			intent.putExtra(EXTRA_DATA, characteristic.getValue());
			intent.putExtra(EXTRA_STATUS, status);
			sendBroadcast(intent);
		}
	
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    /*reset bt connect:wangfei added begin*/
    @Override
	public void onCreate() {
		super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	/*reset bt connect:wangfei added end*/

	@Override
    public IBinder onBind(Intent intent) {
        MainActivity.isServiceBind = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        MainActivity.isServiceBind = false;
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
		mThis = this;    	
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        System.out.println("device.getBondState=="+device.getBondState());
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		Log.d(TAG, "readCharacteristic begin");
		if (!checkGatt())
			return;
		Log.d(TAG, "checkGatt success");
		 
	    handleOptionTimeOut(OPTION_TIMEOUT);		
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void readRemoteRssi() {
		Log.d(TAG, "readRemoteRssi begin");
		if (!checkGatt())
			return;
		Log.d(TAG, "checkGatt success");
		
	    handleOptionTimeOut(OPTION_TIMEOUT);		
        mBluetoothGatt.readRemoteRssi();
    }
    
   
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    /**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *          Characteristic to act on.
	 * @param enabled
	 *          If true, enable notification. False otherwise.
	 */
	public boolean setCharacteristicNotification(
	    BluetoothGattCharacteristic characteristic, boolean enable) {
		
		Log.d(TAG, "setNotification begin");
		if (!checkGatt())
			return false;
		Log.d(TAG, "checkGatt success");
		boolean ok = false;
		if (mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {

			BluetoothGattDescriptor clientConfig = characteristic
			    .getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
			Log.d(TAG, "clientConfig=" + clientConfig);	
			
			if (clientConfig != null) {

				if (enable) {

					ok = clientConfig
					    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					 Log.i(TAG, "Enable ok?:" + ok);					
					
				} else {
					// Log.i(TAG, "Disable notification: " +
					// characteristic.getUuid().toString());
					ok = clientConfig
					    .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
					 Log.i(TAG, "Disable ok?:" + ok);						
				}

				if (ok) {
				    handleOptionTimeOut(OPTION_TIMEOUT);					
					ok = mBluetoothGatt.writeDescriptor(clientConfig);
					// Log.i(TAG, "writeDescriptor: " +
					// characteristic.getUuid().toString());					
				}
			}
		}
		 Log.i(TAG, "end  result ok?:" + ok);	
		return ok;
	}

	public boolean isNotificationEnabled(
	    BluetoothGattCharacteristic characteristic) {
		if (!checkGatt())
			return false;

		BluetoothGattDescriptor clientConfig = characteristic
		    .getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
		if (clientConfig == null)
			return false;

		return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
	}    
	
	private boolean checkGatt() {
		if (mBluetoothAdapter == null) {
			 Log.w(TAG, "BluetoothAdapter not initialized");
			return false;
		}
		if (mBluetoothGatt == null) {
			 Log.w(TAG, "BluetoothGatt not initialized");
			return false;
		}

		if (mBusy) {
			 Log.w(TAG, "LeService busy");
			return false;
		}
		return true;

	}	
	
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		Log.d(TAG, "writeCharacteristic begin");		
		if (!checkGatt())
			return false;
	    handleOptionTimeOut(OPTION_TIMEOUT);  
	    Log.d(TAG, "writeCharacteristic data:" + Conversion.BytetohexString(characteristic.getValue(), false));	
        return mBluetoothGatt.writeCharacteristic(characteristic);		
	}
	
	public boolean writeCharacteristic(
		    BluetoothGattCharacteristic characteristic, byte b) {
		    Log.d(TAG, "writeCharacteristic begin");			
			if (!checkGatt())
				return false;

			byte[] val = new byte[1];
			val[0] = b;
			characteristic.setValue(val);
		    handleOptionTimeOut(OPTION_TIMEOUT);
		    Log.d(TAG, "writeCharacteristic data:" + Conversion.BytetohexString(characteristic.getValue(), false));	
			return mBluetoothGatt.writeCharacteristic(characteristic);
		}
	
	public boolean writeCharacteristic(
		    BluetoothGattCharacteristic characteristic, boolean b) {
		    Log.d(TAG, "writeCharacteristic begin");			
			if (!checkGatt())
				return false;

			byte[] val = new byte[1];

			val[0] = (byte) (b ? 1 : 0);
			characteristic.setValue(val);
		    handleOptionTimeOut(OPTION_TIMEOUT);			
		    Log.d(TAG, "writeCharacteristic data:" + Conversion.BytetohexString(characteristic.getValue(), false));	
			return mBluetoothGatt.writeCharacteristic(characteristic);
		}	
	
	public static BluetoothGatt getBtGatt() {
		return mThis.mBluetoothGatt;
	}

	public static BluetoothManager getBtManager() {
		return mThis.mBluetoothManager;
	}

	public static BluetoothLeService getInstance() {
		return mThis;
	}	

	public boolean waitIdle(int timeout) {
		timeout /= 10;
		while (--timeout > 0) {
			if (mBusy)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			else
				break;
		}

		return timeout > 0;
	}	
	
	
    private Handler mHandler = new Handler();;
    private int mTimeOutCount = 0;
    private final int MAX_TIME_OUT_COUNT = 5;
	public boolean handleOptionTimeOut(int timeout) {
		/* set option statue*/
		mBusy = true;
		
	    return mHandler.postDelayed(new Runnable() {
	        @Override
	        public void run() {
	        	if(mBusy){
	        		
	        		/* reset option statue */
	        		mBusy = false;
	        		mTimeOutCount ++;
	        		
	        		if(mTimeOutCount > MAX_TIME_OUT_COUNT){
		        		Log.e(TAG, "disconnect timeout count");	        			
	        			disconnect();
	        		}
	        		Log.w(TAG, "mTimeOutCount=" + mTimeOutCount);
	        	}
	        }
	    }, timeout);
	}	
	
	
	public boolean optionResponse() {
		mBusy = false;
		mTimeOutCount = 0;
        return true;
	}	

	/* montague 20150103 begin */	
	protected void feedbackDialog() {
		if(mDialogBuilder == null){
			mDialogBuilder = new AlertDialog.Builder(this);
		}

		mDialogBuilder.setTitle(getString(R.string.battery_low_notify));
		mDialogBuilder.setMessage(getString(R.string.battery_low_msg));        
		mDialogBuilder.setNegativeButton(R.string.battery_low_button, new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialoginterface, int i)
			{

			}
		});
		
		if(mDialog == null){
			mDialog = mDialogBuilder.create();
		}
		if(!mDialog.isShowing()){
			mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			mDialog.show();  
		}


    }	
	/* montague 20150103 end */	
	
	private long mLastLowBatteryTime = 0;
	private Dialog mBatteryDialog;
    private void showLowBatteryDialog(int level) {
    	final long time = System.currentTimeMillis();
    	if((level == config.BATTERY_LOW_LEVLE && (time - mLastLowBatteryTime >= 29 * 1000))
    			|| (level == config.BATTERY_EXTREMELY_LOW_EXLEVLE)) {
    		if(mBatteryDialog == null){
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			mBatteryDialog = builder.create();
    			mBatteryDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    		}
    		if(!mBatteryDialog.isShowing()){
    			mBatteryDialog.show();  
    		}

    		mBatteryDialog.setContentView(R.layout.dialog_low_battery);
    		if(level == config.BATTERY_LOW_LEVLE) {
        		((ImageView)mBatteryDialog.findViewById(R.id.dialog_low_battery_image)).setImageResource(R.drawable.battery_level_low);
        		((TextView)mBatteryDialog.findViewById(R.id.dialog_title)).setText(R.string.dialog_battery_low_title);
        		((TextView)mBatteryDialog.findViewById(R.id.dialog_low_battery_message)).setText(R.string.dialog_battery_low_message);
    		} else if(level == config.BATTERY_EXTREMELY_LOW_EXLEVLE) {
        		((ImageView)mBatteryDialog.findViewById(R.id.dialog_low_battery_image)).setImageResource(R.drawable.battery_level_extremely_low);
        		((TextView)mBatteryDialog.findViewById(R.id.dialog_title)).setText(R.string.dialog_battery_extremely_low_title);
        		((TextView)mBatteryDialog.findViewById(R.id.dialog_low_battery_message)).setText(R.string.dialog_battery_extremely_low_message);
    		}
    		
        	mLastLowBatteryTime = time;
        	config.BATTERY_LEVEL = level;
    	}
    	
    }
}
