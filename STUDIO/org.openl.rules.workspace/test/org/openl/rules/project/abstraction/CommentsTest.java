package org.openl.rules.project.abstraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class CommentsTest {

    private Comments comments;

    @Before
    public void setUp() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a.comment-template.user-message.default.save",
            "Project {username} {{project-name}} is saved. {foo}");
        parameters.put("a.comment-template.user-message.default.create",
            "Project {username} {project-name} is created. {foo}");
        parameters.put("a.comment-template.user-message.default.archive",
            "Project {username} {{project-name} is archived. {foo}");
        parameters.put("a.comment-template.user-message.default.restore",
            "Project {username} '{'{project-name} is restored. {foo}");
        parameters.put("a.comment-template.user-message.default.erase",
            "Project {username} {project-name} is erased. {foo}");
        parameters.put("a.comment-template.user-message.default.copied-from",
            "Project {username} {{project-name}} is copied-from. {foo}");
        parameters.put("a.comment-template.user-message.default.restored-from",
            "Project {username} {revision} is restored-from. {foo}");

        comments = new Comments(parameters, "a.");
    }

    @Test
    public void testSaveProject() {
        String actual = comments.saveProject("myProjectName");
        assertEquals("Project {username} {myProjectName} is saved. {foo}", actual);
    }

    @Test
    public void testCreateProject() {
        String actual = comments.createProject("myProjectName");
        assertEquals("Project {username} myProjectName is created. {foo}", actual);
    }

    @Test
    public void testArchiveProject() {
        String actual = comments.archiveProject("myProjectName");
        assertEquals("Project {username} {myProjectName is archived. {foo}", actual);
    }

    @Test
    public void testRestoreProject() {
        String actual = comments.restoreProject("myProjectName");
        assertEquals("Project {username} '{'myProjectName is restored. {foo}", actual);
    }

    @Test
    public void testEraseProject() {
        String actual = comments.eraseProject("myProjectName");
        assertEquals("Project {username} myProjectName is erased. {foo}", actual);
    }

    @Test
    public void testCopiedFrom() {
        String actual = comments.copiedFrom("myProjectName");
        assertEquals("Project {username} {myProjectName} is copied-from. {foo}", actual);
    }

    @Test
    public void testParseSourceOfCopy() {
        List<String> commentParts = comments
            .getCommentParts("Project {username} {myProjectName} is copied-from. {foo}");
        assertEquals(3, commentParts.size());
        assertEquals("Project {username} {", commentParts.get(0));
        assertEquals("myProjectName", commentParts.get(1));
        assertEquals("} is copied-from. {foo}", commentParts.get(2));

        List<String> parts2 = comments.getCommentParts(null);
        assertEquals(1, parts2.size());
        assertNull(parts2.get(0));

        List<String> parts3 = comments.getCommentParts("");
        assertEquals(1, parts3.size());
        assertEquals("", parts3.get(0));

        // Not applied to pattern
        List<String> parts4 = comments.getCommentParts("My comment");
        assertEquals(1, parts4.size());
        assertEquals("My comment", parts4.get(0));
    }

    @Test
    public void testRestoredFrom() {
        String actual = comments.restoredFrom("sdsd-s-ds-d-sd-sd");
        assertEquals("Project {username} sdsd-s-ds-d-sd-sd is restored-from. {foo}", actual);
    }

}
