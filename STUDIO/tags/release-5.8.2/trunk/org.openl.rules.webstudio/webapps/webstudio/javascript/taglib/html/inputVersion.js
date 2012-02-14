//(function($) {
	var initVersion = "0.0.0";

    function showVersionPopup(input, event) {
        event.stopPropagation();
        input = $j(input);
        var inputPos = input.position();
        var popup = input.next();
        popup.popup({
            left: inputPos.left,
            top : inputPos.top + input.outerHeight()
        });
    }

    function applyVersion(element, i) {
        var value = $j(element).find("input").val();
        var popup = $j(element).closest("div");
        var input = popup.prev();
        var hidden = popup.next();
        var version = input.val() || initVersion;
        var subVersions = version.split(".");
        subVersions[i] = value;
        var result = subVersions.join(".");
        input.val(result);
        hidden.val(result);
    }
//})(jQuery);
//]]>
