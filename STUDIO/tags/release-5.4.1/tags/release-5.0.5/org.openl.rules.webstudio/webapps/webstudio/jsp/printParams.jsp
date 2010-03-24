
<p/>
<%
	java.util.Enumeration en = request.getParameterNames();
	for(;en.hasMoreElements();)
	{
		String name = (String)en.nextElement();
		String value = request.getParameter(name);
%>
	<%=name%> = <%=value%>
	<br/>

<%		
	}
%>

<p/>