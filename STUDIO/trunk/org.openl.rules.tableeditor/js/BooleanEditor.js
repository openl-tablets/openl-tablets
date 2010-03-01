/**
 * Boolean editor.
 *
 * @author Andrei Astrouski
 */
var BooleanEditor = Class.create(BaseEditor, {

    editor_initialize: function() {
        this.input = new Element("input");
        this.input.setAttribute("type", "checkbox");

        var self = this;

        ["click", "dblclick"].each(function (s) {self.stopEventPropogation(s)});
    }

});

if (this.tableEditor) {
    TableEditor.Editors["boolean"] = BooleanEditor;
}
