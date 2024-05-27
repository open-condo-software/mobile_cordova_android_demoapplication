cordova.define('cordova/plugin_list', function(require, exports, module) {
  module.exports = [
    {
      "id": "cordova-plugin-condo.Condo",
      "file": "plugins/cordova-plugin-condo/www/condo.js",
      "pluginId": "cordova-plugin-condo",
      "clobbers": [
        "cordova.plugins.condo"
      ]
    },
    {
      "id": "nl.madebymark.share.Share",
      "file": "plugins/nl.madebymark.share/www/share.js",
      "pluginId": "nl.madebymark.share",
      "clobbers": [
        "window.navigator.share"
      ]
    }
  ];
  module.exports.metadata = {
    "cordova-plugin-condo": "0.0.1",
    "cordova-plugin-whitelist": "1.3.5",
    "nl.madebymark.share": "0.1.1"
  };
});