package com.prokarma.dlm.client.lock;

public enum Resource {
	
	PRINTER("PRINTER"),
	FILE("FILE"),
	DATABASE("DATABASE"),
	FOLDER("FOLDER");
	
	Resource(String resourceName)
	{
		this.resourceName = resourceName;
	}
	
	public String getResourceName() {
		return resourceName;
	}

	private String resourceName = null;
}
