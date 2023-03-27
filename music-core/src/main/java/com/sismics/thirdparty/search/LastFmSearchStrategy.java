package com.sismics.thirdparty.search;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LastFmSearchStrategy implements SearchStrategy {
	private Response renderJson(JsonObjectBuilder response) {
		return Response.ok().entity(response.build()).build();
	}

	private static JsonNode getSearchResultsLastFM(String search) throws Exception {

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

	@Override
	public Response search(String sentData) throws Exception {
		String search = sentData.replace(" ", "+");

		JsonNode s = getSearchResultsLastFM(search);

		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		for (JsonNode node : s) {
			JsonObjectBuilder localBuilder = Json.createObjectBuilder();

			String track = node.get("name").toString();
			track = track.substring(1, track.length() - 1);
			localBuilder.add("track", track);

			String artist = node.get("artist").toString();
			artist = artist.substring(1, artist.length() - 1);
			localBuilder.add("artist", artist);

			String url = node.get("url").toString();
			url = url.substring(1, url.length() - 1);
			localBuilder.add("url", url);

			arrayBuilder.add(localBuilder);
		}

		finalBuilder.add("tracks", arrayBuilder);
		return renderJson(finalBuilder);
	}

}
