/**
 * Date editor.
 *
 * @author Andrei Astrouski
 */

var DateEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.node = $(document.createElement("input"));
        this.node.setAttribute("type", "text");
        this.node.style.border = "0px none";
        this.node.style.height = (this.td.offsetHeight - (Prototype.Browser.IE ? 6 : 4)) + "px";

        this.node.style.fontFamily = this.td.style.fontFamily;
        this.node.style.fontSize = this.td.style.fontSize;
        this.node.style.fontStyle = this.td.style.fontStyle;
        this.node.style.fontWeight = this.td.style.fontWeight;
        this.node.style.textAlign = this.td.align;

        this.node.style.margin = "0px";
        this.node.style.padding = "0px";
        this.node.style.width = "93%";

        this.node.setAttribute("id", "datepicker111");

        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})
    },

    show: function(value) {
        BaseTextEditor.prototype.show.call(this, value);
        var opts = {
            formElements:{"datepicker111":"m-sl-d-sl-Y"}
        };
        datePickerController.createDatePicker(opts);
    },

    destroy: function(value) {
        datePickerController.destroyDatePicker("datepicker111");
    }

});

TableEditor.Editors["date"] = DateEditor;
