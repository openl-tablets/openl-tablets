/**
 * Price editor.
 *
 * @author Andrey Naumenko
 */
var PriceEditor = Class.create(BaseTextEditor, {
    editor_initialize: function() {
        this.createInput();
        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})
    },

    show: function($super, value) {
        if (value.startsWith('$')) {value = value.substr(1)}
        $super(value);
    },

    isCancelled : function() {
        return (this.initialValue == this.getValue() || isNaN(this.getValue()));
    }
});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["price"] = PriceEditor;
}
