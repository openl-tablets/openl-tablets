/**
 * Popup component.
 * 
 * @requires jQuery v1.7.1+
 * @author Andrei Astrouski
 */
(function($) {
    $.fn.popup = function(options) {
        var defaults = {
            left     : 0,
            top      : 0,
            zIndex   : 9000,
            closeIcon: false
        };
        options = $.extend({}, defaults, options);

        return this.each(function() {
            var popup = $(this);

            // Unique click event
            var click = "click.jquery.popup." + Date.now();

            function hide() {
                popup.hide();
                $(document).off(click);
            }

            popup.addClass("jquery-popup");

            if (options.closeIcon && $(".jquery-popup-close-icon").length == 0) {
                var closeIcon = $("<span />").addClass("jquery-popup-close-icon")
                    .append($("<img src='" + options.closeIcon + "'>"))
                    .click(hide);
                popup.append(closeIcon);
            }

            // Position
            popup.css({
                'position': 'absolute',
                'z-index' : options.zIndex,
                'left'    : options.left,
                'top'     : options.top
            });

            // Width
            options.minWidth && popup.css('min-width', options.minWidth);
            options.width    && popup.css('width', options.width);
            if (options.maxWidth) {
                if (options.maxWidth.toString().indexOf("calc") > -1) {
                    popup[0].style.maxWidth = "-moz-" + options.maxWidth;
                    popup[0].style.maxWidth = "-webkit-" + options.maxWidth;
                    popup[0].style.maxWidth = options.maxWidth;
                } else {
                    popup.css('max-width', options.maxWidth);
                }
            }

            // Height
            options.minHeight && popup.css('min-height', options.minHeight);
            options.height    && popup.css('height', options.height);
            if (options.maxHeight) {
                if (options.maxHeight.toString().indexOf("calc") > -1) {
                    popup[0].style.maxHeight = "-moz-" + options.maxHeight;
                    popup[0].style.maxHeight = "-webkit-" + options.maxHeight;
                    popup[0].style.maxHeight = options.maxHeight;
                } else {
                    popup.css({
                        'max-height': options.maxHeight
                    });
                }
            }

            popup.show();

            $(document).on(click, function(e) {
                var clicked = e.target;
                var clickedPopup = $(clicked).closest(popup);
                if (!clickedPopup.length && clicked !== options.caller) {
                    hide();
                }
            });
        });
    };
})(jQuery);
