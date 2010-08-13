<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.util.*" %>
<%@ page import = "org.openl.commons.web.util.WebTool" %>
<%@ page import="org.openl.rules.lang.xls.syntax.TableSyntaxNode" %>
<%@page import="org.openl.rules.table.ITable"%>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="tracer" scope="session" class="org.openl.rules.ui.TraceHelper"/>

<%
	String uri = request.getParameter("uri");
	String text = request.getParameter("text");
%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title>OpenL Tracing</title>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>
<style type="text/css">

table.top{font-size:80%;background:black}

td.menu{background:lightblue}

table.menu
{
font-size:100%;
position:absolute;
visibility:hidden;
}


</style>

</head>




<body>
<p/> 

<%
	if (uri == null || uri.equals("null"))
	{

%>
	   <p/>
	   <h2>Source View Frame</h2>
	   <p> This frame will show the source table for the values you select using the navigation
	   frame on the left. You will be able to navigate to the original document from here as usual. 
	   If the value is an expression or a formula, you can explore it's content by opening the tree branch 
	   of that expression. 
	   </p>	

<%
		return;
	}
%>


<%
    ITable table = studio.getModel().getTable(uri);
 	String view = null;

    if (table != null) {
%>

<table>
<tr><td>
<img src="../images/excel-workbook.png"/>
<a class="left" href="showLinks.jsp?uri=<%=StringTool.encodeURL(uri)%>" target="show_app_hidden" title="<%=uri%>">
      &nbsp;<%=table.getName()%> : <%=text%></a>

</td>
<%@include file="tableViewMenu.jspf"%>
 
 </tr>
</table>

<%}%>

<p/>
<%=studio.getModel().showTableWithSelection(uri, view)%>

</body>
</html>