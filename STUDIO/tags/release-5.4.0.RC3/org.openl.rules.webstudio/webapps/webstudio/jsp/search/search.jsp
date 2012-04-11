<%@include file="searchHeader.jsp"%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<%
	String[][] values = {};
	if (searchQuery.length() > 0)
	{
	
		if (studio.getModel() == null || studio.getModel().getIndexer() == null)
		{
%>		
			<p class="warning">There is no valid projects in workspace. Or your session have expired and you need to reload project</p>
			  
<%			
		}
		else
		{
			values = studio.getModel().getIndexer().getResultsForQuery(searchQuery, 200, null);
		}	
    	if (values.length == 0){
      
%>
<H2>No results found</H2><p/>
<%
		}
	}		
%>


<%@include file="displayResults.jspf"%>

