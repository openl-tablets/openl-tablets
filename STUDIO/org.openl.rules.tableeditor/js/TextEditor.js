/**
 * Text editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */
var TextEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.createInput();

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                if (Prototype.Browser.IE ? event.ctrlKey : event.altKey) {
                    this.switchTo("multiline");
                }
                break;
        }
    }
});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["text"] = TextEditor;
}
