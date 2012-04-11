
function openMenu(menuId, td, event) {
    if (event.button == 2) { // mouse right click
        td.oncontextmenu = function() { return false; };
        PopupMenu.sheduleShowMenu(menuId, event, 150);
    }
}

function closeMenu(td) {
    PopupMenu.cancelShowMenu();
}

function triggerEdit(editorId, url, cellToEdit) {
    var cell = cellToEdit;
    if (!cell) {
        cell = $(PopupMenu.lastTarget);
    }
    var editor = $(editorId);
    new Ajax.Request(url, {
        method: "get",
        encoding: "utf-8",
        contentType: "text/javascript",
        parameters: {
            cell: cell.firstChild.value.toQueryParams().cell,
            editorId: editor.id.replace('te_comp','')
        },
        onSuccess: function(data) {
            editor.innerHTML = data.responseText.stripScripts();
            new ScriptLoader().evalScripts(data.responseText);
        },
        onFailure: AjaxHelper.handleError
    });
}
