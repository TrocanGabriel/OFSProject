var app=angular.module('Orange',['ngRoute','ngAnimate','ngMaterial']);


angular.element(document).ready( function($http) {
	  window._keycloak = Keycloak({
			  "realm": "OrangeProject",
			  "auth-server-url": "http://localhost:8080/auth",
			  "ssl-required": "external",
			  "clientId": "frontend",
			  "credentials": {
			    "secret": "c33a0987-ce79-4d23-ab33-0cb4a2f19c75"
			  },
			  "use-resource-role-mappings": true
		
	  });

	  window._keycloak.init({
	      onLoad: 'login-required'
	    })
	     .success((authenticated) => {
        if(authenticated) {
            window._keycloak.loadUserProfile().success(function(profile){
  
                angular.bootstrap(document, ['Orange']); // manually bootstrap Angular
                
            });
        }
        else {
            window.location.reload();
        }
    })
    .error(function () {
        window.location.reload();
    });
});

angular.module('Orange').factory('keycloak', $window => {
	  return $window._keycloak;
	});

//use bearer token when calling backend
app.config(['$httpProvider', function($httpProvider) {
    var token = window._keycloak.token;     
    $httpProvider.defaults.headers.common['Authorization'] = 'BEARER ' + token;
}]);

app.config(['$routeProvider',function($routeProvider){
	
	
$routeProvider
 	.when('/home',{
      templateUrl: 'views/home.html',
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
    })
    .otherwise({
      redirectTo: '/home'
    });
    

}]);

app.controller('HeaderController',['$scope','keycloak', function($scope,keycloak) {
	
	$scope.logoutMess ="HELLO LOG"
	$scope.userLogout = function(){
		keycloak.logout();
		};
}]);



app.controller('ServicesController',['$scope','$http','keycloak', '$templateCache', function($scope, $http, $templateCache, keycloak) {

	
	$scope.method = 'GET';
	$scope.url = 'http://localhost:9090/orangeproject/webapi/customers';
	$scope.getUsers = function() {
		  $scope.isTokenExpired = keycloak.isTokenExpired;
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
		$scope.searchUserUrl = $scope.url + '/' + $scope.searchedMsisdn;
		$scope.users = [];
		if($scope.option == 1 || $scope.option == 2){
			$scope.searchUserUrl += '/switch?location=' + $scope.option;
		}
			
	$http({method: $scope.method, url: $scope.searchUserUrl}).
	then(function(response){
	$scope.status = response.status;
	$scope.userX = response.data;
	$scope.found = true;
    $scope.testCheck = {}

	}, function(response) {
	$scope.userX = response.data || 'Request failed';
          $scope.status = response.status;
          $scope.testCheck = {}
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
	
	$scope.addUsers = function(){
		$scope.users = [];
		$scope.userX = null;

		$http({method: $scope.method, url: $scope.url + '/addSubscribers'}).
		then(function(response){
	          $scope.status = response.status;
	          $scope.users = response.data;
	      	$scope.found = false;
		}, function(response){
			$scope.users = response.data || 'Request failed';
	          $scope.status = response.status;		
	    });
	}
}]);


app.controller('ContactController',['$scope', '$location',function($scope, $location){

$scope.sendMessage = function(){
  $location.path('/contact-success');
};
	
}]);


app.controller('UpdateController',['$scope','$http', '$templateCache','$window', function($scope, $http, $templateCache,$window) {

	$scope.inform = 'Data was update successfully'
	$scope.form = true;
$scope.putMethod = 'PUT';
	$scope.url = 'http://localhost:9090/orangeproject/webapi/customers';
	$scope.updateUser = function() {
	$http({method: $scope.putMethod, url:$scope.url + '/' + $scope.updatedMsisdn 
		+ '/' + $scope.updatedField + '/' + $scope.updatedValue}).
		then(function(response){
	          $scope.status = response.status;
			$scope.userX = [];
		     $window.alert($scope.inform);
		}, function(response){
	          $scope.status = response.status;
		});
	};
	$scope.postMethod = 'POST';
	$scope.url = 'http://localhost:9090/orangeproject/webapi/customers';
	$scope.addUser = function() {
		var data = $scope.fields;
	$http({method: $scope.postMethod, url:$scope.url, data: data}).
		then(function(response){
	          $scope.status = response.status;
	          $window.alert($scope.inform);
		}, function(response){
	          $scope.status = response.status;
		});
	};

}]);