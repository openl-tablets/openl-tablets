package org.openl.rules.repository.jcr;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeTypeManager;

import junit.framework.TestCase;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ValueType;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.jacrkrabbit.transactions.JackrabbitTransactionManager;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.api.ArtefactAPI;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.FileUtils;

public class JcrProductionRepositoryTestCase extends TestCase {
    private static final String TEST_FOLDER = "target/test_work";
    private static final Date EFF_DATE1 = new Date(124612321111L);
    private static final Date EFF_DATE2 = new Date(EFF_DATE1.getTime() + 1000 * 60 * 60 * 24);

    private static final Date EFF_DATE3 = new Date(EFF_DATE1.getTime() + 2000 * 60 * 60 * 24);
    private static final Date EFF_DATE4 = new Date(EFF_DATE1.getTime() + 3000 * 60 * 60 * 24);
    private static final Date EXP_DATE1 = new Date(4456456444517L);
    private static final Date EXP_DATE2 = new Date(EXP_DATE1.getTime() + 1000 * 60 * 60 * 24);

    private static final Date EXP_DATE3 = new Date(EXP_DATE1.getTime() + 2000 * 60 * 60 * 24);
    private static final Date EXP_DATE4 = new Date(EXP_DATE1.getTime() + 3000 * 60 * 60 * 24);
    private static final String LOB_M = "management";
    private static final String LOB_S = "seller";

    private JcrProductionRepository instance;
    private TransientRepository repository;

    private static boolean check(Collection<String> collection, String... names) {
        Set<String> s1 = new HashSet<String>(collection);
        Set<String> s2 = new HashSet<String>(Arrays.asList(names));

        return s1.equals(s2);
    }

    protected static Session createSession(String user, String pass, TransientRepository repository)
            throws RepositoryException {
        char[] password = pass.toCharArray();
        SimpleCredentials sc = new SimpleCredentials(user, password);
        return repository.login(sc);
    }

    private static Collection<String> entities2names(Collection<ArtefactAPI> entities) {
        List<String> result = new ArrayList<String>();
        for (ArtefactAPI entity : entities) {
            result.add(entity.getName());
        }
        return result;
    }

    public void _testEffectiveDate() throws RRepositoryException {
        JcrProductionSearchParams params = new JcrProductionSearchParams();
        params.setLowerEffectiveDate(EFF_DATE2);
        Collection<ArtefactAPI> entityCollection = instance.findNodes(params);

        assertEquals(3, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1", "f2", "folder2"));

        params.setUpperEffectiveDate(EFF_DATE3);
        entityCollection = instance.findNodes(params);
        assertEquals(2, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1", "f2"));
/*
        params.setUpperEffectiveDate(new Date(EFF_DATE3.getTime() - 1));
        entityCollection = instance.findNodes(params);
        assertEquals(1, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1"));*/
    }

    public void _testExpirationDate() throws RRepositoryException {
        JcrProductionSearchParams params = new JcrProductionSearchParams();
        params.setLowerExpirationDate(EXP_DATE2);
       
        Collection<ArtefactAPI> entityCollection = instance.findNodes(params);
        assertEquals(3, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1", "f2", "folder2"));

        params.setUpperExpirationDate(EXP_DATE3);
        entityCollection = instance.findNodes(params);
        assertEquals(2, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1", "f2"));
/*
        params.setUpperExpirationDate(new Date(EXP_DATE3.getTime() - 1));
        entityCollection = instance.findNodes(params);
        assertEquals(1, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1"));*/
    }
    
    private static class TestDeployer{
        public static void deploy(JcrProductionRepository productionRepository) throws ProjectException {
            FolderAPI deployment = productionRepository.createDeploymentProject("lis");

            FolderAPI rProject = deployment.addFolder("p1");
            rProject.addFolder("f1");

            deployment.commit(new CommonUserImpl("sys"), deployment.getVersion().getRevision() +1);
            productionRepository.notifyChanges();
        }
    }

    private void _testListeners() throws ProjectException, InterruptedException {
        final boolean[] flag = new boolean[1];
        class TestListener implements RDeploymentListener {
            public void onEvent() {
                flag[0] = true;
            }
        }

        TestListener listener = new TestListener();
        instance.addListener(listener);
        try {
            TestDeployer.deploy(instance);
            Thread.sleep(500); // notifications come asynchroniously
        } finally {
            instance.removeListener(listener);
        }

        assertTrue(flag[0]);
    }

    public void _testLob() throws RRepositoryException {
        JcrProductionSearchParams params = new JcrProductionSearchParams();

        params.setLineOfBusiness(LOB_M);
        Collection<ArtefactAPI> entityCollection = instance.findNodes(params);
        assertEquals(3, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "prj1", "f1", "folder2"));

        params.setLineOfBusiness(LOB_S);
        entityCollection = instance.findNodes(params);
        assertEquals(1, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f2"));
    }

    public void _testSeveralProperties() throws RRepositoryException {
        JcrProductionSearchParams params = new JcrProductionSearchParams();
        params.setLowerEffectiveDate(new Date(EFF_DATE1.getTime() + 1));
        params.setLineOfBusiness(LOB_M);
        Collection<ArtefactAPI> entityCollection = instance.findNodes(params);
        assertEquals(2, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "folder2", "f1"));

        params.setUpperExpirationDate(EXP_DATE3);
        entityCollection = instance.findNodes(params);

        assertEquals(01, entityCollection.size());
        assertTrue(check(entities2names(entityCollection), "f1"));
    }

    protected void initNodeTypes(NodeTypeManager ntm) throws RepositoryException {
        NodeTypeManagerImpl ntmi = (NodeTypeManagerImpl) ntm;

        try {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream("/org/openl/rules/repository/openl_nodetypes.xml");
                ntmi.registerNodeTypes(is, JackrabbitNodeTypeManager.TEXT_XML, true);
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
  //FIXME refactor to use AProjectArtefacts
    protected void setUp() throws Exception {
        FileUtils.deleteQuietly(new File(TEST_FOLDER));

        String repConf = "/jackrabbit-repository.xml";

        // obtain real path to repository configuration file
        URL url = this.getClass().getResource(repConf);
        String fullPath = url.toURI().getPath();

        repository = new TransientRepository(fullPath, TEST_FOLDER);

        Session session = createSession("admin", "admin", repository);
        instance = new JcrProductionRepository(session, new JackrabbitTransactionManager(session));
        initNodeTypes(session.getWorkspace().getNodeTypeManager());

        FolderAPI deployment = instance.createDeploymentProject("d1");
        FolderAPI rProject = deployment.addFolder("prj1");
        rProject.addProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS, ValueType.STRING, LOB_M);

        FolderAPI folder1 = rProject.addFolder("folder1");
        folder1.addProperty(ArtefactProperties.PROP_EFFECTIVE_DATE, ValueType.DATE, EFF_DATE1);
        folder1.addProperty(ArtefactProperties.PROP_EXPIRATION_DATE, ValueType.DATE, EXP_DATE1);

        FolderAPI folder2 = rProject.addFolder("folder2");
        folder2.addProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS, ValueType.STRING, LOB_M);
        folder2.addProperty(ArtefactProperties.PROP_EFFECTIVE_DATE, ValueType.DATE, EFF_DATE4);
        folder2.addProperty(ArtefactProperties.PROP_EXPIRATION_DATE, ValueType.DATE, EXP_DATE4);

        ArtefactAPI rFile1 = rProject.addResource("f1", new ByteArrayInputStream(new byte[]{}));
        rFile1.addProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS, ValueType.STRING, LOB_M);
        rFile1.addProperty(ArtefactProperties.PROP_EFFECTIVE_DATE, ValueType.DATE, EFF_DATE2);
        rFile1.addProperty(ArtefactProperties.PROP_EXPIRATION_DATE, ValueType.DATE, EXP_DATE2);

        ArtefactAPI rFile2 = rProject.addResource("f2", new ByteArrayInputStream(new byte[]{}));
        rFile2.addProperty(ArtefactProperties.PROP_LINE_OF_BUSINESS, ValueType.STRING, LOB_S);
        rFile2.addProperty(ArtefactProperties.PROP_EFFECTIVE_DATE, ValueType.DATE, EFF_DATE3);
        rFile2.addProperty(ArtefactProperties.PROP_EXPIRATION_DATE, ValueType.DATE, EXP_DATE3);
        deployment.commit(new CommonUserImpl("sys"), deployment.getVersion().getRevision() + 1);
    }

    @Override
    protected void tearDown() throws Exception {
        instance.release();
        instance = null;
        repository.shutdown();
        repository = null;
    }

    /**
     * Contains call to all actual test methods, that is done to speed up test
     * execution, as creating new repository in {@link #setUp()} is time
     * consuming.
     *
     * @throws RRepositoryException if an error occures
     */
    public void testIt() throws ProjectException, InterruptedException {
        _testLob();
        _testEffectiveDate();
        _testExpirationDate();
        _testSeveralProperties();

        _testListeners();
    }
}
