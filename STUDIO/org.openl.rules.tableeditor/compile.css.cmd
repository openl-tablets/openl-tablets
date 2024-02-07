type ^
	css\common.css ^
	css\tooltip.css ^
	css\datepicker.css ^
	css\multiselect.css ^
	css\numberrange.css ^
	css\colorPicker.css ^
	css\popup.css > css\tableeditor.all.css
java -jar yuicompressor-2.4.7.jar css\tableeditor.all.css > css\tableeditor.min.css	
