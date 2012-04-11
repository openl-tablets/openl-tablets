<!--
<%@include file="header.jsp"%>
-->

<head>	
	<%@ page import = "java.net.URLDecoder" %>
	<%@ page import = "java.lang.Character" %>
	<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
	
	 <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link href="webresource/css/openl/style1.css" rel="stylesheet" type="text/css"/>
</head>

<%	
	// There is a problem with decoding. When we call 'Search' on table cell and if
	// the cell contains spaces in string it come to search page with symbols 'Â '.
	// We need to replace it back with space. The problem is in javascript method 
	// triggerSearch(f) in file showTable.xhtml. Try to fix it anther way.
	
	String searchQuery = request.getParameter("searchQuery");
	String remove = Character.toString((char)194)+Character.toString((char)160);
	if(searchQuery.contains(remove)) {
	    searchQuery = searchQuery.replaceAll(remove, " ");	    
	}

	if (searchQuery == null) searchQuery="";
%>


<body>
<form action ="search.jsp">

<!--
<table cellspacing=20 cellpadding=2>
-->
<table>
<tr>
<td rowspan=2>
<img src="webresource/images/openl-search.png">
</td>

<td valign=bottom><a href="allIndexFrame.jsp"><font size=-1>&nbsp;Index</font></a></td>
<td valign=bottom align=center><a href="../../faces/facelets/search/busSearch.xhtml"><font size=-1>Business Search</font></a></td>
<td valign=bottom align=center><a href="../../faces/facelets/search/advSearch.xhtml"><font size=-1>Advanced Search</font></a></td>
<td valign=bottom align=right><a href="../../html/ws-intro.html#search"><font size=-1>Help&nbsp;</font></a></td>
</tr>

<tr>
<td colspan=3 valign=top><input size=50 name="searchQuery"  value="<%=org.openl.util.StringTool.encodeHTMLBody(searchQuery)%>"/></td>
<td valign=top><input type="submit" name="search_button" value="Search"/></td>
</tr>
</table>
</form>
