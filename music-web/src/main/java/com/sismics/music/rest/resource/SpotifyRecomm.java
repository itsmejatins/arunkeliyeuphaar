package com.sismics.music.rest.resource;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.io.*;

import org.omg.CORBA.portable.OutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import org.apache.http.impl.client.HttpClients;



public class SpotifyRecomm {

	public static JsonNode getSpotifyRecommendation(String artistSeed,String token) throws Exception {
		token = "Bearer " + token;
		String urlSearch = "https://api.spotify.com/v1/recommendations?&seed_artists=";
		urlSearch+=	artistSeed;
		urlSearch += "&limit=5";
		
		System.out.println(urlSearch);

		URL url = new URL(urlSearch);
		
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", token);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Connection", "keep-alive");

		// Send the request and get response
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		

		ObjectMapper mapper = new ObjectMapper();
		JsonParser parser = mapper.getFactory().createParser(response.toString());

		JsonNode jsonNode = mapper.readTree(parser);
		
		JsonNode s = jsonNode.get("tracks");
		
		
		for (JsonNode node : s) {
			String str = node.get("album").get("name").toString();
			str = str.substring(1, str.length() - 1);
//			recomm.append(s);
			System.out.println(str);
		}
		
		
		return jsonNode;

	}

	
	
	

	public static void main(String[] args) throws Exception{
		
		String seed = "5r3wPya2PpeTTsXsGhQU8O";
		String  token = "BQBUta_NQ-4sWL3XAsaToii4x0Uqd4VYyNqNZBUZwNBWZYTJH-_cU-BmsUczF-jFUhBvN_8pMdZu9_eLOlw6rNJzxn9V5pzpoFOM4G2IVQ1n7DSOJPcy";
		
		getSpotifyRecommendation(seed,token);

	}

}
