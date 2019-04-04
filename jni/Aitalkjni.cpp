/*----------------------------------------------+
 |												|
 |	com_iflytek_asr_service_Asr.c 	|
 |												|
 |		Copyright (c) 1999-2009, iFLYTEK Ltd.	|
 |		All rights reserved.					|
 |												|
 +----------------------------------------------*/
#include <stdio.h>
#include <malloc.h>
#include <unistd.h>
#include <string.h>
#include <assert.h>
#include <android/log.h>
#include <jni.h>


#include "Inc/ivESR.h"

#define AITALK_SDK_SN "shanxidawei#CPZMWO8WXPRXXPD4VD3IEWNXHR4SSBXGWLGZVJWA2IZEXXMS"

#define LOGD(text) __android_log_write(ANDROID_LOG_DEBUG,"OmsAsrJni",text)
#define LOGE(text) __android_log_write(ANDROID_LOG_ERROR,"OmsAsrJni",text)


#define  FILE_ASR_LOG "/sdcard/Aitalk3Log.esl"
#define  FILE_GRAMMAR	"/sdcard/menu.xml"
#define  DIR_ASR_RESOURCE "/sdcard/asr/"
#define  DIR_SDCARD "/sdcard/"              //������ʱ�ļ���Ŀ¼
//#define  DIR_SDCARD "/data/data/com.iflytek.asr.AsrService/"              //������ʱ�ļ���Ŀ¼
#define  _DEBUG 	                            //����ʱҪ��
#define  MAX_PATH 256


typedef struct _StJavaAsrClass{
	JNIEnv *env;				
	jclass clazzTts;
	jmethodID onJniMessage;
	jmethodID onJniResult;
}StJavaAsrClass;

static 	TUserOS m_stUserOS = {0};                //�û�ϵͳ�ṹ��
static	ivTResPackDesc m_stResPackDesc = {0};    //��Դ���������ṹ��
static	ivHandle m_hEsrObj = ivNull;             //ʶ��ʵ��
static  FILE *m_pLog = ivNull;                   //��־�ļ�
static StJavaAsrClass m_JavaAsrClass = {0};      //�ص�Java
static char m_strSence[40] = {0};                //��ǰ����


static void JniInit(JNIEnv * env)
{
	jclass classTmp =  env->FindClass("com/iflytek/asr/AsrService/Asr");

	if (NULL == classTmp){
		LOGD("FindClass is null");
		return;
	}

	m_JavaAsrClass.clazzTts = (jclass)env->NewGlobalRef(classTmp);
	m_JavaAsrClass.env = env;

	LOGD("JniInit  into");
	if (NULL != m_JavaAsrClass.clazzTts ){
		m_JavaAsrClass.onJniMessage  = env->GetStaticMethodID(m_JavaAsrClass.clazzTts
			,"onCallMessage","(I)I");
		m_JavaAsrClass.onJniResult  = env->GetStaticMethodID(m_JavaAsrClass.clazzTts
			,"onCallResult","()I");
		
		LOGD("JniInit  ok");
	}
}

static void JniDinit()
{
	if (NULL != m_JavaAsrClass.clazzTts){
		m_JavaAsrClass.env->DeleteGlobalRef(m_JavaAsrClass.clazzTts);
		m_JavaAsrClass.clazzTts = NULL;
	}	
	m_JavaAsrClass.env = NULL;
	m_JavaAsrClass.onJniMessage = 0;
	m_JavaAsrClass.onJniResult = 0;	
	LOGD("JniDInit  ok");

}


static void LogComm(const char *comm,const char *memo,int actionId,int ret)
{
#ifdef _DEBUG
	char msg[256] = {0};
	sprintf(msg,"%s %s act=%d ret=%d",comm,memo,actionId,ret);
	LOGD(msg);
#endif 
}

//1. ���ڷ���ص�
static ivPointer ivCall CBRealloc(ivPointer p,ivSize nSize)
{
	return (ivPointer)realloc(p,nSize);
}

//2. �ڴ��ͷŻص�
static void ivCall CBFree(ivPointer p)
{
	free(p);
	p = NULL;
}

//3. ���ļ��ص�����;Ĭ��Ŀ¼��/sdcard/
static ivHandle ivCall CBOpenFile(ivCStr lpFileName,ivInt enMod,ivInt enType)
{
	
	FILE* pf = NULL;
	char szFileName[MAX_PATH] = {0};
	char szMode[8] = {0};

	//��ݲ�ͬ����,����Դ������ͨ�ļ�
	if(ivResFile == enType){
		sprintf(szFileName,"%s%s",DIR_ASR_RESOURCE,lpFileName);
	}
	else{
//		sprintf(szFileName,"%s%s",DIR_SDCARD,lpFileName);
	}
	
	if(ivModWrite == enMod){
		pf = fopen(szFileName,"wb");
	}
	else{
		pf = fopen(szFileName,"rb");
	}
	if (NULL == pf){
//		LOGD("CBOpenFile open file is null :");
//		LOGD(szFileName);
	}
	
	return (ivHandle)pf;
}

//4. �ر��ļ��ص�
static ivBool ivCall CBCloseFile(ivHandle hFile)
{
	if (NULL == hFile){
		LOGD("CBCloseFile  file is null");
		return 0;
	}
	return (0 == fclose((FILE*)hFile));
}



//5. ���ļ��ص�
static ivBool ivCall CBReadFile(ivHandle hFile,ivPByte pBuffer,ivUInt32 iPos,ivSize nSize)
{
	FILE* pf = (FILE*)hFile;
	ivSize nRead = 0;

	if (NULL == pf){
		LOGD("CBReadFile  file is null");
		return 0;
	}
	
	if(FILE_POS_CURRENT != iPos){
		fseek(pf,iPos,SEEK_SET);
	}

	nRead = fread(pBuffer,1,nSize,pf);
	
	return nRead == nSize;
}

//6. д�ļ��ص�
static ivBool ivCall CBWriteFile(ivHandle hFile,ivPCByte pBuffer,ivUInt32 iPos,ivSize nSize)
{
	FILE* pf = (FILE*)hFile;
	ivSize nWrite = 0;

	if (NULL == pf){
		LOGD("CBWriteFile  file is null");
		return 0;
	}
	
	if(FILE_POS_CURRENT != iPos){
		fseek(pf,iPos,SEEK_SET);
	}
	nWrite = fwrite(pBuffer,1,nSize,pf);
	return nWrite == nSize;
}



//7.1. ��־������Ϣ
static void OnMsgLog(ivUInt32 wParam, ivCPointer lParam)
{
	int ret = 0;
	if (NULL != m_pLog){
		ret = fwrite(lParam,wParam, 1, m_pLog);
		fflush(m_pLog);
	}
}

// 7.2  ������н��ص�Java
static int OnMsgCallJava(int msgType)
{
	LogComm("OnMsgCallJava","",0,msgType);

	if (m_JavaAsrClass.env && m_JavaAsrClass.clazzTts
		&& m_JavaAsrClass.onJniMessage){
		m_JavaAsrClass.env->CallStaticIntMethod(m_JavaAsrClass.clazzTts
			,m_JavaAsrClass.onJniMessage,(jint)msgType);
	}else{
		LOGD("OnMsgCallJava  but java method null");
	}
	
	return 0;
}

static int m_nResultCount = 0;
static PCEsrResult m_pResult = NULL;

//7.3  ��ʶ������Ϣ����
static void OnMsgResult(ivUInt32 wParam, ivCPointer lParam)
{


	LogComm("OnMsgResult","",  0, wParam);
	PCEsrResult pResult = (PCEsrResult)lParam;
	ivUInt32	nBest = (ivUInt32)wParam;
	
	m_pResult = pResult;
	m_nResultCount = nBest;

	if (m_JavaAsrClass.env && m_JavaAsrClass.clazzTts
		&& m_JavaAsrClass.onJniResult){
	}else{
		LOGD("OnMsgResult  but java method null");
		return ;
	}
	m_JavaAsrClass.env->CallStaticIntMethod(m_JavaAsrClass.clazzTts
		,m_JavaAsrClass.onJniResult,(jint)nBest);

}


//7. ͳһ��Ϣ�ص�
static ivStatus ivCall CBMsgProc(ivHandle hObj,ivUInt32 uMsg,ivUInt32 wParam,ivCPointer lParam)
{
	int  bOK = 0;

	switch(uMsg)
	{
	case ivMsg_ToSleep:
		//ǿ��ִ��sleep
		//LogComm("CBMsgProc---sleep",wParam,0);
		usleep(wParam);
		break;
	case ivMsg_Create:
		//Create��Ϣ�������ٽ���
		//LOGD("CBMsgProc---InitializeCriticalSection......");
		//InitializeCriticalSection(&g_tCriticalSection);
		break;
	case ivMsg_Destroy:
		// Destroy��Ϣ������ٽ���
		//DeleteCriticalSection(&g_tCriticalSection);
		//LOGD("CBMsgProc---DeleteCriticalSection......");
		break;
	case ivMsg_ToEnterCriticalSection:
		// EnterCriticalSection��Ϣ�������ٽ���
		//EnterCriticalSection(&g_tCriticalSection);
		//LOGD("CBMsgProc---EnterCriticalSection......");
		break;
	case ivMsg_ToLeaveCriticalSection:
		// ExitCriticalSection��Ϣ���˳��ٽ���
		//LeaveCriticalSection(&g_tCriticalSection);
		//LOGD("CBMsgProc---LeaveCriticalSection......");
		break;
	case ivMsg_SpeechStart:
		// SpeechStart��Ϣ����⵽������Ϣ����
		OnMsgCallJava(ivMsg_SpeechStart);
		break;
	case ivMsg_SpeechEnd:
		OnMsgCallJava(ivMsg_SpeechEnd);
		break;
	case ivMsg_ResponseTimeout:
		// ResponseTimeout��Ϣ����Ӧ��ʱ��Ϣ����
		OnMsgCallJava(ivMsg_ResponseTimeout);
		break;
	case ivMsg_SpeechTimeout:
		// SpeechTimeOut��Ϣ��������ʱ��Ϣ����
		OnMsgCallJava(ivMsg_SpeechTimeout);
		break;
	case ivMsg_ToStartAudioRec:
		//ToStartAudioRec��Ϣ������¼����Ϣ����
		OnMsgCallJava(ivMsg_ToStartAudioRec);
		break;
	case ivMsg_ToStopAudioRec:
		//ToStopAudioRec��Ϣ���ر�¼����Ϣ����
		OnMsgCallJava(ivMsg_ToStopAudioRec);
		break;
	case ivMsg_Result:
		//Result��Ϣ��ʶ������Ϣ����
		OnMsgResult(wParam,lParam);
		break;	
	case ivMsg_LOG:
		// LOG��Ϣ����־��Ϣ����
		OnMsgLog(wParam,lParam);
		break;

	case ivMsg_Error:
		LOGD("On Msg Error...");
		LOGD((const char*)lParam);
		break;
	}

	return ivErr_OK;
}

//���ļ�build
static int ESR_BuildGrammarFromFile(char *filename)
{
	int iStatus = 0;	
	
	FILE *fp = fopen(filename, "rb");
	if (NULL == fp){
		LOGD("ESR_BuildScene file is null");
		return  -1;
	}
	fseek(fp, 0, SEEK_END);
	ivUInt32 nSize = ftell(fp);
	ivPByte pBuffer = (ivPByte)malloc(nSize+2);
	if (NULL == pBuffer){
		fclose (fp);
		fp = NULL;
		LOGD("ESR_BuildScene malloc is null");
		return -1;
	}
	memset(pBuffer, 0, nSize + 2);
	fseek(fp, 0, SEEK_SET);
	fread(pBuffer, 1, nSize, fp);
	fclose(fp);
	fp = NULL;
	
	iStatus = EsrBuildGrammar(m_hEsrObj, (ivPCByte)pBuffer);
	LogComm("EsrBuildGrammar file=",filename , nSize ,iStatus);
	free(pBuffer);
	pBuffer = NULL;
	return iStatus;
	
}

static int ESR_Destroy()
{
	int ret = 0;

	if (NULL != m_hEsrObj){		
		ret = EsrStop(m_hEsrObj);
		EsrDestroy(m_hEsrObj);
		m_hEsrObj = NULL;
		
	}
	if (NULL != m_pLog){
		fclose (m_pLog);
		m_pLog = NULL;
	}
	
	m_nResultCount = 0;
	m_pResult = NULL;
	
	LOGD("ESR_Destroy Ok");
	return ret;
}
static int ESR_Create()
{

	int iStatus = 0;	
	if (NULL != m_hEsrObj){
		ESR_Destroy();
	}
	memset((ivPByte)&m_stUserOS, 0, sizeof(TUserOS));
	m_stUserOS.nSize = sizeof(TUserOS);
	m_stUserOS.lpszLicence = (ivCharA *)AITALK_SDK_SN;
	m_stUserOS.lpfnRealloc = CBRealloc;
	m_stUserOS.lpfnFree= CBFree;
	m_stUserOS.pPersisRAM = ivNull;;
	m_stUserOS.nPersisRAMSize = 0;;
	m_stUserOS.lpfnWriteFile = ivNull;
	m_stUserOS.lpfnOpenFile = CBOpenFile;
	m_stUserOS.lpfnCloseFile = CBCloseFile;
	m_stUserOS.lpfnReadFile = CBReadFile;	
	m_stUserOS.lpfnMsgProc = CBMsgProc;
#ifdef _DEBUG
	m_stUserOS.bCheckResource = ivTrue;
	m_pLog = fopen(FILE_ASR_LOG, "wb");
#endif
	iStatus = EsrCreate(&m_hEsrObj, &m_stUserOS);
	LogComm("ESR_Create ","",0,iStatus);
	return iStatus;
}

static int utf16len(const ivCharW* str)
{
	int len = 0;
	const ivCharW *p = str;
	while (*p++ != 0){
		len ++;
	}
	return len;
}

jint JNICALL 
Java_com_iflytek_asr_AsrService_Asr_JniGetVersion
	(JNIEnv *env, jobject thiz)
{
	jint major = 0;
	int minor = 0;
	EsrGetVersion((ivPUInt8)&major,(ivPUInt8)&minor);	
	return major * 100 + minor;
}

jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniCreate
	(JNIEnv *env, jobject thiz)
{
	return ESR_Create();
}

jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniDestroy
	(JNIEnv *env, jobject thiz)
{
	return  ESR_Destroy();
}

jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniStart
	(JNIEnv *env, jobject thiz,jstring sceneName)
{
	int ret = ivErr_Failed;
	int iStatus = ivErr_OK;

	char *pName = NULL;
	if (NULL == sceneName){
		return ivErr_Failed;
	}
	pName = (char *)env->GetStringUTFChars(sceneName,NULL);
	
	strcpy(m_strSence,pName);
	
	env->ReleaseStringUTFChars(sceneName,pName);
	
	iStatus = EsrStart(m_hEsrObj,(ivChar *)m_strSence);

	return ret;
}

jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniStop
	(JNIEnv *env, jobject thiz)
{
	int ret = ivErr_Failed;
	if (NULL == m_hEsrObj){
		return ivErr_Failed;
	}
	ret = EsrExitService(m_hEsrObj,ivTrue);
	LogComm("EsrExitService", "",0, ret);
	return ret;
}

jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniRunTask
	(JNIEnv *env, jobject thiz)
{
	int iStatus = ivErr_OK;
	
	m_pResult = NULL;
	m_nResultCount = 0;
	
	if (NULL == m_hEsrObj){
		ESR_Create();
//		ESR_BuildGrammarFromFile(FILE_GRAMMAR);
	}
	JniInit( env);	
//	iStatus = EsrStart(m_hEsrObj,(ivChar *)m_strSence);
	//LogComm("EsrStart sence  ",m_strSence, 0,iStatus);
	if (ivErr_OK == iStatus){
		iStatus = EsrRunService(m_hEsrObj, ivTrue);
		LogComm("EsrRunService ","", 0,iStatus);
	}else{
		LogComm("EsrStart sence failed ",m_strSence, 0,iStatus);
	}
	JniDinit();
	return iStatus;
}

//1.1 ȡ�ܽ����
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniGetResCount
	(JNIEnv *env, jobject thiz)
{
	return m_nResultCount;
}


//1.2  get sentence id
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniGetSentenceId
	(JNIEnv *env, jobject thiz ,jint resIndex)
{

	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	return 	m_pResult[resIndex].iSyntaxID;
}

//1.2 get result confidence
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniGetConfidence
 (JNIEnv *env, jobject thiz ,jint resIndex)
{

	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	return 	m_pResult[resIndex].nConfidenceScore;
}

//1.3 get SlotNumber
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniGetSlotNumber
	(JNIEnv *env, jobject thiz ,jint resIndex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	
	return 	m_pResult[resIndex].nSlot;
}


//1.4 get item Number
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniGetItemNumber
	(JNIEnv *env, jobject thiz ,jint resIndex,jint slotIndex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	if (slotIndex < 0 || slotIndex >= m_pResult[resIndex].nSlot){
		return 0;
	}
	return 	m_pResult[resIndex].pSlots[slotIndex].nItem;
}

//1.5 get item id
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniGetItemId
	(JNIEnv *env, jobject thiz ,jint resIndex,jint slotIndex,jint itemIdex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	if (slotIndex < 0 || slotIndex >= m_pResult[resIndex].nSlot){
		return 0;
	}

	if (itemIdex < 0 || itemIdex >= m_pResult[resIndex].pSlots[slotIndex].nItem){

		return 0;
	}
	return 	m_pResult[resIndex].pSlots[slotIndex].pItems[itemIdex].nID;
}




//1.6 get item text
jstring  JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniGetItemText
	(JNIEnv *env, jobject thiz ,jint resIndex,jint slotIndex,jint itemIdex)
{
	if (m_pResult == NULL 	|| resIndex < 0 || resIndex >= m_nResultCount){
		return 0;
	}
	if (slotIndex < 0 || slotIndex >= m_pResult[resIndex].nSlot){
		return 0;
	}

	if (itemIdex < 0 || itemIdex >= m_pResult[resIndex].pSlots[slotIndex].nItem){
		return 0;
	}
	ivCStrW pres = m_pResult[resIndex].pSlots[slotIndex].pItems[itemIdex].pText;
	int len = utf16len(pres);
	if (NULL != pres){
		return  env->NewString(pres,len);
	}
    return NULL;
}

jint  JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniAppendData
	(JNIEnv *env, jobject thiz,jbyteArray dataArray,jint dataSize)
{
	if (dataSize <= 0 ){
		LOGD("JniAppendData size is 0");
		return 0;
	}
	jbyte* pBuff = env->GetByteArrayElements(dataArray, NULL);

	if (NULL == pBuff){
		LOGD("JniAppendData dataArray is null");
		return -1;
	}

	int ret = EsrAppendAudioData(m_hEsrObj,(ivPInt16) pBuff,dataSize >> 1);
	LogComm("EsrAppendAudioData","",dataSize,  ret);	

	env->ReleaseByteArrayElements(dataArray, pBuff, 0); 
	return ret;	
	
}

jint  JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniEndData
	(JNIEnv *env, jobject thiz)
{

	int ret = EsrEndAudioData(m_hEsrObj);
	return ret;

}


jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniBuildGrammar
	(JNIEnv* env, jobject thiz, jbyteArray grammarBuff,int buffLen)
{
	jbyte * pBuff = env->GetByteArrayElements(grammarBuff, NULL);

	int iStatus = EsrBuildGrammar(m_hEsrObj, (ivPCByte)pBuff);
	
	LogComm("EsrBuildGrammar buffen and ret;" ,"", buffLen ,iStatus);

	env->ReleaseByteArrayElements(grammarBuff,pBuff,0);
	return iStatus;
}


jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniBeginLexicon
	(JNIEnv *env, jobject thiz,jstring lexiconName,jboolean isPersonName)
{
	const char * pName = env->GetStringUTFChars(lexiconName,NULL);
	int iStatus = EsrBeginLexicon(m_hEsrObj,(ivCStr)pName,isPersonName);	
	LogComm("EsrBeginLexicon " , pName, isPersonName, iStatus);
	
	env->ReleaseStringUTFChars(lexiconName,pName);	
	return iStatus;
}

jint  JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniEndLexicon
	(JNIEnv *env, jobject thiz)
{

	int iStatus = EsrEndLexicon(m_hEsrObj);
	LogComm("EsrEndLexicon " , "", 0, iStatus);	
	return iStatus;
}
 
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniAddLexiconItem
	(JNIEnv *env, jobject thiz,jstring word,jint id)
{
	int len = 0;

	len =  env->GetStringLength(word);

	if (len < 1){

		LOGD("EsrAddLexiconItem length is 0 ");
		return -1;
	}
	ivCharW * pWord = (ivCharW *)malloc(len * sizeof(ivCharW) + 2);
	if (NULL == pWord){
		LOGD("EsrAddLexiconItem malloc is null ");
		return -1;
	}
	
	memset(pWord,0,sizeof(ivCharW) * len  + 2);

	env->GetStringRegion(word, 0, len, (jchar *)pWord);
	
	len = utf16len(pWord);
	int iStatus = EsrAddLexiconItem(m_hEsrObj,(ivCharW*)pWord,id);	
	LogComm("EsrAddLexiconItem " ,"length", len, iStatus);

	free(pWord);
	pWord = NULL;	
	return  iStatus;	
}


//����ʱʹ�õ�
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniBuildGrammarFromFile
	(JNIEnv *env, jobject thiz,jstring filename)
{
	char * pName = NULL;
	pName = (char *)env->GetStringUTFChars(filename,NULL);
	int ret = ESR_BuildGrammarFromFile(pName);
	env->ReleaseStringUTFChars(filename,pName);
	return ret;	
}

 
jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniMakeVoiceTag
	(JNIEnv *env, jobject thiz,jstring lexicon,jstring item,jbyteArray buffer,jint length)
{
	/**
	item ʹ��GetStringChars �г�������
	*/
	const char *pLexicon = env->GetStringUTFChars(lexicon, NULL);
	const jchar *pItem = env->GetStringChars(item, NULL);
	jbyte *pBuff = env->GetByteArrayElements(buffer, NULL);

	int iStatus =EsrMakeVoicetag(m_hEsrObj, (ivStr)pLexicon,
						 (ivStrW)pItem,
						 (ivPInt16)pBuff,
						 length/2);
	LogComm("JniMakeVoiceTag " ,"",length,iStatus);	
	env->ReleaseStringUTFChars(lexicon,pLexicon);
	env->ReleaseStringChars(item,pItem);
	env->ReleaseByteArrayElements(buffer,pBuff,0);
	
	return  0;	
}

jint JNICALL
Java_com_iflytek_asr_AsrService_Asr_JniSetParam
	(JNIEnv *env, jobject thiz,jint iParam,jint iValue)
{
	
	int iStatus = EsrSetParam(m_hEsrObj,iParam,(ivCPointer)iValue);
	LogComm("JniSetParam ","",iParam,iStatus);	
	return  iStatus;	
}



/*
 * Table of methods associated with a single class.
 */
static JNINativeMethod gMethods[] = {
	//name, signature, funcPtr
	{"JniGetVersion",      "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetVersion},
    {"JniCreate",          "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniCreate},
    {"JniDestroy",         "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniDestroy},
    {"JniStart",           "(Ljava/lang/String;)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniStart},
    {"JniStop",            "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniStop},
    
    {"JniRunTask",         "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniRunTask},
    {"JniGetResCount",     "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetResCount},
    {"JniGetSentenceId",   "(I)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetSentenceId},
    {"JniGetConfidence",   "(I)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetConfidence},
    {"JniGetSlotNumber",   "(I)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetSlotNumber},
    {"JniGetItemNumber",   "(II)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetItemNumber},
    
    {"JniGetItemId",       "(III)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetItemId},
    {"JniGetItemText",     "(III)Ljava/lang/String;", (void*)Java_com_iflytek_asr_AsrService_Asr_JniGetItemText},
    {"JniAppendData",      "([BI)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniAppendData},
    {"JniEndData",      "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniEndData},


    {"JniBuildGrammar",    "([BI)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniBuildGrammar},
    {"JniBeginLexicon",    "(Ljava/lang/String;Z)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniBeginLexicon},

	{"JniEndLexicon",      "()I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniEndLexicon},
    {"JniAddLexiconItem",  "(Ljava/lang/String;I)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniAddLexiconItem},
	{"JniMakeVoiceTag",    "(Ljava/lang/String;Ljava/lang/String;[BI)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniMakeVoiceTag},
	{"JniSetParam",        "(II)I", (void*)Java_com_iflytek_asr_AsrService_Asr_JniSetParam},
    
};


/*
 * Returns the JNI version on success, -1 on failure.
 */
jint register_com_iflytek_asr_jni(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jclass clazz = NULL; 
    const char* className = "com/iflytek/asr/AsrService/Asr";

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed\n");
        return -1;
    }
    assert(env != NULL);

    jclass classTmp = env->FindClass(className);
    clazz = (jclass)env->NewGlobalRef(classTmp);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class");
        return -1;
    }
    if (env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        LOGE("RegisterNatives failed for");
        return -1;
    }

    /* success -- return valid version number */
    return JNI_VERSION_1_4;
}


jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    if ( register_com_iflytek_asr_jni(vm, reserved) != JNI_VERSION_1_4) 
		return -1;
    return JNI_VERSION_1_4;
}


