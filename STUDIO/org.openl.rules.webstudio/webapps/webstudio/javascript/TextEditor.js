/**
 * Base class for text based editors.
 *
 * @author Andrey Naumenko
 */
var TextEditor = Class.create();

TextEditor.prototype = Object.extend(new BaseEditor(), {

    /** Constructor */
    initialize : function() {
        this.node = document.createElement("input");
        this.node.setAttribute("type", "text");
        this.node.style.border = "0px none";
        this.node.style.height = "100%";
        this.node.style.margin = "0px";
        this.node.style.padding = "0px";
        this.node.style.width = "100%";
    }

});
