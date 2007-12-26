package org.openl.rules.ruleservice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.production.client.JcrRulesClient;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesWebServicesPublisher implements RDeploymentListener {
    private Log log = LogFactory.getLog(getClass()); 

    private JcrRulesClient client;
    private File tempFolder;
    
    private Map<String, String> deployment2Version = new HashMap<String, String>();
    private Map<String, ClassLoader> deployment2ClassLoader = new HashMap<String, ClassLoader>();

    private DeployAdmin deployAdmin;
    
    public RulesWebServicesPublisher() {
        client = new JcrRulesClient();
    }

    public void setTempFolder(File tempFolder) {
        this.tempFolder = tempFolder;
        if (tempFolder != null) {
            tempFolder.mkdirs();
            FolderHelper.clearFolder(tempFolder);
        }
    }

    public void setDeployAdmin(DeployAdmin deployAdmin) {
        this.deployAdmin = deployAdmin;
    }

    public void init() throws RRepositoryException {
        client.addListener(this);
        deployRequired();
    }

    private synchronized void deployRequired() throws RRepositoryException {
        Collection<String> deployments = client.getDeploymentNames();
        for (String deployment : deployments) {
            deploy(deployment);
        }
    }

    private synchronized void undeploy(DeploymentInfo di) {
        if (deployment2ClassLoader.remove(di.getName()) != null) {
            deployAdmin.undeploy(di.getName());
        }
    }


    private synchronized void deploy(String deployment) {
        DeploymentInfo di = DeploymentInfo.valueOf(deployment);

        final String version = di.getVersion();
        if (version == null || version.equals(deployment2Version.get(di.getName()))) {
            return;
        }
        undeploy(di);

        try {
            File deploymentLocalFolder = downloadDeployment(di);

            List<File> openlWrappers = new ArrayList<File>();
            List<WSInfo> serviceClasses = new ArrayList<WSInfo>();
            List<URL> classPathURLs = new ArrayList<URL>();

            for (File projectFolder : deploymentLocalFolder.listFiles()) {
                if (projectFolder.isDirectory()) {
                    final File projectGenFolder = new File(projectFolder, "gen");
                    final File binFolder = new File(projectFolder, "bin");

                    openlWrappers.clear();
                    FileSystemWalker.walk(projectGenFolder, new OpenLWrapperRecognizer(openlWrappers));

                    for (File wsCandidate : openlWrappers) {
                        String dif = difference(projectGenFolder, wsCandidate);
                        if (dif == null) continue;

                        if (!new File(binFolder, FileSystemWalker.changeExtension(dif, "class")).exists()) continue;

                        String className = FileSystemWalker.removeExtension(dif).replaceAll("[/\\\\]", ".");
                        serviceClasses.add(new WSInfo(projectFolder, className));
                    }

                    addClasspathURL(classPathURLs, binFolder);
                }
            }

            URLClassLoader urlClassLoader = new URLClassLoader(classPathURLs.toArray(new URL[classPathURLs.size()]),
                    Thread.currentThread().getContextClassLoader());
            
            deployment2ClassLoader.put(di.getName(), urlClassLoader);
            deployment2Version.put(di.getName(), di.getVersion());

            deployAdmin.deploy(di.getName(), urlClassLoader, serviceClasses);
        } catch (Exception e) {
            log.error("failed to deploy project " + deployment, e);
        }
    }

    private void addClasspathURL(List<URL> classPathURLs, File folder) {
        try {
            classPathURLs.add(new URL("file:" + folder.getCanonicalPath() + "/"));
        } catch (IOException e) {
            log.error("could not create classpath URL", e);
        }
    }

    private static String difference(File parent, File child) {
        try {
            String parentStr = parent.getCanonicalPath();
            String childStr = child.getCanonicalPath();

            if (!childStr.startsWith(parentStr)) {
                return null;
            }

            return childStr.substring(parentStr.length() + 1);
        } catch (IOException e) {
            return null;
        }
    }

    private File downloadDeployment(DeploymentInfo di) throws Exception {
        File deploymentFolder = new File(tempFolder, di.getName());

        client.fetchDeployment(di.getDeployID(), deploymentFolder);
        return deploymentFolder;
    }

    public void destroy() throws RRepositoryException {
        client.release();
    }

    public void projectsAdded() {
        try {
            deployRequired();
        } catch (RRepositoryException e) {
            log.error("exception deploying new items", e);
        }
    }
}

class ExtensionFilter implements FileFilter {
    private final String ext;

    ExtensionFilter(String ext) {
        this.ext = "." + ext;
    }

    static ExtensionFilter CLASS_FILTER = new ExtensionFilter("class");
    static ExtensionFilter EXCEL_FILTER = new ExtensionFilter("xls");

    /**
     * Tests whether or not the specified abstract pathname should be
     * included in a pathname list.
     *
     * @param pathname The abstract pathname to be tested
     * @return <code>true</code> if and only if <code>pathname</code>
     *         should be included
     */
    public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().endsWith(ext);
    }
}

class OpenLWrapperRecognizer implements FileSystemWalker.Walker {
    private Collection matching;

    OpenLWrapperRecognizer(Collection matching) {
        this.matching = matching;
    }

    public void process(File file) {
        if (file.isFile() && file.getName().endsWith("Wrapper.java")) {
            matching.add(file);
        }
    }
}