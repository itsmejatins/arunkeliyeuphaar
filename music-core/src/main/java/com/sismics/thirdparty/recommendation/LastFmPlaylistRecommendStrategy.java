package com.sismics.thirdparty.recommendation;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.sismics.util.thirdparty.LastFmUtil;
import com.sismics.util.thirdparty.Util;

public class LastFmPlaylistRecommendStrategy implements RecommendationStrategy {

	@Override
	public Response recommend(RecommendDto dto) throws Exception {
		String artists = dto.getArtists(), titles = dto.getTitles();
		artists = artists.substring(0, artists.length() - 1);
		titles = titles.substring(0, titles.length() - 1);

		String[] artistsList = artists.split(",");
		String[] titlesList = titles.split(",");

		for (int i = 0; i < Math.min(artistsList.length, titlesList.length); i++) {
			artistsList[i] = artistsList[i].replace(" ", "+");
			titlesList[i] = titlesList[i].replace(" ", "+");
		}

//Getting recommended tracks and adding 

		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		fillRecommendationData(artistsList, titlesList, arrayBuilder);

		finalBuilder.add("tracks", arrayBuilder);
		return Util.renderJson(finalBuilder);
	}

	private void fillRecommendationData(String[] artistsList, String[] titlesList, JsonArrayBuilder arrayBuilder)
			throws Exception {
		for (int i = 0; i < Math.min(artistsList.length, titlesList.length); i++) {

			JsonNode jsonNode = LastFmUtil.getLastFMRecommendation(artistsList[i], titlesList[i]);

			JsonNode s = jsonNode.get("similartracks").get("track");

			for (JsonNode node : s) {

				JsonObjectBuilder localBuilder = Json.createObjectBuilder();
				String track = node.get("name").toString();
				track = track.substring(1, track.length() - 1);
				localBuilder.add("track", track);

				String artist = node.get("artist").get("name").toString();
				artist = artist.substring(1, artist.length() - 1);
				localBuilder.add("artist", artist);

				String url = node.get("url").toString();
				url = url.substring(1, url.length() - 1);
				localBuilder.add("url", url);

				arrayBuilder.add(localBuilder);
			}

		}
	}

}
