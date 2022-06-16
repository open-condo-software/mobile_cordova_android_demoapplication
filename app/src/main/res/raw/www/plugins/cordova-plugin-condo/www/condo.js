cordova.define("cordova-plugin-condo.Condo", function(require, exports, module) {
var exec = require('cordova/exec');

exports.requestAuthorizationCode = function (arg0, success, error) {
    exec(success, error, 'Condo', 'requestAuthorizationCode', [arg0]);
};

exports.requestAuthorization = function (arg0, arg1, success, error) {
    exec(success, error, 'Condo', 'requestAuthorization', [arg0, arg1]);
};

exports.requestServerAuthorization = function (arg0, arg1, arg2, success, error) {
    exec(success, error, 'Condo', 'requestServerAuthorization', [arg0, arg1, arg2]);
};

exports.closeApplication = function (success, error) {
    exec(success, error, 'Condo', 'closeApplication', []);
};
});
