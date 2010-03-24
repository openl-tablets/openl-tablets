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

function modalInputKeyPress(event, elm) {
  if (Prototype.Browser.IE) {
    event = event || window.event;
    if (event.keyCode == 13) {
      var submitBtn = document.getElementById(elm.form.name + ":sbt")
      if (submitBtn)
      document.getElementById(elm.form.name + ":sbt").click();
      return false;
    }
  }
  return true;
}

function getRadioChecked(formId, radioGroupId) {
    var radioGrp = document['forms'][formId][radioGroupId];
    for(i = 0; i < radioGrp.length; i++) {
        if (radioGrp[i].checked == true) {
            return radioGrp[i];
        }
    }
    return '';
}

function getRadioCheckedValue(formId, radioGroupId) {
    var radioChecked = getRadioChecked(formId, radioGroupId);
    if (radioChecked) {
        return radioChecked.value;
    }
    return '';
}
