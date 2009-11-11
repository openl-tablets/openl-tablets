/**
 * Boolean editor.
 *
 * @author Andrei Astrouski
 */
var BooleanEditor = Class.create(BaseEditor, {

    editor_initialize: function() {
        this.node = $(document.createElement("input"));
        this.node.setAttribute("type", "checkbox");
        this.node.style.border = "0px none";

        var self = this;

        ["click", "dblclick"].each(function (s) {self.stopEventPropogation(s)});
    }

});

TableEditor.Editors["boolean"] = BooleanEditor;
