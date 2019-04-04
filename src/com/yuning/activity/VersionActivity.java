/**************************************************************************************************
  Filename:       FileActivity.java
  Revised:        $Date: 2013-08-30 12:02:37 +0200 (fr, 30 aug 2013) $
  Revision:       $Revision: 27470 $

  Copyright (c) 2013 - 2014 Texas Instruments Incorporated

  All rights reserved not granted herein.
  Limited License. 

  Texas Instruments Incorporated grants a world-wide, royalty-free,
  non-exclusive license under copyrights and patents it now or hereafter
  owns or controls to make, have made, use, import, offer to sell and sell ("Utilize")
  this software subject to the terms herein.  With respect to the foregoing patent
  license, such license is granted  solely to the extent that any such patent is necessary
  to Utilize the software alone.  The patent license shall not apply to any combinations which
  include this software, other than combinations with devices manufactured by or for TI (�TI Devices�). 
  No hardware patent is licensed hereunder.

  Redistributions must preserve existing copyright notices and reproduce this license (including the
  above copyright notice and the disclaimer and (if applicable) source code license limitations below)
  in the documentation and/or other materials provided with the distribution

  Redistribution and use in binary form, without modification, are permitted provided that the following
  conditions are met:

    * No reverse engineering, decompilation, or disassembly of this software is permitted with respect to any
      software provided in binary form.
    * any redistribution and use are licensed by TI for use only with TI Devices.
    * Nothing shall obligate TI to provide you with source code for the software licensed and provided to you in object code.

  If software source code is provided to you, modification and redistribution of the source code are permitted
  provided that the following conditions are met:

    * any redistribution and use of the source code, including any resulting derivative works, are licensed by
      TI for use only with TI Devices.
    * any redistribution and use of any object code compiled from the source code and any resulting derivative
      works, are licensed by TI for use only with TI Devices.

  Neither the name of Texas Instruments Incorporated nor the names of its suppliers may be used to endorse or
  promote products derived from this software without specific prior written permission.

  DISCLAIMER.

  THIS SOFTWARE IS PROVIDED BY TI AND TI�S LICENSORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
  BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL TI AND TI�S LICENSORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.


 **************************************************************************************************/
package com.yuning.activity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.umeng.analytics.MobclickAgent;
import com.yuning.Service.BluetoothLeService;
import com.yuning.lovercommon.R;
import com.yuning.config.GattInfo;
import com.yuning.util.Conversion;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class VersionActivity extends Activity {
  public final static String EXTRA_FILENAME = "ti.android.ble.devicemonitor.FILENAME";

  private static final String TAG = "FileActivity";
  
	  // BLE
  private BluetoothGattService mOadService;
  private List<BluetoothGattCharacteristic> mCharListOad;
  private BluetoothGattCharacteristic mCharIdentify = null;
  private BluetoothLeService mLeService;
  private IntentFilter mIntentFilter;
  private boolean mServiceOk = false;
  // Programming parameters
  private static final int GATT_WRITE_TIMEOUT = 250; // Milliseconds
  
  private TextView mAppVersion;
  private TextView mFWVersion;
  private ImageView backIcon;
  

  public VersionActivity() {
    Log.i(TAG, "construct");
    mLeService = BluetoothLeService.getInstance();    
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_version);
    mAppVersion = (TextView) findViewById(R.id.app_version);
    mFWVersion = (TextView) findViewById(R.id.fw_version);
    mFWVersion.setText(R.string.set_mode_fw_version_unknown);
    backIcon = (ImageView)findViewById(R.id.back_icon);
	backIcon.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			finish();
			return;
		}
	});
    
    PackageManager pm = this.getPackageManager();
    PackageInfo pi;
	try {
		pi = pm.getPackageInfo(this.getPackageName(), 0);
	    String version = pi.versionName;
	    mAppVersion.setText(getResources().getString(R.string.set_mode_app_version) + version);
	} catch (NameNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

  	initIntentFilter();
  }
	
  @Override 
  protected void onResume()
  {
    super.onResume();
	MobclickAgent.onPageStart( TAG );
	MobclickAgent.onResume(this);    

	
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
        Toast.makeText(this, R.string.fwupdate_oad_service_initialisationfailed, Toast.LENGTH_LONG).show();    	
    	finish();
    	return;
    }  
    
    
    if (mServiceOk) {
    	registerReceiver(mGattUpdateReceiver, mIntentFilter);
      // Read target image info
      getTargetImageInfo();
  		// Connection interval is too low by default
	  	//setConnectionParameters();
    } else {
      Toast.makeText(this, R.string.fwupdate_oad_service_initialisationfailed, Toast.LENGTH_LONG).show();
    }
     
  }

  @Override
  protected void onPause() {
  	super.onPause();
	MobclickAgent.onPageEnd( TAG );
	MobclickAgent.onPause(this);  	
  	if(mServiceOk){
  	  	unregisterReceiver(mGattUpdateReceiver);
  	    mServiceOk = false;
  	}
  	
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
	    if (!ok)
	      Toast.makeText(this, R.string.fwupdate_get_target_info_failed, Toast.LENGTH_LONG).show();
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
  
  private void checkOad() {
		List<BluetoothGattService> serviceList = mLeService.getSupportedGattServices();	  
		mOadService = null;
		for (int i = 0; i < serviceList.size()
		    && (mOadService == null); i++) {
			BluetoothGattService srv = serviceList.get(i);
			if (srv.getUuid().equals(GattInfo.OAD_SERVICE_UUID)) {
				mOadService = srv;
			}			
		}
  }
  
  private void initIntentFilter() {
	  	mIntentFilter = new IntentFilter();
	  	mIntentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
	  	mIntentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
	  }
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
		      mFWVersion.setText(getResources().getString(R.string.set_mode_fw_version) + (int)(version>>1));
		      
		    }
		    
			} 
		}
	};

  @Override
  public void onDestroy() {
    super.onDestroy();
  }




}
