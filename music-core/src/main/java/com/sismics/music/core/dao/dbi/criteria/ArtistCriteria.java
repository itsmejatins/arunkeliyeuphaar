package com.sismics.music.core.dao.dbi.criteria;
import com.sismics.music.core.dao.dbi.criteria.Criteria;

/**
 * Artist criteria.
 *
 * @author bgamard
 */
public class ArtistCriteria extends Criteria {
    /**
     * Artist ID.
     */
    private String id;
    
    /**
     * Artist name (like).
     */
    private String nameLike;

    public String getId() {
        return this.id;
    }

    public String getNameLike() {
        return nameLike;
    }

    public ArtistCriteria setNameLike(String nameLike) {
        this.nameLike = nameLike;
        return this;
    }

    public ArtistCriteria setId(String id) {
        this.id = id;
        return this;
    }
}
