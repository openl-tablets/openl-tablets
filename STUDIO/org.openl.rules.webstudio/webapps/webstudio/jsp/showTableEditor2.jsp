<%@ page import = "org.openl.rules.webtools.*" %>
<%@ page import = "javax.faces.context.FacesContext" %>
<%@ page import = "org.openl.rules.ui.*" %>
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

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title><%=text%></title>
<link href="../css/style1.css" rel="stylesheet" type="text/css">

<script type="text/javascript">
function open_win(url)
{
   window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}
</script>

</head>

<!--  added by Kazimirski:begin -->
<script language="javascript" type="text/javascript" src="../js/spreadsheet_navigation.js"></script>
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

<!--  added by Kazimirski -->
<br /><br /><br />

 
<script type="text/javascript">
function beforeSubmit() {
	alert('1');
	if ((null != lastCell) && (undefined != lastCell)) {
		alert('2');
		var pos = extractPosition(lastCell.title);
		document.getElementById('editor_form:row').value=pos[0];
		document.getElementById('editor_form:column').value=pos[1];
		document.getElementById('editor_form:element_id').value='<%=elementID%>';
	} else {
		alert('3');
		return false;	
	}
}
</script>


 
<a4j:form id="editor_form" reRender="spreadsheet">
	<h:inputHidden id="row" value="#{editorBean.row}" />
	<h:inputHidden id="column" value="#{editorBean.column}" />
	<h:inputHidden id="element_id" value="#{editorBean.elementID}" />
	<h:inputText   id="value" value="#{editorBean.value}" />
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<a4j:commandButton id="save" onclick="javascript:beforeSubmit();" action="#{editorBean.updateCellValue}" value="Update cell value" />
	<a4j:commandButton id="add_row_before" style="visibility:hidden" action="#{editorBean.addRowBefore}" />
</a4j:form>



<br /><br /><br />
<%
FacesContext fc = FacesContext.getCurrentInstance();
TableWriter tw = new TableWriter(elementID,view,studio);
tw.render(out);
%>

<script type="text/javascript">
initialRow = '<%=tw.getInitialRow()%>';
initialColumn = '<%=tw.getInitialColumn()%>';
</script>

<br /><br /><br />


<br /><br /><br />



<script type="text/javascript">
function addRowBefore() {
	if ((null != lastCell) && (undefined != lastCell)) {
		var pos = extractPosition(lastCell.title);
		document.getElementById('editor_form:row').value=pos[0];
		document.getElementById('editor_form:column').value=pos[1];
		document.getElementById('editor_form:element_id').value='<%=elementID%>';
		document.getElementById('editor_form:add_row_before').click();
	}	
}
</script>

<input type="button" onclick="javascript:addRowBefore();" value="Add row before" />
<br />
<input type="button" onclick="javascript:addRowAfter();return false;" value="Add row after" />
<br />
<input type="button" onclick="javascript:removeRow();return false;" value="Remove row" />

</body>
</html>

</f:view>