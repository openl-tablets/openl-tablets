/**
 * Common useful functions.
 *
 * @author Andrey Naumenko
 */

function focusElement(elementId) {
  var field = document.getElementById(elementId);
  if (field) {
    field.focus();
  }
}

function changeAllItemStatus(element, areaId) {
    $j("#"+areaId+" INPUT[type='checkbox']:not(:disabled)").prop("checked", element.checked);
}

function changeItemStatus(element, areaId, selectAllElemId) {
    if(!element.checked && $(selectAllElemId).checked) {
        $(selectAllElemId).checked = false;
    }

    checkedCount = $j("#"+areaId+" INPUT[type='checkbox']:checked").not("INPUT[id='"+selectAllElemId+"']").size();
    disabledCount = $j("#"+areaId+" INPUT[type='checkbox']:disabled").not("INPUT[id='"+selectAllElemId+"']").size();
    allCount = $j("#"+areaId+" INPUT[type='checkbox']").not("INPUT[id='"+selectAllElemId+"']").size();

    if (checkedCount + disabledCount == allCount && checkedCount > 0) {
        $(selectAllElemId).checked = true;
    }
}

function message(content, life, closable, styleClass) {
    var messages = $j(".message");

    function remove() {
        message.remove();

        var top = 33;
        $j(".message").each(function() {
            $j(this).css({"top" : top + "px"});
            top += ($j(this).outerHeight() + 5);
        });
    }

    var message = $j("<div />").addClass("message").html(content);

    if (closable !== false) {
        message.addClass("closable").click(remove);
    }
    if (styleClass) {
        message.addClass(styleClass);
    }

    var top;
    if (messages.length) {
        var lastMessage = $j(messages[messages.length - 1]);
        top = lastMessage.position().top + lastMessage.outerHeight() + 5 + "px";
    } else {
        top = "33px";
    }

    message.css({"top": top});
    $j("body").append(message);

    if (life > -1) {
        setTimeout(remove, life);
    }
}

/**
 * EPBDS-4825 rich:popupPanel has an issue: https://issues.jboss.org/browse/RF-10980
 * This function fixes rich:popupPanel's processTabindexes() function to correctly handle TABs in dialog boxes
 */
function fixTabIndexesInRichPopupPanels() {
    RichFaces.ui.PopupPanel.prototype.processTabindexes = function (input) {
        if (!this.firstOutside) {
            this.firstOutside = input;
        }
        if (!input.prevTabIndex) {
            input.prevTabIndex = input.tabIndex;
            // input.tabIndex = -1; // This line was original implementation. It was replaced with the lines below:
            if ($j(input).closest('.rf-pp-cntr').length === 0) {
                // Replace tab indexes with -1 only for inputs outside of popup panel.
                // Tab indexes inside popup panel are not touched.
                input.tabIndex = -1;
            }
        }
        if (!input.prevAccessKey) {
            input.prevAccessKey = input.accessKey;
            input.accessKey = "";
        }
    };
}
