verticalRenderer = {
    htmlTable : null,

    initDomTable : function(domTable) {
        this.htmlTable = domTable;
        this.setDefaultStyle(this.htmlTable);
    },

    setDefaultStyle : function(obj) {
        obj.style.borderWidth = "1px 1px 1px 1px";
        obj.style.borderColor = "#bbbbdd #bbbbdd #bbbbdd #bbbbdd;";
        obj.style.borderStyle = "solid solid solid solid";
        obj.style.textAlign = "center";
    },

    setHeaderStyle : function(obj) {
        this.setDefaultStyle(obj);
        obj.style.backgroundColor="black";
        obj.style.color = "white"; 
    },

    setTitleStyle : function(obj) {
        this.setDefaultStyle(obj);
        obj.style.backgroundColor="#CCFFFF";
    },

    setDataStyle : function(obj) {
        this.setDefaultStyle(obj);
        obj.style.backgroundColor="#FFFF99";
    },

    setPropValueStyle : function(obj) {
        this.setDefaultStyle(obj);
        obj.style.textAlign="left";
    },

    setReturnStyle : function(obj) {
        this.setDefaultStyle(obj);
        obj.style.backgroundColor="#FFCC99";
    },

    createHeaderRow : function(table) {
        var row = this.htmlTable.insertRow(0);
        var cell = row.insertCell(0);
        //this.setDefaultStyle(cell);
        this.setHeaderStyle(cell);
        cell.colSpan = table.header.inParam.length + 1;

        cell.innerHTML = table.headerRow();
    },

    createRow : function(dataRow, isTitle) {
        var row = this.htmlTable.insertRow(-1);

        for(var i = 0; i < dataRow.length; i++) {
            var cell = row.insertCell(-1);
            cell.data = dataRow[i];
            
            if(isTitle) {
                this.setTitleStyle(cell);
            } else {
                this.setDataStyle(cell);
            }

            cell.setAttribute('oncontextmenu','contentMenuAction(this, event, '+isTitle+')');
            cell.innerHTML = this.getCellHtml(dataRow[i].getValue(), "VALUE", cell);
        }
    },

    getCellHtml : function(value, type, cell) {
        if(type == "DATA_TYPE") {
            return value;
        } else if(type == "PROPERTY_TYPE") {
            return "<span id=\"t"+cell.parentNode.rowIndex+"\" onclick=\"tableModel.toEditPropTypeMode(this)\">"+value+"</span><span style=\"display : none\"></span>";
        } else if(type == "PROPERTY_VALUE") {
            cell.setAttribute('onclick','tableModel.toEditPropsMode(this)');
            return "<div style=\"display: inline\">"+value+"</div>";
        } else if(type == "VALUE"){
            cell.setAttribute('onclick','tableModel.toEditorMode(this)');
            return "<div>"+cell.data.value+"</div>";
        } else {
            return value;
        }
    },

    selectValue : function(editElem) {
        var span = editElem.parentNode;
        var element = span.previousSibling;
        span.style.display = "none";
        element.style.display = "";
        
        if(editElem.value == "") {
            element.innerHTML = "undefined";
        } else {
            element.innerHTML = editElem.value;
        }
    },

    createEmptyCol : function(id, dataRows) {
        var rows = this.htmlTable.rows;
        var headerRow = rows[0];
        var headerCell = headerRow.cells[0];

        headerCell.colSpan = headerCell.colSpan + 1;

        for(var i = tableModel.startDataTableRowIndex(); i < rows.length; i++) {
            var row = rows[i];
            var dataRow = dataRows[i - tableModel.startDataTableRowIndex()];
            var cell = row.insertCell(id);
            cell.data = dataRow[id];
 
            if(i == tableModel.startDataTableRowIndex()) {
                this.setTitleStyle(cell);
            } else {
                this.setDataStyle(cell);
            }

            cell.setAttribute('oncontextmenu','contentMenuAction(this, event,'+(i == tableModel.startDataTableRowIndex())+')');
            cell.innerHTML = this.getCellHtml(dataRow[id].getValue(),"VALUE",cell);
        }

        this.refreshTableHeader();
    },

    refreshTableHeader : function() {
        var headerCell = this.htmlTable.rows[0].cells[0];
        var newHeaderSize = tableModel.headerSpanCell();

        if(tableModel.startDataTableRowIndex() > 0 && newHeaderSize < 3 ) {
            headerCell.colSpan = 3;
        } else {
            headerCell.colSpan = newHeaderSize;
        }

        headerCell.innerHTML = tableModel.headerRow();
    },

    deleteRow : function(rowId) {
        this.htmlTable.deleteRow(rowId);
    },

    deleteCondition : function(index) {
        for(var i = tableModel.startDataTableRowIndex(); i < domTable.rows.length; i++) {
            this.htmlTable.rows[i].deleteCell(index);
        }

        this.refreshTableHeader();
    },

    deletePropsRow : function(properties) {
        for(var i = 0;  i < properties.length; i++) {
            this.htmlTable.deleteRow(1);
        }
        
        this.refreshTableHeader();
    },

    refreshPropertyRegion : function(properties) {
        for(var i = 0;  i < properties.length; i++) {
            row = this.htmlTable.insertRow(i+1);

            if(i == 0) {
                titleCell = row.insertCell(-1);
                titleCell.rowSpan = properties.length;
                this.setDefaultStyle(titleCell);
                titleCell.innerHTML = "Properties";
            }

            typeCell = row.insertCell(-1);
            this.setDefaultStyle(typeCell);
            typeCell.innerHTML = this.getCellHtml(properties[i].type, "PROPERTY_TYPE", typeCell);
            typeCell.setAttribute('oncontextmenu','propsContentMenuAction(this, event)');

            valueCell = row.insertCell(-1);
            this.setPropValueStyle(valueCell);
            valueCell.innerHTML = this.getCellHtml(properties[i].value, "PROPERTY_VALUE", valueCell);
            valueCell.setAttribute('oncontextmenu','propsContentMenuAction(this, event)');
            valueCell.props = properties[i];
        }

        this.refreshTableHeader();
    },

    setDataTypeTo : function(elemId, value) {
        document.getElementById(elemId).innerHTML = value;
    },

    refreshTable : function(tableModel) {
        this.refreshTableHeader();
        this.refreshPropertyRegion(tableModel.properties);
        //create title
        this.htmlTable.deleteRow(tableModel.startDataTableRowIndex());
        this.createRow(tableModel.dataRows[0] , true);

        //create data rows
        for (var i = 1; i < tableModel.dataRows.length; i++) {
            this.createRow(tableModel.dataRows[i] , false);
        }
    },

    setErrorMessage : function(error, element) {
        element.html("");

        if(error.length > 0) {
            table = document.createElement('table');

            for(var i = 0;  i < error.length; i++) {
                tr = document.createElement('tr');
                td = document.createElement('td');

                d = document.createElement('div');
                $j(d).addClass('error_box')
                    .html(error[i])
                    .appendTo(td);

                $j(td).appendTo(tr);
                $j(tr).appendTo(table);
            }

            $j(table).appendTo(element);
        }
    },
}