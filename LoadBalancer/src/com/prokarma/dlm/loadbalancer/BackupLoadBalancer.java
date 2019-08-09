package com.prokarma.dlm.loadbalancer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class BackupLoadBalancer {

	public static void main(String[] args) throws Exception {
		
		HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
		server.createContext("/backuploadbalancer", new LoadBalanceHandler());
		server.createContext("/addresource", new AddResourceHandler());
		server.createContext("/updateserverdata", new UpdateServerDataHandler());
		server.setExecutor(null);
		server.start();

		BackupLoadBalancer loadBalancerServer = new BackupLoadBalancer();
		loadBalancerServer.loadServersDetails();
	}
	
	static class LoadBalanceHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException 
		{
            ObjectInputStream objIn = new ObjectInputStream(t.getRequestBody());
            String resourceId = null;
			try {
				resourceId = (String)objIn.readObject();
			} catch (ClassNotFoundException e) {
			
			}
            objIn.close();
            
            String url = getServerInstance(resourceId);
            t.sendResponseHeaders(200, url.length());
            OutputStream os = t.getResponseBody();
            os.write(url.getBytes());
            os.close();
		}
	}
	
	private static String getServerInstance(String resourceId)
	{
		if(resourceMap.containsKey(resourceId))
		{
			return resourceMap.get(resourceId);
		}
		
		
		List<String> activeServers = getAllActiveServers();
		
		if(activeServers == null || activeServers.isEmpty())
		{
			return "no active server found";
		}
		
		count++;
		if(count >= activeServers.size())
		{
			count = 0;
		}
		
		resourceMap.put(resourceId, activeServers.get(count));
		return activeServers.get(count);
	}
	
	static class AddResourceHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException 
		{
			ObjectInputStream objIn = new ObjectInputStream(t.getRequestBody());
            String data = null;
            String message = "failure";
            try {
				data = (String)objIn.readObject();
				if(data != null)
				{
					String[] splitData = data.split(",");
					
					if(splitData != null && splitData.length ==2)
					{
						addResource(splitData[0],splitData[1]);
						message = "success";
					}
				}
			} catch (ClassNotFoundException e) {
			
			}
            objIn.close();
            
            
            t.sendResponseHeaders(200, message.length());
            OutputStream os = t.getResponseBody();
            os.write(message.getBytes());
            os.close();
		}
	}
	
	static class UpdateServerDataHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException 
		{
			ObjectInputStream objIn = new ObjectInputStream(t.getRequestBody());
            String data = null;
            String message = "failure";
            try {
				data = (String)objIn.readObject();
				if(data != null)
				{
					String[] splitData = data.split(",");
					
					if(splitData != null && splitData.length ==2)
					{
						updateServerData(splitData[0],splitData[1]);
						message = "success";
					}
				}
			} catch (ClassNotFoundException e) {
			
			}
            objIn.close();
            
            
            t.sendResponseHeaders(200, message.length());
            OutputStream os = t.getResponseBody();
            os.write(message.getBytes());
            os.close();
		}
	}
	
	private static void addResource(String resourceId,String url)
	{
		if(!resourceMap.containsKey(resourceId))
		{
			resourceMap.put(resourceId, url);
		}
	}
	
	private static void updateServerData(String url,String flag)
	{
		serversMap.put(url,"Y".equals(flag));
	}
	
	private static List<String> getAllActiveServers()
	{
		List<String> activeServersList = new ArrayList<String>();
		for (Entry<String, Boolean> entry : serversMap.entrySet()) {
			
			
			if(entry.getValue())
			{
				activeServersList.add(entry.getKey());
			}
		}
		
		return activeServersList;
	}
	
	private void initiateServerPings()
	{
		Thread thread = new Thread(){
			@Override
			public void run() {
				super.run();
				while(true)
				{
					for (Entry<String, Boolean> entry : serversMap.entrySet()) {
						try{
						
							int responseCode = sendGet(entry.getKey());
							
							if(responseCode == 200)
							{
								serversMap.put(entry.getKey(), Boolean.TRUE);
							}
							
						}catch(Exception e)
						{
							serversMap.put(entry.getKey(), Boolean.FALSE);
						}
					}
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						
					}
				}
			}
		};
		thread.start();
	}
	
	private static int sendGet(String url) throws Exception
	{
		URL obj = new URL(url);
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
		
        ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
		objOut.writeObject(obj);
		
		int responseCode = con.getResponseCode();
		
		objOut.flush();
		objOut.close();	;
		
		return responseCode;
	}
	
	private void loadServersDetails() throws IOException
	{
		Properties properties = new Properties();
		
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("common.properties");
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
	private static int count=0;
	private static Map<String,Boolean> serversMap = new HashMap<String,Boolean>();
	private static Map<String,String> resourceMap = new HashMap<String,String>();



}
