<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<%
String[] typeButtons = searchBean.getTypeButtons();
for(int i=0; i < typeButtons.length; ++i)
{
  String id = "select" + typeButtons[i];
  String value = request.getParameter(id);
  searchBean.selectType(i, "true".equals(value));
}
%>


<html>
<head>
</head>


<body>

<%
   Object res =  studio.getModel().runSearch(searchBean);

%>
  <%=studio.getModel().displayResult(res)%>
<p>

</body>
</html>