/**
 * Multiple choice editor.
 *
 * @author Aliaksandr Antonik
 * @author Andrei Astrouski
 */
var MultiselectEditor = Class.create(BaseTextEditor, {
    multiselectPanel: null,
    ulElement: null,
    entries: null,
    choices: null,
    separator: null,
    separatorEscaper: null,
    destroyed: null,

    editor_initialize: function(param) {
        this.createInput();
        this.choices = param.choices;
        this.entries = $H();

        // creating containing DIV
        this.multiselectPanel = new Element("div");
        this.multiselectPanel.style.position = "absolute";
        this.multiselectPanel.zIndex = "10";
        this.multiselectPanel.className = "multiselect_container_outer";

        // creating buttons
        var self = this;
        this.multiselectPanel.innerHTML = '&nbsp;<input type="button" value="Select All"> <input type="button" value="Deselect All"> <input type="button" value="Done">&nbsp;'
        var b1 = this.multiselectPanel.down(), b2 = b1.next(), b3 = b2.next();
        b1.onclick = function() {self.setAllCheckBoxes(true)}
        b2.onclick = function() {self.setAllCheckBoxes(false)}
        b3.onclick = function() {self.finishEdit()}

        // creating inner DIV
        var container = new Element("div");
        container.className = "multiselect_container";
        this.multiselectPanel.appendChild(new Element("br"));
        this.multiselectPanel.appendChild(container);

        // creating UL HTML element
        this.ulElement = new Element("ul");
        container.appendChild(this.ulElement);

        // creating entries
        var pc = param.choices, pd = param.displayValues;
        for (var ind = 0, len = pc.length; ind < len; ++ind) {
            var li = new Element("li");
            this.ulElement.appendChild(li);

            li.innerHTML = '<input type="checkbox" name="multiselect_cb">' + pd[ind].escapeHTML();
            this.entries[pc[ind]] = li.down();
        }

        ["click", "dblclick"].each(function (s) {self.stopEventPropogation(s)});
        this.input.onclick = function() {
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

        this.destroyed = true;
    },

    open: function() {
        var pos = Element.cumulativeOffset(this.input);
        pos[1] += this.input.getHeight();
        this.multiselectPanel.style.left = pos[0] + "px";
        this.multiselectPanel.style.top = pos[1] + "px";

        document.body.appendChild(this.multiselectPanel);
        this.destroyed = false;
        var entries = this.entries;
        this.splitValue(this.input.value).each(function (key) {
            if (key && entries[key]) {
                entries[key].checked = true;
            }
        });
        Event.observe(document, 'click', this.documentClickListener);
    },

    close: function() {
        if (!this.destroyed) {
            Event.stopObserving(document, 'click', this.documentClickListener);
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
        do {
            if (element == this.multiselectPanel) {
                abort = true;
            }
        } while (element = element.parentNode);
        if (!abort) {
            this.close();
        }
    }

});

TableEditor.Editors["multiselect"] = MultiselectEditor;