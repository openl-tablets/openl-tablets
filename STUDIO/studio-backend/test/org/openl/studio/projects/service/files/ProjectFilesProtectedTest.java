package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.ProjectFiles;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.UploadFormatting;

/**
 * A protected file of a project, such as {@code pom.xml}, does not move with the project: an export leaves it out,
 * an upload neither writes it nor, replacing the project, deletes the one the project holds.
 *
 * @author Yury Molchan
 */
class ProjectFilesProtectedTest {

    private ProjectFilesServiceImpl service;
    private FileRoot root;
    private AProjectFolder tree;

    @BeforeEach
    void init() throws Exception {
        var acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class),
                new BeanValidationProvider(List.of()), new UploadFormatting(mock(TableFormatting.class)),
                IgnoredPaths.of("**/.DS_Store"), ProjectFiles.of("pom.xml, META-INF/MANIFEST.MF", "rules.xml"), false);
        var project = mock(AProject.class);
        var data = new FileData();
        data.setName("");
        when(project.getFileData()).thenReturn(data);
        tree = new AProjectFolder(new HashMap<>(), project, null, "");
        tree.addArtefact(file("pom.xml", "<kept/>"));
        tree.addArtefact(file("rules.xml", "<project/>"));
        var rules = new AProjectFolder(new HashMap<>(), project, null, "rules");
        rules.addArtefact(file("rules/pom.xml", "<nested/>"));
        tree.addArtefact(rules);
        var metaInf = new AProjectFolder(new HashMap<>(), project, null, "META-INF");
        metaInf.addArtefact(file("META-INF/MANIFEST.MF", "Manifest-Version: 1.0"));
        tree.addArtefact(metaInf);
        root = mock(FileRoot.class);
        when(root.isProject()).thenReturn(true);
        when(root.repositoryPathOf(anyString())).thenAnswer(call -> call.getArgument(0));
        when(root.readFolder(null)).thenReturn(tree);
    }

    private static AProjectResource file(String path, String content) throws Exception {
        var file = mock(AProjectResource.class);
        var data = new FileData();
        data.setName(path);
        when(file.getFileData()).thenReturn(data);
        when(file.getName()).thenReturn(path.substring(path.lastIndexOf('/') + 1));
        when(file.getInternalPath()).thenReturn(path);
        when(file.getContent()).thenAnswer(call -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return file;
    }

    private static List<ProjectFilesService.UploadedFile> upload(String... names) {
        var files = new ArrayList<ProjectFilesService.UploadedFile>();
        for (var name : names) {
            files.add(new ProjectFilesService.UploadedFile(name, "<uploaded/>".getBytes(StandardCharsets.UTF_8)));
        }
        return files;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> written(ChangesetType changesetType) {
        ArgumentCaptor<List<FileItem>> items = ArgumentCaptor.forClass(List.class);
        verify(root).writeBatch(eq(""), items.capture(), eq(changesetType), anyString());
        var written = new LinkedHashMap<String, String>();
        for (var item : items.getValue()) {
            try (var content = item.getStream()) {
                written.put(item.getData().getName(), new String(content.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return written;
    }

    @Test
    void leavesTheProtectedFilesOutOfAnUpload() {
        var uploaded = service.uploadFiles(root, "",
                upload("rules.xml", "pom.xml", "rules/pom.xml", ".DS_Store", "META-INF/MANIFEST.MF"),
                ConflictPolicy.OVERWRITE);

        assertEquals(List.of("rules.xml", "rules/pom.xml"), uploaded);
        assertEquals(List.of("rules.xml", "rules/pom.xml"), List.copyOf(written(ChangesetType.DIFF).keySet()));
    }

    @Test
    void keepsTheProtectedFilesOfTheProjectWhenAnUploadReplacesIt() {
        var uploaded = service.uploadFiles(root, "", upload("rules.xml", "pom.xml", "META-INF/MANIFEST.MF"),
                ConflictPolicy.REPLACE);

        assertEquals(List.of("rules.xml"), uploaded);
        assertEquals(Map.of("rules.xml", "<uploaded/>", "pom.xml", "<kept/>",
                "META-INF/MANIFEST.MF", "Manifest-Version: 1.0"), written(ChangesetType.FULL));
    }

    @Test
    void refusesAReplaceThatPutsAFileUnderAKeptProtectedFile() {
        var files = upload("rules.xml", "pom.xml/child.txt");

        var e = assertThrows(ConflictException.class, () -> service.uploadFiles(root, "", files, ConflictPolicy.REPLACE));

        assertEquals("openl.error.409.file.path.not.folder.message", e.getErrorCode());
        verify(root, never()).writeBatch(anyString(), any(), any(), anyString());
    }

    @Test
    void writesThemWhereTheMountIsNotAProject() {
        when(root.isProject()).thenReturn(false);

        var uploaded = service.uploadFiles(root, "", upload("rules.xml", "pom.xml"), ConflictPolicy.OVERWRITE);

        assertEquals(List.of("rules.xml", "pom.xml"), uploaded);
    }

    @Test
    void leavesTheProtectedFilesOutOfAnExport() throws IOException {
        when(root.readFolder(null)).thenReturn(tree);
        var out = new ByteArrayOutputStream();

        service.writeFolderAsZip(root, "", out, null);

        var entries = new ArrayList<String>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.add(entry.getName());
            }
        }
        entries.sort(null);
        assertEquals(List.of("rules.xml", "rules/pom.xml"), entries);
    }
}
