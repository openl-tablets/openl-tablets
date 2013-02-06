var NumberRangeEditor = Class.create(BaseTextEditor, {

    rangePanel: null,
    separatorDefault: null,
    separatorNew: " .. ",
    destroyed: null,
    entryEditor: null,
    checkboxes: null,
    values: null,
    btns: null,
    newFormat: false,

    editor_initialize: function(param) {
        var self = this;
        this.createInput();

        // Creating containing DIV
        this.rangePanel = new Element("div");

        // Creating information DIV
        var infoContainer = new Element("div");
        var table = new Element('table', { 'class':'hide-on-screen'} );
        var check0 = new Element("td").update('Include <br/> <input type="checkbox"/>');
        table.appendChild(check0);
        var minValue = new Element("td").update('From <br/> <input type="text" style="width: 40px"/>');
        table.appendChild(minValue);
        var buttnons = new Element("td").update('<input type="button" id="btnMore" value=">"/> <br/> <input type="button" id="btnLess" value="<"/> <br/> <input type="button" id="btnRange" value="-"/>');
        table.appendChild(buttnons);
        var maxValue = new Element("td").update('To <br/> <input type="text" style="width: 40px"/>');
        table.appendChild(maxValue);
        var check1 = new Element("td").update('Include <br/> <input type="checkbox"/>')
        table.appendChild(check1);
        infoContainer.appendChild(table);
        this.rangePanel.appendChild(infoContainer);
        
        self.checkboxes = new Array();
        self.values = new Array();
        self.btns = new Array();
        self.checkboxes[0] = check0.down().next();
        self.values[0] = minValue.down().next();
        self.btns[0] = buttnons.down();
        self.btns[1] = self.btns[0].next().next();
        self.btns[2] = self.btns[1].next().next();
        self.values[1] = maxValue.down().next();
        self.checkboxes[1] = check1.down().next();
        
        for (var i = 0; i < self.checkboxes.length; i++) {
            self.checkboxes[i].onclick = function() {
                self.newFormat = true;
                self.changeRange();
            }
          }
        
        for (i = 0; i < self.values.length; i++) {
            self.values[i].onkeyup = function() {
                self.changeRange();
            }
          }
        
        for (var i = 0; i < self.btns.length; i++) {
            self.btns[i].setAttribute("style", "width: 20px; height: 20px;");
            self.btns[i].onclick = function() {
                self.changeSign(this.id);
            }
          }
        
     // Creating result DIV
        var resultContainer = new Element("div");
        resultContainer.id = "range";
        this.rangePanel.appendChild(resultContainer);
        
     // Creating buttons DIV
        var buttonContainer = new Element("div");
        buttonContainer.innerHTML = '<br/> <input type="button" value="Done">'
        var b1 = buttonContainer.down().next();
        b1.onclick = function() {
            self.finishEdit();
        }
        this.rangePanel.appendChild(buttonContainer);
        
        if (param) {
            this.entryEditor = param.entryEditor;
            this.separatorDefault = param.separator;
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

        this.documentClickListener = this.documentClickHandler.bindAsEventListener(this);
        
        this.destroyed = true;
    },
    
    open: function() {
        this.rangePanel.setAttribute("style", "width: 220px; height: 140px; background: white; border:1px solid gray;");
        this.rangePanel.setAttribute("align", "center");
        this.input.up().appendChild(this.rangePanel);
        
        this.destroyed = false;
        
        value = this.input.value;
        if ((value.charAt(0) == "<")||(value.charAt(0) == ">")) {
            this.newFormat = true;
            if (value.charAt(0) == "<") {
                if (value.charAt(1) == "=") {
                    this.checkboxes[1].setAttribute("checked", "checked");
                    this.values[1].value = value.substring(2);
                } else {
                    this.values[1].value = value.substring(1);
                }
            } else {
                if (value.charAt(1) == "=") {
                    this.checkboxes[0].setAttribute("checked", "checked");
                    this.values[0].value = value.substring(2);
                } else {
                    this.values[0].value = value.substring(1);
                }
            }
        } else {
            var separator;
            var values;
            if ((value.charAt(0) == "(")||(value.charAt(0) == "[")) {
                this.newFormat = true;
                if (value.charAt(0) == "[") {
                    this.checkboxes[0].setAttribute("checked", "checked");
                }
                if (value.charAt(value.length) == "]") {
                    this.checkboxes[1].setAttribute("checked", "checked");
                }
                separator = this.separatorNew;
                values = this.splitValue(value.substring(1, value.length - 1), separator);
            } else {
                separator = this.separatorDefault;
                values = this.splitValue(value, separator);
                if (values[0]) {
                    this.checkboxes[0].setAttribute("checked", "checked");
                    if (values[1]) {
                        this.checkboxes[1].setAttribute("checked", "checked");
                    }
                }
            }
            if (values.length > 0) {
                this.values[0].value = values[0];
                if (values.length > 1) {
                    this.values[1].value = values[1];
                }
            }
        }
        
        this.changeRange();
        
        Event.observe(document, 'click', this.documentClickListener);
    },
    
    changeRange: function() {
        var content;
        if (this.values[0].value) {
            if (this.values[1].value) {
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
            } else {
                if (this.checkboxes[0].checked) {
                    content = ">="; 
                } else {
                    content = ">";
                }
                content = content + this.values[0].value;
            }
        } else {
            if (this.values[1].value) {
                if (this.checkboxes[1].checked) {
                    content = "<=";
                } else {
                    content = "<";
                }
                content = content + this.values[1].value;
            } else {
                content = "";
            }
        }
        document.getElementById("range").innerHTML = '<br/>' + content;
    },
    
    changeSign: function(btnId) {
        if (btnId != "btnMore") {
            this.checkboxes[1].removeAttribute("disabled");
            this.values[1].removeAttribute("disabled");
            if (btnId == "btnLess") {
                this.values[0].value = "";
            }
        } else {
            this.checkboxes[1].setAttribute("disabled","disabled");
            this.values[1].setAttribute("disabled","disabled");
        } 
        
        if (btnId != "btnLess") {
            this.checkboxes[0].removeAttribute("disabled");
            this.values[0].removeAttribute("disabled");
            if (btnId == "btnMore") {
                this.values[1].value = "";
            }
        } else {
            this.checkboxes[0].setAttribute("disabled","disabled");
            this.values[0].setAttribute("disabled","disabled");
        }
        
        for (var i = 0; i < this.btns.length; i++) {
            if (this.btns[i].id != btnId) {
                this.btns[i].setAttribute("style", "color: black; width: 20px; height: 20px;");
            } else {
                this.btns[i].setAttribute("style", "color: red; width: 20px; height: 20px;");
            }
          }
        
        this.changeRange();
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
        var separator = this.separatorDefault;
        if ((value.charAt(0) == "(")||(value.charAt(0) == "[")) {
            separator = this.separatorNew;
            value = value.substring(1, value.length - 1);
        } else if ((value.charAt(0) == "<")||(value.charAt(0) == ">")) {
            value = value.substring(1);
            if (value.charAt(0) == "=") {
                value = value.substring(1);
            }
        }
        if (self.entryEditor && value) {
            var values = value.split(separator);
            values.each(function(v) {
                var n = Number(v);
                if (isNaN(n)) {
                    valid = false;
                }
            });
        }
        return valid;
    },
    
    combineValue: function() {
        var separator;
        var result;
        if (this.checkboxes[0].checked && this.checkboxes[1].checked && !this.newFormat) {
            separator = this.separatorDefault;
        } else {
            separator = this.separatorNew;
        }
        var values = new Array();
        values[0] = this.values[0].value;
        values[1] = this.values[1].value;
        
        if ((values[0])&&(values[1])) {
            result = values.join(separator);
        } else {
            result = values.join("");
        }
        
        if (separator == this.separatorNew) {
            var prefix = "";
            var suffix = "";
            if (this.values[0].value && this.values[1].value){
                var leftBorder;
                var rightBorder;
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
            } else {
                if (this.values[0].value) {
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
            result = prefix + result + suffix;
        }
        return result;
    },
    
    splitValue: function(value, separator) {
        return value.split(separator);
    },

    destroy: function() {
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
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["range"] = NumberRangeEditor;
}