function PropsEditor () {
    this.initElement = function(cell) {
        var cellProps = cell.data;
        
        var propName = cellProps.type;

        var actionURL = "/webstudio/action/prop_values";
        $j.ajax({
            type:"GET",
            url:actionURL,
            data: "propName="+propName,
            dataType: "json",
            success:function(data, textStatus, XMLHttpRequest){
                hideLoader();
                initComplexSelect(data, cell);
            },
            error:function(XMLHttpRequest, textStatus, errorThrown){
                hideLoader();
            }
        });
    };
};

function createDiv() {
    var newElement = document.createElement('div');
    newElement.id = "div_editor_holder"+Math.floor(Math.random() * 1001);

    return newElement;
}

function initComplexSelect(data, cell) {
   var element = createDiv();

   prop = cell.data;
   var editor;
   showEditorDiv(cell, element);

   if (data.type == "DATE") {
       var specElement = createDiv();
       $j(element).append(specElement);
       
       editor = new DateEditor('', specElement.id, '', prop.getValue() , '');
   } else if (data.type == "TEXT") {
       editor = new TextEditor('', element.id, '', prop.getValue() , true);
   } else if (data.type == "SINGLE") {
       editor = new DropdownEditor('', element.id, data.param, prop.getValue() , true);
   } else if (data.type == "BOOLEAN") {
       editor = new BooleanEditor('', element.id, '', prop.getValue() == true ? "true" : "false", true);
   } else {
       editor = new MultiselectEditor('', element.id, data, element.innerHTML, true);
       editor.open();
   }

   setNewEditor(cell, editor);
};

function closeEditor(cell, value) {
    var dataCell = cell.data;
    $j("#editor_div").hide();
    $j("#editor_div").offset({left:0,top:0});
    dataCell.value = value;
    cell.innerHTML = dataCell.getValue();
};

function showEditorDiv(cell, elementForAdding) {
    if (typeof elementForAdding != "undefined") {
        $j("#editor_div").html("");
        $j("#editor_div").append(elementForAdding);
    }

    var topPos = $j(cell).position().top;
    var leftPos = $j(cell).position().left;

    var position = {
            top : topPos,
            left : leftPos
    };

    $j('#editor_div').css(position);

    $j("#editor_div").height("18px");
    $j("#editor_div").width(($j(cell).outerWidth())+"px");

    $j("#editor_div").show();

    $j("#editor_div").find(">:first-child").height("18px");
    $j("#editor_div").find(">:first-child").width(($j(cell).outerWidth())+"px");
    $j("#editor_div").find(">:first-child").focus();
}

function setNewEditor(cell, editor) {
    $j("#editor_div").keypress(function(event) {
        if(event.keyCode == 13) {
            closeEditor(cell, editor.getValue());
            return false;
        }
    });

    editor.getInputElement().onblur = function() {
        closeEditor(cell, editor.getValue());
    };
}

function Editor(){
    this.initElement = function(cell) {
        var dataCell = cell.data;

        var element = createDiv();
        showEditorDiv(cell, element);
        var editor = null;

        if((dataCell.valueType == "INT" || dataCell.valueType == "FLOAT" ) && !dataCell.iterable) {
            //this.html = this.getIntElement(cell);
            editor = new NumericEditor('', element.id, '', dataCell.getValue() , true);
        } else if(dataCell.valueType == "BOOLEAN" && !dataCell.iterable) {
            //this.html = this.getBooleanElement(cell);
            editor = new BooleanEditor('', element.id, '', dataCell.getValue() == true ? "true" : "false", true);
        } else if(dataCell.valueType == "DATE" && !dataCell.iterable) {
            editor = new DateEditor('', element.id, '', dataCell.getValue() , '');
        } else if(dataCell.valueType == "STRING" && !dataCell.iterable) {
            //this.html = this.getStringElement(cell);
            editor = new TextEditor('', element.id, '', dataCell.getValue() , true);
        } else {
            editor = new NumericEditor('', element.id, '', dataCell.getValue() , true);
        }

        setNewEditor(cell, editor);
    };
};