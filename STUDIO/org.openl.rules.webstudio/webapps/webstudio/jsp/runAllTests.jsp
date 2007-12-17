<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>


<%@include file="checkTimeout.jsp"%>

<% 
   String elementIDstr = request.getParameter("elementID"); 
   int elementID = -100;
   if (elementIDstr != null)
     elementID = Integer.parseInt(elementIDstr);
   	
   org.openl.rules.ui.Explanator.setCurrent(explanator);
   org.openl.rules.ui.AllTestsRunResult atr =  studio.getModel().runAllTests(elementID);
   
  int ntests = atr.getTests().length;
  int ntestsF = atr.numberOfFailedTests();	   
  int nunits = atr.totalNumberOfTestUnits();	   
  int nunitsF = atr.totalNumberOfFailures();	   
	   
%>





<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<link href="../css/style1.css" rel="stylesheet" type="text/css"/>

<%@include file="common.jsp"%>

</head>

<body>
<table>
<tr>
<td>Tests:</td><td align="right"><%=ntests%><%if (ntestsF > 0){ %> <span class="red"> (<%=ntestsF%>)</span> <%}%> </td>
</tr>
<tr>
<td>Units:</td><td align="right"><%=nunits%><%if (nunitsF > 0){ %> <span class="red"> (<%=nunitsF%>)</span> <%}%> </td>
</tr>

</table>

<p>
<%
	for(int i=0; i < ntests; ++i)
	{
	  org.openl.rules.ui.AllTestsRunResult.Test test = atr.getTests()[i]; 
%>
<p>  
<h1> <span class="<%=test.getResult().getNumberOfFailures() > 0 ? "red" : "blue" %>"><%=test.getTestName()%></span> </h1>		
<p>  
  	  
  <%=studio.getModel().displayResult(test.getResult())%>
<%}%>  
<p>

</body>
