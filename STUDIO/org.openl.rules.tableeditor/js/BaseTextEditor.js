/**
 * Base Text editor.
 *
 * Not an editor itself, it just introduces functions common for all text based editors - that is common reaction
 * to F2 F3 keys.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */

var BaseTextEditor = Class.create(BaseEditor, {

    maxInputSize: null,

    createInput: function() {
        this.input = new Element("input");
        this.input.setAttribute("type", "text");
        if (this.maxInputSize) {
            this.input.maxLength = this.maxInputSize;
        }

        this.setDefaultStyle();

        this.input.setStyle(this.style);
    },

    setDefaultStyle: function() {
        this.input.style.border = "1px solid threedface";
        this.input.style.margin = "0px";
        //this.input.style.padding = "0px";
        this.input.style.width = "100%";
        this.input.style.height = "100%";
    },

    /**
     * Moves caret to beginning of the input.
     */
    handleF2: function(event) {
        var input = this.getInputElement();
        if (input.createTextRange) {
            var r = input.createTextRange();
            r.collapse(true);
            r.select();

        } else if (input.setSelectionRange) {
            input.setSelectionRange(0, 0);
            this.focus();
        }
        Event.stop(event);
    },

    /**
     * Moves caret to the end of the input.
     */
    handleF3: function(event) {
        var input = this.getInputElement();
        if (!input) return;
        if (input.createTextRange) {
            var r = input.createTextRange();
            r.collapse(false);
            r.select();

        } else if (input.setSelectionRange) {
            var len = input.value.length;
            input.setSelectionRange(len, len);
            this.focus();
        }

        if (event) Event.stop(event);
    },

    show: function($super, value) {
        $super(value);
        if (this.focussed) {
            this.handleF3();
        }
    }
});