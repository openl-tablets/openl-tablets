<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id='editor' scope='session' class="org.openl.rules.ui.EditorHelper" />


<%@include file="checkTimeout.jspf"%>


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
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css">

<script type="text/javascript">
function open_win(url)
{
   window.open(url,"_blank","toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=900, height=700, top=20, left=100")
}


</script>

</head>


<body>
<h2>Copying...</h2>

<img src="../images/excel-workbook.png"/>
<a class="left" href="showLinks.jsp?<%=url%>" target="show_app_hidden" title="<%=uri%>">
      &nbsp;<%=text+ " : " + name%></a>



<p>

<h2>To</h2>


<form action="">
<table width="80%">
<tr><td>
<fieldset>
<legend>
Workbook
</legend>

<input type="radio"  name="Workbook" value="new"/>
New

<input id="textCell" type="text" size="10" name="newwb" value="Rules2.xls"/>

 
<input type="radio" checked="checked" name="Workbook" value="existing"/>
Existing

<select name="wb">
  <option value="1" selected="selected">UWDemo.xls</option>
</select>


</fieldset>
</td></tr>

<tr><td>
<fieldset>
<legend>
Worksheet
</legend>

<input type="radio" checked="checked" name="Worksheet" value="new"/>
New

<input id="textCell" type="text" size="10" name="newws" value="Sheet X"/>

 
<input type="radio" name="Worksheet" value="existing"/>
Existing

<select name="ws">
  <option value="1">Definitions</option>
  <option value="2" selected="selected">Rules</option>
  <option value="3" >Documentation</option>
  <option value="">Vocabulary</option>
</select>


</fieldset>
</td></tr>
</table>

</form>




</body>

</html>



