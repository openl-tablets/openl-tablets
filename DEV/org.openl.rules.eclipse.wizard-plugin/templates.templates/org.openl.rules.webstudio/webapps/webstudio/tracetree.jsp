<%@ page import = "org.openl.rules.ui.*" %>


<html>
<head>
<title>Trace Tree</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="css/dtree.css"></link>
<link rel="stylesheet" type="text/css" href="css/style1.css"></link>
<script language="JavaScript" type="text/JavaScript" src="dtree.js"></script>


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
<jsp:useBean id="tracer" scope="session" class="org.openl.rules.ui.TraceHelper"/>


<%@include file="jsp/checkTimeout.jsp"%>


<% 
	String s_id = request.getParameter("elementID"); 
	
   	
   	 	
   	int elementID = -100;	
   	if (s_id != null)
   	{
     	elementID = Integer.parseInt(s_id);
     	studio.setTableID(elementID);
	   	String url = studio.getModel().makeXlsUrl(elementID);
	   	String uri = studio.getModel().getUri(elementID);
	   	String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);   
//	   	String name = studio.getModel().getDisplayNameFull(elementID);
//	   	tracer.setName(name);
	   	org.openl.vm.Tracer t =  studio.getModel().traceElement(elementID);
	   	tracer.setRoot(t.getRoot());
    }
    else
    {
%>
	<h1>elementID not found</h1>
<%    	
    }
%>

<br/>

<p/><p/>

<table width=95% style="border-style: solid; border-width: 1;">
<tr>
<td> 
<a href="javascript: top.close()" title="Close Window"><img border=0 src="images/close.gif"></a>
</td>

<td align=right >
<a href="javascript: d.openAll(); d.o(0);" title="Expand All"><img border="0" src="images/expandall.gif"/></a>

<a href="javascript: d.closeAll()" title="Collapse All" > <img border="0" src="images/collapseall.gif"/></a>

</td>
</tr></table>

<p/><p/>



<div class="errmsg" id="msg">
</div>



<div class="dtree" id="tree">
</div>





<script language="JavaScript" defer="defer">
d = new dTree('d');

<%=tracer.renderTraceTree("jsp/showTraceTable.jsp", "mainFrame")%>


document.all['tree'].innerHTML = d;
</script>              




</body>
</html>