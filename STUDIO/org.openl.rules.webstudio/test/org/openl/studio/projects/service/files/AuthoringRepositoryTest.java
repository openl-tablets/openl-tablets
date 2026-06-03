package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;

/**
 * Verifies that {@link AuthoringRepository} stamps the configured author on every write and leaves
 * non-write operations untouched.
 *
 * @author Yury Molchan
 */
class AuthoringRepositoryTest {

    private BranchRepository delegate;
    private UserInfo author;
    private AuthoringRepository repository;

    @BeforeEach
    void setUp() {
        delegate = mock(BranchRepository.class);
        author = new UserInfo("tester", "tester@example.com", "Tester");
        repository = new AuthoringRepository(delegate, author);
    }

    private static FileData named(String name) {
        var data = new FileData();
        data.setName(name);
        return data;
    }

    private static InputStream emptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Test
    void saveSingleFileStampsAuthorAndPassesStreamThrough() throws Exception {
        var data = named("rules/file.txt");
        var stream = emptyStream();

        repository.save(data, stream);

        var captor = ArgumentCaptor.forClass(FileData.class);
        verify(delegate).save(captor.capture(), eq(stream));
        assertSame(author, captor.getValue().getAuthor());
        assertEquals("rules/file.txt", captor.getValue().getName());
        assertEquals("Save file.txt", captor.getValue().getComment());
    }

    @Test
    void existingCommentIsPreserved() throws Exception {
        var data = named("rules/file.txt");
        data.setComment("Custom message");

        repository.save(data, emptyStream());

        var captor = ArgumentCaptor.forClass(FileData.class);
        verify(delegate).save(captor.capture(), any());
        assertEquals("Custom message", captor.getValue().getComment());
    }

    @Test
    void saveFileItemListStampsAuthorOnEveryItem() throws Exception {
        var items = List.of(new FileItem(named("a.txt"), emptyStream()),
                new FileItem(named("b.txt"), emptyStream()));

        repository.save(items);

        verify(delegate).save(items);
        items.forEach(item -> assertSame(author, item.getData().getAuthor()));
    }

    @Test
    void saveFolderStampsAuthorOnFolderAndEntries() throws Exception {
        var folder = named("folder");
        var entries = List.of(new FileItem(named("folder/a.txt"), emptyStream()));

        repository.save(folder, entries, ChangesetType.FULL);

        verify(delegate).save(eq(folder), eq(entries), eq(ChangesetType.FULL));
        assertSame(author, folder.getAuthor());
        entries.forEach(item -> assertSame(author, item.getData().getAuthor()));
    }

    @Test
    void deleteSingleStampsAuthor() throws Exception {
        var data = named("rules/file.txt");

        repository.delete(data);

        var captor = ArgumentCaptor.forClass(FileData.class);
        verify(delegate).delete(captor.capture());
        assertSame(author, captor.getValue().getAuthor());
        assertEquals("Delete file.txt", captor.getValue().getComment());
    }

    @Test
    void deleteListStampsAuthorOnEveryItem() throws Exception {
        var data = List.of(named("a"), named("b"));

        repository.delete(data);

        verify(delegate).delete(data);
        data.forEach(item -> assertSame(author, item.getAuthor()));
    }

    @Test
    void deleteHistoryStampsAuthor() throws Exception {
        var data = named("rules/file.txt");

        repository.deleteHistory(data);

        var captor = ArgumentCaptor.forClass(FileData.class);
        verify(delegate).deleteHistory(captor.capture());
        assertSame(author, captor.getValue().getAuthor());
    }

    @Test
    void copyHistoryStampsAuthorOnDestination() throws Exception {
        var destination = named("destination");

        repository.copyHistory("source", destination, "v1");

        var captor = ArgumentCaptor.forClass(FileData.class);
        verify(delegate).copyHistory(eq("source"), captor.capture(), eq("v1"));
        assertSame(author, captor.getValue().getAuthor());
    }

    @Test
    void forBranchReturnsAuthoringWrapperThatKeepsStampingAuthor() throws Exception {
        var branchDelegate = mock(BranchRepository.class);
        when(delegate.forBranch("dev")).thenReturn(branchDelegate);

        BranchRepository branchRepository = repository.forBranch("dev");

        assertNotSame(repository, branchRepository);
        assertInstanceOf(AuthoringRepository.class, branchRepository);

        branchRepository.save(named("x"), emptyStream());
        var captor = ArgumentCaptor.forClass(FileData.class);
        verify(branchDelegate).save(captor.capture(), any());
        assertSame(author, captor.getValue().getAuthor());
    }

    @Test
    void readOperationsDelegateUnchanged() {
        when(delegate.getName()).thenReturn("design");
        when(delegate.getId()).thenReturn("design-flat");

        assertEquals("design", repository.getName());
        assertEquals("design-flat", repository.getId());
        verify(delegate).getName();
        verify(delegate).getId();
    }
}
