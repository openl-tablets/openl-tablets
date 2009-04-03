/**
 * MultiLine editor.
 *
 * @author Andrey Naumenko
 */
var MultiLineEditor = Class.create();

MultiLineEditor.prototype = Object.extend(new BaseTextEditor(), {
    // special flag, prevents closing on pressing enter
    __do_nothing_on_enter: true,
    ta : null,

    editor_initialize: function() {
        this.node = document.createElement("div");
        this.ta = document.createElement("textarea");
        this.ta.cols = 50;
        this.ta.rows = 6;
        this.ta.maxLength = this.MAX_FIELD_SIZE;
        this.node.appendChild(this.ta);

        this.node.style.position = "absolute";

        var pos = Position.page(this.td);
        pos[1] += Element.Methods.getDimensions(this.td).height;

        this.node.style.left = pos[0] + "px";
        this.node.style.top = pos[1] + "px";
        this.node.zIndex = "10";

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.node, "keypress", this.eventHandler);

        this.stopEventPropogation("click");
    },

    show: function(value) {
        var len = value.length;
        if (len > 600)
            this.ta.cols += 20;
        var rows = len / (this.ta.cols - 3);
        if (rows > this.ta.rows)
            this.ta.rows = Math.min(rows, 20);

        this.ta.value = value;
        document.body.appendChild(this.node);
        this.ta.focus();
        this.handleF3();
    },

    checkLength : function() {
        if (this.ta.value.length > this.MAX_FIELD_SIZE)
            this.ta.value = this.ta.value.substr(0, this.MAX_FIELD_SIZE);
    },

    handleKeyPress: function (event) {
        this.checkLength();
        switch (event.keyCode) {
            case 13:
                if (event.ctrlKey) this.doneEdit();
            break;

            default:
                if (this.ta.value.length >= this.MAX_FIELD_SIZE) {
                    if (event.charCode != undefined && event.charCode != 0)
                        Event.stop(event);
                }
                break;
        }
    },

    destroy: function() {
        document.body.removeChild(this.node);
        Event.stopObserving(this.node, "keypress", this.eventHandler);
    },

    getValue : function() {
        this.checkLength();
        var res = this.ta.value;
        return res.gsub("\r\n", "\n").replace(/\n$/, "");
    },

    /**
     *  Overrides BaseTextEditor.getInputElement
     */
    getInputElement : function() {return this.ta}
});

TableEditor.Editors["multilineText"] = MultiLineEditor;
