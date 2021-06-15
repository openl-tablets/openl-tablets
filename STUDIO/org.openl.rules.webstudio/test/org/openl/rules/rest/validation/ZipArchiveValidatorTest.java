package org.openl.rules.rest.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BindingResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MockConfiguration.class)
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
        assertObjectError("Provided file is not an archive.", bindingResult.getGlobalError());

        bindingResult = validateAndGetResult(Paths.get("test-resources/log4j2-test.properties"), validator);
        assertEquals(0, bindingResult.getFieldErrorCount());
        assertEquals(1, bindingResult.getGlobalErrorCount());
        assertObjectError("Provided file is not an archive.", bindingResult.getGlobalError());
    }

    @Test
    public void testArchives() {
        assertNull(validateAndGetResult(Paths.get("test-resources/upload/zip/test-workspace.zip"), validator));
        assertNull(validateAndGetResult(Paths.get("test-resources/upload/zip/project.zip"), validator));
    }

}
