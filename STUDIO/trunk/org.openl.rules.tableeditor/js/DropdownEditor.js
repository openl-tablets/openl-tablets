
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
        this.createInput();

        this.addOption("", "");
        var pc = param.choices;
        var pd = param.displayValues;
        var len = Math.min(pc.length, pd.length);

        for (var ind = 0; ind < len; ++ind) {
            this.addOption(pc[ind], pd[ind]);
        }

        var self = this;                       
        ["click", "mousedown", "selectstart"].each(function (s) { self.stopEventPropogation(s) })
    },

    createInput: function() {
        this.input = new Element("select");

        this.input.style.borderWidth = "1px";
        this.input.style.borderStyle = "solid";

        this.input.style.fontFamily = this.parentElement.style.fontFamily;
        this.input.style.fontSize = this.parentElement.style.fontSize;
        this.input.style.fontStyle = this.parentElement.style.fontStyle;
        this.input.style.fontWeight = this.parentElement.style.fontWeight;
        this.input.style.textAlign = this.parentElement.align;

        this.input.style.margin = "0px";
        this.input.style.padding = "0px";
        this.input.style.width = "101%";
    },

    /**
     *  @desc add an option element to this select
     *  @type public
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