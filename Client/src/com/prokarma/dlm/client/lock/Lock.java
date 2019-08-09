package com.prokarma.dlm.client.lock;

import java.io.Serializable;

public interface Lock extends Serializable {

	String getLockId();

	String getServerURL();

	String getResource();

	String getTime();

	boolean isReleased();

	boolean isAquiredLock();
	
}
