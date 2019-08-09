package com.prokarma.dlm.loadbalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class LoadBalancerServer {

	public static void main(String[] args) throws Exception {
		
		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.createContext("/loadbalancer", new LoadBalanceHandler());
		server.createContext("/activeservers", new ActiveServersResourceHandler());
		server.setExecutor(null);
		server.start();

		LoadBalancerServer loadBalancerServer = new LoadBalancerServer();
		loadBalancerServer.loadServersDetails();
		loadBalancerServer.initiateServerPings();
		
	}
	
	static class LoadBalanceHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException 
		{
			System.out.println("LoadBalanceHandler:");
            ObjectInputStream objIn = new ObjectInputStream(t.getRequestBody());
            String resourceId = null;
			try {
				resourceId = (String)objIn.readObject();
			} catch (ClassNotFoundException e) {
			
			}
            objIn.close();
            
            String url = getOrUpdateServerInstance(resourceId);
            
            url = url.substring(0, url.lastIndexOf("/"));
            
            t.sendResponseHeaders(200, url.length());
            OutputStream os = t.getResponseBody();
            os.write(url.getBytes());
            os.close();
		}
	}
	
	static class ActiveServersResourceHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException 
		{
			System.out.println("ActiveServersResourceHandler:");
			String url = null;
			List<String> list = getAllActiveServers();
			if(list == null)
			{
				url = "";
			}
			else
			{
				url = list.toString().replace("[", "").replace("]", "");
			}
			
            t.sendResponseHeaders(200, url.length());
            OutputStream os = t.getResponseBody();
            os.write(url.getBytes());
            os.close();
		}
	}
	
	private static String getOrUpdateServerInstance(String resourceId)
	{
		System.out.println("getOrUpdateServerInstance:");
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
		try {
			backupResourceData(resourceId, activeServers.get(count));
		} catch (Exception e) {
		
		}
		
		return activeServers.get(count);
	}
	
	
	private static List<String> getAllActiveServers()
	{
		System.out.println("getAllActiveServers:");
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
						
							int responseCode = getResponseCode(entry.getKey());
							
							if(responseCode == 200)
							{
								if(entry.getValue() != Boolean.TRUE)
								{
									backupServerInstanceData(entry.getKey(), "Y");
									notifyServerInstances(entry.getKey(), true);
								}
								
								serversMap.put(entry.getKey(), Boolean.TRUE);
							}
							
						}catch(Exception e)
						{
							if(entry.getValue() != Boolean.FALSE)
							{
								backupServerInstanceData(entry.getKey(), "N");
								notifyServerInstances(entry.getKey(), false);
								
								modifyResourceURLMap(entry.getKey());
								
								
							}
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
	
	private void modifyResourceURLMap(String url)
	{
		System.out.println("modifyResourceURLMap:");
		Iterator<Entry<String, String>> itr = resourceMap.entrySet().iterator();
		while (itr.hasNext()) 
		{
			Entry<String, String> entry = itr.next();
			if(entry.getValue() != null && entry.getValue().equals(url))
			{
				itr.remove();
				getOrUpdateServerInstance(entry.getKey());
			}
			
		}
	}
	
	private void backupServerInstanceData(String serverUrl, String flag) {

		try{
			System.out.println("backupServerInstanceData:");
			String url = "http://localhost:9000/updateserverdata";

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setDoInput(true);
	        con.setDoOutput(true);
	        con.setUseCaches(false);
	        con.setRequestMethod("POST");
			ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
			objOut.writeObject(serverUrl+","+flag);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			objOut.flush();
			objOut.close();	;
			// print result
			System.out.println(response.toString());
		}catch(Exception e)
		{
			
		}
	}
	
	private void notifyServerInstances(String serverUrl, boolean flag) {
		try{
			System.out.println("notifyServerInstances:");
			String url = "http://172.16.200.134:7999/serverInstanceUpdate";
	
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setDoInput(true);
	        con.setDoOutput(true);
	        con.setUseCaches(false);
	        con.setRequestMethod("POST");
			ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
			objOut.writeObject(serverUrl+","+flag);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			objOut.flush();
			objOut.close();	;
			System.out.println(response.toString());
		}catch(Exception e)
		{
			
		}
	}
	
	private static void backupResourceData(String resourceId, String serverUrl) throws Exception {
		System.out.println("backupResourceData:");
		String url = "http://localhost:9000/addresource";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("POST");
		ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
		objOut.writeObject(resourceId+","+serverUrl);
		

		// optional default is GET
		

		// add request header
		//con.setRequestProperty("User-Agent", USER_AGENT);

		//int responseCode = con.getResponseCode();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		objOut.flush();
		objOut.close();	;
		// print result
		System.out.println(response.toString());

	}
	
	private static int getResponseCode(String url) throws Exception
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
	
	public void loadServersDetails() throws IOException
	{
		System.out.println("loadServersDetails:");
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