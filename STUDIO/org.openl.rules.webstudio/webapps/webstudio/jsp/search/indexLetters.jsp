
<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<%
	String[] letters = studio.getModel().getIndexer().getLetters(); 
%>

<html>
<head>
</head>
<body>
<table width ="80%">
<%
for(int i = 0; i < letters.length; ++i){
%>
<td><font size=+2>
<a href="allIndex.jsp#<%=letters[i]%>" font=+2 target="allIndex"><b>&nbsp;<%=letters[i]%>&nbsp;</b></a>
</font>
</td>
<%
}
%>
</table>


</body>
</html>