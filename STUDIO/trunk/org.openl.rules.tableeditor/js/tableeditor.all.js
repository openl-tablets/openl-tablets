/**
 * Tooltip.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Andrei Astrouski
 */
var Tooltip = Class.create({

    firedTooltips: $H(),

    initialize: function(id, content, params) {
        var self = this;

        this.element = $(id);
        this.content = content;
        this.params = params;

        if (params && params.showOn) {
            this.showOn = params.showOn instanceof Array ? params.showOn : [params.showOn];
        } else {
            this.showOn = ["mouseover"]; // by default
        }

        if (params && params.hideOn) {
            this.hideOn = params.hideOn instanceof Array ? params.hideOn : [params.hideOn];
        } else {
            this.hideOn = ["mouseout"]; // by default
        }

        this.showOn.each(function(e) {
            self.showHandler = self.show.bindAsEventListener(self);
            Event.observe(self.element, e, self.showHandler);
        });

        this.hideOn.each(function(e) {
            self.hideHandler = self.hide.bindAsEventListener(self);
            Event.observe(self.element, e, self.hideHandler);
        });
    },

    show: function() {
        var tooltip = this.createTooltip();
        if (!this.firedTooltips.get(this.element.id)) {
            // Show tooltip
            document.body.appendChild(tooltip);
            this.firedTooltips.set(this.element.id, tooltip);

            var position = (this.params && this.params.position) ? this.params.position : 'top_right';
            tooltip.addClassName('tooltip_' + position);

            this.applyStylesToPointer(tooltip);

            var pos = this.calculateInitPosition(tooltip, position);
            tooltip.style.left = pos[0] + "px";
            tooltip.style.top = pos[1] + "px";
        }
    },

    calculateInitPosition: function(tooltip, position) {
        var initPos = Element.viewportOffset(this.element);

        switch (position) {
            case 'top_right':
                initPos[0] += (this.element.getWidth() - 25);
                initPos[1] -= (this.element.getHeight() + tooltip.getHeight() - 4);
                break;
            case 'top_center':
                break;
            case 'top_left':
            	initPos[0] -= (tooltip.getWidth() - 25);
                initPos[1] -= (this.element.getHeight() + tooltip.getHeight() - 4);
                break;
            case 'right_bottom':
                initPos[0] += (this.element.getWidth() + 10);
                initPos[1] -= 4;
                break;
            case 'right_center':
                break;
            case 'right_bottom':
                break;
            case 'bottom_right':
                break;
            case 'bottom_center':
                break;
            case 'bottom_left':
                break;
            case 'left_top':
                break;
            case 'left_center':
                break;
            case 'left_bottom':
                break;
        }

        return initPos;
    },

    applyStylesToPointer: function(tooltip) {
        var pointer = tooltip.down('div.tooltip_pointer_body');
        if (pointer) {
            // Set pointer background
            var tooltipBackground = tooltip.getStyle('backgroundColor');
            pointer.setStyle({borderTopColor: tooltipBackground});
        }
    },

    createTooltip: function() {
        var tooltipDiv = new Element("div");

        tooltipDiv.id = this.element.id + "_tooltip";
        tooltipDiv.update(this.content);

        var skin = this.params && this.params.skin ? this.params.skin : 'default';
        var skinClass = "tooltip_skin-" + skin;

        var pointer = this.params && this.params.pointer == false ? false : true;
        if (pointer) {
            var tooltipPointerDiv = new Element("div");
            tooltipPointerDiv.addClassName('tooltip_pointer')
            tooltipPointerDiv.addClassName(skinClass);
            var tooltipPointerBodyDiv = new Element("div");

            tooltipPointerBodyDiv.addClassName('tooltip_pointer_body');
            tooltipPointerDiv.appendChild(tooltipPointerBodyDiv);
            tooltipDiv.appendChild(tooltipPointerDiv);
        }

        if (this.params) {
            if (this.params.width) {
                tooltipDiv.style.width = this.params.width;
            } else if (this.params.maxWidth) {
                tooltipDiv.style.maxWidth = this.params.maxWidth;
            } else {
                tooltipDiv.style.maxWidth = "140px"; // by default
            }
        }

        tooltipDiv.addClassName("tooltip");
        tooltipDiv.addClassName(skinClass);
        tooltipDiv.addClassName("corner_all");
        tooltipDiv.addClassName("shadow_all");

        return tooltipDiv;
    },

    hide: function() {
        var currentTooltip = this.firedTooltips.get(this.element.id);
        if (currentTooltip) {
            this.firedTooltips.unset(this.element.id);
            document.body.removeChild(currentTooltip);
        }
    }

});
/**
 * Loads and evaluates both internal (with body) and external javascripts.
 * In Prototype 1.6.1 script tags referencing external files are ignored.
 *
 * @requires Prototype 1.6.1 javascript library
 *
 * @author Andrei Astrouski
 */
var ScriptLoader = Class.create({

    initialize: function() {
	},

    /**
     * Extracts script elements from the html string.
     *
     * @param html the html string
     * @return array of script elements
     */
    extractScripts: function(html) {
        if (html) {
            html = html.toString();
            var matchScripts = new RegExp(Prototype.ScriptFragment, 'img');
            var scripts = html.match(matchScripts) || [];
            var div = new Element('div');
            (div = $(div)).innerHTML =
                ',' + scripts.join(','); // hack for IE
            return div.select('script');
        }
    },

    /**
     * Evaluates all scripts from the html string.
     *
     * @param html the html string
     */
    evalScripts: function(html) {
        //NOTE: IE evaluates scripts in random order (especially the v.6)
        this.extractScripts(html).each(this.evalScript);
    },
    
    /**
     * Evaluates a script in the global context.
     *
     * @param script the script element
     */
    evalScript: function(script) {
        if (script) {
            var head = $$("head")[0];
            var newScript = new Element("script");
            newScript.type = "text/javascript";
            if (scriptSrc = script.src) {
                newScript.src = scriptSrc;
            } else if (scriptBody = (script.innerHTML || script.text)) {
                if (Prototype.Browser.IE) {
                    newScript.text = scriptBody;
                } else {
                    newScript.appendChild(document.createTextNode(scriptBody));
                }
                /* // Another way to eval script body
                if (window.execScript) { // IE
                    window.execScript(scriptBody);
                } else {
                    if (window.eval) {
                        window.eval(scriptBody);
                    }
                    //setTimeout(scriptBody, 0);
                }
                */
            }
            setTimeout(function() {
                head.appendChild(newScript);
            }, 10);
        }
    }

});
/**
 * Table editor.
 *
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 * @author Andrei Astrouski
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
    editCell: null,
    cellIdPrefix: null,
    actions: null,
    editorWrapper: null,
    switchEditorMenu: null,
    toolbar: null,

    fillColorPicker: null,
    fontColorPicker: null,
    hasChanges: false,

    // Constructor
    initialize: function(editorId, url, editCell, actions, mode, editable) {
        this.mode = mode || this.Modes.VIEW;
        this.editorId = editorId;
        this.cellIdPrefix = this.editorId + "_cell-";
        this.tableContainer = $(editorId + "_table");
        this.actions = actions;

        this.editable = editable !== false;

        // Suppress text selection BEGIN
        this.tableContainer.onselectstart = function() { // IE
            return false;
        };
        this.tableContainer.onmousedown = function() { // Mozilla
            return false;
        };
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
        if (!cellToEdit) {
            cellToEdit = $(PopupMenu.lastTarget);
        }

        var cellPos;
        if (cellToEdit) {
            cellPos = cellToEdit.id.split(this.cellIdPrefix)[1];
        }

        this.mode = this.Modes.EDIT;
        this.initEditMode();

        this.editCell = cellPos;
        this.startEditing();
    },

    initEditMode: function() {
        var self = this;

        initToolbar();
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

        self.toolbar.show();

        this.computeTableInfo();
    },

    handleResponse: function(response, callback) {
        var data = eval(response.responseText);

        if (data.message) {
            this.error(data.message);

        } else {
            if (data.html) {
                this.renderTable(data.html);
                this.selectElement();
            }

            if (data.hasUndo === true || data.hasUndo === false) {
                this.hasChanges = data.hasUndo;
                this.undoStateUpdated(data.hasUndo);
                this.redoStateUpdated(data.hasRedo);
            }

            if (callback) {
                callback(data);
            }
        }
    },

    handleError: function(response) {
        if (response.status == 399) { // Redirect
            var redirectPage = response.getResponseHeader("Location");
            if (redirectPage) {
                top.location.href = redirectPage;
                return;
            }
        }

        this.error("Sorry! Server failed to apply your changes!");
    },

    error: function(message) {
        if (this.actions && this.actions.error) {
            this.actions.error({"message": message});
        } else {
            alert(message);
        }
    },

    doOperation: function(operation, params, successCallback) {
        var self = this;

        new Ajax.Request(this.buildUrl(operation), {
            parameters: params,

            onSuccess: function(response) {
                self.handleResponse(response, successCallback);
            },

            onFailure: function(response) {
                self.handleError(response);
            }
        });
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

        this.computeTableInfo();
    },

    /**
     * Computes table width in rows, and height in columns (that is sum of all rowSpans in a column
     * and sum of all colSpans in a row).
     */
    computeTableInfo: function() {
        var table = $(this.tableContainer.childNodes[0]);

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
     * Makes all changes persistent.
     * Sends corresponding request to the server.
     */
    save: function() {
    	this.setCellValue();
        var self = this;

        var beforeSavePassed = true;
        if (this.actions && this.actions.beforeSave) {
            beforeSavePassed = this.actions.beforeSave();
        }
        if (beforeSavePassed == false) return;

        this.doOperation(TableEditor.Operations.SAVE, { editorId: this.editorId }, function(data) {
            if (self.actions && self.actions.afterSave) {
                self.actions.afterSave({"newUri": data.uri});
            }
        });
    },
    
    saveChanges: function() {
        this.setCellValue();
        var selt = this;
        
        var beforeSavePassed = true;
        if (this.actions && this.actions.beforeSave) {
            beforeSavePassed = this.actions.beforeSave();
        }
        if (beforeSavePassed == false) return;

        this.doOperation(TableEditor.Operations.SAVE, { editorId: this.editorId }, hideLoader());
     },

    /**
     * Rolls back all changes. Sends corresponding request to the server.
     */
    rollback: function() {
        this.doOperation(TableEditor.Operations.ROLLBACK, params, function(data) {
            window.onbeforeunload = Prototype.emptyFunction;
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
            this.isFormated(elt);

        } else if (this.isToolbar(elt)) {
            // Do Nothing
        } else {
            this.tableBlur();
        }
    },
    

    isFormated: function(elt) {

        var cell = this.currentElement;
        var decorator = this.decorator;
        var boldElement = $(this.editorId + "_font_bold");
        var italicElement = $(this.editorId + "_font_italic");
        var underlineElement = $(this.editorId + "_font_underline");
        var alignRightElement =  $(this.editorId + "_align_right");
        var alignCenterElement =  $(this.editorId + "_align_center");
        var alignLeftElement = $(this.editorId + "_align_left");

        function decorate(elem, decorated) {
            decorated ? decorator.decorateToolBar(elem) : decorator.undecorateToolBar(elem);
        }

        decorate(boldElement, cell.style.fontWeight == "bold");
        decorate(italicElement, cell.style.fontStyle == "italic");
        decorate(underlineElement, cell.style.textDecoration == "underline");
        decorate(alignRightElement, cell.style.textAlign == "right");
        decorate(alignCenterElement, cell.style.textAlign == "center");
        decorate(alignLeftElement, cell.style.textAlign == "left");
        decorate(alignLeftElement, cell.style.textAlign == "");
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
                            this.unescapeHTML(this.currentElement.innerHTML.replace(/<br>/ig, "\n")).strip());
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
        };

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
        if (keyCode)
            typedText = String.fromCharCode(keyCode);

        var params = {
            editorId: this.editorId,
            row: self.selectionPos[0],
            col: self.selectionPos[1]
        };

        this.doOperation(TableEditor.Operations.GET_CELL_EDITOR, params, function(data) {
            self.editBegin(cell, data, typedText);
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
                initialValue = this.unescapeHTML(
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
            this.editor.input.oncontextmenu = function(e) {
                if (!self.switchEditorMenu) {
                    self.switchEditorMenu = self.createSwitchEditorMenu(availableEditors);
                    self.switchEditorMenu.left = e.clientX + 2;
                    self.switchEditorMenu.top = e.clientY;
                    self.switchEditorMenu.show();
                    return false;
                }
            };
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
        var minWidth = 20;
        var width = cell.offsetWidth - 2;
        if (width < minWidth) {
            cell.style.minWidth = minWidth + "px";
            width = cell.offsetWidth - 2;
        }
        this.editorWrapper.style.width = width + "px";
        this.editorWrapper.style.height = cell.offsetHeight - 2 + "px";
        var pos = Element.positionedOffset(cell);
        this.editorWrapper.style.left = pos[0] + "px";
        this.editorWrapper.style.top = pos[1] + "px";
        this.editorWrapper.show();
    },

    getCellEditorStyle: function(cell) {
        if (cell) {
            return {
                fontFamily: cell.style.fontFamily,
                fontSize  : cell.style.fontSize,
                fontWeight: cell.style.fontWeight,
                fontStyle : cell.style.fontStyle,
                textAlign : cell.style.textAlign,
                padding   : '1px'
            };
        }
    },

    setCellValue: function(prevValue) {
        if (this.editor) {
            if (this.editCell && this.editCell.style) {
                this.editCell.style.minWidth = null;
            }
            if (prevValue) {
                this.editor.initialValue = prevValue;
            }
            if (!this.editor.isCancelled()) {
                var val = this.editor.getValue();
                var self = this;

                var params = {
                    editorId: this.editorId,
                    row : self.selectionPos[0],
                    col : self.selectionPos[1],
                    value: val,
                    editor: this.editorSwitched ? this.editorName : ''
                };

                this.doOperation(TableEditor.Operations.SET_CELL_VALUE, params);
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
        return element && element.id.indexOf(this.cellIdPrefix) >= 0;
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
        this.doOperation(redo ? TableEditor.Operations.REDO : TableEditor.Operations.UNDO, { editorId: this.editorId });
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

    setAlignment: function(_align, elt) {
        if (!this.checkSelection()) return;
        
        var cell = this.currentElement;
        var self = this;
        var cellStyle = cell.style.textAlign;
        var undecorateElement;
        
        if (cellStyle) {
            undecorateElement = $(this.editorId + '_align_' + cellStyle);
        } else {
            undecorateElement = $(this.editorId + '_align_' + "left");
        }

        var params = {
            editorId: this.editorId,
            row : this.selectionPos[0],
            col : this.selectionPos[1],
            align: _align
        };

        if (undecorateElement) {
            this.decorator.undecorateToolBar(undecorateElement);
        }

        this.decorator.decorateToolBar(elt);

        this.doOperation(TableEditor.Operations.SET_ALIGN, params, function(data) {
            if (self.editor) {
                self.editor.input.style.textAlign = _align;
            }
            cell.style.textAlign = _align;
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
        if (!this.checkSelection()) return;

        var params = {
            editorId: this.editorId,
            row : this.selectionPos[0],
            col : this.selectionPos[1],
            color: _color
        };

        this.doOperation(operation, params);
    },

    indent: function(_indent) {
        if (!this.checkSelection()) return;

        var cell = this.currentElement;

        var params = {
            editorId: this.editorId,
            row : this.selectionPos[0],
            col : this.selectionPos[1],
            indent: _indent
        };

        this.doOperation(TableEditor.Operations.SET_INDENT, params, function(data) {
            var resultPadding = 0;
            // TODO Refactor with css calc()
            if (cell.style.paddingLeft.indexOf("em") > 0) {
                resultPadding = parseFloat(cell.style.paddingLeft);
            } else if (cell.style.paddingLeft.indexOf("px") > 0) {
                resultPadding = parseFloat(cell.style.paddingLeft) * 0.063;
            }
            resultPadding += parseInt(_indent);
            if (resultPadding >= 0) {
                cell.style.paddingLeft = resultPadding + "em";
            }
        });
    },

    setFontBold: function(elt) {
        if (!this.checkSelection()) return;

        var cell = this.currentElement;
        var _bold = cell.style.fontWeight == "bold";
        var decorator = this.decorator;
        
        var params = {
            editorId: this.editorId,
            row: this.selectionPos[0],
            col: this.selectionPos[1],
            bold: !_bold
        };
        this.decorator.decorateToolBar(elt);
        this.doOperation(TableEditor.Operations.SET_FONT_BOLD, params, function(data) {
 
            if ( _bold) {
                cell.style.fontWeight = "normal";
                decorator.undecorateToolBar(elt);
            } else {
                cell.style.fontWeight = "bold";
            }
        });
    },

    setFontItalic: function(elt) {
        if (!this.checkSelection()) return;
        
        var cell = this.currentElement;
        var _italic = cell.style.fontStyle == "italic";
        var decorator = this.decorator;
        
        var params = {
            editorId: this.editorId,
            row: this.selectionPos[0],
            col: this.selectionPos[1],
            italic: !_italic
        };
        this.decorator.decorateToolBar(elt);
        this.doOperation(TableEditor.Operations.SET_FONT_ITALIC, params, function(data) {

            if (_italic) {
                cell.style.fontStyle = "normal";
                decorator.undecorateToolBar(elt);
            } else {
                cell.style.fontStyle = "italic";  
            }
        });
    },

    setFontUnderline: function(elt) {
        if (!this.checkSelection()) return;

        var cell = this.currentElement;
        var _underline = cell.style.textDecoration == "underline";
        var decorator = this.decorator;

        var params = {
            editorId: this.editorId,
            row: this.selectionPos[0],
            col: this.selectionPos[1],
            underline: !_underline
        };
        this.decorator.decorateToolBar(elt);
        this.doOperation(TableEditor.Operations.SET_FONT_UNDERLINE, params, function(data) {

            if (_underline) {
                cell.style.textDecoration = "none";
                decorator.undecorateToolBar(elt);
            } else {
                cell.style.textDecoration = "underline";
            }
        });
    },

    checkSelection: function() {
        
        if (!this.hasSelection()) {
            this.error("Nothing is selected");
            return false;
        } 
        return true;
    },

    doTableOperation: function(operation) {
        if (!this.checkSelection()) return;

        var params = {
            editorId: this.editorId,
            row: this.selectionPos[0],
            col: this.selectionPos[1]
        };

        this.doOperation(operation, params);
    },

    unescapeHTML: function(html) {
        return html.replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&nbsp;/g,' ');
    },

    hasSelection : function() {
        return this.selectionPos && this.currentElement;
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
    GET_CELL_EDITOR : "getCellEditor",
    GET_CELL_VALUE : "getCellValue",
    SET_CELL_VALUE : "setCellValue",
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
    SAVE : "saveTable",
    ROLLBACK : "rollbackTable"
};

// Standalone functions

TableEditor.isNavigationKey = function (keyCode) { return  keyCode >= 37 && keyCode <= 41; }

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
    },
    /**
     * Reverts 'selection' from toolBar buttons
     */
    undecorateToolBar: function (elt) {
        $(elt).removeClassName("te_toolbar_item_pressed");
    },
    
    decorateToolBar: function (elt) {
       $(elt).addClassName("te_toolbar_item_pressed"); 
    }

});


//TableEditor Menu 

// @Deprecated
function openMenu(menuId, event) {
    event.preventDefault();
    PopupMenu.sheduleShowMenu(menuId, event, 150);
}

// @Deprecated
function closeMenu() {
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
var pressedClass = "te_toolbar_item_pressed";

// @Deprecated
function initTableEditor(editorId, url, cellToEdit, actions, mode, editable) {
    var tableEditor = new TableEditor(editorId, url, cellToEdit, actions, mode, editable);

    tableEditor.undoStateUpdated = function(hasItems) {
        [save_item, undo_item].each(function(item) {
            processItem(getItemId(editorId, item), hasItems);
        });
       /* if (hasItems) {
            window.onbeforeunload = function() {
               // alert('not saved');
           
                return "Your changes have not been saved.";
            };
        } else { // remove handler if Save/Undo items are disabled
           
          //  window.onbeforeunload = function() {};
           
        }*/
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
function initToolbar() {
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
    img.removeClassName(pressedClass);

    if (img._mouseover) img.onmouseover = img._mouseover;
    if (img._mouseout) img.onmouseout = img._mouseout;
    if (img._onclick) img.onclick = img._onclick;
    img._onmouseover = img._onmouseout = img._onclick = '';
}

// @Deprecated
function disableToolbarItem(img) {
    if (isToolbarItemDisabled(img = $(img))) return;
    if (img) {
        img.addClassName(disabledClass);
        img.removeClassName(pressedClass);

        img._mouseover = img.onmouseover;
        img._mouseout = img.onmouseout;
        img._onclick = img.onclick;
        img.onmouseover = img.onmouseout = img.onclick = Prototype.emptyFunction;  
    }
}

// @Deprecated
function isToolbarItemDisabled(img) {
    if (img)
    return img.hasClassName(disabledClass)
        && img._onclick;
}


var PopupMenu = {
	showChild: function (id, show)
	{
		document.getElementById(id).style.display = show ? "inline" : "none";
	},

	menu_ie: !!(window.attachEvent && !window.opera),
	menu_ns6: document.getElementById && !document.all,
	menuON: false,
	te_menu : undefined,
	delayedFunction: undefined,
	disappearFunction: undefined,
	disappearInterval1: 5000,
	disappearInterval2: 1000,
	delayedState: {
		extraClass: undefined,
		evt: {},
		contentElement: undefined
	},
    lastTarget: null,
    

    getWindowSize: function () {
		var myWidth = 0, myHeight = 0;
		if (typeof( window.innerWidth ) == 'number') {
			//Non-IE
			myWidth = window.innerWidth;
			myHeight = window.innerHeight;
		} else if (document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight )) {
			//IE 6+ in 'standards compliant mode'
			myWidth = document.documentElement.clientWidth;
			myHeight = document.documentElement.clientHeight;
		} else if (document.body && ( document.body.clientWidth || document.body.clientHeight )) {
			//IE 4 compatible
			myWidth = document.body.clientWidth;
			myHeight = document.body.clientHeight;
		}
		return [myWidth, myHeight];
	},
	
	getScrollXY: function () {
		var scrOfX = 0, scrOfY = 0;
		if (typeof( window.pageYOffset ) == 'number') {
			//Netscape compliant
			scrOfY = window.pageYOffset;
			scrOfX = window.pageXOffset;
		} else if (document.body && ( document.body.scrollLeft || document.body.scrollTop )) {
			//DOM compliant
			scrOfY = document.body.scrollTop;
			scrOfX = document.body.scrollLeft;
		} else if (document.documentElement && ( document.documentElement.scrollLeft || document.documentElement.scrollTop )) {
			//IE6 standards compliant mode
			scrOfY = document.documentElement.scrollTop;
			scrOfX = document.documentElement.scrollLeft;
		}
		return [ scrOfX, scrOfY ];
	},

	_showPopupMenu: function (contentElement, event, extraClass) {
		this.cancelDisappear();

		var scrollXY = this.getScrollXY();
		var windowSizeXY = this.getWindowSize();

		this.te_menu.style.visibility = "hidden";
		this.te_menu.innerHTML = document.getElementById(contentElement).innerHTML;
		this.te_menu.style.display = "inline";
		var divWidth = this.te_menu.clientWidth;
		var divHeight = this.te_menu.clientHeight;

		var posX = event.clientX + 5; var delta = 25;
		if (posX + delta + divWidth > windowSizeXY[0]) posX = windowSizeXY[0] - delta - divWidth;
		if (posX < 0) posX = 0;
		var posY = event.clientY + 5; delta = 5;
		if ( (window.opera && document.body.scrollWidth > windowSizeXY[0])
				  || (window.scrollMaxX && window.scrollMaxX > 0))
			delta = 25;

		if (posY + delta + divHeight > windowSizeXY[1]) posY = event.clientY - 5 - divHeight;
		if (posY < 0) posY = windowSizeXY[1] - delta - divHeight;

		posX += scrollXY[0];posY += scrollXY[1];
		if (this.menu_ns6) {
			this.te_menu.style.left = posX + "px";
			this.te_menu.style.top = posY + "px";
		} else {
			this.te_menu.style.pixelLeft = posX;
			this.te_menu.style.pixelTop = posY;
		}
		if (extraClass)
			this.te_menu.className = "te_menu " + extraClass;
		else
			this.te_menu.className = "te_menu";

		this.te_menu.style.visibility = "visible";
		this.menuON = true;
		this.disappearFunction = setTimeout("PopupMenu.closeMenu()", this.disappearInterval1);

        this.lastTarget = this.delayedState.evt.target || this.delayedState.evt.srcElement;
    },

	cancelDisappear : function() {
		if (this.disappearFunction) clearTimeout(this.disappearFunction);
		this.disappearFunction = undefined;
	},

	closeMenu: function () {
		this.cancelDisappear();
		if (this.menuON) {
			this.te_menu.style.display = "none";
		}
	},

	inMenuDiv: function (el) {
		if (el == undefined) return false;
		if (el == this.te_menu) return true;
		if (el.tagName && el.tagName.toLowerCase() == 'a') return false;
		return this.inMenuDiv(el.parentNode);
	},

	getTarget: function (e) {
		var evt = this.menu_ie ? window.event : e;
		var el = undefined;
		if (evt.target) {
			return evt.target;
		} else if (evt.srcElement) {
			return evt.srcElement;
		}
		;
		return undefined;
	},

	_init: function (contentElement, event, extraClass) {
		document.onclick = function(e) {
			var el = PopupMenu.getTarget(e);
			if (el && (el.name != 'menurevealbutton') && !PopupMenu.inMenuDiv(el))
				PopupMenu.closeMenu();
			return true;
		}

		try {
			this.te_menu = document.createElement('<div id="divmenu" class="te_menu" style="display:none; float:none;z-index:5; position:absolute;">');
		} catch (e) {
			this.te_menu = document.createElement("div");
			this.te_menu.setAttribute("class", "te_menu");
			this.te_menu.setAttribute("id", "divmenu");
			this.te_menu.style.display = "none";
			this.te_menu.style.cssFloat = "none";
			this.te_menu.style.zIndex = "5";
			this.te_menu.style.position = "absolute";
		}

		this.te_menu.onmouseout = function(e) {
			if (PopupMenu.getTarget(e) == PopupMenu.te_menu) {
				PopupMenu.cancelDisappear();
				PopupMenu.disappearFunction = setTimeout("PopupMenu.closeMenu()", PopupMenu.disappearInterval2);
			}
		}
		this.te_menu.onmouseover = function(e) {
			PopupMenu.cancelDisappear();
		}

		document.body.appendChild(this.te_menu);
		this.showPopupMenu = this._showPopupMenu;
		this.sheduleShowMenu = this._sheduleShowMenu;
	},

	cancelShowMenu: function() {
		if (this.delayedFunction) clearTimeout(this.delayedFunction);
		this.delayedFunction = undefined;
	},

	showAfterDelay : function() {
        if (!document.getElementById(this.delayedState.contentElement)) return;
		this.te_menu.style.display = "none";
		this._showPopupMenu(this.delayedState.contentElement, this.delayedState.evt, this.delayedState.extraClass);
    },

	_sheduleShowMenu: function(contentElement, event, delay, extraClass) {
		this.cancelShowMenu();
		this.delayedState.evt.clientX = event.clientX;
		this.delayedState.evt.clientY = event.clientY;
		this.delayedState.evt.target = event.target ? event.target : undefined;
		this.delayedState.evt.srcElement = event.srcElement ? event.srcElement : undefined;
		this.delayedState.extraClass = extraClass;
		this.delayedState.contentElement = contentElement;

		this.delayedFunction = setTimeout("PopupMenu.showAfterDelay()", delay);
	},

	// init
	showPopupMenu: function() {this._init(); this._showPopupMenu.apply(this, arguments);},
	sheduleShowMenu: function() {this._init(); this._sheduleShowMenu.apply(this, arguments);}
}/**
 * Base class for Editors.
 * If you need to create your own editor just override methods of this class.
 *
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */

var BaseEditor = Class.create({

    tableEditor: null,
    parentElement: null,
    input: null,
    initialValue: null,
    stoppedEvents: null,
    focus: null,
    style: null,

    /**
     * Constructor.
     * Generally editor constructor performs the following steps:
     *   1. Saves initial cell value into initialValue variable
     *   2. Creates an HTML editor control (e.g. HTMLInputElement) and sets its value
     */
    initialize: function(tableEditor, parentId, params, initialValue, focus, style) {
        if (parentId) {
            this.tableEditor = tableEditor;
            this.parentElement = $(parentId);

            this.style = style;

            this.initialValue = initialValue;

            this.editor_initialize(params);
            this.input.id = this.getId();
            this.focus = (focus && focus == true) ? focus : '';
            this.show(this.initialValue);
        }
    },

    /**
     *  Editor specific constructor.
     *  Typically HTML node is created and possible some events handlers are registered.
     */
    editor_initialize: Prototype.emptyFunction,

    /**
     * Obtains current value from HTML editor control.
     */
    getValue: function() {
        return this.input ? this.input.value.toString().replace(/\u00A0/g, ' ') : null;
    },

    setValue: function(value) {
        this.input.value = value;
    },

    getDisplayValue: function() {
        var value = this.isCancelled() ? this.initialValue : this.getValue();
        if (!value.strip()) {
            value = "&nbsp";
        } else {
            value = value.escapeHTML().replace(/\n/g, "<br/>");
        }
        return value;
    },

    /**
     * Is responsible for making editor visible and active.
     * In most cases it is not needed to be overridden.
     */
    show: function(value) {
        if (this.input) {
            this.parentElement.innerHTML = "";
            this.parentElement.appendChild(this.input);
            this.setValue(value);
            if (this.focus) {
                this.input.focus();
            }
        }
    },

    /**
     * Returns if the editing was cancelled.
     */
    isCancelled : function() {
        return (this.initialValue == this.getValue() || !this.isValid(this.getValue()));
    },

    switchTo: function(editorName) {
        this.tableEditor.switchEditor(editorName);
    },

    /**
     * Can be overridden in editors to clean up resources.
     */
    destroy: Prototype.emptyFunction,

    getId: function() {
        return '_' + this.parentElement.id;
    },

   /**
     * Notifies table editor that editing is finished.
     */
    doneEdit: function() {
        this.tableEditor.setCellValue();
    },

    /**
     * Notifies table editor that editing is finished and canceled.
     */
    cancelEdit: function() {
        this.isCancelled = BaseEditor.T;
        this.doneEdit();
    },

    /**
     *  Returns HTML element which is actually main input element for this editor.
     */
    getInputElement: function() {
        return this.input;
    },

    is: function(element) {
        return element == this.getInputElement();
    },

    bind: function(event, handler) {
        Event.observe(this.getInputElement(), event, handler);
    },

    unbind: function(event, handler) {
        Event.stopObserving(this.getInputElement(), event, handler);
    },

    /**
     * Validates input value.
     */
    isValid: function(value) {
        return true;
    }

});

BaseEditor.T = function() {
    return true;
}

BaseEditor.isTableEditorExists = function() {
    return typeof TableEditor != 'undefined';
}
/**
 * Base Text editor.
 *
 * Not an editor itself, it just introduces functions common for all text based editors - that is common reaction
 * to F2 F3 keys.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */

var BaseTextEditor = Class.create(BaseEditor, {

    maxInputSize: null,

    createInput: function() {
        this.input = new Element("input");
        this.input.setAttribute("type", "text");
        if (this.maxInputSize) {
            this.input.maxLength = this.maxInputSize;
        }

        this.setDefaultStyle();

        this.input.setStyle(this.style);
    },

    setDefaultStyle: function() {
        this.input.style.border = "1px solid threedface";
        this.input.style.margin = "0px";
        //this.input.style.padding = "0px";
        this.input.style.width = "100%";
        this.input.style.height = "100%";
    },

    /**
     * Moves caret to beginning of the input.
     */
    handleF2: function(event) {
        var input = this.getInputElement();
        if (input.createTextRange) {
            var r = input.createTextRange();
            r.collapse(true);
            r.select();

        } else if (input.setSelectionRange) {
            input.setSelectionRange(0, 0);
            input.focus();
        }
        Event.stop(event);
    },

    /**
     * Moves caret to the end of the input.
     */
    handleF3: function(event) {
        var input = this.getInputElement();
        if (!input) return;
        if (input.createTextRange) {
            var r = input.createTextRange();
            r.collapse(false);
            r.select();

        } else if (input.setSelectionRange) {
            var len = input.value.length;
            input.setSelectionRange(len, len);
            input.focus();
        }

        if (event) Event.stop(event);
    },

    show: function($super, value) {
        $super(value);
        if (this.focus) {
            this.handleF3();
        }
    }
});var fdLocale = {
                fullMonths:["January","February","March","April","May","June","July","August","September","October","November","December"],
                monthAbbrs:["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"],
                fullDays:  ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"],
                dayAbbrs:  ["Mon","Tue","Wed","Thu","Fri","Sat","Sun"],
                titles:    ["Previous month","Next month","Previous year","Next year", "Today", "Open Calendar", "wk", "Week [[%0%]] of [[%1%]]", "Week", "Select a date", "Click \u0026 Drag to move", "Display \u201C[[%0%]]\u201D first", "Go to Today\u2019s date", "Disabled date:"],
                firstDayOfWeek:0
};
try { datePickerController.loadLanguage(); } catch(err) {} 
eval(function(p,a,c,k,e,r){e=function(c){return(c<a?'':e(parseInt(c/a)))+((c=c%a)>35?String.fromCharCode(c+29):c.toString(36))};if(!''.replace(/^/,String)){while(c--)r[e(c)]=k[c]||e(c);k=[function(e){return r[e]}];e=function(){return'\\w+'};c=1};while(c--)if(k[c])p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c]);return p}('k 7E=(v 7E(){k 3o=A,5b=9h.1n.aQ.9i(1p.9j)==="[3V aR]",9k=/aS/.5H(9l.9m.2a())&&!/(aT|aU)/.5H(9l.9m.2a()),6z=9n(),P={},aV=0,5I={},1Z=A,3W=15.aW(aX),2G="",5J=A,7F=K,6A=A,6B=K,6C="d-1R-F-1R-Y",4z="F-1R-d-cc-1R-Y",5K=5b?["5c"]:["5c","1R-F-1R-Y"],aY=["dt","4A","ds","cc","1R"],aZ="dt|4A|ds|cc|1R",7G="d|j",7H="m|n|M|F",7I="Y|y",3F=A,5d=/^((1R|dt|4A|ds|cc)|([d|D|l|j|N|w|S|W|M|F|m|n|t|Y|y]))(-((1R|dt|4A|ds|cc)|([d|D|l|j|N|w|S|W|M|F|m|n|t|Y|y])))*$/,2y=/^((\\d\\d\\d\\d)(0[1-9]|1[5L])(0[1-9]|[12][0-9]|3[2S]))$/,5e=/^(((\\d\\d\\d\\d)|(\\*\\*\\*\\*))((0[1-9]|1[5L])|(\\*\\*))(0[1-9]|[12][0-9]|3[2S]))$/;(v(){k 4B=q.1N(\'2T\'),9o=15(4B[4B.16-1].b0).1d(/[\\n\\r\\s\\t]+/g," ").1d(/^\\s+/,"").1d(/\\s+$/,""),3p=9p(9o);h(2z 3p==="3V"&&!("2H"18 3p)){7J(3p)};h(2z(3G)!="3V"){k 6D=q.1N("6D")[0]||q.3q,9q=4B[4B.16-1].6E.V(0,4B[4B.16-1].6E.b1("/"))+"/5M/",2T;Z(k i=0;i<6z.16;i++){2T=q.1J(\'2T\');2T.1v="3X/9r";2T.6E=9q+6z[i]+".b2";2T.b3="b4-8";/*@1L/*@h(@2U)k 5N=q.1N(\'b5\');h(5N.16&&5N[0].b6.16){5N[0].17(2T)}I{q.1N(\'6D\')[0].17(2T)};5N=1a;@I@*/6D.17(2T);/*@23@*/};2T=1a}I{6A=K}})();v 9n(){k 3H=q.1N(\'6F\')[0].7K(\'5M\')||q.1N(\'6F\')[0].7K(\'b7:5M\');h(!3H){3H="b8"}I{3H=3H.2a()};u 3H.J(/^([a-z]{2,3})-([a-z]{2})$/)!=-1?[3H.1W(/^([a-z]{2,3})-([a-z]{2})$/)[1],3H]:[3H]};v 7J(3p){h(2z 3p!=="3V"){u};Z(7L 18 3p){1w=3p[7L];7M(7L.2a()){1r"5M":h(1w.J(/^[a-z]{2,3}(-([a-z]{2}))?$/i)!=-1){6z=[1w.2a()];6A=K};1e;1r"5J":5J=!!1w;1e;1r"9s":7F=!!1w;1e;1r"3Y":6B=!!1w;1e;1r"9t":h(2z 1w=="6G"&&1w.1W(5d)){9u(1w)};1e;1r"9v":h(2z 1w=="6G"&&1w.1W(5d)){4z=1w};1e;1r"6H":h(2z 1w=="6G"){2G=1w}}}};v 9u(1w){h(5b){5K=["5c"];6C="j-1R-F-1R-Y";u};k 3Z=1w.5O("-"),4C=[],4D=[],2b;Z(k 2A=0;2A<3Z.16;2A++){2b=3Z[2A];h(2b=="j"||2b=="d"){h(4D.16){4C.2c(4D.5f("-"));4D=[]};4C.2c("5c")}I{4D.2c(2b)}};h(4D.16){4C.2c(4D.5f("-"))};h(!4C.16||4C.16>3){5K=["5c","1R-F-1R-Y"];6C="j-1R-F-1R-Y";u};5K=4C;6C=1w};v 1h(1w,16){16=16||2;u"b9".V(0,16-1M.3I(15(1w).16,16))+1w};v 2j(1s,1v,2n){2I{h(1s.9w){1s["e"+1v+2n]=2n;1s[1v+2n]=v(){1s["e"+1v+2n](1p.2V)};1s.9w("5g"+1v,1s[1v+2n])}I{1s.5P(1v,2n,K)}}2J(2H){}};v 1A(1s,1v,2n){2I{h(1s.9x){1s.9x("5g"+1v,1s[1v+2n]);1s[1v+2n]=1a}I{1s.7N(1v,2n,K)}}2J(2H){}};v 1O(e){e=e||q.4E.2V;h(e.9y){e.9y();e.ba()};/*@1L@h(@2U)e.bb=K;e.bc=A;@23@*/u A};v 9p(X){h(2z X!==\'6G\'||X==""){u{}};2I{h(2z 5Q==="3V"&&5Q.6I){u 1p.5Q.6I(X)}I h(/5M|9s|3Y|9t|9v|5J|6H/.5H(X.2a())){k f=9z([\'k q,3J,bd,1p,9A,2o,14,9h,9z,\',\'be,15,1M,2K,bf,bg;\',\'u (\',X.1d(/<\\!--.+-->/bh,\'\').1d(/\\bi\\b/g,\'v­\'),\');\'].5f(\'\'));u f()}}2J(e){};h(3o){43"5R 3K 6I 44 5Q 3V";};u{"2H":"5R 3K 6I 44 5Q 3V"}};v 3r(1o,7O){h(1o&&1o.24){1o.2W("7O",7O)}};v 2r(1o,9B,1w){h(1o&&1o.24){1o.2W("bj-"+9B,1w)}};v 1i(E){c.1B=1a;c.5S=A;c.2X=A;c.5T=1a;c.5U=1a;c.3L=0;c.2Y=0;c.6J=0;c.7P=0;c.7Q=0;c.x=0;c.y=0;c.2L=A;c.1j=A;c.45=0;c.3M=99;c.2Z=A;c.5V=A;c.5h=A;c.5W=1a;c.bk=1a;c.1E=E.1E?E.1E:"",c.B=E.1E?L 14(+E.1E.V(0,4),+E.1E.V(4,2)-1,+E.1E.V(6,2)):L 14();c.bl={};c.bm={};c.2M=1Z.2M;c.5i=L 14();c.46=A;c.1S=K;c.3F=A;c.2k=A;c.25=A;c.5X=A;Z(k 4F 18 E){h(4F.J(/5j|2l|4G/)!=-1)3s;c[4F]=E[4F]};/*@1L@h(@2U)c.1F=1a;c.47=A;@23@*//*@1L@h(@4H<=5.7)c.47=q.3q&&2z q.3q.1b.bn!="bo";@23@*/Z(k i=0,5k;5k=["5j","2l","4G"][i];i++){c[5k]={};Z(k 4F 18 E[5k]){c[5k][4F]=E[5k][4F]}};c.B.6K(5);c.9C=v(){o.3a()};c.5l=v(){u c.1B?{"C":c.C,"B":c.1B,"dd":1h(c.B.1C()),"6L":1h(c.B.1f()+1),"3t":c.B.1c()}:{"C":c.C,"B":1a,"dd":1a,"6L":1a,"3t":1a}};c.9D=v(){h(2z(1p.7R)==\'7S\'){u[1p.9E,1p.7R]}I h(q.3b&&(q.3b.3N||q.3b.3O)){u[q.3b.3N,q.3b.3O]}I h(q.3q&&(q.3q.3N||q.3q.3O)){u[q.3q.3N,q.3q.3O]};u[0,0]};c.7T=v(){h(!o.2L||o.1P){u};o.Q.1b.4I="3c";o.Q.1b.4a=o.Q.1b.3J="7U";o.Q.1b.3u="6M";k 5Y=o.Q.5m,6N=o.Q.7V,R=q.1l(\'1g-H-\'+o.C),3d=o.9F(R),6O=(q.6P&&q.6P!="9G")?q.3q:q.3b,7W=o.9D(),3O=7W[1],3N=7W[0],9H=2N(6O.bp+3O)>2N(5Y+3d[1]+R.5m+2),9I=2N(3d[1]-(5Y+R.5m+2))>2N(3O);o.Q.1b.4I="2X";o.Q.1b.4a=2o(2N(6O.9J+3N)<2N(6N+3d[0])?1M.6Q(2N((6O.9J+3N)-6N)):3d[0])+"3e";o.Q.1b.3J=(9H||!9I)?1M.6Q(2N(3d[1]+R.5m+2))+"3e":1M.6Q(2N(3d[1]-(5Y+2)))+"3e";/*@1L@h(@4H<=5.7)h(o.47)u;o.1F.1b.3J=o.Q.1b.3J;o.1F.1b.4a=o.Q.1b.4a;o.1F.1b.7X=6N+"3e";o.1F.1b.7Y=(5Y-2)+"3e";@23@*/};c.5n=v(){k T=q.1l(o.C+"-B-1x-3v");h(T){2I{T.2W(!/*@1L!@*/A?"3f":"5Z","-1");T.3f=-1;T.G=T.G.1d(/B-1x-3v/,"");T.C="";T.6R=1a;T.6S=1a}2J(2H){}}};c.9K=v(){k T=q.1l(o.C+"-B-1x-3v");h(T&&!(T.1N("1k").16)){k 60=T.G.1W(/cd-([\\d]{4})([\\d]{2})([\\d]{2})/),9L=(T.G.J(/B-1x-6T|3w-2O-2P|2d-1j|6U-7Z|3K-4b/)!=-1),4J=q.1J(\'1k\'),3P;4J.G="1g-80-81";2m(T.26)T.3g(T.26);h(9L){3P=4J.82(A);3P.17(q.1X(1D(13)));T.17(3P)};Z(k 2A=0,2b;2b=5K[2A];2A++){h(2b=="5c"){T.17(q.1X(+60[3]))}I{3P=4J.82(A);3P.17(q.1X(3h(L 14(60[1],+60[2]-1,60[3]),2b,K)));T.17(3P)}}}};c.5o=v(){k T=q.1l(o.C+"-B-1x-3v");h(T){2I{T.2W(!/*@1L!@*/A?"3f":"5Z","0");T.3f=0;T.G=T.G.1d(/B-1x-3v/,"")+" B-1x-3v";h(!c.46){T.6R=o.6R;T.6S=o.6S};h(!5b&&!c.46)o.9K();h(!c.1S&&!c.46){6V(v(){2I{T.61()}2J(2H){}},0)}}2J(2H){}}};c.83=v(1G){h(15(1G).J(/^([0-9]{8})$/)!=-1){c.B=L 14(+1G.V(0,4),+1G.V(4,2)-1,+1G.V(6,2));c.1E=1G;h(c.1P){c.2e()}}};c.2e=v(bq){h(!o||o.2Z||!o.2L)u;o.2Z=K;o.5n();h(o.5S&&!o.5X){h(o.2Y){k n=o.B.1C(),d=L 14(o.B);d.2B(2);d.62(d.1f()+o.2Y*1);d.2B(1M.3I(n,21(d.1f(),d.1c())));o.B=L 14(d)}I{o.B.2B(1M.3I(o.B.1C()+o.6J,21(o.B.1f()+o.2Y,o.B.1c()+o.3L)));o.B.62(o.B.1f()+o.2Y);o.B.9M(o.B.1c()+o.3L)}};o.3i();h(!o.63){o.6W()};o.84(o.B);k cd=o.B.1C(),cm=o.B.1f(),cy=o.B.1c(),1E=(15(cy)+1h(cm+1)+1h(cd)),1t=L 14(cy,cm,1);1t.6K(5);k dt,2s,T,i,3j,4c,1m,2C,9N,6X,3P,85,86=(1t.3x()+6)%7,4d=(((86-o.2M)+7)%7)-1,4e=21(cm,cy),2f=L 14(),64=15(1t.1c())+1h(1t.1f()+1),4c=[4,4,4,4,4,4],5p=L 14(cy,cm-1,1),6Y=L 14(cy,cm+1,1),87=21(5p.1f(),5p.1c()),88=15(6Y.1c())+1h(6Y.1f()+1),8a=15(5p.1c())+1h(5p.1f()+1),9O=(6Y.3x()+6)%7,9P=(5p.3x()+6)%7,2f=2f.1c()+1h(2f.1f()+1)+1h(2f.1C()),4J=q.1J(\'1k\');o.4K=!o.4L&&o.5q&&(0-4d<1)?15(8a)+(87+(0-4d)):64+"2S";o.4M=!o.4L&&o.5q?88+1h(41-4d-4e):64+15(4e);o.6Z=64;6X=o.3y("br",{C:o.C,dd:1h(cd),6L:1h(cm+1),3t:cy,bs:o.4K,bt:o.4M})||{};2Q=o.8b(cy,cm+1);o.9Q();85=(o.1B!=1a)?o.1B.1c()+1h(o.1B.1f()+1)+1h(o.1B.1C()):A;4J.G="1g-80-81";h(c.5W!=1a){2r(c.5W,"70",A);c.5W=1a};Z(k 3z=0;3z<42;3z++){1H=1M.bu(3z/7);T=o.8c[3z];3P=4J.82(A);2m(T.26)T.3g(T.26);h((3z>4d&&3z<=(4d+4e))||o.5q){2C=64;4f=86;dt=3z-4d;2s=[];4b=K;h(dt<1){dt=87+dt;2C=8a;4f=9P;4b=!o.4L;2s.2c("1Q-3w")}I h(dt>4e){dt-=4e;2C=88;4f=9O;4b=!o.4L;2s.2c("1Q-3w")};4f=(4f+dt+6)%7;2s.2c("2d-"+3k.3Q[4f].2a());3j=2C+15(dt<10?"0":"")+dt;h(o.1T&&+3j<+o.1T||o.1U&&+3j>+o.1U){T.G="3w-2O-2P";T.22="";T.17(q.1X(dt));h(o.1Y){4c[1H]=1M.3I(4c[1H],2)}}I{h(4b){T.22=4z?3h(L 14(+15(2C).V(0,4),+15(2C).V(4,2)-1,+dt),4z,K):"";2s.2c("cd-"+3j+" 4g-"+2C+" 9R-"+2C.V(4,2)+1h(dt))}I{T.22=4z?1D(13)+" "+3h(L 14(+15(2C).V(0,4),+15(2C).V(4,2)-1,+dt),4z,K):"";2s.2c("4g-"+2C+" 9R-"+2C.V(4,2)+1h(dt)+" 3K-4b")};h(3j==2f){2s.2c("B-1x-2f")};h(85==3j){2s.2c("B-1x-70-B");2r(T,"70","K");c.5W=T};h(o.3A[4f]||2Q[3j]==0){2s.2c("2d-1j");h(4z&&4b){T.22=1D(13)+" "+T.22}}h(3j 18 6X){2s.2c(6X[3j])}h(o.4N[4f]){2s.2c("B-1x-71")};h(1E==3j){T.C=o.C+"-B-1x-3v"};T.17(q.1X(dt));T.G=2s.5f(" ");h(o.1Y){4c[1H]=1M.3I(2s[0]=="1Q-3w"?3:1,4c[1H])}}}I{T.G="B-1x-6T";T.17(q.1X(3W));T.22=""};h(o.1Y&&3z-(1H*7)==6){2m(o.5r[1H].26)o.5r[1H].3g(o.5r[1H].26);o.5r[1H].17(q.1X(4c[1H]==4&&!o.5q?3W:8d(cy,cm,3z-4d-6)));o.5r[1H].G="B-1x-8e-5s"+(["",""," 3w-2O-2P"," 1Q-3w",""][4c[1H]])}};k 1k=o.65.1N("1k");2m(1k[0].26)1k[0].3g(1k[0].26);2m(1k[1].26)1k[1].3g(1k[1].26);1k[0].17(q.1X(9S(cm,A)+3W));1k[1].17(q.1X(cy));h(o.5S){o.4O=50+1M.4h(((o.4O-50)/1.8));o.5U=1p.6V(o.2e,o.4O)};o.2Z=o.5X=A;o.5o()};c.4P=v(){h(q.1l("1g-H-"+c.C)){q.1l("1g-H-"+c.C).2g.3g(q.1l("1g-H-"+c.C))};h(!c.2L){u};1A(c.1y,"4Q",o.2D);1A(c.1y,"bv",o.4R);1A(c.1y,"8f",o.4S);1A(q,"4Q",o.2D);1A(q,"5t",o.4T);h(1p.5P&&!1p.72){2I{1p.7N(\'8g\',c.3B,A)}2J(2H){}}I{1A(q,"3Y",c.3B);1A(1p,"3Y",c.3B)};o.73();66(o.5T);66(o.5U);/*@1L@h(@4H<=5.7)h(!o.1P&&!o.47){2I{o.1F.2g.3g(o.1F);o.1F=1a}2J(2H){}};@23@*/h(c.Q&&c.Q.2g){c.Q.2g.3g(c.Q)};o=1a};c.9T=v(){o.Q.1b.7X=o.1y.7V+"3e";o.Q.1b.7Y=o.1y.5m+"3e"};c.74=v(){h(q.1l("1g-"+c.C))u;c.1S=K;v 67(2E){k 1V=q.1J(\'1V\');h(2E.75)1V.G=2E.75;h(2E.4U){/*@1L/*@h(@2U)1V.2W(\'bw\',2E.4U);@I@*/1V.2W(\'4U\',2E.4U);/*@23@*/};/*@1L/*@h(@2U)1V.68="5g";/*@23@*/u 1V};v 8h(1K,1s){Z(k i=0,2E;2E=1s[i];i++){k 1V=67(2E);1K.17(1V);k H=q.1J(\'1k\');H.G=2E.G;H.C=o.C+2E.C;H.17(q.1X(2E.3X||o.3W));H.22=2E.22||"";/*@1L/*@h(@2U)1V.68=H.68="5g";/*@23@*/1V.17(H)}};c.Q=q.1J(\'Q\');c.Q.C="1g-"+c.C;c.Q.G="1i";c.Q.1b.4I="3c";c.Q.1b.3u="76";h(c.2G&&q.1l(c.2G)){2r(c.Q,"6H",c.2G)};h(c.8i){2r(c.Q,"bx",c.8i.C)};k 1K,1H,1m,4V,69,77;c.1y=q.1J(\'1y\');c.1y.G="by";c.1y.4R=c.4R;c.1y.4S=c.4S;c.1y.4i=c.4i;h(c.1P){c.1y.2D=c.2D};c.Q.17(c.1y);k 5u=!c.8j?" 4W-5v":"";h(!c.1P){c.Q.1b.4I="3c";c.Q.G+=5u;q.1N(\'3b\')[0].17(c.Q);/*@1L@h(@4H<=5.7)h(!c.47){c.1F=q.1J(\'bz\');c.1F.6E="9r:\'<6F></6F>\';";c.1F.2W(\'G\',\'bA\');c.1F.2W("3f",-1);3r(c.1F,"8k");2r(c.1F,"3c","K");c.1F.bB="6U";c.1F.bC="0";c.1F.bD=c.1F.C=c.C+"-bE";q.3b.17(c.1F)};@23@*/2r(c.Q,"3c","K")}I{R=q.1l(c.2R?c.2R:c.C);h(!R){c.Q=1a;h(3o)43 c.2R?"5R 3K 9U a P 9V 9A 1o 4X an C:"+c.2R:"5R 3K 9U a P 9V 5w 4X an C:"+c.C;u};c.Q.G+=" bF-78";h(c.2R){R.17(c.Q)}I{R.2g.9W(c.Q,R.9X)};h(c.8l){Z(k 1z 18 c.2l){R=q.1l(1z);h(R){R.G+=" 1g-3c-5w"}}};6V(c.9T,bG)};3r(c.Q,"bH");h(c.3C){77=q.1J(\'9Y\');c.1y.17(77);1K=q.1J(\'1K\');1K.G="B-1x-9Y";77.17(1K);c.2t=67({75:"B-1x-bI"+5u,4U:c.1Y?8:7});1K.17(c.2t);c.4Y()};4V=q.1J(\'9Z\');c.1y.17(4V);1K=q.1J(\'1K\');3r(1K,"8k");4V.17(1K);c.65=67({75:"B-1x-22"+5u,4U:c.1Y?8:7});1K.17(c.65);1K=1a;k 1k=q.1J(\'1k\');1k.17(q.1X(3W));1k.G="1Q-3u"+5u;c.65.17(1k);1k=q.1J(\'1k\');1k.17(q.1X(3W));1k.G="2p-3u"+5u;c.65.17(1k);1k=1a;1K=q.1J(\'1K\');3r(1K,"8k");4V.17(1K);8h(1K,[{G:"2u-H 2u-2p",C:"-2u-2p-H",3X:"\\bJ",22:1D(2)},{G:"2u-H 2u-1Q",C:"-2u-1Q-H",3X:"\\bK",22:1D(0)},{4U:c.1Y?4:3,G:"2f-H",C:"-2f-H",3X:1D(4)},{G:"2v-H 2v-1Q",C:"-2v-1Q-H",3X:"\\bL",22:1D(1)},{G:"2v-H 2v-2p",C:"-2v-2p-H",3X:"\\bM",22:1D(3)}]);69=q.1J(\'79\');c.1y.17(69);k 6a=c.1Y?8:7,5x=c.1Y?0:-1,H,9N;Z(k 3D=0;3D<7;3D++){1H=q.1J(\'1K\');h(3D!=0){3r(1H,"1H");69.17(1H)}I{4V.17(1H)};Z(k 4j=0;4j<6a;4j++){h(3D===0||(c.1Y&&4j===0)){1m=q.1J(\'1V\')}I{1m=q.1J(\'T\');2r(1m,"6H",c.C+"-1m-"+4j+(c.1Y?" "+c.C+"-1H-"+3D:""));2r(1m,"70","A")};/*@1L@*//*@h(@2U)1m.68="5g";/*@23@*/1H.17(1m);h((c.1Y&&4j>0&&3D>0)||(!c.1Y&&3D>0)){3r(1m,"bN")}I{h(3D===0&&4j>5x){1m.G="B-1x-2d-5s";1m.a0="1m";3r(1m,"bO");1m.C=c.C+"-1m-"+4j}I{1m.G="B-1x-8e-5s";1m.a0="1H";3r(1m,"bP");1m.C=c.C+"-1H-"+3D}}}};1m=1H=1a;c.2w=c.1y.1N(\'9Z\')[0].1N(\'1K\')[2].1N(\'1V\');Z(k y=0;y<6a;y++){h(y==0&&c.1Y){c.2w[y].17(q.1X(1D(6)));c.2w[y].22=1D(8);3s};h(y>(c.1Y?0:-1)){H=q.1J("1k");H.G="1g-2d-5s";/*@1L@*//*@h(@2U)H.68="5g";/*@23@*/c.2w[y].17(H)}};H=1a;c.bQ=c.1y.1N(\'79\')[0].1N(\'1K\');c.8c=c.1y.1N(\'79\')[0].1N(\'T\');c.5y=q.1l(c.C+"-2u-2p-H");c.6b=q.1l(c.C+"-2u-1Q-H");c.6c=q.1l(c.C+"-2f-H");c.6d=q.1l(c.C+"-2v-2p-H");c.6e=q.1l(c.C+"-2v-1Q-H");h(c.63){c.6c.1b.3u="76"};h(c.1Y){c.5r=c.1y.1N(\'79\')[0].1N(\'1V\');c.Q.G+=" bR-bS"};69=4V=1K=8h=67=1a;h(c.1T&&c.1U&&(c.1U-c.1T<7)){c.a1()};c.7a();c.2L=K;c.2e();h(c.1P){c.2X=K;c.45=c.3M=c.4k;c.Q.1b.4I="2X";c.Q.1b.3u="6M";c.1S=K;c.5z()}I{c.7T();c.Q.1b.4I="2X";c.5z();c.1S=K};c.3y("bT",{"C":c.C})};c.5z=v(){1p.66(o.5T);o.5T=1a;k 8m=1M.4h(o.45+((o.3M-o.45)/4));o.8n(8m);h(1M.6Q(o.3M-8m)>3&&!o.8o){o.5T=1p.6V(o.5z,50)}I{o.8n(o.3M);h(o.3M==0){o.Q.1b.3u="76";o.Q.1b.4I="3c";2r(o.Q,"3c","K");o.2X=A}I{2r(o.Q,"3c","A");o.2X=K}}};c.8p=v(e){e=e||1p.2V;k 8q=(e.7b?e.7b:e.7c?e.7c:e.x)-o.7P;k 8r=(e.7d?e.7d:e.7e?e.7e:e.Y)-o.7Q;o.Q.1b.4a=1M.4h(o.x+8q)>0?1M.4h(o.x+8q)+\'3e\':"7U";o.Q.1b.3J=1M.4h(o.y+8r)>0?1M.4h(o.y+8r)+\'3e\':"7U";/*@1L@h(@4H<=5.7)h(o.1P||o.47)u;o.1F.1b.3J=o.Q.1b.3J;o.1F.1b.4a=o.Q.1b.4a;@23@*/};c.8s=v(e){k b=q.1N("3b")[0];b.G=b.G.1d(/1g-4W-3R/g,"");1A(q,\'a2\',o.8p,A);1A(q,\'5t\',o.8s,A);o.Q.1b.a3=bU};c.2D=v(e){e=e||q.4E.2V;k U=e.5A!=1a?e.5A:e.8t,5B=U,8u=K,a4=L 2K("^1g-(H-)?"+o.C+"$");o.6f=1a;2m(U){h(U.C&&U.C.16&&U.C.J(a4)!=-1){8u=A;1e};2I{U=U.2g}2J(2H){1e}};h(8u){6g();u K};h((o.Q.G+5B.G).J(\'1g-1j\')!=-1){u K};h(5B.C.J(L 2K("^"+o.C+"(-2u-2p-H|-2u-1Q-H|-2v-1Q-H|-2v-2p-H)$"))!=-1){o.6f=5B;2j(q,"5t",o.4T);2j(5B,"8f",o.4T);k 7f={"-2u-2p-H":[0,-1,0],"-2u-1Q-H":[0,0,-1],"-2v-2p-H":[0,1,0],"-2v-1Q-H":[0,0,1]},7g=5B.C.1d(o.C,""),7h=2o(o.B.1c()+1h(o.B.1f()+1));o.4O=8v;o.5S=K;o.6J=7f[7g][0];o.3L=7f[7g][1];o.2Y=7f[7g][2];o.bV=1;h(!(o.6Z==7h)){h((o.6Z<7h&&(o.3L==-1||o.2Y==-1))||(o.6Z>7h&&(o.3L==1||o.2Y==1))){o.5X=A;o.4O=bW}I{o.5X=K;o.4O=8v}};o.2e();u 1O(e)}I h(U.G.J("4W-5v")!=-1){o.7P=e.7b?e.7b:e.7c?e.7c:e.x;o.7Q=e.7d?e.7d:e.7e?e.7e:e.Y;o.x=2N(o.Q.1b.4a);o.y=2N(o.Q.1b.3J);2j(q,\'a2\',o.8p,A);2j(q,\'5t\',o.8s,A);k b=q.1N("3b")[0];b.G=b.G.1d(/1g-4W-3R/g,"")+" 1g-4W-3R";o.Q.1b.a3=bX;u 1O(e)};u K};c.4i=v(e){h(o.45!=o.3M||o.1j)u 1O(e);e=e||q.4E.2V;k U=e.5A!=1a?e.5A:e.8t;2m(U.2g){h(U.24&&U.24.2a()=="T"){h(U.G.J(/cd-([0-9]{8})/)==-1||U.G.J(/B-1x-6T|3w-2O-2P|2d-1j|6U-7Z|3K-4b/)!=-1)u 1O(e);k 4l=U.G.1W(/cd-([0-9]{8})/)[1];o.B=L 14(4l.V(0,4),4l.V(4,2)-1,4l.V(6,2));o.1B=L 14(o.B);o.1S=K;o.3y("6h",{"C":o.C,"B":o.1B,"dd":o.1B.1C(),"6L":o.1B.1f()+1,"3t":o.1B.1c()});o.6i();o.3S();o.3l();1e}I h(U.C&&U.C==o.C+"-2f-H"){o.B=L 14();o.2e();o.3l();1e}I h(U.G.J(/B-1x-2d-5s/)!=-1){k 8w=o.1Y?-1:0,R=U;2m(R.a5){R=R.a5;h(R.24&&R.24.2a()=="1V")8w++};o.2M=(o.2M+8w)%7;o.7a();1e};2I{U=U.2g}2J(2H){1e}};u 1O(e)};c.8x=v(4m){h(c.1P){u};k R,1z;Z(1z 18 c.2l){R=q.1l(c.C);h(!R||(R&&R.1j)){u}};c.1S=K;h(!c.2L||!q.1l(\'1g-\'+c.C)){c.2L=A;c.5h=A;c.74();c.5h=K}I{c.3a();c.7T()};c.1S=!!!4m;h(c.1S){c.46=K;2j(q,"4Q",c.2D);h(6B){h(1p.5P&&!1p.72)1p.5P(\'8g\',c.3B,A);I{2j(q,"3Y",c.3B);2j(1p,"3Y",c.3B)}}}I{c.46=A};c.3M=c.4k;c.Q.1b.3u="6M";/*@1L@h(@4H<=5.7)h(!o.47){c.1F.1b.7X=c.Q.7V+"3e";c.1F.1b.7Y=c.Q.5m+"3e";c.1F.1b.3u="6M"};@23@*/c.5o();c.5z();k 4n=q.1l(\'1g-H-\'+c.C);h(4n){4n.G=4n.G.1d("dp-4Z-3R","")+" dp-4Z-3R"}};c.3S=v(){h(!c.2X||!c.2L||!q.1l(\'1g-\'+c.C))u;c.3F=A;o.Q.G=o.Q.G.1d("78-61","");c.3l();c.73();c.46=A;h(c.2t){c.4Y(1D(9))};c.1S=K;c.5o();h(c.1P){u};k 4n=q.1l(\'1g-H-\'+c.C);h(4n)4n.G=4n.G.1d("dp-4Z-3R","");1A(q,"4Q",c.2D);h(6B){h(1p.5P&&!1p.72){2I{1p.7N(\'8g\',c.3B,A)}2J(2H){}}I{1A(q,"3Y",c.3B);1A(1p,"3Y",c.3B)}};/*@1L@h(@4H<=5.7)h(!c.47){c.1F.1b.3u="76"};@23@*/c.3M=0;c.5z()};c.6R=v(e){o.3S()};c.6S=v(e){o.1S=A;o.Q.G=o.Q.G.1d("78-61","")+" 78-61";o.a6()};c.3B=v(e){e=e||q.4E.2V;k 5C=0;h(e.a7){5C=e.a7/bY;h(5b&&1p.9j.bZ()<9.2)5C=-5C}I h(e.a8){5C=-e.a8/3};k n=o.B.1C(),d=L 14(o.B),6j=5C>0?1:-1;d.2B(2);d.62(d.1f()+6j*1);d.2B(1M.3I(n,21(d.1f(),d.1c())));h(o.3i(d)){u 1O(e)};o.B=L 14(d);o.2e();h(o.2t){o.4Y(3h(o.B,o.3C,K))};u 1O(e)};c.3m=v(e){o.3l();h(!o.2X)u A;e=e||q.4E.2V;k 1u=e.7i?e.7i:e.a9;h(1u==13){k T=q.1l(o.C+"-B-1x-3v");h(!T||T.G.J(/cd-([0-9]{8})/)==-1||T.G.J(/6U-7Z|3w-2O-2P|2d-1j/)!=-1){u 1O(e)};o.1B=L 14(o.B);o.3y("6h",o.5l());o.6i();o.3S();u 1O(e)}I h(1u==27){h(!o.1P){o.3S();u 1O(e)};u K}I h(1u==32||1u==0){o.B=L 14();o.2e();u 1O(e)}I h(1u==9){h(!o.1P){u 1O(e)};u K};/*@1L@h(@2U)h(L 14().5D()-o.5i.5D()<50){u 1O(e)};o.5i=L 14();@23@*/h(9k){h(L 14().5D()-o.5i.5D()<50){u 1O(e)};o.5i=L 14()};h((1u>49&&1u<56)||(1u>97&&1u<c0)){h(1u>96)1u-=(96-48);1u-=49;o.2M=(o.2M+1u)%7;o.7a();u 1O(e)};h(1u<33||1u>40)u K;k d=L 14(o.B),c1,c2=o.B.1c()+1h(o.B.1f()+1);h(1u==36){d.2B(1)}I h(1u==35){d.2B(21(d.1f(),d.1c()))}I h(1u==33||1u==34){k 6j=(1u==34)?1:-1;h(e.aa){d.9M(d.1c()+6j*1)}I{k n=o.B.1C();d.2B(2);d.62(d.1f()+6j*1);d.2B(1M.3I(n,21(d.1f(),d.1c())))}}I h(1u==37){d=L 14(o.B.1c(),o.B.1f(),o.B.1C()-1)}I h(1u==39||1u==34){d=L 14(o.B.1c(),o.B.1f(),o.B.1C()+1)}I h(1u==38){d=L 14(o.B.1c(),o.B.1f(),o.B.1C()-7)}I h(1u==40){d=L 14(o.B.1c(),o.B.1f(),o.B.1C()+7)};h(o.3i(d)){u 1O(e)};o.B=d;h(o.2t){o.4Y(3h(o.B,o.3C,K))};k t=15(o.B.1c())+1h(o.B.1f()+1)+1h(o.B.1C());h(e.aa||(1u==33||1u==34)||t<o.4K||t>o.4M){o.2e();/*@1L@h(@2U)o.5i=L 14();@23@*/}I{h(!o.63){o.6W()};o.5n();Z(k i=0,T;T=o.8c[i];i++){h(T.G.J("cd-"+t)==-1){3s};o.84(o.B);T.C=o.C+"-B-1x-3v";o.5o();1e}};u 1O(e)};c.4S=v(e){e=e||q.4E.2V;k p=e.c3||e.c4;2m(p&&p!=c)2I{p=p.2g}2J(e){p=c};h(p==c)u A;h(o.51){o.51.G="";o.51=1a};h(o.2t){o.4Y(3h(o.B,o.3C,K))}};c.4R=v(e){e=e||q.4E.2V;k U=e.5A!=1a?e.5A:e.8t;2m(U.ab!=1){U=U.2g};h(!U||!U.24){u};k 2q=1D(9);7M(U.24.2a()){1r"T":h(U.G.J(/B-1x-6T|3w-2O-2P/)!=-1){2q=1D(9)}h(U.G.J(/cd-([0-9]{8})/)!=-1){o.3l();k 4l=U.G.1W(/cd-([0-9]{8})/)[1];o.5n();U.C=o.C+"-B-1x-3v";o.5o();o.B=L 14(+4l.V(0,4),+4l.V(4,2)-1,+4l.V(6,2));h(!o.63){o.6W()};2q=3h(o.B,o.3C,K)};1e;1r"1V":h(!o.2t){1e};h(U.G.J(/4W-5v/)!=-1){2q=1D(10)}I h(U.G.J(/B-1x-8e-5s/)!=-1){k 4o=U.26?U.26.c5:"";2q=4o.J(/^(\\d+)$/)!=-1?1D(7,[4o,4o<3&&o.B.1f()==11?8y(o.B.1c())+1:8y(o.B.1c())]):1D(9)};1e;1r"1k":h(!o.2t){1e};h(U.G.J(/4W-5v/)!=-1){2q=1D(10)}I h(U.G.J(/2d-([0-6])/)!=-1){k 2d=U.G.1W(/2d-([0-6])/)[1];2q=1D(11,[6k(2d,A)])}I h(U.G.J(/2u-2p/)!=-1){2q=1D(2)}I h(U.G.J(/2u-1Q/)!=-1){2q=1D(0)}I h(U.G.J(/2v-2p/)!=-1){2q=1D(3)}I h(U.G.J(/2v-1Q/)!=-1){2q=1D(1)}I h(U.G.J(/2f-H/)!=-1&&U.G.J(/1j/)==-1){2q=1D(12)};1e;ac:2q=""};2m(U.2g){U=U.2g;h(U.ab==1&&U.24.2a()=="1K"){h(o.51){h(U==o.51)1e;o.51.G=""};U.G="dp-1H-71";o.51=U;1e}};h(o.2t&&2q){o.4Y(2q)}};c.4T=v(){o.3l();o.4O=8v;o.3L=0;o.2Y=0;o.6J=0;1A(q,"5t",o.4T);h(o.6f!=1a){1A(o.6f,"8f",o.4T)};o.6f=1a};k o=c;c.3a();h(c.1P){c.74()}I{c.ad()};(v(){k 1z,R;Z(1z 18 o.2l){R=q.1l(1z);h(R&&R.24&&R.24.J(/8z|5w/i)!=-1){2j(R,"c6",o.9C)};h(!R||R.1j==K){o.8A()}}})();c.5h=K};1i.1n.8B=v(H){v 7j(e){e=e||1p.2V;k 4p=c.C.1d(\'1g-H-\',\'\'),8C=ae(4p),4m=A,3F=P[4p].3F;h(3F){P[4p].3F=A;u};h(e.1v=="6l"){P[4p].3F=K;k 1u=e.7i!=1a?e.7i:e.a9;h(1u!=13)u K;h(8C){c.G=c.G.1d("dp-4Z-3R","");6g();u 1O(e)};4m=K}I{P[4p].3F=A};c.G=c.G.1d("dp-4Z-3R","");h(!8C){c.G+=" dp-4Z-3R";6g(4p);8D(4p,4m)}I{6g()};u 1O(e)};H.3m=7j;H.4i=7j;h(!7F||c.8E===A){H.2W(!/*@1L!@*/A?"3f":"5Z","-1");H.3f=-1;H.3m=1a;1A(H,"6l",7j)}I{H.2W(!/*@1L!@*/A?"3f":"5Z",c.8E);H.3f=c.8E}};1i.1n.ad=v(){h(c.1P||q.1l("1g-H-"+c.C)){u};k 8F=q.1l(c.C),1k=q.1J(\'1k\'),H=q.1J(\'a\');H.c7="#"+c.C;H.G="B-1x-c8";H.22=1D(5);H.C="1g-H-"+c.C;1k.17(q.1X(3W));H.17(1k);1k=q.1J(\'1k\');1k.G="1g-80-81";1k.17(q.1X(H.22));H.17(1k);3r(H,"4Z");2r(H,"c9",K);h(c.2R&&q.1l(c.2R)){q.1l(c.2R).17(H)}I{8F.2g.9W(H,8F.9X)};c.8B(H);H=1a;c.3y("ca",{C:c.C})};1i.1n.af=v(){u c.1B};1i.1n.8G=v(2P){c.1T=(15(2P).J(/^(\\d\\d\\d\\d)(0[1-9]|1[5L])(0[1-9]|[12][0-9]|3[2S])$/)==-1)?A:2P;h(!c.2Z)c.3a()};1i.1n.8H=v(2P){c.1U=(15(2P).J(/^(\\d\\d\\d\\d)(0[1-9]|1[5L])(0[1-9]|[12][0-9]|3[2S])$/)==-1)?A:2P;h(!c.2Z)c.3a()};1i.1n.ag=v(6m){h(!6m.16||6m.16!=7||6m.5f("").J(/^([0|1]{7})$/)==-1){h(3o){43"cb ce cf cg ch 6n 9i ag";};u A};c.3A=6m;h(!c.2Z)c.3a()};1i.1n.8I=v(2x){c.2k={};c.7k(2x)};1i.1n.8J=v(2x){c.25={};c.7l(2x)};1i.1n.7k=v(2x){c.25=A;c.2k=c.2k||{};k 1q;Z(1q 18 2x){h((15(1q).J(5e)!=-1&&2x[1q]==1)||(15(1q).J(2y)!=-1&&15(2x[1q]).J(2y)!=-1)){c.2k[1q]=2x[1q]}};h(!c.2Z)c.3a()};1i.1n.7l=v(2x){c.2k=A;c.25=c.25||{};k 1q;Z(1q 18 2x){h((15(1q).J(5e)!=-1&&2x[1q]==1)||(15(1q).J(2y)!=-1&&15(2x[1q]).J(2y)!=-1)){c.25[1q]=2x[1q]}};h(!c.2Z)c.3a()};1i.1n.8K=v(1G){h(15(1G).J(5e)==-1){u A};k 1W=1G.1W(2y),dt=L 14(+1W[2],+1W[3]-1,+1W[4]);h(!dt||7m(dt)||!c.6o(dt)){u A};c.1B=L 14(dt);h(!c.2Z)c.2e();c.3y("6h",c.5l());c.6i()};1i.1n.9Q=v(){h(c.1B&&!c.6o(c.1B)){c.1B=1a};h(!c.2Z)c.2e()};1i.1n.a6=v(){h(c.5V||c.1S){u};2j(q,"7n",c.3m);2j(q,"4Q",c.2D);/*@1L@h(@2U)1A(q,"7n",c.3m);2j(q,"6l",c.3m);@23@*/h(1p.72){1A(q,"7n",c.3m);2j(q,"6l",c.3m)};c.1S=A;c.5V=K};1i.1n.73=v(){h(!c.5V){u};1A(q,"7n",c.3m);1A(q,"6l",c.3m);1A(q,"4Q",c.2D);c.5V=A};1i.1n.3l=v(){c.5S=A;1p.66(c.5U)};1i.1n.8n=v(7o){c.Q.1b.45=7o/7p;c.Q.1b.ci=\'cj(45=\'+7o+\')\';c.45=7o};1i.1n.8b=v(y,m){k 4e=21(m-1,y),1s={},8L=c.8M(y,m,A),8N=c.8M(y,m,K),2Q=y+1h(m);Z(k i=1;i<=4e;i++){dt=2Q+""+1h(i);h(8L){1s[dt]=(dt 18 8L)?0:1}I h(8N){1s[dt]=(dt 18 8N)?1:0}I{1s[dt]=1}};u 1s};1i.1n.8M=v(y,m,5v){k 7q=5v?c.25:c.2k;h(!7q){u A};m=1h(m);k 1s={},7r=c.4K,7s=c.4M,4q,4r,8O,8P;h(!7s||!7r){7r=c.4K=y+1h(m)+"2S";7s=c.4M=y+1h(m)+1h(21(m,y))};Z(dt 18 7q){4q=dt.1d(/^(\\*\\*\\*\\*)/,y).1d(/^(\\d\\d\\d\\d)(\\*\\*)/,"$1"+m);4r=7q[dt];h(4r==1){1s[4q]=1;3s};h(2o(4q.V(0,6))==+15(c.4K).V(0,6)&&2o(4r.V(0,6))==+15(c.4M).V(0,6)){h(2o(4q.V(0,6))==2o(4r.V(0,6))){Z(k i=4q;i<=4r;i++){1s[i]=1};3s};8O=2o(4q.V(0,6))==+15(c.4K).V(0,6)?4q:7r;8P=2o(4r.V(0,6))==+15(c.4M).V(0,6)?4r:7s;Z(k i=+8O;i<=+8P;i++){1s[i]=1}}};u 1s};1i.1n.9F=v(1o){k 3d=c.ah(1o);h(5b){u 3d};k 8Q=(q.6P&&q.6P!="9G")?q.3q:q.3b,ai=q.8R?8Q.3N:1p.9E,aj=q.8R?8Q.3O:1p.7R,8S=c.ak(1o);u[3d[0]-8S[0]+ai,3d[1]-8S[1]+aj]};1i.1n.ak=v(1o){k t=0,l=0;do{t+=1o.3O||0;l+=1o.3N||0;1o=1o.2g}2m(1o);u[l,t]};1i.1n.ah=v(1o){k t=0,l=0;do{t+=1o.ck||0;l+=1o.cl||0;1o=1o.cn}2m(1o);u[l,t]};1i.1n.a1=v(){k 8T=A,1t;Z(k i=c.1T;i<=c.1U;i++){1t=15(i);h(!c.3A[L 14(1t.V(0,4),1t.V(6,2),1t.V(4,2)).3x()-1]){8T=K;1e}};h(!8T){c.3A=[0,0,0,0,0,0,0]}};1i.1n.3i=v(1t){h(!c.1T&&!c.1U){u A};k 7t=A;h(!1t){7t=K;1t=c.B};k d=1h(1t.1C()),m=1h(1t.1f()+1),y=1t.1c(),dt=15(y)+15(m)+15(d);h(c.1T&&+dt<+c.1T){h(!7t){u K};c.B=L 14(c.1T.V(0,4),c.1T.V(4,2)-1,c.1T.V(6,2),5,0,0);u A};h(c.1U&&+dt>+c.1U){h(!7t){u K};c.B=L 14(c.1U.V(0,4),c.1U.V(4,2)-1,c.1U.V(6,2),5,0,0)};u A};1i.1n.6o=v(1t){h(!1t)u A;k d=1h(1t.1C()),m=1h(1t.1f()+1),y=1t.1c(),dt=15(y)+15(m)+15(d),dd=c.8b(y,m),al=1t.3x()==0?7:1t.3x();h((c.1T&&+dt<+c.1T)||(c.1U&&+dt>+c.1U)||(dd[dt]==0)||c.3A[al-1]){u A};u K};1i.1n.4Y=v(4s){2m(c.2t.26){c.2t.3g(c.2t.26)};h(4s&&c.3C.J(/-S|S-/)!=-1&&4s.J(/([0-9]{1,2})(6p|3E|6q|1V)/)!=-1){4s=4s.1d(/([0-9]{1,2})(6p|3E|6q|1V)/,"$1<4t>$2</4t>").5O(/<4t>|<\\/4t>/);k dc=q.co();Z(k i=0,3E;3E=4s[i];i++){h(/^(6p|3E|6q|1V)$/.5H(3E)){k 4t=q.1J("4t");4t.17(q.1X(3E));dc.17(4t)}I{dc.17(q.1X(3E))}};c.2t.17(dc)}I{c.2t.17(q.1X(4s?4s:1D(9)))}};1i.1n.3a=v(){k cp=c.1B,m=A,dt,1z,R,4u,d,y,7u;c.1B=1a;Z(1z 18 c.2l){R=q.1l(1z);h(!R){u};7u=15(R.1w);4u=c.2l[1z];dt=A;h(!(7u=="")){Z(k i=0,1I;1I=c.4G[1z][i];i++){dt=52(7u,1I);h(dt){1e}}};h(dt){h(4u.J(L 2K(\'[\'+7G+\']\'))!=-1){d=dt.1C()};h(4u.J(L 2K(\'[\'+7H+\']\'))!=-1){m=dt.1f()};h(4u.J(L 2K(\'[\'+7I+\']\'))!=-1){y=dt.1c()}}};dt=A;h(d&&!(m===A)&&y){h(+d>21(+m,+y)){d=21(+m,+y);dt=A}I{dt=L 14(+y,+m,+d)}};h(!dt||7m(dt)){k 6r=L 14(y||L 14().1c(),!(m===A)?m:L 14().1f(),1);c.B=c.1E?L 14(+c.1E.V(0,4),+c.1E.V(4,2)-1,+c.1E.V(6,2)):L 14(6r.1c(),6r.1f(),1M.3I(+d||L 14().1C(),21(6r.1f(),6r.1c())));c.B.6K(5);c.3i();c.3y("6h",c.5l());c.2e();u};dt.6K(5);c.B=L 14(dt);c.3i();h(dt.5D()==c.B.5D()&&c.6o(c.B)){c.1B=L 14(c.B)};c.3y("6h",c.5l());h(c.5h)c.2e();c.6i(K)};1i.1n.am=v(R,ao){Z(k 6s=R.E.16-1;6s>=0;6s--){h(R.E[6s].1w==ao){R.cq=6s;u}}};1i.1n.6i=v(1S){h(!c.1B){u};k d=1h(c.1B.1C()),m=1h(c.1B.1f()+1),y=c.1B.1c(),U=A,1z,R,4u,7v;1S=!!1S;Z(1z 18 c.2l){R=q.1l(1z);h(!R)u;h(!U)U=R;4u=c.2l[1z];7v=3h(c.1B,4u,6A);h(R.24.2a()=="5w"){R.1w=7v}I{c.am(R,7v)}};h(c.1P){c.1S=K;c.2e();c.1S=A};h(c.5h){h(U.1v&&U.1v!="3c"&&!1S){U.61()}}};1i.1n.8A=v(){h(c.1j)u;h(c.1P){c.73();c.5n();c.1S=K;c.Q.G=c.Q.G.1d(/dp-1j/,"")+" dp-1j";c.1y.4R=c.1y.4i=c.1y.4S=c.1y.2D=1a;1A(q,"4Q",c.2D);1A(q,"5t",c.4T)}I{h(c.2X)c.3S();k H=q.1l("1g-H-"+c.C);h(H){H.G=H.G.1d(/dp-1j/,"")+" dp-1j";2r(H,"1j",K);H.3m=H.4i=v(){u A};H.2W(!/*@1L!@*/A?"3f":"5Z","-1");H.3f=-1}};66(c.5U);c.1j=K};1i.1n.ap=v(){h(!c.1j)u;h(c.1P){c.5n();c.1S=K;c.2e();c.Q.G=c.Q.G.1d(/dp-1j/,"");c.1j=A;c.1y.4R=c.4R;c.1y.4S=c.4S;c.1y.4i=c.4i;c.1y.2D=c.2D}I{k H=q.1l("1g-H-"+c.C);h(H){H.G=H.G.1d(/dp-1j/,"");2r(H,"1j",A);c.8B(H)}};c.1j=A};1i.1n.6W=v(){k 2f=L 14();c.6c.G=c.6c.G.1d("1g-1j","");h(c.3i(2f)||(c.B.1C()==2f.1C()&&c.B.1f()==2f.1f()&&c.B.1c()==2f.1c())){c.6c.G+=" 1g-1j"}};1i.1n.7a=v(){k 6a=c.1Y?8:7,5x=c.1Y?1:0,d,H;Z(k 1m=5x;1m<6a;1m++){d=(c.2M+(1m-5x))%7;c.2w[1m].22=6k(d,A);h(1m>5x){H=c.2w[1m].1N("1k")[0];2m(H.26){H.3g(H.26)};H.17(q.1X(6k(d,K)));H.22=c.2w[1m].22;H.G=H.G.1d(/2d-([0-6])/,"")+" 2d-"+d;H=1a}I{2m(c.2w[1m].26){c.2w[1m].3g(c.2w[1m].26)};c.2w[1m].17(q.1X(6k(d,K)))};c.2w[1m].G=c.2w[1m].G.1d(/B-1x-71/g,"");h(c.4N[d]){c.2w[1m].G+=" B-1x-71"}};h(c.2L){c.2e()}};1i.1n.3y=v(1v,aq){h(!1v||!(1v 18 c.5j)){u A};k 8U=A;Z(k 7w=0;7w<c.5j[1v].16;7w++){8U=c.5j[1v][7w](aq||c.C)};u 8U};1i.1n.84=v(1t){h(!c.5y){u};k 53=1t.1f(),54=1t.1c();h(c.3i(L 14((54-1),53,21(+53,54-1)))){h(c.5y.G.J(/1g-1j/)==-1){c.5y.G+=" 1g-1j"};h(c.3L==-1)c.3l()}I{c.5y.G=c.5y.G.1d(/1g-1j/g,"")};h(c.3i(L 14(54,(+53-1),21(+53-1,54)))){h(c.6b.G.J(/1g-1j/)==-1){c.6b.G+=" 1g-1j"};h(c.2Y==-1)c.3l()}I{c.6b.G=c.6b.G.1d(/1g-1j/g,"")};h(c.3i(L 14((54+1),+53,1))){h(c.6d.G.J(/1g-1j/)==-1){c.6d.G+=" 1g-1j"};h(c.3L==1)c.3l()}I{c.6d.G=c.6d.G.1d(/1g-1j/g,"")};h(c.3i(L 14(54,+53+1,1))){h(c.6e.G.J(/1g-1j/)==-1){c.6e.G+=" 1g-1j"};h(c.2Y==1)c.3l()}I{c.6e.G=c.6e.G.1d(/1g-1j/g,"")}};k 3k={4v:["cr","cs","ct","cu","ar","cv","cw","cx","cz","cA","cB","cC"],4w:["cD","cE","cF","cG","ar","cH","cI","cJ","cK","cL","cM","cN"],4x:["cO","cP","cQ","cR","cS","cT","cU"],3Q:["cV","cW","cX","cY","cZ","d0","d1"],2F:["as 1Q","at 1Q","as 2p","at 2p","au","d2 d3","d4","av [[%0%]] 2O [[%1%]]","av","d5 a B","d6 \\d7 d8 6n d9","da \\db[[%0%]]\\de df","dg 6n au\\dh B","di B :"],2M:0,7x:A};k dj=v(){h(!8V.16){u[]}k 7y=[];Z(k i=0;i<8V.16;i++){Z(k j=0,8W;8W=8V[i][j];j++){7y[7y.16]=8W}};u 7y};k 8X=v(){k dp,8Y;Z(dp 18 P){Z(8Y 18 P[dp].2l){h(!q.1l(8Y)){P[dp].4P();P[dp]=1a;6t P[dp];1e}}}};k 6g=v(8Z){k dp;Z(dp 18 P){h(!P[dp].2L||(8Z&&8Z==P[dp].C))3s;P[dp].3S()}};k aw=v(O){h(O 18 P){h(!P[O].2L||P[O].1P)u;P[O].3S()}};k 8D=v(O,4m){h(!(O 18 P))u A;P[O].46=!!!4m;P[O].8x(4m);u K};k 4P=v(e){e=e||1p.2V;h(e.dk){u};Z(dp 18 P){P[dp].4P();P[dp]=1a;6t P[dp]};P=1a;1A(1p,\'ax\',7E.4P)};k ay=v(C){h(C&&(C 18 P)){P[C].4P();P[C]=1a;6t P[C]}};k 1D=v(91,55){55=55||[];h(1Z.2F.16>91){k 4o=1Z.2F[91];h(55&&55.16){Z(k i=0;i<55.16;i++){4o=4o.1d("[[%"+i+"%]]",55[i])}};u 4o.1d(/[[%(\\d)%]]/g,"")};u""};k 6k=v(2d,7z){k 2F=1Z[7z?"3Q":"4x"];u 2F.16&&2F.16>2d?2F[2d]:""};k 9S=v(1Q,7z){k 2F=1Z[7z?"4w":"4v"];u 2F.16&&2F.16>1Q?2F[1Q]:""};k 21=v(6u,7A){6u=(6u+12)%12;u(((0==(7A%4))&&((0!=(7A%7p))||(0==(7A%dl))))&&6u==1)?29:[31,28,31,30,31,30,31,31,30,31,30,31][6u]};k 8y=v(Y){h(Y 18 5I){u 5I[Y]};k 92,93,dm;4X(92=L 14(Y,0,4)){2B(1C()-(6+3x())%7)};4X(93=L 14(Y,11,28)){2B(1C()+(7-3x())%7)};5I[Y]=1M.4h((93-92)/dn);u 5I[Y]};k 8d=v(y,m,d){k d=L 14(y,m,d,0,0,0);k az=d.3x();d.2B(d.1C()-(az+6)%7+3);k aA=d.aB();d.62(0);d.2B(4);u 1M.4h((aA-d.aB())/(7*dq))+1};k 3h=v(B,1I,57){h(!B||7m(B)){u""};k 3Z=1I.5O("-"),X=[],d=B.1C(),D=B.3x(),m=B.1f(),y=B.1c(),94={"1R":" ","dt":".","4A":"/","ds":"-","cc":",","d":1h(d),"D":57?1Z.3Q[D==0?6:D-1]:3k.3Q[D==0?6:D-1],"l":57?1Z.4x[D==0?6:D-1]:3k.4x[D==0?6:D-1],"j":d,"N":D==0?7:D,"w":D,"W":8d(B),"M":57?1Z.4w[m]:3k.4w[m],"F":57?1Z.4v[m]:3k.4v[m],"m":1h(m+1),"n":m+1,"t":21(m,y),"y":15(y).V(2,2),"Y":y,"S":["1V","6p","3E","6q"][d%10>3?0:(d%7p-d%10!=10)*d%10]};Z(k 2A=0,2b;2b=3Z[2A];2A++){X.2c(!(2b 18 94)?"":94[2b])};u X.5f("")};k 52=v(X,1I){k d=A,m=A,y=A,aC=L 14(),3Z=1I.1d(/-1R(-1R)+/g,"-1R").5O("-"),dr={"dt":".","4A":"/","ds":"-","cc":","},X=""+X;95:Z(k 2A=0,2b;2b=3Z[2A];2A++){h(X.16==0){u A};7M(2b){1r"1R":1r"dt":1r"4A":1r"ds":1r"cc":X=X.1d(/^(\\s|\\.|\\/|,|-){1,}/,"");1e;1r"d":1r"j":h(X.J(/^(3[2S]|[12][0-9]|0?[1-9])/)!=-1){d=+X.1W(/^(3[2S]|[12][0-9]|0?[1-9])/)[0];X=X.V(X.1W(/^(3[2S]|[12][0-9]|0?[1-9])/)[0].16);1e}I{u""};1r"D":1r"l":l=3k.4x.3T(3k.3Q);h(1Z.7x){l=l.3T(1Z.4x).3T(1Z.3Q)};Z(k i=0;i<l.16;i++){h(L 2K("^"+l[i],"i").5H(X)){X=X.V(l[i].16);3s 95}};1e;1r"N":1r"w":h(X.J(2b=="N"?/^([1-7])/:/^([0-6])/)!=-1){X=X.V(1)};1e;1r"S":h(X.J(/^(6p|3E|6q|1V)/i)!=-1){X=X.V(2)};1e;1r"W":h(X.J(/^([1-9]|[aD[0-9]|5[0-3])/)!=-1){X=X.V(X.1W(/^([1-9]|[aD[0-9]|5[0-3])/)[0].16)};1e;1r"M":1r"F":l=3k.4v.3T(3k.4w);h(1Z.7x){l=l.3T(1Z.4v).3T(1Z.4w)};Z(k i=0;i<l.16;i++){h(X.J(L 2K("^"+l[i],"i"))!=-1){X=X.V(l[i].16);m=((i+12)%12);3s 95}};u"";1r"m":1r"n":l=/^(1[5L]|0?[1-9])/;h(X.J(l)!=-1){m=+X.1W(l)[0]-1;X=X.V(X.1W(l)[0].16);1e}I{u""};1r"t":h(X.J(/2[89]|3[2S]/)!=-1){X=X.V(2);1e};1e;1r"Y":h(X.J(/^(\\d{4})/)!=-1){y=X.V(0,4);X=X.V(4);1e}I{u""};1r"y":h(X.J(/^(\\d{4})/)!=-1){y=X.V(0,4);X=X.V(4);1e}I h(X.J(/^(0[0-9]|[1-9][0-9])/)!=-1){y=X.V(0,2);y=+y<50?\'20\'+""+15(y):\'19\'+""+15(y);X=X.V(2);1e}I u"";ac:u""}};h(!(X=="")||(d===A&&m===A&&y===A)){u A};m=m===A?11:m;y=y===A?aC.1c():y;d=d===A?21(+m,+y):d;h(d>21(+m,+y)){u A};k 1t=L 14(y,m,d);u!1t||7m(1t)?A:1t};k aE=v(1o){k 4y;h(1o.2g&&1o.2g.24.2a()=="4y")du=1o.2g;I{k 5E=q.1N(\'4y\');Z(k 58=0;58<5E.16;58++){h((5E[58][\'aF\']&&5E[58][\'aF\']==1o.C)||(5E[58].7K(\'Z\')==1o.C)){4y=5E[58];1e}}};h(4y&&!4y.C){4y.C=1o.C+"dv"};u 4y};k 98=v(){h(2z(1p.3G)=="3V"){1Z={2F:3G.2F,4v:3G.4v,4w:3G.4w,4x:3G.4x,3Q:3G.3Q,2M:("2M"18 3G)?3G.2M:0,7x:K}}I h(!1Z){1Z=3k}};k 9a=v(){98();Z(dp 18 P){h(!P[dp].2L)3s;P[dp].2e()}};k aG=v(R){u!(!R||!R.24||!((R.24.2a()=="5w"&&(R.1v=="3X"||R.1v=="3c"))||R.24.2a()=="8z"))};k aH=v(E){98();h(!E.2l){h(3o)43"dw dx dy dz 9b dA dB";u};E.C=(E.C&&(E.C 18 E.2l))?E.C:"";E.4G={};k aI=[7G,7H,7I],6v=[0,0,0],3U,6w,9c,9d,1I,3n,dC,59,2h,2i;Z(k 1z 18 E.2l){R=q.1l(1z);h(!aG(R)){h(3o)43"9e 1o 4X 9f C 2O \'"+1z+"\' dD 2O 44 dE 1v dF dG 3K dH 9b 44 dI";u A};h(!E.C)E.C=1z;1I=E.2l[1z];h(!(1I.1W(5d))){h(3o)43"9e 1o 4X 9f C 2O \'"+1z+"\' aJ 44 aK aL B 7B aM 6n aN: "+1I;u A};3n=[1I];h(E.6x&&(1z 18 E.6x)&&E.6x[1z].16){59=[];Z(k f=0,6y;6y=E.6x[1z][f];f++){h(!(6y.1W(5d))){h(3o)43"9e 1o 4X 9f C 2O \'"+1z+"\' aJ 44 aK aL B 7B aM 6n aN 9b 44 6x dJ: "+6y;u A};59.2c(6y)};3n=3n.3T(59)};3U=[0,0,0];Z(k i=0,5F;5F=aI[i];i++){h(1I.J(L 2K(\'(\'+5F+\')\'))!=-1){6v[i]=3U[i]=1;h(R.24.2a()=="5w"){6w=1I.1W(L 2K(\'(\'+5F+\')\'))[0];9c=15(6w+"|"+5F.1d(L 2K("("+6w+")"),"")).1d("||","|");9d=9c.5O("|");59=[];Z(k z=0,9g;9g=3n[z];z++){Z(k x=0,7C;7C=9d[x];x++){h(7C==6w)3s;59.2c(9g.1d(L 2K(\'(\'+5F+\')(-|$)\',\'g\'),7C+"-").1d(/-$/,""))}};3n=3n.3T(59)}}};E.4G[1z]=3n.3T();h(R.24.2a()=="8z"){2h=2i=0;k 5a=R.E;h(3U[0]&&3U[1]&&3U[2]){k 1G,1E=A;h("2k"18 E){6t(E.2k)};E.25={};Z(i=0;i<5a.16;i++){Z(k f=0,1I;1I=3n[f];f++){dt=52(5a[i].1w,1I);h(dt){1G=dt.1c()+""+1h(dt.1f()+1)+""+1h(dt.1C());h(!1E)1E=1G;E.25[1G]=1;h(!2h||2o(1G)<2h){2h=1G};h(!2i||2o(1G)>2i){2i=1G};1e}}};h(!E.1E&&1E)E.1E=1E}I h(3U[1]&&3U[2]){k 4g;Z(i=0;i<5a.16;i++){Z(k f=0,1I;1I=3n[f];f++){dt=52(5a[i].1w,1I);h(dt){4g=dt.1c()+""+1h(dt.1f()+1);h(!2h||2o(4g)<2h){2h=4g};h(!2i||2o(4g)>2i){2i=4g};1e}}};2h+=""+"2S";2i+=""+21(+2i.V(4,2)-1,+2i.V(0,4))}I h(3U[2]){k 3t;Z(i=0;i<5a.16;i++){Z(k f=0,1I;1I=3n[f];f++){dt=52(5a[i].1w,1I);h(dt){3t=dt.1c();h(!2h||2o(3t)<2h){2h=3t};h(!2i||2o(3t)>2i){2i=3t};1e}}};2h+="dK";2i+="dL"};h(2h&&(!E.1T||(+E.1T<+2h)))E.1T=2h;h(2i&&(!E.1U||(+E.1U>+2h)))E.1U=2i}};h(!(6v[0]&&6v[1]&&6v[2])){h(3o)43"5R 3K dM 8R 2O 44 dN B 3Z Z 1o: "+R.C;u A};k 5G={2l:E.2l,C:E.C,4G:E.4G,1P:!!(E.1P),2R:E.2R&&q.1l(E.2R)?E.2R:"",1T:E.1T&&15(E.1T).J(2y)!=-1?E.1T:"",1U:E.1U&&15(E.1U).J(2y)!=-1?E.1U:"",3C:E.3C&&15(E.3C).J(5d)!=-1?E.3C:"",8o:!!(E.1P)?K:!!(E.8o),8j:5J||!!(E.1P)?K:!!(E.8j),7D:E.7D&&2z E.7D==\'7S\'?2N(E.7D,10):0,4k:E.4k&&2z E.4k==\'7S\'&&(E.4k>20&&E.4k<=7p)?1M.3I(+E.4k,99):(!!(E.1P)?99:90),8l:!!(E.8l),63:!!(E.dO),1Y:!!(E.1Y),5q:!!(E.5q),4L:"4L"18 E?!!(E.4L):K,1E:E.1E&&15(E.1E).J(2y)!=-1?E.1E:"",8i:aE(R),2G:(E.2G&&q.1l(E.2G))?E.2G:2G&&q.1l(2G)?2G:"",5j:E.aO?E.aO:{},4N:E.4N&&E.4N.16&&E.4N.16==7?E.4N:[0,0,0,0,0,1,1],3A:E.3A&&E.3A.16&&E.3A.16==7?E.3A:[0,0,0,0,0,0,0]};h(E.2k){h(E.25)6t(E.25);5G.2k={};k 1q;Z(1q 18 E.2k){h((15(1q).J(5e)!=-1&&E.2k[1q]==1)||(15(1q).J(2y)!=-1&&15(E.2k[1q]).J(2y)!=-1)){5G.2k[1q]=E.2k[1q]}}}I h(E.25){k 1q;5G.25={};Z(1q 18 E.25){h((15(1q).J(5e)!=-1&&E.25[1q]==1)||(15(1q).J(2y)!=-1&&15(E.25[1q]).J(2y)!=-1)){5G.25[1q]=E.25[1q]}}};P[E.C]=L 1i(5G);P[E.C].3y("74",P[E.C].5l())};k ae=v(C){u(!C||!(C 18 P))?A:P[C].2X};2j(1p,\'ax\',4P);u{2j:v(1s,1v,2n){u 2j(1s,1v,2n)},1A:v(1s,1v,2n){u 1A(1s,1v,2n)},1O:v(e){u 1O(e)},8x:v(O){u 8D(O,A)},3S:v(O){u aw(O)},dP:v(E){aH(E)},dQ:v(O){ay(O)},8X:v(){8X()},3h:v(dt,1I,57){u 3h(dt,1I,57)},3a:v(O){h(!O||!(O 18 P))u A;P[O].3a()},8G:v(O,1G){h(!O||!(O 18 P)){u A};P[O].8G(1G)},8H:v(O,1G){h(!O||!(O 18 P)){u A};P[O].8H(1G)},52:v(X,7B){u 52(X,7B)},dR:v(3p){7J(3p)},8K:v(O,1G){h(!O||!(O 18 P)){u A};P[O].8K(1G)},dS:v(O,dt){h(!O||!(O 18 P))u A;u P[O].6o(dt)},7k:v(O,2Q){h(!O||!(O 18 P))u A;P[O].7k(2Q)},8I:v(O,2Q){h(!O||!(O 18 P))u A;P[O].8I(2Q)},7l:v(O,2Q){h(!O||!(O 18 P))u A;P[O].7l(2Q)},8J:v(O,2Q){h(!O||!(O 18 P))u A;P[O].8J(2Q)},dT:v(O){h(!O||!(O 18 P))u A;P[O].8A()},dU:v(O){h(!O||!(O 18 P))u A;P[O].ap()},83:v(O,1G){h(!O||!(O 18 P))u A;P[O].83(1G)},dV:v(O){u(!O||!(O 18 P))?A:P[O].af()},9a:v(){9a()},dW:v(aP){3o=!!(aP)}}})();',62,865,'||||||||||||this|||||if|||var||||||document||||return|function|||||false|date|id||options||className|but|else|search|true|new|||inpID|datePickers|div|elem||td|el|substr||str||for|||||Date|String|length|appendChild|in||null|style|getFullYear|replace|break|getMonth|fd|pad|datePicker|disabled|span|getElementById|col|prototype|element|window|startD|case|obj|tmpDate|kc|type|value|picker|table|elemID|removeEvent|dateSet|getDate|getTitleTranslation|cursorDate|iePopUp|yyyymmdd|row|fmt|createElement|tr|cc_on|Math|getElementsByTagName|stopEvent|staticPos|month|sp|noFocus|rangeLow|rangeHigh|th|match|createTextNode|showWeeks|localeImport||daysInMonth|title|end|tagName|enabledDates|firstChild||||toLowerCase|part|push|day|updateTable|today|parentNode|myMin|myMax|addEvent|disabledDates|formElements|while|fn|Number|year|statusText|setARIAProperty|cName|statusBar|prev|next|ths|dateObj|rangeRegExp|typeof|pt|setDate|currentStub|onmousedown|details|titles|describedBy|err|try|catch|RegExp|created|firstDayOfWeek|parseInt|of|range|dts|positioned|01|script|_win32|event|setAttribute|visible|monthInc|inUpdate|||||||||||setDateFromInput|body|hidden|pos|px|tabIndex|removeChild|printFormattedDate|outOfRange|currentDate|localeDefaults|stopTimer|onkeydown|fmtBag|debug|json|documentElement|setARIARole|continue|yyyy|display|hover|out|getDay|callback|curr|disabledDays|onmousewheel|statusFormat|rows|nd|kbEvent|fdLocale|languageTag|min|top|not|yearInc|opacityTo|scrollLeft|scrollTop|spnC|dayAbbrs|active|hide|concat|tmpPartsFound|object|nbsp|text|mousewheel|parts||||throw|the|opacity|clickActivated|isIE7|||left|selectable|cellAdded|firstColIndex|dpm|weekDay|yyyymm|round|onclick|cols|finalOpacity|cellDate|autoFocus|butt|txt|inpId|dt1|dt2|msg|sup|elemFmt|fullMonths|monthAbbrs|fullDays|label|titleFormat|sl|scriptFiles|fullParts|tmpParts|parentWindow|thing|formatMasks|_jscript_version|visibility|spn|firstDateShown|constrainSelection|lastDateShown|highlightDays|timerInc|destroy|mousedown|onmouseover|onmouseout|clearTimer|colspan|tableHead|drag|with|updateStatus|button||currentTR|parseDateString|tdm|tdy|replacements||useImportedLocale|lbl|newFormats|selOptions|isOpera|placeholder|validFmtRegExp|wcDateRegExp|join|on|fullCreate|interval|callbacks|prop|createCbArgObj|offsetHeight|removeOldFocus|setNewFocus|lm|fillGrid|wkThs|header|mouseup|dragEnabledCN|enabled|input|colOffset|butPrevYear|fade|target|origEl|delta|getTime|labelList|testPart|opts|test|weeksInYearCache|nodrag|formatParts|012|lang|bases|split|addEventListener|JSON|Could|timerSet|fadeTimer|timer|kbEventsAdded|selectedTD|delayedUpdate|osh|tabindex|ymd|focus|setMonth|noToday|stub|titleBar|clearTimeout|createTH|unselectable|tableBody|colspanTotal|butPrevMonth|butToday|butNextYear|butNextMonth|mouseDownElem|hideAll|dateset|returnFormattedDate|inc|getDayTranslation|keydown|dayArray|to|canDateBeSelected|st|rd|newDate|opt|delete|nMonth|partsFound|matchedPart|dateFormats|bDft|languageInfo|returnLocaleDate|mouseWheel|cellFormat|head|src|html|string|describedby|parse|dayInc|setHours|mm|block|osw|trueBody|compatMode|abs|onblur|onfocus|unused|no|setTimeout|disableTodayButton|bespokeRenderClass|nm|currentYYYYMM|selected|highlight|devicePixelRatio|removeOnFocusEvents|create|thClassName|none|tableFoot|datepicker|tbody|updateTableHeaders|pageX|clientX|pageY|clientY|incs|check|dateYYYYMM|keyCode|buttonEvent|addDisabledDates|addEnabledDates|isNaN|keypress|op|100|deDates|lower|upper|level|elemVal|fmtDate|func|imported|nodeList|abbreviation|nYear|format|indPart|bespokeTabindex|datePickerController|buttonTabIndex|dParts|mParts|yParts|affectJSON|getAttribute|key|switch|removeEventListener|role|mx|my|pageYOffset|number|reposition|0px|offsetWidth|sOffsets|width|height|selection|screen|reader|cloneNode|setCursorDate|showHideButtons|dateSetD|weekDayC|daySub|stubN||stubP|getDates|tds|getWeekNumber|week|mouseout|DOMMouseScroll|createThAndButton|labelledBy|dragDisabled|presentation|hideInput|diff|setOpacity|noFadeEffect|trackDrag|diffx|diffy|stopDrag|srcElement|hideDP|800|cnt|show|getWeeksInYear|select|disableDatePicker|addButtonEvents|dpVisible|showDatePicker|bespokeTabIndex|inp|setRangeLow|setRangeHigh|setDisabledDates|setEnabledDates|setSelectedDate|dds|getGenericDates|eds|rngLower|rngUpper|iebody|all|posReal|clearDayFound|ret|arguments|item|cleanUp|fe|exception||num|X1|X2|flags|loopLabel|||updateLanguage||loadLanguage|within|newParts|indParts|The|and|bFmt|Object|call|opera|isMoz|navigator|userAgent|parseUILanguage|scriptInner|parseJSON|loc|javascript|buttontabindex|cellformat|parseCellFormat|titleformat|attachEvent|detachEvent|stopPropagation|Function|parent|property|changeHandler|getScrollOffsets|pageXOffset|truePosition|BackCompat|fitsBottom|fitsTop|clientWidth|addAccessibleDate|noS|setFullYear|abbr|weekDayN|weekDayP|checkSelectedDate|mmdd|getMonthTranslation|resizeInlineDiv|locate|associated|insertBefore|nextSibling|tfoot|thead|scope|equaliseDates|mousemove|zIndex|reg|previousSibling|addOnFocusEvents|wheelDelta|detail|charCode|ctrlKey|nodeType|default|createButton|isVisible|returnSelectedDate|setDisabledDays|cumulativeOffset|dsocleft|dsoctop|realOffset|wd|setSelectIndex||indx|enableDatePicker|args|May|Previous|Next|Today|Week|hideDatePicker|unload|destroySingleDatePicker|DoW|ms|valueOf|now|1234|findLabelForElement|htmlFor|checkElem|addDatePicker|testParts|has|following|incorrect|assigned|it|callbackFunctions|dbg|toString|Opera|mozilla|compatible|webkit|uniqueId|fromCharCode|160|dividors|dvParts|innerHTML|lastIndexOf|js|charSet|utf|base|childNodes|xml|en|0000|preventDefault|cancelBubble|returnValue|self|Array|Image|ActiveXObject|gim|bfunction|aria|cursorTD|defaults|dynDisabledDates|maxHeight|undefined|clientHeight|noCallback|redraw|firstDateDisplayed|lastDateDisplayed|floor|mouseover|colSpan|labelledby|datePickerTable|iframe|iehack|scrolling|frameBorder|name|iePopUpHack|static|300|grid|statusbar|u00AB|u2039|u203A|u00BB|gridcell|columnheader|rowheader|trs|weeks|displayed|domcreate|9999|accellerator|1200|10000|120|version|104|tmp|cursorYYYYMM|toElement|relatedTarget|nodeValue|change|href|control|haspopup|dombuttoncreate|Invalid|||values|located|when|attempting|filter|alpha|offsetTop|offsetLeft||offsetParent|createDocumentFragment|origDateSet|selectedIndex|January|February|March|April|June|July|August||September|October|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Mon|Tue|Wed|Thu|Fri|Sat|Sun|Show|Calendar|wk|Select|Click|u0026|Drag|move|Display|u201C|||u201D|first|Go|u2019s|Disabled|joinNodeLists|persisted|400|NW|604800000|||864e5|divds|||lebel|_label|No|form|elements|stipulated|initialisation|parameters|fmtParts|is|wrong|or|does|exist|DOM|parameter|0101|1231|find|required|noTodayButton|createDatePicker|destroyDatePicker|setGlobalVars|dateValidForSelection|disable|enable|getSelectedDate|setDebug'.split('|'),0,{}))/**
 * Text editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */
var TextEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.createInput();

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    destroy: function() {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                if (Prototype.Browser.IE ? event.ctrlKey : event.altKey) {
                    this.switchTo("multiline");
                }
                break;
        }
    }
});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["text"] = TextEditor;
}
/**
 * Multiline editor.
 *
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */
var MultiLineEditor = Class.create(BaseTextEditor, {
    // Special flag, prevents closing on pressing enter
    __do_nothing_on_enter: true,

    editor_initialize: function() {
        this.createInput();    

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    createInput: function() {
        this.input = new Element("textarea");

        this.setDefaultStyle();
        this.input.setStyle(this.style);
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                if (event.ctrlKey) this.doneEdit();
                break;

            default:
                if (this.maxInputSize && this.input.value.length >= this.maxInputSize) {
                    if (event.charCode != undefined && event.charCode != 0)
                        Event.stop(event);
                }
                break;
        }
    },

    destroy: function() {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
    },

    getValue: function() {
        var res = this.input.value;
        return res.gsub("\r\n", "\n").replace(/\n$/, "");
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["multiline"] = MultiLineEditor;
}
/**
 * Numeric editor.
 * Extends base text editor to restrict input values to numeric values only. Supports min/max constraints.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrey Naumenko
 */
var NumericEditor = Class.create(BaseTextEditor, {

    min: null,
    max: null,

    editor_initialize: function(param) {
        this.createInput();

        var self = this;

        this.input.onkeypress = function(event) {return self.keyPressed(event || window.event)}

        if (param) {
            this.min = param.min;
            this.max = param.max;
        }
    },

    isValid: function(value) {
        var n = Number(value);
        var invalid = isNaN(n) || (this.min && n < this.min) || (this.max && n > this.max);
        return !invalid;
    },

    keyPressed: function(event) {
        var v = this.input.getValue();
        if (event.charCode == 0) return true;
        var code = event.charCode == undefined ? event.keyCode : event.charCode;

        if (code == 45)  // minus
            return v.indexOf("-") < 0;
        if (code == 46)  // point
            return v.indexOf(".") < 0;

        return code >= 48 && code <= 57; // digits (0-9)
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["numeric"] = NumericEditor;
}

/**
 * Base dropdown editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Aliaksandr Antonik
 */
var DropdownEditor = Class.create(BaseEditor, {
    /**
     * Constructor. Creates "select" HTML element and fills it with "option"s from param parameter.
     * @param param an Enumeration with options for this dropdown. 
     */
    editor_initialize: function(param) {
        this.createInput();

        this.addOption("", "");
        var pc = param.choices;
        var pd = param.displayValues;
        var len = Math.min(pc.length, pd.length);

        for (var ind = 0; ind < len; ++ind) {
            this.addOption(pc[ind], pd[ind]);
        }
    },

    createInput: function() {
        this.input = new Element("select");

        // Default styles
        this.input.style.border = "1px solid threedface";
        this.input.style.margin = "0px";
        this.input.style.padding = "0px";
        this.input.style.width = "101%";

        this.input.setStyle(this.style);
    },

    /**
     *  Add an option element to this select
     */
    addOption : function(value, name) {
        var optionElement = new Element("option");
        optionElement.value = value;
        optionElement.innerHTML = name;
        this.input.appendChild(optionElement);
    }

});

if (BaseEditor.isTableEditorExists()) {
	TableEditor.Editors["combo"] = DropdownEditor;
}/**
 * Formula editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */
var FormulaEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.createInput();

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.input, "keypress", this.eventHandler);
    },

    destroy: function() {
        Event.stopObserving(this.input, "keypress", this.eventHandler);
    },

    handleKeyPress: function (event) {
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["formula"] = FormulaEditor;
}
/**
 * Boolean editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */
var BooleanEditor = Class.create(BaseEditor, {

    editor_initialize: function() {
        this.input = new Element("input");
        this.input.setAttribute("type", "checkbox");
    },

    getValue: function() {
        return this.input.checked;
    },

    setValue: function(value) {
        this.input.checked = value == "true" ? true : false;
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["boolean"] = BooleanEditor;
}
/**
 * Date editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */

var DateEditor = Class.create(BaseTextEditor, {

    editor_initialize: function() {
        this.createInput();

        this.input.style.width =    "-moz-calc(100% - 28px)";
        this.input.style.width = "-webkit-calc(100% - 28px)";
        this.input.style.width =         "calc(100% - 28px)";

        var self = this;

        this.input.onkeydown = function(event) {
            return self.keyPressed(event || window.event);
        }
        this.input.oncontextmenu = function(event) {
            return false;
        }
        this.input.onclick = function() {
            datePickerController.show(self.getId());
        };
    },

    show: function($super, value) {
        $super(value);

        var inputId = this.getId();

        var datePickerOpts = {
            formElements: {},
            noFadeEffect: true,
            finalOpacity: 100
        };
        datePickerOpts.formElements[inputId] = "m-sl-d-sl-Y";

        var datePickerGlobalOpts = {
            noDrag: true
        };

        datePickerController.setGlobalVars(datePickerGlobalOpts);

        datePickerController.createDatePicker(datePickerOpts);
    },

    destroy: function(value) {
        datePickerController.destroyDatePicker(this.getId());
    },

    keyPressed: function(event) {
    	var keyCode = event.keyCode;
        switch (keyCode) {
        	case Event.KEY_BACKSPACE:
        	case Event.KEY_DELETE:
        		this.input.value = '';
        		break;
        }
        return false;
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["date"] = DateEditor;
}
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

        buttonContainer.innerHTML = '<input type="button" value="Select All"> <input type="button" value="Done">'
        var b1 = buttonContainer.down(), b2 = b1.next();
        b1.onclick = function() {
            self.setAllCheckBoxes(this.value == "Select All");
            this.value = (this.value == "Select All" ? "Deselect All" : "Select All");
        }

        b2.onclick = function() {
            self.finishEdit();
        }

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
        this.setValue(this.combineValue());
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
}/**
 * Array editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */
var ArrayEditor = Class.create(BaseTextEditor, {

    separator: null,
    entryEditor: null,

    editor_initialize: function(param) {
        this.createInput();

        if (param) {
            this.separator = param.separator;
            this.entryEditor = param.entryEditor;
        }
    },

    isValid: function(value) {
        var valid = true;
        var self = this;
        if (self.entryEditor && value) {
            var values = value.split(this.separator);
            values.each(function(v) {
                if (!self.entryEditor.isValid(v)) {
                    valid = false;
                }
            });
        }
        return valid;
    }

});

if (BaseEditor.isTableEditorExists()) {
    TableEditor.Editors["array"] = ArrayEditor;
}
/**
 * Range editor.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Anastasia Abramova
 */
var NumberRangeEditor = Class.create(BaseTextEditor, {

    rangePanel: null,
    generalSeparator: " .. ",
    stableSeparators: ["more than ", "less than ", " and more", " - "],
    unstableSeparators: [" ... ", ";", "+"],
    dashSeparator: "-",
    currentSeparator: null,
    destroyed: null,
    entryEditor: null,
    checkboxes: null,
    values: null,
    btns: null,

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
                self.currentSeparator = self.generalSeparator;
                self.changeRange();
            }
          }
        
        for (i = 0; i < self.values.length; i++) {
            self.values[i].onkeyup = function() {
                if ((self.currentSeparator == self.stableSeparators[0]) && (this == self.values[1])) {
                    self.currentSeparator = self.generalSeparator;
                }
                if ((self.currentSeparator == self.stableSeparators[1]) && (this == self.values[0])) {
                    self.currentSeparator = self.generalSeparator;
                }
                if ((self.currentSeparator == self.stableSeparators[2]) && (this == self.values[1])) {
                    self.currentSeparator = self.generalSeparator;
                }
                
                self.changeRange();
            }
          }
        
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
        this.rangePanel.setAttribute("style", "width: 230px; height: 140px; background: white; border:1px solid gray;");
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
                if (separator == self.stableSeparators[0]) {
                    self.values[0].value = values[1];
                } else {
                    if (values[0]) {
                        self.values[0].value = values[0];
                        self.checkboxes[0].setAttribute("checked", "checked");
                    }
                    if (values[1]) {
                        self.values[1].value = values[1];
                        if (separator == " - ") {
                            self.checkboxes[1].setAttribute("checked", "checked");
                        }
                    }
                }
            }
        });
        self.unstableSeparators.each(function(separator) {
            if (value.indexOf(separator) !== -1) {
                self.currentSeparator = self.generalSeparator;
                values = self.splitValue(value, separator);
                if (separator == ";") {
                    if (values[0].charAt(0) == "[") {
                        self.checkboxes[0].setAttribute("checked", "checked");
                    }
                    if (values[1].charAt(values[1].length - 1) == "]") {
                        self.checkboxes[1].setAttribute("checked", "checked");
                    }
                    self.values[0].value = values[0].substring(1);
                    self.values[1].value = values[1].substring(0, values[1].length - 1);
                } else {
                    self.values[0].value = values[0];
                    self.checkboxes[0].setAttribute("checked", "checked");
                    if (separator == " ... ") {
                        self.values[1].value = values[1];
                        self.checkboxes[1].setAttribute("checked", "checked");
                    }
                }
            }
        });
        if ((value.indexOf(self.generalSeparator) != -1) || (value.charAt(0) == "<")||(value.charAt(0) == ">")) {
            self.currentSeparator = self.generalSeparator;
            if ((value.charAt(0) == "<")||(value.charAt(0) == ">")) {
                if (value.charAt(0) == "<") {
                    if (value.charAt(1) == "=") {
                        self.checkboxes[1].setAttribute("checked", "checked");
                        self.values[1].value = value.substring(2);
                    } else {
                        self.values[1].value = value.substring(1);
                    }
                } else {
                    if (value.charAt(1) == "=") {
                        self.checkboxes[0].setAttribute("checked", "checked");
                        self.values[0].value = value.substring(2);
                    } else {
                        self.values[0].value = value.substring(1);
                    }
                }
            } else {
                if (value.charAt(0) == "[") {
                    self.checkboxes[0].setAttribute("checked", "checked");
                }
                if (value.charAt(value.length - 1) == "]") {
                    self.checkboxes[1].setAttribute("checked", "checked");
                }
                values = self.splitValue(value.substring(1, value.length - 1), self.currentSeparator);
                self.values[0].value = values[0];
                self.values[1].value = values[1];
            }
        }
        if (!self.currentSeparator) {
            self.currentSeparator = self.dashSeparator;
            values = self.splitValue(value, self.currentSeparator);
            if (values.length == 2) {
                self.values[0].value = values[0];
                self.values[1].value = values[1];
            } else {
                self.values[0].value = -values[1];
                self.values[1].value = values[2];
            }
        }
        self.changeRange();
        Event.observe(document, 'click', self.documentClickListener);
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
                    var matchDouble = v.match(/^-?[0-9]+\.[0-9][0-9]$/);
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
            if (values[0] == values[1]) {
                result = values[0];
            } else {
                result = values.join(this.currentSeparator);
            }
        } else if (this.currentSeparator == this.stableSeparators[0]) {
            result = this.currentSeparator + values[0];
        } else if (this.currentSeparator == this.stableSeparators[1]) {
            result = this.currentSeparator + values[1];
        } else if (this.currentSeparator == this.stableSeparators[2]) {
            result = values[0] + this.currentSeparator;
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
                if (values[0]) {
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
}/**
 * Color Picker.
 * 
 * @requires Prototype v1.6.1+ library
 *  
 * @author Andrei Astrouski
 */
var ColorPicker = Class.create({

    palette: [['#FFFFFF', '#FFDDDD', '#DDFFDD', '#DDDDFF', '#FFFFAA', '#FFE4BE', '#FFDDFF', '#FFD6AF', '#E5F5FF', '#DBFFEB'],
              ['#EEEEEE', '#FFAAAA', '#AAFFAA', '#AAAAFF', '#FFFF77', '#FFC29C', '#FFBBFF', '#FFBF80', '#D2EDFF', '#C2FFDD'],
              ['#CCCCCC', '#FF6666', '#66FF66', '#6666FF', '#FFFF33', '#FF9275', '#F19CEC', '#FAA857', '#AEDEFE', '#8FFFC0'],
              ['#999999', '#FF3333', '#45F745', '#3333FF', '#FFFF00', '#FF7256', '#CD69C9', '#FF7F00', '#87CEFF', '#54FF9F'],
              ['#666666', '#EA0B0B', '#25DA25', '#2222D3', '#EEEE00', '#EE6A50', '#B23AEE', '#EE7600', '#7EC0EE', '#4EEE94'],
              ['#333333', '#AA0000', '#00AA00', '#1717AB', '#CDCD00', '#CD5B45', '#9A32CD', '#CD6600', '#6CA6CD', '#43CD80'],
              ['#000000', '#660000', '#006600', '#000066', '#8B8B00', '#8B3E2F', '#68228B', '#8B4500', '#4A708B', '#2E8B57']],

    colorPicker: null,
    documentClickListener: null,
    opened: false,

    initialize: function(id, parent, onSelect, optParams) {
        this.actionElement = $(id);
        this.onSelect = onSelect;
        this.parent = parent;
        this.optParams = optParams;

        if (this.optParams.showOn != false && !this.optParams.showOn) {
            this.showOn = 'click';
        }

        if (this.showOn) {
            this.showHandler = this.show.bindAsEventListener(this);
            Event.observe(this.actionElement, this.showOn, this.showHandler);
        }

        this.documentClickListener = this.documentClickHandler.bindAsEventListener(this);
    },

    show: function() {
        var self = this;

        if (self.optParams.onShow) {
            if (self.optParams.onShow() === false) {
                return;
            }
        }

        if (!this.opened) {
            if (!this.colorPicker) {
                this.colorPicker = this.createColorPicker();
            }

            // Show Color Picker
            this.parent.appendChild(this.colorPicker);

            if (self.optParams.onMouseOver) {
                this.colorPicker.observe("mouseover", function(e) {
                    self.optParams.onMouseOver();
                });
            }
            if (self.optParams.onMouseOut) {
                this.colorPicker.observe("mouseout", function(e) {
                    self.optParams.onMouseOut();
                });
            }

            Event.observe(document, 'click', this.documentClickListener);

            $$("#" + this.actionElement.id + "_colorPicker .cp_palette table td div").each(function(elem) {
                elem.observe("mouseover", function(e) {
                    this.addClassName("cp_selected");
                    if (self.optParams.onColorMouseOver) {
                        self.optParams.onColorMouseOver(self.toRgb(this.style.backgroundColor));
                    }
                });
                elem.observe("mouseout", function(e) {
                    this.removeClassName("cp_selected");
                    if (self.optParams.onColorMouseOut) {
                        self.optParams.onColorMouseOut();
                    }
                });
                elem.observe("click", function(e) {
                    this.removeClassName("cp_selected");
                    self.hide();
                    self.onSelect(self.toRgb(this.style.backgroundColor));
                });
            });

            this.opened = true;
        }
    },

    documentClickHandler: function(e) {
        var self = this;

        var element = Event.element(e);

        var b = false;
        if (element == this.actionElement) {
            b = true;
        } else {
            do {
                if (element == this.multiselectPanel) {
                    b = true;
                }
            } while (element = element.parentNode);
        }

        if (!b) {
            if (self.optParams.onCancel) {
                if (self.optParams.onCancel() === false) {
                    return;
                }
            }
            this.hide();
        }
    },

    getInitPosition: function() {
        var pos = Element.positionedOffset(this.actionElement);
        pos[1] += this.actionElement.getHeight();
        return pos;
    },

    createColorPicker: function() {
        var colorPickerDiv = new Element("div");

        colorPickerDiv.id = this.actionElement.id + "_colorPicker";
        colorPickerDiv.update(this.createColorPalette());

        var pos = this.getInitPosition();
        colorPickerDiv.style.left = pos[0] + "px";
        colorPickerDiv.style.top = pos[1] + "px";

        colorPickerDiv.addClassName("colorPicker");
        colorPickerDiv.addClassName("corner_all");
        colorPickerDiv.addClassName("shadow_all");

        return colorPickerDiv;
    },

    createColorPalette: function() {
        var nRows = this.palette.length;
        var nCols = this.palette[0].length;
        var paletteHtml = "<div class='cp_palette'><table>";

        for (var row = 0; row < nRows; row++) {
            paletteHtml += "<tr>";
            for (var col = 0; col < nCols; col++) {
                paletteHtml += "<td><div style='background: " + this.palette[row][col] + "'>";
                paletteHtml += "</div></td>";
            }
            paletteHtml += "</tr>";
        }

        paletteHtml += "</table></div>";

        return paletteHtml;
    },

    hide: function() {
        var self = this;

        if (self.optParams.onHide) {
            if (self.optParams.onHide() === false) {
                return;
            }
        }

        if (this.opened) {
            Event.stopObserving(document, 'click', this.documentClickListener);
            Element.remove(this.colorPicker);
            this.opened = false;
        }
    },

    toRgb: function(color) {
        // rgb
        if (color.indexOf("rgb") == 0) {
            return color;
        }

        // hex
        if (color.indexOf("#") == 0) {
            color = color.substr(1);
        }

        var triplets = /^([a-f0-9]{2})([a-f0-9]{2})([a-f0-9]{2})$/i.exec(color).slice(1);

        var red = parseInt(triplets[0], 16);
        var green = parseInt(triplets[1], 16);
        var blue = parseInt(triplets[2], 16);

        return "rgb(" + red + "," + green + "," + blue + ")";
    }

});
/**
 * Popup.
 * 
 * @requires Prototype v1.6.1+ library
 * 
 * @author Andrei Astrouski
 */
var Popup = Class.create({

    popup: null,

    initialize: function(content, left, top, params) {
        this.content = content;
        this.left = left;
        this.top = top;
        this.params = params;
    },

    show: function() {
        var self = this;

        if (!this.popup) {
            this.popup = this.createPopup();

            // Show popup
            document.body.appendChild(this.popup);
        }
    },

    createPopup: function() {
        var popupDiv = new Element("div");

        popupDiv.update(this.content);

        popupDiv.addClassName('popup');

        popupDiv.style.left = this.left + "px";
        popupDiv.style.top = this.top + "px";

        if (this.params) {
            if (this.params.width) {
                popupDiv.style.width = this.params.width;
            }
            if (this.params.height) {
                popupDiv.style.height = this.params.height;
            }
        }

        return popupDiv;
    },

    hide: function() {
        this.hide(0);
    },

    hide: function(timeout) {
        var self = this;
        if (!timeout || timeout < 0) {
            timeout = 0;
        }
        window.setTimeout(function() {
            if (self.popup) {
                document.body.removeChild(self.popup);
                self.popup = null;
            }
        }, timeout);
    },

    has: function(element) {
    	if (!this.popup || !element) {
    		return false;
    	}
        return element.descendantOf(this.popup); 
    },

    bind: function(event, handler) {
        Event.observe(this.popup, event, handler);
    },

    unbind: function(event, handler) {
        Event.stopObserving(this.popup, event, handler);
    }

});
