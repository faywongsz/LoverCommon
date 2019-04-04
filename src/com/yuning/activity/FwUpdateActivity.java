/**************************************************************************************************
  Filename:       FwUpdateActivity.java
  Revised:        $Date: 2013-09-05 05:55:20 +0200 (to, 05 sep 2013) $
  Revision:       $Revision: 27614 $

  Copyright (c) 2013 - 2014 Texas Instruments Incorporated

  All rights reserved not granted herein.
  Limited License. 

  Texas Instruments Incorporated grants a world-wide, royalty-free,
  non-exclusive license under copyrights and patents it now or hereafter
  owns or controls to make, have made, use, import, offer to sell and sell ("Utilize")
  this software subject to the terms herein.  With respect to the foregoing patent
  license, such license is granted  solely to the extent that any such patent is necessary
  to Utilize the software alone.  The patent license shall not apply to any combinations which
  include this software, other than combinations with devices manufactured by or for TI (襎I Devices�. 
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

  THIS SOFTWARE IS PROVIDED BY TI AND TI誗 LICENSORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
  BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL TI AND TI誗 LICENSORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.


 **************************************************************************************************/
package com.yuning.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.umeng.analytics.MobclickAgent;
import com.yuning.Service.BluetoothLeService;
import com.yuning.lovercommon.R;
import com.yuning.config.GattInfo;
import com.yuning.ui.CustomDialog;
import com.yuning.util.Conversion;
import com.yuning.util.ForceUpdateFirmware;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FwUpdateActivity extends Activity {
  public final static String EXTRA_MESSAGE = "com.example.ti.ble.sensortag.MESSAGE";
  // Log
  private static String TAG = FwUpdateActivity.class.getSimpleName();

  // Activity
  //private static final int FILE_ACTIVITY_REQ = 0;
  
  // Programming parameters
  private static final int GATT_WRITE_TIMEOUT = 250; // Milliseconds

  private static final int FILE_BUFFER_SIZE = 0x40000;
  private static final String FW_CUSTOM_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() 
		  + "/fwupdate";
  
  private static final int OAD_BLOCK_SIZE = 16;
  private static final int HAL_FLASH_WORD_SIZE = 4;
  private static final int OAD_BUFFER_SIZE = 2 + OAD_BLOCK_SIZE;
  private static final int OAD_IMG_HDR_SIZE = 8;
	private static final long TIMER_INTERVAL = 1000;
	
	private static final int SEND_INTERVAL = 20; // Milliseconds (make sure this is longer than the connection interval)
	private static final int BLOCKS_PER_CONNECTION = 4; // May sent up to four blocks per connection

  // GUI
  private ProgressBar mProgressBar;
  private Button mBtnLoadC;
  private TextView mTextFirmwareVersion;

  // BLE
  private BluetoothGattService mOadService;
  private List<BluetoothGattCharacteristic> mCharListOad;
  private BluetoothGattCharacteristic mCharIdentify = null;
  private BluetoothGattCharacteristic mCharBlock = null;

  private BluetoothLeService mLeService;

  // Programming
  private final byte[] mFileBuffer = new byte[FILE_BUFFER_SIZE];
  private final byte[] mOadBuffer = new byte[OAD_BUFFER_SIZE];
  private ImgHdr mFileImgHdr = new ImgHdr();
  private ImgHdr mTargImgHdr = new ImgHdr();
  private Timer mTimer = null;
  private ProgInfo mProgInfo = new ProgInfo();
  private TimerTask mTimerTask = null;

  // Housekeeping
  private boolean mServiceOk = false;
  private boolean mProgramming = false;
  private IntentFilter mIntentFilter;
  private ImageView backIcon;
  
  //add by guoliangliang 20150415 begin
  /* sendState
   * 0  success
   * 1  send again
   * 2  time out
   * 3  wait
   * */
  private int mSendState = 0;
  private int sendFailedCount = 0;
  private static final int MAX_SEND_FAILED_COUNT = 10;
  private short mBlockIndex;
  private int mSendTimeOutCount = 0;
  private static final int MAX_SEND_TIME_OUT_COUNT = 10;
  //add by guoliangliang 20150415 end
  
  /*force update firmware:wangfei added begin*/
  private boolean mbForceUpdate = false;
  /*force update firmware:wangfei added end*/
  
  private enum LOADRESULT {
	  SUCCESS,
	  NO_FILE,
	  OLD_VERSION,
	  NOT_PRODUCT
  }

  public FwUpdateActivity() {

    // BLE Gatt Service
    mLeService = BluetoothLeService.getInstance();

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
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate");

    super.onCreate(savedInstanceState);
    // Service information
    
    /*force update firmware:wangfei added begin*/
    if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())) {
    	mbForceUpdate = getIntent().getBooleanExtra("is_force", false);
    }
    /*force update firmware:wangfei added end*/
  
    setContentView(R.layout.activity_fwupdate);

    // Initialize widgets
    mProgressBar = (ProgressBar) findViewById(R.id.pb_progress);
    mProgressBar.setVisibility(View.INVISIBLE);
    mBtnLoadC = (Button) findViewById(R.id.btn_load_c);
    backIcon = (ImageView)findViewById(R.id.title_back);
	backIcon.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			/*force update firmware:wangfei added begin*/
			if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())
					&& mbForceUpdate && mProgramming) {
				Toast.makeText(FwUpdateActivity.this, R.string.prog_ogoing, Toast.LENGTH_LONG).show();
			} else {
				finish();
			}
			/*force update firmware:wangfei added end*/
			return;
		}
	});
	TextView titleName = (TextView) findViewById(R.id.title_name);
	titleName.setText(R.string.set_mode_firmware_update);
	
	mTextFirmwareVersion = (TextView) findViewById(R.id.set_mode_firmware_version);
	mTextFirmwareVersion.setText(getString(R.string.set_mode_firmware_version, "unknown"));
	
    /*force update firmware:wangfei added begin*/
    if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())
    		&& mbForceUpdate) {
    	mBtnLoadC.setText(R.string.force_updating_firmware);
    	((TextView)findViewById(R.id.oad_file_help)).setText(R.string.oad_file_help_force);
    }
    /*force update firmware:wangfei added end*/
    
  	initIntentFilter();
  	
	// GATT database
	Resources res = getResources();
	XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
	new GattInfo(xpp);  	
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy");
    super.onDestroy();
    if (mTimerTask != null)
      mTimerTask.cancel();
    mTimer = null;
  }

  @Override
  public void onBackPressed() {
    Log.d(TAG, "onBackPressed");
    if (mProgramming) {
      Toast.makeText(this, R.string.prog_ogoing, Toast.LENGTH_LONG).show();
    } else
      super.onBackPressed();
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.d(TAG, "onOptionsItemSelected");
    // Handle presses on the action bar items
    switch (item.getItemId()) {
    // Respond to the action bar's Up/Home button
    case android.R.id.home:
      onBackPressed();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
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
          mCharBlock = mCharListOad.get(1);
          mCharBlock.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
          //mCharConnReq = mCharListCc.get(1);
        }    	
    }    
    
    if(mOadService == null){
        Toast.makeText(this, R.string.fwupdate_oad_service_initialisationfailed, Toast.LENGTH_LONG).show();    	
    	finish();
    	return;
    }  
    
    // Sanity check
    mBtnLoadC.setEnabled(mServiceOk);
    
    if (mServiceOk) {
    	registerReceiver(mGattUpdateReceiver, mIntentFilter);
    	
      // Read target image info
      getTargetImageInfo();
      
  		// Connection interval is too low by default
	  	//setConnectionParameters();
    } else {
      Toast.makeText(this, R.string.fwupdate_oad_service_initialisationfailed, Toast.LENGTH_LONG).show();
    }
        
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
  	
  	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
		      /*for(int i=0; i<value.length; i++){
		    	  Log.d(TAG,"value["+ i + "]=" + value[i]);
		      }*/
		      Log.d(TAG,"value = " + Conversion.BytetohexString(value, false));
		      mTargImgHdr.ver = Conversion.buildUint16(value[1], value[0]);
		      mTargImgHdr.imgType = ((mTargImgHdr.ver & 1) == 1) ? 'B' : 'A';
		      mTargImgHdr.productType = Conversion.buildProductType(mTargImgHdr.ver);
		      mTargImgHdr.version = Conversion.buildFWVersion(mTargImgHdr.ver);
		      mTargImgHdr.len = Conversion.buildUint16(value[3], value[2]);
		      //displayImageInfo(mTargImage, mTargImgHdr);
		      /*force update firmware:wangfei added begin*/
				if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())
						&& mbForceUpdate) {
					forceUpdateFirmware();
				} 
		      /*force update firmware:wangfei added end*/
			   mTextFirmwareVersion.setText(getString(R.string.set_mode_firmware_version, mTargImgHdr.getFWVersion()));
			//add by guoliangliang 20150415 begin
		    }else if(uuidStr.equals(mCharBlock.getUuid().toString())){
				mBlockIndex = Conversion.buildUint16(value[1], value[0]);
				if(mProgInfo.iBlocks + 1 == mBlockIndex){
					mSendState = 0;
				}else{
					mSendState = 1;
				}
		    	Log.e(TAG,"blockIndex="+mBlockIndex+"  mSendState="+mSendState);
			//add by guoliangliang 20150415 end
		    }
		    
			} else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
				int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,BluetoothGatt.GATT_SUCCESS);
				if (status != BluetoothGatt.GATT_SUCCESS) {
				    mProgramming = false;
				    /*modify by guoliangliang 20150421 begin*/
				    //Toast.makeText(context, R.string.fwupdate_failed, Toast.LENGTH_SHORT).show();
				    /*android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(context);
				    dialog.setTitle(getString(R.string.fwupdate_failed));
				    dialog.create().show();*/
				    showAlertDialog(R.string.fwupdate_failed, null);
				    /*modify by guoliangliang 20150421 end*/
				}
			} else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				finish();
			}
		}
	};


  private void initIntentFilter() {
  	mIntentFilter = new IntentFilter();
  	mIntentFilter.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
  	mIntentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
  	mIntentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
  }

	//add by guoliangliang 20150415, wait for response
	private void waitForUpdate(int timeOut){
		timeOut /= 10;
		while(--timeOut > 0){
			if(mSendState == 0 || mSendState == 1){
				Log.e(TAG,"waitForUpdate, has response, return");
				return;
			}else{
				try{
					Thread.sleep(5);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
		mSendState = 2;
		Log.e(TAG,"waitForUpdate, did not response, return");
	}

  public void onLoadCustom(View v) {
	/*force update firmware:wangfei added begin*/
	if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())
			&& mbForceUpdate) {
		Toast.makeText(FwUpdateActivity.this, R.string.force_updating_firmware, Toast.LENGTH_LONG).show();
		return;
	}
	/*force update firmware:wangfei added end*/
	
	/*wangfei added begin*/	
	File path = new File(FW_CUSTOM_DIRECTORY);
	File[] files = path.listFiles();
	boolean isAbin = false, isBbin = false;
	for(int i = 0; files != null && i < files.length; i++) {
		if(files[i].isFile()) {
			if(files[i].getName().equals("AppA.bin")) {
				isAbin = true;
			} else if(files[i].getName().equals("AppB.bin")) {
				isBbin = true;
			}
		}
	}
	if(!isAbin || !isBbin) {
		Toast.makeText(this, R.string.oad_file_error, Toast.LENGTH_SHORT).show();
		return;
	}
	/*wangfei added end*/	
	  
	LOADRESULT loadResult = loadFile(FW_CUSTOM_DIRECTORY + "/AppA.bin");
    
    if(loadResult != LOADRESULT.SUCCESS){
        loadResult = loadFile(FW_CUSTOM_DIRECTORY + "/AppB.bin");
    }
    
    if(loadResult == LOADRESULT.SUCCESS){
        if (!mProgramming){
            startProgramming();
        }
    }else if(loadResult == LOADRESULT.NO_FILE){
    	Toast.makeText(this, R.string.oad_file_error, Toast.LENGTH_SHORT).show();
    }else if(loadResult == LOADRESULT.OLD_VERSION){
    	Toast.makeText(this, R.string.oad_file_old_version, Toast.LENGTH_SHORT).show();
    }else if(loadResult == LOADRESULT.NOT_PRODUCT){
    	Toast.makeText(this, R.string.oad_file_not_product, Toast.LENGTH_SHORT).show();
    }
    
  }

  private void startProgramming() {
    Log.d(TAG,"Programming started");
    enableNotification(mCharBlock, true);//add by guoliangliang 20150415
    mProgramming = true;
    mProgressBar.setVisibility(View.VISIBLE);
    updateGui();

    // Prepare image notification
    byte[] buf = new byte[OAD_IMG_HDR_SIZE + 2 + 2];
    buf[0] = Conversion.loUint16(mFileImgHdr.ver);
    buf[1] = Conversion.hiUint16(mFileImgHdr.ver);
    buf[2] = Conversion.loUint16(mFileImgHdr.len);
    buf[3] = Conversion.hiUint16(mFileImgHdr.len);
    System.arraycopy(mFileImgHdr.uid, 0, buf, 4, 4);

    // Send image notification
    mCharIdentify.setValue(buf);
    mLeService.writeCharacteristic(mCharIdentify);

    // Initialize stats
    mProgInfo.reset();

    // Start the programming thread
    new Thread(new OadTask()).start();

    mTimer = new Timer();
    mTimerTask = new ProgTimerTask();
    mTimer.scheduleAtFixedRate(mTimerTask, 0, TIMER_INTERVAL);
  }

  private void stopProgramming() {
	if(mTimer != null){
	    mTimer.cancel();
	    mTimer.purge();		
	}

	if(mTimerTask != null){
	    mTimerTask.cancel();
	    mTimerTask = null;		
	}

    mProgramming = false;
    mProgressBar.setProgress(0);
    updateGui();

    if (mProgInfo.iBlocks == mProgInfo.nBlocks) {
        Log.d(TAG,"Programming complete!");  
        /*modify by guoliangliang 20150421 begin*/
        //Toast.makeText(this, R.string.oad_update_complete, Toast.LENGTH_LONG).show();
        /*android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.oad_update_complete));
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
        dialog.create().show();*/
        showAlertDialog(R.string.oad_update_complete, new CustomDialog.OnClickListener() {
			@Override
			public void onClick(Dialog dialog) {
				dialog.dismiss();
				finish();
			}
        });
        /*modify by guoliangliang 20150421 end*/
    } else {
    	showAlertDialog(R.string.fwupdate_failed, null);
    	Log.d(TAG,"Programming cancelled\n");
    }
  }

  private void updateGui() {
  	if (!mProgramming) {
  		// Idle: program label, enable file selector
  		mProgressBar.setProgress(0);
  		mBtnLoadC.setEnabled(true);
  	}else{
  		mBtnLoadC.setEnabled(false);
  	}
  }

  private LOADRESULT loadFile(String filepath) {
	Log.d(TAG,"filepath:" + filepath);
	
	LOADRESULT fSuccess = LOADRESULT.NO_FILE;

    // Load binary file
    try {
      // Read the file raw into a buffer
    	/*force update firmware:wangfei added begin*/
    	if(ForceUpdateFirmware.isForceUpdateFirmware(getApplicationContext())
    			&& mbForceUpdate) {
  	      InputStream stream = getAssets().open(filepath);;
  	      stream.read(mFileBuffer, 0, mFileBuffer.length);
  	      stream.close();
    	} else {
	      InputStream stream;

	      File f = new File(filepath);
	      stream = new FileInputStream(f);

	      stream.read(mFileBuffer, 0, mFileBuffer.length);
	      stream.close();
    	}
    	/*force update firmware:wangfei added end*/
    } catch (IOException e) {
      // Handle exceptions here
      Log.d(TAG,"File open failed: " + filepath + "\n");
      return LOADRESULT.NO_FILE;
    }

    // Show image info
    mFileImgHdr.ver = Conversion.buildUint16(mFileBuffer[5], mFileBuffer[4]);
    mFileImgHdr.len = Conversion.buildUint16(mFileBuffer[7], mFileBuffer[6]);
    mFileImgHdr.imgType = ((mFileImgHdr.ver & 1) == 1) ? 'B' : 'A';
    mFileImgHdr.productType = Conversion.buildProductType(mFileImgHdr.ver);
    mFileImgHdr.version = Conversion.buildFWVersion(mFileImgHdr.ver);
    System.arraycopy(mFileBuffer, 8, mFileImgHdr.uid, 0, 4);
   // displayImageInfo(mFileImage, mFileImgHdr);

    // Verify image types
    LOADRESULT ready = (mFileImgHdr.imgType != mTargImgHdr.imgType) ? LOADRESULT.SUCCESS : LOADRESULT.NO_FILE;
    Log.d(TAG,"mTargImgHd typer" + mTargImgHdr.imgType);    
    Log.d(TAG,"mFileImgHdr typer" + mFileImgHdr.imgType);      
    //int resid = ready ? R.style.dataStyle1 : R.style.dataStyle2;
   // mFileImage.setTextAppearance(this, resid);

    // Enable programming button only if image types differ
    
    //short fileVersion = (short) (mFileImgHdr.ver>>0x01);
    //short targimgVersion = (short)(mTargImgHdr.ver>>0x01);
    
    Log.i(TAG, "fileVersion version=" + mFileImgHdr.getFWVersion());
    Log.i(TAG, "targimgVersion version=" + mTargImgHdr.getFWVersion());
    
    /*if(fileVersion <= targimgVersion){
    	ready = false;
       Log.i(TAG, "not new version fw");
    }*/
    if(!mFileImgHdr.isNewFWVersion(mTargImgHdr.version)) {
    	ready = LOADRESULT.OLD_VERSION;
        Log.i(TAG, "not new version fw");
    }
    
    if(mFileImgHdr.productType != mTargImgHdr.productType) {
    	ready = LOADRESULT.NOT_PRODUCT;
        Log.i(TAG, "fw product type is wrong");
    }
    
    Log.d(TAG,"ready" + ready);

    // Expected duration
    displayStats();

    // Log
    Log.d(TAG,"Image " + mFileImgHdr.imgType + " selected.\n");  
    Log.d(TAG,(ready == LOADRESULT.SUCCESS) ? "Ready to program device!\n" : "Incompatible image, select alternative!\n");

    updateGui();
    
    fSuccess = ready;
    
    return fSuccess;
  }

  private void displayStats() {
    int byteRate;
    int sec = mProgInfo.iTimeElapsed / 1000;
    if (sec > 0) {
      byteRate = mProgInfo.iBytes / sec;
    } else {
      byteRate = 0;
      return;
    }
    float timeEstimate;

    timeEstimate = ((float)(mFileImgHdr.len *4) / (float)mProgInfo.iBytes) * sec;
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


  /*
   * Called when a notification with the current image info has been received
   */

	private void programBlock() {
		if (!mProgramming)
			return;
  	
		if (mProgInfo.iBlocks < mProgInfo.nBlocks) {
			mProgramming = true;
			String msg = new String();

			// Prepare block
			mOadBuffer[0] = Conversion.loUint16(mProgInfo.iBlocks);
			mOadBuffer[1] = Conversion.hiUint16(mProgInfo.iBlocks);
			System.arraycopy(mFileBuffer, mProgInfo.iBytes, mOadBuffer, 2, OAD_BLOCK_SIZE);

			// Send block
			mCharBlock.setValue(mOadBuffer);
			Log.e(TAG,"send programBlock "+mProgInfo.iBlocks);
			boolean success = mLeService.writeCharacteristic(mCharBlock);
			//modify by guoliangliang 20150415 begin
			mSendState = 3;
			waitForUpdate(250);
			if(mSendState == 2){
				sendFailedCount = 0;
				success = false;
				Log.e(TAG,"update failed");
				mSendTimeOutCount++;
				if(mSendTimeOutCount == MAX_SEND_TIME_OUT_COUNT){
					//stop program
					if(mProgInfo.iBlocks + 1 == mProgInfo.nBlocks){
						mProgInfo.iBlocks++;
						Log.e(TAG,"time out = 10 and index is last,update success");
					}else{
						mProgramming = false;
					}
				}
			}else{
				if(mSendState == 0){
					sendFailedCount = 0;
					Log.d(TAG,"send "+mProgInfo.iBlocks+" success, send next");
					mSendTimeOutCount = 0;
					
					mProgInfo.iBlocks++;
					mProgInfo.iBytes += OAD_BLOCK_SIZE;
	         
					if (!mLeService.waitIdle(GATT_WRITE_TIMEOUT)) {
						mProgramming = false;
						success = false;
						msg = "GATT write timeout\n";
					}
				}else if(mSendState == 1){
					mSendTimeOutCount = 0;
					if(sendFailedCount != MAX_SEND_FAILED_COUNT){
						Log.e(TAG,"send "+mProgInfo.iBlocks+" failed, sendFailedCount="+sendFailedCount+",  send again");
						sendFailedCount++;
					//fix bug,add by guoliangliang 20150429 begin
					}else if(mProgInfo.iBlocks + 1 < mBlockIndex){
						int x = mBlockIndex - mProgInfo.iBlocks;
						mProgInfo.iBlocks += x;
						mProgInfo.iBytes += x * OAD_BLOCK_SIZE;
					//fix bug,add by guoliangliang 20150429 end
					}else{
						Log.e(TAG,"update failed,sendFailedCount="+sendFailedCount);
						sendFailedCount = 0;
						success = false;
						mSendState = 2;
						//fix bug,change by guoliangliang 20150429 begin
						if(mProgInfo.iBlocks > 0){
							mProgInfo.iBlocks--;
							mProgInfo.iBytes -= OAD_BLOCK_SIZE;
						}
						//fix bug,change by guoliangliang 20150429 end
					}
				}
			}

			/*
			if (success) {
				// Update stats
				mProgInfo.iBlocks++;
				mProgInfo.iBytes += OAD_BLOCK_SIZE;
         
				if (!mLeService.waitIdle(GATT_WRITE_TIMEOUT)) {
					mProgramming = false;
					success = false;
					msg = "GATT write timeout\n";
				}
			} else {
				mProgramming = false;
				msg = "GATT writeCharacteristic failed\n";
			}*/
			//modify by guoliangliang 20150415 end
			final String logmsg = msg;
			final boolean finalSucess = success;

			runOnUiThread(new Runnable() {
				public void run() {
					if(finalSucess){
						mProgressBar.setProgress((mProgInfo.iBlocks * 100) / mProgInfo.nBlocks);
					}else{
						Log.d(TAG,logmsg);            		  
					}

				}
			});    	  

		} else {
			mProgramming = false;
		}

		if (!mProgramming) {
			runOnUiThread(new Runnable() {
				public void run() {
					displayStats();
					stopProgramming();
				}
			});
		}
	}
  
	private class OadTask implements Runnable {
		@Override
		public void run() {
			while (mProgramming) {
				try {
					Thread.sleep(SEND_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (int i=0; i<BLOCKS_PER_CONNECTION & mProgramming; i++) {
					programBlock();
				}
				if ((mProgInfo.iBlocks % 100) == 0) {
					// Display statistics each 100th block
					runOnUiThread(new Runnable() {
						public void run() {
							displayStats();
						}
					});
				}
			}
		}
	}

	private class ProgTimerTask extends TimerTask {
    @Override
    public void run() {
      mProgInfo.iTimeElapsed += TIMER_INTERVAL;
    }
  }

  private class ImgHdr {
    short ver;
    short len;
    Character imgType;
    short productType;
    byte[] version;
    byte[] uid = new byte[4];
    
    public String getFWVersion() {
    	return "V" + version[0] + "." + version[1];
    }
    
    public boolean isNewFWVersion(byte[] compareVer) {
    	if(compareVer == null || compareVer.length != 2) {
    		return false;
    	}
    	
    	if(version[0] > compareVer[0]) {
    		return true;
    	} else if(version[0] == compareVer[0]) {
    		if(version[1] > compareVer[1]) {
    			return true;
    		} else {
    			return false;
    		}
    	} else {
    		return false;
    	}
    }
  }

  private class ProgInfo {
    int iBytes = 0; // Number of bytes programmed
    short iBlocks = 0; // Number of blocks programmed
    short nBlocks = 0; // Total number of blocks
    int iTimeElapsed = 0; // Time elapsed in milliseconds

    void reset() {
      iBytes = 0;
      iBlocks = 0;
      iTimeElapsed = 0;
      nBlocks = (short) (mFileImgHdr.len / (OAD_BLOCK_SIZE / HAL_FLASH_WORD_SIZE));
    }
  }
  
  private CustomDialog showAlertDialog(int redId, CustomDialog.OnClickListener listener) {
	  try {
		  CustomDialog dialog = new CustomDialog(this);
		  dialog.setTitle(R.string.dialog_tips);
		  dialog.setMessage(redId);
		  dialog.setYesButton(R.string.yes, listener);
		  return dialog;
	  } catch(Exception e) {
		return null;
	  }
  }
  
  /*force update firmware:wangfei added begin*/
  private void forceUpdateFirmware() {
	  LOADRESULT loadResult = loadFile(ForceUpdateFirmware.ASSET_FIRMWARE_PATH + "/AppA.bin");
	    
	    if(loadResult != LOADRESULT.SUCCESS){
	        loadResult = loadFile(ForceUpdateFirmware.ASSET_FIRMWARE_PATH + "/AppB.bin");
	    }
	    
	    if(loadResult == LOADRESULT.SUCCESS){
	        if (!mProgramming){
	            startProgramming();
	        }
	    }else if(loadResult == LOADRESULT.NO_FILE){
	    	Toast.makeText(this, R.string.oad_file_error, Toast.LENGTH_SHORT).show();
	    }else if(loadResult == LOADRESULT.OLD_VERSION){
	    	Toast.makeText(this, R.string.oad_file_old_version, Toast.LENGTH_SHORT).show();
	    }else if(loadResult == LOADRESULT.NOT_PRODUCT){
	    	Toast.makeText(this, R.string.oad_file_not_product, Toast.LENGTH_SHORT).show();
	    }
  }
  /*force update firmware:wangfei added end*/

}
