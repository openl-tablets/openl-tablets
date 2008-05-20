<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
<%@ page import = "org.openl.rules.webtools.*" %>

<%
	String title =   request.getParameter("title"); 
	String treejsp = request.getParameter("treejsp"); 
	String mainjsp = request.getParameter("mainjsp");
	String mainframe = request.getParameter("mainframe");
	if (mainframe == null)
	  mainframe = "mainFrame";
	String relwidth = request.getParameter("relwidth");
	if (relwidth == null)
	  relwidth = "70";
	String[] usedParams={"title","treejsp", "mainjsp","mainframe", "relwidth"};  
%>

<html>
<head>
<title><%=title%></title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>


<frameset cols="*,<%=relwidth%>%" framespacing="0" frameborder="1" resize="resize"  scrolling="auto" >
<frameset rows="*,1" framespacing="0"  scrolling="auto" >
    <frame src="<%=treejsp%>?<%=WebTool.listParamsExcept(usedParams, request)%>" name="leftFrame" scrolling="auto">
    <frame src="html/nothing.html" name="show_app_hidden">
</frameset>    
<frame src="<%=mainjsp%>?<%=WebTool.listParamsExcept(usedParams, request)%>" name="<%=mainframe%>" scrolling="auto">
</frameset>



<noframes>
<body>
    <p>To view content you need frames capable browser
</body>
</noframes>

</html>
