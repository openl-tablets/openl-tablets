
function cellMouseOver(td, event) {
    PopupMenu.sheduleShowMenu("contextMenu", event, 700);
}

function cellMouseOut(td) {
    PopupMenu.cancelShowMenu();
}

function triggerEdit(f) {
    if (!f) f = document.forms[0];
    f.mode.value = "edit";
    var cellUri = $(PopupMenu.lastTarget).down("input").value;
    f.cell.value = cellUri.toQueryParams().cell;
    f.submit();
}

function triggerEditXls(f) {
    if (!f) f = document.forms[0];
    f.mode.value = "editExcel";
    f.cellUri.value = $(PopupMenu.lastTarget).down("input").value;
    f.submit();
}
