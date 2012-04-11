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
    },

    getInputValue: function(element) {
        if (element.type == "checkbox") {
            return element.checked;
        } else {
            return element.value;
        }
    },

    setInputValue: function(element, value) {
        if (element.type == "checkbox") {
            element.checked = value == "true" ? true : false;
        } else {
            element.value = value;
        }
    },

    unescapeHTML: function(html) {
        return html.replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&nbsp;/g,' ');
    },

    isRightClick: function(e) {
        if (e.which) { 
            return e.which == 3;
        } else if (e.button) {
            return e.button == 2;
        }
    }

}