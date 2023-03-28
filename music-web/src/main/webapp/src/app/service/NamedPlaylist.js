'use strict';

/**
 * Named playlist service.
 */
angular.module('music').factory('NamedPlaylist', function($rootScope, $modal, Restangular, toaster) {
  $rootScope.playlists = [];
  var service = {};

  service = {
    update: function() {
      Restangular.one('playlist').get({
        limit: 1000
      }).then(function() {
        console.log("HERE");
        $rootScope.playlists = data.items;
        console.log(data.items)
      });
    },

    addToPlaylist: function(playlist, tracks) {
      console.log(playlist.id);
      Restangular.one('playlist/' + playlist.id + '/multiple').put({
        id: playlist.id,
        ids: _.pluck(tracks, 'id'),
        clear: false
      }).then(function(data) {
        if(data.status == "success"){
          toaster.pop('success', 'Track' + (tracks.length > 1 ? 's' : '') + ' added to ' + playlist.name,
            _.pluck(tracks, 'title').join('\n'));
        }
      });
    },

    removeTrack: function(playlist, order) {
      return Restangular.one('playlist/' + playlist.id, order).remove().then(function(data){
        console.log(data);
      });
    },

    remove: function(playlist) {
      return Restangular.one('playlist/' + playlist.id).remove().then(function(data) {
        console.log(data);
        if(data.status != 'failure'){
          $rootScope.playlists = _.reject($rootScope.playlists, function(p) {
            return p.id === playlist.id;
          });
        }
      });
    },

    moveTrack: function(playlist, order, neworder) {
      return Restangular.one('playlist/' + playlist.id, order).post('move', {
        neworder: neworder
      });
    },

    createPlaylist: function(tracks) {
      $modal.open({
        templateUrl: 'partial/modal.createplaylist.html',
        controller: function($scope, $modalInstance) {
          'ngInject';
          // 0 for private
          // 1 for public
          // 2 for collaborative
          $scope.playlistType=0;
          $scope.ok = function (name, allowedUsers) {
            $modalInstance.close({name:name, allowedUsers:allowedUsers, playlistType:$scope.playlistType});
          }

          $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
          };

          $scope.changePlaylistType = function(playlistType) {
            $scope.playlistType=playlistType;
          }
        }
      }).result.then(function(data) {
        Restangular.one('playlist').put({
          name: data.name,
          allowedUsers: data.allowedUsers,
          playlistType: data.playlistType
        }).then(function(data) {
          $rootScope.playlists.push(data.item);
          service.addToPlaylist(data.item, tracks);
          toaster.pop('success', 'Playlist created', name);
        });
      });
    }
  };

  return service;
});