package org.openl.rules.repository.jcr;

import static java.lang.System.out;

import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;

/*
 import com.exigen.cm.RepositoryProvider;
 */

public class BigNodes {
    public static final String TEST_PATH = "test-BigNodes";

    private static final long _1M = 1024 * 1024;
    private Repository repository;

    /*
     * protected void startExigen() throws RepositoryException { out.println(">>
     * Starting Exigne JCR"); repository =
     * RepositoryProvider.getInstance().getRepository(); }
     */

    private Session session;

    public static final void main(String[] args) {
        out.println(">> Start");

        BigNodes test = new BigNodes();

        try {
            /*
             * test.startExigen(); test.createSession("user", "pass");
             * test.run();
             */

            // test.startJackrabbit();
            // test.createSession("user", "pass");
            // test.run();
            // test.stopJackrabbit();
        } catch (Exception e) {
            e.printStackTrace(out);
        }

        out.println(">> Done");
    }

    public static final String memStats() {
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        long max = Runtime.getRuntime().maxMemory();

        long f = free / _1M;
        long t = total / _1M;
        long m = max / _1M;

        return ("mem=" + f + "/" + t + "/" + m + " M");
    }

    protected void checkPath() throws RepositoryException {
        Node root = session.getRootNode();

        out.println("> Checking '" + TEST_PATH + "' ...");
        if (root.hasNode(TEST_PATH)) {
            out.println("Exists. Re-creating...");
            Node n = root.getNode(TEST_PATH);
            n.remove();
            root.save();
            root.addNode(TEST_PATH);
            root.save();
        } else {
            out.println("Absent. Creating...");
            root.addNode(TEST_PATH);
            root.save();
        }
    }

    protected void createNodes(int count) throws RepositoryException {
        Node root = session.getRootNode();
        Node path = root.getNode(TEST_PATH);

        out.println("> Creating project...");
        Node project = path.addNode("project");
        path.save();

        out.println("> Creating " + count + " folders in project...");
        for (int i = 0; i < count; i++) {
            String name = "f-" + i;

            out.println("+ folder " + name + "... \t" + memStats());

            Node n = project.addNode(name);
            project.save();
        }
    }

    protected void createSession(String user, String pass) throws RepositoryException {
        char[] password = pass.toCharArray();
        SimpleCredentials sc = new SimpleCredentials(user, password);
        session = repository.login(sc);
    }

    protected void run() throws RepositoryException {
        checkPath();
        createNodes(200);
    }

    protected void startJackrabbit() throws RepositoryException {
        out.println(">> Starting Jackrabbit JCR");
        // obtain real path to repository configuration file
        URL url = this.getClass().getResource("/jackrabbit-repository.xml");
        String fullPath = url.getFile();

        repository = new TransientRepository(fullPath, "/tmp/local-repository");
    }

    protected void stopJackrabbit() {
        out.println(">> Stopping Jackrabbit JCR");
        TransientRepository jackrabbit = (TransientRepository) repository;
        jackrabbit.shutdown();
    }
}
