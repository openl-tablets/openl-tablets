<%@ page import = "org.openl.rules.webstudio.web.jsf.util.Util" %>
<%@ page import="org.openl.meta.DoubleValue" %>
<%@ page import="org.openl.rules.ui.Explanation" %>
<%@ page import="org.openl.rules.ui.OpenLWrapperInfo" %>
<%@ page import="org.openl.rules.webtools.ExcelLauncher" %>


<html>
<head>
<title>Tree Sample</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="css/dtree.css"></link>
<link rel="stylesheet" type="text/css" href="css/style1.css"></link>
<script language="JavaScript" type="text/JavaScript" src="dtree.js"></script>


<style type="text/css">

BODY {
	padding: 0;
	margin: 0 0 0 10px;
	background: <%=System.getProperty( "org.openl.webstudio.tree.bcgr", "#eceef8")%>;
	}

	
#tree {
	text-overflow: ellipsis; 
	overflow : hidden
	width: 100%;
	height: 100%;
	}

</style>
</head>
<body>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<p/><p/>

<table width=95% style="border-style: solid; border-width: 1;">
<tr>
<td>
<%
	if (studio.getModel().getAllTestMethods() != null && studio.getModel().getAllTestMethods().getTests().length > 0)
	{
%>
<a href="jsp/runAllTests.jsp" target="mainFrame" title="Run All Tests"><img border="0" src="images/test_ok.gif"/></a>

<%
	}
%>
<a href="index.jsp?reload=true" title="Refresh Project" target="_top"><img border=0 src="<%= request.getContextPath()%>/images/refresh.gif"></a>

<%if (Util.isLocalRequest(request)) {%>
    <a href="jsp/uploadProjects.jsf" target="mainFrame" title="Upload projects to repository"><img border=0 alt="upload" src="<%= request.getContextPath()%>/images/upload.gif"></a>
<%}%>

</td>

<td align=right >
<a href="javascript: d.openAll(); d.o(0);" title="Expand All"><img border="0" src="images/expandall.gif"/></a>
<a href="javascript: d.closeAll()" title="Collapse All" > <img border="0" src="images/collapseall.gif"/></a>

</td>
</tr></table>

<p/>



<div class="errmsg" id="msg">
</div>



<div class="dtree" id="tree">
</div>


<script language="JavaScript" defer="defer">
d = new dTree('d');

<%=studio.getModel().renderTree("jsp/tableeditor/showTable.jsf")%>


document.getElementById('tree').innerHTML = d;
</script>              




</body>
</html>