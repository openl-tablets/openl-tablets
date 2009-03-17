/**
 * Base Text editor.
 *
 * @author Andrey Naumenko
 */

var BaseTextEditor = Class.create();

/**
 * Not an editor itself, it just introduces functions common for all text based editors - that is common reaction
 * to F2 F3 keys.
 */
BaseTextEditor.prototype = Object.extend(new  BaseEditor(), {
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

    show: function(value) {
        BaseEditor.prototype.show.call(this, value);
        this.handleF3();
    }
});