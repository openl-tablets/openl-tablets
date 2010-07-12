<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.commons.web.util.WebTool" %>
<%@page import="org.openl.rules.webstudio.web.util.Constants"%>
<%@page import="org.openl.util.StringTool"%>


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

    String tracerUri = tracer.getTracerUri(traceElementID);
    String tracerName = tracer.getTracerName(traceElementID);
    String tracerHeader = tracer.getTracerHeader(traceElementID);
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
	String view = null;
	
	if (tracerUri != null)
	{
	
%>

<table>
<tr><td>
<img src="../images/excel-workbook.png"/>
<a class="left" href="showLinks.jsp?uri=<%=StringTool.encodeURL(tracerUri)%>" target="show_app_hidden" title="<%=tracerUri%>">
      &nbsp;<%=tracerHeader + " : " + tracerName%></a>
</td>
<%@include file="tableViewMenu.jspf"%>
 </tr>
</table>


<%}%>

<p/>

<%=tracer.showTrace(traceElementID, studio.getModel(), view)%>

</body>
</html>
