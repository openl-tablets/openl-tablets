<%@include file="header.jsp"%>
<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />

<%
	String[] letters = studio.getModel().getIndexer().getLetters(); 
%>


<html>
<head>
</head>
<body>

<%
for(int i = 0; i < letters.length; ++i){
  String[] indexLinks = studio.getModel().getIndexer().getIndexStrings(letters[i]);
%>
<a name="<%=letters[i]%>"><H1><%=letters[i]%>(<%=indexLinks.length%>)</H1></a><p/>

<table width = 80%>


<%

int ncol = 3;
int width = 100/ncol;
int nrows = (indexLinks.length+ncol-1)/ncol;

for(int j = 0; j < nrows; ++j){
%>
<tr>
<%
  for(int k = 0; k < ncol; ++k){
    if (k * nrows + j < indexLinks.length){
%>

<td width=<%=width%>%>
<%=indexLinks[k * nrows + j]%><p/>
</td>
<%}}%>

</tr>
<%}%>
</table>



<%
}
%>

</body>
</html>