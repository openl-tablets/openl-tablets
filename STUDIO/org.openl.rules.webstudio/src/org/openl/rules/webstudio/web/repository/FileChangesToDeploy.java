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
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileChangesToDeploy implements Iterable<FileItem>, Closeable {
    private final Logger log = LoggerFactory.getLogger(FileChangesToDeploy.class);
    private final DesignTimeRepository designRepo;
    private final List<ProjectDescriptor> descriptors;
    private final String rulesPath;
    private final String deploymentPath;

    private InputStream openedStream;

    FileChangesToDeploy(Collection<ProjectDescriptor> projectDescriptors,
            DesignTimeRepository designRepo,
            String rulesPath,
            String deploymentPath) {
        this.descriptors = new ArrayList<>(projectDescriptors);
        this.designRepo = designRepo;
        this.rulesPath = rulesPath;
        this.deploymentPath = deploymentPath;
    }

    @Override
    public Iterator<FileItem> iterator() {
        return new Iterator<FileItem>() {
            private int descriptorIndex = 0;
            private Iterator<FileItem> projectIterator;

            @Override
            public boolean hasNext() {
                if (projectIterator != null && projectIterator.hasNext()) {
                    return true;
                }

                if (descriptorIndex < descriptors.size()) {
                    ProjectDescriptor<?> pd = descriptors.get(descriptorIndex++);
                    String repositoryId = pd.getRepositoryId();
                    if (repositoryId == null) {
                        repositoryId = designRepo.getRepositories().get(0).getId();
                    }
                    Repository repository = designRepo.getRepository(repositoryId);
                    String version = pd.getProjectVersion().getVersionName();
                    String projectName = pd.getProjectName();
                    projectIterator = getProjectIterator(repository, projectName, version);
                    return projectIterator != null && projectIterator.hasNext();
                } else {
                    return false;
                }
            }

            private Iterator<FileItem> getProjectIterator(Repository baseRepo, String projectName, String version) {
                try {
                    if (baseRepo.supports().folders()) {
                        // Project in design repository is stored as a folder
                        String srcProjectPath = rulesPath + projectName + "/";
                        FolderRepository repository = RepositoryUtils
                            .getRepositoryForVersion((FolderRepository) baseRepo, rulesPath, projectName, version);
                        List<FileData> files = repository.listFiles(srcProjectPath, version);
                        if (files.isEmpty()) {
                            log.warn("Cannot find files in project {}", projectName);
                        }
                        return new FolderIterator(repository, files);
                    } else {
                        // Project in design repository is stored as a zip file
                        FileItem srcPrj = baseRepo.readHistory(rulesPath + projectName, version);
                        if (srcPrj == null) {
                            throw new FileNotFoundException(String
                                .format("File '%s' for version %s is not found.", rulesPath + projectName, version));
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
            public FileItem next() {
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

    private class FolderIterator implements Iterator<FileItem> {
        private final Repository baseRepo;
        private final List<FileData> files;
        private int fileIndex = 0;

        private FolderIterator(Repository baseRepo, List<FileData> files) {
            this.baseRepo = baseRepo;
            this.files = files;
        }

        @Override
        public boolean hasNext() {
            return fileIndex < files.size();
        }

        @Override
        public FileItem next() {
            FileData file = files.get(fileIndex++);
            String srcFileName = file.getName();
            String fileTo = deploymentPath + srcFileName.substring(rulesPath.length());
            FileItem fileItem;
            try {
                fileItem = baseRepo.readHistory(file.getName(), file.getVersion());
                IOUtils.closeQuietly(openedStream);
                openedStream = fileItem.getStream();
                return new FileItem(fileTo, openedStream);
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
