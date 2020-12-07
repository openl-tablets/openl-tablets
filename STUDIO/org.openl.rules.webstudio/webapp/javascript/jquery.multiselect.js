/**
 * Multiselect component.
 * 
 * @requires jQuery v1.7.1+
 * @author Andrei Astrouski
 */
(function($) {
    $.fn.multiselect = function(options) {
        var defaults = {
            separator   : ', ',
            zIndex      : 9000,
            maxHeight   : 170,
            checkAllText: 'Select All'
        };
        options = $.extend({}, defaults, options);

        return this.each(function() {
            var values = {};

            var currentSelect = $(this);
            var selectId = currentSelect.attr("id");
            var selectName = currentSelect.attr("name");
            var newSelect = $("<input type='text'" + (selectId ? " id='" + selectId + "'" : "") + " readonly='readonly' />")
                .insertAfter(currentSelect);

            var popup = $("<div style='display:none' />").addClass("jquery-multiselect-popup");

            var checkAll = $("<label><input type='checkbox' />" + options.checkAllText + "</label>");
            popup.append($("<div class='jquery-multiselect-popup-header' />").append(checkAll));

            var data = $("<div class='jquery-multiselect-popup-data' />");

            currentSelect.find("option").sort(function(left, right) {
                return left.text == right.text ? 0 : left.text < right.text ? -1 : 1;
            }).each(function() {
                var option = $(this);
                var selected = this.getAttribute("selected") ? true : false;
                values[option.val()] = selected;
                data.append("<label><input type='checkbox' value='" + option.val() + "'"
                        + (selected ? " checked='checked'" : "")
                        + (selectName ? " name='" + selectName + "'" : "")
                        +" />" + option.text() + "</label>");
            });
            popup.append(data);
            popup.insertAfter(newSelect);
            currentSelect.remove();

            setValue();

            newSelect.click(function(e) {
                popup.popup({
                    caller   : this,
                    left     : newSelect.position().left + newSelect.offsetParent().scrollLeft(),
                    top      : newSelect.position().top + newSelect.offsetParent().scrollTop() + newSelect.outerHeight() - 1,
                    zIndex   : options.zIndex,
                    minWidth : newSelect.outerWidth() - 2,
                    maxHeight: options.maxHeight
                });
            });

            checkAll.click(function(e) {
                var checked = $(this).find("input").prop("checked");
                popup.find(":checkbox:not(:first)").each(function() {
                    this.checked = checked;
                    values[this.value] = checked;
                });
                setValue();
            });

            popup.find(":checkbox:not(:first)").click(function(e) {
                values[this.value] = this.checked;
                setValue();
                
                changeSelectAllStatus();
            });

            function setValue() {
                var result = [];
                for (v in values) {
                    if (values[v]) {
                        result.push(v);
                    }
                }
                newSelect.val(result.join(options.separator)).change();
            }
            
            function changeSelectAllStatus() {
                allCount = popup.find(":checkbox:not(:first)").size();
                checkedCount = popup.find(":checkbox[checked]:not(:first)").size();
                
                if (checkedCount == allCount) {
                    popup.find(":checkbox:first").prop("checked",true);
                } else {
                    popup.find(":checkbox:first").prop("checked",false);
                }
            }
            
            changeSelectAllStatus();

        });
    };
})(jQuery);
