package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FsNode;

/**
 * Unit tests for {@link ProjectFileLookupServiceImpl} over a mocked repository: name matching up the
 * ancestor line, nearest-first ordering, content reads, and the text/size/count guards.
 */
class ProjectFileLookupServiceImplTest {

    private Repository repository;
    private ProjectFileLookupServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(Repository.class);
        lenient().when(repository.getId()).thenReturn("design");
        service = new ProjectFileLookupServiceImpl(mock(AclProjectsHelper.class), grantAllAclProvider());
    }

    private static RepositoryAclServiceProvider grantAllAclProvider() {
        var aclService = mock(RepositoryAclService.class);
        lenient().when(aclService.isGranted(anyString(), anyString(), anyBoolean(), any(Permission.class)))
                .thenReturn(true);
        var provider = mock(RepositoryAclServiceProvider.class);
        lenient().when(provider.getDesignRepoAclService()).thenReturn(aclService);
        return provider;
    }

    @Test
    void collectsAncestorsUpToRoot_nearestFirst_excludingDescendantsAndSiblings() throws IOException {
        listReturns(
                fileData("a/b/AGENTS.md"),
                fileData("a/AGENTS.md"),
                fileData("a/b/c/AGENTS.md"),   // descendant of the anchor — off the upward line
                fileData("AGENTS.md"),
                fileData("a/b/notes.md"),       // different name — ignored
                fileData("x/AGENTS.md"));        // sibling branch — off the upward line

        List<FsNode> files = service.lookup(repository, "a/b/AGENTS.md", false);

        // Walk up from a/b: the anchor (d0), its ancestor a/ (d1), the repository root (d2). The
        // descendant a/b/c/ and the sibling x/ are not visited.
        assertEquals(List.of(
                "a/b/AGENTS.md",
                "a/AGENTS.md",
                "AGENTS.md"), paths(files));
    }

    @Test
    void includeContent_readsEachReturnedFile() throws IOException {
        listReturns(fileData("a/AGENTS.md"), fileData("AGENTS.md"));
        when(repository.read("a/AGENTS.md")).thenReturn(fileItem("nearest"));
        when(repository.read("AGENTS.md")).thenReturn(fileItem("root"));

        List<FsNode> files = service.lookup(repository, "a/AGENTS.md", true);

        assertEquals(List.of("a/AGENTS.md", "AGENTS.md"), paths(files));
        assertEquals("nearest", content(files.get(0)));
        assertEquals("root", content(files.get(1)));
    }

    @Test
    void unreadableFile_isSkipped_othersStillReturned() throws IOException {
        listReturns(fileData("a/AGENTS.md"), fileData("AGENTS.md"));
        when(repository.read("a/AGENTS.md")).thenThrow(new IOException("boom"));
        when(repository.read("AGENTS.md")).thenReturn(fileItem("root"));

        List<FsNode> files = service.lookup(repository, "a/AGENTS.md", true);

        // The nearest file fails to read; the lookup skips it and still returns the readable ancestor.
        assertEquals(List.of("AGENTS.md"), paths(files));
        assertEquals("root", content(files.getFirst()));
    }

    @Test
    void includeContentFalse_doesNotReadFiles() throws IOException {
        listReturns(fileData("a/AGENTS.md"), fileData("AGENTS.md"));

        List<FsNode> files = service.lookup(repository, "a/AGENTS.md", false);

        assertEquals(2, files.size());
        assertNull(content(files.getFirst()));
        verify(repository, never()).read("a/AGENTS.md");
        verify(repository, never()).read("AGENTS.md");
    }

    @Test
    void nonTextAnchor_returnsEmptyWithoutListing() throws IOException {
        List<FsNode> files = service.lookup(repository, "a/rules.xlsx", true);

        assertTrue(files.isEmpty());
        verify(repository, never()).list("");
    }

    @Test
    void missingName_returnsEmpty() throws IOException {
        listReturns(fileData("a/other.md"), fileData("README.md"));

        assertTrue(service.lookup(repository, "a/AGENTS.md", false).isEmpty());
    }

    @Test
    void deletedAndOversizeMatches_areSkipped() throws IOException {
        var deleted = fileData("a/AGENTS.md");
        deleted.setDeleted(true);
        var oversize = fileData("big/AGENTS.md");
        oversize.setSize(ProjectFileLookupServiceImpl.MAX_FILE_SIZE_BYTES + 1);
        listReturns(deleted, oversize, fileData("AGENTS.md"));

        List<FsNode> files = service.lookup(repository, "a/AGENTS.md", false);

        assertEquals(List.of("AGENTS.md"), paths(files));
    }

    @Test
    void includeContent_oversizeContentWithUndefinedSize_isSkipped() throws IOException {
        // The repository reports no size up front, but the content exceeds the cap: the bounded read
        // must reject it instead of surfacing a partial blob.
        listReturns(fileData("AGENTS.md"));
        String oversize = "a".repeat((int) ProjectFileLookupServiceImpl.MAX_FILE_SIZE_BYTES + 1);
        when(repository.read("AGENTS.md")).thenReturn(fileItem(oversize));

        assertTrue(service.lookup(repository, "AGENTS.md", true).isEmpty());
    }

    @Test
    void capsAtMaxFilesCount() throws IOException {
        // A pathological deep anchor with a same-named file at every level above it: the walk up must
        // stop collecting once the cap is reached.
        var data = new ArrayList<FileData>();
        var dir = new StringBuilder();
        for (int i = 0; i < ProjectFileLookupServiceImpl.MAX_FILES_COUNT + 5; i++) {
            data.add(fileData(dir + "AGENTS.md"));
            dir.append('d').append(i).append('/');
        }
        when(repository.list("")).thenReturn(data);

        List<FsNode> files = service.lookup(repository, dir + "AGENTS.md", false);

        assertEquals(ProjectFileLookupServiceImpl.MAX_FILES_COUNT, files.size());
    }

    // --- helpers ---

    private void listReturns(FileData... files) {
        try {
            lenient().when(repository.list("")).thenReturn(List.of(files));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> paths(List<FsNode> nodes) {
        return nodes.stream().map(FsNode::getPath).toList();
    }

    private static String content(FsNode node) {
        return ((FileNode) node).getContent();
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }

    private static FileItem fileItem(String content) {
        return new FileItem(fileData("ignored"), new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
