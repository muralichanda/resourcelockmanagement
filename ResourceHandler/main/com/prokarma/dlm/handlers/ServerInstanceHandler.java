package com.prokarma.dlm.handlers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;

import com.prokarma.dlm.data.DataRepository;
import com.prokarma.dlm.util.AbstractThread;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ServerInstanceHandler implements HttpHandler {

	@Override
	public void handle(final HttpExchange he){
		AbstractThread thread = new AbstractThread() {
			public void run() {
				try {
					ObjectInputStream requestBody = new ObjectInputStream(he.getRequestBody());
					String obj = (String)requestBody.readObject();
					String[] serverInstance = obj.split(",");
					if("true".equals(serverInstance[1])){
						System.out.println("Server instance(" + serverInstance[0] + ") is up and running, hence adding to active servers!");
						DataRepository.addServerInstance(serverInstance[0].replaceAll("/ping", ""));
					}else{
						System.out.println("Server instance(" + serverInstance[0] + ") is down, hence removing from active servers!");
						DataRepository.removeServerInstance(serverInstance[0].replaceAll("/ping", ""));
					}
					String msg = "Success";
					he.sendResponseHeaders(200, msg.length());
					OutputStream response = he.getResponseBody();
					response.write(msg.getBytes());
					response.close();
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
