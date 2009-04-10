<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.rules.webtools.*" %>
<%@page import="org.openl.rules.webstudio.web.util.Constants"%>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="tracer" scope="session" class="org.openl.rules.ui.TraceHelper"/>

<%

    if (request.getParameter("first") != null)
    {
%>    
      <b>Select  a Trace Element on the left side and you will see it's trace here</b> 
<%
      return;    
    }
 
	String s_id = request.getParameter(Constants.REQUEST_PARAM_ID);

   	int traceElementID = -100;	
   	if (s_id != null)
   	{
     	traceElementID = Integer.parseInt(s_id);
    }

	TableInfo ti = tracer.getTableInfo(traceElementID);

    String uri = tracer.getProjectNodeUri(traceElementID, studio.getModel());
%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title>OpenL Tracing</title>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>
<script type="text/javascript" src="webresource/javascript/prototype/prototype-1.5.1.js"></script>

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
	String view = null;
	
	if (ti != null)
	{
	
%>

<table>
<tr><td>
<img src="../images/excel-workbook.png"/>
<a class="left" href="showLinks.jsp?<%=ti.getUrl()%>" target="show_app_hidden" title="<%=ti.getUri()%>">
      &nbsp;<%=ti.getText()+ " : " + ti.getDisplayName()%></a>
</td>
<%@include file="tableViewMenu.jspf"%>
 </tr>
</table>


<%}%>

<p/>

<%@include file="/WEB-INF/include/contextMenu.inc"%>
<%=tracer.showTrace(traceElementID, studio.getModel(), view)%>

</body>
</html>
