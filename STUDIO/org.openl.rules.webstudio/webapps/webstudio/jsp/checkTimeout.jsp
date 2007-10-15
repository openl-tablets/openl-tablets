
<% 
	if (studio.getModel().getWrapper() == null)
	{
%>
	<span class="error">
		<h3>There is a serious possibility that while you were 
		absent, your session have expired. <p> Don't worry, <a href="../../index.jsp" target="_top">click here</a> and start again. 
	</span>
<%	
		return;
	}
%>	
