/**
 * Formula editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */
var FormulaEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.createInput();

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["formula"] = FormulaEditor;
}
