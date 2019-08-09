package com.prokarma.dlm.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.prokarma.dlm.client.lock.Lock;
import com.prokarma.dlm.client.lock.impl.LockImpl;
import com.prokarma.dlm.client.manager.ResourceManager;
import com.prokarma.dlm.client.manager.impl.ResourceManagerImpl;
import com.prokarma.dlm.client.resource.CallableTask;


public class GetResourceLockMain {
	
	private boolean running = true;
	
	private static Long captureTimeInput(Scanner in)
	{
		System.out.println("Please enter resource time(seconds) you want to get lock on resorce. Enter 0(ZERO) for infinite");
		String time = in.next();
		Long timeInSeconds = null;
		if(time != null && time.trim().length() > 0)
    	{
    		try{
    			timeInSeconds = Long.parseLong(time.trim());
    		}catch(Exception exception)
    		{
    			System.out.println("Invalid time.");
    			timeInSeconds = captureTimeInput(in);
    		}
    	}
		
		return timeInSeconds;
	}
	
	private static String captureResource(Scanner in)
	{
		String resource = in.next();
		
		if(resource == null || resource.trim().length() == 0)
    	{
			System.out.println("Invalid resource. Please enter resource key.");
			System.out.println("Please enter resource key.");
    		resource = captureResource(in);
    	}
		else if(resource.trim().length() > 10)
		{
			System.out.println("Resource can not be more than 10 characters. Please enter resource key.");
			System.out.println("Please enter resource key.");
    		resource = captureResource(in);
		}
		
		return resource.trim().toUpperCase();
	}
	
	public static void main(String[] args) {
		//printOptions();
		System.out.println("Please enter resource number and time(seconds) you want to get lock on resorce.");
		Scanner in = new Scanner(System.in);
		boolean resourceCapture = true;
		Map<String, Long> resourceMap = new HashMap<String, Long>();
		System.out.println("Enter x to exit.");
		System.out.println("Please enter D, if you are done entering resources...");
		while(resourceCapture)
		{	
			System.out.println("Please enter resource key.");
	       /* if(!in.hasNext())
	        {*/
	        	String resource = captureResource(in);
	        	Long timeInSeconds = captureTimeInput(in);
	        	if(timeInSeconds == 0)
	        	{
	        		timeInSeconds = null;
	        	}
	        	resourceMap.put(resource, timeInSeconds);
	        	System.out.println("Resource is: "+resource);
	        	System.out.println("Time is: "+timeInSeconds);
	        	System.out.println("Enter X -- Exit.");
				System.out.println("Enter D -- if you are done entering resources...");
				System.out.println("Any key -- if you want to enter more resources...");
	        	String exitKey = in.next();
	            if(exitKey.equalsIgnoreCase("X"))
	            {
	                System.exit(0);
	            }
	            else if(exitKey.equalsIgnoreCase("D"))
	            {
	            	resourceCapture = false;
	            }                    
	        }
		/*}*/
		
		
		String clientId = getClientId();
		List<CallableTask> callableTasks = new ArrayList<CallableTask>();
		for(Entry<String, Long> entry : resourceMap.entrySet())
		{
			String resource = entry.getKey();
			Long time = entry.getValue();
			String timeValue = time != null ? String.valueOf(time) : null;
			CallableTask task = new CallableTask(resource, clientId, timeValue);
			callableTasks.add(task);
		}
		
		GetResourceLockMain lockMain = new GetResourceLockMain();
		lockMain.initiateServerPings(clientId);
		
		
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		
		List<Future<Lock>> futures = null;
		try {
			futures = executorService.invokeAll(callableTasks);
		} catch (InterruptedException e1) {
			System.out.println("Interrupted while getting lock for resources.");
		}
		
		List<Lock> responseLocks = new ArrayList<Lock>();
		
		if(futures != null)
		{
			for(Future<Lock> future : futures){
				Lock lock = null;
				try {
					lock = future.get();
					if(lock!= null)
					{
						if(lock.isAquiredLock())
						{
							responseLocks.add(lock);
							handleRelease(lockMain, lock, in);
						}
						else
						{
							System.out.println("Could not aquire lock for resource: "+lock.getResource());
						}
					}
				} catch (InterruptedException e) {
					System.out.println("Interrupted while getting lock for resource: "+e.getMessage());
				} catch (ExecutionException e) {
					System.out.println("Interrupted while getting lock for resource: "+e.getMessage());
				}
			    System.out.println("future.get = " + lock);
			}
		}
		else
		{
			System.out.println("Could not invoke getlock service.");
		}
		
		lockMain.checkAndPromptAndReleaseLock(responseLocks, in);
		
		executorService.shutdown();
		lockMain.running = false;
	}
	
	private void checkAndPromptAndReleaseLock(List<Lock> responseLocks, Scanner in)
	{
		for(Iterator<Lock> itr = responseLocks.iterator(); itr.hasNext();)
		{
			Lock lock = itr.next();
			if(lock.isReleased())
			{
				itr.remove();
			}
			else
			{
				promptAndReleaseLock(lock, in);
			}
		}
		
		if(responseLocks.size() > 0)
		{
			checkAndPromptAndReleaseLock(responseLocks, in);
		}
	}
	
	private static void handleRelease(final GetResourceLockMain lockMain, final Lock lock, final Scanner in)
	{
		Thread thread = new Thread() {
			@Override
			public void run() {
				super.run();
				lockMain.promptAndReleaseLock(lock, in);
				/*System.out.println("Enter Y to release resource: "+ lock.getResource() + " or any other key to keep running it.");
				String release = in.next();
				if(release != null && release.trim().equalsIgnoreCase("Y"))
				{
					String released = lockMain.releaseLock(lock.getResource());
					if(released != null && released.trim().equalsIgnoreCase("success"))
					{
						((LockImpl)lock).setReleased(true);
					}
				}*/
			}

		};

		thread.start();

	}
	
	private void promptAndReleaseLock(final Lock lock, final Scanner in)
	{
		System.out.println("Enter Y to release resource: "+ lock.getResource() + " or any other key to keep running it.");
		String release = in.next();
		if(release != null && release.trim().equalsIgnoreCase("Y"))
		{
			String released = releaseLock(lock.getResource());
			if(released != null && released.trim().equalsIgnoreCase("success"))
			{
				((LockImpl)lock).setReleased(true);
			}
		}
	}
	
	private String releaseLock(String resource)
	{
		ResourceManager resourceManager = getResourceManager();
		try {
			String releaseResponse = resourceManager.releaseLock(resource);
			System.out.println("ReleaseResponse for resource "+resource+ "is: "+releaseResponse);
			return releaseResponse;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static String getClientId()
	{
		UUID idOne = UUID.randomUUID();
		return idOne.toString();
	}
	
	/*private Lock getLock(String resource, String clientId)
	{
		ResourceManager resourceManager = getResourceManager();
		try {
			return resourceManager.getLock(resource, clientId, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}*/
	
	
	
	private void initiateServerPings(final String clientId)
	{
		Thread thread = new Thread() {
			@Override
			public void run() {
				super.run();
				while (running)
				{
					try {
						URL obj = new URL("http://172.16.200.134:7999/iAmAlive");
						HttpURLConnection con = (HttpURLConnection) obj
								.openConnection();
						con.setDoInput(true);
						con.setDoOutput(true);
						con.setUseCaches(false);
						con.setRequestMethod("POST");
						ObjectOutputStream objOut = new ObjectOutputStream(
								con.getOutputStream());
						//objOut.writeObject("R3");
						objOut.writeObject(clientId);
						//con.setRequestProperty("User-Agent", USER_AGENT);
						//System.out.println("Pinging client: "+clientId);
						int responseCode = con.getResponseCode();
						
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					
						String inputLine;
					
						StringBuffer response = new StringBuffer();
					
						while ((inputLine = in.readLine()) != null) {
							response.append(inputLine);
						}
						//System.out.println("Ping response: "+response);
						in.close();
						
						//inputStream.close();
						objOut.flush();
						objOut.close();
					} catch (Exception e) {
						System.out.println("Error while client ping...");
					}
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						System.out.println("Error while client sleep...");
					}

				}

			}

		};

		thread.start();

	}

	
	public ResourceManager getResourceManager() {
		if(resourceManager == null)
		{
			resourceManager = new ResourceManagerImpl();
		}
		return resourceManager;
	}
	
	private ResourceManager resourceManager;
	
}
