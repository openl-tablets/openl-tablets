package org.openl.rules.workspace.lw.impl;

import junit.framework.TestCase;
import org.openl.SmartProps;
import org.openl.rules.workspace.TestHelper;
import org.openl.rules.workspace.dtr.impl.RepositoryProjectVersionImpl;
import org.openl.rules.workspace.dtr.impl.RepositoryVersionInfoImpl;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.lw.LWTestHelper;

import java.io.File;
import java.util.Properties;
import java.util.Date;

/**
 * Tests correct working of <code>LocalProjectImpl</code> with properties. 
 */
public class LocalWorkspaceImplPropertiesTestCase extends TestCase {
    private LocalProjectImpl localProject;

    @Override
    protected void setUp() throws Exception {
        TestHelper.ensureTestFolderExistsAndClear();

        Properties properties = new Properties();
        properties.put(LocalWorkspaceManagerImpl.PROP_WS_LOCATION, TestHelper.FOLDER_TEST);

        LocalWorkspaceManagerImpl lwm = new LocalWorkspaceManagerImpl(new SmartProps(properties));

        LocalWorkspaceImpl workspace = lwm.createWorkspace(LWTestHelper.getTestUser());
        localProject = (LocalProjectImpl) workspace.addProject(
                new LocalProjectImpl("sample", new ArtefactPathImpl("sample"),
                new File(TestHelper.FOLDER_TEST),
                new RepositoryProjectVersionImpl(1, 0, 0, new RepositoryVersionInfoImpl(new Date(), "test")),
                workspace));

    }


    public void testIt() {
        
    }
}
