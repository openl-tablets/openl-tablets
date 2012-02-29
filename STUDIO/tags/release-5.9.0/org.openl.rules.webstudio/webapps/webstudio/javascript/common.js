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
