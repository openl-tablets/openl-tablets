package org.openl.rules.ruleservice.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.openl.rules.workspace.production.client.JcrRulesClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesWebServicesPublisher implements Runnable {
    private final Log log = LogFactory.getLog(getClass());

    private JcrRulesClient rulesClient;
    private File tempFolder;

    private Map<String, String> deployment2Version = new HashMap<String, String>();
    private Map<String, ClassLoader> deployment2ClassLoader = new HashMap<String, ClassLoader>();

    private DeployAdmin deployAdmin;

    public void setRulesClient(JcrRulesClient rulesClient) {
        this.rulesClient = rulesClient;
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

    private synchronized void deployRequired() throws RRepositoryException {
        Collection<String> deployments = rulesClient.getDeploymentNames();

        // computing latest versions of deployments
        Map<String, CommonVersion> versionMap = new HashMap<String, CommonVersion>();
        for (String deployment : deployments) {
            DeploymentInfo di = DeploymentInfo.valueOf(deployment);
            if (di == null)
                continue;

            CommonVersion version = versionMap.get(di.getName());

            if (version == null || di.getVersion().compareTo(version) > 0) {
                version = di.getVersion();
            }

            versionMap.put(di.getName(), version);
        }

        for (String deployment : deployments) {
            DeploymentInfo di = DeploymentInfo.valueOf(deployment);
            if (di != null && versionMap.get(di.getName()).equals(di.getVersion())) {
                deploy(di);
            }
        }
    }

    private synchronized void undeploy(DeploymentInfo di) {
        if (deployment2ClassLoader.remove(di.getName()) != null) {
            deployAdmin.undeploy(di.getName());
        }
    }


    private synchronized void deploy(DeploymentInfo di) {
        final String version = di.getVersion().getVersionName();
        if (version.equals(deployment2Version.get(di.getName()))) {
            return;
        }
        undeploy(di);

        try {
            File deploymentLocalFolder = downloadDeployment(di);

            Map<String, WSEntryPoint> openlWrappers = new HashMap<String, WSEntryPoint>();
            List<WSInfo> serviceClasses = new ArrayList<WSInfo>();
            List<URL> classPathURLs = new ArrayList<URL>();
            XlsFileRecognizer projectXls;

            for (File projectFolder : deploymentLocalFolder.listFiles()) {
                if (projectFolder.isDirectory()) {
                    final File projectGenFolder = new File(projectFolder, "gen");
                    final File binFolder = new File(projectFolder, "bin");

                    openlWrappers.clear();
                    FileSystemWalker.walk(projectGenFolder, new OpenLWrapperRecognizer(projectGenFolder, openlWrappers));
                    FileSystemWalker.walk(new File(projectFolder, "rules"), projectXls = new XlsFileRecognizer());

                    for (Map.Entry<String, WSEntryPoint> wsCandidate : openlWrappers.entrySet()) {
                        WSEntryPoint wsEntryPoint = wsCandidate.getValue();
                        String dif = wsEntryPoint.getFullFilename();

                        if (!new File(binFolder, FileSystemWalker.changeExtension(dif, "class")).exists()) continue;

                        String className = FileSystemWalker.removeExtension(dif).replaceAll("[/\\\\]", ".");
                        serviceClasses.add(new WSInfo(projectFolder, projectXls.getFile(),
                                className, wsCandidate.getKey(), wsEntryPoint.isInterface()));
                    }

                    addClasspathURL(classPathURLs, binFolder);
                }
            }

            URLClassLoader urlClassLoader = new URLClassLoader(classPathURLs.toArray(new URL[classPathURLs.size()]),
                    Thread.currentThread().getContextClassLoader());

            deployment2ClassLoader.put(di.getName(), urlClassLoader);
            deployment2Version.put(di.getName(), version);

            deployAdmin.deploy(di.getName(), urlClassLoader, serviceClasses);
        } catch (Exception e) {
            log.error("failed to deploy project " + di.getDeployID(), e);
        }
    }

    private void addClasspathURL(List<URL> classPathURLs, File folder) {
        try {
            classPathURLs.add(new URL("file:" + folder.getCanonicalPath() + "/"));
        } catch (IOException e) {
            log.error("could not create classpath URL", e);
        }
    }

    private File downloadDeployment(DeploymentInfo di) throws Exception {
        File deploymentFolder = new File(tempFolder, di.getName());

        rulesClient.fetchDeployment(di.getDeployID(), deploymentFolder);
        return deploymentFolder;
    }

    public void run() {
        try {
            deployRequired();
        } catch (RRepositoryException e) {
            log.error("exception deploying new items", e);
        }
    }
}

class WSEntryPoint {
    private String fullFilename;
    private boolean _interface;

    WSEntryPoint(String fullFilename, boolean _interface) {
        this.fullFilename = fullFilename;
        this._interface = _interface;
    }

    public String getFullFilename() {
        return fullFilename;
    }

    public boolean isInterface() {
        return _interface;
    }
}


class XlsFileRecognizer implements FileSystemWalker.Walker {
    private File file;
    public void process(File f) {
        if (file == null && f.isFile() && f.getName().endsWith(".xls") && !f.getPath().contains("include"))
            file = f;
    }

    File getFile() {
        return file;
    }
}

class OpenLWrapperRecognizer implements FileSystemWalker.Walker {
    private final Map<String, WSEntryPoint> entryPoints;
    private File baseFolder;

    OpenLWrapperRecognizer(File baseFolder, Map<String, WSEntryPoint> entryPoints) {
        this.baseFolder = baseFolder;
        this.entryPoints = entryPoints;
    }

    public void process(File file) {
        if (!file.isFile())
            return;

        String wsname = getNameWithoutEnding(file, "Wrapper.java");
        if (wsname != null) {
            if (!entryPoints.containsKey(wsname)) {
                entryPoints.put(wsname, new WSEntryPoint(difference(baseFolder, file), false));
            }
        } else if ((wsname = getNameWithoutEnding(file, "WrapperInterface.java")) != null) {
            entryPoints.put(wsname, new WSEntryPoint(difference(baseFolder, file), true));
        }
    }

    String getNameWithoutEnding(File file, String ending) {
        String filename = file.getName();
        if (filename.endsWith(ending)) {
            filename = difference(baseFolder, file);
            return filename.substring(0, filename.length() - ending.length()).replaceAll("[/\\\\]", ".");
        }
        return null;
    }

    static String difference(File parent, File child) {
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
}