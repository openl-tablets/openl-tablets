var tableModel = {
    properties : [],
    dataRows : [],
    renderer : null,
    
    header : {
        name : null,
        returnType : null,
        inParam : []
    },

    headerRow : function() {
        var params = "";

        for(var i = 0; i < this.header.inParam.length; i++) {
            if(i > 0) {
                params += ", ";
            }
            params += "<span id=\"param"+i+"\" oncontextmenu=\"arrayContexMenu(event, "+i+","+this.header.inParam[i].iterable+")\" onclick=\"selectDataTypeAction(this,event,"+i+")\">"
            + this.header.inParam[i].type + ((this.header.inParam[i].iterable == true)? "[]" : "") 
            + "</span> <span onclick='tableModel.toEditMode(this)'>" 
            + this.header.inParam[i].name +"</span>"
            +"<span style=\"display : none\"><input type=\"text\" class=\"editTableInParam\" value=\""+this.header.inParam[i].name+"\" onchange=\"tableModel.setInParamValue(this,"+i+")\"/></span>";
        }

        return  "SimpleRules <span id=\"returnSRT\" oncontextmenu=\"arrayContexMenu(event, -1,"+this.header.returnType.iterable+")\" onclick=\"selectDataTypeAction(this,event, -1)\">"+
        this.header.returnType.type + ((this.header.returnType.iterable == true)? "[]" : "") +"</span> <span onclick='tableModel.toEditMode(this)'>" + this.header.name
        + "</span><span style=\"display : none\"><input type=\"text\" class=\"editTableInParam\" value=\""+this.header.name+"\" onchange=\"tableModel.setInParamValue(this,-1)\"/></span>"
        + "(" + params + ")";
    },
    
    setReturnParam : function(returnParam) {
        this.header.returnType = returnParam;
    },

    initTable : function(renderer) {
        this.renderer = renderer;

        renderer.createHeaderRow(this);
        this.setTitleRow();
    },

    setTitleRow : function() {
        var row = [];

        for(i = 0;  i < this.header.inParam.length; i++) {
            row.push(new Cell(this.header.inParam[i].name, "STRING",false));
        }

        row.push(new Cell("RETURN", "STRING",false));

        this.dataRows.push(row);
        this.renderer.createRow(row, true);
    },

    headerSpanCell : function () {
        if(this.header.inParam) {
            return this.header.inParam.length + 1;
        } else {
            return 1;
        }
    },

    addNewProps : function() {
        this.renderer.deletePropsRow(this.properties);
        this.properties.push(new Property());
        this.renderer.refreshPropertyRegion(this.properties);
        
        
    },

    deleteProps : function(rowId) {
        this.renderer.deletePropsRow(this.properties);

        this.properties.splice(rowId - 1,1);

        this.renderer.refreshPropertyRegion(this.properties);
    },

    createEmptyRow : function() {
        var row = [];

        for(i = 0;  i < this.header.inParam.length; i++) {
            row.push(new Cell("empty", this.header.inParam[i].valuesType, this.header.inParam[i].iterable));
        }

        row.push(new Cell("empty",this.header.returnType.valuesType, this.header.returnType.iterable));

        this.dataRows.push(row);
        this.renderer.createRow(row, false);
    },

    startDataTableRowIndex : function() {
        if(this.properties) {
            return this.properties.length + 1;
        } else {
            return 1;
        }
    },

    toEditMode : function(element) {
        editElem = element.nextSibling;
        element.style.display = "none";
        editElem.style.display = "";
        editElem.firstChild.focus();
        editElem.firstChild.onblur = function() {
            this.onchange();
        };

        editElem.firstChild.onkeypress = function(event) {
            if(event.keyCode == 13) {
                this.onchange();
                return false;
            }
        };
    },

    toEditorMode : function(cell) {
        element = cell.firstChild;

        editor = new Editor();
        editor.initElement(cell.data, element);
    },

    toEditPropsMode : function(cell) {
        element = cell.firstChild;

        editor = new PropsEditor();
        editor.initElement(cell.props, element);
    },

    toNormalMode : function(element) {
        cell = element.parentNode.parentNode;

        editor = new Editor();
        editor.initReturnValue(cell.data, element);
    },

    selectCellValue : function(editElem) {
        this.renderer.selectValue(editElem);

        cell = this.getCellById(editElem.id);
        cell.value = editElem.value;
    },

    setPropValue : function(editElem , cellType) {
        this.renderer.selectValue(editElem);

        prop = this.getPropById(editElem.id);

        if(cellType == "PROPERTY_VALUE") {
            prop.name = editElem.value;
        } else {
            if(editElem.value == "") {
                prop.type = "undefined";
            } else {
                prop.type = editElem.value;
            }
        }
    },

    setInParamValue : function(editElem, paramId) {
        this.renderer.selectValue(editElem);

        if(paramId > -1) {
            this.header.inParam[paramId].name = editElem.value;
        } else {
            this.header.name = editElem.value;
        }
    },

    getCellById : function(index) {
        idList = index.split("_");
        rowId = idList[0].substr(1,idList[0].length);
        cellId = idList[1].substr(1,idList[1].length);

        row =  this.dataRows[rowId - (this.startDataTableRowIndex())];
        return row[cellId];
    },

    getPropById : function(index) {
        rowId = index.substr(1,index.length);
    
        return this.properties[rowId - 1];
    },

    setParamToParamList : function(id) {
        list = this.header.inParam;
        this.header.inParam = [];
        var set = false;
        var newParam = new Param('null', 'null', false, 'condition',"STRING");

        for (i = 0; i < list.length; i++) {
            if (i == id) {
                this.header.inParam.push(newParam);
                set = true;
            }

            this.header.inParam.push(list[i]);
        }

        if(list.length == 0 || set == false) {
            this.header.inParam.push(newParam);
        }

        return newParam;
    },

    createEmptyCol : function(id) {
        var newParam = this.setParamToParamList(id);

        for(i = 0; i < this.dataRows.length; i++) {
            row = this.dataRows[i];
            var cell = new Cell("empty", newParam.valuesType, newParam.iterable);

            row.splice(id, 0, cell);
        }

        this.renderer.createEmptyCol(id, this.dataRows);
    },

    deleteRow : function(rowId) {
        this.dataRows.splice(rowId - this.startDataTableRowIndex(),1);
        this.renderer.deleteRow(rowId);
    },

    deleteCondition : function(index) {
        this.deleteInputParam(index);
        this.renderer.deleteCondition(index);

        for(i = 0; i < this.dataRows.length; i++) {
            row = this.dataRows[i];
            row.splice(index,1);
        }
    },

    deleteInputParam : function(index) {
        tableModel.header.inParam.splice(index,1);
    },

    toEditPropTypeMode : function(element) {
        editElem = element.nextSibling;
        element.style.display = "none";
        editElem.style.display = "";
        editElem.innerHTML = document.getElementById("propsDataType").innerHTML;
        editElem.firstChild.id = element.id;
        editElem.firstChild.value = element.innerHTML;
        
        editElem.firstChild.focus();

        /*delete selected items*/
        for(i = 0; i < this.properties.length; i++) {
            if(this.properties[i].type != element.innerHTML) {
                $j(editElem.firstChild).find('option[value="'+this.properties[i].type+'"]').remove(); 
            }
        }
    },

    setDataTypeTo : function(elemId, value, id, valuesType) {
    //this.renderer.setDataTypeTo(elemId, value);
        if(id > -1) {
          this.header.inParam[id].type = value;
          this.header.inParam[id].valuesType = valuesType;

          this.changeColumnValueType(id, valuesType);
        } else {
          this.header.returnType.type = value;
          this.header.returnType.valuesType = valuesType;

          this.changeColumnValueType(id, valuesType);
        }

        this.renderer.refreshTableHeader();
    },

    changeColumnValueType : function(columnId, valuesType) {
        for(i = 1; i < this.dataRows.length; i++) {
            row = this.dataRows[i];

            if(columnId > -1) {
                row[columnId].valueType = valuesType;
            } else {
                row[row.length - 1].valueType = valuesType;
            }
        }
    },

    changeColumnIterableStatus : function(columnId, iterable) {
        for(i = 1; i < this.dataRows.length; i++) {
            row = this.dataRows[i];
            
            if(columnId > -1) {
                row[columnId].iterable = iterable;
            } else {
                row[row.length - 1].iterable = iterable;
            }
        }
    },

    setIterable : function(id, iterable) {
        if(id > -1) {
            this.header.inParam[id].iterable = iterable;
        } else {
            this.header.returnType.iterable = iterable;
        }

        this.renderer.refreshTableHeader();
        this.changeColumnIterableStatus(id, iterable);
    },

    restoreTableFromJSONString : function(restoreTable) {
        this.properties = JSON.parse(restoreTable.properties);

        for(var i = 0; i < this.properties.length; i++) {
            this.properties[i].getValue = function () {
                if(this.valueType == "DATE" && this.value.getDate) {
                    var curr_date = this.value.getDate();
                    var curr_month = this.value.getMonth();
                    curr_month++;
                    var curr_year = this.value.getFullYear();
                    return curr_month + "/" + curr_date + "/" + curr_year;
                } else {
                    return this.value;
                }
            };
        }

        this.dataRows = JSON.parse(restoreTable.dataRows);

        for(var i = 0; i < this.dataRows.length; i++) {
            var row = this.dataRows[i];
            for(var j = 0; j < row.length; j++) {
                row[j].getValue = function () {
                    if(this.valueType == "DATE" && this.value.getDate) {
                        var curr_date = this.value.getDate();
                        var curr_month = this.value.getMonth();
                        curr_month++;
                        var curr_year = this.value.getFullYear();
                        return curr_month + "/" + curr_date + "/" + curr_year;
                    } else {
                        return this.value;
                    }
                };
            }
        }

        this.header = restoreTable.header;
        this.header.inParam = JSON.parse(restoreTable.header.inParam);

        for(var i = 0; i < this.header.inParam.length; i++) {
            this.header.inParam[i].getValue = function () {
                if(this.valueType == "DATE" && this.value.getDate) {
                    var curr_date = this.value.getDate();
                    var curr_month = this.value.getMonth();
                    curr_month++;
                    var curr_year = this.value.getFullYear();
                    return curr_month + "/" + curr_date + "/" + curr_year;
                } else {
                    return this.value;
                }
            };
        }

        this.renderer.refreshTable(tableModel);
    },

    checkTable : function() {
        var checkingRes = [];
        /*Simple rule can have no input parameters
        if (this.header.inParam.length < 1) {
            checkingRes.push("Required at least one parameter");
        }*/

        if (this.dataRows.length < 2) {
            checkingRes.push("There are no rules row in the table. Please add at least one rules row in the table.");
        }

        this.checkNames(checkingRes);

        return checkingRes;
    },

    checkNames : function(checkingRes) {
        re =/^([a-zA-Z_][a-zA-Z_0-9]*)$/;
        onlyChar = /^([a-zA-Z]+)/;

        if (!re.test(this.header.name)) {
            checkingRes.push("Table name '{0}' is invalid. Name should start with letter or symbols '_' and contain only letters, numbers or symbol '_'.");
        }

        if (this.header.name.length == 1) {
            if (!onlyChar.test(this.header.name)) {
                checkingRes.push("Table name is invalid. Only letters can be used as one symbol table name.");
            }
        }

        for (var i = 0; i < this.header.inParam.length; i++) {
            if(this.header.inParam[i].type == "null") {
                checkingRes.push("Parameter type "+this.header.inParam[i].type+" is invalid");
            }

            if(!re.test(this.header.inParam[i].name)) {
                checkingRes.push("Parameter name "+this.header.inParam[i].name+" is invalid. Name should start with letter or symbols '_' and contain only letters, numbers or symbol '_'");
            }
        }
    },
};

function Cell(value, valueType, iterable) {
    this.value = value;
    this.valueType = valueType;
    this.iterable = iterable;

    this.getValue = function () {
        if(this.valueType == "DATE" && this.value.getDate) {
            var curr_date = this.value.getDate();
            var curr_month = this.value.getMonth();
            curr_month++;
            var curr_year = this.value.getFullYear();
            return curr_month + "/" + curr_date + "/" + curr_year;
        } else {
            return this.value;
        }
    };
}

function Property() {
    var name;
    var type;
    var value;

    this.getValue = function () {
        if(this.valueType == "DATE" && this.value.getDate) {
            var curr_date = this.value.getDate();
            var curr_month = this.value.getMonth();
            curr_month++;
            var curr_year = this.value.getFullYear();
            return curr_month + "/" + curr_date + "/" + curr_year;
        } else {
            return this.value;
        }
    };
}

function Param(name, type, iterable, columnType, valuesType){
    this.name = name;
    this.type = type;
    this.iterable = iterable;
    this.columnType = columnType;
    this.valuesType = valuesType;
}