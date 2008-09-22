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

package com.google.code.jdde;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import com.google.code.jdde.client.DdeClient;
import com.google.code.jdde.event.ConnectEvent;
import com.google.code.jdde.server.DdeServer;
import com.google.code.jdde.server.event.ConnectionAdapter;

/**
 * 
 * @author Vitor Costa
 */
public class JavaDdeTest extends Assert {

	private CountDownLatch latch;
	
	private List<DdeClient> clients = new ArrayList<DdeClient>();
	private List<DdeServer> servers = new ArrayList<DdeServer>();
	
	@Before
	public void clear() {
		latch = null;
		clients.clear();
		servers.clear();
	}
	
	public void startTest(int count) {
		latch = new CountDownLatch(count);
	}

	public void countDown() {
		latch.countDown();
	}
	
	@After
	public void finishTest() {
		if (latch != null) {
			try {
				latch.await(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {}
		}
		
		for (DdeClient client : clients) {
			client.rethrowMessageLoopException();
		}
		for (DdeServer server : servers) {
			server.rethrowMessageLoopException();
		}
	}
	
	@After
	public void releaseResources() {
		for (DdeClient client : clients) {
			client.uninitialize();
		}
		for (DdeServer server : servers) {
			server.uninitialize();
		}
	}
	
	public DdeClient newClient() {
		DdeClient client = new DdeClient();
		clients.add(client);
		return client;
	}
	
	public DdeServer newServer(String ... services) {
		return newServer(0, services);
	}
	
	public DdeServer newServer(int initializeFlags, String ... services) {
		DdeServer server = new DdeServer(initializeFlags);
		for (String service : services) {
			server.registerService(service);
		}
		servers.add(server);
		return server;
	}
	
	public DdeServer newOpenServer(String ... services) {
		return newOpenServer(0, services);
	}
	
	public DdeServer newOpenServer(int initializeFlags, String ... services) {
		DdeServer server = newServer(initializeFlags, services);
		server.setConnectionListener(new ConnectionAdapter() {
			public boolean onConnect(ConnectEvent event) {
				return true;
			}
		});
		return server;
	}
	
}