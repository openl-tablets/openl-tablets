<%@ page import="java.net.URLEncoder" %>
<jsp:useBean id="navigator" class="org.openl.rules.ui.NavigationBean" scope="page">
    <%
        if (navigator.navigate(request)) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?elementURI=" +
                                  URLEncoder.encode((String)request.getAttribute("url"), "UTF-8"));
        } else {
            response.sendRedirect(request.getContextPath());
        }%>
</jsp:useBean>