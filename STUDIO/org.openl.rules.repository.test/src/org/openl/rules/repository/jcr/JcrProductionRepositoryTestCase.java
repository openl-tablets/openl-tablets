package org.openl.rules.repository.jcr;

import junit.framework.TestCase;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.rules.repository.FolderHelper;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.RProductionDeployment;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RFolder;
import org.openl.rules.repository.exceptions.RRepositoryException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeManager;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class JcrProductionRepositoryTestCase extends TestCase {
    private static final String TEST_FOLDER = "test_work";
    private JcrProductionRepository instance;
    private TransientRepository repository;

    private static final Date EFF_DATE1 = new Date(1246123211);
    private static final Date EFF_DATE2 = new Date(EFF_DATE1.getTime() + 1000*60*60*24);
    private static final Date EFF_DATE3 = new Date(EFF_DATE1.getTime() + 2000*60*60*24);
    private static final String LOB_M = "management";
    private static final String LOB_S = "seller";

    @Override
    protected void setUp() throws Exception {
        FolderHelper.deleteFolder(new File(TEST_FOLDER));

        String repConf = "/jackrabbit-repository.xml";

        // obtain real path to repository configuration file
        URL url = this.getClass().getResource(repConf);
        String fullPath = url.getFile();

        repository = new TransientRepository(fullPath, TEST_FOLDER);

        Session session = createSession("user", "pass", repository);
        instance = new JcrProductionRepository("test", session);
        initNodeTypes(session.getWorkspace().getNodeTypeManager());

        RProductionDeployment deployment = instance.createDeployment("d1");
        RProject rProject = deployment.createProject("prj1");
        rProject.setLineOfBusiness(LOB_M);

        RFolder folder = rProject.getRootFolder().createFolder("folder1");
        folder.setEffectiveDate(EFF_DATE1);

        RFile rFile1 = rProject.getRootFolder().createFile("f1");
        rFile1.setLineOfBusiness(LOB_M);
        rFile1.setEffectiveDate(EFF_DATE2);

        RFile rFile2 = rProject.getRootFolder().createFile("f2");
        rFile2.setLineOfBusiness(LOB_S);
        rFile2.setEffectiveDate(EFF_DATE3);

        deployment.save();
    }


    public void testIt() throws RRepositoryException {
//        _testLob();
        _testEffectiveDate();
    }

    public void _testLob() throws RRepositoryException {
        JcrProductionSearchParams params = new JcrProductionSearchParams();

        params.setLineOfBusiness(LOB_M);
        Collection<REntity> entityCollection = instance.findNodes(params);
        assertEquals(2, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "prj1", "f1"));

        params.setLineOfBusiness(LOB_S);
         entityCollection = instance.findNodes(params);
        assertEquals(1, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f2"));
    }

    public void _testEffectiveDate() throws RRepositoryException {
        JcrProductionSearchParams params = new JcrProductionSearchParams();
        params.setLowerEffectiveDate(EFF_DATE2);
        Collection<REntity> entityCollection = instance.findNodes(params);
        assertEquals(2, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1", "f2"));
    }


    private static Collection<String> entities2names(Collection<REntity> entities) {
        List<String> result = new ArrayList<String>();
        for (REntity entity : entities) {
            result.add(entity.getName());
        }
        return result;
    }

    private static boolean check(Collection<String> collection, String... names) {
        Set<String> s1 = new HashSet<String>(collection);
        Set<String> s2 = new HashSet<String>(Arrays.asList(names));

        return s1.equals(s2);
    }

    protected static Session createSession(String user, String pass, TransientRepository repository) throws RepositoryException {
        char[] password = pass.toCharArray();
        SimpleCredentials sc = new SimpleCredentials(user, password);
        return repository.login(sc);
    }

    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        NodeTypeManagerImpl ntmi = (NodeTypeManagerImpl) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream("/org/openl/rules/repository/production_nodetypes.xml");
                ntmi.registerNodeTypes(is, NodeTypeManagerImpl.TEXT_XML, true);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to init NodeTypes: " + e.getMessage(), e);
        }
    }


    @Override
    protected void tearDown() throws Exception {
        instance.release();
        instance = null;
        repository.shutdown();
        repository = null;
    }
}
