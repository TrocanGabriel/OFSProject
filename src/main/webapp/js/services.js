var app=angular.module('Orange',['ngRoute','ngAnimate','ngMaterial']);

var auth = {};
var jsonData;
var client;
var authorization;

angular.element(document).ready( function($scope,$http) {
	  window._keycloak = Keycloak({
		  "realm": "OrangeProject",
		  "auth-server-url": "http://localhost:8081/auth",
		  "ssl-required": "external",
		  "clientId": "frontend",
		  "credentials": {
		    "secret": "c33a0987-ce79-4d23-ab33-0cb4a2f19c75"
		  },
		  "use-resource-role-mappings": true
		}
	  
);


	  window._keycloak.init({
	      onLoad: 'login-required'
	    })
	     .success((authenticated) => {
        if(authenticated) {
            window._keycloak.loadUserProfile().success(function(profile){
            	jsonData = JSON.parse(JSON.stringify(window._keycloak.tokenParsed))
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
	
	$scope.userLogout = function(){
		keycloak.logout();
		};
}]);



app.controller('ServicesController',['$scope','$http','keycloak', function($scope, $http, keycloak) {

	
	$scope.method = 'GET';
	$scope.url = 'http://localhost:9090/orangeproject/webapi/customers';
	
	$scope.getSubscribers = function() {
		
	$scope.subscribers = [];
	$scope.subscriberX = null;
	$http({method: $scope.method, url:$scope.url}).
	then(function(response){
	$scope.status = response.status;
	$scope.subscribers = response.data;
	$scope.found = false;
    $scope.deleted = true;
    if($scope.subscribers.length == 0){	 
		 window.alert('No Subscribers to show from cache!');
		}
	}, function(response) {
	$scope.subscribers = response.data || 'Request failed';
          $scope.status = response.status;
	});

};

	$scope.getSubscribersCache = function() {
	$scope.subscribers = [];
	$scope.subscriberX = null;
	$http({method: $scope.method, url:$scope.url + '/cache'}).
	then(function(response){
	$scope.status = response.status;
	$scope.subscribers = response.data;
	$scope.found = false;
	$scope.deleted = true;
	if($scope.subscribers.length == 0){	 
		 window.alert('No Subscribers to show from cache!');
		}
	}, function(response) {
	$scope.subscribers = response.data || 'Request failed';
	$scope.found = true;
	$scope.status = response.status;
});
	
};


	
	
	$scope.searchSubscriber = function() {
		$scope.searchSubscriberUrl = $scope.url + '/' + $scope.searchedMsisdn;
		$scope.subscriberX = null;
		if($scope.option == 1 || $scope.option == 2){
			$scope.searchSubscriberUrl += '/switch?location=' + $scope.option;
		}
			
	$http({method: $scope.method, url: $scope.searchSubscriberUrl}).
	then(function(response){
	$scope.status = response.status;
	$scope.subscriberX = response.data;
	if($scope.subscriberX.msisdn == undefined){
		$scope.found = false;
		$scope.deleted = true;
		 window.alert('Subscriber not found!');
	} else {
		$scope.found = true;
		$scope.deleted = false;
	}

	}, function(response) {
	$scope.subscriberX = response.data;
          $scope.status = response.status;

	});
	
	$scope.searchedMsisdn = '';
};
	
	$scope.delMethod = 'DELETE';
	$scope.removeSubscriber = function(index) {
		$scope.permission = false;
		for (var i = 0; i < jsonData.realm_access.roles.length; i++) {
    	    var role = jsonData.realm_access.roles[i];
    	    if(role == "admin"){
    	    	$scope.permission = true;
    	    }
    	}
		if($scope.permission == false){
			 window.alert('User not allowed for this action!');
			return;
		}
		var deletedSubscriber = $scope.subscribers[index];
		$http({method: $scope.delMethod, url: $scope.url + '/delete/' + deletedSubscriber['msisdn']}).
		then(function(response){
	          $scope.status = response.status;
			$scope.subscribers.splice(index,1);
		}, function(response){
	          $scope.status = response.status;
				$scope.subscribers.splice(index,1);

		});
	};
	
	$scope.delMethod = 'DELETE';
	$scope.removeSubscriberX = function(subscriberX) {
		$scope.permission = false;
		for (var i = 0; i < jsonData.realm_access.roles.length; i++) {
    	    var role = jsonData.realm_access.roles[i];
    	    if(role == "admin"){
    	    	$scope.permission = true;
    	    }
    	}
		if($scope.permission == false){
			 window.alert('User not allowed for this action!');
			return;
		}
		$http({method: $scope.delMethod, url: $scope.url + '/delete/' + $scope.subscriberX['msisdn']}).
		then(function(response){
	          $scope.status = response.status;
		}, function(response){
	          $scope.status = response.status;
				$scope.found = false;
				$scope.subscriberX = [];

		});
	};
	
	$scope.addSubscribers = function(){
		$scope.subscribers = [];
		$scope.subscriberX = null;

		$http({method: $scope.method, url: $scope.url + '/addSubscribers'}).
		then(function(response){
	          $scope.status = response.status;
	          $scope.subscribers = response.data;
	      	$scope.found = false;	
	      	window.alert(" Subscribers added!");

		}, function(response){
			$scope.subscribers = response.data || 'Request failed';
	          $scope.status = response.status;		
	    });
	}
	
	$scope.deleteAll = function(){
		$scope.permission = false;
		for (var i = 0; i < jsonData.realm_access.roles.length; i++) {
    	    var role = jsonData.realm_access.roles[i];
    	    if(role == "admin"){
    	    	$scope.permission = true;
    	    }
    	}
		if($scope.permission == false){
			 window.alert('User not allowed for this action!');
			return;
		}
		$scope.subscribers = [];
		$http({method: $scope.delMethod, url: $scope.url + '/deleteAll'}).
		then(function(response){
	          $scope.status = response.status;
	          $scope.found = false;
	          $scope.deleted = false;
		}, function(response){
	          $scope.status = response.status;
	          $scope.deleted = false;
	          $scope.found = false;
				$scope.subscriberX = [];

		});
	}

	$scope.deleteCache = function(){
		$scope.permission = false;
		for (var i = 0; i < jsonData.realm_access.roles.length; i++) {
    	    var role = jsonData.realm_access.roles[i];
    	    if(role == "admin"){
    	    	$scope.permission = true;
    	    }
    	}
		if($scope.permission == false){
			 window.alert('User not allowed for this action!');
			return;
		}
		$scope.subscribers = [];
		$http({method: $scope.delMethod, url: $scope.url + '/deleteCache'}).
		then(function(response){
	          $scope.status = response.status;
		}, function(response){
	          $scope.status = response.status;
	          $scope.deleted = false;
	          $scope.found = false;
				$scope.subscriberX = [];

		});
	}
}]);


app.controller('ContactController',['$scope', '$location',function($scope, $location){

$scope.sendMessage = function(){
  $location.path('/contact-success');
};
	
}]);


app.controller('UpdateController',['$scope','$http', '$templateCache','$window', function($scope, $http, $templateCache,$window) {

	$scope.regExMsisdn = "\A04\d{9}"
	$scope.worked = 'Data was update successfully'
		$scope.didntWork = 'Invalid content'
	$scope.form = true;
$scope.putMethod = 'PUT';
	$scope.url = 'http://localhost:9090/orangeproject/webapi/customers';
	$scope.updateSubscriber = function() {
		$scope.permission = false;
		for (var i = 0; i < jsonData.realm_access.roles.length; i++) {
    	    var role = jsonData.realm_access.roles[i];
    	    if(role == "admin"){
    	    	$scope.permission = true;
    	    }
    	}
		if($scope.permission == false){
			 window.alert('User not allowed for this action!');
			return;
		}
		
	$http({method: $scope.putMethod, url:$scope.url + '/' + $scope.updatedMsisdn 
		+ '/' + $scope.updatedField + '/' + $scope.updatedValue}).
		then(function(response){
	          $scope.status = response.status;
	          if($scope.status == 204){	       
	        	  window.alert($scope.didntWork)
	          	}else {
	          		window.alert($scope.worked)
	          	}
		}, function(response){
	          $scope.status = response.status;
		});
	};
	
	$scope.postMethod = 'POST';
	$scope.url = 'http://localhost:9090/orangeproject/webapi/customers';
	$scope.addSubscriber = function() {
		$scope.permission = false;
		for (var i = 0; i < jsonData.realm_access.roles.length; i++) {
    	    var role = jsonData.realm_access.roles[i];
    	    if(role == "admin"){
    	    	$scope.permission = true;
    	    }
    	}
		if($scope.permission == false){
			 window.alert('User not allowed for this action!');
			return;
		}
		
		var data = $scope.fields;
	$http({method: $scope.postMethod, url:$scope.url, data: data}).
		then(function(response){
	          $scope.status = response.status;
	          if($scope.status == 204){	       
	        	  window.alert($scope.didntWork)
	          	}
	          		}, function(response){
	          $scope.status = response.status;
		});
	};

}]);