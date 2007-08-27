<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">

<html>
<head>
<title>OpenL Web Studio</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>


<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />

<%
	String mode = request.getParameter("mode");
	if (mode != null)
	  studio.setMode(mode);
	String reload = request.getParameter("reload");
	if (reload != null)
	  studio.reset();
	String selected = request.getParameter("select_wrapper");
		
	studio.select(selected);
	  
%>

<frameset rows="70,*">
<frame src="header.jsp" name="header" scrolling="no"  noresize resize="no" />

<frameset cols="*,80%" framespacing="0" frameborder="1" resize="resize"  scrolling="auto" >
<frameset rows="*,1" framespacing="0"  scrolling="auto" >
    <frame src="tree.jsp" name="leftFrame" scrolling="auto">
    <frame src="html/nothing.html" name="show_app_hidden">
</frameset>  

<frame src="<%=System.getProperty( "org.openl.webstudio.intro.html", "html/ws-intro.html")%>" name="mainFrame" scrolling="auto"/>


</frameset>


</frameset>


<noframes>
<body>
    <p>To view content you need frames capable browser
</body>
</noframes>

</frameset>

</html>
