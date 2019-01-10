package org.openl.rules.webstudio.web.repository;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.repository.api.*;
import org.openl.rules.repository.folder.FileChangesFromZip;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileChangesToDeploy implements Iterable<FileChange>, Closeable {
    private final Logger log = LoggerFactory.getLogger(FileChangesToDeploy.class);
    private final Repository designRepo;
    private final List<ProjectDescriptor> descriptors;
    private final String rulesPath;
    private final String deploymentPath;

    private InputStream openedStream;

    FileChangesToDeploy(Collection<ProjectDescriptor> projectDescriptors,
            Repository designRepo,
            String rulesPath,
            String deploymentPath) {
        this.descriptors = new ArrayList<>(projectDescriptors);
        this.designRepo = designRepo;
        this.rulesPath = rulesPath;
        this.deploymentPath = deploymentPath;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<FileChange> iterator() {
        return new Iterator<FileChange>() {
            private int descriptorIndex = 0;
            private Iterator<FileChange> projectIterator;

            @Override
            public boolean hasNext() {
                if (projectIterator != null && projectIterator.hasNext()) {
                    return true;
                }

                if (descriptorIndex < descriptors.size()) {
                    ProjectDescriptor<?> pd = descriptors.get(descriptorIndex++);
                    String version = pd.getProjectVersion().getVersionName();
                    String projectName = pd.getProjectName();
                    projectIterator = getProjectIterator(projectName, version);
                    return projectIterator != null && projectIterator.hasNext();
                } else {
                    return false;
                }
            }

            private Iterator<FileChange> getProjectIterator(String projectName, String version) {
                try {
                    if (designRepo instanceof FolderRepository) {
                        // Project in design repository is stored as a folder
                        String srcProjectPath = rulesPath + projectName + "/";
                        return new FolderIterator(((FolderRepository) designRepo).listFiles(srcProjectPath, version));
                    } else {
                        // Project in design repository is stored as a zip file
                        FileItem srcPrj = designRepo.readHistory(rulesPath + projectName, version);
                        if (srcPrj == null) {
                            throw new FileNotFoundException("File '" + rulesPath + projectName + "' for version " + version + " is not found");
                        }
                        IOUtils.closeQuietly(openedStream);
                        ZipInputStream stream = new ZipInputStream(srcPrj.getStream());
                        openedStream = stream;
                        return new FileChangesFromZip(stream, deploymentPath + projectName).iterator();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }

            @Override
            public FileChange next() {
                return projectIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported");
            }
        };
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(openedStream);
        openedStream = null;
    }

    private class FolderIterator implements Iterator<FileChange> {
        private final List<FileData> files;
        private int fileIndex = 0;

        public FolderIterator(List<FileData> files) {
            this.files = files;
        }

        @Override
        public boolean hasNext() {
            return fileIndex < files.size();
        }

        @Override
        public FileChange next() {
            FileData file = files.get(fileIndex++);
            String srcFileName = file.getName();
            String fileTo = deploymentPath + srcFileName.substring(rulesPath.length());
            FileItem fileItem;
            try {
                fileItem = designRepo.readHistory(file.getName(), file.getVersion());
                IOUtils.closeQuietly(openedStream);
                openedStream = fileItem.getStream();
                return new FileChange(fileTo, openedStream);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }

        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported");
        }
    }
}
