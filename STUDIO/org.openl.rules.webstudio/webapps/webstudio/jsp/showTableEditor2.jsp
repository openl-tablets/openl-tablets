<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
    
<%@ page import = "org.openl.rules.webtools.*" %>
<%@ page import = "javax.faces.context.FacesContext" %>
<%@ page import = "org.openl.jsf.*" %>


<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://richfaces.ajax4jsf.org/rich" prefix="rich" %>
<%@ taglib uri="https://ajax4jsf.dev.java.net/ajax" prefix="a4j" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

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
   
	String[] menuParamsView = {"transparency", "filterType", "view"}; 
	String parsView = WebTool.listParamsExcept(menuParamsView, request);
	String view = request.getParameter("view");
	if (view == null)
	{
		view = studio.getModel().getTableView();
	}
   
   FacesContext fc = FacesContext.getCurrentInstance();
   TableWriter tw = new TableWriter(elementID,view,studio);
%>


<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=text%></title>
<link href="../css/style1.css" rel="stylesheet" type="text/css" />
<script language="javascript" type="text/javascript" src="../js/spreadsheet_navigation.js"></script>
<script type="text/javascript">
initialRow = '<%=tw.getInitialRow()%>';
initialColumn = '<%=tw.getInitialColumn()%>';

function open_win(url)
{
   window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}

function beginEditing() {
	//alert('beginEditing');
	if ((null != lastCell) && (undefined != lastCell)) {
		document.getElementById('editor_form:cell_title').value = lastCell.title;
		document.getElementById('editor_form:begin_editing').click();
	} 
}

function stopEditing() {
	
}
</script>
</head>
<body onkeydown='javascript:bodyOnKeyUp(event);' onmouseup='bodyOnMouseDown(event);'>

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


&nbsp;<a class="image2" href="?<%=parsView%>&view=view.business"><img border=0 src="../images/business-view.png" title="Business View"/></a>
&nbsp;<a class="image2" href="?<%=parsView%>&view=view.developer"><img border=0 src="../images/developer-view.png" title="Developer (Full) View"/></a>
</td>
</tr></table>      

<%=studio.getModel().showErrors(elementID)%>


<br />

<f:view>
<%
tw.render(out);
%>
<a4j:form id="editor_form">
<h:inputHidden id="cell_title" value="#{editorBean.cellTitle}" />
<h:inputHidden id="row" value="#{editorBean.row}" />
<h:inputHidden id="row" value="#{editorBean.column}" />

<a4j:commandButton reRender="spreadsheet" id="begin_editing" style="visibility:hidden" action="#{editorBean.beginEditing}" value="click me" />
</a4j:form>
</f:view>

</body>
</html>