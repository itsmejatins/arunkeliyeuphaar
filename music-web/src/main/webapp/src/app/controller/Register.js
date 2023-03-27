angular.module('music').controller('Register', function($scope, $dialog, $state, $stateParams, Restangular) {

    $scope.register=function(){
        var promise = null;
        promise = Restangular
      .one('user')
      .put($scope.user);
    }
})