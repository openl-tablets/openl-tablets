package org.openl.rules.project.abstraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileRepository;
import org.openl.util.IOUtils;

/**
 * Class representing deployment from ProductionRepository. Deployment is set of
 * logically grouped rules projects.
 *
 * @author PUdalau
 */
public class Deployment extends AProjectFolder {
    private Map<String, AProject> projects;

    private String deploymentName;
    private CommonVersion commonVersion;
    private final boolean folderStructure;

    @Deprecated
    public Deployment(Repository repository, String folderName, String deploymentName, CommonVersion commonVersion) {
        this(repository, folderName, deploymentName, commonVersion, true);
    }

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
        projects = new HashMap<String, AProject>();

        for (AProjectArtefact artefact : getArtefactsInternal().values()) {
            String projectPath = artefact.getArtefactPath().getStringValue();
            projects.put(artefact.getName(), new AProject(getRepository(), projectPath, folderStructure));
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
        if (getRepository() instanceof FileRepository) {
            FileRepository repository = (FileRepository) getRepository();
            File[] files = new File(repository.getRoot(), getFolderPath()).listFiles();
            Map<String, AProjectArtefact> result = new HashMap<String, AProjectArtefact>();
            if (files != null) {
                for (File file : files) {
                    String projectPath = getFolderPath() + "/" + file.getName();
                    if (file.isDirectory()) {
                        result.put(file.getName(), new AProject(repository, projectPath, true));
                    } else {
                        InputStream is = null;
                        try {
                            is = new FileInputStream(file);
                            // Check that the file is zip
                            if (new ZipInputStream(is).getNextEntry() != null) {
                                result.put(file.getName(), new AProject(repository, projectPath, false));
                            }
                        } catch (IOException ignored) {
                            // Not a zip. Such a file isn't a project, skip it
                        } finally {
                            IOUtils.closeQuietly(is);
                        }
                    }
                }
            }
            return result;
        } else {
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
                AProject newProject = new AProject(getRepository(), getFolderPath() + "/" + name, folderStructure);
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
