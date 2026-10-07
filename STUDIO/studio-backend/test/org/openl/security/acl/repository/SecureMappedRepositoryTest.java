package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.dtr.impl.MappedRepository;

/**
 * A mapped repository shows its folders under names of its own, while the permissions of a project are kept under
 * the folder it really lives in.
 */
class SecureMappedRepositoryTest {

    private static final String REPOSITORY_ID = "design";
    /** How a mapped repository shows a file: the declared project name paired with the hash of its folder. */
    private static final String MAPPED = "DESIGN/rules/Project:1ef33ea3/rules/Module.xlsx";
    /** The folder that file really lives in. */
    private static final String INTERNAL = "Project/rules/Module.xlsx";

    private MappedRepository delegate;
    private SimpleRepositoryAclService aclService;
    private SecureMappedRepository repository;

    @BeforeEach
    void setUp() {
        delegate = mock(MappedRepository.class);
        aclService = mock(SimpleRepositoryAclService.class);
        when(delegate.getId()).thenReturn(REPOSITORY_ID);
        when(delegate.getRealPath(MAPPED)).thenReturn(INTERNAL);
        repository = new SecureMappedRepository(delegate, aclService);
    }

    @Test
    void readsContentByThePathItsPermissionsAreKeptUnder() throws Exception {
        when(aclService.isGranted(REPOSITORY_ID, INTERNAL, List.of(BasePermission.READ))).thenReturn(true);

        repository.read(MAPPED);

        verify(delegate).read(MAPPED);
    }

    @Test
    void hidesContentGrantedUnderTheNameTheRepositoryShows() throws Exception {
        // The technical name is no project of its own, so a grant on the project would never answer to it.
        when(aclService.isGranted(REPOSITORY_ID, MAPPED, List.of(BasePermission.READ))).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> repository.read(MAPPED));
        verify(delegate, never()).read(MAPPED);
    }

    @Test
    void modifiesContentByThePathItsPermissionsAreKeptUnder() throws Exception {
        var data = fileData(MAPPED);
        var stream = InputStream.nullInputStream();
        when(delegate.check(MAPPED)).thenReturn(data);
        when(aclService.isGranted(REPOSITORY_ID, INTERNAL, List.of(BasePermission.WRITE))).thenReturn(true);

        repository.save(data, stream);

        verify(delegate).save(data, stream);
    }

    @Test
    void createsContentByThePathItsPermissionsAreKeptUnder() throws Exception {
        var data = fileData(MAPPED);
        var stream = InputStream.nullInputStream();
        when(aclService.isGranted(REPOSITORY_ID, INTERNAL, List.of(BasePermission.CREATE))).thenReturn(true);

        repository.save(data, stream);

        verify(delegate).save(data, stream);
    }

    @Test
    void deletesContentByThePathItsPermissionsAreKeptUnder() throws Exception {
        var data = fileData(MAPPED);
        when(aclService.isGranted(REPOSITORY_ID, INTERNAL, true, BasePermission.DELETE)).thenReturn(true);

        repository.delete(data);

        verify(delegate).delete(data);
    }

    @Test
    void deletesAListedFileByThePathTheListingItselfReports() throws Exception {
        var listed = fileData(MAPPED);
        listed.addAdditionalData(new FileMappingData(MAPPED, INTERNAL));
        when(aclService.isGranted(REPOSITORY_ID, INTERNAL, true, BasePermission.DELETE)).thenReturn(true);

        repository.delete(listed);

        // A listing answers with the path itself, and that answer holds even when the mapping is out of date.
        verify(delegate, never()).getRealPath(anyString());
    }

    @Test
    void mergesConflictResolutionsByTheirRepositoryPath() throws Exception {
        var author = mock(UserInfo.class);
        var conflictData = new ConflictResolveData("revision", List.of(new FileItem(INTERNAL, null)), "Resolve");
        when(aclService.isGranted(REPOSITORY_ID, INTERNAL, List.of(BasePermission.WRITE))).thenReturn(true);

        repository.merge("feature", author, conflictData);

        verify(delegate).merge("feature", author, conflictData);
        verify(delegate, never()).getRealPath(anyString());
    }

    private static FileData fileData(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }
}
