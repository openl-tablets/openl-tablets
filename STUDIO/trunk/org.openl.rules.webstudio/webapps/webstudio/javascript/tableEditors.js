function PropsEditor () {
    this.initElement = function(cellProps, element) {
        var propName = cellProps.type;

        var actionURL = "/webstudio/action/prop_values";
        $j.ajax({
            type:"GET",
            url:actionURL,
            data: "propName="+propName,
            dataType: "json",
            success:function(data, textStatus, XMLHttpRequest){
                hideLoader();
                initComplexSelect(data, element);
            },
            error:function(XMLHttpRequest, textStatus, errorThrown){
                hideLoader();
            }
        });
    };
};

function initComplexSelect(data, element) {
   element.id = "cmplx_select_" + Math.floor(Math.random() * 1001);
   prop = element.parentNode.props;
   var editor;

   if (data.type == "DATE") {
       editor = new DateEditor('', element.id, '', prop.getValue() , '');
   } else if (data.type == "TEXT") {
       editor = new TextEditor('', element.id, '', prop.getValue() , '');
   } else if (data.type == "SINGLE") {
       editor = new DropdownEditor('', element.id, data.param, prop.getValue() , '');
   } else if (data.type == "BOOLEAN") {
       editor = new BooleanEditor('', element.id, '', prop.getValue() , '');
   } else {
       editor = new MultiselectEditor('', element.id, data, element.innerHTML, '');
       editor.open();
   }

   element.onclick = function() {};
   element.parentNode.onclick = function() {};

   editor.getInputElement().onblur = function() {
       element.innerHTML = editor.getValue();

       //set action to cell
       element.parentNode.onclick = function(element) {
           tableModel.toEditPropsMode(this);
       };

       if (data.type == "DATE") {
           element.parentNode.props.value = editor.getValue();
       } else {
           element.parentNode.props.value = editor.getValue();
           editor.close();
       }
   };

};

function Editor(){
    var html = "";
    var value = "";

    this.initElement = function(dataCell, element) {
        if(dataCell.valueType == "INT" && !dataCell.iterable) {
            this.html = this.getIntElement(dataCell);

            element.innerHTML = "";
            element.appendChild(this.html);
            element.firstChild.focus();
        } else if(dataCell.valueType == "BOOLEAN" && !dataCell.iterable) {
            this.html = this.getBooleanElement(dataCell);

            element.innerHTML = "";
            element.appendChild(this.html);
            element.firstChild.focus();
        } else if(dataCell.valueType == "DATE" && !dataCell.iterable) {
            element.id = Math.floor(Math.random() * 1001);
            dateEditor = new DateEditor('', element.id, '', dataCell.getValue() , '');

            element.onclick = function() {};

            dateEditor.getInputElement().onblur = function() {
                element.innerHTML = dateEditor.getValue();

                element.parentNode.onclick = function(element) {
                    tableModel.toEditorMode(this);
                };

                element.parentNode.data.value = dateEditor.getValue();
                dateEditor.destroy(element.id);
            };

            element.focus();
        } else if(dataCell.valueType == "STRING" && !dataCell.iterable) {
            this.html = this.getStringElement(dataCell);

            element.innerHTML = "";
            element.appendChild(this.html);

            element.firstChild.focus();
        } else {
            this.html = this.getStringElement(dataCell);

            element.innerHTML = "";
            element.appendChild(this.html);

            element.firstChild.focus();
        }

        element.setAttribute('onclick','');
        element.parentNode.setAttribute('onclick','');
    };

    this.initReturnValue = function(dataCell, element) {
        if(dataCell.valueType == "INT") {
            this.value = element.value;
        } else if(dataCell.valueType == "BOOLEAN") {
            this.value = element.checked;
        } else if(dataCell.valueType == "DATE") {
            arr = element.value.split("/");

            if(arr.length == 3) {
                this.value = new Date(arr[2],arr[0]-1,arr[1]);
            } else {
                this.value = element.value;
            }
            //this.value = element.value;
        } else if(dataCell.valueType == "STRING") {
            this.value = element.value;
        } else {
            this.value = element.value;
        }
        //set action to cell
        element.parentNode.parentNode.setAttribute('onclick','tableModel.toEditorMode(this)');

        dataCell.value = this.value;
        span = element.parentNode;
        span.innerHTML = dataCell.getValue();
    };

    this.getIntElement = function(dataCell) {
        var newElement = document.createElement('input');
        newElement.type = 'text';
        newElement.value = dataCell.getValue();

        newElement.onchange = function () {
            tableModel.toNormalMode(this);
        }

        newElement.onblur = function () {
            tableModel.toNormalMode(this);
        }

        newElement.onkeypress = function(event) {
            var v = this.value;
            if (event.charCode == 0) return true;
            var code = event.charCode == undefined ? event.keyCode : event.charCode;

            if (code == 45)  // minus
                return v.indexOf("-") < 0;
            if (code == 46)  // point
                return v.indexOf(".") < 0;

            return code >= 48 && code <= 57; // digits (0-9)
        }
        
        newElement.focus();
        return newElement;
    };

    this.getBooleanElement = function(dataCell) {
        var newElement = document.createElement('input');
        newElement.type = 'checkbox';

        if(dataCell.value != true) {
            newElement.value = false;
            newElement.checked = "";
        } else {
            newElement.value = true;
            newElement.checked = "checked";
        }

        newElement.onchange = function () {
            tableModel.toNormalMode(newElement);
        }

        newElement.onblur = function () {
            tableModel.toNormalMode(newElement);
        }
        
        newElement.focus();
        return newElement;
    };

    this.getDateElement = function(dataCell, element) {
        var newElement = new Element("input");
        newElement.setAttribute("type", "text");
        newElement.value = dataCell.getValue();
        /*gen random id*/
        newElement.id = Math.floor(Math.random() * 1001);
        //newElement.setAttribute('onclick','datePickerController.show('+newElement.id+')');
        //newElement.setAttribute("onchange","tableModel.toNormalMode(this)");

        newElement.onclick = function() {
            datePickerController.show(newElement.id);
        };

        newElement.onblur = function() {
            tableModel.toNormalMode(this);
        };

        element.innerHTML = "";
        element.appendChild(newElement);

        var datePickerOpts = {
            formElements: {},
            noFadeEffect: true,
            finalOpacity: 100
        };

        datePickerOpts.formElements[newElement.id] = "m-sl-d-sl-Y";

        var datePickerGlobalOpts = {
            noDrag: true
        };

        datePickerController.setGlobalVars(datePickerGlobalOpts);
        datePickerController.createDatePicker(datePickerOpts);

        //datePickerController.show(newElement.id);
        //return newElement;
    };

    this.getStringElement = function(dataCell) {
        var newElement = document.createElement('input');
        newElement.type = 'text';
        newElement.value = dataCell.value;
        newElement.setAttribute('onchange','tableModel.toNormalMode(this)');
        newElement.setAttribute('onblur','tableModel.toNormalMode(this)');

        return newElement;
    };
};