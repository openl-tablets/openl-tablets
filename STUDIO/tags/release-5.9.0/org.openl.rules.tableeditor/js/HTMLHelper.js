/**
 * HTML Helper.
 *
 * @author Andrei Astrouski
 */
var HTMLHelper = {

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