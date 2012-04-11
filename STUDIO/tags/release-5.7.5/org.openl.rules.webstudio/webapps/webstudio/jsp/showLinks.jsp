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
<%@page import="org.openl.util.StringTool"%>

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
            wbPath = StringTool.decodeURL(parser.wbPath);
            wbName = StringTool.decodeURL(parser.wbName); 
            wsName = StringTool.decodeURL(parser.wsName); 
            range = StringTool.decodeURL(parser.range);
            isExcel = true;

        } else if (FileTypeHelper.isWordFile(file)) { // Word
            WordUrlParser parser = new WordUrlParser();
            parser.parse(uri);
            wdPath = StringTool.decodeURL(parser.wdPath);
            wdName = StringTool.decodeURL(parser.wdName); 
            wdParStart = StringTool.decodeURL(parser.wdParStart); 
            wdParEnd = StringTool.decodeURL(parser.wdParEnd);
            isWord = true;
        }

    } else { // by params @Deprecated
        wbName = StringTool.decodeURL(request.getParameter("wbName"));

        if (wbName != null) { // Excel
            wbPath = StringTool.decodeURL(request.getParameter("wbPath"));
            wsName = StringTool.decodeURL(request.getParameter("wsName"));
            range = StringTool.decodeURL(request.getParameter("range"));
            isExcel = true;
        } else {
            wdName = StringTool.decodeURL(request.getParameter("wdName"));

            if (wdName != null) {  // Word
                wdPath = StringTool.decodeURL(request.getParameter("wdPath"));
                wdParStart = StringTool.decodeURL(request.getParameter("wdParStart"));
                wdParEnd = StringTool.decodeURL(request.getParameter("wdParEnd"));
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
