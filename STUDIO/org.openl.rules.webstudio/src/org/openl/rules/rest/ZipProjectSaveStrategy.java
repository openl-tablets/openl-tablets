package org.openl.rules.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.openl.rules.repository.api.AdditionalData;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.folder.FileChangesFromFolder;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ZipProjectSaveStrategy {

    private final DesignTimeRepository designTimeRepository;
    private final PathFilter zipFilter;
    private final ZipCharsetDetector zipCharsetDetector;

    @Inject
    public ZipProjectSaveStrategy(DesignTimeRepository designTimeRepository,
            @Qualifier("zipFilter") PathFilter zipFilter,
            ZipCharsetDetector zipCharsetDetector) {
        this.designTimeRepository = designTimeRepository;
        this.zipFilter = zipFilter;
        this.zipCharsetDetector = zipCharsetDetector;
    }

    public FileData save(CreateUpdateProjectModel model, Path zipArchive) throws IOException {
        Repository repository = designTimeRepository.getRepository(model.getRepoName());

        FileData projectData = new FileData();
        projectData.setName(designTimeRepository.getRulesLocation() + model.getProjectName());
        projectData.setComment(StringUtils.trimToEmpty(model.getComment()));
        projectData.setAuthor(model.getAuthor());
        if (repository.supports().mappedFolders()) {
            AdditionalData<FileMappingData> additionalData = new FileMappingData(projectData.getName(),
                model.getPath() + model.getProjectName());
            projectData.addAdditionalData(additionalData);
        }
        ProjectDescriptorNameAdaptor adaptor = new ProjectDescriptorNameAdaptor(model.getProjectName());
        Predicate<Path> filter = p -> zipFilter.accept(p.toString());
        Charset charset = zipCharsetDetector.detectCharset(() -> Files.newInputStream(zipArchive));
        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(zipArchive),
            Collections.singletonMap("encoding", charset.name()))) {

            final Path root = fs.getPath("/");
            if (repository.supports().folders()) {
                try (FileChangesFromFolder changes = new FileChangesFromFolder(root,
                    projectData.getName(),
                    filter,
                    adaptor)) {
                    return ((FolderRepository) repository).save(projectData, changes, ChangesetType.FULL);
                }
            } else {
                Path tmp = Files.createTempFile(FileUtils.getBaseName(projectData.getName()), ".zip");
                try {
                    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
                        try (FileChangesFromFolder changes = new FileChangesFromFolder(root, filter, adaptor)) {
                            for (FileItem fileItem : changes) {
                                String name = fileItem.getData().getName();
                                if (name.charAt(0) == '/') {
                                    name = name.substring(1);
                                }
                                ZipEntry entry = new ZipEntry(name);
                                zos.putNextEntry(entry);
                                InputStream is = fileItem.getStream();
                                if (is != null) {
                                    IOUtils.copy(is, zos);
                                    IOUtils.closeQuietly(is);
                                }
                            }
                        }
                    }
                    try (InputStream is = Files.newInputStream(tmp)) {
                        repository.save(projectData, is);
                        return repository.check(projectData.getName());
                    }
                } finally {
                    FileUtils.delete(tmp);
                }
            }
        }
    }
}
