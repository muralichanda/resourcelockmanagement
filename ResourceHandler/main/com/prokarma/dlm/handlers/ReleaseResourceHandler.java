package com.prokarma.dlm.handlers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

import com.prokarma.dlm.data.DataRepository;
import com.prokarma.dlm.dto.ResourceDetailsDto;
import com.prokarma.dlm.util.AbstractThread;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ReleaseResourceHandler implements HttpHandler {

	@Override
	public void handle(final HttpExchange he) throws IOException {
		AbstractThread thread = new AbstractThread() {
			public void run() {
				try {
					ObjectInputStream requestBody = new ObjectInputStream(he.getRequestBody());
					ResourceDetailsDto lockedResource = (ResourceDetailsDto)requestBody.readObject();
					
					System.out.println("Notification of Resource Lock released from(" + lockedResource.getCallingServerUrl() + ") : " + lockedResource.getResourceId() + " - " + lockedResource.getClientId());
					
					Set<String> activeServersToNotify = DataRepository.getAllActiveServerInstance();
					for(String server:activeServersToNotify){
						if(lockedResource.getCallingServerUrl() != null && server.contains(lockedResource.getCallingServerUrl())){
							continue;
						}
						try{
							String url = server + DataRepository.getProperty("releaseResourceURL");
							URL obj = new URL(url);
							
							HttpURLConnection con = (HttpURLConnection) obj.openConnection();
							con.setDoInput(true);
					        con.setDoOutput(true);
					        con.setUseCaches(false);
					        con.setRequestMethod("POST");
					        
					        ObjectOutputStream oStream = new ObjectOutputStream(con.getOutputStream());
					        oStream.writeObject(lockedResource);
					        
							int responseCode = con.getResponseCode();
							System.out.println("Sending notification to : " + server + " - Response(" + responseCode + ")");
							if(responseCode != 200){
								throw new Exception("Response code for LockedResourceUpdate is : " + responseCode);
							}
						}catch(Exception e){
							e.printStackTrace();
						}
					}
					he.sendResponseHeaders(200, 0);
					he.getResponseBody().close();
				} catch (Exception e) {
					try {
						String msg = "Failure:" + e.getMessage();
						he.sendResponseHeaders(500, msg.length());
						OutputStream response = he.getResponseBody();
						response.write(msg.getBytes());
						response.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
			}
		};
		
		thread.start();
	}

}
