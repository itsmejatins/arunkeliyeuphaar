package com.sismics.music.core.dao.dbi.query_creators;

import com.sismics.music.core.dao.dbi.criteria.ArtistCriteria;
import com.sismics.music.core.dao.dbi.criteria.Criteria;
import com.sismics.music.core.dao.dbi.dto.ArtistDto;
import com.sismics.music.core.dao.dbi.mapper.ArtistDtoMapper;
import com.sismics.music.core.dao.dbi.mapper.ArtistMapper;
import com.sismics.music.core.dao.dbi.query_creators.QueryCreator;
import com.sismics.music.core.dao.dbi.query_creators.Query;
import com.sismics.music.core.model.dbi.Artist;
import com.sismics.music.core.util.dbi.QueryParam;
import com.sismics.util.context.ThreadLocalContext;
import com.sismics.util.dbi.BaseDao;
import com.sismics.util.dbi.filter.FilterCriteria;
import org.skife.jdbi.v2.Handle;

import java.sql.Timestamp;
import java.util.*;

public class ArtistQueryCreator extends QueryCreator{
        public ArtistQuery createQuery(){
            ArtistQuery artistQuery = new ArtistQuery();
            return artistQuery;
        }

        private class ArtistQuery implements Query{
            // Adds search criteria
            public void addCriteria(Criteria criteria, List<String> criteriaList, Map<String, Object> parameterMap){
                // Adds search criteria
                criteriaList.add("a.deletedate is null");
                if (criteria.getId() != null) {
                    criteriaList.add("a.id = :id");
                    parameterMap.put("id", criteria.getId());
                }
                if (criteria.getNameLike() != null) {
                    criteriaList.add("lower(a.name) like lower(:nameLike)");
                    parameterMap.put("nameLike", "%" + criteria.getNameLike() + "%");
                }
            }

            @Override
            public StringBuilder buildString(Criteria criteria){
                StringBuilder sb = new StringBuilder("select a.id as id, a.name as c0 ");
                sb.append(" from t_artist a ");
                return sb;
            }
        }



    }