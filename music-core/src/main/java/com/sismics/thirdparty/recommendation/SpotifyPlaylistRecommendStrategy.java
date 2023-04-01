package com.sismics.thirdparty.recommendation;

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.sismics.util.thirdparty.SpotifyUtil;
import com.sismics.util.thirdparty.Util;

public class SpotifyPlaylistRecommendStrategy implements RecommendationStrategy {

	@Override
	public Response recommend(RecommendDto dto) throws Exception {
		String artists = dto.getArtists();
		artists = artists.substring(0, artists.length() - 1);

		String[] artistsList = artists.split(",");

		String token = SpotifyUtil.getAccessToken();

		for (int i = 0; i < artistsList.length; i++) {
			artistsList[i] = artistsList[i].replace(" ", "+");

		}

		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		List<String> artistSeeds = getArtistSeeds(artistsList, token);
		fillRecommendationData(token, arrayBuilder, artistSeeds);

		finalBuilder.add("tracks", arrayBuilder);
		return Util.renderJson(finalBuilder);
	}

	private List<String> getArtistSeeds(String[] artistsList, String token) throws Exception {
		List<String> artistSeeds = new ArrayList<>();

		for (int i = 0; i < artistsList.length; i++) {
			String search = artistsList[i];

			String type = "artist";
			JsonNode jsonNode = SpotifyUtil.getSearchResultsSpotify(search, token, type);

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
		return artistSeeds;
	}

	private void fillRecommendationData(String token, JsonArrayBuilder arrayBuilder, List<String> artistSeeds)
			throws Exception {
		for (String seed : artistSeeds) {

			JsonNode jsonNode = SpotifyUtil.getSpotifyRecommendation(seed, token);
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
	}
}
