/**
 * Text editor.
 *
 * @author Andrey Naumenko
 */
var TextEditor = Class.create();

TextEditor.prototype = Object.extend(new BaseEditor(), {
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
        this.node.style.width = "100%";

        this.node.observe("click", BaseEditor.stopPropagationHandler, false);
        this.node.observe("mousedown", BaseEditor.stopPropagationHandler, false);
        this.node.observe("selectstart", BaseEditor.stopPropagationHandler, false);
    }
});

TableEditor.Editors["text"] = TextEditor;
