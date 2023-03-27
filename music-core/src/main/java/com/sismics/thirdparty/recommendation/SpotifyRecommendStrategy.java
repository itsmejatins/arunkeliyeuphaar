package com.sismics.thirdparty.recommendation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SpotifyRecommendStrategy implements RecommendationStrategy {

	private Response renderJson(JsonObjectBuilder response) {
		return Response.ok().entity(response.build()).build();
	}

	private String getAccessToken() throws IOException {
		String CLIENT_ID = "ec93e3ca8e464b8780df9451bf49f7c4";
		String CLIENT_SECRET = "62a828c6ed834ca8a7e306eb83a9d028";
		String AUTH_URL = "https://accounts.spotify.com/api/token";

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(AUTH_URL);
		httpPost.setHeader("Authorization", "Basic " + Base64.getEncoder()
				.encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8)));
		httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.setEntity(new StringEntity("grant_type=client_credentials"));

		try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity);
				ObjectMapper mapper = new ObjectMapper();
				JsonParser parser = mapper.getFactory().createParser(result);

				JsonNode jsonNode = mapper.readTree(parser);
				JsonNode s = jsonNode.get("access_token");

				String token = s.toString();
				token = token.substring(1, token.length() - 1);

				return token;

			}
		}
		return null;
	}

	private JsonNode getSearchResultsSpotify(String search, String token, String type) throws Exception {
		token = "Bearer " + token;

		String urlSearch = "https://api.spotify.com/v1/search?type=";
		urlSearch += type;
		urlSearch += "&q=";
		urlSearch += search;
		urlSearch += "&limit=10&offset=10";
		URL url = new URL(urlSearch);

		System.out.println(url);

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

		return jsonNode;

	}

	private static JsonNode getSpotifyRecommendation(String artistSeed, String token) throws Exception {
		token = "Bearer " + token;
		String urlSearch = "https://api.spotify.com/v1/recommendations?&seed_artists=";
		urlSearch += artistSeed;
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

		return jsonNode;

	}

	@Override
	public Response recommend(String artists) throws Exception {
		artists = artists.substring(0, artists.length() - 1);

		String[] artistsList = artists.split(",");

		String token = getAccessToken();

		for (int i = 0; i < artistsList.length; i++) {
			artistsList[i] = artistsList[i].replace(" ", "+");

		}

		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		List<String> artistSeeds = new ArrayList<>();

		for (int i = 0; i < artistsList.length; i++) {
			String search = artistsList[i];

			String type = "artist";
			JsonNode jsonNode = getSearchResultsSpotify(search, token, type);

			JsonNode s = jsonNode.get("artists").get("items");

			for (JsonNode node : s) {
				String str = node.get("href").toString();
				str = str.substring(1, str.length() - 1);
				String[] temp = str.split("/");
				String seedValue = temp[temp.length - 1];
				artistSeeds.add(seedValue);
				break;
			}

		}

		for (String seed : artistSeeds) {

			JsonNode jsonNode = getSpotifyRecommendation(seed, token);
			JsonNode s = jsonNode.get("tracks");

			for (JsonNode node : s) {
				JsonObjectBuilder localBuilder = Json.createObjectBuilder();

				String track = node.get("album").get("name").toString();
				track = track.substring(1, track.length() - 1);
				localBuilder.add("track", track);

				String artist = node.get("artists").get(0).get("name").toString();
				artist = artist.substring(1, artist.length() - 1);
				localBuilder.add("artist", artist);

				String url = node.get("artists").get(0).get("external_urls").get("spotify").toString();
				url = url.substring(1, url.length() - 1);
				localBuilder.add("url", url);

				arrayBuilder.add(localBuilder);
			}

		}

		finalBuilder.add("tracks", arrayBuilder);
		return renderJson(finalBuilder);
	}
}
