<%@ page import = "org.openl.rules.ui.*" %>
<%@ page import = "org.openl.rules.webstudio.web.util.WebStudioUtils" %>
<%@ page import = "org.openl.rules.util.net.NetUtils" %>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>OpenL Web Studio</title>
<link rel="stylesheet" type="text/css" href="webresource/css/openl/style1.css"></link>
<link rel="stylesheet" type="text/css" href="webresource/css/openl/2col_leftNav.css"></link>



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
     
<table width="97%" cellspacing="0" cellpadding="1">
<tr><td align="right" style="font-size: 40%;">
    <form target="top" action="index.jsp">
    <span style="	font-size:300%;font-weight: bold;color:#8B4513;">Select OpenL Module:</span>
<select name="select_wrapper" onchange="submit()">
<%

  String selected = request.getParameter("select_wrapper");

  OpenLWrapperInfo[] wrappers = studio.getWrappers();

//	studio.select(selected);
  if (wrappers.length == 0)
  {
%>
<option value="-- No OpenL Projects in the Workspace --">-- No OpenL Projects in the Workspace --</option>
<%
  }
  else
  {
    for(int i = 0; i < wrappers.length; ++i)
    {
       boolean current = studio.getCurrentWrapper() == wrappers[i];
       if (current)
           selected = wrappers[i].getWrapperClassName();

%>
<option value="<%=wrappers[i].getWrapperClassName()%>"  <%=current ? " selected='selected'" : ""%>  ><%=studio.getMode().getDisplayName(wrappers[i])%></option>
<%
  }
  }
%>
</select>
</form>
</td>
</tr>
      <tr>
      <td align="right">
      <table style="border-style: solid; border-width: 1; border-color: black" cellspacing="0" cellpadding="2" >
      <td>
        <a href="index.jsp?mode=business" title="Business View" target="top"><img border=0 src="<%= request.getContextPath()%>/webresource/images/business-view.png"></a>
        &nbsp;
        <a href="index.jsp?mode=developer" title="Developer View" target="top"><img border=0 src="<%= request.getContextPath()%>/webresource/images/developer-view.png"></a>
      &nbsp;&nbsp;

<%if (NetUtils.isLocalRequest(request)) {
    if (WebStudioUtils.isRepositoryFailed()) {%>
        <img border=0 src="<%= request.getContextPath()%>/webresource/images/repository/upload-disabled.gif">
        &nbsp;
<%  } else {%>
        <a target="mainFrame" href="<%= request.getContextPath()%>/jsp/uploadProjects.jsf" title="Upload projects to repository"><img border=0 src="<%= request.getContextPath()%>/webresource/images/repository/upload.gif"></a>
        &nbsp;
<%  }
  }%>

        <a target="mainFrame" href="<%= request.getContextPath()%>/jsp/search/search.jsp?searchQuery=&quot;openl tablets&quot;" title="Search"><img border=0 src="<%= request.getContextPath()%>/webresource/images/search.png"></a>
        &nbsp;
        <a href="index.jsp?reload=true&select_wrapper=<%=selected%>" title="Refresh Project(s)" target="top"><img border=0 src="<%= request.getContextPath()%>/webresource/images/refresh.gif"></a>
        &nbsp;
        <a target="mainFrame" href="<%= request.getContextPath()%>/html/ws-intro.html" title="Help"><img border=0 src="<%= request.getContextPath()%>/webresource/images/help.gif"></a>
        &nbsp;
<%if (WebStudioUtils.isRepositoryFailed()) {%>
        <img border=0 src="<%= request.getContextPath()%>/webresource/images/repository/storage-disabled.gif" alt="repository">
<%} else {%>
        <a target="top" href="<%= request.getContextPath()%>/faces/facelets/repository/main.xhtml" title="Rules Repository"><img border=0 src="<%= request.getContextPath()%>/webresource/images/repository/storage.gif" alt="repository"></a>
<% } %>
<%if (!(Boolean)application.getAttribute("hideLogout")) {%>
        &nbsp;
        <a target="top" href='<%=request.getContextPath()%><%= "/logoff.servlet?_j_acegi_logout=true"%>' title="Log out" onclick="return confirm('Are you sure you want to Log out?')"><img border=0 src="<%= request.getContextPath()%>/webresource/images/logout.gif" alt="Log out"></a>
<% } %>
      </td>
      </table>

      </tr>
      </table>
    </div>
    </div>
  </div>
<!-- end masthead -->
</body>
