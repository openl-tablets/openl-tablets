package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.webstudio.web.SearchScope;

/**
 * Regression guard for parallel multi-project editing: a {@link ProjectModel} must report its OWN project
 * identity (derived from the opened module) instead of asking the {@link WebStudio} session for the current
 * selection. Several models share one session, so a model that read {@code studio.getCurrentProject()} would
 * follow whatever project was last selected — clobbering the others.
 */
public class ProjectModelOwnIdentityTest extends AbstractWorkbookGeneratingTest {

    private static final String SHEET_NAME = "Test";
    private static final String MODULE_FILE_NAME = "MainModule.xls";
    private static final String REPO_ID = "design";

    private WebStudio studio;
    private ProjectModel pm;
    private String projectFolderName;

    @BeforeEach
    public void init() throws Exception {
        createModule();

        studio = mock(WebStudio.class);
        when(studio.getProjectResolver()).thenReturn(ProjectResolver.getInstance());
        when(studio.getCurrentRepositoryId()).thenReturn(REPO_ID);

        pm = new ProjectModel(studio);
        var module = getModules().getFirst();
        projectFolderName = module.getProject().getProjectFolder().getFileName().toString();
        pm.setModuleInfo(module);
    }

    @Test
    public void getProjectResolvesOwnProjectAndIgnoresCurrentSelection() {
        RulesProject ownProject = mock(RulesProject.class);
        RulesProject otherProject = mock(RulesProject.class);
        // The model's own project is resolved by (repositoryId, project folder).
        when(studio.getProject(REPO_ID, projectFolderName)).thenReturn(ownProject);
        // The session 'current' selection points at a DIFFERENT project; the model must not follow it.
        when(studio.getCurrentProject()).thenReturn(otherProject);

        assertSame(ownProject, pm.getProject());
        assertNotSame(otherProject, pm.getProject());
    }

    @Test
    public void searchScopeCurrentProjectUsesOwnModuleNotThreadBoundStudio() {
        // Before the decoupling this read WebStudioUtils.getWebStudio().getCurrentModule(), which is null on
        // a non-request thread (no FacesContext / request attributes) and would NPE. It must use this model's
        // own module instead.
        var nodes = pm.getSearchScopeData(SearchScope.CURRENT_PROJECT);
        assertNotNull(nodes);
        assertFalse(nodes.isEmpty(), "current project search scope should contain the opened module's tables");
    }

    private void createModule() throws Exception {
        Workbook book = new HSSFWorkbook();
        Sheet sheet = book.createSheet(SHEET_NAME);
        createTable(sheet, new String[][]{{"Method String getGreeting(String name)"},
                {"Return \"Hi, \" + name;"}});
        writeBook(book, MODULE_FILE_NAME);
    }
}
