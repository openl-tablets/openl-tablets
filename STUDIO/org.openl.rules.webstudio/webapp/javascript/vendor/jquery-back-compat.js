// Limit scope pollution from any deprecated API
(function($) {

    var matched, browser;

// Use of jQuery.browser is frowned upon.
// More details: http://api.jquery.com/jQuery.browser
// jQuery.uaMatch maintained for back-compat
    jQuery.uaMatch = function( ua ) {
        ua = ua.toLowerCase();

        var match = /(chrome)[ \/]([\w.]+)/.exec( ua ) ||
            /(webkit)[ \/]([\w.]+)/.exec( ua ) ||
            /(opera)(?:.*version|)[ \/]([\w.]+)/.exec( ua ) ||
            /(msie) ([\w.]+)/.exec( ua ) ||
            ua.indexOf("compatible") < 0 && /(mozilla)(?:.*? rv:([\w.]+)|)/.exec( ua ) ||
            [];

        return {
            browser: match[ 1 ] || "",
            version: match[ 2 ] || "0"
        };
    };

    matched = jQuery.uaMatch( navigator.userAgent );
    browser = {};

    if ( matched.browser ) {
        browser[ matched.browser ] = true;
        browser.version = matched.version;
    }

// Chrome is Webkit, but Webkit is also Safari.
    if ( browser.chrome ) {
        browser.webkit = true;
    } else if ( browser.webkit ) {
        browser.safari = true;
    }

    jQuery.browser = browser;
    jQuery.boxModel = jQuery.support.boxModel = (document.compatMode === "CSS1Compat");

    jQuery.sub = function() {
        function jQuerySub( selector, context ) {
            return new jQuerySub.fn.init( selector, context );
        }
        jQuery.extend( true, jQuerySub, this );
        jQuerySub.superclass = this;
        jQuerySub.fn = jQuerySub.prototype = this();
        jQuerySub.fn.constructor = jQuerySub;
        jQuerySub.sub = this.sub;
        jQuerySub.fn.init = function init( selector, context ) {
            if ( context && context instanceof jQuery && !(context instanceof jQuerySub) ) {
                context = jQuerySub( context );
            }

            return jQuery.fn.init.call( this, selector, context, rootjQuerySub );
        };
        jQuerySub.fn.init.prototype = jQuerySub.fn;
        var rootjQuerySub = jQuerySub(document);
        return jQuerySub;
    };

    // More details: https://api.jquery.com/size/
    jQuery.fn.size = function() {
        return this.length;
    };

    // More details: https://bugs.jquery.com/ticket/11921/
    jQuery.curCSS = function(element, prop, val) {
        return jQuery(element).css(prop, val);
    };

    // More details: https://api.jquery.com/selector/
    var originaljQuery = $.fn.init; // Store the original jQuery function
    // Create a new init function
    $.fn.init = function(selector, context, root) {
        var result = new originaljQuery(selector, context, root);
        if (typeof selector === "string") { // Only add selector property for string selectors
            result.selector = selector;
        } else {
            result.selector = "";
        }
        return result;
    };
    // Ensure that the modified init function carries over the original jQuery prototype
    $.fn.init.prototype = $.fn;

    // original position function does not work properly, so replace it with this one
    // it's taken from jQuery 1.7.2
    var rroot = /^(?:body|html)$/i;
    $.fn.position = function () {
        if ( !this[0] ) {
            return null;
        }

        var elem = this[0],
            // Get *real* offsetParent
            offsetParent = this.offsetParent(),

            // Get correct offsets
            offset       = this.offset(),
            parentOffset = rroot.test(offsetParent[0].nodeName) ? { top: 0, left: 0 } : offsetParent.offset();

        // Subtract element margins
        // note: when an element has margin: auto the offsetLeft and marginLeft
        // are the same in Safari causing offset.left to incorrectly be 0
        offset.top  -= parseFloat( jQuery.css(elem, "marginTop") ) || 0;
        offset.left -= parseFloat( jQuery.css(elem, "marginLeft") ) || 0;

        // Add offsetParent borders
        parentOffset.top  += parseFloat( jQuery.css(offsetParent[0], "borderTopWidth") ) || 0;
        parentOffset.left += parseFloat( jQuery.css(offsetParent[0], "borderLeftWidth") ) || 0;

        // Subtract the two offsets
        return {
            top:  offset.top  - parentOffset.top,
            left: offset.left - parentOffset.left
        };
    };

})(jQuery);
