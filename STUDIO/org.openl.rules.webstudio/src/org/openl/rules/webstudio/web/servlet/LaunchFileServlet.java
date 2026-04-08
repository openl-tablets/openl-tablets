package org.openl.rules.webstudio.web.servlet;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.common.utils.WebTool;
import org.openl.util.FileTypeHelper;

@WebServlet("/action/launch")
@Slf4j
public class LaunchFileServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;


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

        String id = request.getParameter("tableId");
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

        Path pathToFile = Path.of(parser.getWbPath(), parser.getWbName());
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
