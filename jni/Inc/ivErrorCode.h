/*----------------------------------------------+
|												|
|	ivErrorCode.h - Basic Definitions		|
|												|
|		Copyright (c) 1999-2007, iFLYTEK Ltd.	|
|		All rights reserved.					|
|												|
+----------------------------------------------*/

#ifndef IFLYTEK_VOICE__2008_10_13_ERRORCODE__H
#define IFLYTEK_VOICE__2008_10_13_ERRORCODE__H

#include "ivEsrDefine.h"

#ifdef __cplusplus
extern "C" {
#endif

#if IV_FIXED_ERRORCODE
/* For license check */
#define ivErr_InvSN				((ivStatus)2)

/* General */
#define ivErr_InvArg			((ivStatus)3)
#define ivErr_BufferFull		((ivStatus)4)
#define ivErr_Failed			((ivStatus)5)
#define ivErr_NotSupport		((ivStatus)6)
#define ivErr_OutOfMemory		((ivStatus)7)
#define ivErr_InvResource		((ivStatus)8)
#define ivErr_NotFound			((ivStatus)9)
#define ivErr_InvGrmr           ((ivStatus)10)

/* For object status */
#define ivErr_InvCall			((ivStatus)11)

/* For ASR Input */
#define ivErr_SyntaxError		((ivStatus)12)

/* For Message Call Back */
#define ivErr_Reset				((ivStatus)13)

#define ivErr_Ended				((ivStatus)14)
#define ivErr_Idle				((ivStatus)15)

#define ivErr_CanNotSaveFile    ((ivStatus)16)

/* For Lexicon name */
#define ivErr_InvName    ((ivStatus)17)

#define ES_DEFINE_ERROR(e)		ivTextA(#e),
ivExtern ivConst ivCStrA g_szErrorInfo[];

#else
#define ES_DECLARE_ERROR(e)			ivExtern ivConst ivChar e[];
#if IV_UNICODE 
	#define ES_DEFINE_ERROR(e)		ivConst ivChar e[] = L###e;
#else
	#define ES_DEFINE_ERROR(e)		ivConst ivChar e[] = #e;
#endif

/* For license check */
ES_DECLARE_ERROR(ivErr_InvSN)

/* General */
ES_DECLARE_ERROR(ivErr_InvArg)
ES_DECLARE_ERROR(ivErr_BufferFull)
ES_DECLARE_ERROR(ivErr_Failed)
ES_DECLARE_ERROR(ivErr_NotSupport)
ES_DECLARE_ERROR(ivErr_OutOfMemory)
ES_DECLARE_ERROR(ivErr_InvResource)
ES_DECLARE_ERROR(ivErr_NotFound)
ES_DECLARE_ERROR(ivErr_InvGrmr)

/* For object status */
ES_DECLARE_ERROR(ivErr_InvCall)

/* For ASR Input */
ES_DECLARE_ERROR(ivErr_SyntaxError)

/* For Message Call Back */
ES_DECLARE_ERROR(ivErr_Reset)

ES_DECLARE_ERROR(ivErr_Ended)
ES_DECLARE_ERROR(ivErr_Idle)

ES_DECLARE_ERROR(ivErr_CanNotSaveFile)

ES_DECLARE_ERROR(ivErr_InvName)

#endif /* IV_FIXED_ERRORCODE */

ivCStrA ivGetErrInfo(ivStatus e);

#ifdef __cplusplus
}
#endif


#define	ivErr_OK				((ivStatus)0)
#define	ivErr_FALSE				((ivStatus)1)

#define ivSucceeded(hr)			((ivUInt32)(hr) <= 1)

#endif /* !IFLYTEK_VOICE__2008_10_13_ERRORCODE__H */
