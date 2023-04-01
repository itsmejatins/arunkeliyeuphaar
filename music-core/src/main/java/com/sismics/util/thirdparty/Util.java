package com.sismics.util.thirdparty;

import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

public class Util {
	public static Response renderJson(JsonObjectBuilder response) {
		return Response.ok().entity(response.build()).build();
	}

}
