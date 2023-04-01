package com.sismics.thirdparty.search;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.sismics.util.thirdparty.SpotifyUtil;
import com.sismics.util.thirdparty.Util;

public class SpotifySearchStrategy implements SearchStrategy {

	@Override
	public Response search(String sentData) throws Exception {
		String search = sentData.replace(" ", "+");
		String token = SpotifyUtil.getAccessToken();

		String type = "track";
		JsonNode jsonNode = SpotifyUtil.getSearchResultsSpotify(search, token, type);

		JsonNode s = jsonNode.get("tracks").get("items");

		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		for (JsonNode node : s) {
			JsonObjectBuilder localBuilder = Json.createObjectBuilder();

			String track = node.get("name").toString();
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
		finalBuilder.add("tracks", arrayBuilder);
		return Util.renderJson(finalBuilder);
	}

}
