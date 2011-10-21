
function validateRequired(elementId, showErrorMessage) {
    if (!elementId) { return false; }
    var value = $j("#" + elementId).val();
    if (showErrorMessage) {
        var messageId = elementId + '_Message_';
        var message = $j("#" + messageId);
        if (message) {
            message.remove();
        }
    }
    if (!value) {
        if (showErrorMessage) {
            var errorMessage = " Value is required";
            $j('<span id="' + messageId + '" style="color:red"> ' + errorMessage + '</span>').insertAfter("#" + elementId);
        }
        return false;
    }
    return true;
}
