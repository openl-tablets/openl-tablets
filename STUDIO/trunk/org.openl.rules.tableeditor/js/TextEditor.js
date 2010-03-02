/**
 * Text editor.
 *
 * @author Andrey Naumenko
 */
var TextEditor = Class.create(BaseTextEditor, {
    // special flag, prevents closing on pressing enter
    __do_nothing_on_enter: true,

    editor_initialize: function() {
        this.createInput();
        this.input.maxLength = this.MAX_FIELD_SIZE;

        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    destroy: function() {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                if ((Prototype.Browser.Opera || Prototype.Browser.IE) ? event.ctrlKey : event.altKey)
                    this.switchTo("multilineText");
                else
                    this.doneEdit();
            break;
        }
    }
});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["text"] = TextEditor;
}
