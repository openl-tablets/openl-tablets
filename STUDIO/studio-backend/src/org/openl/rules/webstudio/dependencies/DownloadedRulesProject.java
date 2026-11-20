package org.openl.rules.webstudio.dependencies;

import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.workspace.WorkspaceUser;

/**
 * An OpenL project downloaded from the Maven repository, opened read-only where it is unpacked.
 *
 * <p>It is named as its {@code rules.xml} names it, as a dependency names it, not by the folder it is unpacked into.
 *
 * @author Yury Molchan
 */
final class DownloadedRulesProject extends RulesProject {

    private final String name;

    /**
     * @param user       the user of the session that opened it
     * @param repository the repository holding the folder of the project
     * @param descriptor the project as its {@code rules.xml} describes it
     * @param lockEngine the locks of the projects, which a read-only project never takes
     */
    DownloadedRulesProject(WorkspaceUser user,
                           DownloadedProjectRepository repository,
                           ProjectDescriptor descriptor,
                           LockEngine lockEngine) {
        super(user, repository, folderOf(descriptor), null, null, lockEngine);
        this.name = descriptor.getName();
    }

    private static FileData folderOf(ProjectDescriptor descriptor) {
        var fileData = new FileData();
        fileData.setName(descriptor.getProjectFolder().getFileName().toString());
        return fileData;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBusinessName() {
        return name;
    }

    @Override
    public String getMainBusinessName() {
        return name;
    }
}
