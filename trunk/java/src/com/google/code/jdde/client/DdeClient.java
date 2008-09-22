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

package com.google.code.jdde.client;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.code.jdde.ddeml.CallbackParameters;
import com.google.code.jdde.ddeml.DdeAPI;
import com.google.code.jdde.ddeml.DdeCallback;
import com.google.code.jdde.ddeml.Pointer;
import com.google.code.jdde.ddeml.constants.DmlError;
import com.google.code.jdde.ddeml.constants.FlagCallbackResult;
import com.google.code.jdde.ddeml.constants.InitializeFlags;
import com.google.code.jdde.ddeml.constants.TransactionFlags;
import com.google.code.jdde.misc.ClipboardFormat;
import com.google.code.jdde.misc.DdeApplication;
import com.google.code.jdde.misc.JavaDdeUtil;

/**
 * 
 * @author Vitor Costa
 */
public class DdeClient extends DdeApplication {

	private static Logger logger = JavaDdeUtil.getLogger();
	
	private int defaultTimeout;
	private ClipboardFormat defaultFormat;
	
	private List<ClientConversation> conversations;
	
	public DdeClient() {
		conversations = new ArrayList<ClientConversation>();
		
		defaultTimeout = 9999;
		defaultFormat = ClipboardFormat.TEXT;
		
		initialize(new ClientCallbackImpl(), 
				InitializeFlags.APPCLASS_STANDARD |
				InitializeFlags.APPCMD_CLIENTONLY);
	}
	
	public int getDefaultTimeout() {
		return defaultTimeout;
	}

	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public ClipboardFormat getDefaultFormat() {
		return defaultFormat;
	}

	public void setDefaultFormat(ClipboardFormat defaultFormat) {
		this.defaultFormat = defaultFormat;
	}

	public ClientConversation connect(final String service, final String topic) {
		final Pointer<Integer> error = new Pointer<Integer>();
		final Pointer<Integer> hConv = new Pointer<Integer>();
		
		loop.invokeAndWait(new Runnable() {
			public void run() {
				hConv.value = DdeAPI.Connect(idInst, service, topic, null);
				
				if (hConv.value == 0) {
					error.value = DdeAPI.GetLastError(idInst);
				}
			}
		});

		DmlError.throwExceptionIfValidError(error.value);
		
		ClientConversation conversation = new ClientConversation(this, hConv.value, service, topic);
		conversations.add(conversation);
		
		return conversation;
	}
	
	public ClientConversation findConversation(int hConv) {
		for (int i = 0; i < conversations.size(); i++) {
			ClientConversation conversation = conversations.get(i);
			if (hConv == conversation.getHConv()) {
				return conversation;
			}
		}
		return null;
	}

	private class ClientCallbackImpl implements DdeCallback {
		@Override
		public boolean DdeBooleanCallback(CallbackParameters parameters) {
			logger.warning("DdeClient should never receive a boolean callback");
			return false;
		}
	
		@Override
		public byte[] DdeDataCallback(CallbackParameters parameters) {
			logger.warning("DdeClient should never receive a data callback");
			return null;
		}
	
		@Override
		public FlagCallbackResult DdeFlagCallback(CallbackParameters parameters) {
			switch (parameters.getUType()) {
			case TransactionFlags.XTYP_ADVDATA:
				ClientConversation conversation = findConversation(parameters.getHconv());
				if (conversation != null) {
					conversation.fireValueChanged(parameters);
				}
				else {
					logger.warning("Conversation " + parameters.getHconv() + " hasn't been found");
				}
				break;
			default:
				String tx = JavaDdeUtil.translateTransaction(parameters.getUType());
				logger.warning("DdeClient should never receive a flag callback of type " + tx);
				break;
			}

			return FlagCallbackResult.DDE_FACK;
		}
	
		@Override
		public void DdeNotificationCallback(CallbackParameters parameters) {
			ClientConversation conversation = findConversation(parameters.getHconv());
			
			switch (parameters.getUType()) {
			case TransactionFlags.XTYP_DISCONNECT:
				
				break;
			case TransactionFlags.XTYP_ERROR:
				
				break;
			case TransactionFlags.XTYP_XACT_COMPLETE:
				if (conversation != null) {
					conversation.fireAsyncTransactionCompleted(parameters);
				}
				else {
					logger.warning("Conversation " + parameters.getHconv() + " hasn't been found");
				}
				break;
			default:
				String tx = JavaDdeUtil.translateTransaction(parameters.getUType());
				logger.warning("DdeClient should never receive a notification callback of type " + tx);
				break;
			}
		}
	}
	
}
