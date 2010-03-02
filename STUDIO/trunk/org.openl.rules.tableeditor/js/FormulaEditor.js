/**
 * Formula editor.
 *
 * @author Andrei Astrouski
 */
var FormulaEditor = Class.create(BaseTextEditor, {
    editor_initialize: function() {
        this.createInput();
        this.input.maxLength = this.MAX_FIELD_SIZE

        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    destroy: function() {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["formula"] = FormulaEditor;
}
