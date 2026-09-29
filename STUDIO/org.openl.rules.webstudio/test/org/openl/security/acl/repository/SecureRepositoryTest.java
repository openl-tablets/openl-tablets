package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;

class SecureRepositoryTest {

    private static final String REPOSITORY_ID = "design";
    private static final String REMOVED = "DESIGN/Project/rules/Module.xlsx";

    private Repository delegate;
    private SimpleRepositoryAclService aclService;
    private SecureRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        delegate = mock(Repository.class);
        aclService = mock(SimpleRepositoryAclService.class);
        when(delegate.getId()).thenReturn(REPOSITORY_ID);
        when(aclService.isGranted(nullable(String.class), anyString(), anyBoolean(), any(Permission.class)))
                .thenReturn(true);
        when(aclService.isGranted(nullable(String.class), anyString(), anyList())).thenReturn(true);
        // A repository takes the changes one by one as it writes them, and a change is checked as it is taken.
        when(delegate.save(any(FileData.class), any(), any(ChangesetType.class))).thenAnswer(invocation -> {
            invocation.<Iterable<FileItem>>getArgument(1).forEach(change -> {
            });
            return null;
        });
        repository = new SecureRepository(delegate, aclService);
    }

    @Test
    void aRemovalIsSaved() throws Exception {
        when(delegate.check(REMOVED)).thenReturn(fileData(REMOVED));

        repository.save(fileData("DESIGN/Project"), List.of(new FileItem(fileData(REMOVED), null)), ChangesetType.DIFF);

        // A changeset says what changed, and a file removed is one of those changes: dropping it would commit
        // everything else and leave the file in the repository.
        assertEquals(List.of(REMOVED), paths(savedItems()));
    }

    @Test
    void theRemovedPathIsAskedAboutAsAFileFirst() throws Exception {
        when(delegate.check(REMOVED)).thenReturn(fileData(REMOVED));

        repository.save(fileData("DESIGN/Project"), List.of(new FileItem(fileData(REMOVED), null)), ChangesetType.DIFF);

        // Listing is what a folder answers; a repository asked to list one file may refuse outright.
        verify(delegate, never()).list(REMOVED);
        verify(delegate).check(REMOVED);
    }

    @Test
    void aRemovalOfAFolderIsRecognisedByItsContent() throws Exception {
        when(delegate.check("DESIGN/Project/rules")).thenReturn(null);
        when(delegate.list("DESIGN/Project/rules/")).thenReturn(List.of(fileData(REMOVED)));

        repository.save(fileData("DESIGN/Project"),
                List.of(new FileItem(fileData("DESIGN/Project/rules"), null)),
                ChangesetType.DIFF);

        verify(delegate).list("DESIGN/Project/rules/");
    }

    @Test
    void aFullChangesetCarriesNoRemovalOfItsOwn() throws Exception {
        when(delegate.check(REMOVED)).thenReturn(fileData(REMOVED));
        when(delegate.list(anyString())).thenReturn(List.of());

        repository.save(fileData("DESIGN/Project"), List.of(new FileItem(fileData(REMOVED), null)), ChangesetType.FULL);

        // A full changeset lists what the folder holds, so the repository removes the rest by itself; an item
        // without content is not something every repository can be handed.
        assertEquals(List.of(), paths(savedItems()));
    }

    @Test
    void theFolderOfAFullChangesetIsListedAsAFolder() throws Exception {
        when(delegate.list(anyString())).thenReturn(List.of());

        repository.save(fileData("DESIGN/Project"), List.of(), ChangesetType.FULL);

        // Without the separator the names a listing answers with are glued to the folder path, and nothing of
        // the changeset matches them again.
        verify(delegate).list("DESIGN/Project/");
    }

    @Test
    void theContentOfChangesStreamedFromOneArchiveIsPassedOn() throws Exception {
        when(delegate.list(anyString())).thenReturn(List.of());
        var written = new LinkedHashMap<String, String>();
        when(delegate.save(any(FileData.class), any(), any(ChangesetType.class))).thenAnswer(invocation -> {
            for (FileItem file : invocation.<Iterable<FileItem>>getArgument(1)) {
                written.put(file.getData().getName(),
                        new String(file.getStream().readAllBytes(), StandardCharsets.UTF_8));
            }
            return null;
        });

        repository.save(fileData("DESIGN/Project"),
                archiveOf(Map.of("rules.xml", "<project/>", "rules/Main.xlsx", "workbook")),
                ChangesetType.FULL);

        // A deployment hands over one open archive, so a file is readable only while it is the current change.
        // Holding the changes back until they are all known writes every file of the project empty.
        assertEquals(Map.of("DESIGN/Project/rules.xml", "<project/>", "DESIGN/Project/rules/Main.xlsx", "workbook"),
                written);
    }

    @Test
    void aChangesetAtHandIsRefusedBeforeAnyOfItIsWritten() throws Exception {
        when(aclService.isGranted(nullable(String.class), anyString(), anyList())).thenReturn(false);

        var refused = assertThrows(AccessDeniedException.class,
                () -> repository.save(fileData("DESIGN/Project"),
                        List.of(new FileItem(REMOVED, InputStream.nullInputStream())),
                        ChangesetType.FULL));

        assertTrue(refused.getMessage().contains(REMOVED), refused.getMessage());
        // What is refused stays unwritten: a repository that cannot undo a save would otherwise keep the part
        // of the changeset it had already taken.
        verify(delegate, never()).save(any(FileData.class), any(), any(ChangesetType.class));
    }

    @Test
    void theRefusalOfAStreamedChangeSurvivesTheFailureTheRepositoryReports() throws Exception {
        when(aclService.isGranted(nullable(String.class), anyString(), anyList())).thenReturn(false);
        // A streamed change is checked while the repository is already saving, and the repository reports a
        // change it cannot take as a failure of its own - the way Git answers anything that goes wrong while
        // it commits.
        when(delegate.save(any(FileData.class), any(), any(ChangesetType.class))).thenAnswer(invocation -> {
            try {
                invocation.<Iterable<FileItem>>getArgument(1).forEach(change -> {
                });
            } catch (RuntimeException e) {
                throw new IOException(e.getMessage(), e);
            }
            return null;
        });

        var refused = assertThrows(AccessDeniedException.class,
                () -> repository.save(fileData("DESIGN/Project"),
                        archiveOf(Map.of("rules.xml", "<project/>")),
                        ChangesetType.FULL));

        // Only a refusal answers the caller with "forbidden"; anything else is a fault of the server.
        assertTrue(refused.getMessage().contains("rules.xml"), refused.getMessage());
    }

    @Test
    void aFileTheFileSystemRefusesIsAFailedSaveAndNotAMatterOfPermissions() throws Exception {
        // What the file system refuses - a read-only or locked file - reaches the caller the same way an ACL
        // refusal does, and only the latter means "forbidden" to the user.
        when(delegate.save(any(FileData.class), any(), any(ChangesetType.class)))
                .thenThrow(new IOException("cannot write", new AccessDeniedException("/data/Module.xlsx")));

        var failure = assertThrows(IOException.class,
                () -> repository.save(fileData("DESIGN/Project"),
                        List.of(new FileItem(REMOVED, InputStream.nullInputStream())),
                        ChangesetType.FULL));

        assertEquals(IOException.class, failure.getClass(), failure.toString());
    }

    /**
     * The changes of a project the way a deployment hands them over: one pass over one open archive, where a
     * file is readable only until the next change is asked for.
     */
    private static Iterable<FileItem> archiveOf(Map<String, String> files) throws IOException {
        var archive = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(archive)) {
            for (var file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        var stream = new ZipInputStream(new ByteArrayInputStream(archive.toByteArray()));
        return () -> new Iterator<>() {
            private ZipEntry entry;

            @Override
            public boolean hasNext() {
                try {
                    entry = stream.getNextEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return entry != null;
            }

            @Override
            public FileItem next() {
                return new FileItem("DESIGN/Project/" + entry.getName(), stream);
            }
        };
    }

    private Iterable<FileItem> savedItems() throws Exception {
        var saved = ArgumentCaptor.<Iterable<FileItem>>captor();
        verify(delegate).save(any(FileData.class), saved.capture(), any(ChangesetType.class));
        return saved.getValue();
    }

    private static List<String> paths(Iterable<FileItem> files) {
        var paths = new ArrayList<String>();
        files.forEach(file -> paths.add(file.getData().getName()));
        return paths;
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }
}
