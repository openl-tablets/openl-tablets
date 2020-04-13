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
    if (!RichFaces.ui.PopupPanel) {
        return;
    }
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

function updateSubmitListener(listener) {
    if (!$j) {
        return;
    }
    $j("form input[type='submit']").each(function () {
        var $submit = $j(this);
        // Add showLoader listener only on submit buttons without onclick handler to skip
        // ajax jsf submit buttons and buttons with existed logic in onclick attribute.
        if (!$submit.attr("onclick")) {
            // Because this function is invoked on DOM change we must remove the handler we've set before.
            $submit.off("click", listener);
            $submit.on("click", listener);
        }
    });
}

function showAnimatedPanel(loadingPanel) {
    loadingPanel.show();

    // EPBDS-6231 Workaround for IE.
    // IE freezes animation when the form is submitted or url is changed. The trick below with replacing of html
    // makes IE think that the gif is a new img element and IE animates it.
    // Html must be replaced after form is submitted - that's why timeout is used.
    setTimeout(function() {loadingPanel.html(loadingPanel.html());}, 1);
}

/**
 * Fix the bug related to not updating input when enter too big number and then lose the focus.
 *
 * @param id the id of inputNumberSpinner element
 */
function fixInputNumberSpinner(id) {
    var component = RichFaces.$(id);
    if (!component) {
        return;
    }

    component.__setValue = function (value, event, skipOnchange) {
        if (!isNaN(value)) {
            if (value > component.maxValue) {
                value = component.maxValue;
            } else if (value < component.minValue) {
                value = component.minValue;
            }
            // !!! The line below was changed. See inputNumberSpinner.js for comparison.
            if (Number(value) !== Number(component.input.val()) || event && event.type === 'change') {
                component.input.val(value);
                component.value = value;
                if (component.onchange && !skipOnchange) {
                    component.onchange.call(component.element[0], event);
                }
            }
        }
    };
}

function initExpandableLinks() {
    if (!$j) {
        return;
    }

    $j('.expandable').off().click(function () {
        $j(this).next().show();
        $j(this).hide();
    })
}