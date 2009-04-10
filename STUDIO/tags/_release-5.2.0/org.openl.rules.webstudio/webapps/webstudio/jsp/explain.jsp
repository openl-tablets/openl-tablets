<%@ page import = "org.openl.rules.webtools.*" %>
<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.meta.*" %>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>


<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="webresource/css/openl/dtree.css"></link>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>
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

<%
String header = request.getParameter("header");

String rootID = request.getParameter("rootID");
String showNames = request.getParameter("showNames");
String showValues = request.getParameter("showValues");
String checkedShowNames = showNames == null? "" : "checked" ;
String checkedShowValues = showValues == null? "" : "checked" ;

if (header != null)
{
%>
<title>
<%=header%>
</title>

<%}%>
</head>

<body>
<p/><p/><p/>

<FORM>

<table width=95% cellpadding=0 cellspacing=0 style="border-style: solid; border-width: 1;">
<tr><td>
<a href="../explaintree.jsp?rootID=<%=rootID%>" title="Tree View"><img border=0 src="webresource/images/treeview.gif"></a>
<a href="javascript: top.close()" title="Close Window"><img border=0 src="webresource/images/close.gif"></a>

</td>
<td align=right>
<INPUT TYPE='checkbox' name="showNames" <%=checkedShowNames%> onclick="submit()">Names</input>
<INPUT TYPE='checkbox' name="showValues" <%=checkedShowValues%> onclick="submit()">Values</input>
</td></tr></table>
<input type="hidden" name="header" value="<%=header%>"/>
<input type="hidden" name="rootID" value="<%=rootID%>"/>
</FORM>



<p/>
<table>
<tr>
<th>Value</th>
<th>Name</th>
<th>Formula</th>

</tr>

<%
   Explanation expl = explanator.getExplanation(rootID);
   expl.setShowNamesInFormula(showNames != null);
   expl.setShowValuesInFormula(showValues != null);
   expl.setHeader(header);
   String expandID = request.getParameter("expandID");
   if (expandID != null)
   		expl.expand(expandID);
   String[] cc = expl.htmlTable(expl.getRoot());
%>

<tr>

<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[0]%></td>
<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[1]%></td>
<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[2]%></td>
</tr>

<tr>
<%
  for(int i = 0; i < expl.getExpandedValues().size(); ++i){
  DoubleValue dv = (DoubleValue)expl.getExpandedValues().get(i);
  cc = expl.htmlTable(dv);
%>

<tr>

<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[0]%></td>
<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[1]%></td>
<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[2]%></td>

</tr>
<%}%>

</table>

</body>
</html>

<%
	String wbName = request.getParameter("wbName");
	if (wbName != null)
		ExcelLauncher.launch(
		"LaunchExcel.vbs",
		request.getParameter("wbPath"),
		wbName,
		request.getParameter("wsName"),
		request.getParameter("range")

		);

%>
