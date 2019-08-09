package com.prokarma.dlm.client.resource;

import java.util.concurrent.Callable;

import com.prokarma.dlm.client.lock.Lock;
import com.prokarma.dlm.client.lock.impl.LockImpl;
import com.prokarma.dlm.client.manager.ResourceManager;
import com.prokarma.dlm.client.manager.impl.ResourceManagerImpl;

public class CallableTask implements Callable<Lock> {
	
	private String resource;
	private String clientId;
	private String time;
	
	public CallableTask(String resource, String clientId, String time)
	{
		this.resource = resource;
		this.clientId = clientId;
		this.time = time;
	}

	@Override
	public Lock call() {
		ResourceManager resourceManager = new ResourceManagerImpl();
		Lock lock = null;
		try {
			lock = resourceManager.getLock(resource, clientId, time);
		} catch (Exception e) {
			LockImpl errorLock = new LockImpl();
			errorLock.setResource(resource);
			errorLock.setTime(time);
			return errorLock;
		}
		return lock;
	}

}
