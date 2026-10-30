package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentsTest {

    private static final String TEMPLATE = "FOO";

    private Comments comments;
    private Comments comments2;

    @BeforeEach
    void setUp() {
        var saveProjectTemplate = "Project {username} {{project-name}} is saved. {foo}";
        var createProjectTemplate = "Project {username} {project-name} is created. {foo}";
        var copiedFromTemplate = "Project {username} {{project-name}} is copied-from. {foo}";
        var restoredFromTemplate = "Project {username} {revision} is restored-from. Author: {author}, date: {datetime}. {foo}";
        var newBranchNameTemplate = "{project-name}/{username}/{current-date} {foo}";
        comments = new Comments(saveProjectTemplate,
                createProjectTemplate,
                copiedFromTemplate,
                restoredFromTemplate,
                newBranchNameTemplate);

        comments2 = new Comments(TEMPLATE,
                TEMPLATE,
                TEMPLATE,
                TEMPLATE,
                TEMPLATE);
    }

    @Test
    void testSaveProject() {
        var actual = comments.saveProject("myProjectName");
        assertEquals("Project {username} {myProjectName} is saved. {foo}", actual);
    }

    @Test
    void testSaveProjectWithDollarSign() {
        var actualWithSymbol = comments.saveProject("$$$myProj$ectName$$");
        assertEquals("Project {username} {$$$myProj$ectName$$} is saved. {foo}", actualWithSymbol);
    }

    @Test
    void testCreateProject() {
        var actual = comments.createProject("myProjectName");
        assertEquals("Project {username} myProjectName is created. {foo}", actual);
    }

    @Test
    void testCreateProjectWithDollarSign() {
        var actualWithSymbol = comments.createProject("$$$myProj$ectName$$");
        assertEquals("Project {username} $$$myProj$ectName$$ is created. {foo}", actualWithSymbol);
    }

    @Test
    void testCopiedFrom() {
        var actual = comments.copiedFrom("myProjectName");
        assertEquals("Project {username} {myProjectName} is copied-from. {foo}", actual);
    }

    @Test
    void testCopiedFromProjectWithSpecialSymbols() {
        var actualWithSymbol = comments.copiedFrom("$$$myProj$ectName$$");
        assertEquals("Project {username} {$$$myProj$ectName$$} is copied-from. {foo}", actualWithSymbol);
    }

    @Test
    void testParseSourceOfCopy() {
        var commentParts = comments
                .getCommentParts("Project {username} {myProjectName} is copied-from. {foo}");
        assertEquals(3, commentParts.size());
        assertEquals("Project {username} {", commentParts.getFirst());
        assertEquals("myProjectName", commentParts.get(1));
        assertEquals("} is copied-from. {foo}", commentParts.get(2));

        var parts2 = comments.getCommentParts(null);
        assertEquals(1, parts2.size());
        assertNull(parts2.getFirst());

        var parts3 = comments.getCommentParts("");
        assertEquals(1, parts3.size());
        assertEquals("", parts3.getFirst());

        // Not applied to pattern
        var parts4 = comments.getCommentParts("My comment");
        assertEquals(1, parts4.size());
        assertEquals("My comment", parts4.getFirst());
    }

    @Test
    void testRestoredFrom() {
        var date = Date.from(Instant.parse("2020-06-22T21:02:42.517Z"));
        var actual = comments.restoredFrom("sdsd-s-ds-d-sd-sd", "john", date);
        assertEquals(
                "Project {username} sdsd-s-ds-d-sd-sd is restored-from. Author: john, date: 2020-06-22T21:02:42Z. {foo}",
                actual);
    }

    @Test
    void testRestoredFromWithDollarSign() {
        var date = Date.from(Instant.parse("2020-06-22T21:02:42.517Z"));
        var actualWithSymbol = comments.restoredFrom("$$$12$$3$", "john", date);
        assertEquals(
                "Project {username} $$$12$$3$ is restored-from. Author: john, date: 2020-06-22T21:02:42Z. {foo}",
                actualWithSymbol);
    }

    @Test
    void writesTheMomentInThePatternTheTemplateNames() {
        var date = Date.from(Instant.parse("2020-06-22T21:02:42.517Z"));
        var patterned = new Comments("save", "create", "copied",
                "{revision} of {datetime:yyyy-MM-dd HH:mm}, at {datetime:HH:mm:ss}, as {datetime}", "branch");

        // in UTC, whatever the pattern, so the comment depends on no time zone
        assertEquals("r1 of 2020-06-22 21:02, at 21:02:42, as 2020-06-22T21:02:42Z",
                patterned.restoredFrom("r1", "john", date));
    }

    @Test
    void writesTheLettersTheScreenWritesAndTextInQuotes() {
        var date = Date.from(Instant.parse("2020-06-22T21:02:42.517Z"));
        var patterned = new Comments("save", "create", "copied",
                "{datetime:EEE, d MMM yyyy h:mm:ss.SSS a 'o''clock'''}", "branch");

        assertEquals("Mon, 22 Jun 2020 9:02:42.517 PM o'clock'", patterned.restoredFrom("r1", "john", date));
    }

    @Test
    void writesTheMomentInIsoForAPatternOfLettersTheScreenDoesNotWrite() {
        var date = Date.from(Instant.parse("2020-06-22T21:02:42.517Z"));
        // a zone, a day of the year, two day periods and a text no quote closes
        var others = new Comments("save", "create", "copied",
                "{datetime:HH:mm z}|{datetime:D}|{datetime:hh aa}|{datetime:yyyy 'year}", "branch");

        assertEquals("2020-06-22T21:02:42Z|2020-06-22T21:02:42Z|2020-06-22T21:02:42Z|2020-06-22T21:02:42Z",
                others.restoredFrom("r1", "john", date));
    }

    @Test
    void writesTheMomentInIsoForAPatternThatIsNoPattern() {
        var date = Date.from(Instant.parse("2020-06-22T21:02:42Z"));
        var broken = new Comments("save", "create", "copied", "{datetime:bbb}|{datetime:}|{datetime}", "branch");

        assertEquals("2020-06-22T21:02:42Z|2020-06-22T21:02:42Z|2020-06-22T21:02:42Z",
                broken.restoredFrom("r1", "john", date));
        assertEquals("||", broken.restoredFrom("r1", "john", null));
    }

    @Test
    void testNewBranch() {
        assertEquals("myProjectName/myUserName/myCurrentDate {foo}",
                comments.newBranch("myProjectName", "myUserName", "myCurrentDate"));
        assertEquals("$$$myProj$ectName$$/myUserName/myCurrentDate {foo}",
                comments.newBranch("$$$myProj$ectName$$", "myUserName", "myCurrentDate"));
        assertEquals("Foo岸Бар9-1/myUserName/myCurrentDate {foo}",
                comments.newBranch("Foo岸~^:Бар9-1.", "myUserName", "myCurrentDate"));
    }

    @Test
    void testSimpleComments() {
        assertEquals(TEMPLATE, comments2.saveProject("foo"));
        assertEquals(TEMPLATE, comments2.createProject("foo"));
        assertEquals(TEMPLATE, comments2.copiedFrom("foo"));
        assertEquals(TEMPLATE, comments2.restoredFrom("foo", "bar", new Date()));
        assertEquals("Project {username} {myProjectName} is copied-from. {foo}",
                comments2.getCommentParts("Project {username} {myProjectName} is copied-from. {foo}").getFirst());
    }

}
