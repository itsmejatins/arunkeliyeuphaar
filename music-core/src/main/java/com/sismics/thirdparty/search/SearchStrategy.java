package com.sismics.thirdparty.search;

import javax.ws.rs.core.Response;

public interface SearchStrategy {

	public Response search(String param) throws Exception;

}
