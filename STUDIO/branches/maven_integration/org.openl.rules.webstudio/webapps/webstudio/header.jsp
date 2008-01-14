<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.rules.webstudio.web.util.WebStudioUtils" %>


<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>OpenL Web Studio</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/css/style1.css"></link>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/css/2col_leftNav.css"></link>



</head>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio" />


<body>
<map id="menumap" name="menumap">

<area shape="rect"
coords="49,7,218,58"
alt="OpenL Tablets Project on Sourceforge"
href="http://openl-tablets.sourceforge.net/" target="_new" title="OpenL Tablets Project on Sourceforge"/>

</map>



<div id="container">
  <div id="masthead">
    <div id="search">
<table width="97%" cellspacing="0" cellpadding="1">
<tr><td align="right" style="font-size: 40%;">
    <form target="_top" action="index.jsp">
    <span style="	font-size:300%;font-weight: bold;color:#8B4513;">Select OpenL Module:</span>
<select name="select_wrapper" onchange="submit()">
<%

  String selected = request.getParameter("select_wrapper");

  OpenLWrapperInfo[] wrappers = studio.getWrappers();

//	studio.select(selected);
  for(int i = 0; i < wrappers.length; ++i)
  {
     boolean current = studio.getCurrentWrapper() == wrappers[i];
     if (current)
       selected = wrappers[i].getWrapperClassName();

%>
<option value="<%=wrappers[i].getWrapperClassName()%>"  <%=current ? " selected='selected'" : ""%>  ><%=studio.getMode().getDisplayName(wrappers[i])%></option>
<%}%>
</select>
</form>
</td>
</tr>
      <tr>
      <td align="right">
      <table style="border-style: solid; border-width: 1; border-color: black" cellspacing="0" cellpadding="2" >
      <td>
        <a href="index.jsp?mode=business" title="Business View" target="_top"><img border=0 src="<%= request.getContextPath()%>/images/business-view.png"></a>
        &nbsp;
        <a href="index.jsp?mode=developer" title="Developer View" target="_top"><img border=0 src="<%= request.getContextPath()%>/images/developer-view.png"></a>
      &nbsp;&nbsp;

<%if (WebStudioUtils.isLocalRequest(request)) {%>
        <a target="mainFrame" href="<%= request.getContextPath()%>/jsp/uploadProjects.jsf" title="Upload projects to repository"><img border=0 src="<%= request.getContextPath()%>/images/repository/upload.gif"></a>
        &nbsp;
<%}%>

        <a target="mainFrame" href="<%= request.getContextPath()%>/jsp/search/search.jsp?searchQuery=&quot;openl tablets&quot;" title="Search"><img border=0 src="<%= request.getContextPath()%>/images/search.png"></a>
        &nbsp;
        <a href="index.jsp?reload=true&select_wrapper=<%=selected%>" title="Refresh Project(s)" target="_top"><img border=0 src="<%= request.getContextPath()%>/images/refresh.gif"></a>
        &nbsp;
        <a target="mainFrame" href="<%= request.getContextPath()%>/html/ws-intro.html" title="Help"><img border=0 src="<%= request.getContextPath()%>/images/help.gif"></a>
                &nbsp;
        <a target="_top" href="<%= request.getContextPath()%>/faces/repository/main.xhtml" title="Rules Repository"><img border=0 src="<%= request.getContextPath()%>/images/repository/storage.gif" alt="repository"></a>
            </td>
      </table>

      </tr>
      </table>
    </div>
    </div>
  </div>
<!-- end masthead -->
</body>
