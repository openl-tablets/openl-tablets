package org.openl.rules.ui;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openl.config.ConfigurationManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.util.FileUtils;

public class ProjectDeleteTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ProjectModel pm;
    private File projectFolder;

    @Before
    public void init() throws Exception {
        // FIXME Currently IBM implementation of java for version 1.6 is not supported
        Assume.assumeFalse(System.getProperty("java.vendor")
            .startsWith("IBM") && System.getProperty("java.specification.version").equals("1.6"));

        // Prepare the project: copy it to the working folder
        projectFolder = tempFolder.getRoot();
        FileUtils.copy(new File("test/rules/locking/"), projectFolder);

        WebStudio ws = mock(WebStudio.class);
        when(ws.getSystemConfigManager()).thenReturn(new ConfigurationManager(null));
        when(ws.isChangeableModuleMode()).thenReturn(true);

        pm = new ProjectModel(ws);
    }

    @Test
    public void testOpenThenDelete() throws Exception {
        pm.setModuleInfo(getModules().get(0));
        pm.clearModuleInfo();

        try {
            FileUtils.delete(projectFolder);
        } catch (IOException e) {
            fail("Project is locked and cannot be deleted");
        }
    }

    @Test
    public void testUseSingleModuleThenDelete() throws Exception {
        pm.setModuleInfo(getModules().get(0));
        pm.useSingleModuleMode();
        pm.clearModuleInfo();

        try {
            FileUtils.delete(projectFolder);
        } catch (IOException e) {
            fail("Project is locked and cannot be deleted");
        }
    }

    @Test
    public void testUseMultiModuleThenDelete() throws Exception {
        pm.setModuleInfo(getModules().get(0));
        pm.useSingleModuleMode();
        pm.useMultiModuleMode();
        pm.clearModuleInfo();

        try {
            FileUtils.delete(projectFolder);
        } catch (IOException e) {
            fail("Project is locked and cannot be deleted");
        }
    }

    private List<Module> getModules() throws ProjectResolvingException {
        return ProjectResolver.instance().resolve(projectFolder).getModules();
    }

}
