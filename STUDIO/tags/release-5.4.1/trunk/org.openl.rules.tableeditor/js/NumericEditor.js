/**
 * Numeric editor.
 * Extends base text editor to restrict input values to numeric values only. Supports min/max constraints.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */
var NumericEditor = Class.create(BaseTextEditor, {
    min: null,
    max: null,
    editor_initialize: function(param) {
        this.createInput();

        var self = this;
 
        this.input.onkeypress = function(event) {return self.keyPressed(event || window.event)}
        this.input.onkeyup = function(event) {self.keyPressed(event || window.event)}

        if (param) {
            this.min = param.min; this.max = param.max
        }
    },

    isCancelled : function() {
        return (this.initialValue == this.getValue() || this.isInvalid(this.getValue()));
    },

    isInvalid: function(v) {
        var n = Number(v);
        return isNaN(n) || (this.min && n < this.min) || (this.max && n > this.max)
    },

    keyPressed: function(event) {
        var v = this.input.getValue();
        if (event.type == "keypress") {
            if (event.charCode == 0) return true;
            var code = event.charCode == undefined ? event.keyCode : event.charCode;

            if (code == 46) return v.indexOf(".") < 0;
            if (code == 45) return true;
            return code >= 48 && code <= 57
        }
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["numeric"] = NumericEditor;
}
