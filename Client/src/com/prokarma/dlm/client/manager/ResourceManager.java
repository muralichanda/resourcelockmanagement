package com.prokarma.dlm.client.manager;

import java.io.IOException;
import java.io.Serializable;

import com.prokarma.dlm.client.lock.Lock;

public interface ResourceManager extends Serializable {
	
	Lock getLock(String resource, String clientId, String time) throws Exception;
	
	Lock getLock(String resource, Long timeInSeconds);
	
	String releaseLock(String resource) throws IOException, ClassNotFoundException;
	
}
