<%@ page import = "org.openl.rules.webtools.*" %>
<%@ page import="org.openl.rules.webstudio.web.util.WebStudioUtils" %>
<%@ page import="org.openl.rules.util.net.NetUtils" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<%@include file="../checkTimeout.jspf"%>
<%
  String s_id = request.getParameter("elementID");
     int elementID;
     if (s_id != null)
     {
       elementID = Integer.parseInt(s_id);
       studio.setTableID(elementID);
    }
    else {
         if (request.getParameter("elementURI") != null) {
             int index = studio.getModel().indexForNodeByURI(request.getParameter("elementURI"));
             if (index >= 0) studio.setTableID(index);
         }
      elementID = studio.getTableID();
     }
   String url = studio.getModel().makeXlsUrl(elementID);
   String uri = studio.getModel().getUri(elementID);
   String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);
   String name = studio.getModel().getDisplayNameFull(elementID);
   boolean isRunnable  = studio.getModel().isRunnable(elementID);
   boolean isTestable  = studio.getModel().isTestable(elementID);
   org.openl.syntax.ISyntaxError[] se = studio.getModel().getErrors(elementID);

%>

<jsp:useBean id='editorHelper' scope='session' class="org.openl.rules.ui.EditorHelper" />
<%
   editorHelper.setTableID(elementID, studio.getModel(), request.getParameter("view"), true);
%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title><%=text%></title>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css">
<script type="text/javascript" src="webresource/javascript/prototype/prototype-1.5.1.js"></script>
<script type="text/javascript">
function open_win(url)
{
   window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}

</script>
</head>

<body>
<table><tr>
<td>
<img src="../../images/excel-workbook.png"/>
<a class="left" href="../showLinks.jsp?<%=url%>" target="show_app_hidden" title="<%=uri%>">
      &nbsp;<%=text+ " : " + name%></a>

<%
  if (isRunnable && se.length == 0)
  {
    String tgtUrl = "../../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";
%>

&nbsp;<a href="../runMethod.jsp?elementID=<%=elementID%>" title="Run"><img border=0 src="webresource/images/test.gif" alt="test"/></a>
&nbsp;<a onClick="open_win('<%=tgtUrl%>', 800, 600)" href="#"  title="Trace"><img border=0 src="webresource/images/trace.gif" alt="trace"/></a>
&nbsp;<a href="../benchmarkMethod.jsp?elementID=<%=elementID%>" title="Benchmark"><img border=0 src="webresource/images/clock-icon.png" alt="benchmark"/></a>
<% }
  if (isTestable && se.length == 0) {%>
&nbsp;<a href="../runAllTests.jsp?elementID=<%=elementID%>" title="Test"><img border=0 src="webresource/images/test_ok.gif"/></a>
<%}%>

</td>
<td>
<%
  String[] menuParamsView = {"transparency", "filterType", "view"};
  String parsView = WebTool.listParamsExcept(menuParamsView, request);
%>

&nbsp;<a class="image2" href="?<%=parsView%>&view=view.business"><img border=0 src="webresource/images/business-view.png" title="Business View" alt="Business View"/></a>
&nbsp;<a class="image2" href="?<%=parsView%>&view=view.developer"><img border=0 src="webresource/images/developer-view.png" title="Developer (Full) View" alt="Full View"/></a>
</td>
</tr></table>

<%@include file="/WEB-INF/include/errorDisplay.inc"%>


<%if (studio.getModel().hasErrors(elementID)){%>

<h3>Problems</h3>
<table border="1"><tr><td>
<%=studio.getModel().showErrors(elementID)%>
</td></tr></table>
<p/>
<%}%>

<%if (NetUtils.isLocalRequest(request)||(studio.getCurrentProject(session)!=null && (studio.getCurrentProject(session).isCheckedOut()||studio.getCurrentProject(session).isLocalOnly()))) {%>
<a href="${pageContext.request.contextPath}/jsp/tableeditor/showTableEditor2.jsf?elementID=<%=elementID%>">Edit Table</a>&nbsp;<%}%>

<%if (studio.getCurrentProject(session)!=null && (studio.getCurrentProject(session).isCheckedOut()||studio.getCurrentProject(session).isLocalOnly())) {%>
<a href="${pageContext.request.contextPath}/jsp/copyTable.jsf?elementID=<%=elementID%>">Copy Table</a><%}%>

<%@include file="/WEB-INF/include/contextMenu.inc"%>

<f:view>
    <div>&nbsp;
   <h:outputText value="#{tableViewController.tableView}" escape="false"/>
</div>
</f:view>

<%@include file="../showRuns.jspf"%>
</body>
</html>
