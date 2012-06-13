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

            popup.addClass("jquery-popup");
            popup.css({
                'position': 'absolute',
                'z-index' : options.zIndex,
                'left'    : options.left,
                'top'     : options.top,
                'height'  : options.height
            });
            
            if (options.closeIcon) {
                if ($(".jquery-popup-close-icon").length == 0) {
                    var closeIcon = $("<span />").addClass("jquery-popup-close-icon")
                        .append($("<img src='" + options.closeIcon + "'>"))
                        .click(function() {
                            popup.hide();
                            $(document).off(click);
                        });
                    popup.append(closeIcon);
                }
            }

            if (options.minWidth) {
                popup.css({
                    'min-width': options.minWidth,
                });
            }

            if (options.maxHeight) {
                popup.css({
                    'max-height': options.maxHeight,
                });
            } else if (options.height) {
                popup.css({
                    'height': options.height,
                });
            }

            popup.show();

            $(document).on(click, function(e) {
                var clicked = e.target;
                var clickedPopup = $(clicked).closest(popup);
                if (!clickedPopup.length) {
                    popup.hide();
                    $(document).off(click);
                }
            });
        });
    };
})(jQuery);
