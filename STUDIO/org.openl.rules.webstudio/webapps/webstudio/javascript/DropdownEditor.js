/**
 * Base dropdown editor.
 *
 * @author Aliaksandr Antonik.
 */
var DropdownEditor = Class.create();

DropdownEditor.prototype = Object.extend(new BaseEditor(), {
    /** Constructor */
    editor_initialize: function(param) {
        this.node = document.createElement("select");
        this.node.style.width = "100%";
        this.node.style.border = "0px none";
        this.node.style.margin = "0px";
        this.node.style.padding = "0px";

        this.addOption("", "--Select value--");
        var self = this;
        param.each(function (el) {
            self.addOption(el, el)
        });

        this.node.value = this.td.innerHTML;

        this.node.observe("click", BaseEditor.stopPropagationHandler, false);
        this.node.observe("mousedown", BaseEditor.stopPropagationHandler, false);
        this.node.observe("selectstart", BaseEditor.stopPropagationHandler, false);

        this.td.innerHTML = "";
        this.td.appendChild(this.node);
        this.node.focus();
    },

    /**
     *  @desc add an option element to this select
     *  @type public
     */
    addOption : function(/* String */ value, /* String */ name) {
        var optionElement = document.createElement("option");
        optionElement.value = value;
        optionElement.innerHTML = name;
        this.node.appendChild(optionElement);
    },

    isCancelled : function() {
        return (this.initialValue == this.getValue() || this.node.value == "");
    }
});

TableEditor.Editors["combo"] = DropdownEditor;