/**
 * Array editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */
var ArrayEditor = Class.create(BaseTextEditor, {

    separator: null,
    entryEditor: null,
    intOnly: false,

    editor_initialize: function(param) {
        this.createInput();

        var self = this;
        this.input.onkeypress = function(event) {return self.keyPressed(event || window.event);};

        if (param) {
            this.separator = param.separator;
            this.entryEditor = param.entryEditor;
            this.intOnly = param.intOnly;
        }
    },

    isValid: function(value) {
        var valid = true;
        var self = this;
        if (self.entryEditor && value) {
            var values = value.split(this.separator);
            values.each(function(v) {
                if (!self.entryEditor.isValid(v)) {
                    valid = false;
                }
            });
        }
        return valid;
    },

    keyPressed: function(event) {
        if (event.charCode === 0) return true;

        var code = event.charCode == undefined ? event.keyCode : event.charCode;
        if (code == 46) {
            // point
            return !this.intOnly || this.separator === '.';
        }

        return /^[,0-9]$/.test(String.fromCharCode(code));
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["array"] = ArrayEditor;
}
