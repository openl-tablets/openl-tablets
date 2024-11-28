/**
 * Date editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */

var DateEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.createInput();

        this.input.style.width = "-webkit-calc(100% - 28px)";
        this.input.style.width =         "calc(100% - 28px)";

        var self = this;

        this.input.onkeydown = function(event) {
            return self.keyPressed(event || window.event);
        }
        this.input.oncontextmenu = function(event) {
            return false;
        }
        this.input.onclick = function() {
            datePickerController.show(self.getId());
        };
    },

    show: function($super, value) {
        $super(value);

        var inputId = this.getId();

        var datePickerOpts = {
            formElements: {},
            finalOpacity: 100
        };
        datePickerOpts.formElements[inputId] = "m-sl-d-sl-Y";

        var datePickerGlobalOpts = {
            noDrag: true
        };

        datePickerController.setGlobalVars(datePickerGlobalOpts);

        datePickerController.createDatePicker(datePickerOpts);

        if (this.focussed) {
            this.focus();
        }
    },

    focus: function() {
        datePickerController.show(this.getId());
    },

    bind: function($super, event, handler) {
        if (event == "blur") {
            datePickerController.onBlur(this.getId(), handler);
        } else {
            $super(event, handler);
        }
    },

    unbind: function($super, event, handler) {
        if (!event) {
            datePickerController.onBlur(this.getId(), "");
            $super(event, handler);
        } else if (event == "blur") {
            datePickerController.onBlur(this.getId(), "");
        } else {
            $super(event, handler);
        }
    },

    destroy: function($super) {
        datePickerController.destroyDatePicker(this.getId());
        $super();
    },

    keyPressed: function(event) {
    	var keyCode = event.keyCode;
        switch (keyCode) {
        	case Event.KEY_BACKSPACE:
        	case Event.KEY_DELETE:
        		this.input.value = '';
        		break;
        }
        return false;
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["date"] = DateEditor;
}
