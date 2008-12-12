
function cellMouseOver(td, event) {
    PopupMenu.sheduleShowMenu("contextMenu", event, 700);
}

function cellMouseOut(td) {
    PopupMenu.cancelShowMenu();
}

function triggerEdit(f) {
    f.mode.value = "edit";
    var cellUri = $(PopupMenu.lastTarget).down("input").value;
    f.cell.value = cellUri.toQueryParams().cell;
    f.submit();
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
