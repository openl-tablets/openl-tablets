/**
 * Multiline editor.
 *
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */
var MultiLineEditor = Class.create(BaseTextEditor, {
    // Special flag, prevents closing on pressing enter
    __do_nothing_on_enter: true,

    editor_initialize: function() {
        this.createInput();    

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    createInput: function() {
        this.input = new Element("textarea");

        this.setDefaultStyle();
        this.input.setStyle(this.style);
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                if (event.ctrlKey) this.doneEdit();
                break;

            default:
                if (this.maxInputSize && this.input.value.length >= this.maxInputSize) {
                    if (event.charCode != undefined && event.charCode != 0)
                        Event.stop(event);
                }
                break;
        }
    },

    getValue: function() {
        var res = this.input.value;
        return res.gsub("\r\n", "\n").replace(/\n$/, "");
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["multiline"] = MultiLineEditor;
}
