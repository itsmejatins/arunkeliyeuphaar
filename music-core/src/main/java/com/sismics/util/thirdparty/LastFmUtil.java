package com.sismics.util.thirdparty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LastFmUtil {
	
	public static JsonNode getSearchResultsLastFM(String search) throws Exception {

		String urlSearch = "https://ws.audioscrobbler.com/2.0/?method=track.search&track=";
		urlSearch += search;
		urlSearch += "&api_key=e3dbf66a59b12c26d8e7308512a79120&format=json&limit=10";

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
		JsonNode s = jsonNode.get("results").get("trackmatches").get("track");
		return s;

	}
	
	public static JsonNode getLastFMRecommendation(String artist, String title) throws Exception {

		String urlSearch = "http://ws.audioscrobbler.com/2.0/?method=track.getsimilar&artist=";
		urlSearch += artist;
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
		return jsonNode;

	}

}
