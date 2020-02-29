package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import com.mongodb.client.MongoClient;
import com.sun.net.httpserver.HttpServer;


import javax.inject.Inject;


public class App
{
    static int port = 8080;

    public static void main(String[] args) throws IOException
    {
    	Dagger service = DaggerDaggerComponent.create().buildMongoHttp();
    	Memory mem = new Memory();
    	//Create your server context here
    	HttpServer server = service.getServer();
    	MongoClient db = service.getDb();
	    
    	server.createContext("/api/v1/post", new Put(mem));
    	//Car carOne = component.buildCar();
	    //Car carTwo = component.buildCar();
	    
    	//Dagger daggerPost = service.createContext("/api/v1/post", )
    	//
	    
    	service.getServer().start();
    	
    	System.out.printf("Server started on port %d", port);
    }
}
