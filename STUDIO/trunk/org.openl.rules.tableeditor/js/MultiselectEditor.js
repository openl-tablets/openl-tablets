/**
 * Multiple choice editor.
 *
 * @author Aliaksandr Antonik
 */
var MultiselectEditor = Class.create(BaseTextEditor, {
    multiselectBox: null,
    ulElement: null,
    entries: null,
    sortedKeys: null,
    separator: null,
    separatorEscaper: null,
    destroyed: null,

    editor_initialize: function(param) {
        this.createInput();
        this.sortedKeys = param.choices;
        this.entries = $H();

        // creating containing DIV
        multiselectBox = new Element("div");
        multiselectBox.style.position = "absolute";
        multiselectBox.zIndex = "10";
        multiselectBox.className = "multiselect_container_outer";

        // creating buttons
        var self = this;
        multiselectBox.innerHTML = '&nbsp;<input type="button" value="Select All"> <input type="button" value="Deselect All"> <input type="button" value="Done">&nbsp;'
        var b1 = multiselectBox.down(), b2 = b1.next(), b3 = b2.next();
        b1.onclick = function() {self.setAllCheckBoxes(true)}
        b2.onclick = function() {self.setAllCheckBoxes(false)}
        b3.onclick = function() {self.close()}

        // creating inner DIV
        var container = new Element("div");
        container.className = "multiselect_container";
        multiselectBox.appendChild(new Element("br"));
        multiselectBox.appendChild(container);

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

        this.separator = param.separator || ',';
        this.separatorEscaper = param.separatorEscaper;

        this.destroyed = true;
    },

    show: function($super, value) {
        $super(value);
        var pos = Element.cumulativeOffset(this.input);
        pos[1] += this.input.getHeight();
        multiselectBox.style.left = pos[0] + "px";
        multiselectBox.style.top = pos[1] + "px";
    },

    open: function() {
        document.body.appendChild(multiselectBox);
        this.destroyed = false;
        var entries = this.entries;
        this.splitValue(this.input.value).each(function (key) {
            if (key && entries[key]) {
                entries[key].checked = true;
            }
        });
    },

    close: function() {
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
        return this.sortedKeys.findAll(function(key) {
            return entries[key].checked}
        ).join(this.separator)
    },

    destroy: function() {
        if (!this.destroyed) {
            document.body.removeChild(multiselectBox);
            this.destroyed = true;
        }
    },

    setAllCheckBoxes: function(value) {
        var entries = this.entries;
        this.sortedKeys.findAll(function(key) {
            entries[key].checked = value;
        });
    }
});

TableEditor.Editors["multiselect"] = MultiselectEditor;