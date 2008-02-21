/**
 * Price editor.
 *
 * @author Andrey Naumenko
 */
var PriceEditor = Class.create();

PriceEditor.prototype = Object.extend(new BaseTextEditor(), {
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
    },

    show: function(v) {
        if (v.startsWith('$')) {v = v.substr(1)}
        BaseEditor.prototype.show.call(this, v);
    },

    isCancelled : function() {
        return (this.initialValue == this.getValue() || isNaN(this.getValue()));
    }
});

TableEditor.Editors["price"] = PriceEditor;
