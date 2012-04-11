<%@ page import = "org.openl.rules.webstudio.web.util.WebStudioUtils" %>
<%@ page import="org.openl.meta.DoubleValue" %>
<%@ page import="org.openl.rules.ui.Explanation" %>
<%@ page import="org.openl.rules.ui.OpenLWrapperInfo" %>
<%@ page import="org.openl.rules.webtools.ExcelLauncher" %>
<%@ page import= "org.openl.rules.util.net.NetUtils" %>


<html>
<head>
<title>Tree Sample</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="webresource/css/openl/dtree.css"></link>
<link rel="stylesheet" type="text/css" href="webresource/css/openl/style1.css"></link>
<script language="JavaScript" type="text/JavaScript" src="webresource/dtree.js"></script>


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

<a href="index.jsp?reload=true" title="Refresh Project" target="top"><img border=0 src="<%= request.getContextPath()%>/webresource/images/refresh.gif"></a>

<%
  if (studio.getModel().getAllTestMethods() != null && studio.getModel().getAllTestMethods().getTests().length > 0)
  {
%>
<a href="jsp/runAllTests.jsp" target="mainFrame" title="Run All Tests"><img border="0" src="webresource/images/test_ok.gif"/></a>

<%
  }
%>

<%
  if (studio.getModel().getAllValidatedNodes().size() > 0)
  {
%>
<a href="index.jsp?validate=true&reload=true" title="Validate Project" target="top"><img border=0 src="<%= request.getContextPath()%>/webresource/images/validateAll.png"></a>
<%
  }
%>



<%if (!NetUtils.isLocalRequest(request)) {%>
  <%if (studio.getCurrentProject(session)!=null && studio.getCurrentProject(session).isCheckedOut()) {%>
    <a class="actionButton" href="index.jsp?operation=checkIn" target="top" title="Check in propject"><img border="0" src="<%= request.getContextPath()%>/webresource/images/repository/checkin.gif"></a>
  <%}%>
  <%if (studio.getCurrentProject(session)!=null && (!(studio.getCurrentProject(session).isCheckedOut()
          ||studio.getCurrentProject(session).isLocked()
          ||studio.getCurrentProject(session).isLocalOnly()))) {%>
    <a class="actionButton" href="index.jsp?operation=checkOut" target="top" title="Check Out Propject"><img border="0" src="<%= request.getContextPath()%>/webresource/images/repository/checkout.gif"></a>
  <%}%>
<%}%>

</td>

<td align=right >
<a href="javascript: d.openAll(); d.o(0);" title="Expand All"><img border="0" src="webresource/images/expandall.gif"/></a>
<a href="javascript: d.closeAll()" title="Collapse All" > <img border="0" src="webresource/images/collapseall.gif"/></a>

</td>
</tr></table>

<p/>



<div class="errmsg" id="msg">
</div>



<div class="dtree" id="tree">
</div>

<script language="JavaScript" defer="defer">
d = new dTree('d');
<%=studio.getModel().renderTree(request.getContextPath() + "/faces/facelets/tableeditor/showTable.xhtml")%>


document.getElementById('tree').innerHTML = d;
</script>




</body>
</html>