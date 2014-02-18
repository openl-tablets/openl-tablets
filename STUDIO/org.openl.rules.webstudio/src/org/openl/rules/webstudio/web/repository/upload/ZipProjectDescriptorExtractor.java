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

public class ZipProjectDescriptorExtractor {
    private final Log log = LogFactory.getLog(ZipProjectDescriptorExtractor.class);

    private final PathFilter zipFilter;

    public ZipProjectDescriptorExtractor(PathFilter zipFilter) {
        this.zipFilter = zipFilter;
    }

    public ProjectDescriptor getProjectDescriptor(UploadedFile uploadedFile) {
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
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }

        return projectDescriptor;
    }
}
