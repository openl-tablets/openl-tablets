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
