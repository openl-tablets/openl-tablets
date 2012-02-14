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

    editor_initialize: function(param) {
        this.createInput();

        if (param) {
            this.separator = param.separator;
            this.entryEditor = param.entryEditor;
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
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["array"] = ArrayEditor;
}
