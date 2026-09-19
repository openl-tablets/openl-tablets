package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.openl.studio.projects.service.files.ProjectFilesService.UploadedFile;

class ProjectFilesServiceTest {

    private static UploadedFile file(String name, String content) {
        return new UploadedFile(name, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void filesHoldingEqualBytesAreEqual() {
        var one = file("Main.xlsx", "content");
        var other = file("Main.xlsx", "content");

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void filesDifferingInNameOrBytesAreNotEqual() {
        var file = file("Main.xlsx", "content");

        assertNotEquals(file, file("Other.xlsx", "content"));
        assertNotEquals(file, file("Main.xlsx", "other"));
        assertNotEquals(file, new Object());
    }

    @Test
    void toStringReportsTheSizeInsteadOfTheBytes() {
        assertEquals("UploadedFile[name=Main.xlsx, content=7 bytes]", file("Main.xlsx", "content").toString());
        assertEquals("UploadedFile[name=Main.xlsx, content=0 bytes]", new UploadedFile("Main.xlsx", null).toString());
    }
}
