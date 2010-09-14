/**
 * Multiple choice editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Aliaksandr Antonik
 * @author Andrei Astrouski
 */
var MultiselectEditor = Class.create(BaseTextEditor, {
    multiselectPanel: null,
    entries: null,
    choices: null,
    separator: null,
    separatorEscaper: null,
    destroyed: null,
    ie6Popup: null,
    ie6: null,

    editor_initialize: function(param) {
        this.ie6 = Prototype.Browser.IE
            && parseInt(navigator.userAgent.substring(
                    navigator.userAgent.indexOf("MSIE") + 5)) == 6;

        this.createInput();
        this.choices = param.choices;
        this.entries = $H();

        // Creating containing DIV
        this.multiselectPanel = new Element("div");
        this.multiselectPanel.className = "multiselect_container_outer";

        // Creating buttons
        var self = this;
        this.multiselectPanel.innerHTML = '&nbsp;<input type="button" value="Select All"> <input type="button" value="Deselect All"> <input type="button" value="Done">&nbsp;'
        var b1 = this.multiselectPanel.down(), b2 = b1.next(), b3 = b2.next();
        b1.onclick = function() {self.setAllCheckBoxes(true)}
        b2.onclick = function() {self.setAllCheckBoxes(false)}
        b3.onclick = function() {self.finishEdit()}

        // Creating inner DIV
        var container = new Element("div");
        container.className = "multiselect_container";

        // Creating entries
        var pc = param.choices, pd = param.displayValues;
        for (var ind = 0, len = pc.length; ind < len; ++ind) {
            var entry = new Element("div");
            entry.innerHTML = '<input type="checkbox">' + pd[ind].escapeHTML();
            container.appendChild(entry);

            this.entries[pc[ind]] = entry.down();
        }

        this.multiselectPanel.appendChild(container);

        this.input.onclick = function(event) {
            self.open();
        };
        this.input.onkeydown = function(event) {
            self.open();
            return false;
        }
        this.input.oncontextmenu = function(event) {
            return false;
        }

        this.documentClickListener = this.documentClickHandler.bindAsEventListener(this);

        this.separator = param.separator || ',';
        this.separatorEscaper = param.separatorEscaper;

        // The iframe hack to cover selectlists in Internet Explorer 6.
        // Selectlists are always on top in IE 6, so they are covered by iframe.
        if (this.ie6) {
            this.createIE6Popup();
        }

        this.destroyed = true;
    },

    open: function() {
        var pos = Element.cumulativeOffset(this.input);
        pos[1] += this.input.getHeight();
        this.multiselectPanel.style.left = pos[0] + "px";
        this.multiselectPanel.style.top = pos[1] + "px";

        document.body.appendChild(this.multiselectPanel);

        if (this.ie6) {
            this.openIE6Popup(
                    pos,
                    this.multiselectPanel.offsetWidth + "px",
                    this.multiselectPanel.offsetHeight + "px");
        }

        this.destroyed = false;
        var entries = this.entries;
        this.splitValue(this.input.value).each(function (key) {
            if (key) {
                var checkbox = entries[key.strip()];
                if (checkbox) {
                    checkbox.checked = true;
                }
            }
        });

        Event.observe(document, 'click', this.documentClickListener);
    },

    close: function() {
        if (!this.destroyed) {
            Event.stopObserving(document, 'click', this.documentClickListener);
            if (this.ie6) {
                this.destroyIE6Popup();
            }
            Element.remove(this.multiselectPanel);
            this.destroyed = true;
        }
    },

    finishEdit: function() {
        AjaxHelper.setInputValue(this.input, this.combineValue());
        this.handleF3();
        this.destroy();
    },

    splitValue: function(value) {
        if (this.separatorEscaper) {
            var tempEscaper = ";;;";
            var escaper = this.separatorEscaper + this.separator;
            value = value.replace(escaper, tempEscaper);
            var sValues = value.split(this.separator);
            var result = [];
            sValues.each(function (sValue) {
                if (sValue.indexOf(tempEscaper) > -1) {
                    sValue = sValue.replace(tempEscaper, escaper);
                }
                result.push(sValue);
            });
            return result;
        }
        return value.split(this.separator);
    },

    combineValue: function() {
        var entries = this.entries;
        return this.choices.findAll(function(key) {
            return entries[key].checked}
        ).join(this.separator)
    },

    destroy: function() {
        this.close();
    },

    setAllCheckBoxes: function(value) {
        var entries = this.entries;
        this.choices.findAll(function(key) {
            entries[key].checked = value;
        });
    },

    documentClickHandler: function(e) {
        var element = Event.element(e);
        var abort = false;
        if (!this.is(element)) {
            this.close();
        }
    },

    createIE6Popup: function() {
        this.ie6Popup = new Element("iframe");
        this.ie6Popup.src = "javascript:'<html></html>';";
        this.ie6Popup.setAttribute('className','ms_ie6Popup');
        // Remove iFrame from tabIndex                                        
        this.ie6Popup.setAttribute("tabIndex", -1);                              
        this.ie6Popup.scrolling = "no";
        this.ie6Popup.frameBorder = "0";
    },

    openIE6Popup: function(pos, width, height) {
        this.ie6Popup.style.left = pos[0] + "px";
        this.ie6Popup.style.top = pos[1] + "px";
        this.ie6Popup.style.width = width;
        this.ie6Popup.style.height = height;
        this.ie6Popup.style.display = "block";

        document.body.appendChild(this.ie6Popup);
    },

    destroyIE6Popup: function() {
        Element.remove(this.ie6Popup);
    },

    is: function($super, element) {
        if ($super(element)) {
            return true;
        } else {
            do {
                if (element == this.multiselectPanel) {
                    return true;
                }
            } while (element = element.parentNode);
        }
        return false;
    }

});

if (BaseEditor.isTableEditorExists()) {
	TableEditor.Editors["multiselect"] = MultiselectEditor;
}