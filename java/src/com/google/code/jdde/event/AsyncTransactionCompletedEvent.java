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

package com.google.code.jdde.event;

import com.google.code.jdde.client.AsyncTransaction;
import com.google.code.jdde.client.ClientConversation;
import com.google.code.jdde.client.DdeClient;
import com.google.code.jdde.misc.ClipboardFormat;

/**
 * 
 * @author Vitor Costa
 */
public class AsyncTransactionCompletedEvent extends ConversationEvent<DdeClient, ClientConversation> {

	private final AsyncTransaction transaction;
	private final ClipboardFormat format;
	
	private final byte[] data;
	private final int statusFlags;
	
	public AsyncTransactionCompletedEvent(DdeClient client, ClientConversation conversation, 
			AsyncTransaction transaction, ClipboardFormat format, byte[] data, int statusFlag) {
		
		super(client, conversation);
		
		this.transaction = transaction;
		this.format = format;
		
		this.data = data;
		this.statusFlags = statusFlag;
	}

	public AsyncTransaction getTransaction() {
		return transaction;
	}

	public ClipboardFormat getFormat() {
		return format;
	}

	public byte[] getData() {
		return data;
	}

	public int getStatusFlags() {
		return statusFlags;
	}
	
}
