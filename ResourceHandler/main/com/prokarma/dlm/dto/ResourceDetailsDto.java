package com.prokarma.dlm.dto;

import java.io.Serializable;
import java.util.Date;

public class ResourceDetailsDto implements Serializable
{
	private static final long serialVersionUID = 1L;

	public ResourceDetailsDto(String clientId,String resourceId,Date expiryTime)
	{
		setClientId(clientId);
		setResourceId(resourceId);
		setExpiryTime(expiryTime);
	}
	
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Date getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(Date expiryTime) {
		this.expiryTime = expiryTime;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getCallingServerUrl() {
		return callingServerUrl;
	}

	public void setCallingServerUrl(String callingServerUrl) {
		this.callingServerUrl = callingServerUrl;
	}

	private String resourceId;
	
	private String clientId;
	
	private Date expiryTime;
	
	private String callingServerUrl;
}