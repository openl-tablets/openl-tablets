/**
 * MultiLine editor.
 *
 * @author Andrey Naumenko
 */
var MultiLineEditor = Class.create();

MultiLineEditor.prototype = Object.extend(new BaseEditor(), {
    eventHandler : null,

    editor_initialize: function() {
        this.node = document.createElement("div");
        var ta = document.createElement("textarea");
        ta.cols = 30;
        ta.rows = 3;
        this.node.appendChild(ta);

        this.node.style.position = "absolute";

        var pos = Position.page(this.td);
        pos[1] += Element.Methods.getDimensions(this.td).height;

        this.node.style.left = pos[0] + "px";
        this.node.style.top = pos[1] + "px";
        this.node.zIndex = "10";

        ta.value = this.td.innerHTML.strip();

        document.body.appendChild(this.node);
        ta.focus();

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(ta, "keydown", this.eventHandler);
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13: BaseEditor.stopPropagationHandler(event); break;
        }
    },

    getValue : function() {
        return this.node.firstChild.value;
    },

    detach: function() {
        BaseEditor.prototype.detach.apply(this);
        document.body.removeChild(this.node);
    },

    destroy: function() {
        Event.stopObserving(this.node, "keydown", this.eventHandler);
    }
});

TableEditor.Editors["multilineText"] = MultiLineEditor;
