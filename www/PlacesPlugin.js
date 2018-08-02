var exec = require("cordova/exec");
var PLUGIN_NAME = "GooglePlaces";

module.exports = {
    getPredictions: function(query, options) {
        return new Promise(function(resolve, reject) {
            if (options.types == "geocode") {
                options.types = 1;
            } else if (options.types == "address") {
                options.types = 2;
            } else if (options.types == "establishment") {
                options.types = 3;
            } else if (options.types == "(regions)") {
                options.types = 4;
            } else if (options.types == "(cities)") {
                options.types = 5;
            }

            exec(resolve, reject, PLUGIN_NAME, "getPredictions", [query, options]);
        });
    },
    getById: function(placeId) {
        return new Promise(function(resolve, reject) {
            exec(resolve, reject, PLUGIN_NAME, "getById", [placeId]);
        });
    }
};
