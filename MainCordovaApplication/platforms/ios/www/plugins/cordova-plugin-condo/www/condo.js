cordova.define("cordova-plugin-condo.Condo", function(require, exports, module) {
var exec = require('cordova/exec');

exports.requestAuthorization = function (arg0, arg1, success, error) {
    exec(success, error, 'Condo', 'requestAuthorization', [arg0, arg1]);
};

exports.requestServerAuthorizationByUrl = function (arg0, arg1, success, error) {
    exec(success, error, 'Condo', 'requestServerAuthorizationByUrl', [arg0, arg1]);
};

exports.openURLWithFallback = function (arg0, arg1, success, error) {
    exec(success, error, 'Condo', 'openURLWithFallback', [arg0, arg1]);
};

exports.closeApplication = function (success, error) {
    exec(success, error, 'Condo', 'closeApplication', []);
};

exports.getCurrentResident = function (success, error) {
    exec(success, error, 'Condo', 'getCurrentResident', []);
};

exports.setInputsEnabled = function (arg0, success, error) {
    exec(success, error, 'Condo', 'setInputsEnabled', [arg0]);
};
});
