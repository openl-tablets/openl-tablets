
<%@page import="org.openl.rules.webstudio.web.util.Constants"%><jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<jsp:useBean id="explanator" scope="session" class="org.openl.rules.ui.Explanator"/>
<%@page import="org.openl.util.StringTool"%>
<% 
    String uri = request.getParameter(Constants.REQUEST_PARAM_URI);
   	if (uri != null && !uri.equals("")) {
     	studio.setTableUri(uri);
    } else {  
       uri = studio.getTableUri();
    }
    String url = studio.getModel().makeXlsUrl(uri);
    String text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);   
    String name = studio.getModel().getDisplayNameFull(uri);
    org.openl.rules.ui.Explanator.setCurrent(explanator);
    String testNameFromRequest = request.getParameter("testName");
    String testName = null;
    if (testNameFromRequest != null) {
        testName = StringTool.decodeURL(testNameFromRequest);    
    }        
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
<%@include file="/WEB-INF/include/errorDisplay.inc"%>

<h3> Results of running <%=name%> <%=testDescr == null ? "" : " ("+testDescr + ")"%> </h3>
<p>  

<%
   Object res =  studio.getModel().runElement(uri, testName, testID);
%>
<%=studio.getModel().displayResult(res)%>
<p>

</body>
