package org.openl.rules.webstudio.web.repository.upload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.web.repository.upload.zip.ProjectDescriptorFinder;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipWalker;
import org.openl.rules.workspace.filter.PathFilter;
import org.richfaces.model.UploadedFile;

public final class ZipProjectDescriptorExtractor {
    private ZipProjectDescriptorExtractor() {
    }

    public static ProjectDescriptor getProjectDescriptor(UploadedFile uploadedFile, PathFilter zipFilter) {
        try {
            ZipWalker zipWalker = new ZipWalker(uploadedFile, zipFilter);
            ProjectDescriptorFinder finder = new ProjectDescriptorFinder();
            zipWalker.iterateEntries(finder);
            return finder.getProjectDescriptor();
        } catch (Exception e) {
            Log log = LogFactory.getLog(ZipProjectDescriptorExtractor.class);
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    }

}
