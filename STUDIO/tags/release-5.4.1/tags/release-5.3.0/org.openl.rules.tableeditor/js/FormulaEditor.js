/**
 * Formula editor.
 *
 * @author Andrei Astrouski
 */
var FormulaEditor = Class.create();

FormulaEditor.prototype = Object.extend(new BaseTextEditor(), {
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

        this.node.maxLength = this.MAX_FIELD_SIZE

        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.node, "keypress", this.eventHandler);
    },

    destroy: function() {
        Event.stopObserving(this.node, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
    }

});

TableEditor.Editors["formula"] = FormulaEditor;
