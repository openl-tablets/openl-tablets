/**
 * Text editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */
var TextEditor = Class.create(BaseTextEditor, {
    // Special flag, prevents closing on pressing enter
    __do_nothing_on_enter: true,

    editor_initialize: function() {
        this.createInput();

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    destroy: function() {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                if ((Prototype.Browser.Opera || Prototype.Browser.IE) ? event.ctrlKey : event.altKey) {
                    this.switchTo("multiline");
                } else {
                    this.doneEdit();
                }
            break;
        }
    }
});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["text"] = TextEditor;
}
