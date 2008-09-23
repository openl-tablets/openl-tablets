
<%@ page import="javax.faces.context.FacesContext"%>
<%@ page import="org.openl.rules.webstudio.web.tableeditor.*"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://richfaces.ajax4jsf.org/rich" prefix="rich"%>
<%@ taglib uri="https://ajax4jsf.dev.java.net/ajax" prefix="a4j"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />

<c:if test="(studio.getCurrentProject(session)!=null && (studio.getCurrentProject(session).isCheckedOut()||studio.getCurrentProject(session).isLocalOnly()))">
    <jsp:forward page="showTable.jsp" />
</c:if>

<jsp:useBean id='editorHelper' scope='session' class="org.openl.rules.ui.EditorHelper" />
<%
    boolean switchParam = Boolean.valueOf(request.getParameter("switch"));
    editorHelper.setTableID(Integer.parseInt(request.getParameter("elementID")), studio.getModel(),
           request.getParameter("view"), !switchParam);
%>

<%@include file="../checkTimeout.jspf"%>

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
<link href="../../css/openl/style1.css" rel="stylesheet" type="text/css">
<link href="../../css/openl/tableEditor.css" rel="stylesheet" type="text/css">

 <style type="text/css">
     .editor_invalid {
         background-color: lightcoral;
     }
     .bt_disabled {
         filter: gray() alpha(opacity=30); /* IE */
         opacity: 0.15; /* Safari, Opera and Mozilla */
     }
 </style>

<script type="text/javascript">
function open_win(url) {
window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}
</script>

</head>

<body>
<table>
  <tr>
    <td><img src="../../images/excel-workbook.png" alt=""/> <a class="left" href="${pageContext.request.contextPath}/jsp/showLinks.jsp?<%=url%>" target="show_app_hidden"
      title="<%=uri%>"> &nbsp;<%=text + " : " + name%></a> <%
                 if (isRunnable && se.length == 0) {
                 String tgtUrl = "../../treeview.jsp?title=Trace&treejsp=tracetree.jsp&relwidth=70&mainjsp=jsp/showTraceTable.jsp&elementID="
                         + elementID + "&first=true";
 %> &nbsp;<a href="../runMethod.jsp?elementID=<%=elementID%>" title="Run"><img border=0 src="webresource/images/test.gif" /></a> &nbsp;<a
      onClick="open_win('<%=tgtUrl%>', 800, 600)" href="#" title="Trace"><img border=0 src="webresource/images/trace.gif" /></a> &nbsp;<a
      href="../benchmarkMethod.jsp?elementID=<%=elementID%>" title="Benchmark"><img border=0 src="webresource/images/clock-icon.png" /></a> <%
             }
          if (isTestable && (se==null || se.length == 0)) {
                  %> &nbsp;<a href="../runAllTests.jsp?elementID=<%=elementID%>" title="Test"><img border=0 src="/webresource/images/test_ok.gif" /></a>
        <%}%>
    </td>
    <td>&nbsp;<a class="image2" href="?<%=parsView%>&view=view.business&switch=true"><img border=0 src="webresource/images/business-view.png"
      title="Business View" /></a> &nbsp;<a class="image2" href="?<%=parsView%>&view=view.developer&switch=true"><img border=0
      src="webresource/images/developer-view.png" title="Developer (Full) View" /></a></td>
  </tr>
</table>

<%@include file="/WEB-INF/include/errorDisplay.inc"%>
<%=studio.getModel().showErrors(elementID)%>


<div><f:view>
  <a4j:form id="menu_form">
    <br />
    <br />
    <rich:toolBar itemSeparator="square"><rich:toolBarGroup style="padding: 2px;">
      <h:panelGroup style="display:block;"><h:graphicImage id="save_all" style="vertical-align:bottom;" value="webresource/images/editor/Save.gif" onclick="tableEditor.save()"/></h:panelGroup>
      <h:panelGroup style="display:block;"><h:graphicImage id="validate" value="webresource/images/editor/Validation.gif" /></h:panelGroup>
   </rich:toolBarGroup><rich:toolBarGroup style="padding: 2px;">
     <h:panelGroup style="display:block;"><h:graphicImage id="undo" value="webresource/images/editor/Undo.gif" onclick="tableEditor.undoredo()"/></h:panelGroup>
     <h:panelGroup style="display:block;"><h:graphicImage id="redo" value="webresource/images/editor/Redo.gif" onclick="tableEditor.undoredo(true)"/></h:panelGroup>
   </rich:toolBarGroup><rich:toolBarGroup style="padding: 2px;">
     <rich:dropDownMenu>
     <f:facet name="label">
         <h:panelGrid cellpadding="0" cellspacing="0" columns="3" style="vertical-align:middle">
             <h:graphicImage value="webresource/images/editor/b_row_ins.gif" />
             <rich:spacer width="5" height="5" title=""/>
             <h:outputText value="Rows" style="font-weight:bold;"/>
         </h:panelGrid>
     </f:facet>
       <rich:menuItem submitMode="none"  id="add_row_before_button" value="Add row" onclick="tableEditor.doRowOperation(TableEditor.Constants.ADD_BEFORE)" >
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem  submitMode="none" id="remove_row_button" onclick="tableEditor.doRowOperation(TableEditor.Constants.REMOVE)"  value="Remove row" >
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/row_del.gif" /></f:facet>
       </rich:menuItem>
        <rich:menuItem submitMode="none"  id="move_row_down_button" value="Move row down" onclick="tableEditor.doRowOperation(TableEditor.Constants.MOVE_DOWN)" >
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem submitMode="none"  id="move_row_up_button" value="Move row up" onclick="tableEditor.doRowOperation(TableEditor.Constants.MOVE_UP)" >
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
     </rich:dropDownMenu></rich:toolBarGroup><rich:toolBarGroup style="padding: 2px;">
     <rich:dropDownMenu>
     <f:facet name="label">
         <h:panelGrid cellpadding="0" cellspacing="0" columns="3" style="vertical-align:middle">
             <h:graphicImage value="webresource/images/editor/b_col_ins.gif"/>
             <rich:spacer width="5" height="5" title=""/>
             <h:outputText value="Columns" style="font-weight:bold;"/>
         </h:panelGrid>
     </f:facet>
       <rich:menuItem submitMode="none" id="add_column_before_button" value="Add column" onclick="tableEditor.doColOperation(TableEditor.Constants.ADD_BEFORE)">
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/b_col_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem  submitMode="none" id="remove_column_button" value="Remove column" onclick="tableEditor.doColOperation(TableEditor.Constants.REMOVE)">
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/col_del.gif" /></f:facet>
       </rich:menuItem>
        <rich:menuItem submitMode="none"  id="move_column_right_button" value="Move column right" onclick="tableEditor.doColOperation(TableEditor.Constants.MOVE_DOWN)" >
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       <rich:menuItem submitMode="none"  id="move_column_left_button" value="Move column left" onclick="tableEditor.doRowOperation(TableEditor.Constants.MOVE_UP)" >
         <f:facet name="icon"><h:graphicImage value="webresource/images/editor/b_row_ins.gif" /></f:facet>
       </rich:menuItem>
       </rich:dropDownMenu></rich:toolBarGroup><rich:toolBarGroup style="padding: 2px;">
        <h:panelGroup style="display:block;"><h:graphicImage id="align_left" value="webresource/images/editor/alLeft.gif" onclick="tableEditor.setAlignment('left')"/></h:panelGroup>
        <h:panelGroup style="display:block;"><h:graphicImage id="align_center" value="webresource/images/editor/alCenter.gif" onclick="tableEditor.setAlignment('center')"/></h:panelGroup>
        <h:panelGroup style="display:block;"><h:graphicImage id="align_right" value="webresource/images/editor/alRight.gif" onclick="tableEditor.setAlignment('right')"/></h:panelGroup>
       </rich:toolBarGroup><rich:toolBarGroup location="right"><h:panelGroup>
        <f:verbatim><a href="webresource/docs/editor.html" title="editor help"><img style="border:0;" src="webresource/images/help.gif" alt="help"></a></f:verbatim>
    </h:panelGroup></rich:toolBarGroup>
   </rich:toolBar>
  </a4j:form>
  <br />

<script type="text/javascript" src="webresource/javascript/prototype/prototype-1.5.1.js"></script>
<script type="text/javascript">var jsPath = 'webresource/javascript/';</script>
<script type="text/javascript" src="webresource/javascript/studio.js"></script>
<script type="text/javascript">
    var im = new IconManager('dr-menu-label dr-menu-label-unselect rich-ddmenu-label rich-ddmenu-label-unselect',
          'dr-menu-label dr-menu-label-select rich-ddmenu-label rich-ddmenu-label-select', 'bt_disabled');

function setEnabled(who) {im.enable("menu_form:" + who)}
function setDisabled(who) {im.disable("menu_form:" + who)}

function disableMenu(which){
    var v = $("menu_form:" + which);

    v.className = "dr-menu-item dr-menu-item-disabled rich-menu-item rich-menu-item-disabled";
    v.down("span").className = "dr-menu-icon rich-menu-item-icon dr-menu-icon-disabled rich-menu-item-icon-disabled";
    v.down("span", 1).className = "rich-menu-item-label rich-menu-item-label-disabled";
    v.down("img").style.visibility = "hidden";

    v._mouseover = v.onmouseover;
    v._mouseout = v.onmouseout;
    v.onmouseover = v.onmouseout = Prototype.emptyFunction;

    v._onclick = v.onclick;
    v.onclick = function(event) { Event.stop(event || window.event)}
}

function enableMenu(which){
    var v = $("menu_form:" + which);

    v.className = "dr-menu-item dr-menu-item-enabled rich-menu-item rich-menu-item-enabled";
    v.down("span").className = "dr-menu-icon rich-menu-item-icon dr-menu-icon-enabled rich-menu-item-icon-enabled";
    v.down("span", 1).className = "rich-menu-item-label rich-menu-item-label-enabled";
    v.down("img").style.visibility = "";

    v.onmouseover = v._mouseover;
    v.onmouseout = v._mouseout;
    v.onclick = v._onclick;
}

    var align_buttons = ["align_left", "align_center", "align_right"];
    ["save_all", "validate", "undo", "redo", align_buttons].flatten().each(function(who) {im.init("menu_form:" + who)});

    ["move_row_down_button", "move_row_up_button", "move_column_right_button", "move_column_left_button"].each(disableMenu);
    var menu_items = ["add_row_before_button", "remove_row_button", "add_column_before_button", "remove_column_button"];
    menu_items.each(disableMenu);
</script>

  <script type="text/javascript" src="webresource/javascript/TableEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/BaseEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/TextEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/DropdownEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/SuggestEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/MultiLineEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/DateEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/PriceEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/NumericEditor.js"></script>
  <script type="text/javascript" src="webresource/javascript/MultipleChoiceEditor.js"></script>

  <div id="tableEditor"/>

  <script type="text/javascript">
      var tableEditor = new TableEditor("tableEditor", "${pageContext.request.contextPath}/faces/ajax/", "<%=elementID%>", "<%=switchParam ? "" : request.getParameter("cell")%>");
      tableEditor.undoStateUpdated = function(hasItems) {
        ["save_all","undo"].each(hasItems?setEnabled:setDisabled);
        if (hasItems) {
          window.onbeforeunload = function() { return "Your changes have not been saved."; }
        } else {
          window.onbeforeunload = function() {}
        }
      }
      tableEditor.redoStateUpdated = function(hasItems) {(hasItems?setEnabled:setDisabled)("redo")}
      tableEditor.isSelectedUpdated = function(selected) {
          align_buttons.each(selected?setEnabled:setDisabled);
          menu_items.each(selected?enableMenu:disableMenu);
      }
  </script>
</f:view>
</div>

</body>
</html>