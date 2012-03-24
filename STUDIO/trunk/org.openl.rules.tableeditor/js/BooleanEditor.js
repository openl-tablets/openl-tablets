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
        this.input.checked = value == "true" ? true : false;
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["boolean"] = BooleanEditor;
}
