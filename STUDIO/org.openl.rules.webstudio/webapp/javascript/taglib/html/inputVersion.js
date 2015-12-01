//(function($) {
	var initVersion = "0.0.0";
	var initShortVersion = "0.0";

    function showVersionPopup(input, event) {
        var jinput = $j(input);

        var left = jinput.position().left + jinput.offsetParent().scrollLeft();
        var top  = jinput.position().top + jinput.offsetParent().scrollTop() + jinput.outerHeight();

        var popup = jinput.next();
        popup.popup({
            caller: input,
            left  : left,
            top   : top
        });
    }

    function applyVersion(element, isShort,  i) {
        var value = $j(element).find("input").val();
        var popup = $j(element).closest("div");
        var input = popup.prev();
        var hidden = popup.next();
        var version = input.val();
        if (isShort){
        	version = version || initVersion;
        }else{
        	version = version || initShortVersion;
        }
        var subVersions = version.split(".");
        subVersions[i] = value;
        var result = subVersions.join(".");
        input.val(result);
        hidden.val(result);
    }
//})(jQuery);
//]]>
