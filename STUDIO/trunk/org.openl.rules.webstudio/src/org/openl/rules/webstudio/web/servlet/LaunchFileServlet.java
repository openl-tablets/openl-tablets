package org.openl.rules.webstudio.web.servlet;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_EDIT_TABLES;

import java.io.File;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.util.WebTool;
import org.openl.main.SourceCodeURLConstants;
import org.openl.main.SourceCodeURLTool;
import org.openl.rules.table.word.WordUrlParser;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.ExcelLauncher;
import org.openl.rules.webstudio.util.WordLauncher;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.FileTypeHelper;
import org.openl.util.StringTool;

public class LaunchFileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final Log log = LogFactory.getLog(LaunchFileServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isGranted(PRIVILEGE_EDIT_TABLES)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String excelScriptPath = getServletContext().getRealPath("scripts/LaunchExcel.vbs");
        String wordScriptPath = getServletContext().getRealPath("scripts/LaunchWord.vbs");

        boolean local = WebTool.isLocalRequest(request);

        String uri = request.getParameter(Constants.REQUEST_PARAM_URI);

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

            try {
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
            } catch (Exception e) {
                log.error("Can't parse file uri", e);
                return;
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
            try {
                if (isExcel) {
                    WebStudio ws = getWebStudio(request);
                    if (ws == null)
                        return;
                    
                    ProjectModel project = ws.getModel();
                    project.openWorkbookForEdit(wbName);
                    
                    ExcelLauncher.launch(excelScriptPath, wbPath, wbName, wsName, range);
                    
                    project.afterOpenWorkbookForEdit(wbName);
                    
                    return;
                } else if (isWord) {
                    WordLauncher.launch(wordScriptPath, wdPath, wdName, wdParStart, wdParEnd);
                    return;
                }
            } catch (Exception e) {
                log.error("Can't launch file", e);
            }

        } else { // remote mode
            String fileName = null;
            String path = null;

            if (isExcel) {
                fileName = wbName;
                path = wbPath;
            } else if (isWord) {
                fileName = wdName;
                path = wdPath;
            }
            String filePath = new File(path, fileName).getAbsolutePath();

            String query = "filename=" + StringTool.encodeURL(filePath);
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/action/download?" + query);
            dispatcher.forward(request,response);
        }
    }

    private static WebStudio getWebStudio(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (WebStudio) (session == null ? null : session.getAttribute("studio"));
    }
}
