package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class CommentsTest {

    @Test
    void namesTheProjectAsItIs() {
        assertEquals("Project $$$my {Proj}$ect$$ is saved.", Comments.saveProject("$$$my {Proj}$ect$$"));
        assertEquals("Project $$$my {Proj}$ect$$ is created.", Comments.createProject("$$$my {Proj}$ect$$"));
        assertEquals("Copied from: $$$my {Proj}$ect$$.", Comments.copiedFrom("$$$my {Proj}$ect$$"));
    }

    @Test
    void writesAMissingProjectAsAnEmptyText() {
        assertEquals("Project  is saved.", Comments.saveProject(null));
        assertEquals("Project  is created.", Comments.createProject(null));
        assertEquals("Copied from: .", Comments.copiedFrom(null));
    }

    @Test
    void writesTheMomentOfARestoredRevisionInUtc() {
        var date = Date.from(Instant.parse("2020-06-22T21:02:42.517Z"));

        assertEquals("Restored from revision of john on 06/22/2020 09:02:42 PM.",
                Comments.restoredFrom(" john ", date));
        assertEquals("Restored from revision of  on .", Comments.restoredFrom(null, null));
    }

    @Test
    void splitsTheCommentOfACopyAroundTheSourceProject() {
        assertEquals(List.of("Copied from: ", "Bank Rating", "."), Comments.getCommentParts("Copied from: Bank Rating."));
        assertEquals(List.of("Copied from: ", "", "."), Comments.getCommentParts("Copied from: ."));
    }

    @Test
    void splitsNoOtherComment() {
        assertNull(Comments.getCommentParts(null));
        assertNull(Comments.getCommentParts(""));
        assertNull(Comments.getCommentParts("My comment"));
        assertNull(Comments.getCommentParts("Copied from: Bank Rating"));
        assertNull(Comments.getCommentParts("EPBDS-1 Copied from: Bank Rating."));
    }
}
