package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.util.FileUtils;

public class ProjectDeleteTest {
    @TempDir
    public File tempFolder;

    private ProjectModel pm;
    private File projectFolder;

    @BeforeEach
    public void init() throws Exception {
        // Prepare the project: copy it to the working folder
        projectFolder = tempFolder;
        FileUtils.copy(new File("test/rules/locking/"), projectFolder);

        WebStudio ws = mock(WebStudio.class);
        pm = new ProjectModel(ws);
    }

    @Test
    public void testOpenThenDelete() throws Exception {
        pm.setModuleInfo(getModules().get(0));
        pm.clearModuleInfo();

        try {
            FileUtils.delete(projectFolder.toPath());
        } catch (IOException e) {
            fail("Project is locked and cannot be deleted");
        }
    }

    @Test
    public void testDelete() throws Exception {
        pm.setModuleInfo(getModules().get(0));
        pm.clearModuleInfo();

        try {
            FileUtils.delete(projectFolder.toPath());
        } catch (IOException e) {
            fail("Project is locked and cannot be deleted");
        }
    }

    private List<Module> getModules() throws ProjectResolvingException {
        return ProjectResolver.getInstance().resolve(projectFolder).getModules();
    }

}
