package org.openl.rules.repository;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;

public class RepositoryInstatiatorTest {
    Repository repo;

    @Before
    public void setup() {
        repo = RepositoryInstatiator.newRepository("repo", Map.of("repo.factory", "repo-file", "repo.uri", ".")::get);
    }

    @Test
    public void legal() throws IOException {
        List<FileData> list = repo.list("target/");
        assertFalse("expected non-empty folder", list.isEmpty());
        list.forEach(System.out::println);
    }

    @Test(expected = InvalidPathException.class)
    public void illegal() throws IOException {
        repo.list("target/../../");
    }
}
