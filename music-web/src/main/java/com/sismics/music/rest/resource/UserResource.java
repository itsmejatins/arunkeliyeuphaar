package com.sismics.music.rest.resource;

import com.sismics.music.core.constant.Constants;
import com.sismics.music.core.dao.dbi.AuthenticationTokenDao;
import com.sismics.music.core.dao.dbi.RolePrivilegeDao;
import com.sismics.music.core.dao.dbi.UserDao;
import com.sismics.music.core.dao.dbi.criteria.UserCriteria;
import com.sismics.music.core.dao.dbi.dto.UserDto;
import com.sismics.music.core.event.PasswordChangedEvent;
import com.sismics.music.core.event.UserCreatedEvent;
import com.sismics.music.core.event.async.LastFmUpdateLovedTrackAsyncEvent;
import com.sismics.music.core.event.async.LastFmUpdateTrackPlayCountAsyncEvent;
import com.sismics.music.core.model.context.AppContext;
import com.sismics.music.core.model.dbi.AuthenticationToken;
import com.sismics.music.core.model.dbi.Playlist;
import com.sismics.music.core.model.dbi.User;
import com.sismics.music.core.service.lastfm.LastFmService;
import com.sismics.music.core.util.dbi.PaginatedList;
import com.sismics.music.core.util.dbi.PaginatedLists;
import com.sismics.music.core.util.dbi.SortCriteria;
import com.sismics.music.rest.constant.Privilege;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.Validation;
import com.sismics.security.UserPrincipal;
import com.sismics.util.LocaleUtil;
import com.sismics.util.filter.TokenBasedSecurityFilter;
import de.umass.lastfm.Session;
import org.apache.commons.lang.StringUtils;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.Cookie;
import javax.ws.rs.*;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Set;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
//import com.google.gson.JsonObject;
import org.apache.http.impl.client.HttpClients;

/**
 * User REST resources.
 * 
 * @author jtremeaux
 */
@Path("/user")
public class UserResource extends BaseResource {
	/**
	 * Creates a new user.
	 * 
	 * @param username User's username
	 * @param password Password
	 * @param email    E-Mail
	 * @param localeId Locale ID
	 * @return Response
	 */
	@PUT
	public Response register(@FormParam("username") String username, @FormParam("password") String password,
			@FormParam("locale") String localeId, @FormParam("email") String email) {

		if (!authenticate()) {
			throw new ForbiddenClientException();
		}
		checkPrivilege(Privilege.ADMIN);

		// Validate the input data
		username = Validation.length(username, "username", 3, 50);
		Validation.alphanumeric(username, "username");
		password = Validation.length(password, "password", 8, 50);
		email = Validation.length(email, "email", 3, 50);
		Validation.email(email, "email");

		// Create the user
		User user = new User();
		user.setRoleId(Constants.DEFAULT_USER_ROLE);
		user.setUsername(username);
		user.setPassword(password);
		user.setEmail(email);
		user.setCreateDate(new Date());

		if (localeId == null) {
			// Set the locale from the HTTP headers
			localeId = LocaleUtil.getLocaleIdFromAcceptLanguage(request.getHeader("Accept-Language"));
		}
		user.setLocaleId(localeId);

		// Create the user
		UserDao userDao = new UserDao();
		String userId = null;
		try {
			userId = userDao.create(user);
		} catch (Exception e) {
			if ("AlreadyExistingUsername".equals(e.getMessage())) {
				throw new ServerException("AlreadyExistingUsername", "Login already used", e);
			} else {
				throw new ServerException("UnknownError", "Unknown Server Error", e);
			}
		}

		// Create the default playlist for this user
		Playlist playlist = new Playlist();
		playlist.setUserId(userId);
		Playlist.createPlaylist(playlist);

		// Raise a user creation event
		UserCreatedEvent userCreatedEvent = new UserCreatedEvent();
		userCreatedEvent.setUser(user);
		AppContext.getInstance().getAsyncEventBus().post(userCreatedEvent);

		return okJson();
	}

	/**
	 * Updates user informations.
	 * 
	 * @param password        Password
	 * @param email           E-Mail
	 * @param localeId        Locale ID
	 * @param firstConnection True if the user hasn't acknowledged the first
	 *                        connection wizard yet
	 * @return Response
	 */
	@POST
	public Response update(@FormParam("password") String password, @FormParam("email") String email,
			@FormParam("locale") String localeId, @FormParam("first_connection") Boolean firstConnection) {

		if (!authenticate()) {
			throw new ForbiddenClientException();
		}

		// Validate the input data
		password = Validation.length(password, "password", 8, 50, true);
		email = Validation.length(email, "email", null, 100, true);
		localeId = com.sismics.music.rest.util.ValidationUtil.validateLocale(localeId, "locale", true);

		// Update the user
		UserDao userDao = new UserDao();
		User user = userDao.getActiveByUsername(principal.getName());
		if (email != null) {
			user.setEmail(email);
		}
		if (localeId != null) {
			user.setLocaleId(localeId);
		}
		if (firstConnection != null && hasPrivilege(Privilege.ADMIN)) {
			user.setFirstConnection(firstConnection);
		}

		user = userDao.update(user);

		if (StringUtils.isNotBlank(password)) {
			user.setPassword(password);
			user = userDao.updatePassword(user);
		}

		if (StringUtils.isNotBlank(password)) {
			// Raise a password updated event
			PasswordChangedEvent passwordChangedEvent = new PasswordChangedEvent();
			passwordChangedEvent.setUser(user);
			AppContext.getInstance().getAsyncEventBus().post(passwordChangedEvent);
		}

		return okJson();
	}

	/**
	 * Updates user informations.
	 * 
	 * @param username Username
	 * @param password Password
	 * @param email    E-Mail
	 * @param localeId Locale ID
	 * @return Response
	 */
	@POST
	@Path("{username: [a-zA-Z0-9_]+}")
	public Response update(@PathParam("username") String username, @FormParam("password") String password,
			@FormParam("email") String email, @FormParam("locale") String localeId) {

		if (!authenticate()) {
			throw new ForbiddenClientException();
		}
		checkPrivilege(Privilege.ADMIN);

		// Validate the input data
		password = Validation.length(password, "password", 8, 50, true);
		email = Validation.length(email, "email", null, 100, true);
		localeId = com.sismics.music.rest.util.ValidationUtil.validateLocale(localeId, "locale", true);

		// Check if the user exists
		UserDao userDao = new UserDao();
		User user = userDao.getActiveByUsername(username);
		if (user == null) {
			throw new ClientException("UserNotFound", "The user doesn't exist");
		}

		// Update the user
		if (email != null) {
			user.setEmail(email);
		}
		if (localeId != null) {
			user.setLocaleId(localeId);
		}

		user = userDao.update(user);

		if (StringUtils.isNotBlank(password)) {
			checkPrivilege(Privilege.PASSWORD);

			// Change the password
			user.setPassword(password);
			user = userDao.updatePassword(user);

			// Raise a password updated event
			PasswordChangedEvent passwordChangedEvent = new PasswordChangedEvent();
			passwordChangedEvent.setUser(user);
			AppContext.getInstance().getAsyncEventBus().post(passwordChangedEvent);
		}

		// Always return "ok"
		JsonObject response = Json.createObjectBuilder().add("status", "ok").build();
		return Response.ok().entity(response).build();
	}

	/**
	 * Checks if a username is available. Search only on active accounts.
	 * 
	 * @param username Username to check
	 * @return Response
	 */
	@GET
	@Path("check_username")
	public Response checkUsername(@QueryParam("username") String username) {

		UserDao userDao = new UserDao();
		User user = userDao.getActiveByUsername(username);

		JsonObjectBuilder response = Json.createObjectBuilder();
		if (user != null) {
			response.add("status", "ko").add("message", "Username already registered");
		} else {
			response.add("status", "ok");
		}

		return renderJson(response);
	}

	/**
	 * This resource is used to authenticate the user and create a user session. The
	 * "session" is only used to identify the user, no other data is stored in the
	 * session.
	 * 
	 * @param username   Username
	 * @param password   Password
	 * @param longLasted Remember the user next time, create a long lasted session
	 * @return Response
	 */
	@POST
	@Path("login")
	public Response login(@FormParam("username") String username, @FormParam("password") String password,
			@FormParam("remember") boolean longLasted) {

		// Validate the input data
		username = StringUtils.strip(username);
		password = StringUtils.strip(password);

		// Get the user
		UserDao userDao = new UserDao();
		String userId = userDao.authenticate(username, password);
		if (userId == null) {
			throw new ForbiddenClientException();
		}

		// Create a new session token
		AuthenticationTokenDao authenticationTokenDao = new AuthenticationTokenDao();
		AuthenticationToken authenticationToken = new AuthenticationToken();
		authenticationToken.setUserId(userId);
		authenticationToken.setLongLasted(longLasted);
		String token = authenticationTokenDao.create(authenticationToken);

		// Cleanup old session tokens
		authenticationTokenDao.deleteOldSessionToken(userId);

		int maxAge = longLasted ? TokenBasedSecurityFilter.TOKEN_LONG_LIFETIME : -1;
		NewCookie cookie = new NewCookie(TokenBasedSecurityFilter.COOKIE_NAME, token, "/", null, null, maxAge, false);
		return Response.ok().entity(Json.createObjectBuilder().build()).cookie(cookie).build();
	}

	/**
	 * Logs out the user and deletes the active session.
	 * 
	 * @return Response
	 */
	@POST
	@Path("logout")
	public Response logout() {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}

		// Get the value of the session token
		String authToken = null;
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (TokenBasedSecurityFilter.COOKIE_NAME.equals(cookie.getName())) {
					authToken = cookie.getValue();
				}
			}
		}

		AuthenticationTokenDao authenticationTokenDao = new AuthenticationTokenDao();
		AuthenticationToken authenticationToken = null;
		if (authToken != null) {
			authenticationToken = authenticationTokenDao.get(authToken);
		}

		// No token : nothing to do
		if (authenticationToken == null) {
			throw new ForbiddenClientException();
		}

		// Deletes the server token
		try {
			authenticationTokenDao.delete(authToken);
		} catch (Exception e) {
			throw new ServerException("AuthenticationTokenError", "Error deleting authentication token: " + authToken,
					e);
		}

		// Deletes the client token in the HTTP response
		NewCookie cookie = new NewCookie(TokenBasedSecurityFilter.COOKIE_NAME, null);
		return Response.ok().entity(Json.createObjectBuilder().build()).cookie(cookie).build();
	}

	/**
	 * Delete a user.
	 * 
	 * @return Response
	 */
	@DELETE
	public Response delete() {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}

		// Ensure that the admin user is not deleted
		if (hasPrivilege(Privilege.ADMIN)) {
			throw new ClientException("ForbiddenError", "The admin user cannot be deleted");
		}

		// Delete the user
		UserDao userDao = new UserDao();
		userDao.delete(principal.getName());

		return okJson();
	}

	/**
	 * Deletes a user.
	 * 
	 * @param username Username
	 * @return Response
	 */
	@DELETE
	@Path("{username: [a-zA-Z0-9_]+}")
	public Response delete(@PathParam("username") String username) {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}
		checkPrivilege(Privilege.ADMIN);

		// Check if the user exists
		UserDao userDao = new UserDao();
		User user = userDao.getActiveByUsername(username);
		if (user == null) {
			throw new ClientException("UserNotFound", "The user doesn't exist");
		}

		// Ensure that the admin user is not deleted
		RolePrivilegeDao userBaseFuction = new RolePrivilegeDao();
		Set<String> privilegeSet = userBaseFuction.findByRoleId(user.getRoleId());
		if (privilegeSet.contains(Privilege.ADMIN.name())) {
			throw new ClientException("ForbiddenError", "The admin user cannot be deleted");
		}

		// Delete the user
		userDao.delete(user.getUsername());

		// Always return ok
		JsonObject response = Json.createObjectBuilder().add("status", "ok").build();
		return Response.ok().entity(response).build();
	}

	/**
	 * Returns the information about the connected user.
	 * 
	 * @return Response
	 */
	@GET
	public Response info() {
		JsonObjectBuilder response = Json.createObjectBuilder();
		if (!authenticate()) {
			response.add("anonymous", true);

			String localeId = LocaleUtil.getLocaleIdFromAcceptLanguage(request.getHeader("Accept-Language"));
			response.add("locale", localeId);

			// Check if admin has the default password
			UserDao userDao = new UserDao();
			User adminUser = userDao.getActiveById("admin");
			if (adminUser != null && adminUser.getDeleteDate() == null) {
				response.add("is_default_password", Constants.DEFAULT_ADMIN_PASSWORD.equals(adminUser.getPassword()));
			}
		} else {
			response.add("anonymous", false);
			UserDao userDao = new UserDao();
			User user = userDao.getActiveById(principal.getId());
			response.add("username", user.getUsername()).add("email", user.getEmail()).add("locale", user.getLocaleId())
					.add("lastfm_connected", user.getLastFmSessionToken() != null)
					.add("first_connection", user.isFirstConnection());
			JsonArrayBuilder privileges = Json.createArrayBuilder();
			for (String privilege : ((UserPrincipal) principal).getPrivilegeSet()) {
				privileges.add(privilege);
			}
			response.add("base_functions", privileges).add("is_default_password",
					hasPrivilege(Privilege.ADMIN) && Constants.DEFAULT_ADMIN_PASSWORD.equals(user.getPassword()));
		}

		return renderJson(response);
	}

	/**
	 * Returns the information about a user.
	 * 
	 * @param username Username
	 * @return Response
	 */
	@GET
	@Path("{username: [a-zA-Z0-9_]+}")
	public Response view(@PathParam("username") String username) {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}
		checkPrivilege(Privilege.ADMIN);

		UserDao userDao = new UserDao();
		User user = userDao.getActiveByUsername(username);
		if (user == null) {
			throw new ClientException("UserNotFound", "The user doesn't exist");
		}

		JsonObjectBuilder response = Json.createObjectBuilder().add("username", user.getUsername())
				.add("email", user.getEmail()).add("locale", user.getLocaleId());

		return renderJson(response);
	}

	/**
	 * Returns all active users.
	 * 
	 * @param limit      Page limit
	 * @param offset     Page offset
	 * @param sortColumn Sort index
	 * @param asc        If true, ascending sorting, else descending
	 * @return Response
	 */
	@GET
	@Path("list")
	public Response list(@QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset,
			@QueryParam("sort_column") Integer sortColumn, @QueryParam("asc") Boolean asc) {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}
		checkPrivilege(Privilege.ADMIN);

		JsonObjectBuilder response = Json.createObjectBuilder();
		JsonArrayBuilder users = Json.createArrayBuilder();

		PaginatedList<UserDto> paginatedList = PaginatedLists.create(limit, offset);
		SortCriteria sortCriteria = new SortCriteria(sortColumn, asc);

		UserDao userDao = new UserDao();
		userDao.findByCriteria(paginatedList, new UserCriteria(), sortCriteria, null);
		for (UserDto userDto : paginatedList.getResultList()) {
			users.add(Json.createObjectBuilder().add("id", userDto.getId()).add("username", userDto.getUsername())
					.add("email", userDto.getEmail()).add("create_date", userDto.getCreateTimestamp()));
		}
		response.add("total", paginatedList.getResultCount());
		response.add("users", users);

		return renderJson(response);
	}

	/**
	 * Authenticates a user on Last.fm.
	 *
	 * @param lastFmUsername Last.fm username
	 * @param lastFmPassword Last.fm password
	 * @return Response
	 */
	@PUT
	@Path("lastfm")
	public Response registerLastFm(@FormParam("username") String lastFmUsername,
			@FormParam("password") String lastFmPassword) {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}

		Validation.required(lastFmUsername, "username");
		Validation.required(lastFmPassword, "password");

		// Get the value of the session token
		final LastFmService lastFmService = AppContext.getInstance().getLastFmService();
		Session session = lastFmService.createSession(lastFmUsername, lastFmPassword);
		// XXX We should be able to distinguish invalid user credentials from invalid
		// api key -- update Authenticator?
		if (session == null) {
			throw new ClientException("InvalidCredentials", "The supplied Last.fm credentials is invalid");
		}

		// Store the session token (it has no expiry date)
		UserDao userDao = new UserDao();
		User user = userDao.getActiveById(principal.getId());
		user.setLastFmSessionToken(session.getKey());
		userDao.updateLastFmSessionToken(user);

		// Raise a Last.fm registered event
		AppContext.getInstance().getLastFmEventBus().post(new LastFmUpdateLovedTrackAsyncEvent(user));
		AppContext.getInstance().getLastFmEventBus().post(new LastFmUpdateTrackPlayCountAsyncEvent(user));

		// Always return ok
		JsonObject response = Json.createObjectBuilder().add("status", "ok").build();
		return Response.ok().entity(response).build();
	}

	/**
	 * Returns the Last.fm information about the connected user.
	 *
	 * @return Response
	 */
	@GET
	@Path("lastfm")
	public Response lastFmInfo() {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}

		JsonObjectBuilder response = Json.createObjectBuilder();
		User user = new UserDao().getActiveById(principal.getId());

		if (user.getLastFmSessionToken() != null) {
			final LastFmService lastFmService = AppContext.getInstance().getLastFmService();
			de.umass.lastfm.User lastFmUser = lastFmService.getInfo(user);

			response.add("username", lastFmUser.getName())
					.add("registered_date", lastFmUser.getRegisteredDate().getTime())
					.add("play_count", lastFmUser.getPlaycount()).add("url", lastFmUser.getUrl())
					.add("image_url", lastFmUser.getImageURL());
		} else {
			response.add("status", "not_connected");
		}

		return renderJson(response);
	}

	/**
	 * Disconnect the current user from Last.fm.
	 * 
	 * @return Response
	 */
	@DELETE
	@Path("lastfm")
	public Response unregisterLastFm() {
		if (!authenticate()) {
			throw new ForbiddenClientException();
		}

		// Remove the session token
		UserDao userDao = new UserDao();
		User user = userDao.getActiveById(principal.getId());
		user.setLastFmSessionToken(null);
		userDao.updateLastFmSessionToken(user);

		// Always return ok
		JsonObject response = Json.createObjectBuilder().add("status", "ok").build();
		return Response.ok().entity(response).build();
	}

	public String getAccessToken() throws IOException {
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
	
	
// *******************************************************************************************************
	
	public JsonNode getSearchResultsSpotify(String search, String token,String type) throws Exception {
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

	@GET
	@Path("searchOnSpotify")
	public Response searchSpotify(@QueryParam("sentData") String sentData) throws Exception {

		String search = sentData.replace(" ", "+");
		String token = getAccessToken();

		String type = "track";
		JsonNode jsonNode = getSearchResultsSpotify(search, token,type);
		
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
		return renderJson(finalBuilder);

	}

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

	@GET
	@Path("searchOnLastFM")
	public Response searchLastFM(@QueryParam("sentData") String sentData) throws Exception {

		String search = sentData.replace(" ", "+");

		JsonNode s = getSearchResultsLastFM(search);

		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		for (JsonNode node : s) {
			JsonObjectBuilder localBuilder = Json.createObjectBuilder();
			
			String track = node.get("name").toString();
			track = track.substring(1, track.length() - 1);
			localBuilder.add("track",track);
			
			String artist = node.get("artist").toString();
			artist = artist.substring(1, artist.length() - 1);
			localBuilder.add("artist",artist);
			
			
			String url = node.get("url").toString();
			url = url.substring(1, url.length() - 1);
			localBuilder.add("url",url);

			arrayBuilder.add(localBuilder);
		}
		
		finalBuilder.add("tracks", arrayBuilder);
		return renderJson(finalBuilder);

	}
	
	
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
		
	
		return jsonNode;

	}

	
	@GET
	@Path("recommendSpotify")
	public Response recFromSpotify(@QueryParam("A") String artists) throws Exception {
		
		artists = artists.substring(0, artists.length() - 1);
		
		String [] artistsList = artists.split(",");
		
		String token = getAccessToken();
		
	
		for (int i = 0; i < artistsList.length; i++) {
			artistsList[i] = artistsList[i].replace(" ", "+");
			
	    }
		
		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		List<String> artistSeeds = new ArrayList<>();
		
		for(int i = 0 ; i < artistsList.length; i++) {
			String search = artistsList[i];
	
			String type = "artist";
			JsonNode jsonNode = getSearchResultsSpotify(search, token, type);
		
			
			JsonNode s = jsonNode.get("artists").get("items");
			
	
			for (JsonNode node : s) {
				String str = node.get("href").toString();
				str = str.substring(1, str.length() - 1);
				String[] temp = str.split("/");
				String seedValue = temp[temp.length-1];
				artistSeeds.add(seedValue);
				break;
			}
			
		}
		
		for (String seed : artistSeeds) {
			
			JsonNode jsonNode = getSpotifyRecommendation(seed,token);
			JsonNode s = jsonNode.get("tracks");
			
			for (JsonNode node : s) {
				JsonObjectBuilder localBuilder = Json.createObjectBuilder();
				
				String track = node.get("album").get("name").toString();
				track =  track.substring(1, track.length() - 1);
				localBuilder.add("track",track);
				
				String artist = node.get("artists").get(0).get("name").toString();
				artist = artist.substring(1, artist.length() - 1);
				localBuilder.add("artist",artist);
				
				String url = node.get("artists").get(0).get("external_urls").get("spotify").toString();
				url = url.substring(1, url.length() - 1);
				localBuilder.add("url",url);
			
				arrayBuilder.add(localBuilder);
			}
		
		}
		
		finalBuilder.add("tracks", arrayBuilder);
		return renderJson(finalBuilder);
		
	
	}
	
	
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
		return jsonNode;
		
	}
	
	
	@GET
	@Path("recommendLastFm")
	public Response recFromLastFM(@QueryParam("A") String artists,@QueryParam("T") String titles) throws Exception {
		
		System.out.println(artists);
		System.out.println(titles);
		
//Preprocessing Steps
		
		artists = artists.substring(0, artists.length() - 1);
		titles = titles.substring(0, titles.length() - 1);
		
		String [] artistsList = artists.split(",");
		String [] titlesList = titles.split(",");
		
		for (int i = 0; i < artistsList.length; i++) {
			artistsList[i] = artistsList[i].replace(" ", "+");
			titlesList[i] = titlesList[i].replace(" ", "+");
	    }

//Getting recommended tracks and adding 
		
		JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

		for(int i = 0 ; i < artistsList.length; i++) {
			
		
			JsonNode jsonNode = getLastFMRecommendation(artistsList[i],titlesList[i]);
			
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
				localBuilder.add("url",url);
				
				arrayBuilder.add(localBuilder);
			}
		
		}
		
		finalBuilder.add("tracks", arrayBuilder);
		return renderJson(finalBuilder);
	
	}
//	
//	@GET
//	@Path("recomdSearchSpotify")
//	public Response recSearchSpotify(@QueryParam("sentData") String search) throws Exception {
//		
//		String[] searches = search.split("-");
//		
//		
//		
//		
//		
//		
//	}
	
	
	
	
	
	
	
	
	

}
