package com.sismics.thirdparty.recommendation;

import javax.ws.rs.core.Response;

public interface RecommendationStrategy {
	
	public Response recommend(RecommendDto dto) throws Exception;

}
