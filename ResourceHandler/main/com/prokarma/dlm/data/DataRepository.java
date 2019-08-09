package com.prokarma.dlm.data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.prokarma.dlm.util.AbstractThread;

public class DataRepository {
	
	private static final Properties properties = new Properties();
	
	private static final Map<String, Date> clientDetails = new HashMap<String, Date>();
	
	private static final Set<String> serverInstanceDetails = new HashSet<String>();
	
	static{
		loadProperties();
		getAllActiveServers();
		activateClientAliveInspector();
	}
	
	public static Set<String> getAllActiveServerInstance(){
		if(serverInstanceDetails.isEmpty()){
			getAllActiveServers();
		}
		Set<String> activeServers = new HashSet<String>();
		activeServers.addAll(serverInstanceDetails);
		return activeServers;
	}

	private static void loadProperties() {
		InputStream ioStream;
		try {
			ioStream = new FileInputStream("config/common.properties");
			properties.load(ioStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void activateClientAliveInspector() {
		Thread t = new AbstractThread() {
			@Override
			public void run() {
				while(true){
					try{
						Thread.sleep(500);
					}catch(InterruptedException e){
						e.printStackTrace();
					}
					int seconds = Integer.parseInt(properties.getProperty("clientNotAliveSeconds"));
					long currentMillis = System.currentTimeMillis();
					Iterator<Entry<String, Date>> it = clientDetails.entrySet().iterator();
					while(it.hasNext()){
						Entry<String, Date> entry = it.next();
						long aliveMillis = entry.getValue().getTime();
						long secondsAliveTill = (currentMillis - aliveMillis)/1000;
						if(secondsAliveTill >= seconds){
							boolean success = true;
							System.out.println("Closing Client : " + entry.getKey());
							Set<String> activeServersToNotify = DataRepository.getAllActiveServerInstance();
							for(String server:activeServersToNotify){
								try{
									String url = server + DataRepository.getProperty("clientNotAlive");
									URL obj = new URL(url);
									
									HttpURLConnection con = (HttpURLConnection) obj.openConnection();
									con.setDoInput(true);
							        con.setDoOutput(true);
							        con.setUseCaches(false);
							        con.setRequestMethod("POST");
							        
							        ObjectOutputStream oStream = new ObjectOutputStream(con.getOutputStream());
							        oStream.writeObject(entry.getKey());
							        
									int responseCode = con.getResponseCode();
									System.out.println("Sending notification to : " + server + " - Response(" + responseCode + ")");
									if(responseCode != 200){
										System.out.println("Client Expiry updation failed in server : " + server);
										success = false;
									}
								}catch(Exception e){
									e.printStackTrace();
								}
							}
							if(!success){
								System.out.println("Client(" + entry.getKey() +") updation failed in one or more server(s) will retry in 500ms");
							}else{
								it.remove();
							}
						}
					}
				}
			}
			
		};
		t.start();
	}

	private static void getAllActiveServers() {
		try{
			serverInstanceDetails.clear();
			String url = "http://" + DataRepository.getProperty("loadBalanceURL") + DataRepository.getProperty("activeServersContext");
	
			URL obj = new URL(url);
			
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setDoInput(true);
	        con.setDoOutput(true);
	        con.setUseCaches(false);
	        con.setRequestMethod("POST");
	        
			int responseCode = con.getResponseCode();
			
			if(responseCode != 200){
				throw new Exception("Response code for activeservers is : " + responseCode);
			}
	
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			System.out.println("Got ActiveServers from the LoadBalancer : " + response);
			String[] serverInstances = response.toString().split(",");
			for(String serverInstance:serverInstances){
				serverInstance = serverInstance.replaceAll("/ping", "");
				addServerInstance(serverInstance.trim());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static String getProperty(String key){
		return properties.getProperty(key);
	}
	
	public static void updateOrAddClient(String client){
		clientDetails.put(client, new Date());
	}
	
	public static boolean addServerInstance(String serverInstance){
		System.out.println("Available ServerInstances : " + serverInstanceDetails);
		return serverInstanceDetails.add(serverInstance);
	}
	
	public static boolean removeServerInstance(String serverInstance){
		System.out.println("Available ServerInstances : " + serverInstanceDetails);
		return serverInstanceDetails.remove(serverInstance);
	}

}
