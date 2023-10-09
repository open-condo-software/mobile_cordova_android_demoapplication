cordova.define("cordova-plugin-condo.Condo", function (require, exports, module) {
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

    exports.getLaunchContext = function (success, error) {
        exec(success, error, 'Condo', 'getLaunchContext', []);
    };

    exports.setInputsEnabled = function (arg0, success, error) {
        exec(success, error, 'Condo', 'setInputsEnabled', [arg0]);
    };

    exports.history = {};

    exports.history.back = function (success, error) {
        exec(success, error, 'Condo', 'historyBack', []);
    };

    exports.history.pushState = function (state, title, success, error) {
        exec(success, error, 'Condo', 'historyPushState', [state, title]);
    };

    exports.history.replaceState = function (state, title, success, error) {
        exec(success, error, 'Condo', 'historyReplaceState', [state, title]);
    };

    exports.history.go = function (amount, success, error) {
        exec(success, error, 'Condo', 'historyGo', [amount]);
    };

    exports.hostApplication = {};

    exports.hostApplication.isDemoEnvironment = function () {
        return jsInterface.condoHostApplicationIsDemo() || false;
    }

    exports.hostApplication.baseURL = function () {
        return jsInterface.condoHostApplicationBaseURL() || 'https://v1.doma.ai';
    }

    exports.hostApplication.installationID = function () {
        return jsInterface.condoHostApplicationInstallationID() || '';
    }

    exports.hostApplication.deviceID = function () {
        return jsInterface.condoHostApplicationDeviceID() || '';
    }

    exports.hostApplication.locale = function () {
        return jsInterface.condoHostApplicationLocale() || 'ru-RU';
    }

});