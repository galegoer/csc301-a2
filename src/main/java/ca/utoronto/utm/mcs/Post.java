package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
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
import com.mongodb.client.result.DeleteResult;
import com.mongodb.session.ServerSession;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class Post implements HttpHandler, AutoCloseable 
{
    private static MongoClient database;
    private static MongoDatabase csdb;
    private static MongoCollection<Document> posts;
    
    public Post(MongoClient db) {
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
                //handleGet(r);
            } else if (r.getRequestMethod().equals("PUT")) {
            	//handlePut(r);
            } else if (r.getRequestMethod().equals("DELETE")) {
            	handleDelete(r);
            }
        } catch (Exception e) {
        	//Method not allowed
        	r.sendResponseHeaders(405, -1);
        	return;
        }
    }
    
 public void handlePut(HttpExchange r) throws IOException, JSONException {
	 try {
	 	String body = Utils.convert(r.getRequestBody());
	 	JSONObject deserialized;
	 	try {
	 		deserialized = new JSONObject(body);
	 	} catch (Exception e) {
	 		//Error parsing the JSON Message
	 		r.sendResponseHeaders(400, -1);
	 		return;
	 	}
	 	
	 	String title;
        String author;
        String content;
        List<String> tags = new ArrayList<String>();
        
        if (deserialized.has("title") && deserialized.has("author") 
        		&& deserialized.has("content") && deserialized.has("tags")) {
            title = deserialized.getString("title");
            author = deserialized.getString("author");
            content = deserialized.getString("content");
            try {
            	JSONArray arr = deserialized.getJSONArray("tags");
            	for(int i = 0; i < arr.length(); i++) {
                	tags.add(arr.getString(i));
                }
            } catch (Exception e) {
            	r.sendResponseHeaders(400, -1);
            	return;
            }
        } else {
        	//missing
        	r.sendResponseHeaders(400, -1);
        	return;
        }
		// Good so connect to mongo and post
        	JSONObject post = new JSONObject();
        	post.put("title", title);
        	post.put("author", author);
        	post.put("content", content);
        	post.put("tags", tags);
        	
        	Document doc = Document.parse(post.toString());
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
        
	 } catch (Exception e) {
		 //IF IT ever errors out i guess 500 takes priority
		 r.sendResponseHeaders(500, -1);
		 return;
	 }

}

//public void handleGet(HttpExchange r) throws IOException, JSONException {
//        String body = Utils.convert(r.getRequestBody());
//        JSONObject deserialized;
//	 	try {
//	 		deserialized = new JSONObject(body);
//	 	} catch (Exception e) {
//	 		//Error parsing the JSON Message
//	 		r.sendResponseHeaders(400, -1);
//	 		return;
//	 	}
//	 	
//        String title = memory.getValue();
//        String author = memory.getValue();
//        String content = memory.getValue();
//        String tags = memory.getValue();
//        
//        if (deserialized.has("title") && deserialized.has("author") 
//        		&& deserialized.has("content") && deserialized.has("tags")) {
//        	
//            title = deserialized.getString("title");
//            author = deserialized.getString("author");
//            content = deserialized.getString("content");
//            tags = deserialized.getString("tags");
//        }
//        else {
//        	r.sendResponseHeaders(400, -1);
//        	return;
//        }
//        //Good so send result to mongodb
//       
//        		
//        r.sendResponseHeaders(200, 5); //response.length());
//        OutputStream os = r.getResponseBody();
//        os.write("test".getBytes());		//response.getBytes());
//        os.close();
//        return;
//
//
//}

public void handleDelete(HttpExchange r) throws IOException, JSONException {
	try {
		String body = Utils.convert(r.getRequestBody());
		JSONObject deserialized;
		try {
			deserialized = new JSONObject(body);
		} catch (Exception e) {
			//Error parsing the JSON Message
			r.sendResponseHeaders(400, -1);
			return;
		}

		String Id;

		if (deserialized.has("_id"))
			Id = deserialized.getString("_id");
		else {
			r.sendResponseHeaders(400, -1);
			return;
		}
		Document doc = Document.parse("{\"_id\": ObjectId(\"" + Id + "\")}");
		DeleteResult res = posts.deleteOne(doc);
		if(res.getDeletedCount() == 0) {
			r.sendResponseHeaders(404, -1);
			return;
		}
		r.sendResponseHeaders(200, -1);
		return;
		
	} catch (Exception e) {
		r.sendResponseHeaders(500, -1);
		return;
	}
}
}