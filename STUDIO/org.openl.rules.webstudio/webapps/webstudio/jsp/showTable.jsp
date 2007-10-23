<%@ page import = "org.openl.rules.webtools.*" %>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<%@include file="checkTimeout.jsp"%>


<%
  String s_id = request.getParameter("elementID");
     int elementID = -100;
     if (s_id != null)
     {
       elementID = Integer.parseInt(s_id);
       studio.setTableID(elementID);
    }
    else
      elementID = studio.getTableID();
   String url = studio.getModel().makeXlsUrl(elementID);
   String uri = studio.getModel().getUri(elementID);
   String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);
   String name = studio.getModel().getDisplayNameFull(elementID);
   boolean isRunnable  = studio.getModel().isRunnable(elementID);
   boolean isTestable  = studio.getModel().isTestable(elementID);
   org.openl.syntax.ISyntaxError[] se = studio.getModel().getErrors(elementID);

%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title><%=text%></title>
<link href="../css/style1.css" rel="stylesheet" type="text/css">

<script type="text/javascript">
function clickCell(x,y,event) {}
function open_win(url)
{
   window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}


</script>

</head>

<table><tr>
<td>
<img src="../images/excel-workbook.png"/>
<a class="left" href="showLinks.jsp?<%=url%>" target="show_app_hidden" title="<%=uri%>">
      &nbsp;<%=text+ " : " + name%></a>

<%
  if (isRunnable && se.length == 0)
  {
    String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";
%>

&nbsp;<a href="runMethod.jsp?elementID=<%=elementID%>" title="Run"><img border=0 src="../images/test.gif"/></a>
&nbsp;<a onClick="open_win('<%=tgtUrl%>', 800, 600)" href="#"  title="Trace"><img border=0 src="../images/trace.gif"/></a>
&nbsp;<a href="benchmarkMethod.jsp?elementID=<%=elementID%>" title="Benchmark"><img border=0 src="../images/clock-icon.png"/></a>


<%
  }


  if (isTestable && se.length == 0)
  {
    String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";
%>

&nbsp;<a href="runAllTests.jsp?elementID=<%=elementID%>" title="Test"><img border=0 src="../images/test_ok.gif"/></a>

<%
  }


%>
</td>
<td>
<%
  String[] menuParamsView = {"transparency", "filterType", "view"};
  String parsView = WebTool.listParamsExcept(menuParamsView, request);

    String view = studio.getModel().getTableView(request.getParameter("view"));
%>

&nbsp;<a class="image2" href="?<%=parsView%>&view=view.business"><img border=0 src="../images/business-view.png" title="Business View"/></a>
&nbsp;<a class="image2" href="?<%=parsView%>&view=view.developer"><img border=0 src="../images/developer-view.png" title="Developer (Full) View"/></a>
</td>
</tr></table>

<%=studio.getModel().showErrors(elementID)%>


<%--
<a href="showTableEditor.jsp?elementID=<%=elementID%>">Edit Table</a>
--%>
<a href="${pageContext.request.contextPath}/jsp/tableeditor/showTableEditor2.jsf?elementID=<%=elementID%>">Edit Table</a>
&nbsp;<a href="copyTable.jsp?elementID=<%=elementID%>">Copy Table</a>


<div>
&nbsp;<%=studio.getModel().showTable(elementID, view)%>
</div>

<%@include file="showRuns.jsp"%>

</body>
