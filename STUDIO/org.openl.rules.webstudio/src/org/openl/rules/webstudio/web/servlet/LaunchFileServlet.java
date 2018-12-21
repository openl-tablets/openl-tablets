package org.openl.rules.webstudio.web.servlet;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.EDIT_TABLES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openl.commons.web.util.WebTool;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.ExcelLauncher;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.FileTypeHelper;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (!isGranted(EDIT_TABLES)) {
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
            log.debug("uri: {}", uri);
            decodedUriParameter = StringTool.decodeURL(uri);
            URL url = new URL(decodedUriParameter);
            file = url.getFile();

            int indexQuestionMark = file.indexOf('?');
            file = indexQuestionMark < 0 ? file : file.substring(0, indexQuestionMark);
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
            return;
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
            log.debug("decodedUriParameter: {}", decodedUriParameter);

            XlsUrlParser parser = new XlsUrlParser();
            parser.parse(decodedUriParameter);
            wbPath = parser.getWbPath();
            wbName = parser.getWbName();
            wsName = parser.getWsName();
            range = parser.getRange();

            log.debug("wbPath: {}, wbName: {}, wsName: {}, range: {}", wbPath, wbName, wsName, range);
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

                return;
            } catch (Exception e) {
                log.info("Can't launch an excel file", e);
            }
        }

        File file1 = new File(wbPath, wbName);

        if (file1.isFile()) {
            response.setContentType("application/octet-stream");
            WebTool.setContentDisposition(response, file1.getName());

            OutputStream outputStream = response.getOutputStream();
            FileInputStream fis = new FileInputStream(file1);
            IOUtils.copyAndClose(fis, outputStream);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static WebStudio getWebStudio(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (WebStudio) (session == null ? null : session.getAttribute("studio"));
    }
}
