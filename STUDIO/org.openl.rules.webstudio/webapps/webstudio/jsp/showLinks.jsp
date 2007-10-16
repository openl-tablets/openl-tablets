<%@ page import="org.openl.rules.webtools.XlsUrlParser" %>
<%
    if (request.getParameter("uri") != null) {
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

%>

