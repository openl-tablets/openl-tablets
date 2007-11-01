<%@ page import="org.openl.rules.uw.UserWorkspace" %>
<%@ page import="org.openl.rules.uw.UserWorkspaceProject" %>
<%@ page import="org.openl.rules.webstudio.RulesUserSession" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Local Workspace</title>
</head>
<body>

<%
    Object obj = session.getAttribute("rulesUserSession");

    if (obj == null) {
%>

    No rulesUserSession attribute in session!!!

<%
    } else {
        RulesUserSession rulesUserSession = (RulesUserSession) obj;

        String userId = rulesUserSession.getUserId();

        UserWorkspace uw = rulesUserSession.getUserWorkspace();
%>

    UserId: <%= userId %><br/>
    File: <%= uw.getLocalWorkspaceLocation()%><br/>

    Projects:<br/>
    <ol>

<%
        for (UserWorkspaceProject uwp : uw.getProjects()) {
            %>
            <li>
            <%= uwp.getName() %>
            </li>
            <%

        }

    }
%>
    </ol>

<p/>

</body>
</html>
