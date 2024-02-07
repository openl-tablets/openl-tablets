/**
 * Range editor.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Anastasia Abramova
 * TODO Most of the parsing was moved to server. Remove unused parsing from script.
 */
var NumberRangeEditor = Class.create(BaseTextEditor, {

    rangePanel: null,
    resultContainer: null,
    btnDone: null,

    // TD with checkBoxes and input text elements
    tdValues: null,

    // CheckBoxes and input text elements
    checkboxes: null,
    values: null,

    // Buttons
    btns: null,

    // Default separator; <, <=, >, >= also concern to default separators
    defaultSeparator: " .. ",
    // TODO merge everywhere in code " - " and "-". Trim separators (excluding " .. ") and numbers.
    // Separator changed to default separator only if user change separator's logic
    stableSeparators: ["more than ", "less than ", " and more", " - "],
    // Separator usually changed to default separator
    unstableSeparators: [" ... ", ";", "+"],
    // Concern to stableSeparators
    dashSeparator: "-",
    currentSeparator: null,

    destroyed: null,
    entryEditor: null,
    parsedValue: null,

    equals: false,
    moreThan: false,
    range: false,
    onBlur: null,

    editor_initialize: function(param) {
        var self = this;
        self.createInput();

        // Creating containing DIV
        self.rangePanel = new Element("div", {"class": "range"});

        var table = new Element("table", {"class": "range-table"});

        // Creating variables with checkbox
        self.checkboxes = new Array(2);

        // Creating variables with inputText
        self.tdValues = new Array(2);
        self.values = new Array(2);
        var i; // JavaScript doesn't have block level variables. Only function body, global object or page.
        for (i = 0; i < self.tdValues.length; i++) {
            self.tdValues[i] = new Element("td");
            self.values[i] = new Element("input");
            self.values[i].value = "";
        }
        table.appendChild(this.tdValues[0]);

        // Creating td with buttons
        var buttnons = new Element("td")
            .update('<input type="button" title="More than" value=">"/> <input type="button" title="Less than" value="<"/> <input type="button" title="Range" value="−"/> <input type="button" title="Equals" value="="/>');
        table.appendChild(buttnons);
        table.appendChild(this.tdValues[1]);
        self.rangePanel.appendChild(table);

        // Creating variables with buttons
        self.btns = new Array(4);
        self.btns[0] = buttnons.down();
        for (i = 1; i < self.btns.length; i++) {
            self.btns[i] = self.btns[i - 1].next();
        }
        for (i = 0; i < self.btns.length; i++) {
            self.btns[i].className = "range-btn";
            self.btns[i].onclick = function() {
                self.currentSeparator = self.defaultSeparator;
                self.createIntervalBorders(self.btns.indexOf(this));
                self.createResult();
            };
          }

        // Creating result DIV
        self.resultContainer = new Element("div", {"class": "range-result"});
        this.rangePanel.appendChild(self.resultContainer);

        // Creating button Done
        var buttonContainer = new Element("div", {"class": "range-done"});
        buttonContainer.innerHTML = '<input type="button" value="Done">';
        self.btnDone = buttonContainer.down();
        self.btnDone.onclick = function() {
            self.finishEdit();
        };
        this.rangePanel.appendChild(buttonContainer);

        if (param) {
            this.entryEditor = param.entryEditor;
            this.parsedValue = param.parsedValue;
        }

        this.input.onclick = function () {
            self.open();
        };

        this.input.onkeydown = function() {
            self.open();
            return false;
        };

        this.input.oncontextmenu = function() {
            return false;
        };

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.rangePanel, "keypress", this.eventHandler);

        this.documentClickListener = this.documentClickHandler.bindAsEventListener(this);

        this.destroyed = true;
    },

    show: function($super, value) {
        $super(value);
        this.open();
    },

    open: function() {
        this.input.up().appendChild(this.rangePanel);
        this.destroyed = false;
        // var value = this.input.value;
        var value = this.parsedValue;
        if (value === null) {
            value = this.input.value;
        }
        var self = this;
        var values;
        self.stableSeparators.each(function(separator) {
            if (value.indexOf(separator) !== -1) {
                self.currentSeparator = separator;
                if (separator == " - ") {
                    // Separator == " - "
                    var leftIncluding = true;
                    var rightIncluding = true;

                    if ((value.charAt(0) == "[") || (value.charAt(0) == "(")) {
                        leftIncluding = value.charAt(0) == "[";
                        rightIncluding = value.charAt(value.length - 1) == "]";
                        value = value.substring(1, value.length - 1);
                    }

                    values = self.splitValue(value, separator);

                    self.createIntervalBorders(2);
                    self.values[0].value = values[0];
                    self.values[1].value = values[1];
                    if (leftIncluding) {
                        self.checkboxes[0].setAttribute("checked", "checked");
                    }
                    if (rightIncluding) {
                        self.checkboxes[1].setAttribute("checked", "checked");
                    }
                } else {
                    // Separator == "less than " || "more than " || " and more"
                    values = self.splitValue(value, separator);
                    if (separator == "less than ") {
                        self.createIntervalBorders(1);
                    } else {
                        self.moreThan = "true";
                        self.createIntervalBorders(0);
                    }
                    if (values[0]) {
                        self.values[1].value = values[0];
                        self.checkboxes[1].setAttribute("checked", "checked");
                    } else {
                        self.values[1].value = values[1];
                    }
                }
            }
        });
        self.unstableSeparators.each(function(separator) {
            if (value.indexOf(separator) !== -1) {
                self.currentSeparator = self.defaultSeparator;
                values = self.splitValue(value, separator);
                if (separator == ";") {
                    // Separator == ";"
                    self.createIntervalBorders(2);
                    if (values[0].charAt(0) == "[") {
                        self.checkboxes[0].setAttribute("checked", "checked");
                    }
                    if (values[1].charAt(values[1].length - 1) == "]") {
                        self.checkboxes[1].setAttribute("checked", "checked");
                    }
                    self.values[0].value = values[0].substring(1);
                    self.values[1].value = values[1].substring(0, values[1].length - 1);
                } else if (separator == " ... ") {
                    // Separator == " ... "
                    self.createIntervalBorders(2);
                    self.values[0].value = values[0];
                    self.values[1].value = values[1];
                } else {
                    // Separator == "+"
                    self.createIntervalBorders(0);
                    self.moreThan = "true";
                    self.values[1].value = values[0];
                    self.checkboxes[1].setAttribute("checked", "checked");
                }
            }
        });
        if ((value.indexOf(self.defaultSeparator) != -1) || (value.charAt(0) == "<")||(value.charAt(0) == ">")) {
            self.currentSeparator = self.defaultSeparator;
            if ((value.charAt(0) == "<")||(value.charAt(0) == ">")) {
                // Separator == "<" || "<=" || ">" || ">="
                if (value.charAt(0) == "<") {
                    self.createIntervalBorders(1);
                } else {
                    self.createIntervalBorders(0);
                    self.moreThan = "true";
                }
                
                if (value.charAt(1) == "=") {
                    self.checkboxes[1].setAttribute("checked", "checked");
                    self.values[1].value = value.substring(2);
                } else {
                    self.values[1].value = value.substring(1);
                }
            } else {
                // Separator == " .. "
                self.createIntervalBorders(2);
                if ((value.charAt(0) == "[") || (value.charAt(0) == "(")) {
                    if (value.charAt(0) == "[") {
                        self.checkboxes[0].setAttribute("checked", "checked");
                    }
                    if (value.charAt(value.length - 1) == "]") {
                        self.checkboxes[1].setAttribute("checked", "checked");
                    }
                    values = self.splitValue(value.substring(1, value.length - 1), self.currentSeparator);
                } else {
                    values = self.splitValue(value, self.currentSeparator);
                    self.checkboxes[0].setAttribute("checked", "checked");
                    self.checkboxes[1].setAttribute("checked", "checked");
                }
                self.values[0].value = values[0];
                self.values[1].value = values[1];
            }
        }
        if (!self.currentSeparator) {
            var leftIncluding = true;
            var rightIncluding = true;
            if ((value.charAt(0) == "[") || (value.charAt(0) == "(")) {
                leftIncluding = value.charAt(0) == "[";
                rightIncluding = value.charAt(value.length - 1) == "]";
                value = value.substring(1, value.length - 1);
            }
            self.currentSeparator = self.dashSeparator;
            values = self.splitValue(value, self.currentSeparator);
            if (values[0] && values[1]) {
                // Separator = "-"
                self.createIntervalBorders(2);
                self.values[0].value = values[0];
                self.values[1].value = values[1];
                if (leftIncluding) {
                    self.checkboxes[0].setAttribute("checked", "checked");
                }
                if (rightIncluding) {
                    self.checkboxes[1].setAttribute("checked", "checked");
                }
            } else if (!values[0] && !values[1] || values[0] && !self.isValid(values[0])){
                // Empty cell || invalid character in the cell
                if (values[0]) {
                    //self.input.value = null;
                    self.parsedValue = null;
                }
                self.currentSeparator = self.defaultSeparator;
                self.values[0].value = "";
                self.values[1].value = "";
                self.createIntervalBorders(2);
                if (leftIncluding) {
                    self.checkboxes[0].setAttribute("checked", "checked");
                }
                if (rightIncluding) {
                    self.checkboxes[1].setAttribute("checked", "checked");
                }
                self.btnDone.setAttribute("disabled", "disabled");
            } else {
                // Constant in the cell
                self.currentSeparator = self.defaultSeparator;
                self.createIntervalBorders(3);
                if (values[0]) {
                    self.values[1].value = values[0];
                } else {
                    self.values[1].value = -values[1];
                }
                self.equals = true;
            }
        }
        self.createResult();
        Event.observe(document, 'click', self.documentClickListener);
        self.values[1].focus();
    },

    // Creates range in result DIV
    createResult: function() {
        var content = "";
        if (this.values[1].value && (this.currentSeparator == "more than " || this.currentSeparator == "less than ")) {
            content = this.currentSeparator + this.values[1].value;
        } else if (this.values[1].value && this.currentSeparator == " and more") {
            content = this.values[1].value + this.currentSeparator;
        } else if ((this.currentSeparator == " - " || this.currentSeparator == "-")
            && this.checkboxes[0].checked && this.checkboxes[1].checked) {
            content = this.values[0].value + " - " + this.values[1].value;
        } else if (this.range && this.values[0].value != "") {
            // Current separator == " .. "
            if (this.checkboxes[0].checked && this.checkboxes[1].checked) {
                content = this.values[0].value + " .. " + this.values[1].value;
            } else {
                if (this.checkboxes[0].checked) {
                    content = "[";
                } else {
                    content = "(";
                }
                content = content + this.values[0].value + " .. " + this.values[1].value;
                if (this.checkboxes[1].checked) {
                    content = content + "]";
                } else {
                    content = content + ")";
                }
            }
        } else if (this.values[1].value) {
            // Constant or interval with infinity
            if (!this.equals) {
                if (this.moreThan) {
                    content = ">";
                } else {
                    content = "<";
                }
                if (this.checkboxes[1].checked) {
                    content = content + "=";
                }
            }
            content = content + this.values[1].value;
        }
        this.resultContainer.innerHTML = content;
        if (content == "") {
            this.btnDone.setAttribute("disabled", "disabled");
        } else {
            this.btnDone.removeAttribute("disabled");
        }
    },

    // Creates right and left sides of Range Editor
    createIntervalBorders: function(btnId) {
        var self = this;
        self.range = false;
        if (btnId == 2) {
            self.range = true;
            self.tdValues[0].update('<div class="range-label">From</div> <input type="checkbox" title="Include" /> <input type="text" />');
            var minValue = self.values[0].value;
            self.values[0] = self.tdValues[0].down("input[type='text']");
            self.values[0].className = "range-input";
            self.values[0].value = minValue;
            var checked = false;
            if (self.checkboxes[0]) {
                checked = self.checkboxes[0].checked;
            }
            self.checkboxes[0] = self.tdValues[0].down("input[type='checkbox']");
            if (checked) {
                self.checkboxes[0].setAttribute("checked", "checked");
            }
            self.tdValues[1].update('<div class="range-label">To</div> <input type="text" /> <input type="checkbox" title="Include" />');
            self.checkboxes[0].onclick = function() {
                self.currentSeparator = self.defaultSeparator;
                self.createResult();
            };
            self.values[0].onkeypress = function(event) {return self.keyPressed(event || window.event);};
            self.values[0].onkeyup = function() {self.createResult();};
        } else if (btnId == 3) {
            self.values[0].value = "";
            self.tdValues[0].update('');
            self.tdValues[1].update('<div class="range-label">&nbsp;</div> <input type="text" />');
        } else {
            if (btnId == 0) {
                self.tdValues[1].update('<div class="range-label">&nbsp;</div> <input type="checkbox" title="Include" /> <input type="text" />');
            } else {
                self.tdValues[1].update('<div class="range-label">&nbsp;</div> <input type="text" /> <input type="checkbox" title="Include" />');
            }
            self.tdValues[0].update('');
            self.values[0].value = "";
        }

        self.equals = true;
        if (btnId != 3) {
            self.equals = false;
            if (btnId != 2) {
                self.tdValues[1].update('<div class="range-label">&nbsp;</div> <input type="text" /> <input type="checkbox" title="Include" />');
            }
            if (self.checkboxes[1] && self.checkboxes[1].checked) {
                self.tdValues[1].down("input[type='checkbox']").setAttribute("checked", "checked");
            }
            self.checkboxes[1] = self.tdValues[1].down("input[type='checkbox']");
            self.checkboxes[1].onclick = function() {
                self.currentSeparator = self.defaultSeparator;
                self.createResult();
            };
        }

        if (btnId == 0) {
            self.moreThan = true;
            if (self.values[0].value) {
                self.tdValues[1].down("input[type='text']").value = self.values[0].value;
            } else if (self.values[1].value) {
                self.tdValues[1].down("input[type='text']").value = self.values[1].value;
            }
        } else {
            self.moreThan = false;
            if (self.values[1].value) {
                self.tdValues[1].down("input[type='text']").value = self.values[1].value;
            }
        }

        for (var i = 0; i < self.btns.length; i++) {
            if (i != btnId) {
                self.btns[i].removeClassName("range-btn-selected");
            } else {
                self.btns[i].addClassName("range-btn-selected");
            }
        }

        self.values[1] = self.tdValues[1].down("input[type='text']");
        self.values[1].className = "range-input";
        self.values[1].onkeypress = function(event) {
            return self.keyPressed(event || window.event);
        };
        self.values[1].onkeyup = function() {
            self.createResult();
        };
    },

    close: function() {
        if (!this.destroyed) {
            ///Event.stopObserving(document, 'click', this.documentClickListener);
            var el = $(this.rangePanel);
            // If switch to formula editor, error in browser console is shown because el.parentNode is null.
            if (el.parentNode) {
                Element.remove(this.rangePanel);
            }

            this.destroyed = true;

            if (this.onBlur) {
                this.onBlur();
            }
        }
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

    finishEdit: function() {
        // FIXME WTF? If is not valid, at least, we must show error message. Don't just suppress save.
        if (this.isValid(this.values[0].value) && this.isValid(this.values[1].value)) {
            this.setValue(this.createFinalResult());
            this.handleF3();
            this.destroy();
        }
    },

    isValid: function(value) {
        var valid = true;
        var self = this;
        if ((value.charAt(0) == "<")||(value.charAt(0) == ">")) {
            if (value.charAt(1) == "=") {
                value = value.substring(2);
            } else {
                value = value.substring(1);
            }
        }
        if ((value.charAt(0) == "(")||(value.charAt(0) == "[")) {
            value = value.substring(1, value.length - 1);
        }
        if (self.entryEditor && value) {
            var values = value.split(self.currentSeparator);
            if ((values.length == 3) && (self.currentSeparator == self.dashSeparator) && (value.charAt(0) == "-")) {
                values.splice(1, 1);
            }
            if (!values[0]) {
                values.splice(0, 1);
            }
            if (!values[1]) {
                values.splice(1, 1);
            }
            values.each(function(v) {
                v = v.trim(); // For cases like containing spaces like "[12; 15)"
                if (self.entryEditor == "integer") {
                    var matchInt = v.match(/^\$?-?[,0-9]+[KMB]?$/);
                    if (!matchInt) {
                        valid = false;
                    }
                } else {
                    var matchDouble = v.match(/^\$?-?[,0-9]+(\.?[0-9]+)?[KMB]?$/);
                    if (!matchDouble) {
                        valid = false;
                    }
                }
            });
        }
        return valid;
    },

    // Generates result after editing
    createFinalResult: function() {
        var result;
        var values = [];
        values[0] = this.values[0].value;
        values[1] = this.values[1].value;

        if (values[0] && values[1]) {
            result = values.join(this.currentSeparator);
        } else if ((this.currentSeparator == "more than ") || (this.currentSeparator == "less than ")) {
            result = this.currentSeparator + values[1];
        } else if (this.currentSeparator == " and more") {
            result = values[1] + this.currentSeparator;
        } else {
            result = values.join("");
        }
        if ((this.currentSeparator == this.defaultSeparator || this.currentSeparator.trim() == '-') && (values[0] != values[1])) {
            var prefix = "";
            var suffix = "";
            if (values[0] && values[1]){
                if (!(this.checkboxes[0].checked && this.checkboxes[1].checked)) {
                    if (this.checkboxes[0].checked) {
                        prefix = "[";
                    } else {
                        prefix = "(";
                    }
                    if (this.checkboxes[1].checked) {
                        suffix = "]";
                    } else {
                        suffix = ")";
                    }
                }
            } else {
                if (!this.equals) {
                    if (this.moreThan) {
                        if (this.checkboxes[1].checked) {
                            prefix = ">=";
                        } else {
                            prefix = ">";
                        }
                    } else {
                        if (this.range && values[0]) {
                            if (this.checkboxes[0].checked) {
                                prefix = ">=";
                            } else {
                                prefix = ">";
                            }
                        } else {
                            if (this.checkboxes[1].checked) {
                                prefix = "<=";
                            } else {
                                prefix = "<";
                            }
                        }
                    }
                }
            }
            result = prefix + result + suffix;
        }
        return result;
    },

    splitValue: function(value, separator) {
        return value.split(separator);
    },

    destroy: function($super) {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
        this.close();
        $super();
    },

    documentClickHandler: function(e) {
        var element = Event.element(e);
        if (!this.is(element) && element != "") {
            this.close();
        }
    },

    is: function($super, element) {
        if ($super(element)) {
            return true;
        } else {
            do {
                if (element == this.rangePanel) {
                    return true;
                }
            } while (element = element.parentNode);
        }
        return false;
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                this.finishEdit();
                break;
            default:
                break;
        }
    },

    keyPressed: function(event) {
        if (event.charCode == 0) {
            return true;
        }
        var code = event.charCode == undefined ? event.keyCode : event.charCode;
        return /^[\-$.,0-9KMB]$/.test(String.fromCharCode(code));
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["range"] = NumberRangeEditor;
}