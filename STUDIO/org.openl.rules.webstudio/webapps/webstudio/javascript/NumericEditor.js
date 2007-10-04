/**
 * Numeric editor.
 *
 * @author Andrey Naumenko
 */
var NumericEditor = Class.create();

NumericEditor.prototype = Object.extend(new BaseTextEditor(), {
    keyPressHandler: null,

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

        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})

        this.node.onkeypress = function(event) {return self.keyPressed(event || window.event)}
        this.node.onkeyup = function(event) {self.keyPressed(event || window.event)}
    },

    isCancelled : function() {
        return (this.initialValue == this.getValue() || isNaN(this.getValue()));
    },

    keyPressed: function(event) {
        var v = this.node.getValue();
        if (event.type == "keypress") {
            if (event.charCode == 0) return true;
            var code = event.charCode == undefined ? event.keyCode : event.charCode;

            if (code == 46) return v.indexOf(".") < 0;
            return code >= 48 && code <= 57
        }

        if (isNaN(v)) this.markInvalid(); else this.markValid();
    }
});

TableEditor.Editors["numeric"] = NumericEditor;
