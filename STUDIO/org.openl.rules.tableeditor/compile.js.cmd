type ^
    js\tooltip.js ^
    js\ScriptLoader.js ^
    js\TableEditor.js ^
    js\popup\popupmenu.js ^
    js\BaseEditor.js ^
    js\BaseTextEditor.js ^
    js\datepicker.js ^
    js\TextEditor.js ^
    js\MultiLineEditor.js ^
    js\NumericEditor.js ^
    js\DropdownEditor.js ^
    js\FormulaEditor.js ^
    js\BooleanEditor.js ^
    js\DateEditor.js ^
    js\MultiselectEditor.js ^
    js\ArrayEditor.js ^
    js\NumberRangeEditor.js ^
    js\colorPicker.js ^
    js\popup.js > js\tableeditor.all.js
java -jar yuicompressor-2.4.7.jar js\tableeditor.all.js > js\tableeditor.min.js 
