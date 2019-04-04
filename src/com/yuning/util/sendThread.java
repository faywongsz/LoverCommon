package com.yuning.util;

import com.yuning.activity.BaseActivity;

import android.os.SystemClock;
import android.util.Log;


public class sendThread extends Thread{
	private static final String TAG = "sendThread";
	private static boolean havaDate = false;
	//private static boolean isSending = false;
	private static byte[] databuff;
	private static boolean isThreadRuning = false;
	private static final int SEND_TIME_OUT_COUNT = 5;
	private static int send_error_count = 0;
	long lastUpdateTime = 0;
    private static class SingletonHolder {  
        static final sendThread uniqueInstance = new sendThread();  

    }  
    
    public static sendThread getInstance() {  
        return SingletonHolder.uniqueInstance;  
    } 
    
    public void startThread(){
		if(!isThreadRuning){
			start();
            Log.d(TAG, "sendThread start run");
		}
    }
    
    private sendThread() { 
    	databuff = null;
    	havaDate = false;
    }  
  
    @Override
    public void run() {
    	isThreadRuning = true;
        while(true){
           if(havaDate && databuff != null){  
        	   byte[] curdatabuff = databuff;        	   
			   databuff = null;
        	   havaDate = false;   
        	   
        	   if(BaseActivity.sendData(curdatabuff)){
				  send_error_count = 0;
				  Log.d(TAG, "sendmethed OK"); 
			   }else{			  
            	  send_error_count ++;
				  Log.d(TAG, "sendmethed fail count = " + send_error_count); 
				  if(send_error_count >= SEND_TIME_OUT_COUNT){
				  	   // reset data
                	   send_error_count = 0;
                	   databuff = null;
                	   havaDate = false; 
     				  Log.e(TAG, "sendmethed fail !!!!");                 	   
				  }else if(havaDate == false){
					  havaDate = true;
					  databuff = curdatabuff;
				  }
			   }
           }else{
                
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			
           }

        }
 
    }
    
    
	public static boolean  sendData(byte bs[]){	


		if(havaDate){
            Log.d(TAG, "data removed:" +bs);
			databuff = bs;
		}
		
		havaDate = true;		
		databuff = bs;
		return true;
	}
		
}
