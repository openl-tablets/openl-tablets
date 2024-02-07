package org.openl.rules.rest.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.BindingResult;

@SpringJUnitConfig(classes = MockConfiguration.class)
public class ZipArchiveValidatorTest extends AbstractConstraintValidatorTest {

    @Autowired
    private ZipArchiveValidator validator;

    @Test
    public void testArchives_NotOpenLProject() {
        BindingResult bindingResult = validateAndGetResult(Paths.get("test-resources/upload/zip/test-rules-xml.zip"),
                validator);
        assertEquals(0, bindingResult.getFieldErrorCount());
        assertEquals(1, bindingResult.getGlobalErrorCount());
        assertObjectError("Unknown project structure.", bindingResult.getGlobalError());

        bindingResult = validateAndGetResult(Paths.get("test-resources/XSSFOptimizerTest.xlsx"), validator);
        assertEquals(0, bindingResult.getFieldErrorCount());
        assertEquals(1, bindingResult.getGlobalErrorCount());
        assertObjectError("Unknown project structure.", bindingResult.getGlobalError());
    }

    @Test
    public void testArchives_NotArchive() {
        BindingResult bindingResult = validateAndGetResult(Paths.get("test-resources/test/export/trivial"), validator);
        assertEquals(0, bindingResult.getFieldErrorCount());
        assertEquals(1, bindingResult.getGlobalErrorCount());
        assertObjectError("The provided file is not an archive.", bindingResult.getGlobalError());

        bindingResult = validateAndGetResult(Paths.get("test-resources/log4j2-test.properties"), validator);
        assertEquals(0, bindingResult.getFieldErrorCount());
        assertEquals(1, bindingResult.getGlobalErrorCount());
        assertObjectError("The provided file is not an archive.", bindingResult.getGlobalError());
    }

    @Test
    public void testArchives() {
        assertNull(validateAndGetResult(Paths.get("test-resources/upload/zip/test-workspace.zip"), validator));
        assertNull(validateAndGetResult(Paths.get("test-resources/upload/zip/project.zip"), validator));
    }

}
