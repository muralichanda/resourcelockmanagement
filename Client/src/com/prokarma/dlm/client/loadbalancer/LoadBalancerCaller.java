package com.prokarma.dlm.client.loadbalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

import com.prokarma.dlm.client.lock.Lock;
import com.prokarma.dlm.client.lock.Resource;
import com.prokarma.dlm.client.lock.impl.LockImpl;

public class LoadBalancerCaller implements Serializable
{
	
	//private final String USER_AGENT = "Mozilla/5.0";
	
	public static void main(String[] args) throws Exception
	{
		LoadBalancerCaller http = new LoadBalancerCaller();
		System.out.println("Testing 1 - Send Http GET request");
		System.out.println(http.callServer("Data226", "12", null));
		System.out.println("\nTesting 2 - Send Http POST request");
	}
	
	public String releaseLock(String resource) throws IOException, ClassNotFoundException
	{
		
		String loadBalancerUrl = "http://172.16.203.107:8000/loadbalancer";
		String resourceURL = "/releaseresource";
	
		StringBuffer response = callURL(loadBalancerUrl, resource);
		String serverURL = response.toString();
		serverURL = serverURL + resourceURL;
		
		return callReleaseServer(serverURL , resource);
	}
	
	public Lock callServer(String resource, String clientId, String time) throws Exception
	{
		
		LockImpl lock = new LockImpl();
		
		String loadBalancerUrl = "http://172.16.203.107:8000/loadbalancer";
		String resourceURL = "/lockresource";
	
		StringBuffer response = callURL(loadBalancerUrl, resource);
		String serverURL = response.toString();
		serverURL = serverURL + resourceURL;
		lock.setServerURL(serverURL);
		System.out.println(serverURL);
		if(serverURL != null && serverURL.contains("http:"))
		{
			Lock responseLock = pingServer(serverURL, resource, clientId, time);
			if(responseLock != null)
			{
				((LockImpl)responseLock).setServerURL(serverURL);
				return responseLock;
			}
		}
		
		return lock;

	}
	
	private String callReleaseServer(String url, String resource) throws IOException, ClassNotFoundException
	{
		URL obj = new URL(url);
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	
		con.setDoInput(true);
	
        con.setDoOutput(true);

        con.setUseCaches(false);

        con.setRequestMethod("POST");
	
		ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
	
		
		objOut.writeObject(resource);
		//con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		
		/*ObjectInputStream inputStream = new ObjectInputStream(con.getInputStream());
		Object responseObject = inputStream.readObject();*/
	
		String inputLine;
	
		StringBuffer response = new StringBuffer();
	
		while ((inputLine = in.readLine()) != null) {
	
		response.append(inputLine);
	
		}
		String lockKey = response.toString();
		
		
		objOut.flush();
		objOut.close();
		
		return lockKey;
	}
	
	private Lock pingServer(String url, String resource, String clientId, String time) throws IOException, ClassNotFoundException
	{
		URL obj = new URL(url);
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	
		con.setDoInput(true);
	
        con.setDoOutput(true);

        con.setUseCaches(false);

        con.setRequestMethod("POST");
	
		ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
	
		//objOut.writeObject("R3");
		String completeResource = clientId+","+resource;
		if(time != null)
		{
			completeResource = completeResource + "," + time;
		}
		objOut.writeObject(completeResource);
		//con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		
		/*ObjectInputStream inputStream = new ObjectInputStream(con.getInputStream());
		Object responseObject = inputStream.readObject();*/
	
		String inputLine;
	
		StringBuffer response = new StringBuffer();
	
		while ((inputLine = in.readLine()) != null) {
	
		response.append(inputLine);
	
		}
		String lockKey = response.toString();
		System.out.println(lockKey+"-"+resource);
		LockImpl impl = new LockImpl();
		impl.setLockId(lockKey);
		impl.setResource(resource);
		impl.setTime(time);
		
		objOut.flush();
		objOut.close();
		
		return impl;
	}
	
	private StringBuffer callURL(String url, String resource) throws IOException, ClassNotFoundException
	{
		URL obj = new URL(url);
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	
		con.setDoInput(true);
	
        con.setDoOutput(true);

        con.setUseCaches(false);

        con.setRequestMethod("POST");
	
		ObjectOutputStream objOut = new ObjectOutputStream(con.getOutputStream());
	
		//objOut.writeObject("R3");
		objOut.writeObject(resource);
		
		// add request header
	
		//con.setRequestProperty("User-Agent", USER_AGENT);
		int responseCode = con.getResponseCode();
	
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		
		/*ObjectInputStream inputStream = new ObjectInputStream(con.getInputStream());
		Object responseObject = inputStream.readObject();*/
	
		String inputLine;
	
		StringBuffer response = new StringBuffer();
	
		while ((inputLine = in.readLine()) != null) {
	
		response.append(inputLine);
	
		}
	
		in.close();
		objOut.flush();
		objOut.close();
		
		return response;
	}
	
	
	
	private static final long serialVersionUID = -3145800882669633395L;
	
}
