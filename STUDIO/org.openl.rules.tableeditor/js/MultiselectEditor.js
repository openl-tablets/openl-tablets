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
    onBlur: null,
    selectAllButton: null,

    editor_initialize: function(param) {
        var self = this;

        this.createInput();
        this.choices = param.choices;
        this.entries = $H();

        // Creating containing DIV
        this.multiselectPanel = new Element("div");
        this.multiselectPanel.className = "multiselect_container_outer";

        // Creating buttons
        var buttonContainer = new Element("div");
        buttonContainer.className = "multiselect_buttons";

        buttonContainer.innerHTML = '<input type="button" value="Select All"> <input type="button" value="Done">';
        var b1 = buttonContainer.down(), b2 = b1.next();
        self.selectAllButton = b1;

        b1.onclick = function() {
            self.setAllCheckBoxes(this.value == "Select All");
            this.value = (this.value == "Select All" ? "Deselect All" : "Select All");
        };

        b2.onclick = function() {
            self.finishEdit();
        };

        this.multiselectPanel.appendChild(buttonContainer);

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

        this.input.onclick = function() {
            self.open();
        };
        this.input.onkeydown = function() {
            self.open();
            return false;
        };
        this.input.oncontextmenu = function() {
            return false;
        };

        this.documentClickListener = this.documentClickHandler.bindAsEventListener(this);

        this.separator = param.separator || ',';
        this.separatorEscaper = param.separatorEscaper;

        this.destroyed = true;
    },

    open: function() {
        var pos = Element.positionedOffset(this.input);
        pos[1] += this.input.getHeight();
        this.multiselectPanel.style.left = pos[0] + "px";
        this.multiselectPanel.style.top = pos[1] + "px";

        this.input.up().appendChild(this.multiselectPanel);

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

        if (isAllBoxesChecked()) {
            this.selectAllButton.value = "Deselect All";
        }

        this.changeSelectAllBtnName(this.selectAllButton);

        Event.observe(document, 'click', this.documentClickListener);
    },

    close: function() {
        if (!this.destroyed) {
            Event.stopObserving(document, 'click', this.documentClickListener);
            Element.remove(this.multiselectPanel);
            this.destroyed = true;
            if (this.onBlur) {
                this.onBlur();
            }
        }
    },

    finishEdit: function() {
        this.setValue(this.combineValue());
        this.handleF3();
        this.destroy();
    },

    splitValue: function(value) {
        if (this.separatorEscaper) {
            var tempEscaper = ";;;";
            var escaper = this.separatorEscaper + this.separator;
            var escaper1 = escaper.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1")
            value = value.replace(new RegExp(escaper1,'g'), tempEscaper);
            var sValues = value.split(this.separator);
            var result = [];
            sValues.each(function (sValue) {
                if (sValue.indexOf(tempEscaper) > -1) {
                    sValue = sValue.replace(new RegExp(tempEscaper,'g'), escaper);
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
                return entries[key].checked;
            }).join(this.separator);
    },

    destroy: function($super) {
        this.close();
        $super();
    },

    focus: function() {
        this.open();
    },

    bind: function($super, event, handler) {
        if (event == "blur") {
            // TODO Use array to keep a few blur handlers
            this.onBlur = handler;
        } else {
            $super(event, handler);
        }
    },

    unbind: function($super, event, handler) {
        if (!event) {
            this.onBlur = null;
            $super(event, handler);
        } else if (event == "blur") {
            this.onBlur = null;
        } else {
            $super(event, handler);
        }
    },

    setAllCheckBoxes: function(value) {
        var entries = this.entries;
        this.choices.findAll(function(key) {
            entries[key].checked = value;
        });
    },

    documentClickHandler: function(e) {
        var element = Event.element(e);
        if (!this.is(element)) {
            this.close();
        }
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
    },

    changeSelectAllBtnName: function(val) {
        var allCheckBoxes = $$('div.multiselect_container input:checkbox');

        allCheckBoxes.each (function (e) {
            e.observe('change', function () {
                val.value = "Select All";
                if (isAllBoxesUnchecked()) {
                    val.value = "Select All";
                }
                if (isAllBoxesChecked()) {
                    val.value = "Deselect All";
                }
            }); 
         }); 
    }

});

if (BaseEditor.isTableEditorExists()) {
	TableEditor.Editors["multiselect"] = MultiselectEditor;
}

function isAllBoxesChecked()  {
    var allCheckBoxes = $$('div.multiselect_container input:checkbox');
    var checkedNumber = 0;
    var isAllChecked = true;

    for (i = 0; i < allCheckBoxes.size(); i++) {
        if (allCheckBoxes[i].checked) {
            checkedNumber ++;
        }
    }

    if (checkedNumber != allCheckBoxes.size()) {
        isAllChecked = false;
    }
    return isAllChecked;
}

function isAllBoxesUnchecked () {
    var allCheckBoxes = $$('div.multiselect_container input:checkbox');
    var uncheckedNumber = 0;
    var isAllUnchecked = false;

    for (i = 0; i < allCheckBoxes.size(); i++) {
        if (!allCheckBoxes[i].checked) {
            uncheckedNumber ++;
        }
    }

    if (uncheckedNumber == allCheckBoxes.size()) {
        isAllUnchecked = true;
    }

    return isAllUnchecked;
}
