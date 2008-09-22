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

import com.google.code.jdde.client.event.AsyncTransactionCompletedListener;
import com.google.code.jdde.ddeml.CallbackParameters;
import com.google.code.jdde.event.AsyncTransactionCompletedEvent;
import com.google.code.jdde.misc.ClipboardFormat;

/**
 * 
 * @author Vitor Costa
 */
public class AsyncTransaction {

	private DdeClient client;
	private ClientConversation conversation;
	
	private int transactionId;
	
	private AsyncTransactionCompletedListener listener;
	
	AsyncTransaction(DdeClient client, ClientConversation conversation, int transactionId,
			AsyncTransactionCompletedListener listener) {
		this.client = client;
		this.conversation = conversation;
		
		this.transactionId = transactionId;
		
		this.listener = listener;
	}
	
	public int getTransactionId() {
		return transactionId;
	}
	
	public void abandon() {
		
	}
	
	void fireAsyncTransactionCompleted(CallbackParameters parameters) {
		if (listener != null) {
			ClipboardFormat format = new ClipboardFormat(parameters.getUFmt());
			byte[] data = parameters.getHdata();
			int statusFlags = ((Integer) parameters.getDwData2()).intValue();
			
			AsyncTransactionCompletedEvent event = new AsyncTransactionCompletedEvent(
					client, conversation, this, format, data, statusFlags);
			
			if (data == null) {
				listener.onError(event);
			}
			else {
				listener.onSuccess(event);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + transactionId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AsyncTransaction other = (AsyncTransaction) obj;
		if (transactionId != other.transactionId)
			return false;
		return true;
	}

}
