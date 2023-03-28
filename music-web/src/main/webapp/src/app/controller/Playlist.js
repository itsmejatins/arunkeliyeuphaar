'use strict';

/**
 * Playlist controller.
 */
angular.module('music').controller('Playlist', function($scope, $state, $stateParams, Restangular, Playlist, NamedPlaylist) {
  // Load playlist
  Restangular.one('playlist', $stateParams.id).get().then(function(data) {
    $scope.playlist = data;
  });
  
  //Recommend from Spotify
  $scope.spotifyRecommendation = function() {
    console.log("Reached in spotifyRecommendation")
    console.log($scope.playlist.tracks)
    
    var tracks = $scope.playlist.tracks;
    console.log(tracks);
    
    //artist.name
    
    var artists = ""
    
    for (let prop in tracks) {
		var obj = tracks[prop];
		artists += obj.artist.name;
		artists += ",";
	}
   
    console.log(artists);
    Restangular.one('user/recommendThirdParty').get({ A:artists, T:null, service: "spotify"}).then(function(response) {
		$scope.spotify = response.tracks;
		console.log(response);

	})
    
    
  };

  //Recommend from LastFM
  $scope.lastFMRecommendation = function() {
    console.log("Reached in lastFMRecommendation")
    
    var tracks = $scope.playlist.tracks;
    console.log(tracks);
    
    //artist.name
    //title
    
    var titles = ""
    var artists = ""
    
    for (let prop in tracks) {
  			var obj = tracks[prop];
  			titles += obj.title;
  			titles += ",";
  			
  			artists += obj.artist.name;
  			artists += ",";
	}
    console.log(titles);
    console.log(artists);
    Restangular.one('user/recommendThirdParty').get({ A:artists ,  T: titles, service:"lastfm" }).then(function(response) {
		$scope.lastFM = response.tracks;
		console.log(response);
	

	})
    
  };

  // Play a single track
  $scope.playTrack = function(track) {
    Playlist.removeAndPlay(track);
  };

  // Add a single track to the playlist
  $scope.addTrack = function(track) {
    Playlist.add(track, false);
  };

  // Add all tracks to the playlist in a random order
  $scope.shuffleAllTracks = function() {
    Playlist.addAll(_.shuffle(_.pluck($scope.playlist.tracks, 'id')), false);
  };

  // Play all tracks
  $scope.playAllTracks = function() {
    Playlist.removeAndPlayAll(_.pluck($scope.playlist.tracks, 'id'));
  };

  // Add all tracks to the playlist
  $scope.addAllTracks = function() {
    Playlist.addAll(_.pluck($scope.playlist.tracks, 'id'), false);
  };

  // Like/unlike a track
  $scope.toggleLikeTrack = function(track) {
    Playlist.likeById(track.id, !track.liked);
  };

  // Remove a track
  $scope.removeTrack = function(order) {
    NamedPlaylist.removeTrack($scope.playlist, order).then(function(data) {
      $scope.playlist = data;
    });
  };

  // Delete the playlist
  $scope.remove = function() {
    NamedPlaylist.remove($scope.playlist).then(function() {
      $state.go('main.default');
    });
  };

  // Update UI on track liked
  $scope.$on('track.liked', function(e, trackId, liked) {
    var track = _.findWhere($scope.playlist.tracks, { id: trackId });
    if (track) {
      track.liked = liked;
    }
  });

  // Configuration for track sorting
  $scope.trackSortableOptions = {
    forceHelperSize: true,
    forcePlaceholderSize: true,
    tolerance: 'pointer',
    handle: '.handle',
    containment: 'parent',
    helper: function(e, ui) {
      ui.children().each(function() {
        $(this).width($(this).width());
      });
      return ui;
    },
    stop: function (e, ui) {
      // Send new positions to server
      $scope.$apply(function () {
        NamedPlaylist.moveTrack($scope.playlist, ui.item.attr('data-order'), ui.item.index());
      });
    }
  };
});