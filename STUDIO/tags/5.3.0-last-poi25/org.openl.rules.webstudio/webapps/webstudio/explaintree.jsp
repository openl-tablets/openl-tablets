<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.meta.*" %>


<html>
<head>
<title>Explain Tree</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="webresource/css/openl/dtree.css"></link>
<link rel="stylesheet" type="text/css" href="webresource/css/openl/style1.css"></link>
<script language="JavaScript" type="text/JavaScript" src="webresource/dtree.js"></script>


<style type="text/css">

BODY {
	padding: 0;
	margin: 0 0 0 10px;
	background: #eceef8;
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
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>

<%
	String header = request.getParameter("header");

	String rootID = request.getParameter("rootID");
	String showNames = request.getParameter("showNames");
	String showValues = request.getParameter("showValues");
	String checkedShowNames = showNames == null? "" : "checked" ;
	String checkedShowValues = showValues == null? "" : "checked" ;

	if (header != null)
	{
	    // TODO check or remove it
%>
<title>
	<%=header%>
</title>

<%}%>


<p/><p/>

<table width=95% style="border-style: solid; border-width: 1;">
<tr>
<td>
<a href="jsp/explain.jsp?rootID=<%=rootID%>" title="Table View"><img border=0 src="webresource/images/tableview.gif"></a>
<a href="javascript: top.close()" title="Close Window"><img border=0 src="images/close.gif"></a>

</td>

<td align=right >
<a href="javascript: d.openAll(); d.o(0);" title="Expand All"><img border="0" src="webresource/images/expandall.gif"/></a>

<a href="javascript: d.closeAll()" title="Collapse All" > <img border="0" src="webresource/images/collapseall.gif"/></a>

</td>
</tr></table>

<p/><p/>



<div class="errmsg" id="msg">
</div>



<div class="dtree" id="tree">
</div>

<script language="JavaScript" defer="defer">
d = new dTree('d');


<%
 	Explanation expl = explanator.getExplanation(rootID);
%>

<%=new ExplainTreeRenderer("jsp/showExplainTable.jsp", "mainFrame").renderRoot(expl.getRoot())%>

document.all['tree'].innerHTML = d;
</script>


</body>
</html>