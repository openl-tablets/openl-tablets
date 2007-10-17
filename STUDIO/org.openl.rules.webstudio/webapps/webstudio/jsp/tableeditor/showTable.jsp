<%@ page import = "org.openl.rules.webtools.*" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<%@include file="../checkTimeout.jsp"%>
<%
  String s_id = request.getParameter("elementID");
     int elementID;
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

<jsp:useBean id='editorHelper' scope='session' class="org.openl.rules.ui.EditorHelper" />
<%
   editorHelper.setTableID(Integer.parseInt(request.getParameter("elementID")), studio.getModel(),
           request.getParameter("view"), true);
%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title><%=text%></title>
<link href="../../css/style1.css" rel="stylesheet" type="text/css">
<script type="text/javascript" src="../../javascript/prototype/prototype-1.5.1.js"></script>
<style type="text/css">
    .menuholderdiv {
        background-color: white;
        border: 1px dotted black;
        padding:4px;
    }

    .menuholderdiv a {
        text-decoration: underline;
        color: green;
        font-size:larger;
    }

</style>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/popup/popupmenu.js"></script>
<script type="text/javascript">
function open_win(url)
{
   window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}

    function cellMouseOver(td, event) {
        PopupMenu.sheduleShowMenu('contextMenu', event, 700);
    }

    function cellMouseOut(td) {
        PopupMenu.cancelShowMenu();
    }
    function triggerEdit(f) {
        var uri = $(PopupMenu.lastTarget).down('input').value;
        f.cell.value = uri.toQueryParams().cell;
        f.submit();
    }
    function triggerEditXls(f) {
        f.uri.value = $(PopupMenu.lastTarget).down('input').value;
        f.submit();
    }
    function triggerSearch(f) {
        f.searchQuery.value = $A($(PopupMenu.lastTarget).childNodes).find(function(s) {return s.nodeName == "#text"})
                .nodeValue.sub(/^[.;,! \t\n()^&*%=?\-'"+<>]+/, "").split(/[.;,! \t\n()^&*%=?\-'"+<>]+/, 3)
                .reject(function(s) {return !s}).join(" ");
        f.submit();
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
    String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID=" +elementID + "&first=true";
%>

&nbsp;<a href="runMethod.jsp?elementID=<%=elementID%>" title="Run"><img border=0 src="../../images/test.gif" alt="test"/></a>
&nbsp;<a onClick="open_win('<%=tgtUrl%>', 800, 600)" href="#"  title="Trace"><img border=0 src="../../images/trace.gif" alt="trace"/></a>
&nbsp;<a href="benchmarkMethod.jsp?elementID=<%=elementID%>" title="Benchmark"><img border=0 src="../../images/clock-icon.png" alt="benchmark"/></a>
<% }
  if (isTestable && se.length == 0) {%>
&nbsp;<a href="runAllTests.jsp?elementID=<%=elementID%>" title="Test"><img border=0 src="../../images/test_ok.gif"/></a>
<%}%>

</td>
<td>
<%
  String[] menuParamsView = {"transparency", "filterType", "view"};
  String parsView = WebTool.listParamsExcept(menuParamsView, request);
%>

&nbsp;<a class="image2" href="?<%=parsView%>&view=view.business"><img border=0 src="../../images/business-view.png" title="Business View" alt="Business View"/></a>
&nbsp;<a class="image2" href="?<%=parsView%>&view=view.developer"><img border=0 src="../../images/developer-view.png" title="Developer (Full) View" alt="Full View"/></a>
</td>
</tr></table>

<%=studio.getModel().showErrors(elementID)%>

<a href="${pageContext.request.contextPath}/jsp/tableeditor/showTableEditor2.jsf?elementID=<%=elementID%>">Edit Table</a>
&nbsp;<a href="${pageContext.request.contextPath}/jsp/copyTable.jsp?elementID=<%=elementID%>">Copy Table</a>


<f:view>
<div>&nbsp;
   <h:outputText value="#{tableViewController.tableView}" escape="false"/>
</div>

<form name="editForm" action="showTableEditor2.jsf">
    <input type="hidden" name="elementID" value="<%=elementID%>">
    <input type="hidden" name="cell" value="">
    <h:inputHidden id="view" value="#{tableViewController.mode}" />
</form>
</f:view>    
<form name="editFormXls" action="../showLinks.jsp" target="show_app_hidden">
    <input type="hidden" name="uri" value="">
</form>
<form name="searchForm" action="../search/search.jsp">
    <input type="hidden" name="searchQuery" value="">
</form>

<div id="contextMenu" style="display:none;">
    <table cellpadding="1px">
        <tr><td><a href="#" onclick="triggerEdit(document.forms.editForm)">Edit</a></td></tr>
        <tr><td><a href="#" onclick="triggerEditXls(document.forms.editFormXls)">Edit in Excel</a></td></tr>
        <tr><td><a href="#" onclick="triggerSearch(document.forms.searchForm)">Search</a></td></tr>
    </table>
</div>

<%@include file="../showRuns.jsp"%>
</body>
</html>
