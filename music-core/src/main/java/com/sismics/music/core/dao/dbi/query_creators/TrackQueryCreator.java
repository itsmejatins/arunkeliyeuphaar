package com.sismics.music.core.dao.dbi.query_creators;

import com.sismics.music.core.dao.dbi.criteria.TrackCriteria;
import com.sismics.music.core.dao.dbi.dto.TrackDto;
import com.sismics.music.core.dao.dbi.mapper.TrackDtoMapper;
import com.sismics.music.core.dao.dbi.mapper.TrackMapper;
import com.sismics.music.core.model.dbi.Track;
import com.sismics.music.core.util.dbi.QueryParam;
import com.sismics.music.core.util.dbi.SortCriteria;
import com.sismics.util.context.ThreadLocalContext;
import com.sismics.util.dbi.BaseDao;
import com.sismics.util.dbi.filter.FilterCriteria;
import org.skife.jdbi.v2.Handle;
import com.sismics.music.core.dao.dbi.query_creators.QueryCreator;
import com.sismics.music.core.dao.dbi.query_creators.Query;
import com.sismics.music.core.dao.dbi.criteria.Criteria;


import java.sql.Timestamp;
import java.util.*;


public class TrackQueryCreator extends QueryCreator{
        public TrackQuery createQuery(){
            TrackQuery trackQuery = new TrackQuery();
            return trackQuery;
        }

        private class TrackQuery implements Query{
            // Adds search criteria
            public void addCriteria(Criteria criteria, List<String> criteriaList, Map<String, Object> parameterMap){
                // Adds search criteria
                criteriaList.add("t.deletedate is null");
                if (criteria.getPlaylistId() != null) {
                    criteriaList.add("pt.track_id = t.id");
                    criteriaList.add("pt.playlist_id = :playlistId");
                    parameterMap.put("playlistId", criteria.getPlaylistId());
                }
                if (criteria.getAlbumId() != null) {
                    criteriaList.add("t.album_id = :albumId");
                    parameterMap.put("albumId", criteria.getAlbumId());
                }
                if (criteria.getDirectoryId() != null) {
                    criteriaList.add("alb.directory_id = :directoryId");
                    parameterMap.put("directoryId", criteria.getDirectoryId());
                }
                if (criteria.getArtistId() != null) {
                    criteriaList.add("a.id = :artistId");
                    parameterMap.put("artistId", criteria.getArtistId());
                }
                if (criteria.getTitle() != null) {
                    criteriaList.add("lower(t.title) like lower(:title)");
                    parameterMap.put("title", criteria.getTitle());
                }
                if (criteria.getArtistName() != null) {
                    criteriaList.add("lower(a.name) like lower(:artistName)");
                    parameterMap.put("artistName", criteria.getArtistName());
                }
                if (criteria.getLike() != null) {
                    criteriaList.add("(lower(t.title) like lower(:like) or lower(alb.name) like lower(:like) or lower(a.name) like lower(:like))");
                    parameterMap.put("like", "%" + criteria.getLike() + "%");
                }
                if (criteria.getUserId() != null) {
                    parameterMap.put("userId", criteria.getUserId());
                }
            }

            @Override
            public StringBuilder buildString(Criteria criteria){
                StringBuilder sb = new StringBuilder("select t.id as id, t.filename as fileName, t.title as title, t.year as year, t.genre as genre, t.length as length, t.bitrate as bitrate, t.number as trackOrder, t.vbr as vbr, t.format as format, t.ownerid as ownerId,");
                if (criteria.getUserId() != null) {
                    sb.append(" ut.playcount as userTrackPlayCount, ut.liked userTrackLike, ");
                } else {
                    sb.append(" 0 as userTrackPlayCount, false as userTrackLike, ");
                }
                sb.append(" a.id as artistId, a.name as artistName, t.album_id as albumId, alb.name as albumName, alb.albumart as albumArt");
                if (criteria.getPlaylistId() != null) {
                    sb.append("  from t_playlist_track pt, t_track t ");
                } else {
                    sb.append("  from t_track t ");
                }
                sb.append("  join t_artist a on(a.id = t.artist_id and a.deletedate is null)");
                sb.append("  join t_album alb on(t.album_id = alb.id and alb.deletedate is null)");
                if (criteria.getUserId() != null) {
                    sb.append("  left join t_user_track ut on(ut.track_id = t.id and ut.user_id = :userId and ut.deletedate is null)");
                }
                return sb;
            }
        }
    }