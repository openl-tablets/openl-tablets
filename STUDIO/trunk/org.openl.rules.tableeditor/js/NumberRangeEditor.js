/**
 * Range editor.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Anastasia Abramova
 */
var NumberRangeEditor = Class.create(BaseTextEditor, {

    rangePanel: null,
    resultContainer: null,
    btnDone: null,

    // TD with checkBoxes and input text elements
    tdCheckboxes: null,
    tdValues: null,

    // CheckBoxes and input text elements
    checkboxes: null,
    values: null,

    // Buttons
    btns: null,

    // Default separator; <, <=, >, >= also concern to default separators
    defaultSeparator: " .. ",
    // Separator changed to default separator only if user change separator's logic
    stableSeparators: ["more than ", "less than ", " and more", " - "],
    // Separator usually changed to default separator
    unstableSeparators: [" ... ", ";", "+"],
    // Concern to stableSeparators
    dashSeparator: "-",
    currentSeparator: null,

    destroyed: null,
    entryEditor: null,

    equals: false,
    moreThan: false,
    range: false,
    onBlur: null,

    editor_initialize: function(param) {
        var self = this;
        self.createInput();

        // Creating containing DIV
        self.rangePanel = new Element("div");
        self.rangePanel.className = "range";
        self.rangePanel.setAttribute("align", "center");

        var table = new Element("table");
        
        // Creating variables with checkbox
        self.tdCheckboxes = new Array(2);
        self.checkboxes = new Array(2);
        for (var i = 0; i < self.tdCheckboxes.length; i++) {
            self.tdCheckboxes[i] = new Element("td");
            self.tdCheckboxes[i].setAttribute("align", "center");
            self.tdCheckboxes[i].setAttribute("valign", "middle");
        }
        table.appendChild(this.tdCheckboxes[0]);

        // Creating variables with inputText
        self.tdValues = new Array(2);
        self.values = new Array(2);
        for (var i = 0; i < self.tdValues.length; i++) {
            self.tdValues[i] = new Element("td");
            self.tdValues[i].setAttribute("align", "center");
            self.tdValues[i].setAttribute("valign", "middle");
            self.values[i] = new Element("input");
            self.values[i].value = "";
        }
        table.appendChild(this.tdValues[0]);

        // Creating td with buttons
        var buttnons = new Element("td")
                .update('<input type="button" title="More than" value=">"/> <br/> <input type="button" title="Less than" value="<"/> <br/> <input type="button" title="Range" value="-"/> <br/> <input type="button" title="Equals" value="="/>');
        table.appendChild(buttnons);
        table.appendChild(this.tdValues[1]);
        table.appendChild(this.tdCheckboxes[1]);
        self.rangePanel.appendChild(table);
        
        // Creating variables with buttons
        self.btns = new Array(4);
        self.btns[0] = buttnons.down();
        for (var i = 1; i < self.btns.length; i++) {
            self.btns[i] = self.btns[i - 1].next().next();
        }
        for (var i = 0; i < self.btns.length; i++) {
            self.btns[i].className = "btnRange";
            self.btns[i].onclick = function() {
                self.currentSeparator = self.defaultSeparator;
                self.createIntervalBorders(self.btns.indexOf(this));
                self.createResult();
            }
          }

        // Creating result DIV
        self.resultContainer = new Element("div");
        this.rangePanel.appendChild(self.resultContainer);

        // Creating button Done
        var buttonContainer = new Element("div");
        buttonContainer.innerHTML = '<br/> <input type="button" value="Done">'
        self.btnDone = buttonContainer.down().next();
        self.btnDone.className = "btnDone";
        self.btnDone.onclick = function() {
            self.finishEdit();
        }
        this.rangePanel.appendChild(buttonContainer);

        if (param) {
            this.entryEditor = param.entryEditor;
        }

        this.input.onclick = function(event) {
            self.open();
        }

        this.input.onkeydown = function(event) {
            self.open();
            return false;
        }

        this.input.oncontextmenu = function(event) {
            return false;
        }

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
        var value = this.input.value;
        var self = this;
        var values;
        self.stableSeparators.each(function(separator) {
            if (value.indexOf(separator) !== -1) {
                self.currentSeparator = separator;
                values = self.splitValue(value, separator);
                if (separator == " - ") {
                    // Separator == " - "
                    self.createIntervalBorders(2);
                    self.values[0].value = values[0];
                    self.values[1].value = values[1];
                    self.checkboxes[0].setAttribute("checked", "checked");
                    self.checkboxes[1].setAttribute("checked", "checked");
                } else {
                    // Separator == "less than " || "more than " || " and more"
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
        })
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
            self.currentSeparator = self.dashSeparator;
            values = self.splitValue(value, self.currentSeparator);
            if (values[0] && values[1]) {
                // Separator = "-"
                self.createIntervalBorders(2);
                self.values[0].value = values[0];
                self.values[1].value = values[1];
                self.checkboxes[0].setAttribute("checked", "checked");
                self.checkboxes[1].setAttribute("checked", "checked");
            } else if (!values[0] && !values[1] || values[0] && !self.isValid(values[0])){
                // Empty cell || invalid character in the cell
                if (values[0]) {
                    self.input.value = null;
                }
                self.currentSeparator = self.defaultSeparator;
                self.values[0].value = "";
                self.values[1].value = "";
                self.createIntervalBorders(2);
                self.checkboxes[0].setAttribute("checked", "checked");
                self.checkboxes[1].setAttribute("checked", "checked");
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
        } else if (this.currentSeparator == " - " || this.currentSeparator == "-") {
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
            content = content + this.values[1].value
        }
        this.resultContainer.innerHTML = '<br/>' + content;
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
            self.tdCheckboxes[0].update('Include <br/> <input type="checkbox"/>');
            self.tdCheckboxes[0].className = "tdStandard";
            self.tdValues[0].update('From <br/> <input type="text"/>');
            var minValue = self.values[0].value;
            self.values[0] = self.tdValues[0].down().next();
            self.values[0].className = "tdStandard";
            self.values[0].value = minValue;
            var checked = false;
            if (self.checkboxes[0]) {
                checked = self.checkboxes[0].checked;
            }
            self.checkboxes[0] = self.tdCheckboxes[0].down().next();
            if (checked) {
                self.checkboxes[0].setAttribute("checked", "checked");
            }
            self.tdValues[1].update('To <br/> <input type="text"/>');
            self.checkboxes[0].onclick = function() {
                self.currentSeparator = self.defaultSeparator;
                self.createResult();
            }
            self.values[0].onkeypress = function(event) {return self.keyPressed(event || window.event)};
            self.values[0].onkeyup = function() {self.createResult()};
        } else if (btnId == 3) {
            self.values[0].value = "";
            self.tdCheckboxes[0].update('');
            self.tdCheckboxes[0].className = "tdStandard";
            self.tdValues[0].update('');
            self.tdValues[0].className = "tdStandard";
            self.tdValues[1].update('<br/> <input type="text"/>');
            self.tdValues[1].className = "tdStandard";
            self.tdCheckboxes[1].update('');
            self.tdCheckboxes[1].className = "tdStandard";
        } else {
            if (btnId == 0) {
                self.tdValues[1].update('From <br/> <input type="text"/>');
            } else {
                self.tdValues[1].update('To <br/> <input type="text"/>');
            }
            self.tdCheckboxes[0].update('<span> &infin; </span>');
            self.tdCheckboxes[0].className = "tdWide";
            self.tdValues[0].update('');
            self.tdValues[0].className = "tdNarrow";
            self.values[0].value = "";
        }

        self.equals = true;
        if (btnId != 3) {
            self.equals = false;
            self.tdCheckboxes[1].update('Include <br/> <input type="checkbox"/>');
            self.tdCheckboxes[1].className = "tdStandard";
            if (self.checkboxes[1] && self.checkboxes[1].checked) {
                self.tdCheckboxes[1].down().next().setAttribute("checked", "checked");
            }
            self.checkboxes[1] = self.tdCheckboxes[1].down().next();
            self.checkboxes[1].onclick = function() {
                self.currentSeparator = self.defaultSeparator;
                self.createResult();
            }
        }

        if (btnId == 0) {
            self.moreThan = true;
            if (self.values[0].value) {
                self.tdValues[1].down().next().value = self.values[0].value;
            } else if (self.values[1].value) {
                self.tdValues[1].down().next().value = self.values[1].value;
            }
        } else {
            self.moreThan = false;
            if (self.values[1].value) {
                self.tdValues[1].down().next().value = self.values[1].value;
            }
        }

        for (var i = 0; i < self.btns.length; i++) {
            if (i != btnId) {
                self.btns[i].setAttribute("style", "color: black;");
            } else {
                self.btns[i].setAttribute("style", "color: red;");
            }
        }

        self.values[1] = self.tdValues[1].down().next();
        self.values[1].className = "tdStandard";
        self.values[1].onkeypress = function(event) {
            return self.keyPressed(event || window.event)
        }
        self.values[1].onkeyup = function() {
            self.createResult()
        }
    },

    close: function() {
        if (!this.destroyed) {
            ///Event.stopObserving(document, 'click', this.documentClickListener);
            Element.remove(this.rangePanel);
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
                if (self.entryEditor == "integer") {
                    var matchInt = v.match(/^-?[0-9]+$/);
                    if (!matchInt) {
                        valid = false;
                    }
                } else {
                    var matchDouble = v.match(/^-?[0-9]+(\.?[0-9]+)?$/);
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
        var values = new Array();
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
        if ((this.currentSeparator == this.defaultSeparator) && (values[0] != values[1])) {
            var prefix = "";
            var suffix = "";
            if (values[0] && values[1]){
                var leftBorder;
                var rightBorder;
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
        var abort = false;
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
        var oneLetterWidth = 8.5;
        var minCharacters = 4;
        var length;
        if (this.values[0].value.length > this.values[1].value.length) {
            length = this.values[0].value.length;
        } else {
            length = this.values[1].value.length;
        }
        if (length > minCharacters) {
            this.values[0].style.width = oneLetterWidth * length + "px";
            this.values[1].style.width = oneLetterWidth * length + "px";
        } else {
            this.values[0].className = "tdStandard";
            this.values[1].className = "tdStandard";
        }
        if (event.charCode == 0) {
            return true;
        }
        var code = event.charCode == undefined ? event.keyCode : event.charCode;
        // Minus or point
        if ((code == 45) || (code == 46)) {
            return true;
        }
        // Digits (0-9)
        return code >= 48 && code <= 57;
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["range"] = NumberRangeEditor;
}