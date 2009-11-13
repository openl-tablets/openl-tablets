/**
 * Boolean editor.
 *
 * @author Andrei Astrouski
 */
var BooleanEditor = Class.create(BaseEditor, {

    editor_initialize: function() {
        this.input = $(document.createElement("input"));
        this.input.setAttribute("type", "checkbox");

        var self = this;

        ["click", "dblclick"].each(function (s) {self.stopEventPropogation(s)});
    }

});

TableEditor.Editors["boolean"] = BooleanEditor;
