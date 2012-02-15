/**
 * Table editor.
 *
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */

var TableEditor = Class.create({

    Modes: {
        VIEW: 0,
        EDIT: 1
    },

    editorId: -1,
    mode: null,
    editable: null,
    tableContainer: null,
    currentElement: null,
    editorName: null,
    editor: null,
    editorSwitched: false,
    baseUrl: null,
    selectionPos: null,
    selectionHistory: [],
    decorator: null,
    rows: 0,
    columns: 0,
    modFuncSuccess: null,
    editCell: null,
    cellIdPrefix: null,
    actions: null,
    editorWrapper: null,
    switchEditorMenu: null,
    toolbar: null,

    fillColorPicker: null,
    fontColorPicker: null,

    // Constructor
    initialize: function(editorId, url, editCell, actions, mode, editable) {
        this.mode = mode ? mode : this.Modes.VIEW;
        this.editorId = editorId;
        this.cellIdPrefix = this.editorId + "_cell-";
        this.tableContainer = $(editorId + "_table");
        this.actions = actions;

        this.editable = editable === false ? false : true;

        // Suppress text selection BEGIN
        this.tableContainer.onselectstart = function() { // IE
            return false;
        }
        this.tableContainer.onmousedown = function() { // Mozilla
            return false;
        }
        // Suppress text selection END

        if (editCell) this.editCell = editCell;

        this.baseUrl = url;

        var self = this;

        Event.stopObserving(document, "click");
        Event.stopObserving(document, "keydown");
        Event.stopObserving(document, "keypress");
        this.tableContainer.stopObserving("dblclick");

        if (this.mode == this.Modes.EDIT) {
            this.initEditMode();
        }

        this.tableContainer.observe("dblclick", function(e) {
            self.handleDoubleClick(e);
        });
    },

    toEditMode: function(cellToEdit) {
        var self = this;

        if (!cellToEdit) {
            cellToEdit = $(PopupMenu.lastTarget);
        }

        var cellPos;
        if (cellToEdit) {
            cellPos = cellToEdit.id.split(this.cellIdPrefix)[1];
        }

        new Ajax.Request(this.buildUrl(TableEditor.Operations.EDIT), {
            method: "get",
            encoding: "utf-8",
            contentType: "text/javascript",
            parameters: {
                cell: cellPos,
                editorId: self.editorId
            },
            onSuccess: function(data) {
                $(self.editorId).innerHTML = data.responseText.stripScripts();
                new ScriptLoader().evalScripts(data.responseText);

                self.mode = self.Modes.EDIT;
                self.initEditMode();

                self.editCell = cellPos;
                self.startEditing();
            },
            onFailure: self.handleError
        });
    },

    initEditMode: function() {
        var self = this;

        initToolbar(self.editorId);
        self.toolbar = $(self.editorId).down(".te_toolbar");
        self.editorWrapper = $(self.editorId + "_editorWrapper");

        this.decorator = new Decorator('te_selected');

        // Handle Table Editor events START
        Event.observe(document, "click", function(e) {
            self.handleClick(e);
        });

        Event.observe(document, "keydown", function(e) {
            self.handleKeyDown(e);
        });

        Event.observe(document, "keypress", function(e) {
            self.handleKeyPress(e);
        });
        // Handle Table Editor events END

        this.modFuncSuccess = function(response) {
            response = eval(response.responseText);
            if (response.status) alert(response.status);
            else {
                if (response.response) {
                    this.renderTable(response.response);
                    this.selectElement();
                }
                this.processCallbacks(response, "do");
            }
        }.bindAsEventListener(this);
    },

    /**
     * Handles response error.
     */
    handleError: function(response, errorMessage) {
        if (response.status == 399) { // Redirect
            var redirectPage = response.getResponseHeader("Location");
            if (redirectPage) {
                top.location.href = redirectPage;
            } else {
                alert(response.statusText);
            }
        } else {
            if (!errorMessage) {
                errorMessage = "Error: " + response.status + " - " + response.statusText;
            }
            alert(errorMessage);
        }
    },

    startEditing: function() {
        if (this.editCell && this.editCell.indexOf(":") > 0) {
            var cellPos = this.editCell.split(":");
            var cell = this.$cell(cellPos);
            if (cell) this.editBeginRequest(cell, null, true);
        }
    },

    renderTable: function(data) {
        this.tableContainer.innerHTML = data.stripScripts();
        new ScriptLoader().evalScripts(data);
        var table = $(this.tableContainer.childNodes[0]);

        this.computeTableInfo(table);
    },

    /**
     * Computes table width in rows, and height in columns (that is sum of all rowSpans in a column
     * and sum of all colSpans in a row).
     */
    computeTableInfo: function(table) {
        this.rows = 0;
        this.columns = 0;

        var row = table.down("tr");

        if (row) {
            var tdElt = row.down("td");
            while (tdElt) {
                this.columns += tdElt.colSpan ? tdElt.colSpan : 1;
                tdElt = tdElt.next("td");
            }
        }

        while (row) {
            var tdElt = row.down("td");
            if (tdElt) {
                this.rows += tdElt.rowSpan ? tdElt.rowSpan : 1;
            }
            row = row.next("tr");
        }
    },

    /**
     * Makes all changes persistant. Sends corresponding request to the server.
     */
    save: function() {
        var beforeSavePassed = true;
        if (this.actions && this.actions.beforeSave) {
            beforeSavePassed = this.actions.beforeSave();
        }
        if (beforeSavePassed == false) return;

        if (!Validation.isAllValidated()) { // Validation failed
            if (this.actions && this.actions.saveFailure) {
                this.actions.saveFailure();
            }
            alert('There are validation errors.');
            return;
        }

        var self = this;
        new Ajax.Request(this.buildUrl(TableEditor.Operations.SAVE), {
            parameters: {
                editorId: this.editorId
            },
            onSuccess: function(response) {
                response = eval(response.responseText);
                if (response.status) { // Error
                    alert(response.status);
                    if (self.actions && self.actions.saveFailure) {
                        self.actions.saveFailure();
                    }
                } else {
                    self.processCallbacks(response, "do");
                    if (self.actions && self.actions.afterSave) {
                        self.actions.afterSave({"newUri": response.response});
                    }
                }
            },
            onFailure: function(response) {
                self.handleError(response,
                        "Server failed to save your changes");

                if (self.actions && self.actions.saveFailure) {
                    self.actions.saveFailure();
                }

            }
        });
    },

    /**
     * Handles mouse click on the table.
     */
    handleClick: function(e) {
        var elt = Event.element(e);
        // Click on editor
        if (this.editor && this.editor.is(elt)) {
            return;
        }

        if (this.switchEditorMenu) {
            try {
                if (this.switchEditorMenu.has(elt)) {
                    return;
                }
            } finally {
                this.switchEditorMenu.hide();
                this.switchEditorMenu = null;
            }
        }

        this.setCellValue();
        if (this.isCell(elt)) {
            this.selectElement(elt);
        } else if (this.isToolbar(elt)) {
            // Do Nothing
        } else {
            this.tableBlur();
        }
    },

    /**
     * Handles mouse double click on table.
     */
    handleDoubleClick: function(event) {
        var cell = Event.findElement(event, "td");
        if (cell) {
            switch (this.mode) {
                case this.Modes.EDIT: {
                    // Save value of current editor and close it
                    this.setCellValue();
                    this.editBeginRequest(cell);
                    Event.stop(event);
                    break;
                }

                case this.Modes.VIEW:
                default: {
                    if (this.editable) {
                        this.toEditMode(cell);
                    }
                    break;
                }
            }
        }
    },

    handleKeyPress: function(event) {
        if (!this.isCell(this.currentElement)) {
            return;
        }

        if (this.editor) {
            switch (event.keyCode) {
                case 27: this.editor.cancelEdit(); break;
                case 13: if (this.editor.__do_nothing_on_enter !== true) {
                    this.setCellValue(
                            HTMLHelper.unescapeHTML(this.currentElement.innerHTML.replace(/<br>/ig, "\n")).strip());
                }
                break;
            }
            return;
        }

        if (event.keyCode == 13) {
            if (this.hasSelection()) this.editBeginRequest(this.currentElement);
            return;
        }

        if (this.hasSelection()) {
            if ([event.ctrlKey, event.altKey, event.shiftKey, event.metaKey].any()) return;
            if (event.charCode != undefined) { // FF
                if (event.charCode == 0) return true;
            } else if (event.keyCode < 32 || TableEditor.isNavigationKey(event.keyCode)) return true;

            this.editBeginRequest(this.currentElement, event.charCode || event.keyCode);
        }
    },

    /**
     * Handles key presses. Performs table navigation.
     */
    handleKeyDown: function(event) {
        if (!this.isCell(this.currentElement)) {
            return;
        }

        if (this.editor) {
            switch (event.keyCode) {
                case 113: this.editor.handleF2(event); break;
                case 114: this.editor.handleF3(event); break;
            }
            return;
        }

        if (!TableEditor.isNavigationKey(event.keyCode)) return;

        if (!this.hasSelection()) {
            this.selectionPos = [1, 1];
            this.selectElement();
            return;
        }

        var sp = this.selectionPos.clone();

        // Check history
        if (this.selectionHistory.length > 0 && this.selectionHistory.last()[0] == event.keyCode) {
            this.selectElement(null, -1);
            return;
        }

        var scanUpLeft = function(index, noRestore) {
            var tmp = sp[index];
            while (sp[index] >= 1 && !this.$cell(sp)) --sp[index];
            var res = this.$cell(sp);
            if (!noRestore) sp[index] = tmp;
            return res;
        }

        switch (event.keyCode) {
            case 37: case 38: // LEFT, UP
            var cell = null;
            var theIndex = event.keyCode == 38 ? 0 : 1;
            while (--sp[theIndex] >= 1) {
                cell = scanUpLeft.call(this, 1 - theIndex, true);
                if (cell) {
                    if ((sp[0] + cell.rowSpan >= this.selectionPos[0] + theIndex) &&
                        (sp[1] + cell.colSpan >= this.selectionPos[1] + 1 - theIndex))
                        break;
                }
                sp[1 - theIndex] = this.selectionPos[1 - theIndex];
            }
            if (cell) this.selectElement(cell, event.keyCode + 2);
            break;

            case 39: case 40:  //RIGHT, DOWN
            var theIndex = event.keyCode == 40 ? 0 : 1;

            sp[theIndex] += this.currentElement[["rowSpan", "colSpan"][theIndex]];
            if (sp[theIndex] > this[["rows", "columns"][theIndex]]) break;
            var newCell = scanUpLeft.call(this, 1 - theIndex);
            if (newCell) this.selectElement(newCell, event.keyCode - 2);
            break;
        }
    },

    /**
     * Sends request to server to find out required editor for a cell.
     * After getting response calls this.editBegin.
     */
    editBeginRequest : function(cell, keyCode, ignoreAjaxRequestCount) {
        if (!ignoreAjaxRequestCount && Ajax.activeRequestCount > 0) return;
        var self = this;

        this.selectElement(cell);

        var typedText = undefined;
        if (keyCode) typedText = String.fromCharCode(keyCode);

        new Ajax.Request(this.buildUrl(TableEditor.Operations.GET_CELL_EDITOR), {
            onSuccess  : function(response) {
                self.editBegin(cell, eval(response.responseText), typedText);
            },
            parameters : {
                editorId: this.editorId,
                row : self.selectionPos[0],
                col : self.selectionPos[1]
            },
            onFailure: self.handleError
        });
    },

    /**
     *  Create and activate new editor.
     */
    editBegin : function(cell, response, initialValue) {
        if (!initialValue) {
            if (response.initValue) {
                initialValue = response.initValue;
            } else {
                // Get initial value from table cell
                initialValue = HTMLHelper.unescapeHTML(
                        cell.innerHTML.replace(/<br>/ig, "\n")).strip();
            }
        }

        var editorStyle = this.getCellEditorStyle(cell);
        this.showEditorWrapper(cell);

        this.showCellEditor(response.editor, this.editorWrapper, initialValue, response.params, editorStyle);

        this.editCell = cell;
        this.selectElement(cell);
    },

    showCellEditor: function(editorName, editorWrapper, initialValue, params, style) {
        var self = this;

        if (editorName == 'array') {
            var entryEditorName = params.entryEditor;
            if (entryEditorName) {
                params.entryEditor = new TableEditor.Editors[entryEditorName]('', '', params);
            }
        }

        this.editor = new TableEditor.Editors[editorName](
                this, editorWrapper.id, params, initialValue, true, style);
        this.editorName = editorName;

        // Increase height of multiline editor
        if (editorName == 'multiline') {
        	var input = this.editor.getInputElement();
            var inputHeight = input.getHeight();
            input.style.height = (inputHeight + 20) + 'px';
        }

        var availableEditors = this.getAvailableEditors(editorName);
        if (availableEditors.size() > 0) {
            var switchEditorMenuHandler = function(e) {
                // Uncomment after migration on Prototype 1.7
                // Prototype 1.6.1 has issue with Chrome browser
                // if (Event.isRightClick(e)) {
                if (HTMLHelper.isRightClick(e)) {
                    if (self.switchEditorMenu) {
                        return true;
                    }
                    self.switchEditorMenu = self.createSwitchEditorMenu(availableEditors);
                    // Disable browser context menu
                    e.target.oncontextmenu = function() { return false; };
                    self.switchEditorMenu.left = e.clientX + 2;
                    self.switchEditorMenu.top = e.clientY;
                    self.switchEditorMenu.show();
                }
            };
            this.editor.bind("mousedown", switchEditorMenuHandler);
        }
    },

    createSwitchEditorMenu: function(availableEditors) {
        var self = this;

        var content = new Element("div");
        var header = new Element("div");
        header.className = "te_menu_header";
        header.innerHTML = "Switch to:";
        content.appendChild(header);

        availableEditors.each(function(e) {
            var editorItem = new Element("div");
            editorItem.className = "te_menu_item";
            var editorLink = new Element("a");
            editorLink.observe('click', function() {
                self.switchEditor(e.key);
            });
            editorLink.innerHTML = e.value;
            editorItem.appendChild(editorLink);
            ['mouseover', 'mouseout'].each(function(event) {
                editorItem.observe(event, function() {
                    this.toggleClassName('te_menu_item_hover');
                });
            });
            content.appendChild(editorItem);
        });

        return new Popup(content);
    },

    getAvailableEditors: function(editorName) {
        var availableEditors = $H();
        if (editorName != 'formula' && editorName != 'text' && editorName != 'multiline') {
            availableEditors.set('formula', 'Formula Editor');
        }
        if (editorName == 'multiline') {
            availableEditors.set('text', 'Text Editor');
        }
        if (editorName == 'text') {
            availableEditors.set('multiline', 'Multiline Editor');
        }
        return availableEditors;
    },

    switchEditor: function(editorName, params) {
        var prevEditor = this.editor;

        var editorWrapper = prevEditor.parentElement;
        var initialValue = prevEditor.isCancelled() ? prevEditor.initialValue : prevEditor.getValue();
        var style = prevEditor.style;

        this.showCellEditor(editorName, editorWrapper, initialValue, params, style);

        prevEditor.isCancelled = function () { return true; };
        prevEditor.destroy();
        
        this.editorSwitched = true;
    },

    showEditorWrapper: function(cell) {
        this.editorWrapper.style.width = cell.offsetWidth - 2 + "px";
        this.editorWrapper.style.height = cell.offsetHeight - 2 + "px";
        var pos = Element.positionedOffset(cell);
        this.editorWrapper.style.left = pos[0] + "px";
        this.editorWrapper.style.top = pos[1] + "px";
        this.editorWrapper.show();
    },

    getCellEditorStyle: function(cell) {
        if (cell) {
            var style = {
                fontFamily: cell.style.fontFamily,
                fontSize: cell.style.fontSize,
                fontWeight: cell.style.fontWeight,
                fontStyle: cell.style.fontStyle,

                textAlign: cell.style.textAlign,

                padding: '1px',
                boxShadow: '0 0 1px orange'
            }

            return style;
        }
    },

    setCellValue: function(prevValue) {
        if (this.editor) {
            if (prevValue) {
                this.editor.initialValue = prevValue;
            }
            if (!this.editor.isCancelled()) {
                var val = this.editor.getValue();
                var self = this;
                new Ajax.Request(this.buildUrl(TableEditor.Operations.SET_CELL_VALUE), {
                    method    : "get",
                    encoding   : "utf-8",
                    contentType : "text/javascript",
                    onSuccess  : this.modFuncSuccess,
                    parameters: {
                        editorId: this.editorId,
                        row : self.selectionPos[0],
                        col : self.selectionPos[1],
                        value: val,
                        editor: this.editorSwitched ? this.editorName : ''
                    },
                    onFailure: function(response) {
                        self.handleError(response,
                                "Error during setting the value.");
                    }
                });
            }
            this.editor.destroy();
            this.editorWrapper.hide();
            this.editor = null;
            this.editorName = null;
            this.editorSwitched = false;
        }
    },

    buildUrl: function(action, paramString) {
        var url = this.baseUrl + action;
        if (paramString)
            url = url + "?" + paramString;
        return url
    },

    /**
     * Makes a cell 'selected', that is sets up this.selectionPos and this.currentElement, and also applies
     * visual decoration to the cell.
     * If elt is null(undefined) than this.currentElement is set based on value of this.selectionPos array.
     * dir param is used to track selections history, if it is not given history is cleared, if it is set to -1 and
     * elt param is not given the new selection is taken from history.
     */
    selectElement: function(elt, dir) {
        if (elt && this.currentElement && elt.id == this.currentElement.id) return;

        var wasSelected = this.hasSelection();

        if (elt && dir) { // save to selection history
            if (this.selectionPos) this.selectionHistory.push([dir, this.selectionPos[0], this.selectionPos[1]]);
            if (this.selectionHistory.length > 10) this.selectionHistory.shift();
        } else {
            if (dir == -1) {
                var lastEntry = this.selectionHistory.pop();
                this.selectionPos[0] = lastEntry[1];
                this.selectionPos[1] = lastEntry[2];
            } else
                this.selectionHistory.clear();
        }

        if (elt) {
            var newSelectionPos = this.elementPosition(elt);
            if (!newSelectionPos) return;
            this.selectionPos = newSelectionPos;
        } else if (this.selectionPos) {
            elt = this.$cell(this.selectionPos);
        }
        this.decorator.undecorate(this.currentElement);
        this.decorator.decorate(this.currentElement = elt);
        if (!wasSelected != !this.hasSelection())
        	this.isSelectedUpdated(!wasSelected);
    },

    tableBlur: function() {
        if (this.currentElement) {
            this.decorator.undecorate(this.currentElement);
            this.currentElement = null;
            this.selectionPos = null;
            this.isSelectedUpdated(false);
        }
    },

    $cell: function(pos) {
        var cell = $(this.cellIdPrefix + pos[0] + ":" + pos[1]);
        if (!cell) return cell;
        if (!cell.rowSpan) cell.rowSpan = 1;
        if (!cell.colSpan) cell.colSpan = 1;
        return cell;
    },

    isCell: function(element) {
        if (element && element.id.indexOf(this.cellIdPrefix) >= 0) {
            return true;
        }
        return false;
    },

    isToolbar: function(element) {
        if (!element) return false;

        var toolbar = element.up(".te_toolbar");
        var picker;

        if (!toolbar) {
            do {
                if (element.nodeName == "DIV"
                        && element.hasClassName("cp_palette")) {
                    picker = element;
                    break;
                }
            } while (element = element.parentNode);
        }

        return (toolbar && toolbar.up().id.indexOf(this.editorId) >= 0)
            || (picker && picker.up().id.indexOf("_color_colorPicker") >= 0);
    },

    undoredo: function(redo) {
        if (Ajax.activeRequestCount > 0) return;
        new Ajax.Request(this.buildUrl((redo ? TableEditor.Operations.REDO : TableEditor.Operations.UNDO)), {
            parameters: {
                editorId: this.editorId
            },
            onSuccess: this.modFuncSuccess,
            onFailure: this.handleError
        })
    },

    /**
     * Inspect element id and extracts its position in table. Element is expected to be a TD.
     */
    elementPosition: function(e) {
        var id = $(e).id;
        var pos = id.lastIndexOf("-");
        if (pos < 0) return null;
        var splitted = id.substr(pos + 1).split(":", 2);
        splitted[0] = parseInt(splitted[0]);
        splitted[1] = parseInt(splitted[1]);
        return splitted;
    },

    setAlignment: function(_align) {
        if (!this.hasSelection()) {
            alert("Nothing is selected");
            return;
        }

        var cell = this.currentElement;
        var self = this;
        var params = {
            editorId: this.editorId,    
            row : this.selectionPos[0],
            col : this.selectionPos[1],
            align: _align
        }
        new Ajax.Request(this.buildUrl(TableEditor.Operations.SET_ALIGN), {
            onSuccess: function(response) {
                response = eval(response.responseText);
                if (response.status)
                    alert(response.status);
                else {
                    self.processCallbacks(response, "do");
                    if (self.editor) {
                        self.editor.input.style.textAlign = _align;
                    }
                    cell.style.textAlign = _align;
                }
            },
            parameters: params,
            onFailure: self.handleError
        });
    },

    selectFillColor: function(actionElemId) {
        var self = this;

        this.currentFillColor = this.currentElement.style.backgroundColor;

        if (!this.fillColorPicker) { // Lazy initialization

            this.fillColorPicker = new ColorPicker(
                actionElemId,
                self.toolbar,
                function(color) {
                    self.setColor(color, TableEditor.Operations.SET_FILL_COLOR);
                },
                { // Optional params
	                showOn: false,
	                onMouseOver: function () {
	                    self.decorator.undecorate(self.currentElement);
	            	},
	            	onColorMouseOver: function(color) {
	            	    self.currentElement.style.backgroundColor = color;
	                },
	                onMouseOut: function () {
	                    self.currentElement.style.backgroundColor = self.currentFillColor;
	                    self.decorator.decorate(self.currentElement);
	                }
	            }
            );
        }
        this.fillColorPicker.show();
    },

    selectFontColor: function(actionElemId) {
        var self = this;

        this.currentFontColor = this.currentElement.style.color;

        if (!this.fontColorPicker) { // Lazy initialization

            this.fontColorPicker = new ColorPicker(
                actionElemId,
                self.toolbar,
                function(color) {
                    self.setColor(color, TableEditor.Operations.SET_FONT_COLOR);
                },
                { // Optional params
                    showOn: false,
                    onMouseOver: function () {
                        self.decorator.undecorate(self.currentElement);
                    },
                    onColorMouseOver: function(color) {
                        self.currentElement.style.color = color;
                    },
                    onMouseOut: function () {
                        self.currentElement.style.color = self.currentFontColor;
                        self.decorator.decorate(self.currentElement);
                    }
                }
            );
        }
        this.fontColorPicker.show();
    },

    setColor: function(_color, operation) {
        if (!this.hasSelection()) {
            alert("Nothing is selected");
            return;
        }

        var cell = this.currentElement;
        var self = this;

        var params = {
            editorId: this.editorId,    
            row : this.selectionPos[0],
            col : this.selectionPos[1],
            color: _color
        }

        new Ajax.Request(this.buildUrl(operation), {
            onSuccess: function(response) {
                response = eval(response.responseText);
                if (response.status)
                    alert(response.status);
                else {
                    self.processCallbacks(response, "do");
                }
            },
            parameters: params,
            onFailure: self.handleError
        });
    },

    indent: function(_indent) {
        if (!this.hasSelection()) {
            alert("Nothing is selected");
            return;
        }

        var cell = this.currentElement;
        var self = this;
        var params = {
            editorId: this.editorId,    
            row : this.selectionPos[0],
            col : this.selectionPos[1],
            indent: _indent
        }
        new Ajax.Request(this.buildUrl(TableEditor.Operations.SET_INDENT), {
            onSuccess: function(response) {
                response = eval(response.responseText);
                if (response.status)
                    alert(response.status)
                else {
                    self.processCallbacks(response, "do");
                    resultPadding = 0;
                    if (cell.style.paddingLeft.indexOf("em") > 0) {
                        resultPadding = parseFloat(cell.style.paddingLeft);
                    } else if (cell.style.paddingLeft.indexOf("px") > 0) {
                        resultPadding = parseFloat(cell.style.paddingLeft) * 0.063;
                    }
                    resultPadding = resultPadding + parseInt(_indent);
                    if (resultPadding >= 0) {
                        cell.style.paddingLeft = resultPadding + "em";
                    }
                }
            },
            parameters: params,
            onFailure: self.handleError
        });
    },

    setFontBold: function() {
        if (!this.hasSelection()) {
            alert("Nothing is selected");
            return;
        }

        var self = this;

        var cell = this.currentElement;
        var _bold = cell.style.fontWeight == "bold";

        var params = {
            editorId: this.editorId,    
            row: this.selectionPos[0],
            col: this.selectionPos[1],
            bold: !_bold
        }
        new Ajax.Request(this.buildUrl(TableEditor.Operations.SET_FONT_BOLD), {
            onSuccess: function(response) {
                response = eval(response.responseText);
                if (response.status)
                    alert(response.status)
                else {
                    self.processCallbacks(response, "do");
                    cell.style.fontWeight = _bold ? "normal" : "bold";
                }
            },
            parameters: params,
            onFailure: self.handleError
        });
    },

    setFontItalic: function() {
        if (!this.hasSelection()) {
            alert("Nothing is selected");
            return;
        }

        var self = this;

        var cell = this.currentElement;
        var _italic = cell.style.fontStyle == "italic";

        var params = {
            editorId: this.editorId,    
            row: this.selectionPos[0],
            col: this.selectionPos[1],
            italic: !_italic
        }
        new Ajax.Request(this.buildUrl(TableEditor.Operations.SET_FONT_ITALIC), {
            onSuccess: function(response) {
                response = eval(response.responseText);
                if (response.status)
                    alert(response.status)
                else {
                    self.processCallbacks(response, "do");
                    cell.style.fontStyle = _italic ? "normal" : "italic";
                }
            },
            parameters: params,
            onFailure: self.handleError
        });
    },

    setFontUnderline: function() {
        if (!this.hasSelection()) {
            alert("Nothing is selected");
            return;
        }

        var self = this;

        var cell = this.currentElement;
        var _underline = cell.style.textDecoration == "underline";

        var params = {
            editorId: this.editorId,    
            row: this.selectionPos[0],
            col: this.selectionPos[1],
            underline: !_underline
        }
        new Ajax.Request(this.buildUrl(TableEditor.Operations.SET_FONT_UNDERLINE), {
            onSuccess: function(response) {
                response = eval(response.responseText);
                if (response.status)
                    alert(response.status)
                else {
                    self.processCallbacks(response, "do");
                    cell.style.textDecoration = _underline ? "none" : "underline";
                }
            },
            parameters: params,
            onFailure: self.handleError
        });
    },

    doTableOperation: function(operation) {
        if (!this.hasSelection()) {
            alert("Nothing is selected");
            return;
        }
        var params = {
            editorId: this.editorId,
            row: this.selectionPos[0],
            col: this.selectionPos[1]
        }
        new Ajax.Request(this.buildUrl(operation), {
            onSuccess: this.modFuncSuccess,
            parameters: params,
            onFailure: this.handleError
        });
    },

    hasSelection : function() {
        return this.selectionPos && this.currentElement;
    },

    processCallbacks: function(obj, action) {
        function isBoolean(t) {return obj.hasUndo === true || obj.hasUndo === false}
        try {
            switch (action) {
                case "do":
                    if (obj) {
                        if (isBoolean(obj.hasUndo)) try {this.undoStateUpdated(obj.hasUndo)} catch (e) {}
                        this.redoStateUpdated(obj.hasRedo)
                    }
                    break;
                case "selection":
                    if (isBoolean(obj)) this.isSelectedUpdated(obj);
                    break;

            }
        } catch(ex) {}
    },
    // Callback functions
    undoStateUpdated : Prototype.emptyFunction,
    redoStateUpdated : Prototype.emptyFunction,
    isSelectedUpdated : Prototype.emptyFunction
});

/**
 *  Here is editors registry.
 *  The editors would add themselves to this hash with the name as a key.
 */
TableEditor.Editors = $H();

TableEditor.Operations = {
    EDIT : "edit",
    GET_CELL_EDITOR : "getCellEditor",
    GET_CELL_VALUE : "getCellValue",
    SET_CELL_VALUE : "setCellValue",
    SET_CELL_FORMULA : "setCellFormula",
    SET_ALIGN : "setAlign",
    SET_FILL_COLOR : "setFillColor",
    SET_FONT_COLOR : "setFontColor",
    SET_FONT_BOLD : "setFontBold",
    SET_FONT_ITALIC : "setFontItalic",
    SET_FONT_UNDERLINE : "setFontUnderline",
    SET_INDENT : "setIndent",
    REMOVE_ROW : "removeRow",
    REMOVE_COLUMN : "removeColumn",
    INSERT_ROW_BEFORE : "insertRowBefore",
    INSERT_COLUMN_BEFORE : "insertColumnBefore",
    UNDO : "undo",
    REDO : "redo",
    SAVE : "saveTable"
};

// Standalone functions

TableEditor.isNavigationKey = function (keyCode) {return  keyCode >= 37 && keyCode <= 41}

/**
 *  Responsible for visual display of 'selected' element.
 */
var Decorator = Class.create({

    // Empty constructor
    initialize : function(selectStyleClass) {
        this.selectStyleClass = selectStyleClass;
    },

    /**
     * Changes elememnt style, so it looks 'selected'.
     */
    decorate: function(/* Element */ elt) {
        if (elt) {
            elt.addClassName(this.selectStyleClass);
        }
    },

    /**
     * Reverts 'selection' of last decorated element.
     */
    undecorate: function(/* Element */ elt) {
        if (elt) {
            elt.removeClassName(this.selectStyleClass);
        }
    }
});


//TableEditor Menu

// @Deprecated
function openMenu(menuId, td, event) {
    if (HTMLHelper.isRightClick(event)) {
        td.oncontextmenu = function() { return false; };
        PopupMenu.sheduleShowMenu(menuId, event, 150);
    }
}

// @Deprecated
function closeMenu(td) {
    PopupMenu.cancelShowMenu();
}


// TableEditor Toolbar

// @Deprecated
var save_item = "_save_all";
var undo_item = "_undo";
var redo_item = "_redo";
var indent_items = ["_decrease_indent", "_increase_indent"];
var align_items = ["_align_left", "_align_center", "_align_right"];
var addremove_items = ["_insert_row_before", "_remove_row", "_insert_column_before", "_remove_column"];
var font_items = ["_font_bold", "_font_italic", "_font_underline"];
var color_items = ["_fill_color", "_font_color"];
var other_items = ["_help"];

var itemClass = "te_toolbar_item";
var disabledClass = "te_toolbar_item_disabled";
var overClass = "te_toolbar_item_over";

// @Deprecated
function initTableEditor(editorId, url, cellToEdit, actions, mode, editable) {
    var tableEditor = new TableEditor(editorId, url, cellToEdit, actions, mode, editable);

    tableEditor.undoStateUpdated = function(hasItems) {
        [save_item, undo_item].each(function(item) {
            processItem(getItemId(editorId, item), hasItems);
        });
        if (hasItems) {
            window.onbeforeunload = function() {
                return "Your changes have not been saved.";
            };
        } else { // remove handler if Save/Undo items are disabled
            window.onbeforeunload = function() {};
        }
    };

    tableEditor.redoStateUpdated = function(hasItems) {
        processItem(getItemId(editorId, redo_item), hasItems);
    };

    tableEditor.isSelectedUpdated = function(selected) {
        [indent_items, align_items, font_items, color_items,
            addremove_items, other_items].flatten().each(function(item) {
            processItem(getItemId(editorId, item), selected);
        });
    };

    tableEditor.startEditing();

    return tableEditor;
}

// @Deprecated
function initToolbar(editorId) {
    $$("." + itemClass).each(function(item) {
        processItem(item, false);
        item.onmouseover = function() {
            this.addClassName(overClass);
        };
        item.onmouseout = function() {
            this.removeClassName(overClass);
        };
    });
}

// @Deprecated
function processItem(item, enable) {
    if (enable) {
        enableToolbarItem(item);
    } else {
        disableToolbarItem(item);
    }
}

// @Deprecated
function getItemId(editorId, itemId) {
    if (editorId && itemId) {
        return editorId + itemId;
    }
}

// @Deprecated
function enableToolbarItem(img) {
    if (!isToolbarItemDisabled(img = $(img))) return;
    img.removeClassName(disabledClass);

    if (img._mouseover) img.onmouseover = img._mouseover;
    if (img._mouseout) img.onmouseout = img._mouseout;
    if (img._onclick) img.onclick = img._onclick;
    img._onmouseover = img._onmouseout = img._onclick = '';
}

// @Deprecated
function disableToolbarItem(img) {
    if (isToolbarItemDisabled(img = $(img))) return;
    img.addClassName(disabledClass);

    img._mouseover = img.onmouseover;
    img._mouseout = img.onmouseout;
    img._onclick = img.onclick;
    img.onmouseover = img.onmouseout = img.onclick = Prototype.emptyFunction;
}

// @Deprecated
function isToolbarItemDisabled(img) {
    return img.hasClassName(disabledClass)
        && img._onclick;
}
