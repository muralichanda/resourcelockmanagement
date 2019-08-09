package com.prokarma.dlm;

import java.net.InetSocketAddress;

import com.prokarma.dlm.data.DataRepository;
import com.prokarma.dlm.handlers.AllDataSyncHandler;
import com.prokarma.dlm.handlers.ClientAliveHandler;
import com.prokarma.dlm.handlers.LockResourceHandler;
import com.prokarma.dlm.handlers.ReleaseResourceHandler;
import com.prokarma.dlm.handlers.ServerInstanceHandler;
import com.sun.net.httpserver.HttpServer;

public class ResourceHandlerServer {
	
	public static void main(String[] args) {
		try {
			String serverPort = DataRepository.getProperty("handlerServerPort");
			if(serverPort == null || serverPort.trim().equals("")){
				System.out.println("Handler Server Port is not set, Please check the properties file!");
			}
			HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(serverPort)), 0);
			server.createContext(DataRepository.getProperty("lockHandlerContext"), new LockResourceHandler());//Done
			server.createContext(DataRepository.getProperty("releaseHandlerContext"), new ReleaseResourceHandler());//Done
			server.createContext(DataRepository.getProperty("allDataSync"), new AllDataSyncHandler());
			server.createContext(DataRepository.getProperty("iAmAlive"), new ClientAliveHandler());//Done
			server.createContext(DataRepository.getProperty("serverInstanceUpdate"), new ServerInstanceHandler());//Done
			server.setExecutor(null);
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
