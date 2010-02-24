<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">

<%@page import="org.openl.rules.webstudio.web.servlet.RulesUserSession"%>
<%@page import="org.openl.rules.webstudio.web.util.WebStudioUtils"%>
<%@page import="org.openl.rules.workspace.uw.UserWorkspace"%>
<%@page import="org.openl.rules.ui.WebStudio"%>
<%@page import="org.openl.util.StringTool"%>
<%@page import="java.util.*"%>
<%@page import="org.openl.rules.workspace.abstracts.Project"%>
<%@page import="org.openl.rules.workspace.uw.UserWorkspaceProject"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.openl.rules.webstudio.security.CurrentUserInfo"%>
<%@page import="org.openl.rules.workspace.MultiUserWorkspaceManager"%>
<%@page import = "org.openl.rules.util.net.NetUtils" %>
<%@page import="org.openl.rules.webstudio.web.jsf.WebContext"%>
<html>
<head>
<title>OpenL Web Studio</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>

<%
RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(session);
if (rulesUserSession==null) {
    rulesUserSession = new RulesUserSession();
    rulesUserSession.setUser(((CurrentUserInfo)WebApplicationContextUtils.getWebApplicationContext(getServletConfig().getServletContext()).getBean("currentUserInfo")).getUser());
    rulesUserSession.setWorkspaceManager((MultiUserWorkspaceManager)WebApplicationContextUtils.getWebApplicationContext(getServletConfig().getServletContext()).getBean("workspaceManager"));
    session.setAttribute("rulesUserSession", rulesUserSession);
}
if (WebContext.getContextPath() == null) {
    WebContext.setContextPath(request.getContextPath());
}
%>

<jsp:useBean id='studio' scope='session' class="org.openl.rules.ui.WebStudio">
    <%studio.init(session);%>
</jsp:useBean>

<%
if (rulesUserSession != null && !NetUtils.isLocalRequest(request) && (session.getAttribute("studio_from_userWorkspace")==null)) {
        UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
        String path = userWorkspace.getLocalWorkspaceLocation().getAbsolutePath();

        studio = new WebStudio(path);
        Set<String> writableProjects = new HashSet<String>();
        for (Project project : userWorkspace.getProjects()) {
            UserWorkspaceProject workspaceProject = (UserWorkspaceProject) project;
            if (workspaceProject.isCheckedOut() || workspaceProject.isLocalOnly()) {
                writableProjects.add(workspaceProject.getName());
            }
        }
        studio.setWritableProjects(writableProjects);
        session.setAttribute("studio", studio);
        session.setAttribute("studio_from_userWorkspace", "");
}

  String showFormulas = request.getParameter("showFormulas");
  if (showFormulas != null)
    studio.setShowFormulas(showFormulas);

  String collapseProperties = request.getParameter("collapseProperties");
  if (collapseProperties != null)
    studio.setCollapseProperties(collapseProperties);

 String mode = request.getParameter("mode");
  if (mode != null)
    studio.switchMode(mode);
  String reload = request.getParameter("reload");
  if (reload != null)
    studio.reset();
  
  String tableUri = request.getParameter("tableUri");
  String nodeToOpen = null;
  String encodedTableUri = null;
  if (tableUri != null) {
      nodeToOpen = studio.getModel().getTreeNodeId(tableUri);   
      encodedTableUri = StringTool.encodeURL(tableUri);
  }

  String validate = request.getParameter("validate");
  List validateErrors = null;
  if (validate != null)
  {
    validateErrors = studio.getModel().validateAll();
  }

    String operation = request.getParameter("operation");
    if (operation != null)
      studio.executeOperation(operation, session);

    String selected = request.getParameter("headerForm:select_wrapper");

  studio.select(selected);
%>

<frameset rows="78,*" framespacing="0" border="3">
<frame src="${pageContext.request.contextPath}/faces/facelets/studio/header.xhtml" name="header" scrolling="no"
    noresize="noresize" />

<frameset cols="*,79%" frameborder="yes" border="4">
<frameset rows="*,1" border="0">
    <frame src="${pageContext.request.contextPath}/faces/facelets/tree.xhtml?nodeToOpen=<%=nodeToOpen%>" name="leftFrame" scrolling="auto">
    <frame src="html/nothing.html" name="show_app_hidden">
</frameset>


<%if (validateErrors != null) { if (validateErrors.size() > 0){%>
<frame src="${pageContext.request.contextPath}/html/yesValidateErrors.html" name="mainFrame" scrolling="auto"/>
<%} else { %>
<frame src="${pageContext.request.contextPath}/html/noValidateErrors.html" name="mainFrame" scrolling="auto"/>
<%}} else { %>
<%if (encodedTableUri != null) {%>
<frame src="${pageContext.request.contextPath}/faces/facelets/tableeditor/showTable.xhtml?uri=<%=encodedTableUri%>" name="mainFrame" scrolling="auto"/>
<%} else { %>
<frame src="<%=System.getProperty( "org.openl.webstudio.intro.html", "webresource/html/ws-intro.html")%>" name="mainFrame" scrolling="auto"/>
<%}} %>


</frameset>


</frameset>

<noframes>
<body>
    <p>To view content you need frames capable browser
</body>
</noframes>

</html>
