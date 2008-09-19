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

package com.google.code.jdde.client.event;

import com.google.code.jdde.client.Conversation;
import com.google.code.jdde.client.DdeClient;

/**
 * 
 * @author Vitor Costa
 */
public class ConversationEvent {

	private final DdeClient client;
	private final Conversation conversation;
	
	public ConversationEvent(DdeClient client, Conversation conversation) {
		this.client = client;
		this.conversation = conversation;
	}

	public DdeClient getClient() {
		return client;
	}

	public Conversation getConversation() {
		return conversation;
	}
	
}
