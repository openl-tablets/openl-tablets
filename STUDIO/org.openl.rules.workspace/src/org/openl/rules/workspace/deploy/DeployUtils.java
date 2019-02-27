package org.openl.rules.workspace.deploy;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;

public final class DeployUtils {
    public static final String SEPARATOR = "#";
    public static final String API_VERSION_SEPARATOR = "_V";

    public static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private DeployUtils() {
    }

    public static Collection<Deployment> getLastDeploymentProjects(Repository repository, String deployPath) throws RRepositoryException {

        Map<String, Deployment> latestDeployments = new HashMap<>();
        Map<String, Integer> versionsList = new HashMap<>();

        Collection<FileData> fileDatas;
        try {
            if (repository.supports().folders()) {
                // All deployments
                fileDatas = ((FolderRepository) repository).listFolders(deployPath);
            } else {
                // Projects inside all deployments
                fileDatas = repository.list(deployPath);
            }
        } catch (IOException ex) {
            throw new RRepositoryException("Cannot read the deploy repository", ex);
        }
        for (FileData fileData : fileDatas) {
            String deploymentFolderName = fileData.getName().substring(deployPath.length()).split("/")[0];
            int separatorPosition = deploymentFolderName.lastIndexOf(SEPARATOR);

            String deploymentName = deploymentFolderName;
            Integer version = 0;
            CommonVersionImpl commonVersion;
            if (separatorPosition >= 0) {
                deploymentName = deploymentFolderName.substring(0, separatorPosition);
                version = Integer.valueOf(deploymentFolderName.substring(separatorPosition + 1));
                commonVersion = new CommonVersionImpl(version);
            } else {
                commonVersion = new CommonVersionImpl(fileData.getVersion());
            }
            Integer previous = versionsList.put(deploymentName, version);
            if (previous != null && previous > version) {
                // rollback
                versionsList.put(deploymentName, previous);
            } else {
                // put the latest deployment

                String folderPath = deployPath + deploymentFolderName;
                boolean folderStructure;
                try {
                    if (repository.supports().folders()) {
                        folderStructure = !((FolderRepository) repository).listFolders(folderPath + "/").isEmpty();
                    } else {
                        folderStructure = false;
                    }
                } catch (IOException e) {
                    throw new RRepositoryException(e.getMessage(), e);
                }
                Deployment deployment = new Deployment(repository,
                        folderPath,
                        deploymentName,
                        commonVersion,
                        folderStructure);
                latestDeployments.put(deploymentName, deployment);
            }
        }

        return latestDeployments.values();
    }

    public static int getNextDeploymentVersion(Repository repository, String project, String deployPath) throws RRepositoryException {
        Collection<Deployment> lastDeploymentProjects = getLastDeploymentProjects(repository, deployPath);
        int version = 1;
        String prefix = project + SEPARATOR;
        for (Deployment deployment : lastDeploymentProjects) {
            if (deployment.getName().startsWith(prefix)) {
                version = Integer.valueOf(deployment.getCommonVersion().getRevision()) + 1;
                break;
            }
        }
        return version;
    }

}
