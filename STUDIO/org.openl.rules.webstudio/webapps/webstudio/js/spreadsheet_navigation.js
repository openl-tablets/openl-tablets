var currentRow=0;
var currentColumn=0;

var initialRow=1;
var initialColumn=1;
var lastCell;
var lastColor;
var oldMargin;
var oldPadding;
var oldBorder;
var oldBorderWidth;
var oldBorderStyle;
var oldBorderColor;

var selectColor1 = 'rgb(255,255,0)'; 
var selectColor2 = 'rgb(255,0,0)';

var isEditing = false;

function parse_rgb(s) {
	var i1 = s.indexOf(',');
	var red = s.substring(4,i1);
	var s2 = s.substring(i1+1,s.length);
	var i2 = s2.indexOf(',');
	var green = s2.substring(0,i2);
	var blue = s3.substring(0,s3.length-1);
	
	return [red,green,blue];		
}

function invertRGB(s) {
	var rgb = parse_rgb(s);
	return 'rgb(' + (255-rgb[0]) + ',' + (255-rgb[1]) + ',' + (255-rgb[2]) + ')';
}

function checkInitial() {
	//
	if ((0 == currentRow) && (0 == currentColumn)) {
		currentRow = initialRow;
		currentColumn = initialColumn;
		refreshSelection();
		return true;
	} else {
		return false;
	}
}

function refreshSelection() {
	var cell = findCell(currentRow,currentColumn);
	
//	if (undefined != lastColor) {
//		lastCell.style.backgroundColor = lastColor;
//	}
//	lastColor = cell.style.backgroundColor;
//	cell.style.backgroundColor = selectColor1;


	if (undefined != lastCell) {
		lastCell.style.margin = oldMargin;
		lastCell.style.padding = oldPadding;
		lastCell.style.border = oldBorder;
		lastCell.style.borderWidth = oldBorderWidth;
		lastCell.style.borderStyle = oldBorderStyle;
		lastCell.style.borderColor = oldBorderColor;
	//	alert(oldBorder);
	}
	oldMargin = cell.style.margin;
	oldPadding = cell.style.padding;
	oldBorder = cell.style.border;
	oldBorderWidth = cell.style.borderWidth;
	oldBorderStyle = cell.style.borderStyle;
	oldBorderColor = cell.style.borderColor;
	
	cell.style.margin='1px';
	cell.style.padding='1px';
	cell.style.borderWidth = '3px';
	cell.style.borderStyle = 'solid';
	cell.style.borderColor = '#666633';

	lastCell = cell;
	
	stopEditing();
	
	refreshSelectionAfter();	
}

function findCell(row,column) {
	var id='cell-' + row + '-' + column + '_';
	var els = document.getElementsByTagName('td');
	for(i=0;i<els.length;i++)
	{
		//alert(els[i].title + ' ' + id);
		if ((null != els[i].title) && (undefined != els[i].title)) {
			if ((-1) < els[i].title.indexOf(id)) {
				return els[i];
			}
		}
	}
	return null;
}

function move(direction) {
	if (true == checkInitial()) {
		return;	
	}
	var el;
	var row = currentRow;
	var col = currentColumn;
	do {
		switch(direction) {
			case('LEFT'):col--;break;
			case('RIGHT'):col++;break;
			case('UP'):row--;break;
			case('DOWN'):row++;break;				
		}
		el = findCell(row,col);
		if (null == el) {
			return;
		}
	} while(el == lastCell);
	
	currentColumn = col;
	currentRow = row;
	refreshSelection();	
}

//function beginEditing() {
//	if ((null != lastCell) && (undefined != lastCell)) {
//		alert(lastCell.title);
//		document.getElementById('editor_form:current_cell_title').value=lastCell.title;
//		//document.getElementById('editor_form').submit();
//		alert(document.getElementById('editor_form:current_cell_title').value);
//		document.getElementById('editor_form:activator').onclick();
	//}
//}

function findElement(id) {

}

function bodyOnKeyUp(event) 
{
	if (false == isEditing) {
	//alert(event.keyCode);
	switch(event.keyCode) {
		case(40):
			//alert('DOWN pressed');
			move('DOWN');
			break;
		case(9):		
		case(39):
			//alert('RIGHT pressed');
			move('RIGHT');
			break;
		case(37):
			//alert('LEFT pressed');
			move('LEFT');
			break;
		case(38):
			//alert('UP pressed');
			move('UP');
			break;
		case(13):
		case(113):
			//alert('ENTER pressed');
			beginEditing();
			break;
//		default:
//			alert('unknown key pressed');
	}
	} else {
	switch(event.keyCode) {
		case(13):
		case(113):
			//alert('ENTER pressed');
			stopEditing();
			break;
		}
	}
}

function extractPosition(title) {
	var i = title.indexOf('cell-');
	if ((-1) < i) {
		var s = title.substring(5,title.length); 
		var j = s.indexOf('-');
		var k = s.indexOf('_'); 
		if (((-1) < j) && ((-1) < k)) {
			return [(s.substring(0,j)),(s.substring(j+1,k))];
		}
	}
}


function bodyOnMouseDown(event) {
	
	if (false == isEditing) {
	
	var targetTitle;
	if(undefined != event.target) {
		//alert('it is firefox');
		var targetTitle = event.target.title; 
	} else {
		if(undefined != event.srcElement) {
			//alert('it is IE');
			var targetTitle = event.srcElement.title;
		}
	}
	
	if (('' != targetTitle) && (undefined != targetTitle) && (null != targetTitle)) {
	
	
		//var re = new RegExp('^(cell-/d+-/d+_)+$');
		//var result = targetTitle.match(re);
		//alert(result);
		//if ((undefined != result) && (null != result)) {
		
			var pos = extractPosition(targetTitle);
			
			//var row = extractRow(targetTitle);
			//var col = extractColumn(targetTitle);
			
			if ((undefined != pos[0]) && (undefined != pos[1])) {
				currentRow = pos[0];
				currentColumn = pos[1];
				refreshSelection();
			}
		//}
	}
	
	} else {
		stopEditing();
	}
}