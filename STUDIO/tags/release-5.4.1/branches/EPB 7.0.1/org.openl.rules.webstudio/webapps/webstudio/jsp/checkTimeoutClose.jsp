
<% 
	if (studio.getModel().getWrapper() == null)
	{
%>
	<span class="error">
		<h3>There is a serious possibility that while you were 
		absent, your session have expired. <p> Please, <a href="javascript: top.close()">close this window</a> and re-load main 
		webstudio page. 
	</span>
<%	
		return;
	}
%>	
