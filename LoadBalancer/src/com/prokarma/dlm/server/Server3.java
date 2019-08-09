package com.prokarma.dlm.server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.prokarma.dlm.dto.ResourceDetailsDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Server3 {

	private static final int portNumber = 8003;
	
    public static void main(String[] args) throws Exception {
    	syncDataOnLoad();
		HttpServer server = HttpServer.create(new InetSocketAddress(portNumber), 0);
        server.createContext("/ping", new HeartBeatHandler());
        server.createContext("/lockresource", new ResourceLockHandler());
        server.createContext("/releaseresource", new ResourceReleaseHandler());
        server.createContext("/updateLockedResource", new UpdateLockedResourceHandler());
        server.createContext("/getAllLockedResources", new GetAllLockedResources());
        server.createContext("/clientNotAlive", new ClientNotAlive());
        server.createContext("/updateReleasedResource", new UpdateReleaseResourceHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        Server3 server1 = new Server3();
        server1.removeExpiredLocks();
    }
    
    static class UpdateReleaseResourceHandler implements HttpHandler {

		@Override
		public void handle(final HttpExchange t) throws IOException {
			try {
				System.out.println("UpdateReleaseResourceHandler:"+portNumber);
				ObjectInputStream os = new ObjectInputStream(t.getRequestBody());
				ResourceDetailsDto lockedResource = (ResourceDetailsDto)os.readObject();
				resourceDetailsmap.remove(lockedResource.getResourceId());
				t.sendResponseHeaders(200, 0);
				t.getResponseBody().close();
			} catch (Exception e) {
				try {
					String msg = "Failure:" + e.getMessage();
					t.sendResponseHeaders(500, msg.length());
					OutputStream response = t.getResponseBody();
					response.write(msg.getBytes());
					response.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}
    	
    }
    
    static class GetAllLockedResources implements HttpHandler {

		@Override
		public void handle(final HttpExchange t) throws IOException {
			try {
				System.out.println("GetAllLockedResources:"+portNumber);
				t.sendResponseHeaders(200, 0);
				ObjectOutputStream os = new ObjectOutputStream(t.getResponseBody());
				os.writeObject(resourceDetailsmap);
				os.flush();
				os.close();
			} catch (Exception e) {
				try {
					String msg = "Failure:" + e.getMessage();
					t.sendResponseHeaders(500, msg.length());
					OutputStream response = t.getResponseBody();
					response.write(msg.getBytes());
					response.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}
    	
    }
    
    static class ClientNotAlive implements HttpHandler {

		@Override
		public void handle(final HttpExchange t) throws IOException {
			try {
				System.out.println("ClientNotAlive:"+portNumber);
				ObjectInputStream objIn = new ObjectInputStream(t.getRequestBody());
                String clientId = null;
                String message = "failure";
                try {
    				clientId = (String)objIn.readObject();
    				if(clientId != null)
    				{
    					
    					if(resourceDetailsmap != null && !resourceDetailsmap.isEmpty())
            	    	{
            	    		Iterator<Entry<String,ResourceDetailsDto>> itr = resourceDetailsmap.entrySet().iterator();
            	    		
            	    		while(itr.hasNext())
            	    		{
            	    			Entry<String,ResourceDetailsDto> entry = itr.next();
            	    			ResourceDetailsDto resourceDetails = entry.getValue();
            	    			
            	    			if(resourceDetails != null && clientId.equals(resourceDetails.getClientId()))
            	    			{
            	    				itr.remove();
            	    			}
            	    		}
            	    	}
    					
    					message = "success";
    				}
    			} catch (ClassNotFoundException e) {
    				e.printStackTrace();
    			}
                objIn.close();
                t.sendResponseHeaders(200, message.length());
                OutputStream os = t.getResponseBody();
                os.write(message.getBytes());
                os.close();
			} catch (Exception e) {
				try {
					String msg = "Failure:" + e.getMessage();
					t.sendResponseHeaders(500, msg.length());
					OutputStream response = t.getResponseBody();
					response.write(msg.getBytes());
					response.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}
    	
    }
    
    static class UpdateLockedResourceHandler implements HttpHandler {

		@Override
		public void handle(final HttpExchange t) throws IOException {
			try {
				System.out.println("UpdateLockedResourceHandler:"+portNumber);
				ObjectInputStream os = new ObjectInputStream(t.getRequestBody());
				ResourceDetailsDto lockedResource = (ResourceDetailsDto)os.readObject();
				resourceDetailsmap.put(lockedResource.getResourceId(), lockedResource);
				t.sendResponseHeaders(200, 0);
				t.getResponseBody().close();
			} catch (Exception e) {
				try {
					String msg = "Failure:" + e.getMessage();
					t.sendResponseHeaders(500, msg.length());
					OutputStream response = t.getResponseBody();
					response.write(msg.getBytes());
					response.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}
    	
    }
    
    static class HeartBeatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "This is the response";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class ResourceLockHandler implements HttpHandler {
    	
        @Override
        public void handle(final HttpExchange t) throws IOException {
        	Thread thread = new Thread(){
        		@Override
        		public void run() {
        			super.run();
        			try{
        				System.out.println("ResourceLockHandler:"+portNumber);
        				String lockId = null;
        				ObjectInputStream objIn = new ObjectInputStream(t.getRequestBody());
                        String data = null;
                        try {
            				data = (String)objIn.readObject();
            				if(data != null)
            				{
            					String[] splitData = data.split(",");
            					
            					if(splitData != null)
            					{
            						if(splitData.length ==2)
            						{
            							lockId = lockResource(splitData[0],splitData[1],null);
            						}
            						else if(splitData.length ==3)
            						{
            							lockId = lockResource(splitData[0],splitData[1],splitData[2]);
            						}
            					}
            					
            				}
            			} catch (ClassNotFoundException e) {
            				e.printStackTrace();
            			}
                        objIn.close();
                        t.sendResponseHeaders(200, lockId.length());
                        OutputStream os = t.getResponseBody();
                        os.write(lockId.getBytes());
                        os.close();
        			}catch(Exception e)
        			{
        				e.printStackTrace();
        			}
        		}
        	};
        	
        	thread.start();
        }
    }
    
    static class ResourceReleaseHandler implements HttpHandler {
    	
        @Override
        public void handle(final HttpExchange t) throws IOException {
        	Thread thread = new Thread(){
        		@Override
        		public void run() {
        			super.run();
        			try{
        				System.out.println("ResourceReleaseHandler:"+portNumber);
        				ObjectInputStream objIn = new ObjectInputStream(t.getRequestBody());
                        String data = null;
                        String message = "failure";
                        try {
            				data = (String)objIn.readObject();
            				if(data != null)
            				{
            					releaseLock(data);
            					message = "success";
            				}
            			} catch (ClassNotFoundException e) {
            				e.printStackTrace();
            			}
                        objIn.close();
                        t.sendResponseHeaders(200, message.length());
                        OutputStream os = t.getResponseBody();
                        os.write(message.getBytes());
                        os.close();
        			}catch(Exception e)
        			{
        				e.printStackTrace();
        			}
        		}
        	};
        	
        	thread.start();
        }
    }
    

    private static String lockResource(String clientId, String resourceId,String timeInSeconds)
    {
    	System.out.println("lockResource:"+resourceId+":"+portNumber);
    	String lockId = null;
    	
    	if(!resourceDetailsmap.containsKey(resourceId))
    	{
    		Date expiryDate = null;
        	if(timeInSeconds != null)
        	{
        		Calendar calendar = Calendar.getInstance();
        		calendar.add(Calendar.SECOND, Integer.parseInt(timeInSeconds));
        		expiryDate = calendar.getTime();
        	}
        	ResourceDetailsDto resourceDetailsDto = new ResourceDetailsDto(clientId, resourceId, expiryDate);
    		
    		resourceDetailsmap.put(resourceId, resourceDetailsDto);
    		notifyResourceAddition(resourceDetailsDto);
    		lockId = getLockId();
    	}
    	else
    	{
        	ResourceDetailsDto resourceDetailsDto = new ResourceDetailsDto(clientId, resourceId, timeInSeconds);
    		
    		resourceQueue.add(resourceDetailsDto);
    		lockId = waitForLock(resourceDetailsDto);
    	}
    	
    	return lockId;
    }
    
    private static void notifyResourceAddition(ResourceDetailsDto resourceDetailsDto) {

    	try{
    		System.out.println("notifyResourceAddition:"+portNumber);
    		String url = "http://172.16.200.134:7999/notifyLockedResource";
    		URL obj = new URL(url);
    		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    		con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("POST");
    		ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
    		try{
				resourceDetailsDto.setCallingServerUrl(InetAddress.getLocalHost().getHostAddress() + ":" + portNumber);
			}catch(Exception e){
				e.printStackTrace();
			}
    		objOut.writeObject(resourceDetailsDto);
    		
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
    
    private static void syncDataOnLoad() {

    	try{
    		System.out.println("syncDataOnLoad:"+portNumber);
    		String url = "http://172.16.200.134:7999/allDataSync";
    		URL obj = new URL(url);
    		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    		con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("POST");
    		ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
    		objOut.writeObject("");
    		
    		ObjectInputStream in = new ObjectInputStream(con.getInputStream());
    		resourceDetailsmap = (Map<String, ResourceDetailsDto>) in.readObject();
    		in.close();
    		objOut.flush();
    		objOut.close();	;
    	}catch(Exception e)
    	{
    		
    	}
	}
  
    
    private static String waitForLock(ResourceDetailsDto resourceDetailsDto)
    {
    	System.out.println("waitForLock:"+portNumber);
    	while(true)
		{
			if(!resourceQueue.contains(resourceDetailsDto))
	    	{
				System.out.println("4");
				return getLockId();
	    	}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    private static void releaseLock(String resourceId)
    {
    	System.out.println("releaseLock:"+portNumber);
    	if(resourceDetailsmap != null && resourceDetailsmap.containsKey(resourceId))
    	{
    		ResourceDetailsDto resourceDetailsDto = resourceDetailsmap.get(resourceId);
    		resourceDetailsmap.remove(resourceId);
			assignLockToNewResource(resourceDetailsDto);
			try{
				resourceDetailsDto.setCallingServerUrl(InetAddress.getLocalHost().getHostAddress() + ":" + portNumber);
			}catch(Exception e){
				e.printStackTrace();
			}
    		notifyResourceHandlerToRelaseTheResource(resourceDetailsDto);
    	}
    }
    
    private static void notifyResourceHandlerToRelaseTheResource(
			ResourceDetailsDto resourceDetailsDto) {
    	try{
    		System.out.println("notifyResourceHandlerToRelaseTheResource:"+portNumber);
    		String url = "http://172.16.200.134:7999/releaseLockedResource";
			
			URL obj = new URL(url);
			
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setDoInput(true);
	        con.setDoOutput(true);
	        con.setUseCaches(false);
	        con.setRequestMethod("POST");
	        
	        ObjectOutputStream oStream = new ObjectOutputStream(con.getOutputStream());
	        oStream.writeObject(resourceDetailsDto);
	        
			int responseCode = con.getResponseCode();
			
			if(responseCode != 200){
				throw new Exception("Response code for LockedResourceUpdate is : " + responseCode);
			}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}

	private void removeExpiredLocks()
    {
    	Thread thread = new Thread(){
    		@Override
    		public void run() {
    			super.run();
    			while(true)
				{
    				if(resourceDetailsmap != null && !resourceDetailsmap.isEmpty())
        	    	{
        	    		Calendar calendar = Calendar.getInstance();
        	    		Iterator<Entry<String,ResourceDetailsDto>> itr = resourceDetailsmap.entrySet().iterator();
        	    		
        	    		while(itr.hasNext())
        	    		{
        	    			Entry<String,ResourceDetailsDto> entry = itr.next();
        	    			ResourceDetailsDto resourceDetails = entry.getValue();
        	    			
        	    			if(resourceDetails != null && resourceDetails.getExpiryTime() != null && 
        	    					calendar.getTime().after(resourceDetails.getExpiryTime()))
        	    			{
        	    				itr.remove();
        	    				notifyResourceHandlerToRelaseTheResource(resourceDetails);
        	    				assignLockToNewResource(resourceDetails);
        	    			}
        	    		}
        	    	}
    				try {
    					Thread.sleep(20);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
				}
    		}
    	};
    	
    	thread.start();
    	
    }
    
    private static void assignLockToNewResource(ResourceDetailsDto resourceDetails)
    {
    	System.out.println("assignLockToNewResource:"+portNumber);
    	if(resourceQueue != null)
    	{
    		Iterator<ResourceDetailsDto> itr = resourceQueue.iterator();
    		while (itr.hasNext())
    		{
    			ResourceDetailsDto resourceDetailsDto = itr.next();
    			if(resourceDetails == null || (resourceDetailsDto != null && resourceDetails != null && resourceDetailsDto.getResourceId().equals(resourceDetails.getResourceId())))
    			{
    				if(resourceDetailsDto.getExpirySeconds() != null)
    				{
    					Calendar calendar = Calendar.getInstance();
        				calendar.add(Calendar.SECOND, Integer.parseInt(resourceDetailsDto.getExpirySeconds()));
        				resourceDetailsDto.setExpiryTime(calendar.getTime());
    				}
    				
    				resourceDetailsmap.put(resourceDetailsDto.getResourceId(), resourceDetailsDto);
    				itr.remove();
    				break;
    			}
			}
    	}
    }
    
    private static String getLockId()
    {
    	System.out.println("getLockId:"+portNumber);
    	UUID uuid = UUID.randomUUID();
    	
    	return uuid.toString();
    }

    private static List<ResourceDetailsDto> resourceQueue = new ArrayList<ResourceDetailsDto>();
    
    private static Map<String,ResourceDetailsDto> resourceDetailsmap = new HashMap<String, ResourceDetailsDto>();
}

