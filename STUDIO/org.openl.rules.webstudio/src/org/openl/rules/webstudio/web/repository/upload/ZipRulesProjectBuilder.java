package org.openl.rules.webstudio.web.repository.upload;

import java.io.InputStream;

import lombok.AccessLevel;
import lombok.Getter;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;

/**
 * Project builder for projects uploaded as zip files.
 *
 * @author DLiauchuk
 */
public class ZipRulesProjectBuilder extends RulesProjectBuilder {
    private final PathFilter filter;
    @Getter(AccessLevel.PACKAGE)
    private final RootFolderExtractor folderExtractor;

    ZipRulesProjectBuilder(UserWorkspace workspace,
                           Repository repository,
                           String projectName,
                           String projectFolder,
                           PathFilter filter,
                           RootFolderExtractor folderExtractor,
                           String comment) {
        super(workspace, repository, projectName, projectFolder, comment);
        this.filter = filter;
        this.folderExtractor = folderExtractor;
    }

    @Override
    public boolean addFile(String fileName, InputStream inputStream) throws ProjectException {
        var filteredFileName = folderExtractor.extractFromRootFolder(fileName);
        if (filteredFileName != null) {
            if (filter != null && !filter.accept(filteredFileName)) {
                return false;
            }
            return super.addFile(filteredFileName, inputStream);
        }
        return false;

    }

    @Override
    public boolean addFolder(String folderName) throws ProjectException {
        var filteredFolderName = folderExtractor.extractFromRootFolder(folderName);
        if (filteredFolderName != null) {
            if (!filter.accept(filteredFolderName)) {
                return false;
            }
            return super.addFolder(filteredFolderName);
        }
        return false;

    }
}
