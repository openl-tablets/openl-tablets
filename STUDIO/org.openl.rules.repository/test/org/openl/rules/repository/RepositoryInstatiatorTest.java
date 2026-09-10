package org.openl.rules.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.Repository;

class RepositoryInstatiatorTest {
    Repository repo;

    @BeforeEach
    void setup() {
        repo = RepositoryInstatiator.newRepository("repo", Map.of("repo.factory", "repo-file", "repo.uri", ".")::get);
    }

    @Test
    void legal() throws IOException {
        var list = repo.list("target/");
        assertFalse(list.isEmpty(), "expected non-empty folder");
        list.forEach(System.out::println);
    }

    @Test
    void illegal() throws IOException {
        assertThrows(InvalidPathException.class, () -> {
            repo.list("target/../../");
        });
    }

    @Test
    void namingARepositoryTypeDoesNotLookUpTheFactoriesAgain() {
        // Every repository the settings name asks for its type, and a listing names them all. A lookup
        // reads the declaration out of every jar on the class path and builds each factory it names, so
        // it must happen once however many types are named.
        assertEquals("repo-file", RepositoryInstatiator.getRefID("repo-file"));
        var lookups = CountingRepositoryFactory.BUILT.get();

        for (var i = 0; i < 100; i++) {
            RepositoryInstatiator.getRefID("repo-file");
            RepositoryInstatiator.getRefID("repo-jdbc");
        }

        assertEquals(lookups, CountingRepositoryFactory.BUILT.get());
    }

    @Test
    void aFactoryThatCannotBeCreatedLeavesTheOthersUsable() {
        // FailingRepositoryFactory is declared next to the working ones and throws when it is created.
        // Were it not left out, no repository type could be named at all.
        assertEquals("repo-file", RepositoryInstatiator.getRefID("repo-file"));
        assertEquals("repo-jdbc", RepositoryInstatiator.getRefID("repo-jdbc"));
        assertNull(RepositoryInstatiator.getRefID("repo-failing-test-only"));
    }

    @Test
    void anUnknownTypeIsNotNamed() {
        assertNull(RepositoryInstatiator.getRefID("repo-does-not-exist"));
        assertNull(RepositoryInstatiator.getRefID(null));
    }
}
