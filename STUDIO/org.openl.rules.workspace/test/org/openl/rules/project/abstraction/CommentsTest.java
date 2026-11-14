package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Collections;
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
    void keepsAnyOtherCommentWhole() {
        assertEquals(Collections.singletonList(null), Comments.getCommentParts(null));
        assertEquals(List.of(""), Comments.getCommentParts(""));
        assertEquals(List.of("My comment"), Comments.getCommentParts("My comment"));
        assertEquals(List.of("Copied from: Bank Rating"), Comments.getCommentParts("Copied from: Bank Rating"));
        assertEquals(List.of("EPBDS-1 Copied from: Bank Rating."),
                Comments.getCommentParts("EPBDS-1 Copied from: Bank Rating."));
    }
}
