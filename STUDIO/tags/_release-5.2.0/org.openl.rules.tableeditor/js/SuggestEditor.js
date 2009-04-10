/**
 * Suggest input editor.
 *
 * @author Aliaksandr Antonik
 */

document.write("<script src='" + jsPath + "scriptaculous/scriptaculous.js'></script>");
document.write("<script src='" + jsPath + "scriptaculous/effects.js'></script>");
document.write("<script src='" + jsPath + "scriptaculous/controls.js'></script>");
document.write("<link rel='stylesheet' type='text/css' href='" + jsPath + "suggest/suggest.css'></link>");

var SuggestEditor = Class.create();

SuggestEditor.Elements = {};

/**
 * Suggest editor, based on Ajax.Autocompleter from scriptaculous JS library. 
 */
SuggestEditor.prototype = Object.extend(new BaseEditor(), {

    /** Constructor */
    editor_initialize: function(param) {
        if (!SuggestEditor.Elements.div) {
            var i = $(document.createElement("input"));
            i.setAttribute("type", "text");
            i.style.border = "0px none";
            i.style.height = "100%";
            i.style.margin = "0px";
            i.style.padding = "0px";
            i.style.width = "100%";
            i.setAttribute("id", "ac_input");
            i.setAttribute("name", "ac_value");
            i.maxLength = this.MAX_FIELD_SIZE;

            SuggestEditor.Elements.input = i;

            var d = $(document.createElement("div"));
            d.setAttribute("id", "ac_container");
            Element.hide(d);
            Element.addClassName(d, "suggestion_list");
            SuggestEditor.Elements.div = d;
            SuggestEditor.completer = new Ajax.Autocompleter(i, d, "autocomplete.jsp");
        }

        var d = SuggestEditor.Elements.div;
        Element.hide(d);
        document.body.appendChild(d);
        this.node = SuggestEditor.Elements.input;

        var self = this;
        ["click", "mousedown", "selectstart"].each(function (s) {self.stopEventPropogation(s)})

        SuggestEditor.completer.options.minChars = param.minChars || 1;
        SuggestEditor.completer.options.frequency = param.delay ? param.delay/1000.0 : 0.4;
    },


    destroy: function() {
        var ac = SuggestEditor.completer;
        if(ac.observer) clearTimeout(ac.observer);
        document.body.removeChild(SuggestEditor.Elements.div);
    }
});

TableEditor.Editors["suggestText"] = SuggestEditor;
