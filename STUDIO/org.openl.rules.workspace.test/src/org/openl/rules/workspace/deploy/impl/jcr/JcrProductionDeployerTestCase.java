package org.openl.rules.workspace.deploy.impl.jcr;

import junit.framework.TestCase;
import org.openl.SmartProps;
import static org.openl.rules.workspace.TestHelper.*;

import java.io.File;
import java.util.Properties;

public class JcrProductionDeployerTestCase extends TestCase {
    /**
     * <code>JcrProductionDeployer</code> instance to be used in tests. 
     */
    private JcrProductionDeployer instance;

    @Override
    protected void setUp() throws Exception {
        ensureTestFolderExistsAndClear();

        Properties properties = new Properties();
        properties.put(JcrProductionDeployer.PROPNAME_ZIPFOLDER, FOLDER_TEST);

        instance = new JcrProductionDeployer(getWorkspaceUser(), new SmartProps(properties));
    }

    public void testTempFolder() {
        assertTrue("temp folder for test user was not created", new File(FOLDER_TEST, getWorkspaceUser().getUserId()).exists());
    }
}
