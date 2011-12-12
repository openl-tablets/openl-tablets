/**
 * Multiselect component.
 * 
 * @requires jQuery v1.7.1+
 * @author Andrei Astrouski
 */
(function($) {
    $.fn.multiselect = function(options) {
        var defaults = {
            separator       : ',',
            separatorEscaper: '\\',
            zIndex          : 9000
        };
        options = $.extend({}, defaults, options);

        return this.each(function() {
            var values = {};

            var currentSelect = $(this).hide();
            var selectName = currentSelect.attr("name");
            var newSelect = $("<input readonly='readonly' />").insertAfter(currentSelect);

            var popup = $("<div style='display:none; border-top: 0' />");
            var checkAll = $("<input type='checkbox' />");
            popup.append("<div />").append(checkAll).append("Check All");
            currentSelect.children("option").each(function() {
                var option = $(this);
                var selected = this.getAttribute("selected") ? true : false;
                values[option.val()] = selected;
                popup.append("<div><input type='checkbox' value='" + option.val() + "'"
                        + (selected ? " checked='checked'" : "")
                        + (selectName ? " name='" + selectName + "'" : "")
                        +" />" + option.text() + "</div>");
            });
            popup.insertAfter(newSelect);

            setValue();

            newSelect.click(function(e) {
                e.stopPropagation();
                popup.popup({
                    left    : newSelect.position().left,
                    top     : newSelect.position().top + newSelect.outerHeight(),
                    zIndex  : options.zIndex,
                    minWidth: newSelect.outerWidth() - 2
                });
            });

            checkAll.click(function(e) {
                var checked = this.checked;
                popup.find(":checkbox:not(:first)").each(function() {
                    this.checked = checked;
                    values[this.value] = checked;
                });
                setValue();
            });

            popup.find(":checkbox:not(:first)").click(function(e) {
                values[this.value] = this.checked;
                setValue();
            });

            function setValue() {
                var result = [];
                for (v in values) {
                    if (values[v]) {
                        result.push(v);
                    }
                }
                newSelect.val(result.join(options.separator));
            }

        });
    };
})(jQuery);
