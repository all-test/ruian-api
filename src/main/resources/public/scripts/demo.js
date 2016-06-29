var app = angular.module('angulartest', [ 'ngSanitize', 'ui.select']);


//cervene oramovani kdyz je chybka
app.directive('showErrors', function() {
    return {
        restrict: 'A',
        require:  '^form',
        link: function (scope, el, attrs, formCtrl) {
          // find the text box element, which has the 'name' attribute
          var inputEl   = el[0].querySelector("[name]");
          // convert the native text box element to an angular element
          var inputNgEl = angular.element(inputEl);
          // get the name on the text box so we know the property to check
          // on the form controller
          var inputName = inputNgEl.attr('name');

          // only apply the has-error class after the user leaves the text box
          inputNgEl.bind('blur', function() {
            el.toggleClass('has-error', formCtrl[inputName].$invalid);
          })
        }
      }
    });





app.controller('DemoCtrl', function($scope, $http) {

  $scope.address3 = {};
  $scope.refreshAddresses3 = function(address) {
    var params = {sobec: address};
    return $http.get(
      '/api/sobec',
      {params: params}
    ).then(function(response) {
      $scope.addresses3 = response.data
    });
  };


});



