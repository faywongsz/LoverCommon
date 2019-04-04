package com.iflytek.asr.AsrService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.yuning.activity.Aitalkmode;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class Asr { 
	private static final String TAG = "Asrnew";
	
	//Message define
	public static final int MSG_START_RECORD      = 0x310;
	public static final int MSG_STOP_RECORD       = 0x311;
	
	public static final int MSG_SPEECH_START      = 0x401;
	public static final int MSG_SPEECH_END        = 0x402;
	public static final int MSG_SPEECH_FLUSH_END  = 0x403;
	public static final int MSG_SPEECH_NO_DETECT  = 0x40f; 
	
	public static final int MSG_RESPONSE_TIMEOUT  = 0x410;
	public static final int MSG_SPEECH_TIMEOUT    = 0x411;
	public static final int MSG_END_BY_USER       = 0x412;
	
	public static final int MSG_HAVE_RESULT       = 0x500;	
	
	public static final int MSG_HAVE_RESTART       = 0x1000;	
	
	
	public static final List<RecognitionResult> mResult = new ArrayList<RecognitionResult>();

	private static Aitalkmode mActivity ;
	public static boolean mNeedStop = true;
	
	private static OnCallback mOnCallback;
	public interface OnCallback {
		public void onCallback();
	}
 
	private static Handler mMsgHandler = new Handler() { 
		@Override
		public void handleMessage(Message msg)
		{ 
			switch (msg.what)   
			{			
			case MSG_START_RECORD:
				AsrRecord.startRecord();
				if(!mNeedStop){
					//Toast.makeText(mActivity, "begin to record...", Toast.LENGTH_SHORT).show();
				}
				break;
			case MSG_STOP_RECORD:
				AsrRecord.stopRecord();
				if(!mNeedStop){

				}				
			
				break;
			case MSG_SPEECH_START:
				
				Log.d(TAG,"MSG_SPEECH_START");
				break;
			case MSG_SPEECH_END:
				Log.d(TAG,"MSG_SPEECH_END");
				break;
			case MSG_SPEECH_FLUSH_END:
				Log.d(TAG,"MSG_SPEECH_FLUSH_END");
				break;				
			case MSG_SPEECH_NO_DETECT:
				Log.d(TAG,"MSG_SPEECH_NO_DETECT");
				break;
			case MSG_RESPONSE_TIMEOUT:

				break;
			case MSG_SPEECH_TIMEOUT:

				break;
			case MSG_END_BY_USER:
				Log.d(TAG,"MSG_END_BY_USER");
				break;	
			case Asr.MSG_HAVE_RESULT:	
				Log.d(TAG,"MSG_HAVE_RESULT ; result size=" + mResult.size());
				if(mActivity != null) mActivity.onResult();
				if(mOnCallback != null) mOnCallback.onCallback();

				break;

			default:
				Log.d(TAG,"unkown  message: " + msg.what);
				break;
			}
		} 
	}; 
    
	
	/**
	 * 瀵拷顬婄拠鍡楀焼娴犺濮熺痪璺ㄢ柤
	 */
	
	private static final ReentrantLock asrRunLock = new ReentrantLock();
	private static final long TIMEOUT_WAIT_QUEUE = 500;  
	public  synchronized static void startRecoThread(Aitalkmode ctx){
		startRecoThread(ctx, null);
	}
	public  synchronized static void startRecoThread(Aitalkmode ctx, OnCallback callback){
		mActivity = ctx;
		mOnCallback = callback;
        class AsrRunThread implements Runnable{

            boolean isAsrRunable = false;

            int nRet = 0;

            public void run()

            {

                     mResult.clear(); 

                     try{

                               Log.i(TAG, "AsrRunThread to start");

                               //1. lock run

                               isAsrRunable = asrRunLock.tryLock(TIMEOUT_WAIT_QUEUE, TimeUnit.MILLISECONDS);

                               if (!isAsrRunable){

                                        Log.e(TAG, "AsrRunThread tryLock  is unavailable");

                                        //Asr.errorCallback( RecognitionResult.ASR_BUSY);

                                        Thread.sleep(100);

                                        return ;

                               }                                   

                               nRet = JniRunTask();

                               if (nRet != 0){

                                        Log.i(TAG, "AsrRunThread Start Error!");      

                                        //Asr.errorCallback( RecognitionResult.ASR_ERROR);

                               }

                               Log.i(TAG, "AsrRunThread run ok ");               

                              

                     }catch (InterruptedException e){

                               Log.e(TAG, "AsrRunThread interrupted");

                    e.printStackTrace();

                     }finally{

                               if (isAsrRunable){

                                        asrRunLock.unlock();

                               }

                               Log.i(TAG, "AsrRunThread run End!");

                     }                          

            }

           

   }

  		
		Thread asrRun = (new Thread(new AsrRunThread()));
		asrRun.setPriority(Thread.MAX_PRIORITY);
		asrRun.start();
	}


	/**
	 * JNI閻ㄥ嚋娴狅絿鐖滅拫鍐暏,閻€劋绨径鍕倞瀵洘鎼搁崣鎴ｇ箖閺夈儳娈戝☉鍫熶紖
	 */	
	public static int onCallMessage(int msgType){
		Message s_msg = mMsgHandler.obtainMessage(msgType);
		mMsgHandler.sendMessageDelayed(s_msg, 0);
		return 0;		
	}	
	
	/**
	 * 瑜版捁鐦戦崚顐㈢穿閹垮孩婀佺紒鎾寸亯閺冩儼鐨熼悽锟芥穱婵嗙摠鐠囧棗鍩嗙紒鎾寸亯閸掞拷List<RecognitionResult>
	 * @return
	 */
	public static int onCallResult(){
		Log.d(TAG,"onCallResult");
		int iResCount = 0;
		int iSlotCount = 0;
		int iItemCount = 0;
		//1. get result
		mResult.clear();
		iResCount = JniGetResCount();
		for (int iRes = 0 ;iRes < iResCount;iRes ++){
			//1.1  Get result count
			int sentenceId = JniGetSentenceId(iRes);
			iSlotCount = JniGetSlotNumber(iRes);
			int confidence = JniGetConfidence(iRes);			
			Log.d(TAG, "onCallResult res:"+ (iRes + 1) + " sentenceId:" + sentenceId 
					+ "  confidence:" + confidence + " SlotCount:"+ iSlotCount );
			RecognitionResult rs = new RecognitionResult(sentenceId,confidence,iSlotCount);
			
			//1.2 Get slot 
			for (int iSlot = 0;iSlot < iSlotCount;iSlot ++){
				iItemCount = JniGetItemNumber(iRes,iSlot);
				if (iItemCount <= 0 ){
					Log.e(TAG,"Error iItemCount < 0");
					continue;
				}
				int itemIds [] = new int[iItemCount];
				String itemTexts [] = new String[iItemCount];
				
				Log.d(TAG,"onCallResult slot:"+ (iSlot + 1) + " iItemCount:" + iItemCount);
				for (int iItem = 0; iItem < iItemCount;iItem ++ ){
					itemIds[iItem] = JniGetItemId(iRes,iSlot,iItem);
					itemTexts[iItem] = JniGetItemText(iRes,iSlot,iItem) ;					
					if (null == itemTexts[iItem]){
						itemTexts[iItem] = ""; 
					}					
					Log.d(TAG,"onCallResult slot item:"+ (iItem + 1) + " itemTexts:"
							+ itemTexts[iItem] +" itemIds " + itemIds[iItem]);
				}	
				rs.AddSlot(iItemCount, itemIds, itemTexts);
			}
			mResult.add(rs);	
		}
		
		//2. Call back to application

		Log.d(TAG,"MSG_HAVE_RESULT");
		Message s_msg = mMsgHandler.obtainMessage(MSG_HAVE_RESULT);
		mMsgHandler.sendMessageDelayed(s_msg, 0);
		return 0;
	}
	
	public static void reStart() {
		//mMsgHandler.sendEmptyMessageDelayed(MSG_HAVE_RESTART, 50);
		//reStart();
   	 if(!mNeedStop){
				 Asr.JniStart("menu");	
   	 }		
	}
	
	/**
	 * 瑜版洟鐓剁痪璺ㄢ柤鐠嬪啰鏁�瑜版挻婀佽ぐ鏇㈢叾閺佺増宓侀弮鍫曪拷閸忋儱绱╅幙锟�
	 * @param buff
	 * @param length
	 * @return
	 */
	
	public static int  appendData(byte[] buff, int length) {
		int ret = 0;
		ret = JniAppendData(buff,length);
		return ret;
	}

	/**
	 * Java native interface code
	 */
	static {
		System.loadLibrary("Aitalk");
	}
	
	public native static int JniGetVersion();
	
	public native static int JniCreate();	
	public native static int JniDestroy();
	public native static int JniStart(String sceneName);
	public native static int JniStop(); 	
	public native static int JniRunTask();
	
	
	public native static int JniGetResCount();
	public native static int JniGetSentenceId(int resIndex);
	public native static int JniGetConfidence(int resIndex);
	public native static int JniGetSlotNumber(int resIndex);
	
	public native static int JniGetItemNumber(int resIndex,int slotIndex);
	public native static int JniGetItemId(int resIndex,int slotIndex,int itemIdex);	
	public native static String JniGetItemText(int resIndex,int slotIndex,int itemIdex);	

	public native static int JniAppendData(byte []data,int length);
	public native static int JniEndData();
	
	public native static int JniBuildGrammar(byte[] xmlText,int length);	
	
	public native static int JniAddLexiconItem(String word,int id);
	public native static int JniBeginLexicon(String lexiconName,boolean isPersonName);
	public native static int JniEndLexicon();
	public native static int JniMakeVoiceTag(String lexiconName,String item,byte[] data,int dataLen);
	public native static int JniSetParam(int paramId,int paramValue);
}
