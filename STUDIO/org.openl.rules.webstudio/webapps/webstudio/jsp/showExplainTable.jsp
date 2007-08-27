<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.util.*" %>
<%@ page import = "org.openl.rules.webtools.*" %>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="tracer" scope="session" class="org.openl.rules.ui.TraceHelper"/>


<%@include file="checkTimeoutClose.jsp"%>


<%
	String uri = request.getParameter("uri");
	String text = request.getParameter("text");
	
 
	   
%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title>OpenL Tracing</title>
<link href="../css/style1.css" rel="stylesheet" type="text/css"/>

<style>

table.top{font-size:80%;background:black}


td.menu{background:lightblue}

table.menu
{
font-size:100%;
position:absolute;
visibility:hidden;
}


</style>
<script type="text/javascript">
function showmenu(elmnt)
{
document.getElementById(elmnt).style.visibility="visible"
}
function hidemenu(elmnt)
{
document.getElementById(elmnt).style.visibility="hidden"
}
</script>
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
	TableInfo ti = studio.getModel().getTableInfo(uri);   
 	String view = null;
 	
if (ti != null)
{

%>

<table>
<tr><td>
<img src="../images/excel-workbook.png"/>
<a class="left" href="showLinks.jsp?<%=ti.getUrl()%>" target="show_app_hidden" title="<%=ti.getUri()%>">
      &nbsp;<%=ti.getText()+ " : " + ti.getDisplayName()%> : <%=text%></a>

</td>
<%@include file="tableViewMenu.jsp"%>
 
 </tr>
</table>
</div>
</td></tr></table>



<%}%>

<p/>
	<%=studio.getModel().showTableWithSelection(uri, view)%>

</body>
