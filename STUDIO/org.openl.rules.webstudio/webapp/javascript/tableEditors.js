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
                ws.ui.hideLoader();
                initComplexSelect(data, cell);
            },
            error:function(XMLHttpRequest, textStatus, errorThrown){
                ws.ui.hideLoader();
            }
        });
    };
};

function createDiv() {
    var newElement = document.createElement('div');
    newElement.id = "div_editor_holder" + Math.floor(Math.random() * 1001);

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

       editor = new DateEditor('', specElement.id, '', prop.getValue(), true);
   } else if (data.type == "MULTI") {
       editor = new MultiselectEditor('', element.id, data, prop.getValue(), true);
   } else if (data.type == "SINGLE") {
       editor = new DropdownEditor('', element.id, data.param, prop.getValue() , true);
   } else if (data.type == "BOOLEAN") {
       editor = new BooleanEditor('', element.id, '', prop.getValue() == true ? "true" : "false", true);
   } else {
       editor = new TextEditor('', element.id, '', prop.getValue() , true);
   }

   setNewEditor(cell, editor);
};

function showEditorDiv(cell, elementForAdding) {
    var editorDiv = $j("#editor_div");

    if (typeof elementForAdding != "undefined") {
        editorDiv.html("");
        editorDiv.append(elementForAdding);
    }

    var minWidth = 20;
    var width = cell.offsetWidth - 2;

    if (width < minWidth) {
        cell.style.minWidth = minWidth + "px";
        width = cell.offsetWidth - 2;
    }

    var jcell = $j(cell);

    var position = {
        top : jcell.position().top + jcell.offsetParent().scrollTop(),
        left : jcell.position().left +  + jcell.offsetParent().scrollLeft()
    };

    //var height = jcell.outerHeight();
    //$j("#editor_div").css({"height" : height + "px"});
    var elemHeight;
    var elemWidth;

    var browserName = navigator.appName;
    if (browserName == "Netscape") { 
        if (navigator.userAgent.toLowerCase().indexOf('chrome') > -1) {
            //chrome
            elemHeight = cell.offsetHeight + 2 + "px";
            elemWidth = width + 2 +"px";
        } else {
            elemHeight = cell.offsetHeight - 1 + "px";
            elemWidth = width +"px";
        }

        /* Setting div size for correct rendering background if the edit element is smaller then the Cell size.
         F.ex: BooleanEditor. */
        editorDiv.height(elemHeight);
        editorDiv.width(elemWidth);
    } else if (browserName=="Microsoft Internet Explorer") {
        elemHeight = (cell.offsetHeight - 8) + "px";
        elemWidth = (width - 5) + "px";

        /* Setting div size for correct rendering background if the edit element is smaller then the Cell size.
         F.ex: BooleanEditor. */
        editorDiv.height(cell.offsetHeight -1  + "px");
        editorDiv.width(width + 1 + "px");
    }
    //For correct work of DateEditor
    editorDiv.find(">:first-child").height(elemHeight);
    editorDiv.find(">:first-child").width(elemWidth);

    editorDiv.css(position);
    editorDiv.show();
}

function setNewEditor(cell, editor) {
    $j("#editor_div").keypress(function(event) {
        if(event.keyCode == 13) {
            closeEditor(cell, editor);
            return false;
        }
    });

    editor.bind("blur", function() {
        closeEditor(cell, editor);
    });
}

function closeEditor(cell, editor) {
    var dataCell = cell.data;
    $j("#editor_div").hide();
    $j("#editor_div").offset({left:0,top:0});
    dataCell.value = editor.getValue();
    editor.destroy();
    cell.innerHTML = dataCell.getValue();
};

function Editor(){
    this.initElement = function(cell) {
        var dataCell = cell.data;

        var element = createDiv();
        showEditorDiv(cell, element);
        var editor = null;

        if ((dataCell.valueType == "INT" || dataCell.valueType == "FLOAT" ) && !dataCell.iterable) {
            editor = new NumericEditor('', element.id, '', dataCell.getValue() , true);
        } else if (dataCell.valueType == "BOOLEAN" && !dataCell.iterable) {
            editor = new BooleanEditor('', element.id, '', dataCell.getValue() == true ? "true" : "false", true);
        } else if (dataCell.valueType == "DATE" && !dataCell.iterable) {
            editor = new DateEditor('', element.id, '', dataCell.getValue() , true);
        } else if (dataCell.valueType == "STRING" && !dataCell.iterable) {
            editor = new TextEditor('', element.id, '', dataCell.getValue() , true);
        } else if (dataCell.valueType == "RANGE") {
            editor = new NumberRangeEditor('', element.id, '', dataCell.getValue() , true);
        } else {
            editor = new TextEditor('', element.id, '', dataCell.getValue() , true);
        }

        setNewEditor(cell, editor);
    };
};