package org.openl.rules.project.abstraction;

import java.io.InputStream;
import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileItem;

/**
 * @author NSamatov.
 */
public interface ResourceTransformer {
    InputStream transform(AProjectResource resource) throws ProjectException;
    List<FileItem> transformChangedFiles(String rootPath, List<FileItem> changes);
}
