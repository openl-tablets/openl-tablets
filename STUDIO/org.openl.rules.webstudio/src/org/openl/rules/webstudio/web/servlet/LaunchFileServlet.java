package org.openl.rules.webstudio.web.servlet;

import org.openl.commons.web.util.WebTool;
import org.openl.extension.ExtensionWrapperGrid;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.ExcelLauncher;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.FileTypeHelper;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.DefaultPrivileges.PRIVILEGE_EDIT_TABLES;

public class LaunchFileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient final Logger log = LoggerFactory.getLogger(LaunchFileServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        if (!isGranted(PRIVILEGE_EDIT_TABLES)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        WebStudio ws = getWebStudio(request);
        if (ws == null) return;

        ProjectModel model = ws.getModel();

        String id = request.getParameter(Constants.REQUEST_PARAM_ID);
        IOpenLTable table = model.getTableById(id);
        if (table == null) return;

        String uri = table.getUri();

        String file;
        String decodedUriParameter;
        try {
            decodedUriParameter = StringTool.decodeURL(uri);
            URL url = new URL(decodedUriParameter);
            file = url.getFile();

            int indexQuestionMark = file.indexOf('?');
            file = indexQuestionMark < 0 ? file : file.substring(0, indexQuestionMark);
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
            return;
        }

        ExtensionWrapperGrid wrapperGrid = null;
        if (table.getGridTable().getGrid() instanceof ExtensionWrapperGrid) {
            wrapperGrid = (ExtensionWrapperGrid) table.getGridTable().getGrid();
            if (wrapperGrid.isLaunchSupported()) {
                wrapperGrid.getFileLauncher().launch();
                return;
            }
            file = wrapperGrid.getSourceFileName();
        }

        if (!FileTypeHelper.isExcelFile(file)) { // Excel
            log.error("Unsupported file format [{}]", file);
            return;
        }

        decodedUriParameter = decodedUriParameter.replaceAll("\\+", "%2B"); //Support '+' sign in file names;

        String wbPath;
        String wbName;
        String wsName;
        String range;

        // Parse url
        try {
            XlsUrlParser parser = new XlsUrlParser();
            parser.parse(decodedUriParameter);
            wbPath = parser.wbPath;
            wbName = wrapperGrid != null ? wrapperGrid.getSourceFileName() : parser.wbName;
            wsName = parser.wsName;
            range = parser.range;
        } catch (Exception e) {
            log.error("Can't parse file uri", e);
            return;
        }

        boolean local = WebTool.isLocalRequest(request);
        if (local) { // local mode
            try {
                model.openWorkbookForEdit(wbName);

                String excelScriptPath = getServletContext().getRealPath("/scripts/LaunchExcel.vbs");
                ExcelLauncher.launch(excelScriptPath, wbPath, wbName, wsName, range);

                model.afterOpenWorkbookForEdit(wbName);

            } catch (Exception e) {
                log.error("Can't launch file", e);
            }

        } else { // remote mode

            String filePath = new File(wbPath, wbName).getAbsolutePath();

            String query = "filename=" + StringTool.encodeURL(filePath);
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/action/download?" + query);
            dispatcher.forward(request, response);
        }
    }

    private static WebStudio getWebStudio(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (WebStudio) (session == null ? null : session.getAttribute("studio"));
    }
}
