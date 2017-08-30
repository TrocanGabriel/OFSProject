
var app=angular.module('Orange',['ngRoute']);

app.config(['$routeProvider',function($routeProvider){
	
	
$routeProvider
 	.when('/home',{
      templateUrl: 'views/home.html'
    })
    .when('/contact',{
      templateUrl: 'views/contact.html',
      controller: 'ContactController'
    })
    .when('/contact-success',{
      templateUrl: 'views/contact-success.html',
      controller: 'ContactController'
    })
     .when('/update',{
      templateUrl: 'views/update.html',
      controller: 'UpdateController'
    })
    .when('/directory',{
      templateUrl: 'views/directory.html',
      controller: 'ServicesController'
    }).otherwise({
      redirectTo: '/home'
    });
    

}]);


app.controller('ServicesController',['$scope','$http', '$templateCache', function($scope, $http, $templateCache) {

	$scope.method = 'GET';
	$scope.url = 'http://localhost:8080/orangeproject/webapi/customers';
	$scope.getUsers = function() {
	$scope.users = [];
	$scope.userX = null;
	$http({method: $scope.method, url:$scope.url}).
	then(function(response){
	$scope.status = response.status;
	$scope.users = response.data;
	$scope.found = false;
	}, function(response) {
	$scope.users = response.data || 'Request failed';
          $scope.status = response.status;
	});
};
	
	
	$scope.searchUser = function() {
		$scope.users = [];
	$http({method: $scope.method, url: $scope.url + '/' + $scope.searchedMsisdn}).
	then(function(response){
	$scope.status = response.status;
	$scope.userX = response.data;
	$scope.found = true;
	}, function(response) {
	$scope.userX = response.data || 'Request failed';
          $scope.status = response.status;
	});
	
	$scope.searchedMsisdn = '';
};
	
	$scope.delMethod = 'DELETE';
	$scope.removeUser = function(index) {
		var deletedUser = $scope.users[index];
		$http({method: $scope.delMethod, url: $scope.url + '/delete/' + deletedUser['msisdn']}).
		then(function(response){
	          $scope.status = response.status;
			$scope.users.splice(index,1);
		}, function(response){
	          $scope.status = response.status;
				$scope.users.splice(index,1);

		});
	};
	
	$scope.delMethod = 'DELETE';
	$scope.removeUserX = function(userX) {
		$http({method: $scope.delMethod, url: $scope.url + '/delete/' + $scope.userX['msisdn']}).
		then(function(response){
	          $scope.status = response.status;
		}, function(response){
	          $scope.status = response.status;
				$scope.found = false;
				$scope.userX = [];

		});
	};
}]);


app.controller('ContactController',['$scope', '$location',function($scope, $location){

$scope.sendMessage = function(){
  $location.path('/contact-success');
};
	
}]);


app.controller('UpdateController',['$scope','$http', '$templateCache', function($scope, $http, $templateCache) {

	$scope.form = true;
$scope.putMethod = 'PUT';
	$scope.url = 'http://localhost:8080/orangeproject/webapi/customers';
	$scope.updateUser = function() {
	$http({method: $scope.putMethod, url:$scope.url + '/' + $scope.updatedMsisdn 
		+ '/' + $scope.updatedField + '/' + $scope.updatedValue}).
		then(function(response){
	          $scope.status = response.status;
			$scope.userX = [];
		}, function(response){
	          $scope.status = response.status;
		});
	};
	$scope.postMethod = 'POST';
	$scope.url = 'http://localhost:8080/orangeproject/webapi/customers';
	$scope.addUser = function() {
		var data = $scope.fields;
	$http({method: $scope.postMethod, url:$scope.url, data: data}).
		then(function(response){
	          $scope.status = response.status;
		}, function(response){
	          $scope.status = response.status;
		});
	};
}]);