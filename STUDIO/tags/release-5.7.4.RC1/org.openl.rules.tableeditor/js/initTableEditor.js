//TODO Refactor - Move to TableEditor.js

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

function initTableEditor(editorId, url, cellToEdit, actions) {
    var tableEditor = new TableEditor(editorId, url, cellToEdit, actions);
    initToolbar(editorId);

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

    tableEditor.loadData();

    return tableEditor;
}

function initToolbar(editorId) {
    $$("." + itemClass).each(function(item) {
        item.onmouseover = function() {
            this.addClassName(overClass);
        };
        item.onmouseout = function() {
            this.removeClassName(overClass);
        };
    });
}

function processItem(item, enable) {
    if (enable) {
        enableToolbarItem(item);
    } else {
        disableToolbarItem(item);
    }
}

function getItemId(editorId, itemId) {
    if (editorId && itemId) {
        return editorId + itemId;
    }
}

function enableToolbarItem(img) {
    if (isToolbarItemEnabled(img = $(img))) return;
    img.removeClassName(disabledClass);

    if (img._mouseover) img.onmouseover = img._mouseover;
    if (img._mouseout) img.onmouseout = img._mouseout;
    if (img._onclick) img.onclick = img._onclick;
}

function disableToolbarItem(img) {
    if (!isToolbarItemEnabled(img = $(img))) return;
    img.addClassName(disabledClass);

    img._mouseover = img.onmouseover;
    img._mouseout = img.onmouseout;
    img._onclick = img.onclick;
    img.onmouseover = img.onmouseout = img.onclick = Prototype.emptyFunction;
}

function isToolbarItemEnabled(img) {
    return !img.hasClassName(disabledClass);
}