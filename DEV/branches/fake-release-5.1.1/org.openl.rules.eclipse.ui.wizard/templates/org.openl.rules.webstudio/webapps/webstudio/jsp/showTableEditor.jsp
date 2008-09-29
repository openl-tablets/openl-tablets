

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id='editor' scope='session' class="org.openl.rules.ui.EditorHelper" />


<%@include file="checkTimeout.jspf"%>


<%
	String s_id = request.getParameter("elementID"); 
   	if (s_id != null)
   	{
     	int elementID = Integer.parseInt(s_id);
     	editor.setTableID(elementID, studio.getModel());
    }
   
%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>

<!--
<link href="webresource/css/openl/tableEditor.css" rel="stylesheet" type="text/css"/>

-->

<script type="text/javascript">
function open_win(url)
{
   window.open(url,"trace","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}


</script>


<style type="text/css">
.cellcursor {
 margin:1px;
 padding:1px;
 border:3px solid #666633;
 }

.normalcell {
 }

</style>

<script type="text/javascript">
var activeCell

function clickCell(x,y,event)
{
    cellID = 'c'+x+'x'+y;
	cell = document.getElementById(cellID);
  	cell.className = 'cellcursor';
	if (activeCell)
	   activeCell.className = 'normalcell';
  	activeCell = cell;
	document.getElementById('rowCell').value=y;
	document.getElementById('colCell').value=x;
	
	var content = cell.innerHTML;
 	
	document.getElementById('textCell').value=content
	         .replace(/&nbsp;/g," ")
	         .replace(/&lt;/g,"<")
	         .replace(/&gt;/g,">")
	         .replace(/&amp;/g,"&");
}
</script>

</head>

<%@include file="tableEditorForm.jspf"%>

      

	



<div>
&nbsp;<%=editor.showTable()%>
</div>
