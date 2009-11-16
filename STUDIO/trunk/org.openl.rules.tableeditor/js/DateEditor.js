/**
 * Date editor.
 *
 * @author Andrei Astrouski
 */

var DateEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.createInput();
        this.input.style.width = "85%";

        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})
    },

    show: function($super, value) {
        $super(value);
        var id = this.getId();
        var opts = {
            formElements:{},
            noFadeEffect:true
        };
        opts.formElements[id] = "m-sl-d-sl-Y";
        datePickerController.createDatePicker(opts);
    },

    destroy: function(value) {
        datePickerController.destroyDatePicker(this.getId());
    }

});

TableEditor.Editors["date"] = DateEditor;
