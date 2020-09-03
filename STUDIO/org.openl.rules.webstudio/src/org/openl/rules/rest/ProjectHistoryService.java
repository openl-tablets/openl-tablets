package org.openl.rules.rest;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.ProjectHistoryItem;
import org.openl.rules.webstudio.web.admin.ProjectsInHistoryController;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/history/")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectHistoryService {

    @Autowired
    private HttpSession httpSession;

    @GET
    @Path("project")
    public List<ProjectHistoryItem> getProjectHistory() throws IOException {
        WebStudio webStudio = WebStudioUtils.getWebStudio(httpSession);
        ProjectModel model = webStudio.getModel();
        String projectHistoryPath = Paths
            .get(webStudio.getWorkspacePath(),
                model.getProject().getFolderPath(),
                FolderHelper.HISTORY_FOLDER,
                model.getModuleInfo().getName())
            .toString();
        return ProjectsInHistoryController.getProjectHistory(projectHistoryPath);
    }

    @POST
    @Path("restore")
    public void restore(String versionToRestore) throws Exception {
        ProjectModel model = WebStudioUtils.getWebStudio(httpSession).getModel();
        if (model != null) {
            model.getHistoryManager().restore(versionToRestore);
        }
    }
}
