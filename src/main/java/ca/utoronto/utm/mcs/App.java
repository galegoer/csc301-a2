package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import javax.inject.Inject;


public class App
{
    static int port = 8080;

    public static void main(String[] args) throws IOException
    {
    	Dagger service = DaggerDaggerComponent.create().buildMongoHttp();
    		
    	//Create your server context here
    	HttpServer server = service.getServer();
    	MongoClient db = service.getDb();
    	//db.startSession() --put in post.java
    	//MongoDatabase database = db.getDatabase("csc301a2"); --do in post.java
    	Memory mem = new Memory();

    	MongoDatabase database = db.getDatabase("csc301a2");
    	if(database.getCollection("posts") == null)
    		database.createCollection("posts");
    	server.createContext("/api/v1/post", new Post(mem, db));
	    
    	//Dagger daggerPost = service.createContext("/api/v1/post", )
    	//
	    
    	service.getServer().start();
    	
    	System.out.printf("Server started on port %d", port);
    }
}
