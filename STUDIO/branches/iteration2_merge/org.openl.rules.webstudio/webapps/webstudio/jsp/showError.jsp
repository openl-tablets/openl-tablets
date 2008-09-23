<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.rules.webtools.*" %>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<%@include file="checkTimeoutClose.jspf"%>


<%

 
	String s_id = request.getParameter("elementID"); 
   	 	
   	int elementID = -100;	
   	if (s_id != null)
   	{
     	elementID = Integer.parseInt(s_id);
    }
    
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


<p/>

<%@include file="/WEB-INF/include/contextMenu.inc"%>
<%=studio.getModel().showError(elementID)%>

</body>
</html>
