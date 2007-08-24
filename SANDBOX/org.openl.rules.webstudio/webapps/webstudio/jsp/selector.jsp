<%@ page import = "org.openl.rules.ui.*" %>


<html>
<body>

<jsp:useBean id='locator' scope='session' class="org.openl.rules.ui.OpenLProjectLocator" />

<form>
<select name="select_wrapper" onclick="submit()">

<%

	String selected = request.getParameter("select_wrapper");
		
	OpenLWrapperInfo[] wrappers = locator.listOpenLProjects();
	if (selected == null && wrappers.length > 0)
	{
		selected = wrappers[0].getName();
	}
	
	for(int i = 0; i < wrappers.length; ++i)	
	{
	
		if (selected.equals(wrappers[i].getName()))
		{
			session.setAttribute("current_wrapper", wrappers[i]);
		}
%>
<option value="<%=wrappers[i].getName()%>"  <%=selected.equals(wrappers[i].getName())? " selected='selected'" : ""%>  ><%=wrappers[i].getName()%></option>
<%}%>
</select>
</form>

</body>
</html>

