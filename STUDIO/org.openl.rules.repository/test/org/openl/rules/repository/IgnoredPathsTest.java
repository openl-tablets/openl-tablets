package org.openl.rules.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IgnoredPathsTest {

    private static final IgnoredPaths SETTING = IgnoredPaths.of(" **/.git*/** , **/pom.xml,, **/.mvn/** ");

    @ParameterizedTest
    @ValueSource(strings = {".git", ".git/config", "project/.git/hooks/pre-commit", ".GIT/config", "a/.Git"})
    void alwaysIgnoresTheGitFolder(String path) {
        assertTrue(IgnoredPaths.of(null).matches(path));
        assertTrue(IgnoredPaths.of(" ").matches(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pom.xml", "rules/Project/pom.xml", "POM.XML", ".gitignore", "p/.github/workflows/b.yml",
            ".gitlab-ci.yml", "p/.mvn", "p/.mvn/wrapper/maven-wrapper.properties"})
    void ignoresThePathsTheSettingLists(String path) {
        assertTrue(SETTING.matches(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "rules.xml", "rules/Project/Rules.xlsx", "pom.xml.bak", "rules/pom", "my.git/file",
            "target/Rules.xlsx"})
    void keepsTheOtherPaths(String path) {
        assertFalse(SETTING.matches(path));
    }

    /** The temporary files the default setting ignores, as a ZIP archive opened as a file system names them too. */
    @ParameterizedTest
    @ValueSource(strings = {"p/.DS_Store", "/.DS_Store", "p/~$Rules.xlsx", "/p/target/classes/a.class", "__MACOSX",
            "__MACOSX/._Rules.xlsx", "p/Thumbs.db"})
    void ignoresTemporaryFilesAPatternNames(String path) {
        assertTrue(IgnoredPaths.of("**/.DS_Store, **/~$*, **/target/**, **/__MACOSX/**, **/Thumbs.db").matches(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pom.xml", ".gitignore"})
    void ignoresOnlyTheGitFolderWithoutSetting(String path) {
        assertFalse(IgnoredPaths.of(null).matches(path));
    }

    /** The entries of an uploaded ZIP archive, a folder ending with a slash, as the files of other VCS name them. */
    @ParameterizedTest
    @ValueSource(strings = {"CVS/", ".svn/", ".svn", "root/CVS/", "root/.svn/", "root/folder1/CVS/base",
            "root/folder1/.svn/base", ".SVN/entries", "root/cVS/entries", ".cvsignore", "root/.cvsignore"})
    void ignoresTheEntriesOfOtherVersionControlSystems(String path) {
        assertTrue(IgnoredPaths.of("**/.svn/**, **/CVS/**, **/.cvsignore").matches(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"f", "f/", "svn/hello.java", "root/file.svn", ".svn./1", "root/folder/.svnt/", "CVS./1",
            "root/folder/.CVSNT/", "cvsignore", ".cvsignore/1.txt"})
    void keepsTheEntriesNamedAlike(String path) {
        assertFalse(IgnoredPaths.of("**/.svn/**, **/CVS/**, **/.cvsignore").matches(path));
    }
}
