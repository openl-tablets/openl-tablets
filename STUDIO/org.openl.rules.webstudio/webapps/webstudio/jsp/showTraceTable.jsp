<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.rules.webtools.*" %>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="tracer" scope="session" class="org.openl.rules.ui.TraceHelper"/>


<%@include file="checkTimeoutClose.jsp"%>


<%

    if (request.getParameter("first") != null)
    {
%>    
      <b>Select  a Trace Element on the left side and you will see it's trace here</b> 
<%
      return;    
    }
 
	String s_id = request.getParameter("elementID"); 
   	 	
   	int elementID = -100;	
   	if (s_id != null)
   	{
     	elementID = Integer.parseInt(s_id);
    }
    
	TableInfo ti = tracer.getTableInfo(elementID);   
   
	   
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
<%@include file="tableViewMenu.jsp"%>
 </tr>
</table>


<%}%>

<p/>
	<%=tracer.showTrace(elementID, studio.getModel(), view)%>

</body>
</html>
