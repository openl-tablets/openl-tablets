
function openMenu(menuId, td, event) {
    PopupMenu.sheduleShowMenu(menuId, event, 400);
}

function closeMenu(td) {
    PopupMenu.cancelShowMenu();
}

Ajax.Responders.register({
    onFailure: function() {
        alert("global");
    }
});

function triggerEdit(editorId, url) {
    var cellUri = $(PopupMenu.lastTarget).down("input").value;
    var editor = $(editorId);
    new Ajax.Request(url, {
        method: "get",
        encoding: "utf-8",
        contentType: "text/javascript",
        parameters: {
            cell: cellUri.toQueryParams().cell,
            editorId: editor.id.replace('te_comp','')
        },
        onSuccess: function(data) {
            editor.innerHTML = data.responseText.stripScripts();
            new ScriptLoader().evalScripts(data.responseText);
        },
        onFailure: AjaxHelper.handleError
    });
}

function triggerEditXls(url) {
    var cellUri = $(PopupMenu.lastTarget).down("input").value;
    new Ajax.Request(url, {
        method: "get",
        parameters: {
            cellUri: cellUri
        },
        onFailure: AjaxHelper.handleError
    });
}