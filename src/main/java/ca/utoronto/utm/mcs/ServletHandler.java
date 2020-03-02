package ca.utoronto.utm.mcs;

import javax.inject.Inject;

import com.mongodb.client.MongoClient;
import com.sun.net.httpserver.HttpServer;

public class ServletHandler {

	private HttpServer server;
	private Post post;
	private MongoClient db;

	@Inject
	public ServletHandler(Post p) {
		//this.server = server;
		this.post = p;
	}

	public HttpServer getServer() {
		return this.server;
	}
	
	public Post getPost() {
		return this.post;
	}

	public void setServer(HttpServer server) {
		this.server = server;
	}

	public MongoClient getDb() {
		return this.db;
	}

	public void setDb(MongoClient db) {
		this.db = db;
		post.setDb(db);
	}

}
