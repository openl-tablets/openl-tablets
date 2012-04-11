package org.openl.rules.repository.jcr;

import static java.lang.System.out;

import java.io.IOException;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.core.TransientRepository;

/*
 import com.exigen.cm.RepositoryProvider;
 */

public class CheckObservation {
    private class EL implements EventListener {
        public void onEvent(EventIterator ei) {
            eventFired = true;

            while (ei.hasNext()) {
                Event e = ei.nextEvent();
                String path;

                try {
                    path = e.getPath();
                } catch (Exception ex) {
                    // Oops...
                    out.println("* onEvent exception: " + ex.getMessage());
                    continue;
                }

                int type = e.getType();
                String msg;
                switch (type) {
                    case Event.NODE_ADDED:
                        msg = "node was added";
                        break;
                    case Event.NODE_REMOVED:
                        msg = "node was removed";
                        break;
                    case Event.PROPERTY_ADDED:
                        msg = "property was added";
                        break;
                    case Event.PROPERTY_REMOVED:
                        msg = "property was removed";
                        break;
                    case Event.PROPERTY_CHANGED:
                        msg = "propery was changed";
                        break;
                    default:
                        msg = "unknown";
                }

                out.println("  Event: " + msg + ": " + path);
            }
        }
    }

    public static final String TEST_PATH = "test-CheckObservation";
    private static boolean eventFired;

    /*
     * protected void startExigen() throws RepositoryException { out.println(">>
     * Starting Exigne JCR"); repository =
     * RepositoryProvider.getInstance().getRepository(); }
     */

    private Repository repository;

    private Session session;

    public static final void main(String[] args) {
        out.println(">> Start");

        CheckObservation test = new CheckObservation();

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

        if (!eventFired) {
            out.println("* WARNING: No events were fired!!!");
        }

        out.println(">> Done");
    }

    protected void checkPath() throws RepositoryException {
        Node root = session.getRootNode();

        ObservationManager om = session.getWorkspace().getObservationManager();
        om.addEventListener(new EL(), Event.NODE_ADDED | Event.NODE_REMOVED, "/" + TEST_PATH, false, null, null, false);
        om.addEventListener(new EL(), Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, "/" + TEST_PATH + "/project",
                false, null, null, false);

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

            out.println("+ folder " + name + "...");

            Node n = project.addNode(name);
            n.setProperty("prop-count", i);
            project.save();
        }

        out.println("> Setting project's revision (property)...");
        project.setProperty("revision", 103);
        project.save();
        out.println("> Updating project's revision (property)...");
        project.setProperty("revision", 105);
        project.save();
    }

    protected void createSession(String user, String pass) throws RepositoryException {
        char[] password = pass.toCharArray();
        SimpleCredentials sc = new SimpleCredentials(user, password);
        session = repository.login(sc);
    }

    protected void run() throws RepositoryException {
        checkPath();
        createNodes(10);

        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    protected void startJackrabbit() throws RepositoryException {
        out.println(">> Starting Jackrabbit JCR");
        try {
            // obtain real path to repository configuration file
            URL url = this.getClass().getResource("/jackrabbit-repository.xml");
            String fullPath = url.getFile();

            repository = new TransientRepository(fullPath, "/tmp/local-repository");
        } catch (IOException e) {
            // TODO: log
            throw new RepositoryException("Failed to init: " + e.getMessage(), e);
        }
    }

    protected void stopJackrabbit() {
        out.println(">> Stopping Jackrabbit JCR");
        TransientRepository jackrabbit = (TransientRepository) repository;
        jackrabbit.shutdown();
    }
}
