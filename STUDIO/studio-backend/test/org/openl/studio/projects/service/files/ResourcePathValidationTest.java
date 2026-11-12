package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.rules.webstudio.util.NameChecker;
import org.openl.studio.common.exception.BadRequestException;

/**
 * Checks that a refused path of a project file says why it is refused.
 *
 * @author Yury Molchan
 */
class ResourcePathValidationTest {

    @Test
    void acceptsAPathOfPlainNames() {
        assertDoesNotThrow(() -> ProjectFilesServiceImpl.validateResourcePath("rules/Main copy.xlsx"));
    }

    @Test
    void namesTheForbiddenCharacters() {
        var error = assertThrows(BadRequestException.class,
                () -> ProjectFilesServiceImpl.validateResourcePath("rules/a<b.xlsx"));

        assertEquals("openl.error.400.file.path.forbidden.message", error.getErrorCode());
        assertArrayEquals(new Object[]{NameChecker.FORBIDDEN_CHARS_STRING}, error.getArgs());
    }

    @Test
    void namesTheReservedName() {
        var error = assertThrows(BadRequestException.class,
                () -> ProjectFilesServiceImpl.validateResourcePath("rules/Con.xlsx"));

        assertEquals("openl.error.400.file.path.reserved.message", error.getErrorCode());
        assertArrayEquals(new Object[]{"Con.xlsx"}, error.getArgs());
    }

    @Test
    void keepsAnExistingFileOfAReservedNameReachable() {
        assertDoesNotThrow(() -> ProjectFilesServiceImpl.validateExistingPath("rules/Con.xlsx"));
    }

    @Test
    void refusesForbiddenCharactersInAnExistingPath() {
        var error = assertThrows(BadRequestException.class,
                () -> ProjectFilesServiceImpl.validateExistingPath("rules/../a.xlsx"));

        assertEquals("openl.error.400.file.path.forbidden.message", error.getErrorCode());
    }
}
