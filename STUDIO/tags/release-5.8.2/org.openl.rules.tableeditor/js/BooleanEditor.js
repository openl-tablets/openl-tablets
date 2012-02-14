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
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["boolean"] = BooleanEditor;
}
