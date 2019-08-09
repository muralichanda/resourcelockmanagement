package com.prokarma.dlm.handlers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.prokarma.dlm.data.DataRepository;
import com.prokarma.dlm.dto.ResourceDetailsDto;
import com.prokarma.dlm.util.AbstractThread;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AllDataSyncHandler implements HttpHandler {

	@Override
	public void handle(final HttpExchange he) throws IOException {
		AbstractThread thread = new AbstractThread() {
			@SuppressWarnings("unchecked")
			public void run() {
				try {
					System.out.println("GetAllLockedResources received : ");
					Map<String, ResourceDetailsDto> retObj = new HashMap<String, ResourceDetailsDto>();
					Set<String> activeServersToNotify = DataRepository.getAllActiveServerInstance();
					for(String server:activeServersToNotify){
						String url = server + DataRepository.getProperty("getAllLockedResources");
					    URL obj = new URL(url);
					    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
					    int responseCode = con.getResponseCode();
					   
						
						if(responseCode != 200){
							System.out.println("Trying to get all locked resources from : " + server + " - Response(" + responseCode + "), Hence further servers will be called for sync data!");
							continue;
						}
						System.out.println("Trying to get all locked resources from : " + server + " - Response(" + responseCode + "), Hence NO further servers will be called for sync data!");
				
						ObjectInputStream in = new ObjectInputStream(con.getInputStream());
						retObj = (Map<String, ResourceDetailsDto>)in.readObject();
						con.disconnect();
						in.close();
						break;
					}
					he.sendResponseHeaders(200,0);//TODO:
					ObjectOutputStream out = new ObjectOutputStream(he.getResponseBody());
					out.writeObject(retObj);
					out.flush();
					out.close();
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
