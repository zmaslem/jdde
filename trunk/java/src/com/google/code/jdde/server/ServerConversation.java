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

import com.google.code.jdde.ddeml.CallbackParameters;
import com.google.code.jdde.ddeml.constants.FlagCallbackResult;
import com.google.code.jdde.event.AdviseRequestEvent;
import com.google.code.jdde.event.AdviseStartEvent;
import com.google.code.jdde.event.AdviseStopEvent;
import com.google.code.jdde.event.ExecuteEvent;
import com.google.code.jdde.event.PokeEvent;
import com.google.code.jdde.event.RequestEvent;
import com.google.code.jdde.misc.Conversation;
import com.google.code.jdde.server.event.TransactionListener;

/**
 * 
 * @author Vitor Costa
 */
public class ServerConversation extends Conversation {

	private TransactionListener transactionListener;
	
	ServerConversation(DdeServer server, int hConv, String service, String topic) {
		super(server, hConv, service, topic);
	}
	
	public void setTransactionListener(TransactionListener transactionListener) {
		this.transactionListener = transactionListener;
	}
	
	@Override
	public DdeServer getApplication() {
		return (DdeServer) super.getApplication();
	}
	
	/* ************************************ *
	 ********** dispatch callbacks ********** 
	 * ************************************ */
	
	byte[] fireOnAdviseRequest(CallbackParameters parameters) {
		if (transactionListener != null) {
			AdviseRequestEvent event = new AdviseRequestEvent(getApplication(), this, parameters);
			return transactionListener.onAdviseRequest(event);
		}
		return null;
	}
	
	boolean fireOnAdviseStart(CallbackParameters parameters) {
		if (transactionListener != null) {
			AdviseStartEvent event = new AdviseStartEvent(getApplication(), this, parameters);
			return transactionListener.onAdviseStart(event);
		}
		return false;
	}

	void fireOnAdviseStop(CallbackParameters parameters) {
		if (transactionListener != null) {
			AdviseStopEvent event = new AdviseStopEvent(getApplication(), this, parameters);
			transactionListener.onAdviseStop(event);
		}
	}
	
	void fireOnDisconnect(CallbackParameters parameters) {
		if (transactionListener != null) {
//			ServerDisconnectEvent event = new ServerDisconnectEvent(getApplication(), this, parameters);
			//TODO missing call to listener
		}
	}
	
	FlagCallbackResult fireOnExecute(CallbackParameters parameters) {
		if (transactionListener != null) {
			ExecuteEvent event = new ExecuteEvent(getApplication(), this, parameters);
			return transactionListener.onExecute(event);
		}
		return FlagCallbackResult.DDE_FNOTPROCESSED;
	}
	
	FlagCallbackResult fireOnPoke(CallbackParameters parameters) {
		if (transactionListener != null) {
			PokeEvent event = new PokeEvent(getApplication(), this, parameters);
			return transactionListener.onPoke(event);
		}
		return FlagCallbackResult.DDE_FNOTPROCESSED;
	}
	
	byte[] fireOnRequest(CallbackParameters parameters) {
		if (transactionListener != null) {
			RequestEvent event = new RequestEvent(getApplication(), this, parameters);
			return transactionListener.onRequest(event);
		}
		return null;
	}
	
}
