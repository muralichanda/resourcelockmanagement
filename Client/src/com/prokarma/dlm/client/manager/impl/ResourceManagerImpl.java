package com.prokarma.dlm.client.manager.impl;

import java.io.IOException;

import com.prokarma.dlm.client.loadbalancer.LoadBalancerCaller;
import com.prokarma.dlm.client.lock.Lock;
import com.prokarma.dlm.client.manager.ResourceManager;

public class ResourceManagerImpl implements ResourceManager {
	
	
	@Override
	public Lock getLock(String resource, String clientId, String time) throws Exception {
		Lock lock = getLoadBalancerCaller().callServer(resource, clientId, time);
		
		return lock;
	}

	@Override
	public Lock getLock(String resource, Long timeInSeconds) {
		return null;
	}
	
	@Override
	public String releaseLock(String resource) throws IOException, ClassNotFoundException {
		return getLoadBalancerCaller().releaseLock(resource);
	}
	
	private LoadBalancerCaller getLoadBalancerCaller()
	{
		if(loadBalancerCaller == null)
		{
			loadBalancerCaller = new LoadBalancerCaller();
		}
		
		return loadBalancerCaller;
	}
	
	
	private LoadBalancerCaller loadBalancerCaller;
	
	private static final long serialVersionUID = 6816776399658441716L;
	
}
