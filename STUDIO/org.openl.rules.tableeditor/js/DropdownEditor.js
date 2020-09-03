
/**
 * Base dropdown editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Aliaksandr Antonik
 */
var DropdownEditor = Class.create(BaseEditor, {
    /**
     * Constructor. Creates "select" HTML element and fills it with "option"s from param parameter.
     * @param param an Enumeration with options for this dropdown. 
     */
    editor_initialize: function(param) {
        this.createInput();

        this.addOption("", "");
        var pc = param.choices;
        var pd = param.displayValues;
        var len = Math.min(pc.length, pd.length);

        for (var ind = 0; ind < len; ++ind) {
            this.addOption(pc[ind], pd[ind]);
        }
    },

    createInput: function() {
        this.input = new Element("select");

        // Default styles
        this.input.style.border = "1px solid threedface";
        this.input.style.margin = "0px";
        this.input.style.padding = "0px";
        this.input.style.width = "101%";

        this.input.setStyle(this.style);
    },

    /**
     *  Add an option element to this select
     */
    addOption : function(value, name) {
        var optionElement = new Element("option");
        optionElement.value = value;
        optionElement.innerHTML = name;
        this.input.appendChild(optionElement);
    }

});

if (BaseEditor.isTableEditorExists()) {
	TableEditor.Editors["combo"] = DropdownEditor;
}
