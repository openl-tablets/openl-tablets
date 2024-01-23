package org.openl.rules.rest.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.repository.api.BranchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Vladyslav Pikus
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MockConfiguration.class)
public class NewBranchValidatorTest extends AbstractConstraintValidatorTest {

    private NewBranchValidator validator;
    private BranchRepository branchRepository;

    @Before
    public void setUp() throws IOException {
        branchRepository = mock(BranchRepository.class);
        when(branchRepository.isValidBranchName(any())).thenReturn(true);
        when(branchRepository.getBranches(null)).thenReturn(List.of());
        validator = new NewBranchValidator(branchRepository, null, null);
    }

    @Test
    public void test_validate_basic() {
        var result = validateAndGetResult(" ", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("The branch name cannot be empty.", result.getGlobalError());

        result = validateAndGetResult("branch\\name", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError(
            "Invalid branch name. Must not contain whitespaces or following characters: \\ : * ? \" < > | { } ~ ^",
            result.getGlobalError());

        result = validateAndGetResult("./branch-name", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Invalid branch name. Cannot start with '.' or '/'.", result.getGlobalError());

        result = validateAndGetResult("foo/./branch-name", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Invalid branch name. Should not contain consecutive symbols '.' or '/'.",
            result.getGlobalError());

        result = validateAndGetResult(".lock", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Invalid branch name. Should not contain '.lock/' or end with '.lock'.",
            result.getGlobalError());

        result = validateAndGetResult(".lock/foo", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Invalid branch name. Should not contain '.lock/' or end with '.lock'.",
            result.getGlobalError());
    }

    @Test
    public void test_validate_repository() {
        var branchName = "invalid_branch_name";
        when(branchRepository.isValidBranchName(branchName)).thenReturn(false);

        var result = validateAndGetResult(branchName, validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Branch name contains reserved words or symbols.", result.getGlobalError());
    }

    @Test
    public void test_validate_repository_1() throws IOException {
        when(branchRepository.getBranches(null)).thenReturn(List.of("FoO"));

        var result = validateAndGetResult("foo", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Branch 'foo' already exists in repository.", result.getGlobalError());
    }

    @Test
    public void test_validate_repository_2() throws IOException {
        when(branchRepository.getBranches(null)).thenReturn(List.of("foo"));

        var result = validateAndGetResult("foo/bar", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Cannot create the branch 'foo/bar' because the branch 'foo' already exists.\n" +
                "Explanation: for example a branch 'foo/bar' exists. That branch can be considered as a file bar located in the folder 'foo'.\n" +
                "So you cannot create a branch 'foo/bar/baz' because you cannot create the folder 'foo/bar': the file with such name already exists.", result.getGlobalError());
    }

    @Test
    public void test_validate() {
        assertNull(validateAndGetResult("foo", validator));
        assertNull(validateAndGetResult("foo/bar", validator));
    }

    @Test
    public void test_validate_customRegex() {
        validator = new NewBranchValidator(branchRepository,
            "^[a-z0-9_\\-]+$",
            "Only lowercase letters, numbers, underscores and dashes are allowed.");
        assertNull(validateAndGetResult("foo", validator));

        var result = validateAndGetResult("FOOOO", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Only lowercase letters, numbers, underscores and dashes are allowed.",
            result.getGlobalError());
    }

    @Test
    public void test_validate_customRegex_noCustomMessage() {
        validator = new NewBranchValidator(branchRepository, "^[a-z0-9_\\-]+$", null);

        var result = validateAndGetResult("FOOOO", validator);
        assertEquals(0, result.getFieldErrorCount());
        assertEquals(1, result.getGlobalErrorCount());
        assertObjectError("Branch name must match the following pattern: ^[a-z0-9_\\-]+$.", result.getGlobalError());
    }

}
