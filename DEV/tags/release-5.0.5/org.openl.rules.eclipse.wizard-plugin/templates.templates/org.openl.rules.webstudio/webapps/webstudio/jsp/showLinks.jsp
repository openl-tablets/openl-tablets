<%@ page import="org.openl.rules.webstudio.web.util.WebStudioUtils" %>
<%@ page import="org.openl.rules.webtools.XlsUrlParser" %>
<%@ page import="java.io.File" %>

<%
    boolean local = WebStudioUtils.isLocalRequest(request);
    boolean wantURI = request.getParameter("uri") != null;
    if (local) {
        if (wantURI) {
            XlsUrlParser parser = new XlsUrlParser();
            parser.parse(request.getParameter("uri"));
            org.openl.rules.webtools.ExcelLauncher.launch("LaunchExcel.vbs",
                    parser.wbPath, parser.wbName, parser.wsName, parser.range);
            return;
        }

        String wbName = request.getParameter("wbName");
        if (wbName != null)
            org.openl.rules.webtools.ExcelLauncher.launch(
                    "LaunchExcel.vbs",
                    request.getParameter("wbPath"),
                    wbName,
                    request.getParameter("wsName"),
                    request.getParameter("range")

            );

        String wdName = request.getParameter("wdName");
        if (wdName != null)
            org.openl.rules.webtools.WordLauncher.launch(
                    "LaunchWord.vbs",
                    request.getParameter("wdPath"),
                    wdName,
                    request.getParameter("wdParStart"),
                    request.getParameter("wdParEnd")

            );
    } else if (wantURI) {
%>
<script type="text/javascript">alert("This action is available only from the machine server runs at.")</script>
<% } else {
    String filename = request.getParameter("wbName");
    String path = request.getParameter("wbPath");
    if (filename == null) {
        filename = request.getParameter("wdName");
        path = request.getParameter("wdPath");
    }
    pageContext.setAttribute("filename", new File(path, filename).getAbsolutePath());
%>
<jsp:forward page="/action/download"><jsp:param name="filename" value="${filename}" /></jsp:forward>
<%}%>
