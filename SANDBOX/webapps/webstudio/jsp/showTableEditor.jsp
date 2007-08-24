

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id='editor' scope='session' class="org.openl.rules.ui.EditorHelper" />


<%@include file="checkTimeout.jsp"%>


<%
	String s_id = request.getParameter("elementID"); 
   	if (s_id != null)
   	{
     	int elementID = Integer.parseInt(s_id);
     	editor.setTableID(elementID, studio.getModel());
    }
   
%>


<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<link href="../css/style1.css" rel="stylesheet" type="text/css">

<script type="text/javascript">
function open_win(url)
{
   window.open(url,"trace","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}


</script>

</head>

<%@include file="tableEditorForm.jsp"%>

      

	



<div>
&nbsp;<%=editor.showTable()%>
</div>
