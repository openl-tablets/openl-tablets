/**
 * Base Text editor.
 *
 * Not an editor itself, it just introduces functions common for all text based editors - that is common reaction
 * to F2 F3 keys.
 *
 * @author Andrey Naumenko
 */

var BaseTextEditor = Class.create(BaseEditor, {

    MAX_FIELD_SIZE : 1500,

    createInput: function() {
        this.input = new Element("input");
        this.input.setAttribute("type", "text");

        this.input.style.borderWidth = "1px";
        this.input.style.borderStyle = "solid";

        this.input.style.fontFamily = this.parentElement.style.fontFamily;
        this.input.style.fontSize = this.parentElement.style.fontSize;
        this.input.style.fontStyle = this.parentElement.style.fontStyle;
        this.input.style.fontWeight = this.parentElement.style.fontWeight;
        this.input.style.textAlign = this.parentElement.align;

        this.input.style.margin = "0px";
        this.input.style.padding = "0px";
        this.input.style.width = "100%";
        this.input.style.height = "100%";
    },

    /**
     * Moves caret to beginning of the input
     */
    handleF2: function(event) {
        var input = this.getInputElement();
        if (input.createTextRange) {
            var r = input.createTextRange();
            r.collapse(true);
            r.select()

        } else if (input.setSelectionRange) {
            input.setSelectionRange(0, 0);
            input.focus()
        }
        Event.stop(event);
    },

    /**
     * Moves caret to the end of the input
     */
    handleF3: function(event) {
        var input = this.getInputElement();
        if (!input) return;
        if (input.createTextRange) {
            var r = input.createTextRange();
            r.collapse(false);
            r.select()

        } else if (input.setSelectionRange) {
            var len = input.value.length;
            input.setSelectionRange(len, len);
            input.focus()
        }

        if (event) Event.stop(event);
    },

    show: function($super, value) {
        $super(value);
        if (this.focus) {
            this.handleF3();
        }
    }
});