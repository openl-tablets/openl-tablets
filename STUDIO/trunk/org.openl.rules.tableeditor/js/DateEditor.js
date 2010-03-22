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
        this.input.style.width = "85%";

        var self = this;

        this.input.onkeydown = function(event) {
        	return self.keyPressed(event || window.event)
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
            noFadeEffect: true,
            finalOpacity: 100
        };
        datePickerOpts.formElements[inputId] = "m-sl-d-sl-Y";

        var datePickerGlobalOpts = {
            noDrag: true
        };

        datePickerController.setGlobalVars(datePickerGlobalOpts);

        datePickerController.createDatePicker(datePickerOpts);
    },

    destroy: function(value) {
        datePickerController.destroyDatePicker(this.getId());
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
