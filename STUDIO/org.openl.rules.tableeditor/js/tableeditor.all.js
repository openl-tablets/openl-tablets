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

        tooltipDiv.addClassName("ctooltip");
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
    menuId: null,
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
        this.cellIdPrefix = this.editorId + "_c-";
        this.menuId = this.editorId + "_menu";
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

        if (this.editable || $(this.menuId)) {
            this.tableContainer.oncontextmenu = function(e) {
                self.handleContextMenu(e);
            };
        }
    },

    toEditMode: function(cellToEdit) {
        if (this.actions && this.actions.beforeEdit && !this.actions.beforeEdit()) {
            return;
        }

        // Remove links in EDIT mode
        $$('.te-meta-info > a').each(function(item) {
            $(item).replace($(item).text);
        });


        if (!cellToEdit) {
            cellToEdit = $(PopupMenu.lastTarget);
        }

        var cellPos;
        if (cellToEdit) {
            // cellToEdit can be either a string id or an DOM element
            var cellId = (typeof cellToEdit == "string" ? cellToEdit : cellToEdit.id);
            cellPos = cellId.split(this.cellIdPrefix)[1];
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

        self.actions && self.actions.requestStart && self.actions.requestStart();

        new Ajax.Request(this.buildUrl(operation), {
            parameters: params,

            onSuccess: function(response) {
                self.handleResponse(response, successCallback);
                self.actions && self.actions.requestEnd && self.actions.requestEnd();
            },

            onFailure: function(response) {
                self.handleError(response);
                self.actions && self.actions.requestEnd && self.actions.requestEnd();
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

        var tdElt; // JavaScript doesn't have block level variables. Only function body, global object or page.
        if (row) {
            tdElt = row.down("td");
            while (tdElt) {
                this.columns += tdElt.colSpan ? tdElt.colSpan : 1;
                tdElt = tdElt.next("td");
            }
        }

        while (row) {
            tdElt = row.down("td");
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
                self.actions.afterSave({"newId": data.id});
            }
        });
    },
    
    saveChanges: function() {
        this.setCellValue();

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
        this.editor && this.editor.cancelEdit();

        this.doOperation(TableEditor.Operations.ROLLBACK, params, function() {
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
        if (elt && elt.hasClassName("title")) {
            elt = elt.parentNode;
        }
        if (this.isCell(elt)) {
            this.selectElement(elt);
            this.isFormated(elt);

        } else if (this.isToolbar(elt)) {
            // Do Nothing
        } else {
            this.tableBlur();
        }
    },
    

    isFormated: function() {

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

    handleContextMenu: function(event) {
        event.preventDefault();
        var elt = Event.element(event);
        if (elt.tagName.toLowerCase() == "td") {
            PopupMenu.sheduleShowMenu(this.menuId, event, 150);
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

        var theIndex; // JavaScript doesn't have block level variables. Only function body, global object or page.
        switch (event.keyCode) {
            case 37: case 38: // LEFT, UP
            var cell = null;
            theIndex = event.keyCode == 38 ? 0 : 1;
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
            theIndex = event.keyCode == 40 ? 0 : 1;

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
    editBegin : function(cell, response, typedText) {
        var initialValue;
        if (response.initValue) {
            initialValue = response.initValue;
        } else {
            // Get initial value from table cell
            initialValue = this.unescapeHTML(
                cell.innerHTML.replace(/<br>/ig, "\n")).strip();
        }

        var editorStyle = this.getCellEditorStyle(cell);
        
        this.showEditorWrapper(cell);

        this.showCellEditor(response.editor, this.editorWrapper, initialValue, response.params, editorStyle);
        if (typedText) {
            this.editor.setValue(typedText);
        }

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
        var minWidth = 41;
        var width = cell.offsetWidth + 1;
        if (width < minWidth) {
            cell.style.minWidth = minWidth + "px";
            width = cell.offsetWidth + 1;
        }
        this.editorWrapper.style.width = width + "px";
        this.editorWrapper.style.height = cell.offsetHeight + 1 + "px";
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
                this.hasChanges = true;
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
        return url;
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
        this.editor && this.editor.cancelEdit();
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

        this.setCellValue();

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

        this.doOperation(TableEditor.Operations.SET_ALIGN, params, function() {
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

        this.setCellValue();

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

        this.setCellValue();

        var cell = this.currentElement;

        var params = {
            editorId: this.editorId,
            row : this.selectionPos[0],
            col : this.selectionPos[1],
            indent: _indent
        };

        this.doOperation(TableEditor.Operations.SET_INDENT, params, function() {
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

        this.setCellValue();

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
        this.doOperation(TableEditor.Operations.SET_FONT_BOLD, params, function() {
 
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

        this.setCellValue();

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
        this.doOperation(TableEditor.Operations.SET_FONT_ITALIC, params, function() {

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

        this.setCellValue();

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
        this.doOperation(TableEditor.Operations.SET_FONT_UNDERLINE, params, function() {

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

        // Due to asynchronous nature of setCellValue() it can be executed after next operation.
        // So we cancel unsaved changes instead.
        // TODO: Refactor to invoke operation after successful asynchronous this.setCellValue() operation
        this.editor && this.editor.cancelEdit();

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

    isHasChanges: function () {
        return this.hasChanges;
    },

    setHasChanges: function (hasChanges) {
        this.hasChanges = hasChanges;
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

TableEditor.isNavigationKey = function (keyCode) { return  keyCode >= 37 && keyCode <= 41; };

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


// @Deprecated
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
		if (evt.target) {
			return evt.target;
		} else if (evt.srcElement) {
			return evt.srcElement;
		}
		return undefined;
	},

	_init: function () {
		document.onclick = function(e) {
			var el = PopupMenu.getTarget(e);
			if (el && (el.name != 'menurevealbutton') && !PopupMenu.inMenuDiv(el))
				PopupMenu.closeMenu();
			return true;
		};

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
		};
		this.te_menu.onmouseover = function() {
			PopupMenu.cancelDisappear();
		};

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
};/**
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
    params: null,
    initialValue: null,
    stoppedEvents: null,
    focussed: null,
    style: null,

    /**
     * Constructor.
     * Generally editor constructor performs the following steps:
     *   1. Saves initial cell value into initialValue variable
     *   2. Creates an HTML editor control (e.g. HTMLInputElement) and sets its value
     */
    initialize: function(tableEditor, parentId, params, initialValue, focussed, style) {
        if (parentId) {
            this.tableEditor = tableEditor;
            this.parentElement = $(parentId);

            this.style = style;

            this.initialValue = initialValue;

            this.params = params;
            this.editor_initialize(params);
            this.input.id = this.getId();
            this.focussed = (focussed && focussed == true) ? focussed : '';
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
            if (this.focussed) {
                this.focus();
            }
        }
    },

    focus: function() {
        this.input.focus();
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
    destroy: function() {
        this.unbind();
    },

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
            this.focus();
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
            this.focus();
        }

        if (event) Event.stop(event);
    },

    show: function($super, value) {
        $super(value);
        if (this.focussed) {
            this.handleF3();
        }
    }
});/*
        DatePicker v5.4 by frequency-decoder.com

        Released under a creative commons Attribution-Share Alike 3.0 Unported license (http://creativecommons.org/licenses/by-sa/3.0/)

        Please credit frequency-decoder in any derivative work - thanks.
        
        You are free:
        
        * to Share � to copy, distribute and transmit the work
        * to Remix � to adapt the work
            
        Under the following conditions:

        * Attribution � You must attribute the work in the manner specified by the author or licensor (but not in any way that suggests that they endorse you or your use of the work).      
        * Share Alike � If you alter, transform, or build upon this work, you may distribute the resulting work only under the same, similar or a compatible license.
*/

var datePickerController = (function datePickerController() {

        var isMoz               = /mozilla/.test( navigator.userAgent.toLowerCase() ) && !/(compatible|webkit)/.test( navigator.userAgent.toLowerCase() ),
            languageInfo        = parseUILanguage(),
            datePickers         = {},
            uniqueId            = 0,
            weeksInYearCache    = {},
            localeImport        = false,
            nbsp                = String.fromCharCode(160),
            describedBy         = "",
            nodrag              = false,            
            buttonTabIndex      = true,
            returnLocaleDate    = false,
            mouseWheel          = true,              
            cellFormat          = "d-sp-F-sp-Y",
            titleFormat         = "F-sp-d-cc-sp-Y",
            formatParts         = ["placeholder", "sp-F-sp-Y"],
            dividors            = ["dt","sl","ds","cc","sp"],
            dvParts             = "dt|sl|ds|cc|sp",
            dParts              = "d|j",
            mParts              = "m|n|M|F",            
            yParts              = "Y|y",                        
            kbEvent             = false,
            bespokeTitles       = {},
            finalOpacity        = 100,
            validFmtRegExp      = /^((sp|dt|sl|ds|cc)|([d|D|l|j|N|w|S|W|M|F|m|n|t|Y|y]))(-((sp|dt|sl|ds|cc)|([d|D|l|j|N|w|S|W|M|F|m|n|t|Y|y])))*$/,
            rangeRegExp         = /^((\d\d\d\d)(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01]))$/,
            wcDateRegExp        = /^(((\d\d\d\d)|(\*\*\*\*))((0[1-9]|1[012])|(\*\*))(0[1-9]|[12][0-9]|3[01]))$/;                                      
                
        (function() {
                var scriptFiles = document.getElementsByTagName('script'),                    
                    scriptInner = String(scriptFiles[scriptFiles.length - 1].innerHTML).replace(/[\n\r\s\t]+/g, " ").replace(/^\s+/, "").replace(/\s+$/, ""),                    
                    json        = parseJSON(scriptInner);                
               
                if(typeof json === "object" && !("err" in json)) {                          
                        affectJSON(json);
                };
        })();
        
        function parseUILanguage() {                                 
                var languageTag = document.getElementsByTagName('html')[0].getAttribute('lang') || document.getElementsByTagName('html')[0].getAttribute('xml:lang');
                
                if(!languageTag) {
                        languageTag = "en";
                } else {
                        languageTag = languageTag.toLowerCase();
                };
                                                            
                return languageTag.search(/^([a-z]{2,3})-([a-z]{2})$/) != -1 ? [languageTag.match(/^([a-z]{2,3})-([a-z]{2})$/)[1], languageTag] : [languageTag];                       
        };
        
        function affectJSON(json) {
                if(typeof json !== "object") { return; };
                for(key in json) {
                        value = json[key];                                                                
                        switch(key.toLowerCase()) { 
                                case "lang":
                                        if(value.search(/^[a-z]{2,3}(-([a-z]{2}))?$/i) != -1) {                                                
                                                languageInfo = [value.toLowerCase()];                                                   
                                                returnLocaleDate = true;
                                        };
                                        break;                                                               
                                case "nodrag":
                                        nodrag = !!value;
                                        break;                                
                                case "buttontabindex":
                                        buttonTabIndex = !!value;
                                        break;
                                case "mousewheel":
                                        mouseWheel = !!value;
                                        break;  
                                case "cellformat":
                                        if(typeof value == "string" && value.match(validFmtRegExp)) {
                                                parseCellFormat(value);
                                        };
                                        break;
                                case "titleformat":
                                        if(typeof value == "string" && value.match(validFmtRegExp)) {
                                                titleFormat = value;
                                        }; 
                                        break;
                                case "describedby":
                                        if(typeof value == "string") {
                                                describedBy = value;
                                        };
                                        break; 
                                case "finalopacity":
                                        if(typeof value == 'number' && (+value > 20 && +value <= 100)) {
                                                finalOpacity = parseInt(value, 10);
                                        }; 
                                        break; 
                                case "bespoketitles":
                                        bespokeTitles = {};
                                        for(var dt in value) {
                                                bespokeTitles[dt] = value[dt];
                                        };                                                                                                                                             
                        };          
                };        
        };                  
        
        function parseCellFormat(value) {                  
                // I'm sure this could be done with a regExp and a split in one line... seriously...
                var parts       = value.split("-"),
                    fullParts   = [],
                    tmpParts    = [],
                    part;                              
                
                for(var pt = 0; pt < parts.length; pt++) {
                        part = parts[pt];                         
                        if(part == "j" || part == "d") { 
                                if(tmpParts.length) {
                                        fullParts.push(tmpParts.join("-")); 
                                        tmpParts = [];
                                };
                                fullParts.push("placeholder");   
                        } else { 
                                tmpParts.push(part);
                        };                                             
                };                  
                
                if(tmpParts.length) {
                        fullParts.push(tmpParts.join("-"));                                         
                };
                
                if(!fullParts.length || fullParts.length > 3) {
                        formatParts = ["placeholder", "sp-F-sp-Y"];
                        cellFormat = "j-sp-F-sp-Y"; 
                        return;
                };                
                
                formatParts = fullParts;
                cellFormat  = value;               
        };
         
        function pad(value, length) { 
                length = length || 2; 
                return "0000".substr(0,length - Math.min(String(value).length, length)) + value; 
        };
        
        function addEvent(obj, type, fn) { 
                try {                 
                        if( obj.attachEvent ) {
                                obj["e"+type+fn] = fn;
                                obj[type+fn] = function(){obj["e"+type+fn]( window.event );};
                                obj.attachEvent( "on"+type, obj[type+fn] );
                        } else {
                                obj.addEventListener( type, fn, true );
                        };
                } catch(err) {}
        };
        
        function removeEvent(obj, type, fn) {
                try {
                        if( obj.detachEvent ) {
                                obj.detachEvent( "on"+type, obj[type+fn] );
                                obj[type+fn] = null;
                        } else {
                                obj.removeEventListener( type, fn, true );
                        };
                } catch(err) {};
        };   

        function stopEvent(e) {
                e = e || document.parentWindow.event;
                if(e.stopPropagation) {
                        e.stopPropagation();
                        e.preventDefault();
                };
                /*@cc_on
                @if(@_win32)
                e.cancelBubble = true;
                e.returnValue = false;
                @end
                @*/
                return false;
        };
        
        function parseJSON(str) {
                // Check we have a String
                if(typeof str !== 'string' || str == "") { return {}; };                 
                try {
                        // Does a JSON (native or not) Object exist                              
                        if(typeof JSON === "object" && JSON.parse) {                                              
                                return window.JSON.parse(str);  
                        // Genious code taken from: http://kentbrewster.com/badges/                                                      
                        } else if(/lang|buttontabindex|mousewheel|cellformat|titleformat|nodrag|describedby/.test(str.toLowerCase())) {                                               
                                var f = Function(['var document,top,self,window,parent,Number,Date,Object,Function,',
                                        'Array,String,Math,RegExp,Image,ActiveXObject;',
                                        'return (' , str.replace(/<\!--.+-->/gim,'').replace(/\bfunction\b/g,'function�') , ');'].join(''));
                                return f();                          
                        };
                } catch (e) { };
                
                return {"err":"Could not parse the JSON object"};                                            
        };        

        function setARIARole(element, role) {
                if(element && element.tagName) {
                        element.setAttribute("role", role);
                };
        };
        
        function setARIAProperty(element, property, value) {
		if(element && element.tagName) {
                        element.setAttribute("aria-" + property, value);
                };	
	};

        // The datePicker object itself 
        function datePicker(options) {                                      
                this.dateSet             = null;                 
                this.timerSet            = false;
                this.visible             = false;
                this.timer               = null;
                this.yearInc             = 0;
                this.monthInc            = 0;
                this.dayInc              = 0;
                this.mx                  = 0;
                this.my                  = 0;
                this.x                   = 0;
                this.y                   = 0; 
                this.created             = false;
                this.disabled            = false;
                this.opacity             = 0; 
                this.opacityTo           = 99;
                this.inUpdate            = false;                              
                this.kbEventsAdded       = false;
                this.fullCreate          = false;
                this.selectedTD          = null;
                this.cursorTD            = null;
                this.cursorDate          = options.cursorDate ? options.cursorDate : "",       
                this.date                = options.cursorDate ? new Date(+options.cursorDate.substr(0,4), +options.cursorDate.substr(4,2) - 1, +options.cursorDate.substr(6,2)) : new Date();
                this.defaults            = {};
                this.dynDisabledDates    = {};
                this.firstDayOfWeek      = localeImport.firstDayOfWeek; 
                this.interval            = new Date();
                this.clickActivated      = false;
                this.noFocus             = true;
                this.kbEvent             = false; 
                this.disabledDates       = false;
                this.enabledDates        = false;
                this.delayedUpdate       = false;  
                this.bespokeTitles       = {};
                
                for(var thing in options) {
                        if(thing.search(/callbacks|formElements|formatMasks/) != -1) continue;
                        this[thing] = options[thing];                 
                };

                for(var i = 0, prop; prop = ["callbacks", "formElements", "formatMasks"][i]; i++) { 
                        this[prop] = {};                        
                        for(var thing in options[prop]) {                                
                                this[prop][thing] = options[prop][thing];                 
                        };
                };

                // Adjust time to stop daylight savings madness on windows
                this.date.setHours(5);              
                
                this.changeHandler = function() {                        
                        o.setDateFromInput();  
                        o.callback("dateset", o.createCbArgObj());                                                                                 
                };
                this.createCbArgObj = function() {                        
                        return this.dateSet ? {"id":this.id,"date":this.dateSet,"dd":pad(this.date.getDate()),"mm":pad(this.date.getMonth() + 1),"yyyy":this.date.getFullYear()} : {"id":this.id,"date":null,"dd":null,"mm":null,"yyyy":null};                         
                };
                this.getScrollOffsets = function() {                         
                        if(typeof(window.pageYOffset) == 'number') {
                                //Netscape compliant
                                return [window.pageXOffset, window.pageYOffset];                                
                        } else if(document.body && (document.body.scrollLeft || document.body.scrollTop)) {
                                //DOM compliant
                                return [document.body.scrollLeft, document.body.scrollTop];                                
                        } else if(document.documentElement && (document.documentElement.scrollLeft || document.documentElement.scrollTop)) {
                                //IE6 standards compliant mode
                                return [document.documentElement.scrollLeft, document.documentElement.scrollTop];
                        };
                        return [0,0];
                };
                this.reposition = function() {
                        if(!o.created || o.staticPos) { return; };

                        o.div.style.visibility = "hidden";
                        o.div.style.left = o.div.style.top = "0px";                           
                        o.div.style.display = "block";

                        var osh         = o.div.offsetHeight,
                            osw         = o.div.offsetWidth,
                            elem        = document.getElementById('fd-but-' + o.id),
                            pos         = o.truePosition(elem),
                            trueBody    = (document.compatMode && document.compatMode!="BackCompat") ? document.documentElement : document.body,
                            sOffsets    = o.getScrollOffsets(),
                            scrollTop   = sOffsets[1], 
                            scrollLeft  = sOffsets[0],
                            fitsBottom  = parseInt(trueBody.clientHeight+scrollTop) > parseInt(osh+pos[1]+elem.offsetHeight+2),
                            fitsTop     = parseInt(pos[1]-(osh+elem.offsetHeight+2)) > parseInt(scrollTop); 
                        
                        o.div.style.visibility = "visible";

                        o.div.style.left = Number(parseInt(trueBody.clientWidth+scrollLeft) < parseInt(osw+pos[0]) ? Math.abs(parseInt((trueBody.clientWidth+scrollLeft) - osw)) : pos[0]) + "px";
                        o.div.style.top  = (fitsBottom || !fitsTop) ? Math.abs(parseInt(pos[1] + elem.offsetHeight + 2)) + "px" : Math.abs(parseInt(pos[1] - (osh + 2))) + "px";
                };
                this.removeOldFocus = function() {
                        var td = document.getElementById(o.id + "-date-picker-hover");
                        if(td) {                                        
                                try { 
                                        td.setAttribute(!/*@cc_on!@*/false ? "tabIndex" : "tabindex", "-1");
                                        td.tabIndex = -1;                                          
                                        td.className = td.className.replace(/date-picker-hover/, "");                                         
                                        td.id = ""; 
                                        td.onblur  = null; 
                                        td.onfocus = null;                                                                             
                                } catch(err) {};
                        };
                }; 
                this.addAccessibleDate = function() {
                        var td   = document.getElementById(o.id + "-date-picker-hover");                            
                                
                        if(td && !(td.getElementsByTagName("span").length)) {                                                          
                                var ymd = td.className.match(/cd-([\d]{4})([\d]{2})([\d]{2})/),
                                    noS = (td.className.search(/date-picker-unused|out-of-range|day-disabled|no-selection|not-selectable/) != -1),
                                    spn  = document.createElement('span'),
                                    spnC;                                        
                        
                                spn.className       = "fd-screen-reader";;
                                
                                while(td.firstChild) td.removeChild(td.firstChild);
                                
                                if(noS) {
                                        spnC = spn.cloneNode(false);
                                        spnC.appendChild(document.createTextNode(getTitleTranslation(13)));
                                        td.appendChild(spnC);
                                };
                                
                                for(var pt = 0, part; part = formatParts[pt]; pt++) {
                                        if(part == "placeholder") {
                                                td.appendChild(document.createTextNode(+ymd[3]));
                                        } else {
                                                spnC = spn.cloneNode(false);
                                                spnC.appendChild(document.createTextNode(printFormattedDate(new Date(ymd[1], +ymd[2]-1, ymd[3]), part, true)));
                                                td.appendChild(spnC);
                                        };                                                
                                };
                        };
                };
                this.setNewFocus = function() {                                                                                             
                        var td = document.getElementById(o.id + "-date-picker-hover");
                        if(td) {
                                try {                                             
                                        td.setAttribute(!/*@cc_on!@*/false ? "tabIndex" : "tabindex", "0");                                
                                        td.tabIndex  = 0;  
                                                                                                                   
                                        td.className = td.className.replace(/date-picker-hover/, "") + " date-picker-hover"; 
                                        if(!this.clickActivated) {                                                
                                                td.onblur    = o.onblur;  
                                                td.onfocus   = o.onfocus;                                   
                                        };
                                                                                                                                                                                         
                                        if(!this.clickActivated) o.addAccessibleDate();
                                        
                                        if(!this.noFocus && !this.clickActivated) {                                                                                                                                                   
                                                setTimeout(function() { try { td.focus(); } catch(err) {}; }, 0);
                                        };                                         
                                } catch(err) { };
                        };
                };
                this.setCursorDate = function(yyyymmdd) {                        
                        if(String(yyyymmdd).search(/^([0-9]{8})$/) != -1) {
                                this.date = new Date(+yyyymmdd.substr(0,4), +yyyymmdd.substr(4,2) - 1, +yyyymmdd.substr(6,2));
                                this.cursorDate = yyyymmdd;
                                
                                if(this.staticPos) {                                         
                                        this.updateTable();
                                };                                                                                                  
                        };
                };                  
                this.updateTable = function(noCallback) {  
                        if(!o || o.inUpdate || !o.created) return;
                        
                        o.inUpdate = true;                                         
                        o.removeOldFocus();
                        
                        if(o.timerSet && !o.delayedUpdate) {
                                if(o.monthInc) {
                                        var n = o.date.getDate(),
                                            d = new Date(o.date);                         
                       
                                        d.setDate(2);                                               
                                        d.setMonth(d.getMonth() + o.monthInc * 1);
                                        d.setDate(Math.min(n, daysInMonth(d.getMonth(),d.getFullYear())));
                                        
                                        o.date = new Date(d);
                                } else {                                 
                                        o.date.setDate(Math.min(o.date.getDate()+o.dayInc, daysInMonth(o.date.getMonth()+o.monthInc,o.date.getFullYear()+o.yearInc)));
                                        o.date.setMonth(o.date.getMonth() + o.monthInc);                                        
                                        o.date.setFullYear(o.date.getFullYear() + o.yearInc);
                                };                                       
                        }; 
        
                        o.outOfRange();
                        if(!o.noToday) { o.disableTodayButton(); };
                        o.showHideButtons(o.date);
                
                        var cd = o.date.getDate(),
                            cm = o.date.getMonth(),
                            cy = o.date.getFullYear(),
                            cursorDate = (String(cy) + pad(cm+1) + pad(cd)),
                            tmpDate    = new Date(cy, cm, 1);                      
                        
                        tmpDate.setHours(5);
                        
                        var dt, cName, td, i, currentDate, cellAdded, col, currentStub, abbr, bespokeRenderClass, spnC, dateSetD,
                        weekDayC            = ( tmpDate.getDay() + 6 ) % 7,                
                        firstColIndex       = (((weekDayC - o.firstDayOfWeek) + 7 ) % 7) - 1,
                        dpm                 = daysInMonth(cm, cy),
                        today               = new Date(),                        
                        stub                = String(tmpDate.getFullYear()) + pad(tmpDate.getMonth()+1),
                        cellAdded           = [4,4,4,4,4,4],                                                                   
                        lm                  = new Date(cy, cm-1, 1),
                        nm                  = new Date(cy, cm+1, 1),                          
                        daySub              = daysInMonth(lm.getMonth(), lm.getFullYear()),                
                        stubN               = String(nm.getFullYear()) + pad(nm.getMonth()+1),
                        stubP               = String(lm.getFullYear()) + pad(lm.getMonth()+1),                
                        weekDayN            = (nm.getDay() + 6) % 7,
                        weekDayP            = (lm.getDay() + 6) % 7,                                       
                        today               = today.getFullYear() + pad(today.getMonth()+1) + pad(today.getDate()),
                        spn                 = document.createElement('span');                        
                        
                        o.firstDateShown    = !o.constrainSelection && o.fillGrid && (0 - firstColIndex < 1) ? String(stubP) + (daySub + (0 - firstColIndex)) : stub + "01";            
                        o.lastDateShown     = !o.constrainSelection && o.fillGrid ? stubN + pad(41 - firstColIndex - dpm) : stub + String(dpm);
                        o.currentYYYYMM     = stub;                    
                
                        bespokeRenderClass  = o.callback("redraw", {id:o.id, dd:pad(cd), mm:pad(cm+1), yyyy:cy, firstDateDisplayed:o.firstDateShown, lastDateDisplayed:o.lastDateShown}) || {};                                            
                        dts                 = o.getDates(cy, cm+1);                               
                
                        o.checkSelectedDate();
                        
                        dateSetD            = (o.dateSet != null) ? o.dateSet.getFullYear() + pad(o.dateSet.getMonth()+1) + pad(o.dateSet.getDate()) : false;
                        spn.className       = "fd-screen-reader";
                        
                        if(this.selectedTD != null) {
                                setARIAProperty(this.selectedTD, "selected", false);
                                this.selectedTD = null;
                        };
                        
                        for(var curr = 0; curr < 42; curr++) {
                                row  = Math.floor(curr / 7);                         
                                td   = o.tds[curr];
                                spnC = spn.cloneNode(false); 
                                
                                while(td.firstChild) td.removeChild(td.firstChild);
                                
                                if((curr > firstColIndex && curr <= (firstColIndex + dpm)) || o.fillGrid) {
                                        currentStub     = stub;
                                        weekDay         = weekDayC;                                
                                        dt              = curr - firstColIndex;
                                        cName           = [];                                         
                                        selectable      = true;                                     
                                        
                                        if(dt < 1) {
                                                dt              = daySub + dt;
                                                currentStub     = stubP;
                                                weekDay         = weekDayP;                                        
                                                selectable      = !o.constrainSelection;
                                                cName.push("month-out");                                                  
                                        } else if(dt > dpm) {
                                                dt -= dpm;
                                                currentStub     = stubN;
                                                weekDay         = weekDayN;                                        
                                                selectable      = !o.constrainSelection; 
                                                cName.push("month-out");                                                                                           
                                        }; 
                                        
                                        weekDay = ( weekDay + dt + 6 ) % 7;
                                        
                                        cName.push("day-" + localeDefaults.dayAbbrs[weekDay].toLowerCase());
                                        
                                        currentDate = currentStub + String(dt < 10 ? "0" : "") + dt;                            
                                        
                                        if(o.rangeLow && +currentDate < +o.rangeLow || o.rangeHigh && +currentDate > +o.rangeHigh) {                                          
                                                td.className = "out-of-range";  
                                                td.title = ""; 
                                                td.appendChild(document.createTextNode(dt));                                             
                                                if(o.showWeeks) { cellAdded[row] = Math.min(cellAdded[row], 2); };                                                                                                                                               
                                        } else {  
                                                if(selectable) {                                                                                                        
                                                        td.title = titleFormat ? printFormattedDate(new Date(+String(currentStub).substr(0,4), +String(currentStub).substr(4, 2) - 1, +dt), titleFormat, true) : "";                                                                                                      
                                                        cName.push("cd-" + currentDate + " yyyymm-" + currentStub + " mmdd-" + currentStub.substr(4,2) + pad(dt));
                                                } else {  
                                                        td.title = titleFormat ? getTitleTranslation(13) + " " + printFormattedDate(new Date(+String(currentStub).substr(0,4), +String(currentStub).substr(4, 2) - 1, +dt), titleFormat, true) : "";                                                                       
                                                        cName.push("yyyymm-" + currentStub + " mmdd-" + currentStub.substr(4,2) + pad(dt) + " not-selectable");
                                                };                                                                                                                                             
                                                
                                                if(currentDate == today) { cName.push("date-picker-today"); };

                                                if(dateSetD == currentDate) { 
                                                        cName.push("date-picker-selected-date"); 
                                                        setARIAProperty(td, "selected", "true");
                                                        this.selectedTD = td;
                                                };

                                                if(o.disabledDays[weekDay] || dts[currentDate] == 0) { cName.push("day-disabled"); if(titleFormat && selectable) { td.title = getTitleTranslation(13) + " " + td.title; }; }
                                        
                                                if(currentDate in bespokeRenderClass) { cName.push(bespokeRenderClass[currentDate]); }
                                        
                                                if(o.highlightDays[weekDay]) { cName.push("date-picker-highlight"); };

                                                if(cursorDate == currentDate) { 
                                                        td.id = o.id + "-date-picker-hover";                                                                                                                                                                 
                                                };      
                                                                                   
                                                td.appendChild(document.createTextNode(dt));
                                                td.className = cName.join(" ");
                                               
                                                if(o.showWeeks) {                                                         
                                                        cellAdded[row] = Math.min(cName[0] == "month-out" ? 3 : 1, cellAdded[row]);                                                          
                                                }; 
                                        };                       
                                } else {
                                        td.className = "date-picker-unused";                                                                                                                    
                                        td.appendChild(document.createTextNode(nbsp));
                                        td.title = "";                                                                              
                                };                                                  
                                
                                if(o.showWeeks && curr - (row * 7) == 6) { 
                                        while(o.wkThs[row].firstChild) o.wkThs[row].removeChild(o.wkThs[row].firstChild);                                         
                                        o.wkThs[row].appendChild(document.createTextNode(cellAdded[row] == 4 && !o.fillGrid ? nbsp : getWeekNumber(cy, cm, curr - firstColIndex - 6)));
                                        o.wkThs[row].className = "date-picker-week-header" + (["",""," out-of-range"," month-out",""][cellAdded[row]]);                                          
                                };                                
                        };            
                        
                        var span = o.titleBar.getElementsByTagName("span");
                        while(span[0].firstChild) span[0].removeChild(span[0].firstChild);
                        while(span[1].firstChild) span[1].removeChild(span[1].firstChild);
                        span[0].appendChild(document.createTextNode(getMonthTranslation(cm, false) + nbsp));
                        span[1].appendChild(document.createTextNode(cy));
                        
                        if(o.timerSet) {
                                o.timerInc = 50 + Math.round(((o.timerInc - 50) / 1.8));
                                o.timer = window.setTimeout(o.updateTable, o.timerInc);
                        };
                        
                        o.inUpdate = o.delayedUpdate = false; 
                        o.setNewFocus();                         
                };
                
                this.destroy = function() {
                        if(document.getElementById("fd-but-" + this.id)) {
                                document.getElementById("fd-but-" + this.id).parentNode.removeChild(document.getElementById("fd-but-" + this.id));        
                        };
                        
                        if(!this.created) { return; };
                        
                        // Cleanup for Internet Explorer
                        removeEvent(this.table, "mousedown", o.onmousedown);  
                        removeEvent(this.table, "mouseover", o.onmouseover);
                        removeEvent(this.table, "mouseout", o.onmouseout);
                        removeEvent(document, "mousedown", o.onmousedown);
                        removeEvent(document, "mouseup",   o.clearTimer);
                        
                        if (window.addEventListener && !window.devicePixelRatio) {
                                try {
                                        window.removeEventListener('DOMMouseScroll', this.onmousewheel, false);
                                } catch(err) {};                                 
                        } else {
                                removeEvent(document, "mousewheel", this.onmousewheel);
                                removeEvent(window,   "mousewheel", this.onmousewheel);
                        }; 
                        o.removeOnFocusEvents();
                        clearTimeout(o.timer);

                        if(this.div && this.div.parentNode) {
                                this.div.parentNode.removeChild(this.div);
                        };
                        o = null;
                };
                this.resizeInlineDiv = function()  {                        
                        o.div.style.width = o.table.offsetWidth + "px";
                        o.div.style.height = o.table.offsetHeight + "px";
                };
                this.create = function() {
                        
                        if(document.getElementById("fd-" + this.id)) return;
                        
                        this.noFocus = true; 
                        
                        function createTH(details) {
                                var th = document.createElement('th');
                                if(details.thClassName) th.className = details.thClassName;
                                if(details.colspan) {
                                        /*@cc_on
                                        /*@if (@_win32)
                                        th.setAttribute('colSpan',details.colspan);
                                        @else @*/
                                        th.setAttribute('colspan',details.colspan);
                                        /*@end
                                        @*/
                                };
                                /*@cc_on
                                /*@if (@_win32)
                                th.unselectable = "on";
                                /*@end@*/
                                return th;
                        };
                        function createThAndButton(tr, obj) {
                                for(var i = 0, details; details = obj[i]; i++) {
                                        var th = createTH(details);
                                        tr.appendChild(th);
                                        var but = document.createElement('span');
                                        but.className = details.className;
                                        but.id = o.id + details.id;
                                        but.appendChild(document.createTextNode(details.text || o.nbsp));
                                        but.title = details.title || "";                                          
                                        /*@cc_on
                                        /*@if(@_win32)
                                        th.unselectable = but.unselectable = "on";
                                        /*@end@*/
                                        th.appendChild(but);
                                };
                        };  
                        
                        this.div                     = document.createElement('div');
                        this.div.id                  = "fd-" + this.id;
                        this.div.className           = "datePicker";  
                        
                        // Attempt to hide the div from screen readers during content creation
                        this.div.style.visibility = "hidden";
                        this.div.style.display = "none";
                                                
                        // Set the ARIA describedby property if the required block available
                        if(this.describedBy && document.getElementById(this.describedBy)) {
                                setARIAProperty(this.div, "describedby", this.describedBy);
                        };
                        
                        // Set the ARIA labelled property if the required label available
                        if(this.labelledBy) {
                                setARIAProperty(this.div, "labelledby", this.labelledBy.id);
                        };
                              
                        var tr, row, col, tableHead, tableBody, tableFoot;

                        this.table             = document.createElement('table');
                        this.table.className   = "datePickerTable";                         
                        this.table.onmouseover = this.onmouseover;
                        this.table.onmouseout  = this.onmouseout;
                        this.table.onclick     = this.onclick;

                        if(this.staticPos) {
                                this.table.onmousedown  = this.onmousedown;
                        };

                        this.div.appendChild(this.table);   
                        
                        var dragEnabledCN = !this.dragDisabled ? " drag-enabled" : "";
                                
                        if(!this.staticPos) {
                                this.div.style.visibility = "hidden";
                                this.div.className += dragEnabledCN;
                                document.getElementsByTagName('body')[0].appendChild(this.div);
                                
                                // Aria "hidden" property for non active popup datepickers
                                setARIAProperty(this.div, "hidden", "true");
                        } else {
                                elem = document.getElementById(this.positioned ? this.positioned : this.id);
                                if(!elem) {
                                        this.div = null;
                                        return;
                                };

                                this.div.className += " static-datepicker";                          

                                if(this.positioned) {
                                        elem.appendChild(this.div);
                                } else {
                                        elem.parentNode.insertBefore(this.div, elem.nextSibling);
                                };
                                
                                if(this.hideInput) {
                                        for(var elemID in this.formElements) {
                                                elem = document.getElementById(elemID);
                                                if(elem) {
                                                        elem.className += " fd-hidden-input";
                                                };        
                                        };                                        
                                };                                                                  
                                                                          
                                setTimeout(this.resizeInlineDiv, 300);                               
                        };                          
                                
                        // ARIA Grid role
                        setARIARole(this.div, "grid");
                       
                        if(this.statusFormat) {
                                tableFoot = document.createElement('tfoot');
                                this.table.appendChild(tableFoot);
                                tr = document.createElement('tr');
                                tr.className = "date-picker-tfoot";
                                tableFoot.appendChild(tr);                                
                                this.statusBar = createTH({thClassName:"date-picker-statusbar" + dragEnabledCN, colspan:this.showWeeks ? 8 : 7});
                                tr.appendChild(this.statusBar); 
                                this.updateStatus(); 
                        };

                        tableHead = document.createElement('thead');
                        this.table.appendChild(tableHead);

                        tr  = document.createElement('tr');
                        setARIARole(tr, "presentation");
                        
                        tableHead.appendChild(tr);

                        // Title Bar
                        this.titleBar = createTH({thClassName:"date-picker-title" + dragEnabledCN, colspan:this.showWeeks ? 8 : 7});
                        
                        tr.appendChild(this.titleBar);
                        tr = null;

                        var span = document.createElement('span');
                        span.appendChild(document.createTextNode(nbsp));
                        span.className = "month-display" + dragEnabledCN; 
                        this.titleBar.appendChild(span);

                        span = document.createElement('span');
                        span.appendChild(document.createTextNode(nbsp));
                        span.className = "year-display" + dragEnabledCN; 
                        this.titleBar.appendChild(span);

                        span = null;

                        tr  = document.createElement('tr');
                        setARIARole(tr, "presentation");
                        tableHead.appendChild(tr);

                        createThAndButton(tr, [
                        {className:"prev-but prev-year",  id:"-prev-year-but", text:"\u00AB", title:getTitleTranslation(2) },
                        {className:"prev-but prev-month", id:"-prev-month-but", text:"\u2039", title:getTitleTranslation(0) },
                        {colspan:this.showWeeks ? 4 : 3, className:"today-but", id:"-today-but", text:getTitleTranslation(4)},
                        {className:"next-but next-month", id:"-next-month-but", text:"\u203A", title:getTitleTranslation(1)},
                        {className:"next-but next-year",  id:"-next-year-but", text:"\u00BB", title:getTitleTranslation(3) }
                        ]);

                        tableBody = document.createElement('tbody');
                        this.table.appendChild(tableBody);

                        var colspanTotal = this.showWeeks ? 8 : 7,
                            colOffset    = this.showWeeks ? 0 : -1,
                            but, abbr;   
                
                        for(var rows = 0; rows < 7; rows++) {
                                row = document.createElement('tr');

                                if(rows != 0) {
                                        // ARIA Grid role
                                        setARIARole(row, "row");
                                        tableBody.appendChild(row);   
                                } else {
                                        tableHead.appendChild(row);
                                };

                                for(var cols = 0; cols < colspanTotal; cols++) {                                                                                
                                        if(rows === 0 || (this.showWeeks && cols === 0)) {
                                                col = document.createElement('th');                                                                                              
                                        } else {
                                                col = document.createElement('td');                                                                                           
                                                setARIAProperty(col, "describedby", this.id + "-col-" + cols + (this.showWeeks ? " " + this.id + "-row-" + rows : ""));
                                                setARIAProperty(col, "selected", "false");                                                 
                                        };
                                        
                                        /*@cc_on@*/
                                        /*@if(@_win32)
                                        col.unselectable = "on";
                                        /*@end@*/  
                                        
                                        row.appendChild(col);
                                        if((this.showWeeks && cols > 0 && rows > 0) || (!this.showWeeks && rows > 0)) {                                                
                                                setARIARole(col, "gridcell"); 
                                        } else {
                                                if(rows === 0 && cols > colOffset) {
                                                        col.className = "date-picker-day-header";
                                                        col.scope = "col";
                                                        setARIARole(col, "columnheader"); 
                                                        col.id = this.id + "-col-" + cols;                                          
                                                } else {
                                                        col.className = "date-picker-week-header";
                                                        col.scope = "row";
                                                        setARIARole(col, "rowheader");
                                                        col.id = this.id + "-row-" + rows;
                                                };
                                        };
                                };
                        };

                        col = row = null; 
                
                        this.ths = this.table.getElementsByTagName('thead')[0].getElementsByTagName('tr')[2].getElementsByTagName('th');
                        for (var y = 0; y < colspanTotal; y++) {
                                if(y == 0 && this.showWeeks) {
                                        this.ths[y].appendChild(document.createTextNode(getTitleTranslation(6)));
                                        this.ths[y].title = getTitleTranslation(8);
                                        continue;
                                };

                                if(y > (this.showWeeks ? 0 : -1)) {
                                        but = document.createElement("span");
                                        but.className = "fd-day-header";                                        
                                        /*@cc_on@*/
                                        /*@if(@_win32)
                                        but.unselectable = "on";
                                        /*@end@*/
                                        this.ths[y].appendChild(but);
                                };
                        };
                
                        but = null; 
                                        
                        this.trs             = this.table.getElementsByTagName('tbody')[0].getElementsByTagName('tr');
                        this.tds             = this.table.getElementsByTagName('tbody')[0].getElementsByTagName('td');
                        this.butPrevYear     = document.getElementById(this.id + "-prev-year-but");
                        this.butPrevMonth    = document.getElementById(this.id + "-prev-month-but");
                        this.butToday        = document.getElementById(this.id + "-today-but");
                        this.butNextYear     = document.getElementById(this.id + "-next-year-but"); 
                        this.butNextMonth    = document.getElementById(this.id + "-next-month-but");
        
                        if(this.noToday) {
                                this.butToday.style.display = "none";        
                        };
                        
                        if(this.showWeeks) {
                                this.wkThs = this.table.getElementsByTagName('tbody')[0].getElementsByTagName('th');
                                this.div.className += " weeks-displayed";
                        };

                        tableBody = tableHead = tr = createThAndButton = createTH = null;

                        if(this.rangeLow && this.rangeHigh && (this.rangeHigh - this.rangeLow < 7)) { this.equaliseDates(); };                        
                                                             
                        this.updateTableHeaders();
                        this.created = true;                                                                    
                        this.updateTable();                         
                        
                        if(this.staticPos) {                                 
                                this.visible = true;
                                this.opacity = this.opacityTo = this.finalOpacity;                                                                                              
                                this.div.style.visibility = "visible";                       
                                this.div.style.display = "block";
                                this.noFocus = true;                                                          
                                this.fade();
                        } else {                                     
                                this.reposition();
                                this.div.style.visibility = "visible";
                                this.fade();
                                this.noFocus = true;   
                        };   
                        
                        this.callback("domcreate", { "id":this.id });                                                   
                };                 
                this.fade = function() {
                                o.setOpacity(o.opacityTo);
                                if(o.opacityTo == 0) {
                                        o.div.style.display    = "none";
                                        o.div.style.visibility = "hidden";
                                        setARIAProperty(o.div, "hidden", "true");
                                        o.visible = false;
                                } else {
                                        setARIAProperty(o.div, "hidden", "false");
                                        o.visible = true;                                        
                                };
                };                  
                this.trackDrag = function(e) {
                        e = e || window.event;
                        var diffx = (e.pageX?e.pageX:e.clientX?e.clientX:e.x) - o.mx;
                        var diffy = (e.pageY?e.pageY:e.clientY?e.clientY:e.Y) - o.my;
                        o.div.style.left = Math.round(o.x + diffx) > 0 ? Math.round(o.x + diffx) + 'px' : "0px";
                        o.div.style.top  = Math.round(o.y + diffy) > 0 ? Math.round(o.y + diffy) + 'px' : "0px";
                };
                this.stopDrag = function(e) {
                        var b = document.getElementsByTagName("body")[0];
                        b.className = b.className.replace(/fd-drag-active/g, "");
                        removeEvent(document,'mousemove',o.trackDrag, false);
                        removeEvent(document,'mouseup',o.stopDrag, false);
                        o.div.style.zIndex = 9999;
                }; 
                this.onmousedown = function(e) {
                        e = e || document.parentWindow.event;
                        var el     = e.target != null ? e.target : e.srcElement,
                            origEl = el,
                            hideDP = true,
                            reg    = new RegExp("^fd-(but-)?" + o.id + "$");
                        
                        o.mouseDownElem = null;
                       
                        // Are we within the wrapper div or the button    
                        while(el) {
                                if(el.id && el.id.length && el.id.search(reg) != -1) { 
                                        hideDP = false;
                                        break;
                                };
                                try { el = el.parentNode; } catch(err) { break; };
                        };
                        
                        // If not, then ...     
                        if(hideDP) {                                                        
                                hideAll();                                                            
                                return true;                                                                  
                        };
                        
                        if((o.div.className + origEl.className).search('fd-disabled') != -1) { return true; };                                                                                                            
                        
                        // We check the mousedown events on the buttons
                        if(origEl.id.search(new RegExp("^" + o.id + "(-prev-year-but|-prev-month-but|-next-month-but|-next-year-but)$")) != -1) {
                                
                                o.mouseDownElem = origEl;
                                
                                addEvent(document, "mouseup", o.clearTimer);
                                addEvent(origEl, "mouseout",  o.clearTimer); 
                                                                 
                                var incs = {
                                        "-prev-year-but":[0,-1,0],
                                        "-prev-month-but":[0,0,-1],
                                        "-next-year-but":[0,1,0],
                                        "-next-month-but":[0,0,1]
                                    },
                                    check = origEl.id.replace(o.id, ""),
                                    dateYYYYMM = Number(o.date.getFullYear() + pad(o.date.getMonth()+1));
                                
                                o.timerInc      = 800;
                                o.timerSet      = true;
                                o.dayInc        = incs[check][0];
                                o.yearInc       = incs[check][1];
                                o.monthInc      = incs[check][2]; 
                                o.accellerator  = 1;
                                
                                if(!(o.currentYYYYMM == dateYYYYMM)) {
                                        if((o.currentYYYYMM < dateYYYYMM && (o.yearInc == -1 || o.monthInc == -1)) || (o.currentYYYYMM > dateYYYYMM && (o.yearInc == 1 || o.monthInc == 1))) {
                                                o.delayedUpdate = false; 
                                                o.timerInc = 1200;                                                
                                        } else {
                                                o.delayedUpdate = true;
                                                o.timerInc = 800;                                                
                                        };  
                                };
                                
                                o.updateTable();    
                                
                                return stopEvent(e);
                                                            
                        } else if(el.className.search("drag-enabled") != -1) {                                  
                                o.mx = e.pageX ? e.pageX : e.clientX ? e.clientX : e.x;
                                o.my = e.pageY ? e.pageY : e.clientY ? e.clientY : e.Y;
                                o.x  = parseInt(o.div.style.left);
                                o.y  = parseInt(o.div.style.top);
                                addEvent(document,'mousemove',o.trackDrag, false);
                                addEvent(document,'mouseup',o.stopDrag, false);
                                var b = document.getElementsByTagName("body")[0];
                                b.className = b.className.replace(/fd-drag-active/g, "") + " fd-drag-active";
                                o.div.style.zIndex = 10000;
                                
                                return stopEvent(e);
                        };
                        return true;                                                                      
                }; 
                this.onclick = function(e) {
                        if(o.opacity != o.opacityTo || o.disabled) return stopEvent(e);
                        
                        e = e || document.parentWindow.event;
                        var el = e.target != null ? e.target : e.srcElement;                         
                          
                        while(el.parentNode) {
                                // Are we within a valid i.e. clickable TD node  
                                if(el.tagName && el.tagName.toLowerCase() == "td") {   
                                                                        
                                        if(el.className.search(/cd-([0-9]{8})/) == -1 || el.className.search(/date-picker-unused|out-of-range|day-disabled|no-selection|not-selectable/) != -1) return stopEvent(e);
                                        
                                        var cellDate = el.className.match(/cd-([0-9]{8})/)[1];                                                                                                                                                                           
                                        o.date       = new Date(cellDate.substr(0,4),cellDate.substr(4,2)-1,cellDate.substr(6,2));                                                                                
                                        o.dateSet    = new Date(o.date); 
                                        o.noFocus    = true;                                                                       
                                        o.callback("dateset", { "id":o.id, "date":o.dateSet, "dd":o.dateSet.getDate(), "mm":o.dateSet.getMonth() + 1, "yyyy":o.dateSet.getFullYear() });                                          
                                        o.returnFormattedDate();
                                        o.hide();                  
                                                
                                        //o.stopTimer();
                                        
                                        break;   
                                // Today button pressed             
                                } else if(el.id && el.id == o.id + "-today-but") {                                 
                                        o.date = new Date(); 
                                        o.updateTable();
                                        o.stopTimer();
                                        break; 
                                // Day headers clicked, change the first day of the week      
                                } else if(el.className.search(/date-picker-day-header/) != -1) {
                                        var cnt = o.showWeeks ? -1 : 0,
                                        elem = el;
                                        
                                        while(elem.previousSibling) {
                                                elem = elem.previousSibling;
                                                if(elem.tagName && elem.tagName.toLowerCase() == "th") cnt++;
                                        };
                                        
                                        o.firstDayOfWeek = (o.firstDayOfWeek + cnt) % 7;
                                        o.updateTableHeaders();
                                        break;     
                                };
                                try { el = el.parentNode; } catch(err) { break; };
                        };
                        
                        return stopEvent(e);                                                
                };
                
                this.show = function(autoFocus) {                         
                        if(this.staticPos) { return; };
                        
                        var elem, elemID;
                        for(elemID in this.formElements) {
                                elem = document.getElementById(this.id);
                                if(!elem || (elem && elem.disabled)) { return; };   
                        };
                        
                        this.noFocus = true; 
                        
                        // If the datepicker doesn't exist in the dom  
                        if(!this.created || !document.getElementById('fd-' + this.id)) {                          
                                this.created    = false;
                                this.fullCreate = false;                                                                                             
                                this.create();                                 
                                this.fullCreate = true;                                                            
                        } else {                                                        
                                this.setDateFromInput();                                                               
                                this.reposition();                                 
                        };                      
                        
                        this.noFocus = !!!autoFocus;                          
                        
                        if(this.noFocus) { 
                                this.clickActivated = true;
                                addEvent(document, "mousedown", this.onmousedown); 
                                if(mouseWheel) {
                                        if (window.addEventListener && !window.devicePixelRatio) window.addEventListener('DOMMouseScroll', this.onmousewheel, false);
                                        else {
                                                addEvent(document, "mousewheel", this.onmousewheel);
                                                addEvent(window,   "mousewheel", this.onmousewheel);
                                        };
                                };     
                        } else {
                                this.clickActivated = false;
                        };    
                        
                        this.opacityTo = this.finalOpacity;
                        this.div.style.display = "block";                        
                     
                        this.setNewFocus(); 
                        this.fade();
                        var butt = document.getElementById('fd-but-' + this.id);
                        if(butt) { butt.className = butt.className.replace("dp-button-active", "") + " dp-button-active"; };                                                
                };
                this.hide = function() {
                        if(!this.visible || !this.created || !document.getElementById('fd-' + this.id)) return;
                        
                        this.kbEvent = false;
                        
                        o.div.className = o.div.className.replace("datepicker-focus", "");  
                        
                        this.stopTimer();
                        this.removeOnFocusEvents();
                        this.clickActivated = false;                         
                                                                
                        // Update status bar                                
                        if(this.statusBar) { this.updateStatus(getTitleTranslation(9)); };    
                        
                        this.noFocus = true;
                        this.setNewFocus();
                        
                        if(this.staticPos) {                                                                 
                                return; 
                        };

                        var butt = document.getElementById('fd-but-' + this.id);
                        if(butt) butt.className = butt.className.replace("dp-button-active", "");
                
                        removeEvent(document, "mousedown", this.onmousedown);
                        
                        if(mouseWheel) {
                                if (window.addEventListener && !window.devicePixelRatio) {
                                        try { window.removeEventListener('DOMMouseScroll', this.onmousewheel, false);} catch(err) {};                                 
                                } else {
                                        removeEvent(document, "mousewheel", this.onmousewheel);
                                        removeEvent(window,   "mousewheel", this.onmousewheel);
                                }; 
                        };
                        
                        this.opacityTo = 0;
                        this.fade();
                        if (this.onPickerBlur) {
                            this.onPickerBlur();
                        }
                };
                this.onblur = function(e) {                                                                                                  
                        o.hide();
                };
                this.onfocus = function(e) {                                               
                        o.noFocus = false; 
                        o.div.className = o.div.className.replace("datepicker-focus", "") + " datepicker-focus";                                                                                                      
                        o.addOnFocusEvents();                                                                        
                };   
                this.onmousewheel = function(e) {                        
                        e = e || document.parentWindow.event;
                        var delta = 0;
                        
                        if (e.wheelDelta) {
                                delta = e.wheelDelta/120;
                        } else if(e.detail) {
                                delta = -e.detail/3;
                        };                          
                        
                        var n = o.date.getDate(),
                            d = new Date(o.date),
                            inc = delta > 0 ? 1 : -1;                         
                       
                        d.setDate(2);
                        d.setMonth(d.getMonth() + inc * 1);
                        d.setDate(Math.min(n, daysInMonth(d.getMonth(),d.getFullYear())));
                      
                        if(o.outOfRange(d)) { return stopEvent(e); };
                        
                        o.date = new Date(d);
                        
                        o.updateTable(); 
                        
                        if(o.statusBar) { o.updateStatus(printFormattedDate(o.date, o.statusFormat, true)); };
                        
                        return stopEvent(e);                                                       
                };                      
                this.onkeydown = function (e) {
                        o.stopTimer();
                        if(!o.visible) return false;
                                
                         e = e || document.parentWindow.event;
                        var kc = e.keyCode ? e.keyCode : e.charCode;
                                
                        if( kc == 13 ) {
                                // RETURN/ENTER: close & select the date
                                var td = document.getElementById(o.id + "-date-picker-hover");                                         
                                if(!td || td.className.search(/cd-([0-9]{8})/) == -1 || td.className.search(/no-selection|out-of-range|day-disabled/) != -1) {
                                        return stopEvent(e);
                                };
                                o.dateSet = new Date(o.date);
                                o.callback("dateset", o.createCbArgObj()); 
                                o.returnFormattedDate();    
                                o.hide();
                                return stopEvent(e);
                        } else if(kc == 27) {
                                // ESC: close, no date selection 
                                if(!o.staticPos) {
                                        o.hide();
                                        return stopEvent(e);
                                };
                                return true;
                        } else if(kc == 32 || kc == 0) {
                                // SPACE: goto today's date 
                                o.date = new Date();
                                o.updateTable();
                                return stopEvent(e);
                        } else if(kc == 9) {
                                // TAB: close, no date selection & focus on btton - popup only                                      
                                if(!o.staticPos) {
                                        return stopEvent(e);
                                };
                                return true;                                
                        };    
                                 
                        // Internet Explorer fires the keydown event faster than the JavaScript engine can
                        // update the interface. The following attempts to fix this.
                                
                        /*@cc_on
                        @if(@_win32)                                 
                        if(new Date().getTime() - o.interval.getTime() < 50) { return stopEvent(e); }; 
                        o.interval = new Date();                                 
                        @end
                        @*/
                        
                        if(isMoz) {
                                if(new Date().getTime() - o.interval.getTime() < 50) { return stopEvent(e); }; 
                                o.interval = new Date();
                        };                                 
                        
                        if ((kc > 49 && kc < 56) || (kc > 97 && kc < 104)) {
                                if(kc > 96) kc -= (96-48);
                                kc -= 49;
                                o.firstDayOfWeek = (o.firstDayOfWeek + kc) % 7;
                                o.updateTableHeaders();
                                return stopEvent(e);
                        };

                        if ( kc < 33 || kc > 40 ) return true;

                        var d = new Date(o.date), tmp, cursorYYYYMM = o.date.getFullYear() + pad(o.date.getMonth()+1); 

                        // HOME: Set date to first day of current month
                        if(kc == 36) {
                                d.setDate(1); 
                        // END: Set date to last day of current month                                 
                        } else if(kc == 35) {
                                d.setDate(daysInMonth(d.getMonth(),d.getFullYear())); 
                        // PAGE UP & DOWN                                   
                        } else if ( kc == 33 || kc == 34) {
                                var inc = (kc == 34) ? 1 : -1; 
                                
                                // CTRL + PAGE UP/DOWN: Moves to the same date in the previous/next year
                                if(e.ctrlKey) {                                                                                                               
                                        d.setFullYear(d.getFullYear() + inc * 1);
                                // PAGE UP/DOWN: Moves to the same date in the previous/next month                                            
                                } else {                                          
                                        var n = o.date.getDate();                         
                       
                                        d.setDate(2);
                                        d.setMonth(d.getMonth() + inc * 1);
                                        d.setDate(Math.min(n, daysInMonth(d.getMonth(),d.getFullYear())));                                         
                                };                                                                    
                        // LEFT ARROW                                    
                        } else if ( kc == 37 ) {                                         
                                d = new Date(o.date.getFullYear(), o.date.getMonth(), o.date.getDate() - 1);                                       
                        // RIGHT ARROW
                        } else if ( kc == 39 || kc == 34) {                                         
                                d = new Date(o.date.getFullYear(), o.date.getMonth(), o.date.getDate() + 1 ); 
                        // UP ARROW                                        
                        } else if ( kc == 38 ) {                                          
                                d = new Date(o.date.getFullYear(), o.date.getMonth(), o.date.getDate() - 7);  
                        // DOWN ARROW                                        
                        } else if ( kc == 40 ) {                                          
                                d = new Date(o.date.getFullYear(), o.date.getMonth(), o.date.getDate() + 7);                                         
                        };

                        if(o.outOfRange(d)) { return stopEvent(e); };
                        o.date = d;
                        
                        if(o.statusBar) { 
                                o.updateStatus(o.getBespokeTitle(o.date.getFullYear(),o.date.getMonth() + 1,o.date.getDate()) || printFormattedDate(o.date, o.statusFormat, true));                                
                        };
                        
                        var t = String(o.date.getFullYear()) + pad(o.date.getMonth()+1) + pad(o.date.getDate());

                        if(e.ctrlKey || (kc == 33 || kc == 34) || t < o.firstDateShown || t > o.lastDateShown) {                                                                       
                                o.updateTable(); 
                                /*@cc_on
                                @if(@_win32)
                                o.interval = new Date();                        
                                @end
                                @*/                                       
                        } else {                                    
                                if(!o.noToday) { o.disableTodayButton(); };                                        
                                o.removeOldFocus();
                                            
                                for(var i = 0, td; td = o.tds[i]; i++) {                                                                                             
                                        if(td.className.search("cd-" + t) == -1) {                                                          
                                                continue;
                                        };                                                 
                                        o.showHideButtons(o.date);
                                        td.id = o.id + "-date-picker-hover";                                                
                                        o.setNewFocus();
                                        break;
                                };
                        };

                        return stopEvent(e);
                }; 
                this.onmouseout = function(e) {
                        e = e || document.parentWindow.event;
                        var p = e.toElement || e.relatedTarget;
                        while (p && p != this) try { p = p.parentNode } catch(e) { p = this; };
                        if (p == this) return false;
                        if(o.currentTR) {
                                o.currentTR.className = ""; 
                                o.currentTR = null;
                        };
                        
                        if(o.statusBar) { 
                                o.updateStatus(o.getBespokeTitle(o.date.getFullYear(),o.date.getMonth() + 1,o.date.getDate()) || printFormattedDate(o.date, o.statusFormat, true));                                
                        };                          
                };
                this.onmouseover = function(e) {
                        e = e || document.parentWindow.event;
                        var el = e.target != null ? e.target : e.srcElement;
                        while(el.nodeType != 1) { el = el.parentNode; }; 
                                
                        if(!el || ! el.tagName) { return; };                              
                                
                        var statusText = getTitleTranslation(9);
                        switch (el.tagName.toLowerCase()) {
                                case "td":                                            
                                        if(el.className.search(/date-picker-unused|out-of-range/) != -1) {
                                                statusText = getTitleTranslation(9);
                                        } if(el.className.search(/cd-([0-9]{8})/) != -1) {                                                                                               
                                                o.stopTimer();
                                                var cellDate = el.className.match(/cd-([0-9]{8})/)[1];                                                                                                                          
                                                
                                                o.removeOldFocus();
                                                el.id = o.id+"-date-picker-hover";
                                                o.setNewFocus();
                                                                                       
                                                o.date = new Date(+cellDate.substr(0,4),+cellDate.substr(4,2)-1,+cellDate.substr(6,2));                                                
                                                if(!o.noToday) { o.disableTodayButton(); };
                                                
                                                statusText = o.getBespokeTitle(+cellDate.substr(0,4),+cellDate.substr(4,2),+cellDate.substr(6,2)) || printFormattedDate(o.date, o.statusFormat, true);                                                
                                        };
                                        break;
                                case "th":
                                        if(!o.statusBar) { break; };
                                        if(el.className.search(/drag-enabled/) != -1) {
                                                statusText = getTitleTranslation(10);
                                        } else if(el.className.search(/date-picker-week-header/) != -1) {
                                                var txt = el.firstChild ? el.firstChild.nodeValue : "";
                                                statusText = txt.search(/^(\d+)$/) != -1 ? getTitleTranslation(7, [txt, txt < 3 && o.date.getMonth() == 11 ? getWeeksInYear(o.date.getFullYear()) + 1 : getWeeksInYear(o.date.getFullYear())]) : getTitleTranslation(9);
                                        };
                                        break;
                                case "span":
                                        if(!o.statusBar) { break; };
                                        if(el.className.search(/drag-enabled/) != -1) {
                                                statusText = getTitleTranslation(10);
                                        } else if(el.className.search(/day-([0-6])/) != -1) {
                                                var day = el.className.match(/day-([0-6])/)[1];
                                                statusText = getTitleTranslation(11, [getDayTranslation(day, false)]);
                                        } else if(el.className.search(/prev-year/) != -1) {
                                                statusText = getTitleTranslation(2);
                                        } else if(el.className.search(/prev-month/) != -1) {
                                                statusText = getTitleTranslation(0);
                                        } else if(el.className.search(/next-year/) != -1) {
                                                statusText = getTitleTranslation(3);
                                        } else if(el.className.search(/next-month/) != -1) {
                                                statusText = getTitleTranslation(1);
                                        } else if(el.className.search(/today-but/) != -1 && el.className.search(/disabled/) == -1) {
                                                statusText = getTitleTranslation(12);
                                        };
                                        break;
                                default:
                                        statusText = "";
                        };
                        while(el.parentNode) {
                                el = el.parentNode;
                                if(el.nodeType == 1 && el.tagName.toLowerCase() == "tr") {                                                  
                                        if(o.currentTR) {
                                                if(el == o.currentTR) break;
                                                o.currentTR.className = ""; 
                                        };                                                 
                                        el.className = "dp-row-highlight";
                                        o.currentTR = el;
                                        break;
                                };
                        };                                                          
                        if(o.statusBar && statusText) { o.updateStatus(statusText); };                                 
                }; 
                this.clearTimer = function() {
                        o.stopTimer();
                        o.timerInc      = 800;
                        o.yearInc       = 0;
                        o.monthInc      = 0;
                        o.dayInc        = 0;
                        
                        removeEvent(document, "mouseup", o.clearTimer);
                        if(o.mouseDownElem != null) {
                                removeEvent(o.mouseDownElem, "mouseout",  o.clearTimer);
                        };
                        o.mouseDownElem = null;
                };    
                
                var o = this;                 
                
                this.setDateFromInput();
                
                if(this.staticPos) {                          
                        this.create();                                               
                } else { 
                        this.createButton();                                               
                };
               
                (function() {
                        var elemID, elem;
                        
                        for(elemID in o.formElements) {                              
                                elem = document.getElementById(elemID);
                                if(elem && elem.tagName && elem.tagName.search(/select|input/i) != -1) {                                                                     
                                        addEvent(elem, "change", o.changeHandler);                                
                                };
                                
                                if(!elem || elem.disabled == true) {
                                        o.disableDatePicker();
                                };                         
                        };                                      
                })();   
                
                
                // We have fully created the datepicker...
                this.fullCreate = true;
                
                
        };
        datePicker.prototype.addButtonEvents = function(but) {
               function buttonEvent (e) {
                        e = e || window.event;                      
                        
                        var inpId     = this.id.replace('fd-but-',''),
                            dpVisible = isVisible(inpId),
                            autoFocus = false,
                            kbEvent   = datePickers[inpId].kbEvent;
                            
                        if(kbEvent) {
                                datePickers[inpId].kbEvent = false;
                                return;
                        };

                        if(e.type == "keydown") {
                                datePickers[inpId].kbEvent = true;
                                var kc = e.keyCode != null ? e.keyCode : e.charCode;
                                if(kc != 13) return true; 
                                if(dpVisible) {
                                        this.className = this.className.replace("dp-button-active", "");                                          
                                        hideAll();
                                        return stopEvent(e);
                                };                                   
                                autoFocus = true;
                        } else {
                                datePickers[inpId].kbEvent = false;
                        };

                        this.className = this.className.replace("dp-button-active", "");
                        
                        if(!dpVisible) {                                 
                                this.className += " dp-button-active";
                                hideAll(inpId);                                                             
                                showDatePicker(inpId, autoFocus);
                        } else {
                                hideAll();
                        };
                
                        return stopEvent(e);
                };
                
                but.onkeydown = buttonEvent;
                but.onclick = buttonEvent;
                
                if(!buttonTabIndex || this.bespokeTabIndex === false) {
                        but.setAttribute(!/*@cc_on!@*/false ? "tabIndex" : "tabindex", "-1");
                        but.tabIndex = -1; 
                        but.onkeydown = null; 
                        removeEvent(but, "keydown", buttonEvent);
                } else {
                        but.setAttribute(!/*@cc_on!@*/false ? "tabIndex" : "tabindex", this.bespokeTabIndex);
                        but.tabIndex = this.bespokeTabIndex;
                };                              
        };
        
        datePicker.prototype.createButton = function() {
                
                if(this.staticPos || document.getElementById("fd-but-" + this.id)) { return; };

                var inp         = document.getElementById(this.id),
                    span        = document.createElement('span'),
                    but         = document.createElement('a');

                but.href        = "#" + this.id;
                but.className   = "date-picker-control";
                but.title       = getTitleTranslation(5);
                but.id          = "fd-but-" + this.id;
                                
                span.appendChild(document.createTextNode(nbsp));
                but.appendChild(span);

                span = document.createElement('span');
                span.className = "fd-screen-reader";
                span.appendChild(document.createTextNode(but.title));
                but.appendChild(span);
                
                // Set the ARIA role to be "button"
                setARIARole(but, "button");                 
                
                // Set a "haspopup" ARIA property - should this not be a list if ID's????
                setARIAProperty(but, "haspopup", true);
                                             			                	
                if(this.positioned && document.getElementById(this.positioned)) {
                        document.getElementById(this.positioned).appendChild(but);
                } else {
                        inp.parentNode.insertBefore(but, inp.nextSibling);
                };                   
                
                this.addButtonEvents(but);

                but = null;
                
                this.callback("dombuttoncreate", {id:this.id});
        };
        datePicker.prototype.setBespokeTitles = function(titles) {                
                this.bespokeTitles = titles;               
        }; 
        datePicker.prototype.addBespokeTitles = function(titles) {                
                for(var dt in titles) {
                        this.bespokeTitles[dt] = titles[dt];
                };              
        }; 
        datePicker.prototype.getBespokeTitle = function(y,m,d) {
                var dt, dtFull, yyyymmdd = y + String(pad(m)) + pad(d);
                
                // Try this datepickers bespoke titles
                for(dt in this.bespokeTitles) {
                        dtFull = dt.replace(/^(\*\*\*\*)/, y).replace(/^(\d\d\d\d)(\*\*)/, "$1"+ pad(m));        
                        if(dtFull == yyyymmdd) return this.bespokeTitles[dt];
                };
                                
                // Try the generic bespoke titles
                for(dt in bespokeTitles) {
                        dtFull = dt.replace(/^(\*\*\*\*)/, y).replace(/^(\d\d\d\d)(\*\*)/, "$1"+ pad(m));        
                        if(dtFull == yyyymmdd) return bespokeTitles[dt];
                };
                
                return false;             
        };
        datePicker.prototype.returnSelectedDate = function() {                
                return this.dateSet;                
        };   
        datePicker.prototype.setRangeLow = function(range) {
                this.rangeLow = (String(range).search(/^(\d\d\d\d)(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])$/) == -1) ? false : range;                                              
                if(!this.inUpdate) this.setDateFromInput();                
        };
        datePicker.prototype.setRangeHigh = function(range) {
                this.rangeHigh = (String(range).search(/^(\d\d\d\d)(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])$/) == -1) ? false : range;                                               
                if(!this.inUpdate) this.setDateFromInput();                
        };
        datePicker.prototype.setDisabledDays = function(dayArray) {
                if(!dayArray.length || dayArray.length != 7 || dayArray.join("").search(/^([0|1]{7})$/) == -1) {
                        return false;
                };                
                this.disabledDays = dayArray;                 
                if(!this.inUpdate) this.setDateFromInput();    
        };
        datePicker.prototype.setDisabledDates = function(dateObj) {                               
                this.disabledDates  = {};               
                this.addDisabledDates(dateObj);                
        }; 
        datePicker.prototype.setEnabledDates = function(dateObj) {                               
                this.enabledDates = {};                 
                this.addEnabledDates(dateObj);                
        };         
        datePicker.prototype.addDisabledDates = function(dateObj) {                    
                this.enabledDates  = false; 
                this.disabledDates = this.disabledDates || {};
                
                var startD;
                for(startD in dateObj) {
                        if((String(startD).search(wcDateRegExp) != -1 && dateObj[startD] == 1) || (String(startD).search(rangeRegExp) != -1 && String(dateObj[startD]).search(rangeRegExp) != -1)) {
                                this.disabledDates[startD] = dateObj[startD];
                        };
                };
                           
                if(!this.inUpdate) this.setDateFromInput();                                                                
        };
        datePicker.prototype.addEnabledDates = function(dateObj) {
                this.disabledDates = false; 
                this.enabledDates  = this.enabledDates || {};
                
                var startD;
                for(startD in dateObj) {
                        if((String(startD).search(wcDateRegExp) != -1 && dateObj[startD] == 1) || (String(startD).search(rangeRegExp) != -1 && String(dateObj[startD]).search(rangeRegExp) != -1)) {
                                this.enabledDates[startD] = dateObj[startD];
                        };
                };
                            
                if(!this.inUpdate) this.setDateFromInput();                                                                   
        };
        datePicker.prototype.setSelectedDate = function(yyyymmdd) {                                             
                if(String(yyyymmdd).search(wcDateRegExp) == -1) {
                        return false;
                };  
                
                var match = yyyymmdd.match(rangeRegExp),
                    dt    = new Date(+match[2],+match[3]-1,+match[4]);
                
                if(!dt || isNaN(dt) || !this.canDateBeSelected(dt)) {
                        return false;
                };
                    
                this.dateSet = new Date(dt);
                
                if(!this.inUpdate) this.updateTable();
                
                this.callback("dateset", this.createCbArgObj());
                this.returnFormattedDate();                                         
        };
        datePicker.prototype.checkSelectedDate = function() {                
                if(this.dateSet && !this.canDateBeSelected(this.dateSet)) {                        
                        this.dateSet = null;
                };
                if(!this.inUpdate) this.updateTable();
        };
        datePicker.prototype.addOnFocusEvents = function() {                              
                if(this.kbEventsAdded || this.noFocus) {                         
                        return;
                };
                
                addEvent(document, "keypress", this.onkeydown);
                addEvent(document, "mousedown", this.onmousedown);
                
                /*@cc_on
                @if(@_win32)
                removeEvent(document, "keypress", this.onkeydown);
                addEvent(document, "keydown", this.onkeydown);                 
                @end
                @*/
                if(window.devicePixelRatio) {
                        removeEvent(document, "keypress", this.onkeydown);
                        addEvent(document, "keydown", this.onkeydown);
                };             
                this.noFocus = false;   
                this.kbEventsAdded = true;                
        };         
        datePicker.prototype.removeOnFocusEvents = function() {
                
                if(!this.kbEventsAdded) { return; };
                
                removeEvent(document, "keypress",  this.onkeydown);
                removeEvent(document, "keydown",   this.onkeydown);
                removeEvent(document, "mousedown", this.onmousedown);                 
                
                this.kbEventsAdded = false;                 
        };         
        datePicker.prototype.stopTimer = function() {
                this.timerSet = false;
                window.clearTimeout(this.timer);
        };
        datePicker.prototype.setOpacity = function(op) {
                this.div.style.opacity = op/100;
                this.div.style.filter = 'alpha(opacity=' + op + ')';
                this.opacity = op;
        };         
        datePicker.prototype.getDates = function(y, m) {                
                var dpm = daysInMonth(m - 1, y),
                    obj = {},
                    dds = this.getGenericDates(y, m, false), 
                    eds = this.getGenericDates(y, m, true), 
                    dts = y + pad(m);                
                   
                for(var i = 1; i <= dpm; i++) {
                        dt = dts + "" + pad(i);
                        
                        if(dds) {                                
                                obj[dt] = (dt in dds) ? 0 : 1;                                
                        } else if(eds) {
                                obj[dt] = (dt in eds) ? 1 : 0;
                        } else {
                                obj[dt] = 1;
                        };                                                    
                };
                
                return obj;
        }; 
        datePicker.prototype.getGenericDates = function(y, m, enabled) {
                var deDates = enabled ? this.enabledDates : this.disabledDates;                
                
                if(!deDates) {                        
                        return false;
                };
                
                m = pad(m);                 
                
                var obj    = {},            
                    lower  = this.firstDateShown,
                    upper  = this.lastDateShown,                            
                    dt1, dt2, rngLower, rngUpper;  
                
                if(!upper || !lower) {
                        lower = this.firstDateShown = y + pad(m) + "01";
                        upper = this.lastDateShown  = y + pad(m) + pad(daysInMonth(m, y));                        
                };                 
                
                for(dt in deDates) {                         
                        dt1 = dt.replace(/^(\*\*\*\*)/, y).replace(/^(\d\d\d\d)(\*\*)/, "$1"+m);
                        dt2 = deDates[dt];
                        
                        if(dt2 == 1) {                                                
                                if(Number(dt1.substr(0,6)) >= +String(this.firstDateShown).substr(0,6)
                                   && 
                                   Number(dt1.substr(0,6)) <= +String(this.lastDateShown).substr(0,6)) {
                                        obj[dt1] = 1;                                                              
                                };
                                continue; 
                        };                       
                        
                        // Range
                        if(+String(this.firstDateShown).substr(0,6) >= Number(dt1.substr(0,6))
                           &&
                           +String(this.lastDateShown).substr(0,6) <= Number(dt2.substr(0,6))) {                                              
                                // Same month
                                if(Number(dt1.substr(0,6)) == Number(dt2.substr(0,6))) {
                                        for(var i = dt1; i <= dt2; i++) {
                                                obj[i] = 1;                                                                                                
                                        };
                                        continue;
                                };

                                // Different months but we only want this month
                                rngLower = Number(dt1.substr(0,6)) == +String(this.firstDateShown).substr(0,6) ? dt1 : lower;
                                rngUpper = Number(dt2.substr(0,6)) == +String(this.lastDateShown).substr(0,6) ? dt2 : upper;
                                for(var i = +rngLower; i <= +rngUpper; i++) {
                                        obj[i] = 1;                                                                                    
                                };
                        };
                };
                return obj;
        };       
        datePicker.prototype.truePosition = function(element) {
                var pos = this.cumulativeOffset(element);
                var iebody      = (document.compatMode && document.compatMode != "BackCompat")? document.documentElement : document.body,
                    dsocleft    = document.all ? iebody.scrollLeft : window.pageXOffset,
                    dsoctop     = document.all ? iebody.scrollTop  : window.pageYOffset,
                    posReal     = this.realOffset(element);
                return [pos[0] - posReal[0] + dsocleft, pos[1] - posReal[1] + dsoctop];
        };
        datePicker.prototype.realOffset = function(element) {
                var t = 0, l = 0;
                do {
                        t += element.scrollTop  || 0;
                        l += element.scrollLeft || 0;
                        element = element.parentNode;
                } while(element);
                return [l, t];
        };
        datePicker.prototype.cumulativeOffset = function(element) {
                var t = 0, l = 0;
                do {
                        t += element.offsetTop  || 0;
                        l += element.offsetLeft || 0;
                        element = element.offsetParent;
                } while(element);
                return [l, t];
        };
        datePicker.prototype.equaliseDates = function() {
                var clearDayFound = false, tmpDate;
                for(var i = this.rangeLow; i <= this.rangeHigh; i++) {
                        tmpDate = String(i);
                        if(!this.disabledDays[new Date(tmpDate.substr(0,4), tmpDate.substr(6,2), tmpDate.substr(4,2)).getDay() - 1]) {
                                clearDayFound = true;
                                break;
                        };
                };
                if(!clearDayFound) { this.disabledDays = [0,0,0,0,0,0,0] };
        };
        datePicker.prototype.outOfRange = function(tmpDate) {
                
                if(!this.rangeLow && !this.rangeHigh) { return false; };
                var level = false;
                if(!tmpDate) {
                        level   = true;
                        tmpDate = this.date;
                };

                var d  = pad(tmpDate.getDate()),
                    m  = pad(tmpDate.getMonth() + 1),
                    y  = tmpDate.getFullYear(),
                    dt = String(y)+String(m)+String(d);

                if(this.rangeLow && +dt < +this.rangeLow) {
                        if(!level) { return true; };
                        this.date = new Date(this.rangeLow.substr(0,4), this.rangeLow.substr(4,2)-1, this.rangeLow.substr(6,2), 5, 0, 0);
                        return false;
                };
                if(this.rangeHigh && +dt > +this.rangeHigh) {
                        if(!level) { return true; };
                        this.date = new Date(this.rangeHigh.substr(0,4), this.rangeHigh.substr(4,2)-1, this.rangeHigh.substr(6,2), 5, 0, 0);
                };
                return false;
        };  
        datePicker.prototype.canDateBeSelected = function(tmpDate) {
                if(!tmpDate) return false;
                                                               
                var d  = pad(tmpDate.getDate()),
                    m  = pad(tmpDate.getMonth() + 1),
                    y  = tmpDate.getFullYear(),
                    dt = String(y)+String(m)+String(d),
                    dd = this.getDates(y, m),                    
                    wd = tmpDate.getDay() == 0 ? 7 : tmpDate.getDay();               
                
                if((this.rangeLow && +dt < +this.rangeLow) || (this.rangeHigh && +dt > +this.rangeHigh) || (dd[dt] == 0) || this.disabledDays[wd-1]) {
                        return false;
                };
                
                return true;
        };        
        datePicker.prototype.updateStatus = function(msg) {                                
                while(this.statusBar.firstChild) { this.statusBar.removeChild(this.statusBar.firstChild); };
                
                if(msg && this.statusFormat.search(/-S|S-/) != -1 && msg.search(/([0-9]{1,2})(st|nd|rd|th)/) != -1) {                
                        msg = msg.replace(/([0-9]{1,2})(st|nd|rd|th)/, "$1<sup>$2</sup>").split(/<sup>|<\/sup>/);                                                 
                        var dc = document.createDocumentFragment();
                        for(var i = 0, nd; nd = msg[i]; i++) {
                                if(/^(st|nd|rd|th)$/.test(nd)) {
                                        var sup = document.createElement("sup");
                                        sup.appendChild(document.createTextNode(nd));
                                        dc.appendChild(sup);
                                } else {
                                        dc.appendChild(document.createTextNode(nd));
                                };
                        };
                        this.statusBar.appendChild(dc);                        
                } else {                        
                        this.statusBar.appendChild(document.createTextNode(msg ? msg : getTitleTranslation(9)));                                                 
                };                                    
        };
        datePicker.prototype.setDateFromInput = function() {
                var origDateSet = this.dateSet,
                    m = false,
                    dt, elemID, elem, elemFmt, d, y, elemVal;
                
                this.dateSet = null;
                   
                for(elemID in this.formElements) {
                        elem = document.getElementById(elemID);
                        
                        if(!elem) {
                                return;
                        };
                        
                        elemVal = String(elem.value);
                        elemFmt = this.formElements[elemID];
                        dt      = false;
                        
                        if(!(elemVal == "")) {                        
                                for(var i = 0, fmt; fmt = this.formatMasks[elemID][i]; i++) {                                        
                                        dt = parseDateString(elemVal, fmt);                                                              
                                        if(dt) {                                                                                       
                                                break;
                                        };                                
                                }; 
                        };
                        
                        if(dt) {
                                if(elemFmt.search(new RegExp('[' + dParts + ']')) != -1) {
                                        //console.log("located d part " + elemFmt + " : " + dt.getDate());
                                        d = dt.getDate();        
                                };
                                if(elemFmt.search(new RegExp('[' + mParts + ']')) != -1) { 
                                        //console.log("located m part " + elemFmt + " : " + dt.getMonth());                                       
                                        m = dt.getMonth();                                               
                                };
                                if(elemFmt.search(new RegExp('[' + yParts + ']')) != -1) {
                                        //console.log("located y part " + elemFmt + " : " + dt.getFullYear());
                                        y = dt.getFullYear()        
                                };                        
                        };                                            
                };
                
                dt = false;
                
                if(d && !(m === false) && y) {                                            
                        if(+d > daysInMonth(+m, +y)) { 
                                d  = daysInMonth(+m, +y);
                                dt = false;
                        } else {
                                dt = new Date(+y, +m, +d);
                        };
                };
               
                if(!dt || isNaN(dt)) {                        
                        var newDate = new Date(y || new Date().getFullYear(), !(m === false) ? m : new Date().getMonth(), 1);
                        this.date = this.cursorDate ? new Date(+this.cursorDate.substr(0,4), +this.cursorDate.substr(4,2) - 1, +this.cursorDate.substr(6,2)) : new Date(newDate.getFullYear(), newDate.getMonth(), Math.min(+d || new Date().getDate(), daysInMonth(newDate.getMonth(), newDate.getFullYear())));
                        
                        this.date.setHours(5);
                        this.outOfRange();                         
                        //this.callback("dateset", this.createCbArgObj());  
                        this.updateTable();                         
                        return;
                };

        
                dt.setHours(5);
                this.date = new Date(dt);                            
                this.outOfRange();                 
                
                if(dt.getTime() == this.date.getTime() && this.canDateBeSelected(this.date)) {                                              
                        this.dateSet = new Date(this.date);
                };
                
                //this.callback("dateset", this.createCbArgObj()); 
                if(this.fullCreate) this.updateTable();
                this.returnFormattedDate(true);
        };
        datePicker.prototype.setSelectIndex = function(elem, indx) {
                for(var opt = elem.options.length-1; opt >= 0; opt--) {
                        if(elem.options[opt].value == indx) {
                                elem.selectedIndex = opt;
                                return;
                        };
                };
        };
        datePicker.prototype.returnFormattedDate = function(noFocus) {     
                if(!this.dateSet) {                                
                        return;
                };
                
                var d   = pad(this.dateSet.getDate()),
                    m   = pad(this.dateSet.getMonth() + 1),
                    y   = this.dateSet.getFullYear(),
                    el  = false, 
                    elemID, elem, elemFmt, fmtDate;
                
                noFocus = !!noFocus;
                 
                for(elemID in this.formElements) {
                        elem    = document.getElementById(elemID);
                        
                        if(!elem) return;
                        
                        if(!el) el = elem;
                        
                        elemFmt = this.formElements[elemID];
                        
                        fmtDate = printFormattedDate(this.dateSet, elemFmt, returnLocaleDate);                   
                        if(elem.tagName.toLowerCase() == "input") {
                                elem.value = fmtDate; 
                        } else {  
                                this.setSelectIndex(elem, fmtDate);                              
                        };
                };
                
                if(this.staticPos) { 
                        this.noFocus = true;
                        this.updateTable(); 
                        this.noFocus = false;
                };                         
                        
                if(this.fullCreate) {
                        if(el.type && el.type != "hidden" && !noFocus) { el.focus(); };                                                                                                                                             
                };         
        };
        datePicker.prototype.disableDatePicker = function() {
                if(this.disabled) return;
                
                if(this.staticPos) {
                        this.removeOnFocusEvents();
                        this.removeOldFocus();
                        this.noFocus = true;
                        this.div.className = this.div.className.replace(/dp-disabled/, "") + " dp-disabled";  
                        this.table.onmouseover = this.table.onclick = this.table.onmouseout = this.table.onmousedown = null;                                      
                        removeEvent(document, "mousedown", this.onmousedown);                         
                        removeEvent(document, "mouseup",   this.clearTimer);                       
                } else {  
                        if(this.visible) this.hide();                        
                        var but = document.getElementById("fd-but-" + this.id);
                        if(but) {
                                but.className = but.className.replace(/dp-disabled/, "") + " dp-disabled";
                                // Set a "disabled" ARIA state
                                setARIAProperty(but, "disabled", true);                               
                                but.onkeydown = but.onclick = function() { return false; }; 
                                but.setAttribute(!/*@cc_on!@*/false ? "tabIndex" : "tabindex", "-1");
                                but.tabIndex = -1;                
                        };                         
                };               
                                
                clearTimeout(this.timer);                
                this.disabled = true;  
        }; 
        datePicker.prototype.enableDatePicker = function() {
                if(!this.disabled) return;
                
                if(this.staticPos) {
                        this.removeOldFocus();
                        this.noFocus = true;                        
                        this.updateTable();
                        this.div.className = this.div.className.replace(/dp-disabled/, "");
                        this.disabled = false;                         
                        this.table.onmouseover = this.onmouseover;
                        this.table.onmouseout  = this.onmouseout;
                        this.table.onclick     = this.onclick;                         
                        this.table.onmousedown = this.onmousedown;                                                                    
                } else {                         
                        var but = document.getElementById("fd-but-" + this.id);
                        if(but) {
                                but.className = but.className.replace(/dp-disabled/, "");
                                // Reset the "disabled" ARIA state
                                setARIAProperty(but, "disabled", false);
                                this.addButtonEvents(but);                                                
                        };                         
                };
                
                this.disabled = false;                
        };
        datePicker.prototype.disableTodayButton = function() {
                var today = new Date();                     
                this.butToday.className = this.butToday.className.replace("fd-disabled", "");
                if(this.outOfRange(today) || (this.date.getDate() == today.getDate() && this.date.getMonth() == today.getMonth() && this.date.getFullYear() == today.getFullYear())) {
                        this.butToday.className += " fd-disabled";                          
                };
        };
        datePicker.prototype.updateTableHeaders = function() {
                var colspanTotal = this.showWeeks ? 8 : 7,
                    colOffset    = this.showWeeks ? 1 : 0,
                    d, but;

                for(var col = colOffset; col < colspanTotal; col++ ) {
                        d = (this.firstDayOfWeek + (col - colOffset)) % 7;
                        this.ths[col].title = getDayTranslation(d, false);

                        if(col > colOffset) {
                                but = this.ths[col].getElementsByTagName("span")[0];
                                while(but.firstChild) { but.removeChild(but.firstChild); };
                                but.appendChild(document.createTextNode(getDayTranslation(d, true)));
                                but.title = this.ths[col].title;
                                but.className = but.className.replace(/day-([0-6])/, "") + " day-" + d;
                                but = null;
                        } else {
                                while(this.ths[col].firstChild) { this.ths[col].removeChild(this.ths[col].firstChild); };
                                this.ths[col].appendChild(document.createTextNode(getDayTranslation(d, true)));
                        };

                        this.ths[col].className = this.ths[col].className.replace(/date-picker-highlight/g, "");
                        if(this.highlightDays[d]) {
                                this.ths[col].className += " date-picker-highlight";
                        };
                };
                
                if(this.created) { this.updateTable(); }
        }; 
        datePicker.prototype.callback = function(type, args) {   
                if(!type || !(type in this.callbacks)) { 
                        return false; 
                };
                
                var ret = false;                   
                for(var func = 0; func < this.callbacks[type].length; func++) {                         
                        ret = this.callbacks[type][func](args || this.id);                        
                };                      
                return ret;
        };      
        datePicker.prototype.showHideButtons = function(tmpDate) {
                if(!this.butPrevYear) { return; };
                
                var tdm = tmpDate.getMonth(),
                    tdy = tmpDate.getFullYear();

                if(this.outOfRange(new Date((tdy - 1), tdm, daysInMonth(+tdm, tdy-1)))) {                            
                        if(this.butPrevYear.className.search(/fd-disabled/) == -1) {
                                this.butPrevYear.className += " fd-disabled";
                        };
                        if(this.yearInc == -1) this.stopTimer();
                } else {
                        this.butPrevYear.className = this.butPrevYear.className.replace(/fd-disabled/g, "");
                };                 
                
                if(this.outOfRange(new Date(tdy, (+tdm - 1), daysInMonth(+tdm-1, tdy)))) {                           
                        if(this.butPrevMonth.className.search(/fd-disabled/) == -1) {
                                this.butPrevMonth.className += " fd-disabled";
                        };
                        if(this.monthInc == -1) this.stopTimer();
                } else {
                        this.butPrevMonth.className = this.butPrevMonth.className.replace(/fd-disabled/g, "");
                };
         
                if(this.outOfRange(new Date((tdy + 1), +tdm, 1))) {                            
                        if(this.butNextYear.className.search(/fd-disabled/) == -1) {
                                this.butNextYear.className += " fd-disabled";
                        };
                        if(this.yearInc == 1) this.stopTimer();
                } else {
                        this.butNextYear.className = this.butNextYear.className.replace(/fd-disabled/g, "");
                };                
                
                if(this.outOfRange(new Date(tdy, +tdm + 1, 1))) {
                        if(this.butNextMonth.className.search(/fd-disabled/) == -1) {
                                this.butNextMonth.className += " fd-disabled";
                        };
                        if(this.monthInc == 1) this.stopTimer();
                } else {
                        this.butNextMonth.className = this.butNextMonth.className.replace(/fd-disabled/g, "");
                };
        };        
        var localeDefaults = {
                fullMonths:["January","February","March","April","May","June","July","August","September","October","November","December"],
                monthAbbrs:["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"],
                fullDays:  ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"],
                dayAbbrs:  ["Mon","Tue","Wed","Thu","Fri","Sat","Sun"],
                titles:    ["Previous month","Next month","Previous year","Next year", "Today", "Show Calendar", "wk", "Week [[%0%]] of [[%1%]]", "Week", "Select a date", "Click \u0026 Drag to move", "Display \u201C[[%0%]]\u201D first", "Go to Today\u2019s date", "Disabled date :"],
                firstDayOfWeek:0,
                imported:  false
        };        
        var joinNodeLists = function() {
                if(!arguments.length) { return []; }
                var nodeList = [];
                for (var i = 0; i < arguments.length; i++) {
                        for (var j = 0, item; item = arguments[i][j]; j++) {
                                nodeList[nodeList.length] = item;
                        };
                };
                return nodeList;
        };
        var cleanUp = function() {
                var dp, fe;
                for(dp in datePickers) {
                        for(fe in datePickers[dp].formElements) {
                                if(!document.getElementById(fe)) {
                                        datePickers[dp].destroy();
                                        datePickers[dp] = null;
                                        delete datePickers[dp];
                                        break;
                                }
                        };
                };
        };         
        var hideAll = function(exception) {
                var dp;
                for(dp in datePickers) {
                        if(!datePickers[dp].created || (exception && exception == datePickers[dp].id)) continue;
                        datePickers[dp].hide();
                };
        };
        var hideDatePicker = function(inpID) {
                if(inpID in datePickers) {
                        if(!datePickers[inpID].created || datePickers[inpID].staticPos) return;
                        datePickers[inpID].hide();
                };
        };
        var showDatePicker = function(inpID, autoFocus) {                
                if(!(inpID in datePickers)) return false;   
                
                datePickers[inpID].clickActivated = !!!autoFocus;             
                datePickers[inpID].show(autoFocus);
                return true;        
        };
        var destroy = function(e) {
                e = e || window.event;
                
                // Don't remove datepickers if it's a pagehide/pagecache event (webkit et al)
                if(e.persisted) {
                        return;
                };
                
                for(dp in datePickers) {
                        datePickers[dp].destroy();
                        datePickers[dp] = null;
                        delete datePickers[dp];
                };
                datePickers = null;
                
                removeEvent(window, 'unload', datePickerController.destroy);
        }; 
        var destroySingleDatePicker = function(id) {
                if(id && (id in datePickers)) {
                        datePickers[id].destroy();
                        datePickers[id] = null;
                        delete datePickers[id];        
                };
        };
        var onPickerBlur = function(id, handler) {
            if(id && (id in datePickers)) {
                    datePickers[id].onPickerBlur = handler;
            };
        };
        var getTitleTranslation = function(num, replacements) {
                replacements = replacements || [];
                if(localeImport.titles.length > num) {
                         var txt = localeImport.titles[num];
                         if(replacements && replacements.length) {
                                for(var i = 0; i < replacements.length; i++) {
                                        txt = txt.replace("[[%" + i + "%]]", replacements[i]);
                                };
                         };
                         return txt.replace(/[[%(\d)%]]/g,"");
                };
                return "";
        };
        var getDayTranslation = function(day, abbreviation) {
                var titles = localeImport[abbreviation ? "dayAbbrs" : "fullDays"];
                return titles.length && titles.length > day ? titles[day] : "";
        };
        var getMonthTranslation = function(month, abbreviation) {
                var titles = localeImport[abbreviation ? "monthAbbrs" : "fullMonths"];
                return titles.length && titles.length > month ? titles[month] : "";
        };
        var daysInMonth = function(nMonth, nYear) {
                nMonth = (nMonth + 12) % 12;
                return (((0 == (nYear%4)) && ((0 != (nYear%100)) || (0 == (nYear%400)))) && nMonth == 1) ? 29: [31,28,31,30,31,30,31,31,30,31,30,31][nMonth];
        };
        
        var getWeeksInYear = function(Y) {
                if(Y in weeksInYearCache) {
                        return weeksInYearCache[Y];
                };
                var X1, X2, NW;
                with (X1 = new Date(Y, 0, 4)) {
                        setDate(getDate() - (6 + getDay()) % 7);
                };
                with (X2 = new Date(Y, 11, 28)) {
                        setDate(getDate() + (7 - getDay()) % 7);
                };
                weeksInYearCache[Y] = Math.round((X2 - X1) / 604800000);
                return weeksInYearCache[Y];
        };

        var getWeekNumber = function(y,m,d) {
                var d = new Date(y, m, d, 0, 0, 0);
                var DoW = d.getDay();
                d.setDate(d.getDate() - (DoW + 6) % 7 + 3); // Nearest Thu
                var ms = d.valueOf(); // GMT
                d.setMonth(0);
                d.setDate(4); // Thu in Week 1
                return Math.round((ms - d.valueOf()) / (7 * 864e5)) + 1;
        };

        var printFormattedDate = function(date, fmt, useImportedLocale) {
                if(!date || isNaN(date)) { return ""; };                
                
                var parts = fmt.split("-"),
                      str = [],
                        d = date.getDate(),
                        D = date.getDay(),
                        m = date.getMonth(),
                        y = date.getFullYear(),
                    flags = {
                                "sp":" ",
                                "dt":".",
                                "sl":"/",
                                "ds":"-",
                                "cc":",",
                                "d":pad(d),
                                "D":useImportedLocale ? localeImport.dayAbbrs[D == 0 ? 6 : D - 1] : localeDefaults.dayAbbrs[D == 0 ? 6 : D - 1],
                                "l":useImportedLocale ? localeImport.fullDays[D == 0 ? 6 : D - 1] : localeDefaults.fullDays[D == 0 ? 6 : D - 1],
                                "j":d,
                                "N":D == 0 ? 7 : D,
                                "w":D,                                
                                "W":getWeekNumber(y,m,d),
                                "M":useImportedLocale ? localeImport.monthAbbrs[m] : localeDefaults.monthAbbrs[m],
                                "F":useImportedLocale ? localeImport.fullMonths[m] : localeDefaults.fullMonths[m],
                                "m":pad(m + 1),
                                "n":m + 1,
                                "t":daysInMonth(m, y),
                                "y":String(y).substr(2,2),                                
                                "Y":y,
                                "S":["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
                            };    
                
                for(var pt = 0, part; part = parts[pt]; pt++) {  
                        str.push(!(part in flags) ? "" : flags[part]);
                };
                
                return str.join("");
        };
        var parseDateString = function(str, fmt) {
                var d     = false,
                    m     = false,
                    y     = false,
                    now   = new Date(),
                    parts = fmt.replace(/-sp(-sp)+/g, "-sp").split("-"),
                    divds = { "dt":".","sl":"/","ds":"-","cc":"," },
                    str   = "" + str;                    
            
                loopLabel:
                for(var pt = 0, part; part = parts[pt]; pt++) {                        
                        if(str.length == 0) { return false; };
                              
                        switch(part) {
                                // Dividers - be easy on them all i.e. accept them all when parsing...                                
                                case "sp":
                                case "dt":
                                case "sl":
                                case "ds":
                                case "cc":       
                                                str = str.replace(/^(\s|\.|\/|,|-){1,}/, "");                                     
                                                break;
                                // DAY
                                case "d": // Day of the month, 2 digits with leading zeros (01 - 31)
                                case "j": // Day of the month without leading zeros (1 - 31)  
                                          // Accept both when parsing                                                          
                                                if(str.search(/^(3[01]|[12][0-9]|0?[1-9])/) != -1) {
                                                        d = +str.match(/^(3[01]|[12][0-9]|0?[1-9])/)[0];
                                                        str = str.substr(str.match(/^(3[01]|[12][0-9]|0?[1-9])/)[0].length);                                                        
                                                        break;
                                                } else {                                                        
                                                        return "";
                                                };
                                case "D": // A textual representation of a day, three letters (Mon - Sun)
                                case "l": // A full textual representation of the day of the week (Monday - Sunday)
                                          // Accept English & imported locales and both modifiers                                                  
                                                l = localeDefaults.fullDays.concat(localeDefaults.dayAbbrs);                                                  
                                                if(localeImport.imported) {
                                                        l = l.concat(localeImport.fullDays).concat(localeImport.dayAbbrs);
                                                }; 
                                                
                                                for(var i = 0; i < l.length; i++) {
                                                        if(new RegExp("^" + l[i], "i").test(str)) {                                                                
                                                                str = str.substr(l[i].length);
                                                                continue loopLabel;
                                                        };
                                                };
                                                
                                                break;                                  
                                case "N": // ISO-8601 numeric representation of the day of the week (added in PHP 5.1.0) 1 (for Monday) through 7 (for Sunday)
                                case "w": // Numeric representation of the day of the week 0 (for Sunday) through 6 (for Saturday)
                                                if(str.search(part == "N" ? /^([1-7])/ : /^([0-6])/) != -1) {
                                                        str = str.substr(1);
                                                        
                                                };
                                                break;
                                case "S": // English ordinal suffix for the day of the month, 2 characters: st, nd, rd or th
                                                if(str.search(/^(st|nd|rd|th)/i) != -1) {
                                                        str = str.substr(2);                                                        
                                                };
                                                break;                                
                                // WEEK
                                case "W": // ISO-8601 week number of year, weeks starting on Monday (added in PHP 4.1.0): 1 - 53
                                                if(str.search(/^([1-9]|[1234[0-9]|5[0-3])/) != -1) {
                                                        str = str.substr(str.match(/^([1-9]|[1234[0-9]|5[0-3])/)[0].length);                                                        
                                                };
                                                break;
                                // MONTH
                                case "M": // A short textual representation of a month, three letters
                                case "F": // A full textual representation of a month, such as January or March
                                          // Accept English & imported locales and both modifiers                                                    
                                                l = localeDefaults.fullMonths.concat(localeDefaults.monthAbbrs);
                                                if(localeImport.imported) {
                                                        l = l.concat(localeImport.fullMonths).concat(localeImport.monthAbbrs);
                                                };
                                                for(var i = 0; i < l.length; i++) {                                                        
                                                        if(str.search(new RegExp("^" + l[i],"i")) != -1) {
                                                                str = str.substr(l[i].length);
                                                                m = ((i + 12) % 12);                                                                 
                                                                continue loopLabel;
                                                        };
                                                };
                                                return "";
                                case "m": // Numeric representation of a month, with leading zeros
                                case "n": // Numeric representation of a month, without leading zeros
                                          // Accept either when parsing
                                                l = /^(1[012]|0?[1-9])/;
                                                if(str.search(l) != -1) {
                                                        m = +str.match(l)[0] - 1;
                                                        str = str.substr(str.match(l)[0].length);
                                                        break;
                                                } else {                                                        
                                                        return "";
                                                };
                                case "t": // Number of days in the given month: 28 through 31
                                                if(str.search(/2[89]|3[01]/) != -1) {
                                                        str = str.substr(2);
                                                        break;
                                                };
                                                break;
                                // YEAR
                                case "Y": // A full numeric representation of a year, 4 digits
                                                if(str.search(/^(\d{4})/) != -1) {
                                                        y = str.substr(0,4);
                                                        str = str.substr(4);
                                                        break;
                                                } else {                                                        
                                                        return "";
                                                };
                                case "y": // A two digit representation of a year - be easy on four figure dates though                                                
                                                if(str.search(/^(\d{4})/) != -1) {
                                                        y = str.substr(0,4);
                                                        str = str.substr(4);
                                                        break;
                                                } else if(str.search(/^(0[0-9]|[1-9][0-9])/) != -1) {
                                                        y = str.substr(0,2);
                                                        y = +y < 50 ? '20' + "" + String(y) : '19' + "" + String(y);
                                                        str = str.substr(2);
                                                        break;
                                                } else return "";
                                       
                                default:
                                                return "";
                        };
                };   
                
                if(!(str == "") || (d === false && m === false && y === false)) {
                        return false;
                };                
                
                m = m === false ? 11                  : m;
                y = y === false ? now.getFullYear()   : y;
                d = d === false ? daysInMonth(+m, +y) : d;
                
                if(d > daysInMonth(+m, +y)) {
                        return false;
                };
                
                var tmpDate = new Date(y,m,d);
                
                return !tmpDate || isNaN(tmpDate) ? false : tmpDate;
        };        
        var findLabelForElement = function(element) {
                var label;
                if(element.parentNode && element.parentNode.tagName.toLowerCase() == "label") lebel = element.parentNode;
                else {
                        var labelList = document.getElementsByTagName('label');
                        // loop through label array attempting to match each 'for' attribute to the id of the current element
                        for(var lbl = 0; lbl < labelList.length; lbl++) {
                                // Internet Explorer requires the htmlFor test
                                if((labelList[lbl]['htmlFor'] && labelList[lbl]['htmlFor'] == element.id) || (labelList[lbl].getAttribute('for') == element.id)) {
                                        label = labelList[lbl];
                                        break;
                                };
                        };
                };
                
                if(label && !label.id) { label.id = element.id + "_label"; };
                return label;         
        };  
        var updateLanguage = function() {
                if(typeof(window.fdLocale) == "object" ) {                         
                        localeImport = {
                                titles          : fdLocale.titles,
                                fullMonths      : fdLocale.fullMonths,
                                monthAbbrs      : fdLocale.monthAbbrs,
                                fullDays        : fdLocale.fullDays,
                                dayAbbrs        : fdLocale.dayAbbrs,
                                firstDayOfWeek  : ("firstDayOfWeek" in fdLocale) ? fdLocale.firstDayOfWeek : 0,
                                imported        : true
                        };                                               
                } else if(!localeImport) {                        
                        localeImport = localeDefaults;
                };    
        };
        var loadLanguage = function() {
                updateLanguage();
                for(dp in datePickers) {
                        if(!datePickers[dp].created) continue;
                        datePickers[dp].updateTable();
                };   
        };
        var checkElem = function(elem) {                        
                return !(!elem || !elem.tagName || !((elem.tagName.toLowerCase() == "input" && (elem.type == "text" || elem.type == "hidden")) || elem.tagName.toLowerCase() == "select"));                
        };
        var addDatePicker = function(options) {  
                
                updateLanguage();
                
                if(!options.formElements) {
                        return;
                };
               
                options.id = (options.id && (options.id in options.formElements)) ? options.id : "";
                options.formatMasks = {};
                 
                var testParts  = [dParts,mParts,yParts],
                    partsFound = [0,0,0],
                    tmpPartsFound,
                    matchedPart,
                    newParts,
                    indParts,
                    fmt,
                    fmtBag,
                    fmtParts,
                    newFormats,
                    myMin,
                    myMax;               
                
                for(var elemID in options.formElements) {                
                        elem = document.getElementById(elemID);
                        
                        if(!checkElem(elem)) {
                                return false;
                        };
                        
                        if(!options.id) options.id = elemID;
                        
                        fmt             = options.formElements[elemID];
                        
                        if(!(fmt.match(validFmtRegExp))) {
                                return false;
                        };
                        
                        fmtBag          = [fmt];
                        
                        if(options.dateFormats && (elemID in options.dateFormats) && options.dateFormats[elemID].length) {
                                newFormats = [];
                                
                                for(var f = 0, bDft; bDft = options.dateFormats[elemID][f]; f++) {                                       
                                        if(!(bDft.match(validFmtRegExp))) {
                                                return false;
                                        };  
                                        
                                        newFormats.push(bDft); 
                                };
                                
                                fmtBag = fmtBag.concat(newFormats);  
                        };
                         
                        tmpPartsFound   = [0,0,0];                        
                        
                        for(var i = 0, testPart; testPart = testParts[i]; i++) {                                
                                if(fmt.search(new RegExp('('+testPart+')')) != -1) {
                                        partsFound[i] = tmpPartsFound[i] = 1;
                                        
                                        // Create the date format strings to check against later for text input elements
                                        if(elem.tagName.toLowerCase() == "input") {
                                                matchedPart = fmt.match(new RegExp('('+testPart+')'))[0];
                                                newParts    = String(matchedPart + "|" + testPart.replace(new RegExp("(" + matchedPart + ")"), "")).replace("||", "|");
                                                indParts    = newParts.split("|");
                                                newFormats  = [];
                                        
                                                for(var z = 0, bFmt; bFmt = fmtBag[z]; z++) {
                                                        for(var x = 0, indPart; indPart = indParts[x]; x++) {
                                                                if(indPart == matchedPart) continue;
                                                                newFormats.push(bFmt.replace(new RegExp('(' + testPart + ')(-|$)', 'g'), indPart + "-").replace(/-$/, ""));
                                                        };
                                                };
                                        
                                                fmtBag = fmtBag.concat(newFormats);
                                        };
                                };
                        };                        
                        
                        options.formatMasks[elemID] = fmtBag.concat();
                        
                        if(elem.tagName.toLowerCase() == "select") {
                                myMin = myMax = 0;
                        
                                // If we have a selectList, then try to parse the higher and lower limits 
                                var selOptions = elem.options;
                                
                                // Check the yyyymmdd 
                                if(tmpPartsFound[0] && tmpPartsFound[1] && tmpPartsFound[2]) { 
                                        var yyyymmdd, 
                                            cursorDate = false;
                                        
                                        // Remove the disabledDates parameter
                                        if("disabledDates" in options) {
                                                delete(options.disabledDates);
                                        };
                                        
                                        // Dynamically calculate the available "enabled" dates
                                        options.enabledDates = {};
                                            
                                        for(i = 0; i < selOptions.length; i++) {
                                                for(var f = 0, fmt; fmt = fmtBag[f]; f++) {
                                                        dt = parseDateString(selOptions[i].value, fmt /*options.formElements[elemID]*/);
                                                        if(dt) {
                                                                yyyymmdd = dt.getFullYear() + "" + pad(dt.getMonth()+1) + "" + pad(dt.getDate());
                                                        
                                                                if(!cursorDate) cursorDate = yyyymmdd;
                                                        
                                                                options.enabledDates[yyyymmdd] = 1;
                                                        
                                                                if(!myMin || Number(yyyymmdd) < myMin) {
                                                                        myMin = yyyymmdd;
                                                                }; 
                                                        
                                                                if(!myMax || Number(yyyymmdd) > myMax) {
                                                                        myMax = yyyymmdd;
                                                                };
                                                                
                                                                break;
                                                        };                                                
                                                };                                        
                                        };  
                        
                                        // Automatically set cursor to first available date (if no bespoke cursorDate was set);                                        
                                        if(!options.cursorDate && cursorDate) options.cursorDate = cursorDate;
                                          
                                } else if(tmpPartsFound[1] && tmpPartsFound[2]) {
                                        var yyyymm;
                                            
                                        for(i = 0; i < selOptions.length; i++) {
                                                for(var f = 0, fmt; fmt = fmtBag[f]; f++) {                                                
                                                        dt = parseDateString(selOptions[i].value, fmt /*options.formElements[elemID]*/);
                                                        if(dt) {
                                                                yyyymm = dt.getFullYear() + "" + pad(dt.getMonth()+1);
                                                        
                                                                if(!myMin || Number(yyyymm) < myMin) {
                                                                        myMin = yyyymm;
                                                                }; 
                                                        
                                                                if(!myMax || Number(yyyymm) > myMax) {
                                                                        myMax = yyyymm;
                                                                };   
                                                                
                                                                break;                                             
                                                        }; 
                                                };                                       
                                        };                                           
                                        
                                        // Round the min & max values to be used as rangeLow & rangeHigh
                                        myMin += "" + "01";
                                        myMax += "" + daysInMonth(+myMax.substr(4,2) - 1, +myMax.substr(0,4));
                                                                                
                                } else if(tmpPartsFound[2]) {
                                        var yyyy;
                                            
                                        for(i = 0; i < selOptions.length; i++) {
                                                for(var f = 0, fmt; fmt = fmtBag[f]; f++) { 
                                                        dt = parseDateString(selOptions[i].value, fmt /*options.formElements[elemID]*/);
                                                        if(dt) {
                                                                yyyy = dt.getFullYear();                                                        
                                                                if(!myMin || Number(yyyy) < myMin) {
                                                                        myMin = yyyy;
                                                                }; 
                                                        
                                                                if(!myMax || Number(yyyy) > myMax) {
                                                                        myMax = yyyy;
                                                                }; 
                                                               
                                                                break;
                                                        };                                               
                                                };                           
                                        };  
                                        
                                        // Round the min & max values to be used as rangeLow & rangeHigh
                                        myMin += "0101";
                                        myMax += "1231";                                                                                                    
                                };
                                
                                if(myMin && (!options.rangeLow  || (+options.rangeLow < +myMin)))  options.rangeLow = myMin;
                                if(myMax && (!options.rangeHigh || (+options.rangeHigh > +myMin))) options.rangeHigh = myMax;                                
                        };
                };
                
                if(!(partsFound[0] && partsFound[1] && partsFound[2])) {
                        return false;
                }; 
                
                var opts = {
                        afterHide:options.afterHide || "",
                        formElements:options.formElements,
                        // Form element id
                        id:options.id,
                        // Format masks 
                        formatMasks:options.formatMasks,
                        // Non popup datepicker required
                        staticPos:!!(options.staticPos),
                        // Position static datepicker or popup datepicker's button
                        positioned:options.positioned && document.getElementById(options.positioned) ? options.positioned : "",
                        // Ranges stipulated in YYYYMMDD format       
                        rangeLow:options.rangeLow && String(options.rangeLow).search(rangeRegExp) != -1 ? options.rangeLow : "",
                        rangeHigh:options.rangeHigh && String(options.rangeHigh).search(rangeRegExp) != -1 ? options.rangeHigh : "",
                        // Status bar format
                        statusFormat:options.statusFormat && String(options.statusFormat).search(validFmtRegExp) != -1 ? options.statusFormat : "",                                                                                 
                        // No drag functionality
                        dragDisabled:nodrag || !!(options.staticPos) ? true : !!(options.dragDisabled),
                        // Bespoke tabindex for this datePicker (or it's activation button)
                        bespokeTabIndex:options.bespokeTabindex && typeof options.bespokeTabindex == 'number' ? parseInt(options.bespokeTabindex, 10) : 0,
                        // Bespoke titles
                        bespokeTitles:options.bespokeTitles || {},
                        // Final opacity 
                        finalOpacity:options.finalOpacity && typeof options.finalOpacity == 'number' && (options.finalOpacity > 20 && options.finalOpacity <= 100) ? parseInt(+options.finalOpacity, 10) : (!!(options.staticPos) ? 100 : finalOpacity),
                        // Do we hide the form elements on datepicker creation
                        hideInput:!!(options.hideInput),
                        // Do we hide the "today" button
                        noToday:!!(options.noTodayButton),
                        // Do we show week numbers
                        showWeeks:!!(options.showWeeks),
                        // Do we fill the entire grid with dates                                                  
                        fillGrid:!!(options.fillGrid),
                        // Do we constrain selection of dates outside the current month
                        constrainSelection:"constrainSelection" in options ? !!(options.constrainSelection) : true,
                        // The date to set the initial cursor to
                        cursorDate:options.cursorDate && String(options.cursorDate).search(rangeRegExp) != -1 ? options.cursorDate : "",                        
                        // Locate label to set the ARIA labelled-by property
                        labelledBy:findLabelForElement(elem),
                        // Have we been passed a describedBy to set the ARIA decribed-by property...
                        describedBy:(options.describedBy && document.getElementById(options.describedBy)) ? options.describedBy : describedBy && document.getElementById(describedBy) ? describedBy : "",
                        // Callback functions
                        callbacks:options.callbackFunctions ? options.callbackFunctions : {},
                        // Days of the week to highlight (normally the weekend)
                        highlightDays:options.highlightDays && options.highlightDays.length && options.highlightDays.length == 7 ? options.highlightDays : [0,0,0,0,0,1,1],
                        // Days of the week to disable
                        disabledDays:options.disabledDays && options.disabledDays.length && options.disabledDays.length == 7 ? options.disabledDays : [0,0,0,0,0,0,0]                                                                   
                };  
                
                if(options.disabledDates) {
                        if(options.enabledDates) delete(options.enabledDates);
                        opts.disabledDates = {};
                        var startD;
                        for(startD in options.disabledDates) {                                
                                if((String(startD).search(wcDateRegExp) != -1 && options.disabledDates[startD] == 1) || (String(startD).search(rangeRegExp) != -1 && String(options.disabledDates[startD]).search(rangeRegExp) != -1)) {
                                        opts.disabledDates[startD] = options.disabledDates[startD];                                           
                                };
                        };
                } else if(options.enabledDates) {                        
                        var startD;
                        opts.enabledDates = {};
                        for(startD in options.enabledDates) {                                
                                if((String(startD).search(wcDateRegExp) != -1 && options.enabledDates[startD] == 1) || (String(startD).search(rangeRegExp) != -1 && String(options.enabledDates[startD]).search(rangeRegExp) != -1)) {
                                        opts.enabledDates[startD] = options.enabledDates[startD];                                                                            
                                };
                        };
                };                
                
                datePickers[options.id] = new datePicker(opts);               
                datePickers[options.id].callback("create", datePickers[options.id].createCbArgObj());                  
        };

        // Used by the button to dictate whether to open or close the datePicker
        var isVisible = function(id) {
                return (!id || !(id in datePickers)) ? false : datePickers[id].visible;
        };  
        
        addEvent(window, 'unload', destroy);
        
        return {
                // General event functions...
                addEvent:               function(obj, type, fn) { return addEvent(obj, type, fn); },
                removeEvent:            function(obj, type, fn) { return removeEvent(obj, type, fn); },
                stopEvent:              function(e) { return stopEvent(e); },
                // Show a single popup datepicker
                show:                   function(inpID) { return showDatePicker(inpID, false); },
                // Hide a popup datepicker
                hide:                   function(inpID) { return hideDatePicker(inpID); },                
                // Create a new datepicker
                createDatePicker:       function(options) { addDatePicker(options); },
                // Destroy a datepicker (remove events and DOM nodes)               
                destroyDatePicker:      function(inpID) { destroySingleDatePicker(inpID); },
                onBlur:      function(inpID, handler) { onPickerBlur(inpID, handler); },
                // Check datePicker form elements exist, if not, destroy the datepicker
                cleanUp:                function() { cleanUp(); },                    
                // Pretty print a date object according to the format passed in               
                printFormattedDate:     function(dt, fmt, useImportedLocale) { return printFormattedDate(dt, fmt, useImportedLocale); },
                // Update the internal date using the form element value
                setDateFromInput:       function(inpID) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].setDateFromInput(); },
                // Set low and high date ranges
                setRangeLow:            function(inpID, yyyymmdd) { if(!inpID || !(inpID in datePickers)) { return false; }; datePickers[inpID].setRangeLow(yyyymmdd); },
                setRangeHigh:           function(inpID, yyyymmdd) { if(!inpID || !(inpID in datePickers)) { return false; }; datePickers[inpID].setRangeHigh(yyyymmdd); },
                // Set bespoke titles for a datepicker instance
                setBespokeTitles:       function(inpID, titles) {if(!inpID || !(inpID in datePickers)) { return false; }; datePickers[inpID].setBespokeTitles(titles); },
                // Add bespoke titles for a datepicker instance
                addBespokeTitles:       function(inpID, titles) {if(!inpID || !(inpID in datePickers)) { return false; }; datePickers[inpID].addBespokeTitles(titles); },                
                // Attempt to parse a valid date from a date string using the passed in format
                parseDateString:        function(str, format) { return parseDateString(str, format); },
                // Change global configuration parameters
                setGlobalVars:          function(json) { affectJSON(json); },
                setSelectedDate:        function(inpID, yyyymmdd) { if(!inpID || !(inpID in datePickers)) { return false; }; datePickers[inpID].setSelectedDate(yyyymmdd); },
                // Is the date valid for selection i.e. not outside ranges etc
                dateValidForSelection:  function(inpID, dt) { if(!inpID || !(inpID in datePickers)) return false; return datePickers[inpID].canDateBeSelected(dt); },
                // Add disabled and enabled dates
                addDisabledDates:       function(inpID, dts) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].addDisabledDates(dts); },
                setDisabledDates:       function(inpID, dts) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].setDisabledDates(dts); },
                addEnabledDates:        function(inpID, dts) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].addEnabledDates(dts); },
                setEnabledDates:        function(inpID, dts) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].setEnabledDates(dts); },
                // Disable and enable the datepicker
                disable:                function(inpID) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].disableDatePicker(); },
                enable:                 function(inpID) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].enableDatePicker(); },
                // Set the cursor date
                setCursorDate:          function(inpID, yyyymmdd) { if(!inpID || !(inpID in datePickers)) return false; datePickers[inpID].setCursorDate(yyyymmdd); },
                // Whats the currently selected date
                getSelectedDate:        function(inpID) { return (!inpID || !(inpID in datePickers)) ? false : datePickers[inpID].returnSelectedDate(); },
                // Attempt to update the language (causes a redraw of all datepickers on the page)
                loadLanguage:           function() { loadLanguage(); }
        }; 
})();/**
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
    intOnly: false,

    editor_initialize: function(param) {
        this.createInput();

        var self = this;

        this.input.onkeypress = function(event) {return self.keyPressed(event || window.event);};

        if (param) {
            this.min = param.min;
            this.max = param.max;
            this.intOnly = param.intOnly;
        }
    },

    isValid: function(value) {
        var n = Number(value);
        var invalid = isNaN(n) || (this.min && n < this.min) || (this.max && n > this.max);
        return !invalid;
    },

    keyPressed: function(event) {
        var v = this.input.getValue();
        if (event.charCode === 0) return true;
        var code = event.charCode == undefined ? event.keyCode : event.charCode;

        if (code == 45)  // minus
            return v.indexOf("-") < 0;
        if (code == 46)  // point
            return !this.intOnly && v.indexOf(".") < 0;

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
        var checked = false;
        if (value != null) {
            var trueValues = ["true", "on", "yes", "t", "y"];
            for (var i = 0; i < trueValues.length; ++i) {
                if (trueValues[i] == value.toLowerCase()) {
                    checked = true;
                    break;
                }
            }
        }
        this.input.checked = checked;
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
            finalOpacity: 100
        };
        datePickerOpts.formElements[inputId] = "m-sl-d-sl-Y";

        var datePickerGlobalOpts = {
            noDrag: true
        };

        datePickerController.setGlobalVars(datePickerGlobalOpts);

        datePickerController.createDatePicker(datePickerOpts);

        if (this.focussed) {
            this.focus();
        }
    },

    focus: function() {
        datePickerController.show(this.getId());
    },

    bind: function($super, event, handler) {
        if (event == "blur") {
            datePickerController.onBlur(this.getId(), handler);
        } else {
            $super(event, handler);
        }
    },

    unbind: function($super, event, handler) {
        if (!event) {
            datePickerController.onBlur(this.getId(), "");
            $super(event, handler);
        } else if (event == "blur") {
            datePickerController.onBlur(this.getId(), "");
        } else {
            $super(event, handler);
        }
    },

    destroy: function($super) {
        datePickerController.destroyDatePicker(this.getId());
        $super();
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
/**
 * Array editor.
 * 
 * @requires Prototype v1.6.1+ library
 *
 * @author Andrei Astrouski
 */
var ArrayEditor = Class.create(BaseTextEditor, {

    separator: null,
    entryEditor: null,
    intOnly: false,

    editor_initialize: function(param) {
        this.createInput();

        var self = this;
        this.input.onkeypress = function(event) {return self.keyPressed(event || window.event);};

        if (param) {
            this.separator = param.separator;
            this.entryEditor = param.entryEditor;
            this.intOnly = param.intOnly;
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
    },

    keyPressed: function(event) {
        if (event.charCode === 0) return true;

        var code = event.charCode == undefined ? event.keyCode : event.charCode;
        if (code == 46) {
            // point
            return !this.intOnly || this.separator === '.';
        }

        return /^[,0-9]$/.test(String.fromCharCode(code));
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
                    var matchInt = v.match(/^-?[,0-9]+[KMB]?$/);
                    if (!matchInt) {
                        valid = false;
                    }
                } else {
                    var matchDouble = v.match(/^-?[,0-9]+(\.?[0-9]+)?[KMB]?$/);
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
        return /^[.,0-9KMB]$/.test(String.fromCharCode(code));
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
