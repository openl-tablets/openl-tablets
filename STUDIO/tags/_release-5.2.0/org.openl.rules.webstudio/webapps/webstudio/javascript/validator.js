
//prototype-1.5.1 is required
function validateRequired(elementId, showErrorMessage) {
    if (!elementId) { return false; }
    var value = $(elementId).value;
    if (showErrorMessage) {
        var messageId = elementId + '_Message_';
        var message = $(messageId);
        if (message) { message.remove(); }
    }
    if (!value) {
        if (showErrorMessage) {
            var errorMessage = " Value is required";
            new Insertion.After(elementId, '<span id="' + messageId + '" style="color:red"> ' + errorMessage + '</span>');
        }
        return false;
    }
    return true;
}