package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.*;

import com.mongodb.client.MongoClient;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class Post implements HttpHandler, AutoCloseable 
{
    private static MongoClient database;
    private static MongoDatabase csdb;
    private static MongoCollection<Document> posts;
    
    public Post() {}
    
    public void setDb(MongoClient db) {
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
            	handleDelete(r);
            }
            else
            	throw new Exception();
    	}
    	catch (Exception e) {
    		r.sendResponseHeaders(405, -1);
    		return;
    	}
            //else {
            //	r.sendResponseHeaders(405, -1);
		return;
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
        
        if (deserialized.has("title") && deserialized.get("title") instanceof String 
        		&& deserialized.has("author") && deserialized.get("author") instanceof String 
        		&& deserialized.has("content") && deserialized.get("content") instanceof String 
        		&& deserialized.has("tags")) {

        	title = deserialized.getString("title");
        	author = deserialized.getString("author");
        	content = deserialized.getString("content");
        	
            try {
            	JSONArray arr = deserialized.getJSONArray("tags");
            	for(int i = 0; i < arr.length(); i++) {
                	tags.add(arr.getString(i));
                }
            } catch (Exception e) {
            	//tags is not an array
            	r.sendResponseHeaders(400, -1);
            	return;
            }
        } else {
        	//missing something or type is not of string
        	r.sendResponseHeaders(400, -1);
        	return;
        }
        // Good so post to mongo
        JSONObject post = new JSONObject();
        post.put("title", title);
        post.put("author", author);
        post.put("content", content);
        post.put("tags", tags);
        Document doc = Document.parse(post.toString());
        posts.insertOne(doc);
        ObjectId id = doc.getObjectId("_id");
        //Went through send response
        String response = "{\n\t\"_id\": \"" + id.toString() + "\"\n}";
        r.sendResponseHeaders(200, response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
        return;
	
	} catch (Exception e) {
		 //If it ever errors out 500 takes priority
		r.sendResponseHeaders(500, -1);
		return;
	}

}
public void handleGet(HttpExchange r) throws IOException, JSONException {
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
	 	
        String id;
        String title;
        String contents = "["; //start of response
   
        //id only, or id takes priority --------------------------------------------------------------------------------------------
        if (deserialized.has("_id")) {
            id = deserialized.getString("_id");
            if (!ObjectId.isValid(id)) {//has both but id is incorrect RETURN 400 according to ilir
            	r.sendResponseHeaders(400, -1);
            	return;
            }
            //query for _id = id
            BasicDBObject whereQuery = new BasicDBObject();
            whereQuery.put("_id", new ObjectId(id));
            FindIterable<Document> cursor = posts.find(whereQuery);
          
            if (cursor.first() == null) { 
            	r.sendResponseHeaders(404,-1);
            	return;
            }
            Document d = cursor.first();
            contents += this.generate_response(d);
        }
        //title only ----------------------------------------------------------------------------------------------------------------
        else if (deserialized.has("title") && deserialized.get("title") instanceof String) {
        	title = deserialized.getString("title");
        	//query for all titles that contain string "title"
            BasicDBObject regexQuery = new BasicDBObject();
            regexQuery.put("title", 
                new BasicDBObject("$regex", title));
            FindIterable<Document> cursor = posts.find(regexQuery);
            if (cursor.first() == null) {//no post(s) found
            	r.sendResponseHeaders(404,-1);
            	return;
            }
            Iterator<Document> T = cursor.iterator();
            while (T.hasNext()) {
            	Document D = T.next();
            	String temp_Id = D.getObjectId("_id").toString();
            	if (!ObjectId.isValid(temp_Id)) {
                	r.sendResponseHeaders(400, -1);
                	return;
            	}
            	contents += this.generate_response(D);
            	if (T.hasNext()) //if NOT last doc then add comma
            		contents += ",";
            }
      }
        //error ---------------------------------------------------------------------------------------------------------------------
        else {  //no id or title found
        	r.sendResponseHeaders(400, -1);
        	return;
        }     
        
        contents += "\n]"; //end of response
        
        r.sendResponseHeaders(200, contents.length()); //response.length());
        OutputStream os = r.getResponseBody();
        os.write(contents.getBytes());		//response.getBytes());
        os.close();
        return;
        
	 } catch (Exception e) {
		 //If it ever errors 500 takes priority
		 r.sendResponseHeaders(500, -1);
		 return;
	 }

}


public String generate_response(Document d) {
	String contents = ("\n\t{\n\t\t\"_id\": {\n\t\t\t\"$oid\": " +
			"\"" + d.getObjectId("_id") + "\"" + "\n\t\t}");
    
    Set <String> keys = d.keySet(); //set of keys
    int count = keys.size(); //# of keys
    int temp = 0; //keeping track of num keys written to contents so far
    for (String k: keys) {
    	temp++;
    	if (k.equals("title"))
    		contents += ("\n\t\t\"title\": \"" + d.getString("title")) + "\""; //"title": "My First Post",
    
    	else if (k.equals("author"))
    		contents += ("\n\t\t\"author\": \"" + d.getString("author")) + "\"" ; //"author": "My First Post",
    
    	else if (k.equals("content"))
    		contents += ("\n\t\t\"content\": \"" + d.getString("content")) + "\""; //"content": "My First Post",
    
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
        	contents+=  ",";
    }
    contents += "\n\t}";
	return contents;
}

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
		if (!ObjectId.isValid(Id)) {//has both but id is incorrect RETURN 400 according to ilir
        	r.sendResponseHeaders(400, -1);
        	return;
        }
		Document doc = Document.parse("{\"_id\": ObjectId(\"" + Id + "\")}");
		DeleteResult res = posts.deleteOne(doc);
		if(res.getDeletedCount() == 0) {
			//nothing deleted therefore 404 error
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
