/*----------------------------------------------+
 |												|
 |	ivPlatform.h - Platform Config |
 |												|
 |		Platform: Win32 (X86)					|
 |												|
 |		Copyright (c) 1999-2009, iFLYTEK Ltd.	|
 |		All rights reserved.					|
 |												|
 +----------------------------------------------*/

#ifndef IFLYTEK_VOICE__PLATFORM__H
#define IFLYTEK_VOICE__PLATFORM__H

/*
 *	TODO: ���������Ŀ��ƽ̨������Ҫ�Ĺ���ͷ�ļ�
 */
 

/*
#include <crtdbg.h>
#include <memory.h>
#include <stdlib.h>
#include <string.h>
#include <tchar.h>
#include <windows.h>
*/

/*
 *	TODO: ����Ŀ��ƽ̨�����޸����������ѡ��
 */


#define IV_UNIT_BITS			8			/* �ڴ������Ԫλ�� */
#define IV_BIG_ENDIAN			0			/* �Ƿ��� Big-Endian �ֽ��� */
#define IV_PTR_GRID				4			/* ���ָ�����ֵ */

#define IV_PTR_PREFIX						/* ָ�����ιؼ���(����ȡֵ�� near | far, ����Ϊ��) */
#define IV_CONST				const		/* �����ؼ���(����Ϊ��) */
#define IV_EXTERN				extern		/* �ⲿ�ؼ��� */
#define IV_STATIC				static		/* ��̬�����ؼ���(����Ϊ��) */
#define IV_INLINE					/* �����ؼ���(����ȡֵ�� inline, ����Ϊ��) */
#define IV_CALL_STANDARD			/* ��ͨ�������ιؼ���(����ȡֵ�� stdcall | fastcall | pascal, ����Ϊ��) */
#define IV_CALL_REENTRANT			/* �ݹ麯�����ιؼ���(����ȡֵ�� stdcall | reentrant, ����Ϊ��) */
#define IV_CALL_VAR_ARG					/* ��κ������ιؼ���(����ȡֵ�� cdecl, ����Ϊ��) */

#define IV_TYPE_INT8			char		/* 8λ�������� */
#define IV_TYPE_INT16			short		/* 16λ�������� */
#define IV_TYPE_INT24			int			/* 24λ�������� */
#define IV_TYPE_INT32			long		/* 32λ�������� */

#if 1 /* 48/64 λ���������ǿ�ѡ��, ��Ǳ�Ҫ��Ҫ����, ��ĳЩ 32 λƽ̨��, ʹ��ģ�ⷽʽ�ṩ�� 48/64 λ������������Ч�ʺܵ� */
#define IV_TYPE_INT48			long long		/* 48λ�������� */
#define IV_TYPE_INT64			long long		/* 64λ�������� */
#endif

#define IV_TYPE_ADDRESS			ivUInt32		/* ��ַ�������� */
#define IV_TYPE_SIZE			ivUInt32		/* ��С�������� */

#define IV_VOLATILE				volatile

#define IV_ANSI_MEMORY			0			/* �Ƿ�ʹ�� ANSI �ڴ������ */
#define	IV_ANSI_STRING			0			/* �Ƿ�ʹ�� ANSI �ַ��������� */

#define IV_ASSERT(exp)			/* _ASSERT(exp)*/ /* ���Բ��� */
#define IV_YIELD				/* ���в���(��Э��ʽ����ϵͳ��Ӧ����Ϊ�����л�����, ����Ϊ��) */
#define IV_TTS_ARM_CODECACHE	0			/* �ֹ�����Cache */

/* ����ƽ̨����ѡ������Ƿ�֧�ֵ��� */
#if defined(DEBUG) || defined(_DEBUG)
	#define IV_DEBUG			1			/* �Ƿ�֧�ֵ��� */
#else
	#define IV_DEBUG			0			/* �Ƿ�֧�ֵ��� */
#endif

/* ����ƽ̨����ѡ������Ƿ��� Unicode ��ʽ���� */
#if defined(UNICODE) || defined(_UNICODE)
	#define IV_UNICODE			1			/* �Ƿ��� Unicode ��ʽ���� */
#else
	#define IV_UNICODE			0			/* �Ƿ��� Unicode ��ʽ���� */
#endif

#endif /* IFLYTEK_VOICE__PLATFORM__H */
