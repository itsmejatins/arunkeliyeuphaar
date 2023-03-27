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

public class LastFMRecomm {
	
	public static JsonNode getLastFMRecommendation(String artist, String title) throws Exception {
		
		String urlSearch = "http://ws.audioscrobbler.com/2.0/?method=track.getsimilar&artist=";
		urlSearch+=	artist;
		urlSearch += "&track=";
		urlSearch += title;
		urlSearch += "&api_key=e3dbf66a59b12c26d8e7308512a79120&format=json&limit=5";
		
		URL url = new URL(urlSearch);
		
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");

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
		
		JsonNode s = jsonNode.get("similartracks").get("track");
		
		StringBuilder recomm = new StringBuilder();
		
		for (JsonNode node : s) {
			String str = node.get("name").toString();
			str = str.substring(1, str.length() - 1);
			recomm.append(s);
			System.out.println(str);
		}
			
		
		return jsonNode;
		
	}

	public static void main(String[] args) throws Exception {
		
		
		String artists = "Selena Gomez,Ellie Goulding,The Weeknd,";
		String titles = "Bad Liar,Burn,Secrets,";
		
//		Preprocessing Steps
		artists = artists.substring(0, artists.length() - 1);
		titles = titles.substring(0, titles.length() - 1);
		
		String [] artistsList = artists.split(",");
		String [] titlesList = titles.split(",");
		
		for (int i = 0; i < artistsList.length; i++) {
			artistsList[i] = artistsList[i].replace(" ", "+");
			titlesList[i] = titlesList[i].replace(" ", "+");
	    }
		
		StringBuilder recomm = new StringBuilder();
		
		for(int i = 0 ; i < artistsList.length; i++) {
			
		JsonNode jsonNode = getLastFMRecommendation(artistsList[i],titlesList[i]);
	

	}

}
}
