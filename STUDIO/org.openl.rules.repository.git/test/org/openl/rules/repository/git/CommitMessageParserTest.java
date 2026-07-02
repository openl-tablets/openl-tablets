package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.git.CommitMessageParser.CommitMessage;

class CommitMessageParserTest {

    @Test
    void testMessageParser() {
        CommitMessageParser decompiler = new CommitMessageParser(
                "{user-message} Author: {username}. Commit type: {commit-type}.");
        CommitMessage commitMessage = decompiler
                .parse("Project My Rules is saved. Author: John Smith. Commit type: SAVE.");

        assertNotNull(commitMessage, "Commit message must be parsed!");
        assertEquals("Project My Rules is saved.", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("Project My Rules is deleted. Author: John Smith. Commit type: DELETE.");
        assertNotNull(commitMessage, "Commit message must be parsed!");
        assertEquals("Project My Rules is deleted.", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.DELETE, commitMessage.getCommitType());

        commitMessage = decompiler
                .parse("Project my-project was saved. Author: John Smith. Commit type: SAVE.\n");
        assertNotNull(commitMessage, "Commit message must be parsed!");
        assertEquals("Project my-project was saved.", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse(
                "Merge with commit 573dd47e6302faf1ba15ff6599e997f2bed4cbb9 Conflicts: My Rules/rules-deploy.xml (yours). Author: John Smith. Commit type: MERGE.");
        assertNotNull(commitMessage, "Commit message must be parsed!");
        assertEquals(
                "Merge with commit 573dd47e6302faf1ba15ff6599e997f2bed4cbb9 Conflicts: My Rules/rules-deploy.xml (yours).",
                commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.MERGE, commitMessage.getCommitType());

        decompiler = new CommitMessageParser("{username} {commit-type}.");
        commitMessage = decompiler.parse("John Smith SAVE.");
        assertNotNull(commitMessage, "Commit message must be parsed!");
        assertNull(commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        decompiler = new CommitMessageParser("{commit-type} {username}.");
        commitMessage = decompiler.parse("SAVE John Smith.");
        assertNotNull(commitMessage, "Commit message must be parsed!");
        assertNull(commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("");
        assertNull(commitMessage, "Commit message must not be parsed!");

        commitMessage = decompiler.parse(null);
        assertNull(commitMessage, "Commit message must not be parsed!");

        commitMessage = decompiler.parse("Project My Rules is archived. Author: John Smith. Commit type: ARCHIVE.");
        assertNull(commitMessage, "Commit message must not be parsed!");

        commitMessage = decompiler.parse("Project My Rules is restored. Author: John Smith. Commit type: RESTORE.");
        assertNull(commitMessage, "Commit message must not be parsed!");

        commitMessage = decompiler.parse("Project My Rules is erased. Author: John Smith. Commit type: ERASE.");
        assertNull(commitMessage, "Commit message must not be parsed!");

        decompiler = new CommitMessageParser(".*? {commit-type} {username}.");
        commitMessage = decompiler.parse(".*? SAVE John Smith.");
        assertNotNull(commitMessage, "Commit message must be parsed!");
        assertNull(commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("foo SAVE John Smith.");
        assertNull(commitMessage, "Commit message must not be parsed!");

        decompiler = new CommitMessageParser("");
        commitMessage = decompiler.parse("");
        assertNull(commitMessage, "Commit message must not be parsed!");

        decompiler = new CommitMessageParser(null);
        commitMessage = decompiler.parse("");
        assertNull(commitMessage, "Commit message must not be parsed!");

        decompiler = new CommitMessageParser(
                "Webstudio {user-message} Author: {username}. Commit type: {commit-type}. {user-message} Author: {username}. Commit type: {commit-type}.");
        commitMessage = decompiler.parse(
                "Webstudio foo-bar Author: John Smith. Commit type: ARCHIVE. foo-bar Author: John Smith. Commit type: ARCHIVE.");
        assertNull(commitMessage, "Commit message must not be parsed!");
    }

}
