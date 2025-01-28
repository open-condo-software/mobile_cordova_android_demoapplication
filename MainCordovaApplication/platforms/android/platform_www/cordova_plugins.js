cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "cordova-plugin-ble-peripheral.blePeripheral",
      "file": "plugins/cordova-plugin-ble-peripheral/www/blePeripheral.js",
      "pluginId": "cordova-plugin-ble-peripheral",
      "clobbers": [
        "blePeripheral"
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
    "cordova-plugin-ble-peripheral": "1.1.2.1",
    "cordova-plugin-condo": "0.0.1"
  };
});