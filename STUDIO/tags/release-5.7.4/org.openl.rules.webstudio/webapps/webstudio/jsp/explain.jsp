<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.meta.*" %>
<%@page import="org.openl.rules.webstudio.util.ExcelLauncher"%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>


<%@page import="org.openl.meta.explanation.ExplanationNumberValue"%><html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>
<link href="webresource/css/tree.css" rel="stylesheet" type="text/css"/>

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

<table class="treeMenu">
<tr>
<td>
<a href="<%=request.getContextPath()%>/faces/facelets/explain/tree.xhtml?rootID=<%=rootID%>" title="Tree View"><img
    src="webresource/images/treeview.gif"></a>
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
   String[] cc = expl.htmlTable(expl.getExplainTree());
%>

<tr>

<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[0]%></td>
<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[1]%></td>
<td style="border-style: solid; border-width: 1;">&nbsp;<%=cc[2]%></td>
</tr>

<tr>
<%
  for(int i = 0; i < expl.getExpandedValues().size(); ++i){
  ExplanationNumberValue<?> dv = (ExplanationNumberValue<?>)expl.getExpandedValues().get(i);
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
	String excelScriptPath = pageContext.getServletContext().getRealPath("scripts/LaunchExcel.vbs");
	
	String wbName = request.getParameter("wbName");
	if (wbName != null)
		ExcelLauncher.launch(excelScriptPath,
		request.getParameter("wbPath"),
		wbName,
		request.getParameter("wsName"),
		request.getParameter("range")

		);

%>
