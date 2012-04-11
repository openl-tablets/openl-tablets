
function cellMouseOver(td, event) {
    PopupMenu.sheduleShowMenu("contextMenu", event, 700);
}

function cellMouseOut(td) {
    PopupMenu.cancelShowMenu();
}

function triggerEdit(url) {
    var cellUri = $(PopupMenu.lastTarget).down("input").value;
    new Ajax.Request(url, {
        method: "get",
        encoding: "utf-8",
        contentType: "text/html",
        parameters: {
            cell: cellUri.toQueryParams().cell
        },
        onSuccess: function(data) {
            $('te_').innerHTML = data.responseText.stripScripts();
            new ScriptLoader().evalScripts(data.responseText);
        }
    });
}

function triggerEditXls(url) {
    var cellUri = $(PopupMenu.lastTarget).down("input").value;
    new Ajax.Request(url, {
        method: "get",
        parameters: {
            cellUri: cellUri
        }
    });
}
