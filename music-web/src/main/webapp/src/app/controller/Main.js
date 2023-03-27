'use strict';

/**
 * Main controller.
 */
angular.module('music').controller('Main', function($rootScope, $state, $scope, Playlist, Album, Restangular) {
	$scope.register = function() {
		var promise = null;
		promise = Restangular
			.one('user')
			.put($scope.user);
	}
	$scope.partyMode = Playlist.isPartyMode();

	$scope.searchSpotifySongs = function(searchData) {

		Restangular.one('user/searchOnSpotify').get({ sentData: searchData }).then(function(response) {
			$scope.myData = response.tracks;

		})
	}

	$scope.searchLastFMSongs = function(searchData) {

		Restangular.one('user/searchOnLastFM').get({ sentData: searchData }).then(function(response) {

			$scope.myData = response.tracks;

		})

	}

	$scope.searchSpotifyRecom = function(searchData) {

		var myArray = searchData.split("-");

		var promises = [];

		for (var i = 0; i < myArray.length; i++) {

			var promise = Restangular.one('user/searchOnSpotify').get({ sentData: myArray[i] });
			promises.push(promise);

		}

		Promise.all(promises).then(function(responses) {
			var dataa = [];
			for (var j = 0; j < responses.length; j++) {
				dataa = dataa.concat(responses[j].tracks);
			}
			$scope.rData = dataa;

		});


	}

	$scope.searchLastFMRecom = function(searchData) {

		var myArray = searchData.split("-");

		var promises = [];

		for (var i = 0; i < myArray.length; i++) {

			var promise = Restangular.one('user/searchOnLastFM').get({ sentData: myArray[i] });
			promises.push(promise);

		}

		Promise.all(promises).then(function(responses) {
			var dataa = [];
			for (var j = 0; j < responses.length; j++) {
				dataa = dataa.concat(responses[j].tracks);
			}
			$scope.rData = dataa;

		});

	}

	// Keep party mode in sync
	$rootScope.$on('playlist.party', function(e, partyMode) {
		$scope.partyMode = partyMode;
	});

	// Start party mode
	$scope.startPartyMode = function() {
		Playlist.party(true, true);
		$state.transitionTo('main.playing');
	};

	// Stop party mode
	$scope.stopPartyMode = function() {
		Playlist.setPartyMode(false);
	};

	// Clear the albums cache if the previous state is not main.album
	$scope.$on('$stateChangeStart', function(e, to, toParams, from) {
		if (to.name == 'main.music.albums' && from.name != 'main.album') {
			Album.clearCache();
		}
	});
});