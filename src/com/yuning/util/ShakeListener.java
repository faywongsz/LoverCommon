package com.yuning.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * 
 * @author pingweiqiang 濡備綍妫�祴鎵嬫満鏄惁鏅冨姩锛�鍦ㄤ竴涓緝鐭殑鏃堕棿闂撮殧姹傚嚭鍔犻�搴︾殑宸�锛�
 *         璺熶竴涓寚瀹氱殑闃堝�姣旇緝锛屽鏋滃樊鍊煎ぇ浜庨槇鍊硷紝鍒欒涓烘槸鎽囨檭鍙戠敓浜嗐�
 * 
 */
 /** 
  *  
  * 涓�釜妫�祴鎵嬫満鎽囨檭鐨勭洃鍚櫒 
  * @author pingweiqiang 
  * 
 */  
 public class ShakeListener implements SensorEventListener { 
  private static final String TAG = "ShakeListener";
  private static double MAXVALUE = 200.0F;  
  //閫熷害闃堝�锛屽綋鎽囨檭閫熷害杈惧埌杩欏�鍚庝骇鐢熶綔鐢� 
  private static final int SPEED_SHRESHOLD = 3300;  
  //涓ゆ妫�祴鐨勬椂闂撮棿闅� 
  private static final long UPTATE_INTERVAL_TIME = 20L; 
  
  private static final int SHAKE_DURATION = 1500; // 鏅冨姩闂撮殧鏃堕棿闃堝�
    
  //浼犳劅鍣ㄧ鐞嗗櫒  
  private SensorManager sensorManager;  
  //浼犳劅鍣� 
  private Sensor sensor;  
  //閲嶅姏鎰熷簲鐩戝惉鍣� 
  private OnShakeListener onShakeListener;  
  //涓婁笅鏂� 
  private Context context;  
  //鎵嬫満涓婁竴涓綅缃椂閲嶅姏鎰熷簲鍧愭爣  
  private float lastX;  
  private float lastY;  
  private float lastZ;  
    
  //涓婃妫�祴鏃堕棿  
 private long lastUpdateTime; 
 
 private long mLastShake;
  
 //鏋勯�鍣� 
  public ShakeListener(Context c) {  
   //鑾峰緱鐩戝惉瀵硅薄  
   context = c;  
   //start();  
  }  
    
  //寮�  
  public void start() {  
   //鑾峰緱浼犳劅鍣ㄧ鐞嗗櫒  
   sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);   
   if(sensorManager != null) {  
    //鑾峰緱閲嶅姏浼犳劅鍣� 
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);  
   }  
   //娉ㄥ唽  
   if(sensor != null) {  
    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);  
  }  
     
  }  
    
  //鍋滄妫�祴  
  public void stop() {  
	  if(sensorManager != null) {  
		  sensorManager.unregisterListener(this);  
	  }
  }  
    
  //鎽囨檭鐩戝惉鎺ュ彛  
  public interface OnShakeListener {  
   public void onShake(int level);  
  }  
    
  //璁剧疆閲嶅姏鎰熷簲鐩戝惉鍣� 
  public void setOnShakeListener(OnShakeListener listener) {  
   onShakeListener = listener;  
  }  
    
    
  //閲嶅姏鎰熷簲鍣ㄦ劅搴旇幏寰楀彉鍖栨暟鎹� 
  public void onSensorChanged(SensorEvent event) {  
    if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {  
        return;  
    }  
  //鐜板湪妫�祴鏃堕棿  
    long currentUpdateTime = SystemClock.elapsedRealtime();  
   //涓ゆ妫�祴鐨勬椂闂撮棿闅� 
    long timeInterval = currentUpdateTime - lastUpdateTime;  
    //Log.i(TAG,"timeInterval=="+timeInterval);
//   //鍒ゆ柇鏄惁杈惧埌浜嗘娴嬫椂闂撮棿闅� 
     if(timeInterval < UPTATE_INTERVAL_TIME) {  
         return;  
     }
     
     //鐜板湪鐨勬椂闂村彉鎴恖ast鏃堕棿  
     lastUpdateTime = currentUpdateTime; 
  
     
    //鑾峰緱x,y,z鍧愭爣  
     float x = event.values[0];  
     float y = event.values[1];  
     float z = event.values[2];  
     
   //鑾峰緱x,y,z鐨勫彉鍖栧�  currentUpdateTime
     float deltaX = x - lastX;  
     float deltaY = y - lastY;  
     float deltaZ = z - lastZ;  
    
  //灏嗙幇鍦ㄧ殑鍧愭爣鍙樻垚last鍧愭爣  
     lastX = x;  
     lastY = y;  
     lastZ = z;  

   //double speed = Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ)/timeInterval * 10000;
   double speed = 10 * (int)Math.sqrt(Math.pow(deltaX, 2.0D) + Math.pow(deltaY, 2.0D) + Math.pow(deltaZ, 2.0D));   
  //杈惧埌閫熷害闃��锛屽彂鍑烘彁绀� 
   //Log.i(TAG,"speed=="+speed);
   
   int i = getDangWei(speed);
   if (speed > MAXVALUE){
	   MAXVALUE = speed;   
   }

   Log.i(TAG,"dangwei = " + i);
   onShakeListener.onShake(i);
   
   /*
   if(speed >= SPEED_SHRESHOLD && (now - mLastShake > SHAKE_DURATION) )  {
	  mLastShake = now;  
     onShakeListener.onShake();
     Log.d("xxxxx","value = " + (speed - SPEED_SHRESHOLD));
   }
   */
   
  
  }  
  

  private int getDangWei(double paramDouble)
  {
    if (paramDouble < 0.5D * MAXVALUE)
    {
      if (paramDouble < 0.2D * MAXVALUE)
      {
        if (paramDouble < 0.1D * MAXVALUE)
          return 0;
        return 10;
      }
      if (paramDouble < 0.3D * MAXVALUE)
        return 15;
      if (paramDouble < 0.4D * MAXVALUE)
        return 20;
      return 25;
    }
    if (paramDouble < 0.7D * MAXVALUE)
    {
      if (paramDouble < 0.6D * MAXVALUE)
        return 25;
      return 30;
    }
    if (paramDouble < 0.9D * MAXVALUE)
    {
      if (paramDouble < 0.8D * MAXVALUE)
        return 35;
      return 40;
    }
    if (paramDouble < MAXVALUE)
      return 45;
    return 50;
  }
  
    
  public void onAccuracyChanged(Sensor sensor, int accuracy) {  
     
 }  
   
 } 
 //[BIRD_COOL_APP_SUPPORT][BIRD_PERSONAL_OF_COOL][娉㈠B闄㈠紑鍙戠殑鐐姛鑳界殑鐗规晥閮ㄥ垎][pingweiqiang][2013.5.17]end
