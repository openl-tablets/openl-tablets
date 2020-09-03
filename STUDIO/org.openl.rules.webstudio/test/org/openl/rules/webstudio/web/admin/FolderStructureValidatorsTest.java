package org.openl.rules.webstudio.web.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.faces.validator.ValidatorException;

import org.junit.Test;
import org.openl.rules.webstudio.util.NameChecker;

public class FolderStructureValidatorsTest {
    private FolderStructureValidators validators = new FolderStructureValidators();

    @Test
    public void pathInRepository() {
        validators.pathInRepository(null, null, null);
        validators.pathInRepository(null, null, "");
        validators.pathInRepository(null, null, "DESIGN/rules");
        validators.pathInRepository(null, null, "DESIGN/rules/");
        assertInvalid("Path in repository cannot start with '/'",
            () -> validators.pathInRepository(null, null, "/my-folder"));

        assertInvalid(NameChecker.BAD_NAME_MSG, () -> validators.pathInRepository(null, null, "DESIGN/rules/path?"));
        assertInvalid("Invalid name '.git'", () -> validators.pathInRepository(null, null, "DESIGN/rules/.git"));

        String suffix = " is a reserved word.";
        assertInvalid("'NUL'" + suffix, () -> validators.pathInRepository(null, null, "DESIGN/rules/NUL"));
        assertInvalid("'CON'" + suffix, () -> validators.pathInRepository(null, null, "CON"));
        assertInvalid("'AUX'" + suffix, () -> validators.pathInRepository(null, null, "AUX"));
        assertInvalid("'PRN'" + suffix, () -> validators.pathInRepository(null, null, "PRN"));
        for (int i = 1; i < 9; i++) {
            String name = "COM" + i;
            assertInvalid("'" + name + "'" + suffix, () -> validators.pathInRepository(null, null, name));
        }
        for (int i = 1; i < 9; i++) {
            String name = "LPT" + i;
            assertInvalid("'" + name + "'" + suffix, () -> validators.pathInRepository(null, null, name));
        }

        assertInvalid("Invalid name '.git'", () -> validators.pathInRepository(null, null, ".git"));
        assertInvalid("'PRN' is a reserved word.", () -> validators.pathInRepository(null, null, "PRN"));
    }

    private void assertInvalid(String message, Executable executable) {
        try {
            executable.execute();
        } catch (ValidatorException validationException) {
            assertEquals(message, validationException.getMessage());
            return;
        } catch (Throwable actualException) {
            fail("Expected ValidatorException but thrown " + actualException);
        }

        fail("Expected ValidatorException but nothing was thrown ");
    }

    @FunctionalInterface
    public interface Executable {
        void execute() throws Throwable;
    }
}