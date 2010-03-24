/**
 * Ajax helper.
 *
 * @author Andrei Astrouski
 */
var AjaxHelper = {
    /**
     * Handles response error.
     */
    handleError: function(response, errorMessage) {
        if (response.status == 399) { // redirect
            var redirectPage = response.getResponseHeader("Location");
            if (redirectPage) {
                top.location.href = redirectPage;
            } else {
                alert(response.statusText);
            }
        } else {
            if (!errorMessage) {
                errorMessage = "Error: " + response.status + " - " + response.statusText;
            }
            alert(errorMessage);
        }
    }

}