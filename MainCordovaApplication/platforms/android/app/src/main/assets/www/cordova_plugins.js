cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "cordova-plugin-device.device",
      "file": "plugins/cordova-plugin-device/www/device.js",
      "pluginId": "cordova-plugin-device",
      "clobbers": [
        "device"
      ]
    },
    {
      "id": "com.unarin.cordova.beacon.underscorejs",
      "file": "plugins/com.unarin.cordova.beacon/www/lib/underscore-min-1.6.js",
      "pluginId": "com.unarin.cordova.beacon",
      "runs": true
    },
    {
      "id": "com.unarin.cordova.beacon.Q",
      "file": "plugins/com.unarin.cordova.beacon/www/lib/q.min.js",
      "pluginId": "com.unarin.cordova.beacon",
      "runs": true
    },
    {
      "id": "com.unarin.cordova.beacon.LocationManager",
      "file": "plugins/com.unarin.cordova.beacon/www/LocationManager.js",
      "pluginId": "com.unarin.cordova.beacon",
      "merges": [
        "cordova.plugins"
      ]
    },
    {
      "id": "com.unarin.cordova.beacon.Delegate",
      "file": "plugins/com.unarin.cordova.beacon/www/Delegate.js",
      "pluginId": "com.unarin.cordova.beacon",
      "runs": true
    },
    {
      "id": "com.unarin.cordova.beacon.Region",
      "file": "plugins/com.unarin.cordova.beacon/www/model/Region.js",
      "pluginId": "com.unarin.cordova.beacon",
      "runs": true
    },
    {
      "id": "com.unarin.cordova.beacon.Regions",
      "file": "plugins/com.unarin.cordova.beacon/www/Regions.js",
      "pluginId": "com.unarin.cordova.beacon",
      "runs": true
    },
    {
      "id": "com.unarin.cordova.beacon.CircularRegion",
      "file": "plugins/com.unarin.cordova.beacon/www/model/CircularRegion.js",
      "pluginId": "com.unarin.cordova.beacon",
      "runs": true
    },
    {
      "id": "com.unarin.cordova.beacon.BeaconRegion",
      "file": "plugins/com.unarin.cordova.beacon/www/model/BeaconRegion.js",
      "pluginId": "com.unarin.cordova.beacon",
      "runs": true
    },
    {
      "id": "cordova-plugin-ble-peripheral.blePeripheral",
      "file": "plugins/cordova-plugin-ble-peripheral/www/blePeripheral.js",
      "pluginId": "cordova-plugin-ble-peripheral",
      "clobbers": [
        "blePeripheral"
      ]
    },
    {
      "id": "cordova-plugin-camera.Camera",
      "file": "plugins/cordova-plugin-camera/www/CameraConstants.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "Camera"
      ]
    },
    {
      "id": "cordova-plugin-camera.CameraPopoverOptions",
      "file": "plugins/cordova-plugin-camera/www/CameraPopoverOptions.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "CameraPopoverOptions"
      ]
    },
    {
      "id": "cordova-plugin-camera.camera",
      "file": "plugins/cordova-plugin-camera/www/Camera.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "navigator.camera"
      ]
    },
    {
      "id": "cordova-plugin-camera.CameraPopoverHandle",
      "file": "plugins/cordova-plugin-camera/www/CameraPopoverHandle.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "CameraPopoverHandle"
      ]
    },
    {
      "id": "cordova-plugin-camera.CameraPopoverHandle",
      "file": "plugins/cordova-plugin-camera/www/CameraPopoverHandle.js",
      "pluginId": "cordova-plugin-camera",
      "clobbers": [
        "CameraPopoverHandle"
      ]
    },
    {
      "id": "cordova-plugin-condo.Condo",
      "file": "plugins/cordova-plugin-condo/www/condo.js",
      "pluginId": "cordova-plugin-condo",
      "clobbers": [
        "cordova.plugins.condo"
      ]
    }
  ];
  module.exports.metadata = {
    "cordova-plugin-device": "3.0.0",
    "com.unarin.cordova.beacon": "3.8.1",
    "cordova-plugin-ble-peripheral": "1.1.2.1",
    "cordova-plugin-camera": "2.4.1",
    "cordova-plugin-condo": "0.0.1"
  };
});