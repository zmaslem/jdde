/*
 * Copyright 2008 Vitor Costa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include <windows.h>
#include <winuser.h>
#include <ddeml.h>

#include "DdeAPI.h"
#include "Util.h"

#define iCodePage CP_WINANSI


JNIEXPORT jbyteArray JNICALL Java_com_google_code_jdde_ddeml_DdeAPI_ClientTransaction
  (JNIEnv *env, jclass cls, jint idInst, jbyteArray jpData, jint hConv, jstring jhszItem,
		  jint wFmt, jint wType, jint dwTimeout, jobject $dwResult)
{
	HSZ hszItem = NULL;
	HDDEDATA pData = NULL;
	DWORD dwResult = 0;

	if (jhszItem != NULL) {
		const char *strItem = env->GetStringUTFChars(jhszItem, 0);
		hszItem = DdeCreateStringHandle(idInst, strItem, iCodePage);
		env->ReleaseStringUTFChars(jhszItem, strItem);
	}

	if (jpData != NULL) {
		jsize cb = env->GetArrayLength(jpData);
		jbyte *pSrc = env->GetByteArrayElements(jpData, 0);
		pData = DdeCreateDataHandle(idInst, (LPBYTE) pSrc, cb, 0, hszItem, wFmt, 0);
		env->ReleaseByteArrayElements(jpData, pSrc, 0);
	}

	HDDEDATA hddeData = DdeClientTransaction(
			(LPBYTE) pData,
			0xFFFFFFFF,
			(HCONV) hConv,
			hszItem,
			wFmt,
			wType,
			dwTimeout,
			&dwResult
	);

	if (pData != NULL) {
		DdeFreeDataHandle(pData);
	}

	if (hszItem != NULL) {
		DdeFreeStringHandle(idInst, hszItem);
	}

	if ($dwResult != NULL) {
		SetObjectInPointer(env, $dwResult, NewInteger(env, dwResult));
	}

	if (hddeData == NULL) {
		return NULL;
	}
	else if (wType == XTYP_REQUEST) {
		jbyteArray result = ExtractData(env, hddeData);
		DdeFreeDataHandle(hddeData);
		return result;
	}

	return env->NewByteArray(0);
}

JNIEXPORT jint JNICALL Java_com_google_code_jdde_ddeml_DdeAPI_Connect
  (JNIEnv *env, jclass cls, jint idInst, jstring jhszService, jstring jhszTopic, jobject pCC)
{
	HSZ hszService = NULL;
	HSZ hszTopic = NULL;

	if (jhszService != NULL) {
		const char *strService = env->GetStringUTFChars(jhszService, 0);
		hszService = DdeCreateStringHandle(idInst, strService, iCodePage);
		env->ReleaseStringUTFChars(jhszService, strService);
	}

	if (jhszTopic != NULL) {
	    const char *strTopic = env->GetStringUTFChars(jhszTopic, 0);
		hszTopic = DdeCreateStringHandle(idInst, strTopic, iCodePage);
		env->ReleaseStringUTFChars(jhszTopic, strTopic);
	}

	HCONV hConv = DdeConnect(
			idInst,					// instance identifier
			hszService,				// service name string handle
			hszTopic,				// topic string handle
			(PCONVCONTEXT) NULL);	// use default context

	if (hszService != NULL) {
		DdeFreeStringHandle(idInst, hszService);
	}

	if (hszTopic != NULL) {
		DdeFreeStringHandle(idInst, hszTopic);
	}

	return (UINT) hConv;
}

JNIEXPORT jboolean JNICALL Java_com_google_code_jdde_ddeml_DdeAPI_Disconnect
  (JNIEnv *env, jclass cls, jint hConv)
{
	return DdeDisconnect((HCONV) hConv);
}

JNIEXPORT jint JNICALL Java_com_google_code_jdde_ddeml_DdeAPI_GetLastError
  (JNIEnv *env, jclass cls, jint idInst)
{
	return DdeGetLastError(idInst);
}

JNIEXPORT jint JNICALL Java_com_google_code_jdde_ddeml_DdeAPI_Initialize
  (JNIEnv *env, jclass cls, jobject $idInst, jint afCmd)
{
	DWORD idInst = 0;

	UINT initError = DdeInitialize(
			&idInst, // receives instance identifier
			(PFNCALLBACK) DdeCallback, // pointer to callback function
			afCmd, 0);

	SetObjectInPointer(env, $idInst, NewInteger(env, idInst));

	return initError;
}

JNIEXPORT jboolean JNICALL Java_com_google_code_jdde_ddeml_DdeAPI_NameService
  (JNIEnv *env, jclass cls, jint idInst, jstring jhsz1, jint afCmd)
{
	const char *str1 = env->GetStringUTFChars(jhsz1, 0);
	HSZ hsz1 = DdeCreateStringHandle(idInst, str1, iCodePage);
	env->ReleaseStringUTFChars(jhsz1, str1);

	HDDEDATA data = DdeNameService(idInst, hsz1, 0L, afCmd);

	DdeFreeStringHandle(idInst, hsz1);

	return data != NULL;
}

JNIEXPORT jboolean JNICALL Java_com_google_code_jdde_ddeml_DdeAPI_Uninitialize
  (JNIEnv *env, jclass cls, jint idInst)
{
	return DdeUninitialize(idInst);
}

JavaVM *jvm;
jclass CallbackManager = NULL;
jmethodID mGetIdInst;
jmethodID mBooleanCallback;
jmethodID mDataCallback;
jmethodID mFlagCallback;
jmethodID mNotificationCallback;

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	jvm = vm;
	return JNI_VERSION_1_4;
}

HDDEDATA CALLBACK DdeCallback(
    UINT uType,     // Transaction type.
    UINT uFmt,      // Clipboard data format.
    HCONV hconv,    // Handle to the conversation.
    HSZ hsz1,       // Handle to a string.
    HSZ hsz2,       // Handle to a string.
    HDDEDATA hdata, // Handle to a global memory object.
    DWORD dwData1,  // Transaction-specific data.
    DWORD dwData2)  // Transaction-specific data.

{
	JNIEnv *env;
	jvm->GetEnv((void **) &env, JNI_VERSION_1_4);

	env->PushLocalFrame(16);

	HDDEDATA result = NULL;

	UINT idThread = GetCurrentThreadId();

	if (CallbackManager == NULL) {
		jclass clazz = env->FindClass("com/google/code/jdde/ddeml/CallbackManager");
		CallbackManager = (jclass) env->NewGlobalRef(clazz);

		mGetIdInst = env->GetStaticMethodID(CallbackManager, "getIdInst", "(I)I");
		mBooleanCallback = env->GetStaticMethodID(clazz, "DdeBooleanCallback", "(Lcom/google/code/jdde/ddeml/CallbackParameters;)Z");
		mDataCallback = env->GetStaticMethodID(clazz, "DdeDataCallback", "(Lcom/google/code/jdde/ddeml/CallbackParameters;)[B");
		mFlagCallback = env->GetStaticMethodID(clazz, "DdeFlagCallback", "(Lcom/google/code/jdde/ddeml/CallbackParameters;)I");
		mNotificationCallback = env->GetStaticMethodID(clazz, "DdeNotificationCallback", "(Lcom/google/code/jdde/ddeml/CallbackParameters;)V");
	}

	UINT idInst = env->CallStaticIntMethod(CallbackManager, mGetIdInst, idThread);

	jobject parameter = WrapCallbackParameters(env, idInst, idThread, uType,
			uFmt, hconv, hsz1, hsz2, hdata, dwData1, dwData2);

	switch (uType) {
	case XTYP_ADVSTART:
	case XTYP_CONNECT:
		result = (HDDEDATA) env->CallStaticBooleanMethod(CallbackManager, mBooleanCallback, parameter);
		break;
	case XTYP_ADVREQ:
	case XTYP_REQUEST:
	case XTYP_WILDCONNECT:
		jobject cbResult = env->CallStaticObjectMethod(CallbackManager, mDataCallback, parameter);
		break;
	case XTYP_ADVDATA:
	case XTYP_EXECUTE:
	case XTYP_POKE:
		result = (HDDEDATA) env->CallStaticIntMethod(CallbackManager, mFlagCallback, parameter);
		break;
	case XTYP_ADVSTOP:
	case XTYP_CONNECT_CONFIRM:
	case XTYP_DISCONNECT:
	case XTYP_ERROR:
	case XTYP_MONITOR:
	case XTYP_REGISTER:
	case XTYP_XACT_COMPLETE:
	case XTYP_UNREGISTER:
		env->CallStaticVoidMethod(CallbackManager, mNotificationCallback, parameter);
		break;
	}

	env->PopLocalFrame(NULL);
	return result;
}
