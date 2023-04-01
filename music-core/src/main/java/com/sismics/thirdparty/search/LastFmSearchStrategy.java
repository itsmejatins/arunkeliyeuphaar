package com.sismics.thirdparty.search;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.sismics.util.thirdparty.LastFmUtil;
import com.sismics.util.thirdparty.Util;

public class LastFmSearchStrategy implements SearchStrategy {

	@Override
	public Response search(String sentData) throws Exception {
		String search = sentData.replace(" ", "+");

		JsonNode s = LastFmUtil.getSearchResultsLastFM(search);

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
		return Util.renderJson(finalBuilder);
	}

}
