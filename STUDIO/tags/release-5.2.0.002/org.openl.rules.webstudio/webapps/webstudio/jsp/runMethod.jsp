<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>

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
   org.openl.rules.ui.Explanator.setCurrent(explanator);
   String testName = request.getParameter("testName");
   String testID = request.getParameter("testID");
   String testDescr = request.getParameter("testDescr");
	   
%>





<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=windows-1257">
<title><%=text%></title>
<link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>

<%@include file="common.jspf"%>

</head>

<body>


<h3> Results of running <%=name%> <%=testDescr == null ? "" : " ("+testDescr + ")"%> </h3>
<p>  

<%
   Object res =  studio.getModel().runElement(elementID, testName, testID);
//   Object res =  studio.getModel().runElement(elementID);

%>
  <%=studio.getModel().displayResult(res)%>
<p>

</body>
