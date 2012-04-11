var tableEditor;

function initTableEditor(id, url, cellToEdit) {
    tableEditor = new TableEditor(id, url, cellToEdit);

    tableEditor.undoStateUpdated = function (hasItems) {
        ["save_all", "undo"].each(hasItems ? setEnabled : setDisabled);
        if (hasItems) {
            window.onbeforeunload = function () {
                return "Your changes have not been saved.";
            };
        } else {
            window.onbeforeunload = function () {
            };
        }
    };

    tableEditor.redoStateUpdated = function (hasItems) {
        (hasItems ? setEnabled : setDisabled)("redo");
    };

    tableEditor.isSelectedUpdated = function (selected) {
        align_items.each(selected ? setEnabled : setDisabled);
        addremove_items.each(selected ? setEnabled : setDisabled);
    };

    tableEditor.loadData();
}