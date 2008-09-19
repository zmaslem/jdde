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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.code.jdde.client.event.AdviseDataListener;
import com.google.code.jdde.client.event.AsyncTransactionCompletedListener;
import com.google.code.jdde.ddeml.CallbackParameters;
import com.google.code.jdde.ddeml.DdeAPI;
import com.google.code.jdde.ddeml.constants.TransactionFlags;
import com.google.code.jdde.misc.ClientTransaction;
import com.google.code.jdde.misc.ClipboardFormat;
import com.google.code.jdde.misc.MessageLoop;
import com.google.code.jdde.misc.PosTransactionTask;
import com.google.code.jdde.misc.JavaDdeUtil;

/**
 * 
 * @author Vitor Costa
 */
public class Conversation {

	private static Logger logger = JavaDdeUtil.getLogger();
	
	private int idInst;
	private int hConv;
	private MessageLoop loop;
	private DdeClient client;
	
	private String service;
	private String topic;
	
	private List<Advise> advises;
	private Map<Integer, AsyncTransaction> transactions;
	
	Conversation(DdeClient client, int hConv, String service, String topic) {
		this.hConv = hConv;
		this.idInst = client.getIdInst();
		this.loop = client.getLoop();
		this.client = client;
		
		this.service = service;
		this.topic = topic;
		
		advises = new ArrayList<Advise>();
		transactions = new HashMap<Integer, AsyncTransaction>();
	}
	
	int getHandle() {
		return hConv;
	}
	
	public String getService() {
		return service;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void disconnect() {
		loop.invokeAndWait(new Runnable() {
			public void run() {
				DdeAPI.Disconnect(hConv);					
			}
		});
	}
	
	public byte[] request(String item) {
		return request(item, client.getDefaultFormat(), client.getDefaultTimeout());
	}
	
	public byte[] request(String item, ClipboardFormat format, int timeout) {
		ClientTransaction tx = new ClientTransaction(loop);
		tx.call(idInst, null, hConv, item, format.getValue(), TransactionFlags.XTYP_REQUEST, timeout);
		
		tx.throwExceptionOnError();
		return tx.getData();
	}
	
	public AsyncTransaction requestAsync(String item, AsyncTransactionCompletedListener listener) {
		return requestAsync(item, client.getDefaultFormat(), listener);
	}
	
	public AsyncTransaction requestAsync(String item, ClipboardFormat format,
			AsyncTransactionCompletedListener listener) {
		
		AsyncTransactionTask task = new AsyncTransactionTask(listener);
		
		ClientTransaction tx = new ClientTransaction(loop);
		tx.call(idInst, null, hConv, item, format.getValue(),
				TransactionFlags.XTYP_REQUEST, TransactionFlags.TIMEOUT_ASYNC, task);
		
		tx.throwExceptionOnError();
		
		return task.asyncTx;
	}
	
	public void execute(String command) {
		execute(command, client.getDefaultTimeout());
	}
	
	public void execute(String command, int timeout) {
		ClientTransaction tx = new ClientTransaction(loop);
		tx.call(idInst, command.getBytes(), hConv, null, ClipboardFormat.TEXT.getValue(),
				TransactionFlags.XTYP_EXECUTE, timeout);

		tx.throwExceptionOnError();
	}
	
	public void poke(String item, byte[] data) {
		poke(item, data, client.getDefaultFormat(), client.getDefaultTimeout());
	}
	
	public void poke(String item, byte[] data, ClipboardFormat format, int timeout) {
		ClientTransaction tx = new ClientTransaction(loop);
		tx.call(idInst, data, hConv, item, format.getValue(), TransactionFlags.XTYP_POKE, timeout);
		
		tx.throwExceptionOnError();
	}
	
	public Advise startAdvise(String item, AdviseDataListener listener) {
		return startAdvise(item, client.getDefaultFormat(), client.getDefaultTimeout(), listener);
	}
	
	public Advise startAdvise(String item, ClipboardFormat format, int timeout,
			AdviseDataListener listener) {
		
		StartAdviseTask task = new StartAdviseTask(item, format, listener);
		
		ClientTransaction tx = new ClientTransaction(loop);
		tx.call(idInst, null, hConv, item, format.getValue(),
				TransactionFlags.XTYP_ADVSTART, timeout, task);
		
		tx.throwExceptionOnError();
		
		return task.advise;
	}
	
	/* ************************************ *
	 ********** dispatch callbacks ********** 
	 * ************************************ */
	
	void fireValueChanged(CallbackParameters parameters) {
		for (Advise advise : advises) {
			advise.fireValueChanged(parameters);
		}
	}
	
	void fireAsyncTransactionCompleted(CallbackParameters parameters) {
		Object txId = parameters.getDwData1();

		if (txId == null) {
			logger.warning("CallbackParameters.dwData1 is null");
		}
		else if (txId.getClass() != Integer.class) {
			logger.warning("CallbackParameters.dwData1 is not of type Integer");
		}
		else {
			AsyncTransaction asyncTx = transactions.get(txId);
			
			if (asyncTx != null) { 
				asyncTx.fireAsyncTransactionCompleted(parameters);
				transactions.remove(txId);
			}
			else {
				logger.warning("Could not find transaction with the given id: " + txId);
			}
		}
	}
	
	void adviseStoped(Advise advise) {
		advises.remove(advise);
	}

	/* ************************************ *
	 ************ helper classes ************ 
	 * ************************************ */
	
	private class StartAdviseTask implements PosTransactionTask {
		
		private Advise advise;
		
		private String item;
		private ClipboardFormat format;
		private AdviseDataListener listener;
		
		public StartAdviseTask(String item, ClipboardFormat format,
				AdviseDataListener listener) {

			this.format = format;
			this.item = item;
			this.listener = listener;
		}

		@Override
		public void call(ClientTransaction clientTx) {
			advise = new Advise(client, Conversation.this, item, format, listener);
			advises.add(advise);
		}
		
	}
	
	private class AsyncTransactionTask implements PosTransactionTask {
		
		private AsyncTransaction asyncTx;
		private AsyncTransactionCompletedListener listener;
		
		public AsyncTransactionTask(AsyncTransactionCompletedListener listener) {
			this.listener = listener;
		}
		
		@Override
		public void call(ClientTransaction clientTx) {
			Integer txId = clientTx.getResult();
			asyncTx = new AsyncTransaction(client, Conversation.this, txId, listener);

			transactions.put(txId, asyncTx);
		}
		
	};
	
}
