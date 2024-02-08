verticalRenderer = {
    htmlTable : null,

    initDomTable : function(domTable) {
        this.htmlTable = domTable;
        this.setDefaultStyle(this.htmlTable);
    },

    /*  
    Take away these style methods to a new class which should be inherited by new style-scheme classes. Delegate methods from that new class to this class
    */
    //NOT use color #FFFFFF and #00000 you will have problem wich colors
    setDefaultStyle : function(obj) {
        obj.style.textAlign = "center";
        obj.style.fontSize = "12px";
        obj.style.fontFamily = "Franklin Gothik Book";
        obj.style.minWidth = "50px";
        obj.style.height = "21px";
        obj.style.backgroundColor="#FFFFFE";
    },

    setHeaderStyle : function(obj) {
        this.setDefaultStyle(obj);

        obj.style.color = "#000001";
        //obj.style.backgroundColor="#FFFFFF";
        obj.style.borderTop = "1px solid #000001";
        obj.style.borderBottom = "1px solid #000001";

        tableModel.header.style.push(obj.style);
    },

    setTitleStyle : function(obj) {
        this.setDefaultStyle(obj);

        obj.style.backgroundColor= "#A6A6A6";
        obj.style.borderBottom = "1px solid #000001";
        obj.style.fontWeight = 'bold';

        obj.data.style = obj.style;
    },

    setDataStyle : function(obj) {
        this.setDefaultStyle(obj);

        //obj.style.backgroundColor="#FFFFFF";
        obj.style.borderWidth = "1px 1px 1px 1px";
        obj.style.borderColor = "#DDDDDD #DDDDDD #DDDDDD #DDDDDD";
        obj.style.borderStyle = "solid solid solid solid";

        obj.data.style = obj.style;
    },

    setPropStyle : function(obj) {
        this.setDefaultStyle(obj);

        obj.style.textAlign="left";
        obj.style.color = "#808080";
        //obj.style.backgroundColor="#FFFFFF";
    },

    setPropValueStyle : function(obj) {
        this.setDefaultStyle(obj);

        obj.style.textAlign="left";
        obj.style.color = "#808080";
        //obj.style.backgroundColor="#FFFFFF";

        obj.data.style = obj.style;
    },

    setReturnTitleStyle : function(obj) {
        this.setDefaultStyle(obj);

        obj.style.backgroundColor= "rgb(250, 210, 70)";//"#F0DB5E";
        obj.style.borderBottom = "3px solid #FFC91D";
        obj.style.fontWeight = 'bold';

        obj.data.style = obj.style;
    },

    setReturnDataStyle : function(obj) {
        this.setDefaultStyle(obj);

        obj.style.backgroundColor="#D9D9D9";
        obj.style.borderWidth = "1px 1px 1px 1px";
        obj.style.borderColor = "#DDDDDD #DDDDDD #DDDDDD #DDDDDD";
        obj.style.borderStyle = "solid solid solid solid";

        obj.data.style = obj.style;
    },

    setLastCellBorderStyle : function(obj, isReturnCell) {
        obj.style.borderTop = "1px solid #DDDDDD";

        if(!isReturnCell) {
            obj.style.borderBottom = "1px solid #000001";
        } else {
            obj.style.borderBottom = "3px solid #FFC91D";
        }

        obj.data.style = obj.style;
    },

    createHeaderRow : function(table) {
        var row = this.htmlTable.insertRow(0);
        var cell = row.insertCell(0);
        cell.colSpan = table.header.inParam.length + 1;
        cell.innerHTML = table.headerRow();

        this.setHeaderStyle(cell);
    },

    createRow : function(dataRow, isTitle, isLast) {
        var row = this.htmlTable.insertRow(-1);

        for (var i = 0; i < dataRow.length; i++) {
            var cell = row.insertCell(-1);
            cell.data = dataRow[i];

            if (isTitle) {
                if(cell.data.isReturn) {
                    this.setReturnTitleStyle(cell);
                } else {
                    this.setTitleStyle(cell);
                }
            } else {
                if(cell.data.isReturn) {
                    this.setReturnDataStyle(cell);
                } else {
                    this.setDataStyle(cell);
                }
            }

            if (isLast) {
                //set button border for last cell;
                if(cell.data.isReturn) {
                    this.setLastCellBorderStyle(cell, true);
                } else {
                    this.setLastCellBorderStyle(cell, false);
                }
            }

            if (this.htmlTable.rows.length - 2 > tableModel.startDataTableRowIndex()) {
                //if needed delete last row style from prev cell
                this.deleteCellStyleFromPrevRow();
            }

            cell.setAttribute('oncontextmenu','contentMenuAction(this, event, '+isTitle+')');
            cell.innerHTML = this.getCellHtml(dataRow[i].getValue(), "VALUE", cell);
        }
    },

    deleteCellStyleFromPrevRow : function() {
        var beforeLastRow = this.htmlTable.rows[this.htmlTable.rows.length - 2];

        for (var cellNum = 0; cellNum < beforeLastRow.cells.length; cellNum++) {
            if(cellNum < tableModel.header.inParam.length) {
                this.setDataStyle(beforeLastRow.cells[cellNum]);
            } else {
                this.setReturnDataStyle(beforeLastRow.cells[cellNum]);
            }

        }
    },

    refreshLastRowStyle : function() {
        if (this.htmlTable.rows.length > tableModel.startDataTableRowIndex()) {
            var lastRow = this.htmlTable.rows[this.htmlTable.rows.length - 1];

            for (var cellNum = 0; cellNum < lastRow.cells.length; cellNum++) {
               if (tableModel.header.inParam.length == cellNum) {
                   this.setLastCellBorderStyle(lastRow.cells[cellNum], true);
               } else {
                   this.setLastCellBorderStyle(lastRow.cells[cellNum], false);
               }
            }
        }
    },

    getCellHtml : function(value, type, cell) {
        if (type == "DATA_TYPE") {
            return value;
        } else if(type == "PROPERTY_TYPE") {
            return "<span style=\"display : none\"></span><span id=\"t"+cell.parentNode.rowIndex+"\" onclick=\"tableModel.toEditPropTypeMode(this)\">"+value+"</span>";
        } else if(type == "PROPERTY_VALUE") {
            cell.setAttribute('onclick','tableModel.toEditPropsMode(this)');
            return value;
        } else if(type == "VALUE"){
            cell.setAttribute('onclick','tableModel.toEditDataMode(this, event)');
            return cell.data.value;
        } else {
            return value;
        }
    },

    selectValue : function(editElem) {
        var span = editElem.parentNode;
        var element = span.nextSibling;
        span.style.display = "none";
        element.style.display = "";

        if(editElem.value == "") {
            element.innerHTML = "undefined"/*editElem.value*/;
        } else {
            element.innerHTML = editElem.value;
        }
    },

    createEmptyCol : function(id, dataRows) {
        var rows = this.htmlTable.rows;
        var headerRow = rows[0];
        var headerCell = headerRow.cells[0];

        headerCell.colSpan = headerCell.colSpan + 1;

        var cell = null;
        if (tableModel.headerSpanCell() > 3) {
            for(var i = 1; i < tableModel.startDataTableRowIndex(); i++) {
                var row = rows[i];
                cell = row.insertCell(-1);
                this.setPropStyle(cell);
            }
        }

        for (var i = tableModel.startDataTableRowIndex(); i < rows.length; i++) {
            var row = rows[i];
            var dataRow = dataRows[i - tableModel.startDataTableRowIndex()];
            cell = row.insertCell(id);
            cell.data = dataRow[id];
 
            if(i == tableModel.startDataTableRowIndex()) {
                this.setTitleStyle(cell);
            } else {
                this.setDataStyle(cell);
            }

            cell.setAttribute('oncontextmenu','contentMenuAction(this, event,'+(i == tableModel.startDataTableRowIndex())+')');
            cell.innerHTML = this.getCellHtml(dataRow[id].getValue(),"VALUE",cell);
        }

        //set button border for last cell;
        this.setLastCellBorderStyle(cell);
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

        this.refreshLastRowStyle();
    },

    deleteCondition : function(index) {
        if (tableModel.headerSpanCell() > 2) {
            for(var i = 1; i < tableModel.startDataTableRowIndex(); i++) {
                var row = this.htmlTable.rows[i];
                row.deleteCell(row.cells.length-1);
            }
        }

        for (var i = tableModel.startDataTableRowIndex(); i < domTable.rows.length; i++) {
            this.htmlTable.rows[i].deleteCell(index);
        }

        this.refreshTableHeader();
    },

    deletePropsRow : function(properties) {
        for (var i = 0;  i < properties.length; i++) {
            this.htmlTable.deleteRow(1);
        }

        this.refreshTableHeader();
    },

    refreshPropertyRegion : function(properties) {
        for (var i = 0;  i < properties.length; i++) {
            row = this.htmlTable.insertRow(i+1);

            if (i == 0) {
                titleCell = row.insertCell(-1);
                titleCell.rowSpan = properties.length;
                this.setPropStyle(titleCell);
                titleCell.innerHTML = "Properties";
            }

            typeCell = row.insertCell(-1);
            this.setPropStyle(typeCell);
            typeCell.innerHTML = this.getCellHtml(properties[i].type, "PROPERTY_TYPE", typeCell);
            typeCell.setAttribute('oncontextmenu','propsContentMenuAction(this, event)');

            valueCell = row.insertCell(-1);
            valueCell.innerHTML = this.getCellHtml(properties[i].value, "PROPERTY_VALUE", valueCell);
            valueCell.setAttribute('oncontextmenu','propsContentMenuAction(this, event)');
            valueCell.data = properties[i];

            this.setPropValueStyle(valueCell);

            for (var propColumn = 3; propColumn < tableModel.headerSpanCell(); propColumn++) {
                valueCell = row.insertCell(-1);
                valueCell.innerHTML = "";

                this.setPropStyle(valueCell);
            }
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
        this.createRow(tableModel.dataRows[0] , true, false);

        //create data rows
        for (var i = 1; i < tableModel.dataRows.length; i++) {
            if ((i + 1) < tableModel.dataRows.length) {
                this.createRow(tableModel.dataRows[i] , false, false);
            } else {
                //last row
                this.createRow(tableModel.dataRows[i] , false, true);
            }
        }
    },

    setErrorMessage : function(error, element) {
        element.html("");

        if (error.length > 0) {
            table = document.createElement('table');

            for (var i = 0;  i < error.length; i++) {
                tr = document.createElement('tr');
                td = document.createElement('td');

                d = document.createElement('div');
                $j(d).addClass('problem-error')
                    .html(error[i])
                    .appendTo(td);

                $j(td).appendTo(tr);
                $j(tr).appendTo(table);
            }

            $j(table).appendTo(element);
        }
    },

    setConditionTitle : function(newTitle, conditionId) {
        var titleRowId = tableModel.startDataTableRowIndex();
        var title = this.htmlTable.rows[titleRowId].cells[conditionId].innerHTML;

        if (title == "") {
            this.htmlTable.rows[titleRowId].cells[conditionId].innerHTML = newTitle;
            this.htmlTable.rows[titleRowId].cells[conditionId].data.value = newTitle;
        }           
    }
};