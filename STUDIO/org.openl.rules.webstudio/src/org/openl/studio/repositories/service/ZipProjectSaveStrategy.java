package org.openl.studio.repositories.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.AdditionalData;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.folder.FileChangesFromFolder;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.studio.repositories.model.CreateUpdateProjectModel;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;

@Component
public class ZipProjectSaveStrategy {

    private static final String ROOT_XLSX_MODULE_PATTERN = "*.xlsx";

    private final DesignTimeRepository designTimeRepository;
    private final PathFilter zipFilter;
    private final ZipCharsetDetector zipCharsetDetector;
    private final UserManagementService userManagementService;

    public ZipProjectSaveStrategy(DesignTimeRepository designTimeRepository,
                                  @Qualifier("zipFilter") PathFilter zipFilter,
                                  ZipCharsetDetector zipCharsetDetector,
                                  UserManagementService userManagementService) {
        this.designTimeRepository = designTimeRepository;
        this.zipFilter = zipFilter;
        this.zipCharsetDetector = zipCharsetDetector;
        this.userManagementService = userManagementService;
    }

    public FileData save(Repository repository, CreateUpdateProjectModel model, Path zipArchive) throws IOException {
        var author = Optional.ofNullable(userManagementService.getUser(model.getAuthor()))
                .map(user -> new UserInfo(user.getUsername(), user.getEmail(), user.getDisplayName()))
                .orElse(new UserInfo(model.getAuthor()));
        var projectData = new FileData();
        projectData.setName(designTimeRepository.getRulesLocation() + model.getProjectName());
        projectData.setComment(StringUtils.trimToEmpty(model.getComment()));
        projectData.setAuthor(author);
        if (repository.supports().mappedFolders()) {
            AdditionalData<FileMappingData> additionalData = new FileMappingData(projectData.getName(),
                    model.getFullPath());
            projectData.addAdditionalData(additionalData);
        }
        var adaptor = new ProjectDescriptorNameAdaptor(model.getProjectName());
        Predicate<Path> filter = p -> zipFilter.accept(p.toString());
        var charset = zipCharsetDetector.detectCharset(() -> Files.newInputStream(zipArchive));
        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(zipArchive),
                Map.of("encoding", charset.name()))) {

            final var root = fs.getPath("/");
            var generatedRulesXml = Files.exists(root.resolve(ProjectDescriptor.FILE_NAME))
                    ? Optional.<byte[]>empty()
                    : Optional.of(rulesXml(root, filter, model.getProjectName()));
            if (repository.supports().folders()) {
                try (var changes = new FileChangesFromFolder(root,
                        projectData.getName(),
                        filter,
                        adaptor)) {
                    var projectChanges = generatedRulesXml.isPresent()
                            ? appendProjectDescriptor(changes, projectData.getName(), generatedRulesXml.orElseThrow())
                            : changes;
                    return repository.save(projectData, projectChanges, ChangesetType.FULL);
                }
            }

            return saveAsArchive(repository, projectData, root, filter, adaptor, generatedRulesXml);
        }
    }

    private static FileData saveAsArchive(Repository repository,
                                          FileData projectData,
                                          Path projectRoot,
                                          Predicate<Path> filter,
                                          ProjectDescriptorNameAdaptor adaptor,
                                          Optional<byte[]> generatedRulesXml) throws IOException {
        Path tmp = FileUtils.createPrivateTempFile(FileUtils.getBaseName(projectData.getName()), ".zip");
        try {
            try (var zos = new ZipOutputStream(Files.newOutputStream(tmp))) {
                try (var changes = new FileChangesFromFolder(projectRoot, filter, adaptor)) {
                    for (FileItem fileItem : changes) {
                        var name = fileItem.getData().getName();
                        if (name.charAt(0) == '/') {
                            name = name.substring(1);
                        }
                        var entry = new ZipEntry(name);
                        zos.putNextEntry(entry);
                        var is = fileItem.getStream();
                        if (is != null) {
                            is.transferTo(zos);
                            IOUtils.closeQuietly(is);
                        }
                    }
                }
                if (generatedRulesXml.isPresent()) {
                    zos.putNextEntry(new ZipEntry(ProjectDescriptor.FILE_NAME));
                    zos.write(generatedRulesXml.orElseThrow());
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

    private static Iterable<FileItem> appendProjectDescriptor(Iterable<FileItem> changes,
                                                              String projectFolder,
                                                              byte[] rulesXml) {
        var descriptor = new FileItem(projectFolder + "/" + ProjectDescriptor.FILE_NAME,
                new ByteArrayInputStream(rulesXml));
        return () -> Stream.concat(StreamSupport.stream(changes.spliterator(), false), Stream.of(descriptor))
                .iterator();
    }

    private static byte[] rulesXml(Path projectRoot, Predicate<Path> filter, String projectName) throws IOException {
        var modules = new ArrayList<Module>();
        modules.add(module(ROOT_XLSX_MODULE_PATTERN));
        try (var files = Files.list(projectRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(filter)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(FileTypeHelper::isExcelFile)
                    .filter(fileName -> !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                    .filter(fileName -> !fileName.startsWith("._"))
                    .map(ZipProjectSaveStrategy::module)
                    .forEach(modules::add);
        }
        var descriptor = new ProjectDescriptor();
        descriptor.setName(projectName);
        descriptor.setModules(modules);
        return descriptor.toBytes();
    }

    private static Module module(String path) {
        var module = new Module();
        module.setRulesRootPath(path);
        return module;
    }
}
