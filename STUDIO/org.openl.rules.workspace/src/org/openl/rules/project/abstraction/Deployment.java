package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.*;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class representing deployment from ProductionRepository. Deployment is set of
 * logically grouped rules projects.
 *
 * @author PUdalau
 */
public class Deployment extends AProjectFolder {
    private final Logger log = LoggerFactory.getLogger(Deployment.class);
    private Map<String, AProject> projects;

    private String deploymentName;
    private CommonVersion commonVersion;
    private final boolean folderStructure;

    public Deployment(Repository repository,
            String folderName,
            String deploymentName,
            CommonVersion commonVersion,
            boolean folderStructure) {
        super(null, repository, folderName, commonVersion == null ? null : commonVersion.getVersionName());
        this.folderStructure = folderStructure;
        init();
        this.commonVersion = commonVersion;
        this.deploymentName = deploymentName;
    }

    public CommonVersion getCommonVersion() {
        if (commonVersion == null)
            return this.getVersion();
        return commonVersion;
    }

    public String getDeploymentName() {
        if (deploymentName == null)
            return this.getName();
        return deploymentName;
    }

    @Override
    public void refresh() {
        init();
    }

    private void init() {
        super.refresh();
        projects = new HashMap<>();

        for (AProjectArtefact artefact : getArtefactsInternal().values()) {
            String projectPath = artefact.getArtefactPath().getStringValue();
            AProject project = new AProject(getRepository(), projectPath);
            project.overrideFolderStructure(folderStructure);
            projects.put(artefact.getName(), project);
        }
    }

    public Collection<AProject> getProjects() {
        return projects.values();
    }

    public AProject getProject(String name) {
        return projects.get(name);
    }

    @Override
    public ProjectVersion getVersion() {
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(null, null);
        return new RepositoryProjectVersionImpl(commonVersion, rvii);
    }

    @Override
    protected Map<String, AProjectArtefact> createInternalArtefacts() {
        if (getRepository() instanceof FolderRepository) {
            FolderRepository repository = (FolderRepository) getRepository();
            List<FileData> fileDataList;
            try {
                if (folderStructure) {
                    fileDataList = repository.listFolders(getFolderPath());
                } else {
                    fileDataList = repository.list(getFolderPath());
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return Collections.emptyMap();
            }

            Map<String, AProjectArtefact> result = new HashMap<>();
            for (FileData file : fileDataList) {
                AProject project = new AProject(repository, file);
                project.overrideFolderStructure(folderStructure);
                result.put(file.getName(), project);
            }
            return result;
        } else {
            // In this case unpacked projects aren't supported. Projects should be archived to zip.
            return super.createInternalArtefacts();
        }
    }

    @Override
    public boolean isHistoric() {
        return false;
    }

    @Override
    public void update(AProjectArtefact newFolder, CommonUser user) throws ProjectException {
        Deployment other = (Deployment) newFolder;
        // add new
        for (AProject otherProject : other.getProjects()) {
            String name = otherProject.getName();
            if (!hasArtefact(name)) {
                AProject newProject = new AProject(getRepository(), getFolderPath() + "/" + name);
                newProject.overrideFolderStructure(folderStructure);
                newProject.update(otherProject, user);
                projects.put(newProject.getName(), newProject);
            }
        }
    }

    @Override
    public String getInternalPath() {
        throw new UnsupportedOperationException("Internal path for deployment has no meaning");
    }
}
