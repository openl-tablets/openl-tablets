var tableModel = {
    properties : [],
    propertyStyle : [],
    dataRows : [],
    renderer : null,

    header : {
        name : null,
        returnType : null,
        style : [],
        inParam : []
    },

    headerRow : function() {
        var params = "";

        for(var i = 0; i < this.header.inParam.length; i++) {
            if(i > 0) {
                params += ", ";
            }
            params += "<span id=\"param"+i+"\" onclick=\"selectDataTypeAction(this,event,"+i+","+this.header.inParam[i].iterable+")\">"
            + this.header.inParam[i].type + ((this.header.inParam[i].iterable == true)? "[]" : "") +" </span> "
            +"<span style=\"display : none; position: absolute;\"><input type=\"text\" class=\"editTableInParam\" value=\""+((this.header.inParam[i].name == 'null') ? "" :  this.header.inParam[i].name)+"\" onchange=\"tableModel.setInParamValue(this,"+i+")\"/></span>"
            +"<span onclick='tableModel.toEditHeaderMode(this)' id=\"param_value"+i+"\">" 
            + ((this.header.inParam[i].name == 'null' || this.header.inParam[i].name == "") ? "undefined" :  this.header.inParam[i].name) +"</span>";
        }

        return  "<font style=\"position: relative\">SimpleRules <span id=\"returnSRT\" onclick=\"selectDataTypeAction(this,event, -1,"+this.header.returnType.iterable+")\">"+
        this.header.returnType.type + ((this.header.returnType.iterable == true)? "[]" : "") +"</span> <span style=\"display : none; position: absolute\"><input type=\"text\" class=\"editTableInParam\" value=\""+this.header.name+"\" onchange=\"tableModel.setInParamValue(this,-1)\"/></span><span onclick='tableModel.toEditHeaderMode(this)'>" + this.header.name
        + "</span> (" + params + ")</font>";
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

        for (var i = 0;  i < this.header.inParam.length; i++) {
            row.push(new Cell(this.header.inParam[i].name, "STRING", false, false));
        }

        row.push(new Cell("RETURN", "STRING", false, true));

        this.dataRows.push(row);
        this.renderer.createRow(row, true);
    },

    headerSpanCell : function () {
        if (this.header.inParam) {
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

        for (i = 0;  i < this.header.inParam.length; i++) {
            row.push(new Cell("", this.header.inParam[i].valuesType, this.header.inParam[i].iterable, false));
        }

        row.push(new Cell("",this.header.returnType.valuesType, this.header.returnType.iterable, true));

        this.dataRows.push(row);
        this.renderer.createRow(row, false, true);
    },

    startDataTableRowIndex : function() {
        if (this.properties) {
            return this.properties.length + 1;
        } else {
            return 1;
        }
    },

    toEditHeaderMode : function(element) {
        editElem = element.previousSibling;
        editElem.style.display = "";
        var textEditor = editElem.firstChild;
        var editorCharLength = textEditor.value.length;
        var editorWidth  = (editorCharLength + 1 ) * 6 + "px";
        var editorHeight = "22px";

        // When typing a symbol into the text field it's length increases 
        textEditor.onkeydown = function () {

            this.style.width = $j(this).outerWidth() <  ((this.value.length + 1) * 6) ? ((this.value.length + 1) * 6) + 'px' : $j(this).outerWidth();
        };

        var browserName = navigator.appName;
        if (browserName == "Netscape") { 
            if (navigator.userAgent.toLowerCase().indexOf('chrome') > -1) {
                //chrome
                editorHeight = ($j(element).outerHeight() + 2) + "px";
            } else {
                //FF
                editorHeight = ($j(element).outerHeight() + 3) + "px";
            }
        } else if (browserName=="Microsoft Internet Explorer") {
            editorHeight = ($j(element).outerHeight() ) + "px";
        }

        textEditor.style.width = editorWidth;
        textEditor.style.height = editorHeight;

        textEditor.focus();
        textEditor.onblur = function() {
            this.onchange();
        };

        textEditor.onkeypress = function(event) {
            if(event.keyCode == 13) {
                this.onchange();
                return false;
            }
        };
    },

    toEditDataMode : function(cell, event) {
        event.stopPropagation();
        element = cell.firstChild;

        editor = new Editor();
        editor.initElement(cell);

        return false;
    },

    toEditPropsMode : function(cell) {
        element = cell.firstChild;

        editor = new PropsEditor();
        editor.initElement(cell);
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

        if (cellType == "PROPERTY_VALUE") {
            prop.name = editElem.value;
        } else {
            if (editElem.value == "") {
                prop.type = "undefined";
            } else {
                prop.type = editElem.value;
            }
        }
    },

    setInParamValue : function(editElem, paramId) {
        if (paramId > -1) {
            if (editElem.value != "") {
                this.header.inParam[paramId].name = editElem.value;
                this.renderer.setConditionTitle(editElem.value, paramId);
                this.renderer.selectValue(editElem);
            } else {
                this.header.inParam[paramId].name = "param" + (paramId + 1);
                this.renderer.setConditionTitle("param" + (paramId + 1), paramId);
                editElem.value = "param" + (paramId + 1);
                this.renderer.selectValue(editElem);
            }
        } else {
            this.header.name = editElem.value;
            this.renderer.selectValue(editElem);
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
        var newParam = new Param('param'+(id+1), 'null', false, 'condition',"STRING");

        for (i = 0; i < list.length; i++) {
            if (i == id) {
                this.header.inParam.push(newParam);
                set = true;
            }

            this.header.inParam.push(list[i]);
        }

        if (list.length == 0 || set == false) {
            this.header.inParam.push(newParam);
        }

        return newParam;
    },

    createEmptyCol : function(id) {
        var newParam = this.setParamToParamList(id);

        for(var i = 0; i < this.dataRows.length; i++) {
            row = this.dataRows[i];
            var cell = new Cell("", newParam.valuesType, newParam.iterable, false);

            row.splice(id, 0, cell);
        }

        this.renderer.createEmptyCol(id, this.dataRows);

        // Show popup for setting patameter type
        $j("#param" + id).click();

        // Set popup menu on the bottom of param
        var position = {
            "top" : $j("#param" + id).offset().top + "px",
            "left" : $j("#param" + id).offset().left + $j("#param" + id).width() - 3  + "px"
        };
        setTimeout(function() {
            $j(PopupMenu.te_menu).css(position);
        }, 10);
    },

    deleteRow : function(rowId) {
        this.dataRows.splice(rowId - this.startDataTableRowIndex(),1);
        this.renderer.deleteRow(rowId);
    },

    deleteCondition : function(index) {
        this.deleteInputParam(index);
        this.renderer.deleteCondition(index);

        for (i = 0; i < this.dataRows.length; i++) {
            row = this.dataRows[i];
            row.splice(index,1);
        }
    },

    deleteInputParam : function(index) {
        tableModel.header.inParam.splice(index,1);
    },

    toEditPropTypeMode : function(element) {
        editElem = element.previousSibling;
        element.style.display = "none";
        editElem.style.display = "";
        editElem.innerHTML = $j("#propsDataType").html();
        var selectBox = editElem.firstChild;
        selectBox.id = element.id;
        selectBox.value = element.innerHTML;

        selectBox.focus();

        /*delete selected items*/
        for (i = 0; i < this.properties.length; i++) {
            if(this.properties[i].type != element.innerHTML) {
                $j(selectBox).find('option[value="'+this.properties[i].type+'"]').remove(); 
            }
        }

    },

    setDataTypeTo : function(elemId, value, id, valuesType) {
        if (id > -1) {
          this.header.inParam[id].type = value;
          this.header.inParam[id].valuesType = valuesType;

          this.changeColumnValueType(id, valuesType);
        } else {
          this.header.returnType.type = value;
          this.header.returnType.valuesType = valuesType;

          this.changeColumnValueType(id, valuesType);
        }

        this.renderer.refreshTableHeader();

        if (id > -1 && this.header.inParam[id].name == 'param'+(id+1)) {
            //open property name editor
            $j("#param_value"+id).click();
        }
    },

    changeColumnValueType : function(columnId, valuesType) {
        for (i = 1; i < this.dataRows.length; i++) {
            row = this.dataRows[i];

            if (columnId > -1) {
                row[columnId].valueType = valuesType;
            } else {
                row[row.length - 1].valueType = valuesType;
            }
        }
    },

    changeColumnIterableStatus : function(columnId, iterable) {
        for (i = 1; i < this.dataRows.length; i++) {
            row = this.dataRows[i];

            if (columnId > -1) {
                row[columnId].iterable = iterable;
            } else {
                row[row.length - 1].iterable = iterable;
            }
        }
    },

    setIterable : function(id, iterable) {
        if (id > -1) {
            this.header.inParam[id].iterable = iterable;
        } else {
            this.header.returnType.iterable = iterable;
        }

        this.renderer.refreshTableHeader();
        this.changeColumnIterableStatus(id, iterable);
    },

    restoreTableFromJSONString : function(restoreTable) {
        this.properties = JSON.parse(restoreTable.properties);

        for (var i = 0; i < this.properties.length; i++) {
            this.properties[i].getValue = function () {
                if (this.valueType == "DATE" && this.value.getDate) {
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

        for (var i = 0; i < this.dataRows.length; i++) {
            var row = this.dataRows[i];
            for (var j = 0; j < row.length; j++) {
                row[j].getValue = function () {
                    if (this.valueType == "DATE" && this.value.getDate) {
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

        for (var i = 0; i < this.header.inParam.length; i++) {
            this.header.inParam[i].getValue = function () {
                if (this.valueType == "DATE" && this.value.getDate) {
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

        if (this.dataRows.length < 2) {
            checkingRes.push("There are no rules in the table. <a href=\"#\" title=\"Add rule\" onclick=\"tableModel.createEmptyRow(); return false;\">Add</a> at least one rule to the table.");
        }

        this.checkNames(checkingRes);

        return checkingRes;
    },

    checkNames : function(checkingRes) {
        re =/^([a-zA-Z_][a-zA-Z_0-9]*)$/;
        onlyChar = /^([a-zA-Z]+)/;
        var repeatedNames = [];

        if (!re.test(this.header.name)) {
            checkingRes.push("Table name '"+this.header.name+"' is invalid. Name should start with letter or symbols '_' and contain only letters, numbers or symbol '_'.");
        }

        if (this.header.name.length == 1) {
            if (!onlyChar.test(this.header.name)) {
                checkingRes.push("Table name '"+this.header.name+"' is invalid. Only letters can be used as one symbol table name.");
            }
        }

        for (var i = 0; i < this.header.inParam.length; i++) {
            if (this.header.inParam[i].type == "null") {
                checkingRes.push("Parameter type cannot be empty. Select the parameter type.");
            }

            if (this.header.inParam[i].name.length == 0) {
                checkingRes.push("Parameter name cannot be empty. The name should start with a letter or a symbols '_' and contain only letters, numbers or symbol '_'.");
            } else if (!re.test(this.header.inParam[i].name)) {
                checkingRes.push("Parameter name '"+this.header.inParam[i].name+"' is invalid. The name should start with a letter or a symbols '_' and contain only letters, numbers or symbol '_'.");
            }

            for (var paramId = 0; paramId < this.header.inParam.length; paramId++) {
                if (paramId != i && this.header.inParam[paramId].name == this.header.inParam[i].name) {
                    if (repeatedNames.indexOf(this.header.inParam[i].name) == -1) {
                        checkingRes.push("Parameter '"+this.header.inParam[i].name+"' already exists. Rename this parameter.");
                        repeatedNames.push(this.header.inParam[i].name);
                    }
                }
            }
        }

        for (var i = 1; i < this.dataRows.length; i++) {
            var isEmpty = true;
            var row = this.dataRows[i];

            for (var col = 0; col < row.length; col++) {
                var cell = row[col];

                if ((cell.getValue() != null) && (cell.getValue()+"" != "")) {
                    isEmpty = false;
                }
            }

            if (isEmpty) {
                checkingRes.push("Rule row #"+(i)+" is empty. Fill it in.");
            }

        }
    }
};

function Cell(value, valueType, iterable, isReturn) {
    this.value = value;
    this.valueType = valueType;
    this.iterable = iterable;
    this.isReturn = isReturn;
    this.style;

    this.getValue = function () {
        if (this.valueType == "DATE" && this.value.getDate) {
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
    var style;

    this.getValue = function () {
        if (this.valueType == "DATE" && this.value.getDate) {
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
