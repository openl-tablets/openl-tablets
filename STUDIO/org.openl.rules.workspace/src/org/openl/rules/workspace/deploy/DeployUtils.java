package org.openl.rules.workspace.deploy;

import java.util.*;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;

public final class DeployUtils {
    public static final String SEPARATOR = "#";

    public static final String DEPLOY_PATH = "deploy/";

    private DeployUtils() {
    }

    public static Collection<Deployment> getLastDeploymentProjects(Repository repository) throws RRepositoryException {

        Map<String, Deployment> latestDeployments = new HashMap<String, Deployment>();
        Map<String, String> versionsList = new HashMap<String, String>();

        Collection<FileData> fileDatas = repository.list(DEPLOY_PATH);
        Set<String> deploymentFolderNames = new HashSet<String>();
        for (FileData fileData : fileDatas) {
            String deploymentName = fileData.getName().substring(DEPLOY_PATH.length()).split("/")[0];
            deploymentFolderNames.add(deploymentName);
        }

        List<Deployment> deployments = new ArrayList<Deployment>();
        for (String deploymentFolderName : deploymentFolderNames) {
            int separatorPosition = deploymentFolderName.lastIndexOf(SEPARATOR);

            if (separatorPosition >= 0) {
                String deploymentName = deploymentFolderName.substring(0, separatorPosition);
                String version = deploymentFolderName.substring(separatorPosition + 1);
                CommonVersionImpl commonVersion = new CommonVersionImpl(version);
                deployments.add(new Deployment(repository, DEPLOY_PATH + deploymentFolderName, deploymentName, commonVersion));
            } else {
                deployments.add(new Deployment(repository, DEPLOY_PATH + deploymentFolderName, deploymentFolderName, null));
            }
        }
        for (Deployment deployment : deployments) {
            String deploymentName = deployment.getDeploymentName();
            String versionNum = deployment.getCommonVersion().getRevision();
            if (versionNum == null) {
                versionNum = "0";
            }

            if (versionsList.containsKey(deploymentName)) {
                if (versionNum.compareTo(versionsList.get(deploymentName)) > 0) {
                    versionsList.put(deploymentName, versionNum);
                    latestDeployments.put(deploymentName, deployment);
                }
            } else {
                versionsList.put(deploymentName, versionNum);
                latestDeployments.put(deploymentName, deployment);
            }
        }

        return latestDeployments.values();
    }

    public static int getNextDeploymentVersion(Repository repository, ADeploymentProject project) throws RRepositoryException {
        Collection<Deployment> lastDeploymentProjects = getLastDeploymentProjects(repository);
        int version = 0;
        String prefix = project.getName() + "#";
        for (Deployment deployment : lastDeploymentProjects) {
            if (deployment.getName().startsWith(prefix)) {
                version = Integer.valueOf(deployment.getCommonVersion().getRevision()) + 1;
                break;
            }
        }
        return version;
    }
}
