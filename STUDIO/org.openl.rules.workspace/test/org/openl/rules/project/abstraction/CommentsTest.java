package org.openl.rules.project.abstraction;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
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
        String actual = comments.parseSourceOfCopy("Project {username} {myProjectName} is copied-from. {foo}");
        assertEquals("myProjectName", actual);
    }

    @Test
    public void testRestoredFrom() {
        String actual = comments.restoredFrom("sdsd-s-ds-d-sd-sd");
        assertEquals("Project {username} sdsd-s-ds-d-sd-sd is restored-from. {foo}", actual);
    }

}
