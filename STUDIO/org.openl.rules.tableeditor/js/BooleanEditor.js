/**
 * Boolean editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */
var BooleanEditor = Class.create(BaseEditor, {

    editor_initialize: function() {
        this.input = new Element("input");
        this.input.setAttribute("type", "checkbox");
    },

    getValue: function() {
        return this.input.checked;
    },

    setValue: function(value) {
        var checked = false;
        if (value != null) {
            var trueValues = ["true", "on", "yes", "t", "y"];
            for (var i = 0; i < trueValues.length; ++i) {
                if (trueValues[i] == value.toLowerCase()) {
                    checked = true;
                    break;
                }
            }
        }
        this.input.checked = checked;
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["boolean"] = BooleanEditor;
}
