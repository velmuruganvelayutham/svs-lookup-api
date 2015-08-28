package com.svs.lookup;

import java.io.File;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 * Root resource
 */
@Path("lookup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LookupResource {

	private static DB db;
	JsonBuilderFactory factory = Json.createBuilderFactory(null);

	static {

		db = DBMaker.fileDB(new File("testdb")).encryptionEnable("password").make();
	}

	@GET
	public Response getAll() {

		ConcurrentNavigableMap<String, String> map = db.treeMap("keys");
		JsonObjectBuilder createObjectBuilder = factory.createObjectBuilder();
		NavigableSet<String> keySet = map.keySet();
		for (String key : keySet) {
			createObjectBuilder.add(key, map.get(key));
		}
		return Response.status(200).entity(createObjectBuilder.build().toString()).build();
	}

	@GET
	@Path("{accesskey}")
	public Response getIt(@PathParam(value = "accesskey") String accesskey) {

		if (accesskey == null) {
			return sendBadRequestResponse("accesskey parameter is mandatory !");
		}
		String secretKey = getSecretKey(accesskey);
		if (secretKey == null) {
			return sendBadRequestResponse("access key not found  !");
		}
		return Response.status(200).entity(secretKey).build();
	}

	@DELETE
	@Path("{accesskey}")
	public Response deleteIt(@PathParam(value = "accesskey") String accesskey) {
		
		if (accesskey == null) {
			return sendBadRequestResponse("accesskey parameter is mandatory !");
		}
		String secretKey = removeSecretKey(accesskey);
		if (secretKey == null) {
			return sendBadRequestResponse("key not found !"); 
		}
		
		return sendBadRequestResponse("keys are deleted successfully"); 
	}

	private Response sendBadRequestResponse(String message) {
		JsonObjectBuilder createObjectBuilder = factory.createObjectBuilder();
		return Response.status(400).entity(createObjectBuilder.add("message", message).build()).build();
	}

	@POST
	public Response addNewKey(JsonObject document) {

		
		JsonObjectBuilder createObjectBuilder = factory.createObjectBuilder();
		if (document == null) {
			return sendBadRequestResponse("request  payload is missing!"); 
		}
		JsonString accesskey = document.getJsonString("accesskey");
		if (accesskey == null ) {
			return sendBadRequestResponse("access key is missing !"); 
		}
		JsonString secretkey = document.getJsonString("secretkey");
		if (secretkey == null) {
			return sendBadRequestResponse("secretKey is missing !"); 
		}
		db.treeMap("keys").put(accesskey.getString(), secretkey.getString());
		db.commit();
		return Response.status(201).entity(createObjectBuilder.add("message", "keys are added successfully").build())
				.build();

	}

	private String getSecretKey(String key) {

		ConcurrentNavigableMap<String, String> map = db.treeMap("keys");

		return map.get(key);
	}

	private String removeSecretKey(String key) {

		ConcurrentNavigableMap<String, String> map = db.treeMap("keys");

		return map.remove(key);
	}
}
