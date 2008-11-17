<%@ page import = "org.openl.rules.webtools.*" %>
<%@ page import = "org.openl.meta.*" %>
<%@ page import="org.openl.rules.ui.Explanation" %>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>


<html>
<head>
<link rel="stylesheet" type="text/css" href="webresource/css/openl/style1.css"></link>

<% 
	if (studio.getModel().getWrapper() == null)
	{
%>
	<span class="error">
		<h3>There is a serious possibility that while you were 
		absent, your session have expired. <p> 
		Don't worry, <a href="../index.jsp" target="#" onClick="window.close()">click here</a> and start again. 
	</span>
<%	
		return;
	}
%>	



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


<p/>
<a href="/webstudio/explaintree.jsp?rootID=<%=rootID%>">Show in Tree</a>
<p/>


<FORM>
<INPUT TYPE='checkbox' name="showNames" <%=checkedShowNames%> onclick="submit()">Show Names in Formula</input>
<INPUT TYPE='checkbox' name="showValues" <%=checkedShowValues%> onclick="submit()">Show Values</input>
<input type="hidden" name="header" value="<%=header%>"/>
<input type="hidden" name="rootID" value="<%=rootID%>"/>
</FORM>



<p/>
<table border=1>
<tr>
<th>Value</th>
<th>Name</th>
<th>Formula</th>



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

<td>&nbsp;<%=cc[0]%></td>
<td>&nbsp;<%=cc[1]%></td>
<td>&nbsp;<%=cc[2]%></td>

<tr>
<%
  for(int i = 0; i < expl.getExpandedValues().size(); ++i){
  DoubleValue dv = (DoubleValue)expl.getExpandedValues().get(i);
  cc = expl.htmlTable(dv);
%>

<tr>

<td>&nbsp;<%=cc[0]%></td>
<td>&nbsp;<%=cc[1]%></td>
<td>&nbsp;<%=cc[2]%></td>

<%}%>



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