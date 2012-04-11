
function openMenu(menuId, td, event) {
    if (AjaxHelper.isRightClick(event)) {
        td.oncontextmenu = function() { return false; };
        PopupMenu.sheduleShowMenu(menuId, event, 150);
    }
}

function closeMenu(td) {
    PopupMenu.cancelShowMenu();
}

function triggerEdit(editorCompId, url, cellToEdit) {
    var cellElement = cellToEdit;
    if (!cellElement) {
        cellElement = $(PopupMenu.lastTarget);
    }
    var cellUri = encodeURI(cellElement.firstChild.value);
    var cell = cellUri.toQueryParams().cell;

    var editor = $(editorCompId);
    var editorId = editor.id.replace('te_comp','');

    new Ajax.Request(url, {
        method: "get",
        encoding: "utf-8",
        contentType: "text/javascript",
        parameters: {
            cell: cell,
            editorId: editorId
        },
        onSuccess: function(data) {
            editor.innerHTML = data.responseText.stripScripts();
            new ScriptLoader().evalScripts(data.responseText);
        },
        onFailure: AjaxHelper.handleError
    });
}
