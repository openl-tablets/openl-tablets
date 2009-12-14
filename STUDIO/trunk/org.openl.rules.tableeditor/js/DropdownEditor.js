/**
 * Base dropdown editor.
 *
 * @author Aliaksandr Antonik.
 */
var DropdownEditor = Class.create(BaseEditor, {
    /**
     * Constructor. Creates "select" HTML element and fills it with "option"s from param parameter.
     * @param param an Enumeration with options for this dropdown. 
     */
    editor_initialize: function(param) {
        this.input = $(document.createElement("select"));
        this.input.style.width = "100%";
        this.input.style.margin = "0px";
        this.input.style.padding = "0px";
        this.input.style.border = "0px none";

        this.addOption("", "--Select value--");
        this.addOption(" ", "--Empty-- ");
        var pc = param.choices, pd = param.displayValues, len = Math.min(pc.length, pd.length);
        for (var ind = 0; ind < len; ++ind) this.addOption(pc[ind], pd[ind]);

        var self = this;                       
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})
    },

    /**
     *  @desc add an option element to this select
     *  @type public
     */
    addOption : function(/* String */ value, /* String */ name) {
        var optionElement = document.createElement("option");
        optionElement.value = value;
        optionElement.innerHTML = name;
        this.input.appendChild(optionElement);
    },

    /**
     *  Overrides default implementation. When user chose "--Select value--" item we regard editor state as cancelled.   
     */
    isCancelled : function() {
        return (this.initialValue == this.getValue() || this.input.value == "");
    }
});

TableEditor.Editors["combo"] = DropdownEditor;