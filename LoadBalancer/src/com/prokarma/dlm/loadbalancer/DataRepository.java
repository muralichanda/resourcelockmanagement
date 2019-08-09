package com.prokarma.dlm.loadbalancer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class DataRepository{
	
	private static final Properties properties = new Properties();
	
	public static void loadServersDetails(Map<String,Boolean> serversMap) throws IOException
	{
		
		InputStream inputStream = new FileInputStream("/common.properties");
		if(inputStream != null)
		{
			properties.load(inputStream);
		}
		if(properties != null)
		{
			String[] urls = properties.getProperty("server.urls").split(",");
			
			for (String url : urls) {
				serversMap.put(url, Boolean.TRUE);
			}
		}
	}
	
	public static String getProperty(String key){
		return properties.getProperty(key);
	}
}