package org.openl.rules.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;

public class RepositoryInstatiatorTest {
    Repository repo;

    @BeforeEach
    public void setup() {
        repo = RepositoryInstatiator.newRepository("repo", Map.of("repo.factory", "repo-file", "repo.uri", ".")::get);
    }

    @Test
    public void legal() throws IOException {
        List<FileData> list = repo.list("target/");
        assertFalse(list.isEmpty(), "expected non-empty folder");
        list.forEach(System.out::println);
    }

    @Test
    public void illegal() throws IOException {
        assertThrows(InvalidPathException.class, () -> {
            repo.list("target/../../");
        });
    }
}
