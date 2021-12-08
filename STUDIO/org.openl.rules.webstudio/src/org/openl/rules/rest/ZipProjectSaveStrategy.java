package org.openl.rules.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.api.AdditionalData;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.folder.FileChangesFromFolder;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.webstudio.service.UserManagementService;
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
    private final UserManagementService userManagementService;

    @Inject
    public ZipProjectSaveStrategy(DesignTimeRepository designTimeRepository,
            @Qualifier("zipFilter") PathFilter zipFilter,
            ZipCharsetDetector zipCharsetDetector,
            UserManagementService userManagementService) {
        this.designTimeRepository = designTimeRepository;
        this.zipFilter = zipFilter;
        this.zipCharsetDetector = zipCharsetDetector;
        this.userManagementService = userManagementService;
    }

    public FileData save(CreateUpdateProjectModel model, Path zipArchive) throws IOException {
        Repository repository = designTimeRepository.getRepository(model.getRepoName());
        UserInfo author = Optional.ofNullable(userManagementService.getUser(model.getAuthor()))
            .map(user -> new UserInfo(user.getUsername(), user.getEmail(), user.getDisplayName()))
            .orElse(new UserInfo(model.getAuthor()));
        FileData projectData = new FileData();
        projectData.setName(designTimeRepository.getRulesLocation() + model.getProjectName());
        projectData.setComment(StringUtils.trimToEmpty(model.getComment()));
        projectData.setAuthor(author);
        if (repository.supports().mappedFolders()) {
            AdditionalData<FileMappingData> additionalData = new FileMappingData(projectData.getName(),
                model.getFullPath());
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
                    if (checkIfRequiredProjectDescriptorCreation(model, root)) {
                        FileItem descriptor = createVirtualProjectDescriptor(model, projectData.getName());
                        Iterable<FileItem> files = () -> concat(changes, Stream.of(descriptor)).iterator();
                        return ((FolderRepository) repository).save(projectData, files, ChangesetType.FULL);
                    } else {
                        return ((FolderRepository) repository).save(projectData, changes, ChangesetType.FULL);
                    }
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
                    FileUtils.deleteQuietly(tmp);
                }
            }
        }
    }

    private FileItem createVirtualProjectDescriptor(CreateUpdateProjectModel model, String folderTo) {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setName(model.getProjectName());
        XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
        final byte[] bytes = serializer.serialize(descriptor).getBytes(StandardCharsets.UTF_8);

        String name = folderTo + "/" + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME;
        return new FileItem(name, new ByteArrayInputStream(bytes));
    }

    private boolean checkIfRequiredProjectDescriptorCreation(CreateUpdateProjectModel model, Path projectRoot) {
        Path p = Paths.get(model.getFullPath());
        String folderName = p.getName(p.getNameCount() - 1).toString();
        return !folderName.equals(model.getProjectName()) && !Files
            .exists(projectRoot.resolve(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME));
    }

    private static <T> Stream<T> concat(Iterable<T> a, Stream<T> b) {
        Spliterator<? extends T> spA = a.spliterator();
        Spliterator<? extends T> spB = b.spliterator();

        long s = spA.estimateSize() + spB.estimateSize();
        if (s < 0) {
            s = Long.MAX_VALUE;
        }
        int ch = spA.characteristics() & spB.characteristics() & (Spliterator.NONNULL | Spliterator.SIZED);
        ch |= Spliterator.ORDERED;

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(s, ch) {
            private Spliterator<? extends T> sp1 = spA;
            private Spliterator<? extends T> sp2 = spB;

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                Spliterator<? extends T> sp = sp1;
                if (sp.tryAdvance(action)) {
                    sp1 = sp2;
                    sp2 = sp;
                    return true;
                }
                return sp2.tryAdvance(action);
            }
        }, false);
    }
}
