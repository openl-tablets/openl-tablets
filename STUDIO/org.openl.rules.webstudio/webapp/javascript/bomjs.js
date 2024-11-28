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

            setParams: function(location, params) {
                var result;

                for (var param in params) {
                    var regex = new RegExp("[\\?&]" + param + "=([^&#]*)");
                    var paramObj = regex.exec(location);
                    result = result || location;
                    if (paramObj) {
                        result = result.replace(paramObj[0].substring(1), param + "=" + params[param]);
                    } else {
                        var paramPrefix = location.indexOf("?") > 0 ? "&" : "?";
                        result += (paramPrefix + param + "=" + params[param]);
                    }
                }

                return result || location;
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