package com.sismics.music.core.dao.dbi.query_creators;

import com.google.common.collect.Lists;
import com.sismics.music.core.dao.dbi.criteria.Criteria;
import com.sismics.music.core.dao.dbi.criteria.AlbumCriteria;
import com.sismics.music.core.dao.dbi.dto.AlbumDto;
import com.sismics.music.core.dao.dbi.mapper.AlbumDtoMapper;
import com.sismics.music.core.dao.dbi.mapper.AlbumMapper;
import com.sismics.music.core.model.dbi.Album;
import com.sismics.music.core.util.dbi.QueryParam;
import com.sismics.util.context.ThreadLocalContext;
import com.sismics.util.dbi.BaseDao;
import com.sismics.util.dbi.filter.FilterCriteria;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.util.IntegerMapper;

import java.sql.Timestamp;
import java.util.*;


interface Query{
    public void addCriteria(Criteria criteria, List<String> criteriaList, Map<String, Object> parameterMap);
    public StringBuilder buildString(Criteria criteria);
}

public abstract class QueryCreator{
    public abstract Query createQuery();
    public StringBuilder getQueryParams(Criteria criteria, List<String> criteriaList, Map<String, Object> parameterMap){
        Query query = createQuery();
        StringBuilder sb = query.buildString(criteria);
        query.addCriteria(criteria, criteriaList, parameterMap);
        return sb;
    }    
}





// private class AlbumQuery implements Query{
//     // Adds search criteria
//     public void addCriteria(AlbumCriteria criteria, List<String> criteriaList, Map<String, Object> parameterMap){
//         criteriaList.add("ar.deletedate is null");
//         criteriaList.add("a.deletedate is null");
//         if (criteria.getId() != null) {
//             criteriaList.add("a.id = :id");
//             parameterMap.put("id", criteria.getId());
//         }
//         if (criteria.getDirectoryId() != null) {
//             criteriaList.add("a.directory_id = :directoryId");
//             parameterMap.put("directoryId", criteria.getDirectoryId());
//         }
//         if (criteria.getArtistId() != null) {
//             criteriaList.add("ar.id = :artistId");
//             parameterMap.put("artistId", criteria.getArtistId());
//         }
//         if (criteria.getNameLike() != null) {
//             criteriaList.add("(lower(a.name) like lower(:like) or lower(ar.name) like lower(:like))");
//             parameterMap.put("like", "%" + criteria.getNameLike() + "%");
//         }
//     }

//     public StringBuilder buildString(AlbumCriteria criteria){
//         StringBuilder sb = new StringBuilder("select a.id as id, a.name as c0, a.albumart as albumArt, a.artist_id as artistId, ar.name as artistName, a.updatedate as c1, ");
//         if (criteria.getUserId() == null) {
//             sb.append("sum(0) as c2");
//         } else {
//             sb.append("sum(utr.playcount) as c2");
//         }
//         sb.append(" from t_album a ");
//         sb.append(" join t_artist ar on(ar.id = a.artist_id) ");
//         if (criteria.getUserId() != null) {
//             sb.append(" left join t_track tr on(tr.album_id = a.id) ");
//             sb.append(" left join t_user_track utr on(tr.id = utr.track_id) ");
//         }
//         return sb;
//     }
// }