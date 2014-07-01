package org.openl.rules.webstudio.web.repository.upload;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.workspace.filter.PathFilter;
import org.richfaces.model.UploadedFile;

import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ZipProjectDescriptorExtractor {
    private ZipProjectDescriptorExtractor() {
    }

    public static ProjectDescriptor getProjectDescriptor(UploadedFile uploadedFile, PathFilter zipFilter) {
        ProjectDescriptor projectDescriptor = null;
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(uploadedFile.getInputStream());
            Set<String> names = new TreeSet<String>();
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                names.add(zipEntry.getName());
            }
            zipInputStream.close();

            RootFolderExtractor folderExtractor = new RootFolderExtractor(names, zipFilter);
            for (String name : names) {
                String fileName = folderExtractor.extractFromRootFolder(name);
                if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(fileName)) {
                    zipInputStream = new ZipInputStream(uploadedFile.getInputStream());
                    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                        if (zipEntry.getName().equals(name)) {
                            projectDescriptor = new XmlProjectDescriptorSerializer(false).deserialize(zipInputStream);
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Log log = LogFactory.getLog(ZipProjectDescriptorExtractor.class);
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }

        return projectDescriptor;
    }
}
