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

package com.google.code.jdde.server;

import java.util.logging.Logger;

import com.google.code.jdde.ddeml.CallbackParameters;
import com.google.code.jdde.ddeml.DdeAPI;
import com.google.code.jdde.ddeml.DdeCallback;
import com.google.code.jdde.ddeml.Pointer;
import com.google.code.jdde.ddeml.constants.DmlError;
import com.google.code.jdde.ddeml.constants.FlagCallbackResult;
import com.google.code.jdde.ddeml.constants.InitializeFlags;
import com.google.code.jdde.ddeml.constants.NameServiceFlags;
import com.google.code.jdde.ddeml.constants.TransactionFlags;
import com.google.code.jdde.event.ConnectConfirmEvent;
import com.google.code.jdde.event.ConnectEvent;
import com.google.code.jdde.misc.DdeApplication;
import com.google.code.jdde.misc.JavaDdeUtil;
import com.google.code.jdde.server.event.ConnectionListener;
import com.google.code.jdde.server.event.TransactionListener;

/**
 * 
 * @author Vitor Costa
 */
public class DdeServer extends DdeApplication {

	private static Logger logger = JavaDdeUtil.getLogger();
	
	private ConnectionListener connectionListener;
	private TransactionListener transactionListener;
	
	public DdeServer() {
		this(0);
	}
	
	public DdeServer(int initializeFlags) {
		initialize(new ServerCallbackImpl(), 
				InitializeFlags.APPCLASS_STANDARD |
				InitializeFlags.APPCMD_FILTERINITS |
				initializeFlags);
	}
	
	public void setConnectionListener(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
	}
	
	public void setTransactionListener(TransactionListener transactionListener) {
		this.transactionListener = transactionListener;
	}
	
	public void registerService(String service) {
		invokeNameService(service, NameServiceFlags.DNS_REGISTER);
	}
	
	public void unregisterService(String service) {
		invokeNameService(service, NameServiceFlags.DNS_UNREGISTER);
	}
	
	public void unregisterAllServices() {
		invokeNameService(null, NameServiceFlags.DNS_UNREGISTER);
	}
	
	private void invokeNameService(final String service, final int commands) {
		final Pointer<Integer> error = new Pointer<Integer>();
		
		loop.invokeAndWait(new Runnable() {
			public void run() {
				boolean succeded = DdeAPI.NameService(idInst, service, commands);

				if (!succeded) {
					error.value = DdeAPI.GetLastError(idInst);
				}				
			}
		});
		
		DmlError.throwExceptionIfValidError(error.value);
	}
	
	public void postAdvise(final String topic, final String item) {
		final Pointer<Integer> error = new Pointer<Integer>();
		
		loop.invokeAndWait(new Runnable() {
			public void run() {
				boolean succeded = DdeAPI.PostAdvise(idInst, topic, item);

				if (!succeded) {
					error.value = DdeAPI.GetLastError(idInst);
				}				
			}
		});
		
		DmlError.throwExceptionIfValidError(error.value);
	}
	
	@Override
	public ServerConversation findConversation(int conv) {
		return (ServerConversation) super.findConversation(conv);
	}
	
	private class ServerCallbackImpl implements DdeCallback {

		@Override
		public boolean DdeBooleanCallback(CallbackParameters parameters) {
			boolean result = false;
			
			switch (parameters.getUType()) {
			case TransactionFlags.XTYP_ADVSTART:
				
				break;
			case TransactionFlags.XTYP_CONNECT:
				ConnectEvent event = new ConnectEvent(DdeServer.this, parameters);

				if (connectionListener != null) {
					result = connectionListener.onConnect(event);
				}
				break;
			default:
				String tx = JavaDdeUtil.translateTransaction(parameters.getUType());
				logger.warning("DdeServer should never receive a boolean callback of type " + tx);
				break;
			}
			
			return result;
		}

		@Override
		public byte[] DdeDataCallback(CallbackParameters parameters) {
			switch (parameters.getUType()) {
			case TransactionFlags.XTYP_ADVREQ:
				
				break;
			case TransactionFlags.XTYP_REQUEST:
				ServerConversation conversation = findConversation(parameters.getHconv());
				if (conversation != null) {
					return conversation.fireOnRequest(parameters);
				}
				break;
			case TransactionFlags.XTYP_WILDCONNECT:
				
				break;
			default:
				String tx = JavaDdeUtil.translateTransaction(parameters.getUType());
				logger.warning("DdeServer should never receive a data callback of type " + tx);
				break;
			}
			return null;
		}

		@Override
		public FlagCallbackResult DdeFlagCallback(CallbackParameters parameters) {
			ServerConversation conversation = findConversation(parameters.getHconv());
			if (conversation == null) {
				return FlagCallbackResult.DDE_FNOTPROCESSED;
			}
			
			switch (parameters.getUType()) {
			case TransactionFlags.XTYP_EXECUTE:
				return conversation.fireOnExecute(parameters);
			case TransactionFlags.XTYP_POKE:
				return conversation.fireOnPoke(parameters);
			default:
				String tx = JavaDdeUtil.translateTransaction(parameters.getUType());
				logger.warning("DdeServer should never receive a flag callback of type " + tx);
				return FlagCallbackResult.DDE_FNOTPROCESSED;
			}
		}

		@Override
		public void DdeNotificationCallback(CallbackParameters parameters) {
			ServerConversation conversation = null;
			
			switch (parameters.getUType()) {
			case TransactionFlags.XTYP_ADVSTOP:
				
				break;
			case TransactionFlags.XTYP_CONNECT_CONFIRM:
				ConnectConfirmEvent event = new ConnectConfirmEvent(DdeServer.this, parameters);
				
				conversation = event.getConversation();
				conversation.setTransactionListener(transactionListener);
				
				conversations.add(conversation);
				
				if (connectionListener != null) {
					connectionListener.onConnectConfirm(event);
				}
				break;
			case TransactionFlags.XTYP_DISCONNECT:
				conversation = findConversation(parameters.getHconv());
				if (conversation != null) {
					conversation.fireOnDisconnect(parameters);
				}
				break;
			case TransactionFlags.XTYP_ERROR:
				
				break;
			case TransactionFlags.XTYP_REGISTER:
				
				break;
			case TransactionFlags.XTYP_UNREGISTER:
				
				break;
			default:
				String tx = JavaDdeUtil.translateTransaction(parameters.getUType());
				logger.warning("DdeServer should never receive a notification callback of type " + tx);
				break;
			}
		}

	}
	
}
