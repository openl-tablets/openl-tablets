<HTML>
<HEAD>

<TITLE>Index Search</TITLE>
    <link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>
</HEAD>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />
<%
    org.openl.rules.ui.ProjectIndexer indexer = studio.getModel().getIndexer();
    boolean hasLetters = (indexer != null) && (indexer.getLetters().length > 0);

    if (hasLetters) {
        String[] letters = studio.getModel().getIndexer().getLetters();
%>
<!-- Index Letters -->
<div style="border-bottom:1px solid;overflow-x:scroll; width: 100%">
<table width="80%">
<%
for(int i = 0; i < letters.length; ++i){
%>
<td><font size=+2>
<a href="#<%=letters[i]%>" font=+2><b>&nbsp;<%=letters[i]%>&nbsp;</b></a>
</font>
</td>
<%
}
%>
</table>
</div>

<!-- Search Results -->
<div style="height:91%; width:100%; overflow-y:scroll">
<%
for(int i = 0; i < letters.length; ++i){
  String[] indexLinks = studio.getModel().getIndexer().getIndexStrings(letters[i]);
%>
<a name="<%=letters[i]%>"><H1><%=letters[i]%>(<%=indexLinks.length%>)</H1></a><p/>

<table width="80%">


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
</div>

<%
    } else {
%>
<BODY>
<P>
There are no available index results.
</BODY>
<%
    }
%>
