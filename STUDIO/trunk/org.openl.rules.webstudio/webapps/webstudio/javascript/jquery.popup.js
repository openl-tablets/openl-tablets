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
            
            if (options.closeIcon) {
                if ($(".jquery-popup-close-icon").length == 0) {
                    var closeIcon = $("<span />").addClass("jquery-popup-close-icon")
                        .append($("<img src='" + options.closeIcon + "'>"))
                        .click(hide);
                    popup.append(closeIcon);
                }
            }

            // Position
            popup.css({
                'position': 'absolute',
                'z-index' : options.zIndex,
                'left'    : options.left,
                'top'     : options.top
            });

            // Width
            if (options.minWidth) {
                popup.css({
                    'min-width': options.minWidth
                });
            }

            // Height
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
            } else if (options.height) {
                popup.css({
                    'height': options.height
                });
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
