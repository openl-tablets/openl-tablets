

<%@ page import = "org.openl.rules.webtools.*" %>
<%@ page import = "javax.faces.context.FacesContext" %>
<%@ page import = "org.openl.jsf.*" %>


<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://richfaces.ajax4jsf.org/rich" prefix="rich" %>
<%@ taglib uri="https://ajax4jsf.dev.java.net/ajax" prefix="a4j" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c"%>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<%@include file="checkTimeout.jsp"%>

<%
	String s_id = request.getParameter("elementID"); 
   	int elementID = -100; 	
   	if (s_id != null)
   	{
     	elementID = Integer.parseInt(s_id);
     	studio.setTableID(elementID);
    }
    else 
      elementID = studio.getTableID(); 	
   String url = studio.getModel().makeXlsUrl(elementID);
   String uri = studio.getModel().getUri(elementID);
   String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);   
   String name = studio.getModel().getDisplayNameFull(elementID);
   boolean isRunnable  = studio.getModel().isRunnable(elementID);
   boolean isTestable  = studio.getModel().isTestable(elementID);
   org.openl.syntax.ISyntaxError[] se = studio.getModel().getErrors(elementID);
   
%>


<f:view>
<html>

<head><title><%=text%></title>
<link href="../css/style1.css" rel="stylesheet" type="text/css" />


<script type="text/javascript">
function open_win(url)
{
   window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}
</script>

</head>

<!--  added by Kazimirski:begin -->
<%-- 
<script language="javascript" type="text/javascript" src="../js/spreadsheet_navigation.js"></script>
--%>


<script language="javascript" type="text/javascript">
var currentRow=0;
var currentColumn=0;

var initialRow=1;
var initialColumn=1;
var lastCell;
var lastColor;
var lastBorderWidth;

var selectColor1 = 'rgb(255,255,0)'; 
var selectColor2 = 'rgb(255,0,0)';

function parse_rgb(s) {
	var i1 = s.indexOf(',');
	var red = s.substring(4,i1);
	var s2 = s.substring(i1+1,s.length);
	var i2 = s2.indexOf(',');
	var green = s2.substring(0,i2);
	var s3 = s2.substring(i2+1,s2.length);
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
	
	if (undefined != lastColor) {
		lastCell.style.backgroundColor = lastColor;
	}
	lastColor = cell.style.backgroundColor;
	cell.style.backgroundColor = selectColor1;

	lastCell = cell;
	
	//document.getElementById('editor_form:value').value = lastCell.elements[0].innerHTML;	
	//document.getElementById('editor_form:value').value = document.getElementById('spreadsheet:0:' + lastCell.title + 'text').innerHTML;
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
}
</script>

<body  onkeydown='javascript:bodyOnKeyUp(event);' onmouseup='bodyOnMouseDown(event);'>
<!--  added by Kazimirski:end -->



<table><tr>
<td>
<img src="../images/excel-workbook.png"/>
<a class="left" href="showLinks.jsp?<%=url%>" target="show_app_hidden" title="<%=uri%>">
      &nbsp;<%=text+ " : " + name%></a>
      
<%
	if (isRunnable && se.length == 0)
	{
	  String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";
%>   

&nbsp;<a href="runMethod.jsp?elementID=<%=elementID%>" title="Run"><img border=0 src="../images/test.gif"/></a>   
&nbsp;<a onClick="open_win('<%=tgtUrl%>', 800, 600)" href="#"  title="Trace"><img border=0 src="../images/trace.gif"/></a>   
 

<%
	}
	

	if (isTestable && se.length == 0)
	{
	  String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";
%>   

&nbsp;<a href="runAllTests.jsp?elementID=<%=elementID%>" title="Test"><img border=0 src="../images/test_ok.gif"/></a>   

<%
	}


%>
</td>
<td>
<%
	String[] menuParamsView = {"transparency", "filterType", "view"}; 
	String parsView = WebTool.listParamsExcept(menuParamsView, request);

	
	String view = request.getParameter("view");
	if (view == null)
	{
		view = studio.getModel().getTableView();
	}
%>

&nbsp;<a class="image2" href="?<%=parsView%>&view=view.business"><img border=0 src="../images/business-view.png" title="Business View"/></a>
&nbsp;<a class="image2" href="?<%=parsView%>&view=view.developer"><img border=0 src="../images/developer-view.png" title="Developer (Full) View"/></a>
</td>
</tr></table>      

<%=studio.getModel().showErrors(elementID)%>


<%-- 
<a href="showTableEditor.jsp?elementID=<%=elementID%>">Edit Table</a>


<div>
&nbsp;<%=studio.getModel().showTable(elementID, view)%>
</div>


<%@include file="showRuns.jsp"%>
--%>

<br /><br /><br />



<a4j:form id="editor_form" reRender="spreadsheet">
	<h:inputHidden id="cell_title" value="#{editorBean.cellTitle}" />
	<a4j:commandButton id="begin_editing" style="visibility:hidden" value="123" action="#{editorBean.beginEditing}" />
</a4j:form>


<br />

<%
FacesContext fc = FacesContext.getCurrentInstance();
TableWriter tw = new TableWriter(elementID,view,studio);
tw.render(out);
%>

<script type="text/javascript" language="javascript">
initialRow = '<%=tw.getInitialRow()%>';
initialColumn = '<%=tw.getInitialColumn()%>';

function beginEditing() {
	if ((null != lastCell) && (undefined != lastCell)) {
		alert(lastCell.title);
		document.getElementById('editor_form:cell_title').value=lastCell.title;
		//document.getElementById('editor_form').submit();
		//alert(document.getElementById('editor_form:current_cell_title').value);
		document.getElementById('editor_form:begin_editing').click();
	}
}
</script>

</body>
</html>
</f:view>