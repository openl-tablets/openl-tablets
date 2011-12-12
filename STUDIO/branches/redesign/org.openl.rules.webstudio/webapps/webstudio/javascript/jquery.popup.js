/**
 * Popup component.
 * 
 * @requires jQuery v1.7.1+
 * @author Andrei Astrouski
 */
(function($) {
    $.fn.popup = function(options) {
        var defaults = {
            left  : 0,
            top   : 0,
            zIndex: 9000
        };
        options = $.extend({}, defaults, options);

        return this.each(function() {
            var popup = $(this);

            popup.addClass("jquery-popup");
            popup.css({
                'position': 'absolute',
                'z-index' : options.zIndex,
                'left'    : options.left,
                'top'     : options.top
            });
            if (options.minWidth) {
                popup.css({
                    'min-width': options.minWidth,
                });
            }
            popup.show();

            $(document).on("click.jquery.popup", function(e) {
                var clicked = e.target;
                var clickedPopup = $(clicked).closest(popup);
                if (!clickedPopup.length) {
                    popup.hide();
                    $(document).off("click.jquery.popup");
                }
            });
        });
    };
})(jQuery);
