package com.sismics.music.core.model.dbi;

import com.google.common.base.Objects;
import com.sismics.music.core.dao.dbi.PlaylistDao;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Playlist entity.
 * A playlist is either the list of current played tracks (the "default" playlist), or a saved playlist with a name.
 * 
 * @author jtremeaux
 */
public class Playlist {
    /**
     * Playlist ID.
     */
    private String id;

    /**
     * User ID.
     */
    private String userId;

    /**
     * Playlist name.
     */
    private String name;

    private ArrayList<String> allowedUsers = new ArrayList<String>();

    private Integer playlistType;


    public Playlist() {
    }

    public Playlist(String id) {
        this.id = id;
    }

    public Playlist(String id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    public Playlist(String id, String userId, Integer playlistType, ArrayList<String> allowedUsers){
        this.id = id;
        this.userId = userId;
        this.playlistType = playlistType;
        this.allowedUsers = allowedUsers;
    }

    /**
     * Getter of id.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Setter of id.
     *
     * @param id id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Getter of userId.
     *
     * @return userId
     */
    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getActualName() {
        return name.split("\\$")[0];
    }

    public Integer getPlaylistType() {
        Integer ret = Integer.parseInt(name.split("\\$")[1]);
        return ret;
    }

    public ArrayList<String> getAllowedUsers() {
        String au = name.split("\\$")[2];
        ArrayList<String> allowed = new ArrayList<String>();
        allowed.addAll(Arrays.asList(au.split(" ")));
        return allowed;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Setter of userId.
     *
     * @param userId userId
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPlaylistType(Integer playlistType){
        this.playlistType = playlistType;
    }

    public void setAllowedUsers(ArrayList<String> allowedUsers){
        this.allowedUsers = allowedUsers;
    }

    /**
     * Create a named playlist.
     *
     * @param playlist The playlist to create
     */
    public static void createPlaylist(Playlist playlist) {
        playlist.id = UUID.randomUUID().toString();
        new PlaylistDao().create(playlist);
    }

    /**
     * Update a named playlist.
     *
     * @param playlist The playlist to update
     */
    public static void updatePlaylist(Playlist playlist) {
        new PlaylistDao().update(playlist);
    }

    /**
     * Delete a named playlist.
     *
     * @param playlist The playlist to delete
     */
    public static void deletePlaylist(Playlist playlist) {
        new PlaylistDao().delete(playlist);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("userId", userId)
                .add("name", name)
                .toString();
    }
}
