/**
 * Range editor.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Anastasia Abramova
 */
var NumberRangeEditor = Class.create(BaseTextEditor, {

    rangePanel: null,

    // td with checkBoxes and input text elements
    tdCheckboxes: null,
    tdValues: null,

    // checkBoxes and input text elements
    checkboxes: null,
    values: null,

    // buttons
    btns: null,

    // separators
    generalSeparator: " .. ",
    stableSeparators: ["more than ", "less than ", " and more", " - "],
    unstableSeparators: [" ... ", ";", "+"],
    dashSeparator: "-",
    currentSeparator: null,

    destroyed: null,
    entryEditor: null,
    
    equals: false,
    moreThan: false,
    range: false,

    editor_initialize: function(param) {
        var self = this;
        this.createInput();

        // Creating containing DIV
        this.rangePanel = new Element("div");

        // Creating information DIV
        var infoContainer = new Element("div");
        var table = new Element('table', {'class':'hide-on-screen'} );
        table.setAttribute("style", "padding-top: 5px;");
        
        self.tdCheckboxes = new Array(2);
        for (var i = 0; i < self.tdCheckboxes.length; i++) {
            self.tdCheckboxes[i] = new Element("td");
            self.tdCheckboxes[i].setAttribute("align", "center");
            self.tdCheckboxes[i].setAttribute("valign", "middle");
        }
        table.appendChild(this.tdCheckboxes[0]);
        
        self.tdValues = new Array(2);
        for (var i = 0; i < self.tdValues.length; i++) {
            self.tdValues[i] = new Element("td");
            self.tdValues[i].setAttribute("align", "center");
            self.tdValues[i].setAttribute("valign", "middle");
        }

        table.appendChild(this.tdValues[0]);

        var buttnons = new Element("td")
                .update('<input type="button" id="btnMore" value=">"/> <br/> <input type="button" id="btnLess" value="<"/> <br/> <input type="button" id="btnRange" value="-"/> <br/> <input type="button" id="btnEquals" value="="/>');
        table.appendChild(buttnons);

        table.appendChild(this.tdValues[1]);
        table.appendChild(this.tdCheckboxes[1]);
        
        infoContainer.appendChild(table);
        self.rangePanel.appendChild(infoContainer);

        self.checkboxes = new Array();
        self.values = new Array();
        self.btns = new Array(4);
        self.values[0] = self.tdValues[0];
        self.values[0].value = "";

        self.btns[0] = buttnons.down();

        for (var i = 1; i < self.btns.length; i++) {
            self.btns[i] = self.btns[i - 1].next().next();
        }

        self.values[1] = self.tdValues[1];
        self.values[1].value = "";

        for (var i = 0; i < self.btns.length; i++) {
            self.btns[i].setAttribute("style", "width: 20px; height: 20px;");
            self.btns[i].onclick = function() {
                self.currentSeparator = self.generalSeparator;
                self.changeSign(this.id);
            }
          }

        // Creating result DIV
        var resultContainer = new Element("div");
        resultContainer.id = "range";
        this.rangePanel.appendChild(resultContainer);

        // Creating button Done DIV
        var buttonContainer = new Element("div");
        buttonContainer.innerHTML = '<br/> <input type="button" id="btnDone" value="Done">'
        var b1 = buttonContainer.down().next();
        b1.onclick = function() {
            self.finishEdit();
        }
        this.rangePanel.appendChild(buttonContainer);

        if (param) {
            this.entryEditor = param.entryEditor;
        };

        this.input.onclick = function(event) {
            self.open();
        };

        this.input.onkeydown = function(event) {
            self.open();
            return false;
        };

        this.input.oncontextmenu = function(event) {
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

    createInput: function() {
        this.input = new Element("textarea");
        this.setDefaultStyle();
        this.input.setStyle(this.style);
    },

    open: function() {
        this.rangePanel.setAttribute("style", "width: 230px; height: 170px; background: white; border:1px solid gray;");
        this.rangePanel.setAttribute("align", "center");
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
                    self.disableInputsChecks(2);
                    self.values[0].value = values[0];
                    self.values[1].value = values[1];
                    self.checkboxes[0].setAttribute("checked", "checked");
                    self.checkboxes[1].setAttribute("checked", "checked");
                } else {
                    if (separator != "less than ") {
                        self.moreThan = "true";
                        self.disableInputsChecks(0);
                    } else {
                        self.disableInputsChecks(1);
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
                self.currentSeparator = self.generalSeparator;
                values = self.splitValue(value, separator);
                if (separator == ";") {
                    self.disableInputsChecks(2);
                    if (values[0].charAt(0) == "[") {
                        self.checkboxes[0].setAttribute("checked", "checked");
                    }
                    if (values[1].charAt(values[1].length - 1) == "]") {
                        self.checkboxes[1].setAttribute("checked", "checked");
                    }
                    self.values[0].value = values[0].substring(1);
                    self.values[1].value = values[1].substring(0, values[1].length - 1);
                } else if (separator == " ... ") {
                    self.disableInputsChecks(2);
                    self.values[0].value = values[0];
                    self.values[1].value = values[1];
                    self.checkboxes[0].setAttribute("checked", "checked");
                    self.checkboxes[1].setAttribute("checked", "checked");
                } else {
                    self.disableInputsChecks(0);
                    self.moreThan = "true";
                    self.values[1].value = values[0];
                    self.checkboxes[1].setAttribute("checked", "checked");
                }
            }
        });
        if ((value.indexOf(self.generalSeparator) != -1) || (value.charAt(0) == "<")||(value.charAt(0) == ">")) {
            self.currentSeparator = self.generalSeparator;
            if ((value.charAt(0) == "<")||(value.charAt(0) == ">")) {
                if (value.charAt(0) == "<") {
                    self.disableInputsChecks(1);
                    if (value.charAt(1) == "=") {
                        self.checkboxes[1].setAttribute("checked", "checked");
                        self.values[1].value = value.substring(2);
                    } else {
                        self.values[1].value = value.substring(1);
                    }
                } else {
                    self.disableInputsChecks(0);
                    self.moreThan = "true";
                    if (value.charAt(1) == "=") {
                        self.values[1].value = value.substring(2);
                    } else {
                        self.values[1].value = value.substring(1);
                    }
                }
            } else {
                self.disableInputsChecks(2);
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
                }
                self.values[0].value = values[0];
                self.values[1].value = values[1];
            }
        }
        if (!self.currentSeparator) {
            self.currentSeparator = self.dashSeparator;
            values = self.splitValue(value, self.currentSeparator);
            if (values[0] && values[1]) {
                self.disableInputsChecks(2);
                self.values[0].value = values[0];
                self.values[1].value = values[1];
                self.checkboxes[0].setAttribute("checked", "checked");
                self.checkboxes[1].setAttribute("checked", "checked");
            } else if (values[0] || values[1]) {
                self.currentSeparator = self.generalSeparator;
                self.disableInputsChecks(3);
                if (values[0]) {
                    self.values[1].value = values[0];
                } else {
                    self.values[1].value = -values[1];
                }
                self.equals = true;
            } else {
                self.currentSeparator = self.generalSeparator;
                self.values[0] = "";
                self.values[1] = "";
                self.disableInputsChecks(2);
                self.checkboxes[0].setAttribute("checked", "checked");
                self.checkboxes[1].setAttribute("checked", "checked");
                document.getElementById("btnDone").setAttribute("disabled", "disabled");
            }
        }
        self.changeRange();
        Event.observe(document, 'click', self.documentClickListener);
        self.values[1].focus();
    },

    changeRange: function() {
        var content = "";
        if (this.currentSeparator == "more than " || this.currentSeparator == "less than ") {
            if (this.values[1].value) {
                content = this.currentSeparator + this.values[1].value;
            }
        } else if (this.currentSeparator == " and more") {
            if (this.values[1].value) {
                content = this.values[1].value + this.currentSeparator;
            }
        } else if (this.values[0].value || this.range) {
            if (this.checkboxes[0].checked) {
                content = "[";
            } else {
                content = "(";
            }
            content = content + this.values[0].value + "-" + this.values[1].value;
            if (this.checkboxes[1].checked) {
                content = content + "]";
            } else {
                content = content + ")";
            }
        } else if (this.values[1].value) {
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
        document.getElementById("range").innerHTML = '<br/>' + content;
        if (content == "") {
            document.getElementById("btnDone").setAttribute("disabled", "disabled");
        } else {
            document.getElementById("btnDone").removeAttribute("disabled");
        }
    },

    changeSign: function(btnId) {
        var self = this;
        if (btnId == "btnRange") {
            if (self.checkboxes[1]) {
                self.checkboxes[1].removeAttribute("disabled");
            }
            self.disableInputsChecks(2);
        } else if (btnId == "btnEquals") {
            self.disableInputsChecks(3);
        } else if (btnId == "btnMore") {
            self.disableInputsChecks(0);
            self.checkboxes[1].removeAttribute("disabled");
        } else {
            self.disableInputsChecks(1);
            self.checkboxes[1].removeAttribute("disabled");
        }

        if (btnId == "btnMore") {
            self.moreThan = true;
        } else {
            self.moreThan = false;
        }

        if (btnId != "btnRange") {
            self.values[0].value = "";
            self.range = false;
        } else {
            self.range = true;
        }

        if (btnId == "btnEquals") {
            self.equals = true;
        } else {
            self.equals = false;
        }
        self.changeRange();
     },

     disableInputsChecks: function(btnId) {
         var self = this;
         if (btnId == 2) {
             self.tdCheckboxes[0].update('Include <br/> <input type="checkbox"/>');
             self.tdCheckboxes[0].setAttribute("style", "width: 40px;");
             self.tdValues[0].update('From <br/> <input type="text" style="width: 40px"/>');
             self.values[0] = self.tdValues[0].down().next();
             self.checkboxes[0] = self.tdCheckboxes[0].down().next();
             self.tdValues[1].update('To <br/> <input type="text" style="width: 40px"/>');
             self.checkboxes[0].onclick = function() {
                 self.currentSeparator = self.generalSeparator;
                 self.changeRange();
             }
             self.values[0].onkeyup = function() {
                 self.changeRange();
             }
         } else if (btnId == 3) {
             self.tdCheckboxes[0].update('');
             self.tdCheckboxes[0].setAttribute("style", "width: 40px;");
             self.tdValues[0].update('');
             self.tdValues[0].setAttribute("style", "width: 40px;");
             self.tdValues[1].update('<br/> <input type="text" style="width: 40px"/>');
             self.tdCheckboxes[1].update('');
             self.tdCheckboxes[1].setAttribute("style", "width: 40px;");
             if (self.checkboxes[1]) {
                 self.checkboxes[1].removeAttribute("checked");
             }
         } else {
             if (btnId == 0) {
                 self.tdValues[1].update('From <br/> <input type="text" style="width: 40px"/>');
             } else {
                 self.tdValues[1].update('To <br/> <input type="text" style="width: 40px"/>');
             }
             self.tdCheckboxes[0].update('<span style="font-size:20px"> &infin; </span>');
             self.tdCheckboxes[0].setAttribute("style", "width: 80px;");
             self.tdValues[0].update('');
             self.tdValues[0].setAttribute("style", "width: 2px;");
         }

         if (btnId != 3) {
             self.tdCheckboxes[1].update('Include <br/> <input type="checkbox"/>');
             self.tdCheckboxes[1].setAttribute("style", "width: 40px;");
             if (self.checkboxes[1]) {
                 if (self.checkboxes[1].checked) {
                     self.tdCheckboxes[1].down().next().setAttribute("checked", "checked");
                 }
             }
             self.checkboxes[1] = self.tdCheckboxes[1].down().next();
             self.checkboxes[1].onclick = function() {
                 self.currentSeparator = self.generalSeparator;
                 self.changeRange();
             }
         }

         if (btnId == 0) {
             if (self.values[0].value) {
                 self.tdValues[1].down().next().value = self.values[0].value;
             } else if (self.values[1].value) {
                 self.tdValues[1].down().next().value = self.values[1].value;
             }
         } else {
             if (self.values[1].value) {
                 self.tdValues[1].down().next().value = self.values[1].value;
             }
         }

         for (var i = 0; i < self.btns.length; i++) {
             if (i != btnId) {
                 self.btns[i].setAttribute("style", "color: black; width: 20px; height: 20px;");
             } else {
                 self.btns[i].setAttribute("style", "color: red; width: 20px; height: 20px;");
             }
         }

         self.values[1] = self.tdValues[1].down().next();
         self.values[1].onkeyup = function() {
             self.changeRange();
         }
     },

    close: function() {
        if (!this.destroyed) {
            Event.stopObserving(document, 'click', this.documentClickListener);
            Element.remove(this.rangePanel);
            this.destroyed = true;
        }
    },

    finishEdit: function() {
        if (this.isValid(this.values[0].value) && this.isValid(this.values[1].value)) {
            this.setValue(this.combineValue());
            this.handleF3();
            this.destroy();
        }
    },

    isValid: function(value) {
        var valid = true;
        var self = this;
        if ((value.charAt(0) == "<")||(value.charAt(0) == ">")) {
            value = value.substring(1);
            if (value.charAt(0) == "=") {
                value = value.substring(1);
            }
        }
        if ((value.charAt(0) == "(")||(value.charAt(0) == "[")) {
            value = value.substring(1, value.length - 1);
        }
        if (self.entryEditor && value) {
            var values = value.split(self.currentSeparator);
            if ((values.length == 3) && (self.currentSeparator == self.dashSeparator) && (value.charAt(0) == "-")) {
                values.splice(1,1);
            }
            if (!values[0]) {
                values.splice(0,1);
            }
            if (!values[1]) {
                values.splice(1,1);
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

    combineValue: function() {
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
        if ((this.currentSeparator == this.generalSeparator) && (values[0] != values[1])) {
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
                        if (this.checkboxes[1].checked) {
                            prefix = "<=";
                        } else {
                            prefix = "<";
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

    destroy: function() {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
        this.close();
    },

    documentClickHandler: function(e) {
        var element = Event.element(e);
        var abort = false;
        if (!this.is(element)) {
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
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["range"] = NumberRangeEditor;
}