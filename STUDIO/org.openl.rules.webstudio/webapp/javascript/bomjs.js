/**
 * @author Andrei Ostrovski
 */

(function(window) {

    var location = window.location;

    var $location = {

        set: function(href) {
            location.href = href;
        },

        getHash: function(decoded) {
            return this.utils.getHash(location.href, decoded);
        },

        setHash: function(hash) {
            location.hash = hash;
        },

        utils: {

            getHash: function(location, decoded) {
                var hash = location.split("#")[1] || "";
                if (hash && decoded === true) {
                    hash = this.decode(hash);
                }
                return hash;
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

            decode: function(location) {
                return decodeURIComponent(location);
            }

        }

    };

    window.$location = $location;

})(window);