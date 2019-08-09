package com.prokarma.dlm.client.lock.impl;

import com.prokarma.dlm.client.lock.Lock;

public class LockImpl implements Lock {
	
	
	@Override
	public String getLockId() {
		return lockId;
	}

	
	public void setLockId(String lockId) {
		this.lockId = lockId;
	}
	
	
	@Override
	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}


	@Override
	public String getResource() {
		return resource;
	}


	public void setResource(String resource) {
		this.resource = resource;
	}



	@Override
	public String getTime() {
		return time;
	}


	public void setTime(String time) {
		this.time = time;
	}


	@Override
	public boolean isReleased() {
		return released;
	}


	public void setReleased(boolean released) {
		this.released = released;
	}
	
	@Override
	public boolean isAquiredLock()
	{
		if(getLockId() == null)
		{
			return false;
		}
		
		return true;
	}



	@Override
	public String toString() {
		return "LockImpl [lockId=" + lockId + ", serverURL=" + serverURL
				+ ", resource=" + resource + ", time=" + time + ", released="
				+ released + "]";
	}



	private String lockId;
	private String serverURL;
	private String resource;
	private String time;
	private boolean released = false;
	
	
	private static final long serialVersionUID = 5607199725716180489L;
	
}
