function contentMenuAction(cell, event, isTitle) {
    //FIXME move out tableModel using
    var dataRowsStartIndex = tableModel.startDataTableRowIndex();

    if(!dataRowsStartIndex)
        dataRowsStartIndex = 1;

    var div = document.getElementById("srtPopupDiv");
    div.innerHTML = "";

    var addRowLink = document.createElement('a');
    addRowLink.setAttribute('href','javascript:void(0)');
    addRowLink.setAttribute('onclick','tableModel.createEmptyRow();');
    addRowLink.innerHTML = ("Add Rule");
    div.appendChild(addRowLink);

    div.appendChild(document.createElement('br'));

    var addColLink = document.createElement('a');
    addColLink.setAttribute('href','javascript:void(0)');
    addColLink.setAttribute('onclick','tableModel.createEmptyCol('+cell.cellIndex+');');
    addColLink.innerHTML = ("Insert Condition Before");
    div.appendChild(addColLink);

    if(cell.cellIndex != (domTable.rows[dataRowsStartIndex].cells.length -1)) {
        div.appendChild(document.createElement('br'));

        var cellIndex = cell.cellIndex + 1;

        var addColLink = document.createElement('a');
        addColLink.setAttribute('href','javascript:void(0)');
        addColLink.setAttribute('onclick','tableModel.createEmptyCol('+cellIndex+');');
        addColLink.innerHTML = ("Insert Condition After");
        div.appendChild(addColLink);
    }

    if(cell.cellIndex != (domTable.rows[dataRowsStartIndex].cells.length -1)) {
        div.appendChild(document.createElement('br'));

        var addColLink = document.createElement('a');
        addColLink.setAttribute('href','javascript:void(0)');
        addColLink.setAttribute('onclick','tableModel.deleteCondition('+cell.cellIndex+');');
        addColLink.innerHTML = ("Delete Condition");
        div.appendChild(addColLink);
    }

    if(!isTitle) {
        div.appendChild(document.createElement('br'));

        var addColLink = document.createElement('a');
        addColLink.setAttribute('href','javascript:void(0)');
        addColLink.setAttribute('onclick','tableModel.deleteRow('+cell.parentNode.rowIndex+');');
        addColLink.innerHTML = ("Delete Row");
        div.appendChild(addColLink);
    }

    if(isTitle && isPropertyCanBeAdded()) {
        div.appendChild(document.createElement('br'));

        var addColLink = document.createElement('a');
        addColLink.setAttribute('href','javascript:void(0)');
        addColLink.setAttribute('onclick','tableModel.addNewProps();');
        addColLink.innerHTML = ("Add Property");
        div.appendChild(addColLink);
    }

    event.preventDefault();
    PopupMenu.sheduleShowMenu('srtPopupDiv', event, 0);
    return false;
}

function propsContentMenuAction(cell, event) {
    var div = document.getElementById("srtPopupDiv");
    div.innerHTML = "";

    var addRowLink = document.createElement('a');
    addRowLink.setAttribute('href','javascript:void(0)');
    addRowLink.setAttribute('onclick','tableModel.deleteProps('+cell.parentNode.rowIndex+');');
    addRowLink.innerHTML = ("Delete Property");
    div.appendChild(addRowLink);

    if(isPropertyCanBeAdded()) {
        div.appendChild(document.createElement('br'));

        var addColLink = document.createElement('a');
        addColLink.setAttribute('href','javascript:void(0)');
        addColLink.setAttribute('onclick','tableModel.addNewProps();');
        addColLink.innerHTML = ("Add Property");
        div.appendChild(addColLink);
    }

    event.preventDefault();
    PopupMenu.sheduleShowMenu('srtPopupDiv', event, 0);
    return false;
}

function arrayContexMenu(event, id, iterable) {
    var div = document.getElementById("srtPopupDiv");
    div.innerHTML = "";

    var addRowLink = document.createElement('a');
    addRowLink.setAttribute('href','javascript:void(0)');
    addRowLink.setAttribute('onclick','tableModel.setIterable('+id+','+!iterable+');');

    if(iterable) {
        addRowLink.innerHTML = ("Set As Single");
    } else {
        addRowLink.innerHTML = ("Set As Array");
    }
    div.appendChild(addRowLink);

    event.preventDefault();
    PopupMenu.sheduleShowMenu('srtPopupDiv', event, 0);
    return false;
}

//FIXME move out tableModel using
function isPropertyCanBeAdded() {
    return ($j("#propsDataType").find('option').size() - 1) > tableModel.properties.length;
}