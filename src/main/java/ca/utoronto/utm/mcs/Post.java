package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.*;

import com.mongodb.client.MongoClient;
import com.mongodb.DBObject;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.session.ServerSession;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class Post implements HttpHandler, AutoCloseable 
{
    private static Memory memory;
    private static MongoClient database;
    private static MongoDatabase csdb;
    private static MongoCollection<Document> posts;
    
    public Post(Memory mem, MongoClient db) {
        memory = mem;
        database = db;
        csdb = database.getDatabase("csc301a2");
        posts = csdb.getCollection("posts");
    }
    
    @Override
    public void close() throws Exception {
    	// TODO Auto-generated method stub
    	
    }

    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            } else if (r.getRequestMethod().equals("PUT")) {
            	handlePut(r);
            } else if (r.getRequestMethod().equals("DELETE")) {
            	//handleDelete(r);
            }
        } catch (Exception e) {
        	//Method not allowed
        	r.sendResponseHeaders(405, -1);
        	return;
        }
    }

 public void handlePut(HttpExchange r) throws IOException, JSONException {
	 	String body = Utils.convert(r.getRequestBody());
	 	JSONObject deserialized;
	 	try {
	 		deserialized = new JSONObject(body);
	 	} catch (Exception e) {
	 		//Error parsing the JSON Message
	 		r.sendResponseHeaders(400, -1);
	 		return;
	 	}
	 	
	 	String title = memory.getValue();
        String author = memory.getValue();
        String content = memory.getValue();
        String tags = memory.getValue();
        
        if (deserialized.has("title") && deserialized.has("author") 
        		&& deserialized.has("content") && deserialized.has("tags")) {
            title = deserialized.getString("title");
            author = deserialized.getString("author");
            content = deserialized.getString("content");
            tags = deserialized.getString("tags");
        } else {
        	//missing
        	r.sendResponseHeaders(400, -1);
        	return;
        }
		// Good so connect to mongo and post
        //try (ClientSession session = database.startSession())
        //{	
        	//session.startTransaction();
        	Document doc = Document.parse(deserialized.toString());
        	posts.insertOne(doc);
        	ObjectId id = doc.getObjectId("_id");
      	//} catch(Exception e) {
      	//	e.printStackTrace();
      	//	r.sendResponseHeaders(500, -1);
      	//	return;
      	//}
        //Went through send response
        String response = "{\n\t\"_id\": \"" + id.toString() + "\"\n}";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;

}

public void handleGet(HttpExchange r) throws IOException, JSONException {
        String body = Utils.convert(r.getRequestBody());
        JSONObject deserialized;
	 	try {
	 		deserialized = new JSONObject(body);
	 	} catch (Exception e) {
	 		//Error parsing the JSON Message
	 		r.sendResponseHeaders(400, -1);
	 		return;
	 	}
	 	
        String title = memory.getValue();
        String author = memory.getValue();
        String content = memory.getValue();
        String tags = memory.getValue();
        
        if (deserialized.has("title") && deserialized.has("author") 
        		&& deserialized.has("content") && deserialized.has("tags")) {
        	
            title = deserialized.getString("title");
            author = deserialized.getString("author");
            content = deserialized.getString("content");
            tags = deserialized.getString("tags");
        }
        else {
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        //Good so send result to mongodb
       
        		
        r.sendResponseHeaders(200, 5); //response.length());
        OutputStream os = r.getResponseBody();
        os.write("test".getBytes());		//response.getBytes());
        os.close();
        return;


}

//public void handleDelete(HttpExchange r) throws IOException, JSONException {
//    String body = Utils.convert(r.getRequestBody());
//    JSONObject deserialized;
// 	try {
// 		deserialized = new JSONObject(body);
// 	} catch (Exception e) {
// 		//Error parsing the JSON Message
// 		r.sendResponseHeaders(400, -1);
// 		return;
// 	}
// 	
//    String Id = memory.getValue();
//
//    if (deserialized.has("actorId"))
//        Id = deserialized.getString("actorId");
//    else {
//    	r.sendResponseHeaders(400, -1);
//    	return;
//    }
//    
//    try (ServerSession session = driver.session())
//    {	
//    	try (Transaction tx = session.beginTransaction())
//    	{	
//    		actor_name = tx.run("MATCH (a:actor) WHERE a.id = $actorId RETURN a.Name", parameters("actorId", Id)); 
//    		if(actor_name.hasNext()) { //actor_id exists
//    			//retrieve movies since we know actorID is in the database
//    			actor_movies = tx.run("MATCH (:actor { id: {x} })--(movie) RETURN movie.id", parameters("x", Id));
//    			tx.success();  // Mark this write as successful.
//    		} else {
//    			r.sendResponseHeaders(404, -1); //SEND 404 NOT FOUND IF NAME ISNT FOUND I.E NO ACTORID IN DB
//    			return;
//    		}
//    	}
//    }catch(Exception e) {
//    	r.sendResponseHeaders(500, -1);
//    	return;
//    }
//    
//    String movies_list = "\n\t\t";
//    List<Record> results = actor_movies.list();
//    if (results.isEmpty()) 
//    	movies_list = "";
//    else {
//    	for (int i = 0; i < results.size(); i++) {
//    		movies_list = movies_list + results.get( i ).get("movie.id");
//    		if (i != results.size() -1)
//    			movies_list += ",\n\t\t";
//    	}
//    	movies_list += "\n\t";
//    }
//    
//    String response = "{\n\t" + 
//    		"\"actorId\": " + "\"" + Id + "\",\n\t" +
//    		"\"name\": " + "\"" + actor_name.single().get( 0 ).asString() + "\",\n\t" + 
//    		"\"movies\": " + 
//    			"[" + movies_list + "]"
//    		+ "\n}";
//    		
//    r.sendResponseHeaders(200, response.length());
//    OutputStream os = r.getResponseBody();
//    os.write(response.getBytes());
//    os.close();
//    return;
//
//
//}
}