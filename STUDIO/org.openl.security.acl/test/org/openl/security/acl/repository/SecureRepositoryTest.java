package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

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
    private SecureRepository repository;

    @BeforeEach
    void setUp() {
        delegate = mock(Repository.class);
        var aclService = mock(SimpleRepositoryAclService.class);
        when(delegate.getId()).thenReturn(REPOSITORY_ID);
        when(aclService.isGranted(nullable(String.class), anyString(), anyBoolean(), any(Permission.class)))
                .thenReturn(true);
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
