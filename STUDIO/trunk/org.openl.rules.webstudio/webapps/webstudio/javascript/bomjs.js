/**
 * @author Andrei Ostrovski
 */

(function(window) {

    var location = window.location;

    var $location = {

        get: function() {
            return location.href;
        },

        set: function(href) {
            location.href = href;
        },
/*
        refresh: function() {
        },
*/
        getHash: function(decoded) {
            return this.utils.getHash(location.href, decoded);
        },

        setHash: function(hash) {
            location.hash = hash;
        },

        getParamsQuery: function() {
            return this.utils.getParamsQuery(location.search);
        },
/*
        getParams: function(decoded) {
        },

        getParam: function(name, decoded) {
        },

        getDomain: function() {
        },

        getPort: function() {
        },

        getProtocol: function() {
        }
*/
        utils: {

            getHash: function(location, decoded) {
                var hash = location.split("#")[1] || "";
                if (hash && decoded === true) {
                    hash = this.decode(hash);
                }
                return hash;
            },

            getParamsQuery: function(location) {
                return location.split("?")[1] || "";
            },
/*
            toQuery: function(params, noEmpty) {
            },
*/
            decode: function(location) {
                return decodeURIComponent(location);
            },

            encode: function(location) {
                return encodeURIComponent(location);
            }

        }

    };

    window.$location = $location;

})(window);