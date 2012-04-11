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
    boolean wantURI = request.getParameter("uri") != null;
    if (local) {

        if (wantURI) {
        
        	String file = SourceCodeURLTool.parseUrl(request.getParameter("uri")).get(SourceCodeURLConstants.FILE);
        	
        	if (FileTypeHelper.isExcelFile(file)) {
        	
		        XlsUrlParser parser = new XlsUrlParser();
		        parser.parse(request.getParameter("uri"));
		        ExcelLauncher.launch(excelScriptPath,
		        	parser.wbPath,
		            parser.wbName, 
		            parser.wsName, 
		            parser.range);
		        return;
            }
            
            if (FileTypeHelper.isWordFile(file)) {
        	
		        WordUrlParser parser = new WordUrlParser();
		        parser.parse(request.getParameter("uri"));
				WordLauncher.launch(wordScriptPath,
                    parser.wdPath,
                    parser.wdName,
                    parser.wdParStart,
                    parser.wdParEnd);
                return;
            }
        }

        String wbName = request.getParameter("wbName");
        
        if (wbName != null)
            ExcelLauncher.launch(excelScriptPath,
                    request.getParameter("wbPath"),
                    wbName,
                    request.getParameter("wsName"),
                    request.getParameter("range")

            );

        String wdName = request.getParameter("wdName");
        
        if (wdName != null)
            WordLauncher.launch(
                    wordScriptPath,
                    request.getParameter("wdPath"),
                    wdName,
                    request.getParameter("wdParStart"),
                    request.getParameter("wdParEnd")

            );
    } else { // Remote
        String path;
        String filename;
        path = request.getParameter("wbPath");
        
        if (path != null) {
            XlsUrlParser parser = new XlsUrlParser();
            parser.parse(request.getParameter("uri"));
            path = parser.wbPath;
            filename = parser.wbName;
        } else {
            path = request.getParameter("wbPath");
            filename = request.getParameter("wbName");
            if (filename == null) {
                filename = request.getParameter("wdName");
                path = request.getParameter("wdPath");
            }
        }
        pageContext.setAttribute("filename", new File(path, filename).getAbsolutePath());
    %>
    <jsp:forward page="/action/download"><jsp:param name="filename" value="${filename}" /></jsp:forward>
    <%}%>
