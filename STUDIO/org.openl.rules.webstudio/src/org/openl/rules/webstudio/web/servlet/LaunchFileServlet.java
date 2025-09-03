package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.WebTool;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.util.FileTypeHelper;

@WebServlet("/action/launch")
public class LaunchFileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient final Logger log = LoggerFactory.getLogger(LaunchFileServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        WebStudio ws = getWebStudio(request);
        if (ws == null) {
            return;
        }
        RulesProject currentProject = ws.getCurrentProject();
        AProjectArtefact currentModule;
        try {
            currentModule = currentProject.getArtefact(ws.getCurrentModule().getRulesRootPath().getPath());
        } catch (ProjectException e) {
            return;
        }
        if (!ws.getDesignRepositoryAclService().isGranted(currentModule, List.of(BasePermission.WRITE))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        ProjectModel model = ws.getModel();

        String id = request.getParameter(Constants.REQUEST_PARAM_ID);
        IOpenLTable table = model.getTableById(id);
        if (table == null) {
            return;
        }

        String uri = "file://" + ws.getWorkspacePath() + "/" + table.getUri();
        uri = uri.replaceAll("\\+", "%2B"); // Support '+' sign in file names;

        final XlsUrlParser parser;
        // Parse url
        try {
            log.debug("uri: {}", uri);
            parser = new XlsUrlParser(uri);
        } catch (Exception e) {
            log.error("Cannot parse file uri", e);
            return;
        }

        if (!FileTypeHelper.isExcelFile(parser.getWbName())) { // Excel
            log.error("Unsupported file format [{}]", parser.getWbName());
            return;
        }

        Path pathToFile = Paths.get(parser.getWbPath(), parser.getWbName());
        if (Files.isRegularFile(pathToFile)) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    WebTool.getContentDispositionValue(pathToFile.getFileName().toString()));

            try (var in = Files.newInputStream(pathToFile); var out = response.getOutputStream()) {
                in.transferTo(out);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static WebStudio getWebStudio(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (WebStudio) (session == null ? null : session.getAttribute("studio"));
    }
}
