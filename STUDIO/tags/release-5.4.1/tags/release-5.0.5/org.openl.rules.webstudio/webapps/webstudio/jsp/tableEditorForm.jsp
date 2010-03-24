<%
	String colStr = request.getParameter("col");
	int col = colStr == null || colStr.length() == 0 ? 0 : Integer.parseInt(colStr);
	String rowStr = request.getParameter("row");
	int row = rowStr == null || rowStr.length() == 0 ? 0 : Integer.parseInt(rowStr);
	String cellStr = request.getParameter("cell");
	String cell = cellStr == null ? "" : cellStr;
%>

<%
	if (request.getParameter("remove") != null)
		editor.getModel().removeRows(1, row);
	else if (request.getParameter("insert") != null)
		editor.getModel().insertRows(1, row);
	else if (request.getParameter("insertC") != null)
		editor.getModel().insertColumns(1, col);
	else if (request.getParameter("removeC") != null)
		editor.getModel().removeColumns(1, col);
	else if (request.getParameter("undo") != null)
		editor.getModel().undo();
	else if (request.getParameter("redo") != null)
		editor.getModel().redo();
	else if (request.getParameter("edit") != null)
		editor.getModel().setCellValue(row, col, cell);
	else if (request.getParameter("save") != null)
		editor.getModel().save();
	else if (request.getParameter("cancel") != null)
		editor.getModel().cancel();
%>


<body onload="clickCell(<%=col%>,<%=row%>, event)">
<fieldset>
<legend>
Command Options:
</legend>
<form>
<input id="colCell" name="col" type="text" size="3" value="<%=col%>"> Column
<input id="textCell" type="text" size="25" name="cell" value="<%=cell%>"> <input type="submit" value="Edit Cell" name="edit"><br/>
<input id="rowCell" name="row" type="text" size="3" value="<%=row%>"> Row
<br/>

<!--
<%if (editor.getModel().canRemoveRows(1)){%>

	<a href="showTableEditor.jsp?remove=yes&col=<%=col%>&row=<%=row%>" title="Delete Row"><img src="../images/editor/delete_row.png" /></a>  

<%}else{%>
<img src="../images/editor/delete_row_dis.png" />
<%}%>
-->
<p>
<input type="image" value="Insert Row" name="insert" src="../images/editor/add_rowD.png"<%=!editor.getModel().canAddRows(1) ? "disabled" : ""%> > 
<input type="image" value="Remove Row" name="remove" src="../images/editor/delete_row.png" <%=!editor.getModel().canRemoveRows(1) ? "disabled" : ""%> > 
&nbsp;
<input type="image" value="Insert Col" name="insertC" src="../images/editor/add_colR.png" <%=!editor.getModel().canAddCols(1) ? "disabled" : ""%> > 
<input type="image" value="Remove Col" name="removeC" src="../images/editor/delete_col.png"<%=!editor.getModel().canRemoveCols(1) ? "disabled" : ""%> > 
&nbsp;

<!--
<input type="submit" value="Remove Row" name="remove" <%=!editor.getModel().canRemoveRows(1) ? "disabled" : ""%> > 
<input type="submit" value="Insert Row" name="insert" <%=!editor.getModel().canAddRows(1) ? "disabled" : ""%> > 
&nbsp;
<input type="submit" value="Remove Col" name="removeC" <%=!editor.getModel().canRemoveCols(1) ? "disabled" : ""%> > 
<input type="submit" value="Insert Col" name="insertC" <%=!editor.getModel().canAddCols(1) ? "disabled" : ""%> > 
<br/>
-->
<input type="submit" value="Undo"  name="undo"  <%=!editor.getModel().hasUndo() ? "disabled" : ""%>> 
<input type="submit" value="Redo" name="redo" <%=!editor.getModel().hasRedo() ? "disabled" : ""%>>
&nbsp;
<input type="submit" value="Save" name="save" <%=!editor.getModel().hasUndo() ? "disabled" : ""%>>
<input type="submit" value="Cancel" name="cancel" <%=!editor.getModel().hasUndo() ? "disabled" : ""%>>

</form>
</fieldset>
