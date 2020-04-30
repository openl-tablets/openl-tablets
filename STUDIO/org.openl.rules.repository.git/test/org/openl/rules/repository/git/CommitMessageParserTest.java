package org.openl.rules.repository.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openl.rules.repository.git.CommitMessageParser.CommitMessage;

public class CommitMessageParserTest {

    @Test
    public void testMessageParser() {
        CommitMessageParser decompiler = new CommitMessageParser("{user-message} Author: {username}. Commit type: {commit-type}.");
        CommitMessage commitMessage = decompiler.parse("Project My Rules is saved. Author: John Smith. Commit type: SAVE.");

        assertNotNull("Commit message must be parsed!", commitMessage);
        assertEquals("Project My Rules is saved.", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("Project My Rules is archived. Author: John Smith. Commit type: ARCHIVE.");
        assertNotNull("Commit message must be parsed!", commitMessage);
        assertEquals("Project My Rules is archived.", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.ARCHIVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("Project My Rules is restored. Author: John Smith. Commit type: RESTORE.");
        assertNotNull("Commit message must be parsed!", commitMessage);
        assertEquals("Project My Rules is restored.", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.RESTORE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("Project My Rules is erased. Author: John Smith. Commit type: ERASE.");
        assertNotNull("Commit message must be parsed!", commitMessage);
        assertEquals("Project My Rules is erased.", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.ERASE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("Merge with commit 573dd47e6302faf1ba15ff6599e997f2bed4cbb9 Conflicts: My Rules/rules-deploy.xml (yours). Author: John Smith. Commit type: MERGE.");
        assertNotNull("Commit message must be parsed!", commitMessage);
        assertEquals("Merge with commit 573dd47e6302faf1ba15ff6599e997f2bed4cbb9 Conflicts: My Rules/rules-deploy.xml (yours).", commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.MERGE, commitMessage.getCommitType());

        decompiler = new CommitMessageParser("{username} {commit-type}.");
        commitMessage = decompiler.parse("John Smith SAVE.");
        assertNotNull("Commit message must be parsed!", commitMessage);
        assertNull(commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        decompiler = new CommitMessageParser("{commit-type} {username}.");
        commitMessage = decompiler.parse("SAVE John Smith.");
        assertNotNull("Commit message must be parsed!", commitMessage);
        assertNull(commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("");
        assertNull("Commit message must not be parsed!", commitMessage);

        commitMessage = decompiler.parse(null);
        assertNull("Commit message must not be parsed!", commitMessage);

        commitMessage = decompiler.parse("Project My Rules is archived. Author: John Smith. Commit type: ARCHIVE.");
        assertNull("Commit message must not be parsed!", commitMessage);

        decompiler = new CommitMessageParser(".*? {commit-type} {username}.");
        commitMessage = decompiler.parse(".*? SAVE John Smith.");
        assertNotNull("Commit message must be parsed!", commitMessage);
        assertNull(commitMessage.getMessage());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals("John Smith", commitMessage.getAuthor());
        assertEquals(CommitType.SAVE, commitMessage.getCommitType());

        commitMessage = decompiler.parse("foo SAVE John Smith.");
        assertNull("Commit message must not be parsed!", commitMessage);

        decompiler = new CommitMessageParser("");
        commitMessage = decompiler.parse("");
        assertNull("Commit message must not be parsed!", commitMessage);

        decompiler = new CommitMessageParser(null);
        commitMessage = decompiler.parse("");
        assertNull("Commit message must not be parsed!", commitMessage);
    }

}
