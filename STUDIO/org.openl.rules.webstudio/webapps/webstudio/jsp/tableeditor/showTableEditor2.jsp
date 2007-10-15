
<%@ page import="javax.faces.context.FacesContext"%>
<%@ page import="org.openl.jsf.*"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://richfaces.ajax4jsf.org/rich" prefix="rich"%>
<%@ taglib uri="https://ajax4jsf.dev.java.net/ajax" prefix="a4j"%>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c"%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id='editorHelper' scope='session' class="org.openl.rules.ui.EditorHelper" />
<%
   editorHelper.setTableID(Integer.parseInt(request.getParameter("elementID")), studio.getModel(),
           request.getParameter("view"), !Boolean.valueOf(request.getParameter("switch")));
%>

<%@include file="../checkTimeout.jsp"%>

<%
            FacesContext fc = FacesContext.getCurrentInstance();
            TableWriterBean twb = (TableWriterBean) (fc.getApplication()
                    .getVariableResolver().resolveVariable(fc,
                    "tableWriterBean"));
            int elementID = twb.getElementID();
            String name = twb.getName();
            String text = twb.getTitle();
            org.openl.syntax.ISyntaxError[] se = twb.getSe();
            String url = twb.getUrl();
            String uri = twb.getUri();
            boolean isRunnable = twb.isRunnable();
            boolean isTestable = twb.isTestable();
            String parsView = twb.getParsView();
%>

<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title><%=text%></title>
<link href="../../css/style1.css" rel="stylesheet" type="text/css">
<link href="../../css/tableEditor.css" rel="stylesheet" type="text/css">

 <style type="text/css">
     .editor_invalid {
         background-color: lightcoral;
     }
 </style>

</head>

<body>
<table>
  <tr>
    <td><img src="../../images/excel-workbook.png" alt=""/> <a class="left" href="${pageContext.request.contextPath}/jsp/showLinks.jsp?<%=url%>" target="show_app_hidden"
      title="<%=uri%>"> &nbsp;<%=text + " : " + name%></a> <%
                 if (isRunnable && se.length == 0) {
                 String tgtUrl = "../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID="
                         + elementID + "&first=true";
 %> &nbsp;<a href="runMethod.jsp?elementID=<%=elementID%>" title="Run"><img border=0 src="../../images/test.gif" /></a> &nbsp;<a
      onClick="open_win('<%=tgtUrl%>', 800, 600)" href="#" title="Trace"><img border=0 src="../../images/trace.gif" /></a> &nbsp;<a
      href="benchmarkMethod.jsp?elementID=<%=elementID%>" title="Benchmark"><img border=0 src="../../images/clock-icon.png" /></a> <%
             }
          if (isTestable && (se==null || se.length == 0)) {
                  %> &nbsp;<a href="runAllTests.jsp?elementID=<%=elementID%>" title="Test"><img border=0 src="../../images/test_ok.gif" /></a>
        <%}%>
    </td>
    <td>&nbsp;<a class="image2" href="?<%=parsView%>&view=view.business&switch=true"><img border=0 src="../../images/business-view.png"
      title="Business View" /></a> &nbsp;<a class="image2" href="?<%=parsView%>&view=view.developer&switch=true"><img border=0
      src="../../images/developer-view.png" title="Developer (Full) View" /></a></td>
  </tr>
</table>

<%=studio.getModel().showErrors(elementID)%>


<div><f:view>
  <a4j:form id="menu_form">
    <br />
    <br />
    <rich:toolBar itemSeparator="square"><rich:toolBarGroup>
     <h:graphicImage value="/images/editor/Save.gif" onclick="tableEditor.save()"/>
     <h:graphicImage value="/images/editor/Validation.gif" />
   </rich:toolBarGroup><rich:toolBarGroup>
     <h:graphicImage value="/images/editor/Undo.gif" onclick="tableEditor.undoredo()"/>
     <h:graphicImage value="/images/editor/Redo.gif" onclick="tableEditor.undoredo(true)"/>
   </rich:toolBarGroup><rich:toolBarGroup style="padding: 2px;">
     <rich:dropDownMenu>
     <f:facet name="label">
         <h:panelGrid cellpadding="0" cellspacing="0" columns="3" style="vertical-align:middle">
             <h:graphicImage value="/images/editor/b_row_ins.gif" />
             <rich:spacer width="5" height="5" title=""/>
             <h:outputText value="Rows" style="font-weight:bold;"/>
         </h:panelGrid>
     </f:facet>
       <rich:menuItem submitMode="none"  id="add_row_before_button" value="Add row" onclick="tableEditor.doRowOperation(TableEditor.Constants.ADD_BEFORE)" >
         <f:facet name="icon"><h:graphicImage value="/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem submitMode="none" id="remove_row_button" onclick="tableEditor.doRowOperation(TableEditor.Constants.REMOVE)"  value="Remove row" >
         <f:facet name="icon"><h:graphicImage value="/images/editor/row_del.gif" /></f:facet>
       </rich:menuItem>
        <rich:menuItem submitMode="none"  id="move_row_down_button" value="Move row down" onclick="tableEditor.doRowOperation(TableEditor.Constants.MOVE_DOWN)" >
         <f:facet name="icon"><h:graphicImage value="/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem submitMode="none"  id="move_row_up_button" value="Move row up" onclick="tableEditor.doRowOperation(TableEditor.Constants.MOVE_UP)" >
         <f:facet name="icon"><h:graphicImage value="/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
     </rich:dropDownMenu></rich:toolBarGroup><rich:toolBarGroup style="padding: 2px;">
     <rich:dropDownMenu>
     <f:facet name="label">
         <h:panelGrid cellpadding="0" cellspacing="0" columns="3" style="vertical-align:middle">
             <h:graphicImage value="/images/editor/b_col_ins.gif"/>
             <rich:spacer width="5" height="5" title=""/>
             <h:outputText value="Columns" style="font-weight:bold;"/>
         </h:panelGrid>
     </f:facet>
       <rich:menuItem submitMode="none" id="add_column_before_button" value="Add column" onclick="tableEditor.doColOperation(TableEditor.Constants.ADD_BEFORE)">
         <f:facet name="icon"><h:graphicImage value="/images/editor/b_col_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem  submitMode="none" id="remove_column_button" value="Remove column" onclick="tableEditor.doColOperation(TableEditor.Constants.REMOVE)">
         <f:facet name="icon"><h:graphicImage value="/images/editor/col_del.gif" /></f:facet>
       </rich:menuItem>
        <rich:menuItem submitMode="none"  id="move_column_button_right" value="Move column right" onclick="tableEditor.doColOperation(TableEditor.Constants.MOVE_DOWN)" >
         <f:facet name="icon"><h:graphicImage value="/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem submitMode="none"  id="move_column_button_left" value="Move column left" onclick="tableEditor.doRowOperation(TableEditor.Constants.MOVE_UP)" >
         <f:facet name="icon"><h:graphicImage value="/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       </rich:dropDownMenu></rich:toolBarGroup><rich:toolBarGroup>
          <h:graphicImage value="/images/editor/alLeft.gif" onclick="tableEditor.setAlignment('left')"/>
          <h:graphicImage value="/images/editor/alCenter.gif" onclick="tableEditor.setAlignment('center')"/>
          <h:graphicImage value="/images/editor/alRight.gif" onclick="tableEditor.setAlignment('right')"/>
       </rich:toolBarGroup>
   </rich:toolBar>
  </a4j:form>
  <br />

  <script type="text/javascript">var jsPath = '../../javascript/';</script>

  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/prototype/prototype-1.5.1.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/TableEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/BaseEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/TextEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/DropdownEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/SuggestEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/MultiLineEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/DateEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/PriceEditor.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/NumericEditor.js"></script>

  <div id="tableEditor"/>

  <script type="text/javascript">
    var tableEditor = new TableEditor("tableEditor", "${pageContext.request.contextPath}/faces/ajax/", "<%=elementID%>", "<%=request.getParameter("cell")%>");
  </script>
</f:view>
</div>

</body>
</html>