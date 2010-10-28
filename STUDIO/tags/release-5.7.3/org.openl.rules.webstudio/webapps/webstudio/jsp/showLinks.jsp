<%@ page import="org.openl.rules.webstudio.web.util.WebStudioUtils" %>
<%@ page import="org.openl.rules.table.xls.XlsUrlParser"%>
<%@ page import="org.openl.rules.table.word.WordUrlParser"%>
<%@ page import="org.openl.rules.webstudio.util.ExcelLauncher"%>
<%@ page import="org.openl.rules.webstudio.util.WordLauncher"%>
<%@ page import="org.openl.util.FileTypeHelper"%>
<%@ page import="org.openl.main.SourceCodeURLTool"%>
<%@ page import="org.openl.main.SourceCodeURLConstants"%>
<%@ page import="java.io.File" %>
<%@page import="org.openl.commons.web.util.WebTool"%>

<%
	String excelScriptPath = pageContext.getServletContext().getRealPath("scripts/LaunchExcel.vbs");
	String wordScriptPath = pageContext.getServletContext().getRealPath("scripts/LaunchWord.vbs");

    boolean local = WebTool.isLocalRequest(request);

    String uri = request.getParameter("uri");

    String wbPath = null;
    String wbName = null;
    String wsName = null;
    String range = null;
    
    String wdPath = null;
    String wdName = null;
    String wdParStart = null;
    String wdParEnd = null;

    boolean isExcel = false;
    boolean isWord = false;

    if (uri != null) { // by uri
        String file = SourceCodeURLTool.parseUrl(uri).get(SourceCodeURLConstants.FILE);

        if (FileTypeHelper.isExcelFile(file)) { // Excel
            XlsUrlParser parser = new XlsUrlParser();
            parser.parse(uri);
            wbPath = parser.wbPath;
            wbName = parser.wbName; 
            wsName = parser.wsName; 
            range = parser.range;
            isExcel = true;

        } else if (FileTypeHelper.isWordFile(file)) { // Word
            WordUrlParser parser = new WordUrlParser();
            parser.parse(uri);
            wdPath = parser.wdPath;
            wdName = parser.wdName; 
            wdParStart = parser.wdParStart; 
            wdParEnd = parser.wdParEnd;
            isWord = true;
        }

    } else { // by params
        wbName = request.getParameter("wbName");

        if (wbName != null) { // Excel
            wbPath = request.getParameter("wbPath");
            wsName = request.getParameter("wsName");
            range = request.getParameter("range");
            isExcel = true;
        } else {
            wdName = request.getParameter("wdName");

            if (wdName != null) {  // Word
                wdPath = request.getParameter("wdPath");
                wdParStart = request.getParameter("wdParStart");
                wdParEnd = request.getParameter("wdParEnd");
                isWord = true;
            }
        }
    }

    if (local) { // local mode
        if (isExcel) {
		    ExcelLauncher.launch(excelScriptPath, wbPath, wbName, wsName, range);
		    return;
        } else if (isWord) {
        	WordLauncher.launch(wordScriptPath, wdPath, wdName, wdParStart, wdParEnd);
            return;
        }

    } else { // remote mode
        String filename = null;
        String path = null;

        if (isExcel) {
            filename = wbName;
            path = wbPath;
        } else if (isWord) {
            filename = wdName;
            path = wdPath;
        }

        pageContext.setAttribute("filename", new File(path, filename).getAbsolutePath());
        %>
        <jsp:forward page="/action/download"><jsp:param name="filename" value="${filename}" /></jsp:forward>
    <%}%>
