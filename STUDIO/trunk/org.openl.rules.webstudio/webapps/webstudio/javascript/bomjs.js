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

        getParam: function(name, decoded) {
            return this.utils.getParam(location.search, name, decoded);
        },

/*
        getParams: function(decoded) {
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

            getParam: function(location, name, decoded) {
                var regex = new RegExp("[\\?&]" + name + "=([^&#]*)");
                var params = regex.exec(location);
                var value = (params && params[1].replace(/\+/g, " ")) || "";

                if (value && decoded === true) {
                    value = this.decode(value);
                }

                return value;
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