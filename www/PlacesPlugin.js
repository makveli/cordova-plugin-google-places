var exec = require("cordova/exec");
var PLUGIN_NAME = "GooglePlaces";

module.exports = {
    getPredictions: function(query, options) {
        return new Promise(function(resolve, reject) {
            exec(resolve, reject, PLUGIN_NAME, "getPredictions", [query, options]);
        });
    },
    getById: function(placeId) {
        return new Promise(function(resolve, reject) {
            exec(resolve, reject, PLUGIN_NAME, "getById", [placeId]);
        });
    }
};
