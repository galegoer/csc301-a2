package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.*;

import com.mongodb.client.MongoClient;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
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
	 	
        String id = memory.getValue();
        String title = memory.getValue();
        String contents = "["; //start of response
        Document d = null;
        
        //id only, or id takes priority --------------------------------------------------------------------------------------------
        if (deserialized.has("_id")) {
            id = deserialized.getString("_id");
            
            //query for _id = id
            BasicDBObject whereQuery = new BasicDBObject();
            whereQuery.put("_id", id);
            FindIterable<Document> cursor = posts.find(whereQuery);
            if (cursor.first() == null)
            	r.sendResponseHeaders(404,-1);
            d = cursor.first();
            if (!ObjectId.isValid(id))
            	r.sendResponseHeaders(400, -1);
            contents += this.generate_response(d, id);
        }
        //title only ----------------------------------------------------------------------------------------------------------------
        else if (deserialized.has("title")) {
        	title = deserialized.getString("title");
        	
        	//query for all titles that contain string "title"
            BasicDBObject regexQuery = new BasicDBObject();
            regexQuery.put("title", 
                new BasicDBObject("$regex", title)
                .append("$options", "i"));
            FindIterable<Document> cursor = posts.find(regexQuery);
            
            Iterator T = cursor.iterator();
            while (T.hasNext()) {
                contents += this.generate_response((Document) T.next(), d.getString("_id"));
                if (!ObjectId.isValid(d.getString("_id")))
                	r.sendResponseHeaders(400, -1);
                
                contents += ","; //if there are multiple documents we need a "," after the ending }
                //might need to check if last doc then dont put "," ---
            }
        }
        //error ---------------------------------------------------------------------------------------------------------------------
        else {  //no id or title found
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        
     
//        contents += ("\n\t{\n\t\t\"_id\": {\n\t\t\t\"$oid\": " +	//part of generate response
//				"\"" + id + "\"" + "\n\t\t},");
//        
//        Set <String> keys = d.keySet(); //set of keys
//        int count = keys.size(); //# of keys
//        int temp = 0; //keeping track of num keys written to contents so far
//        for (String k: keys) {
//        	temp++;
//        	if (k.equals("title"))
//        		contents += ("\n\t\t\"title\": \"" + d.getString("title")); //"title": "My First Post",
//        
//        	else if (k.equals("author"))
//        		contents += ("\n\t\t\"author\": \"" + d.getString("author")); //"author": "My First Post",
//        
//        	else if (k.equals("content"))
//        		contents += ("\n\t\t\"content\": \"" + d.getString("title")); //"content": "My First Post",
//        
//        	else if (k.equals("tags")) {
//        		contents += ("\n\t\t\"tags\": ["); //"tags": [
//        		List <String> tg = (List <String> )d.get("tags");
//        		for (int i = 0; i < tg.size(); i++) {
//        			contents += ("\n\t\t\t\"" + tg.get(i) + "\"");
//        			if (i != tg.size()-1) //if not last tag
//        				contents += ",";
//        		}
//        		contents += "\n\t\t]";
//        	}
//        	
//        	if (temp != count && !k.equals("tags")) //if not last key and key just read isnt tags, add comma
//            	contents+=  "\",";
//        }
//        contents += "\n\t}"; //part of generate_response
        
        contents += "\n]"; //end of response
        
//        if (d.containsKey("title"))
//        	contents += ("\n\t\t\"title\": \"" + d.getString("title") + "\","); //"title": "My First Post",
//        
//        if (d.containsKey("author"))
//        	contents += ("\n\t\t\"author\": \"" + d.getString("author") + "\","); //"title": "My First Post",
//        
//        if (d.containsKey("content"))
//        	contents += ("\n\t\t\"content\": \"" + d.getString("title") + "\","); //"title": "My First Post",
//        
//        if (d.containsKey("tags")) {
//        	contents += ("\n\t\t\"tags\": ["); //"title": "My First Post",
//        	List <String> tg = (List <String> )d.get("tags");
//        	for (int i = 0; i < tg.size(); i++) {
//        		contents += ("\n\t\t\t\"" + tg.get(i) + "\"");
//        		if (i != tg.size()-1) //if not last tag
//        			contents += ",";
//        	}
//        	contents += "\n\t\t]";
//        }
 //String response = "[\n\t{\n\t\t\"_id\": {\n\t\t\t\"$oid\": " +
        //						"\"" + id + "\"" + "\n\t\t}," + contents;
        		
        
        
        r.sendResponseHeaders(200, 5); //response.length());
        OutputStream os = r.getResponseBody();
        os.write(contents.getBytes());		//response.getBytes());
        os.close();
        return;


}

public String generate_response(Document d, String id) {
	String contents = ("\n\t{\n\t\t\"_id\": {\n\t\t\t\"$oid\": " +
			"\"" + id + "\"" + "\n\t\t},");
    
    Set <String> keys = d.keySet(); //set of keys
    int count = keys.size(); //# of keys
    int temp = 0; //keeping track of num keys written to contents so far
    for (String k: keys) {
    	temp++;
    	if (k.equals("title"))
    		contents += ("\n\t\t\"title\": \"" + d.getString("title")); //"title": "My First Post",
    
    	else if (k.equals("author"))
    		contents += ("\n\t\t\"author\": \"" + d.getString("author")); //"author": "My First Post",
    
    	else if (k.equals("content"))
    		contents += ("\n\t\t\"content\": \"" + d.getString("title")); //"content": "My First Post",
    
    	else if (k.equals("tags")) {
    		contents += ("\n\t\t\"tags\": ["); //"tags": [
    		List <String> tg = (List <String> )d.get("tags");
    		for (int i = 0; i < tg.size(); i++) {
    			contents += ("\n\t\t\t\"" + tg.get(i) + "\"");
    			if (i != tg.size()-1) //if not last tag
    				contents += ",";
    		}
    		contents += "\n\t\t]";
    	}
    	
    	if (temp != count && !k.equals("tags")) //if not last key and key just read isnt tags, add comma
        	contents+=  "\",";
    }
    contents += "\n\t}";
	return contents;
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