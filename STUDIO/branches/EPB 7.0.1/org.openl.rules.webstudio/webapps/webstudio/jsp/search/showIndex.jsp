<%@include file="header.jsp"%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<%
  String value = request.getParameter("value");
  String searchString = value;
  /*
  //Following code makes the indexing alias sensitive
  SRPAttribute attr = IDATMain.the.findAttributeByName(value);
  SRPEntity entity = IDATMain.the.findEntityByName(value);
  if (attr != null)
  {
  	searchString = "&quot;" + attr.getName()+ "&quot;";
  	String [] aliases = attr.getAliases();
	if (aliases != null)
	{
		for (int m = 0; m < aliases.length; m++)
		{
		  searchString += " &quot;" + aliases[m] + "&quot;";
		}	
	}
  }
  else
  if (entity != null)
  {
  	searchString = "&quot;" + entity.getName()+ "&quot;";
  	String [] aliases = entity.getAliases();
	if (aliases != null)
	{
		for (int m = 0; m < aliases.length; m++)
		{
		  searchString += " &quot;" + aliases[m] + "&quot;";
		}	
	}
  }
  */
  String[][] values = studio.getModel().getIndexer().getResultsForIndex(searchString);
%>


<H2><%=value%></H2><p/>

<%@include file="displayResults.jsp"%>



