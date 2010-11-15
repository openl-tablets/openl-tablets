var save_item = "_save_all";
var undo_item = "_undo";
var redo_item = "_redo";
var indent_items = ["_decrease_indent", "_increase_indent"];
var align_items = ["_align_left", "_align_center", "_align_right"];
var addremove_items = ["_insert_row_before", "_remove_row", "_insert_column_before", "_remove_column"];
var color_items = ["_fill_color"];
var other_items = ["_help"];

function initTableEditor(editorId, url, cellToEdit, actions) {
    var tableEditor = new TableEditor(editorId, url, cellToEdit, actions);
    var iconManager = initIconManager(editorId);

    tableEditor.undoStateUpdated = function(hasItems) {
        [save_item, undo_item].each(function(item) {
            enableItem(iconManager, getItemId(editorId, item), hasItems);
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
        enableItem(iconManager, getItemId(editorId, redo_item), hasItems);
    };

    tableEditor.isSelectedUpdated = function(selected) {
        [indent_items, align_items, color_items, addremove_items].flatten().each(function(item) {
            enableItem(iconManager, getItemId(editorId, item), selected);
        });
    };

    tableEditor.loadData();

    return tableEditor;
}

function initIconManager(editorId) {
    var im = new IconManager("item_enabled", "item_over", "item_disabled");

    [save_item, undo_item, redo_item, indent_items, align_items, color_items,
        addremove_items, other_items].flatten().each(
            function(item) {
        im.init(getItemId(editorId, item));
    });
    [other_items].flatten().each(function(item) {
        im.enable(getItemId(editorId, item));
    });
    
    return im;
}

function enableItem(iconManager, item, enable) {
    if (enable) {
        iconManager.enable(item);
    } else {
        iconManager.disable(item);
    }
}

function getItemId(editorId, itemId) {
    if (editorId && itemId) {
        return editorId + itemId;
    }
}