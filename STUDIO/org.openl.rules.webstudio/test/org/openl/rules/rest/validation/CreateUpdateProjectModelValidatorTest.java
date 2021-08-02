package org.openl.rules.rest.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.CreateUpdateProjectModel;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.MappedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.BindingResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MockConfiguration.class)
@TestPropertySource(properties = { "repository.design-rating.comment-template.use-custom-comments = false",
        "repository.design-rating2.comment-template.use-custom-comments = true",
        "repository.design-rating2.comment-template.comment-validation-pattern = \\\\p{Upper}{3,}-\\\\d+:?\\\\s+[^\\\\s].{4,}",
        "repository.design-rating2.comment-template.invalid-comment-message = Invalid comment" })
public class CreateUpdateProjectModelValidatorTest extends AbstractConstraintValidatorTest {

    private static final Consumer<FeaturesBuilder> NO_EXTRA_FEATURES = builder -> {
    };

    @Autowired
    private CreateUpdateProjectModelValidator validator;

    @Autowired
    private DesignTimeRepository designTimeRepository;

    @After
    public void reset_mocks() {
        Mockito.reset(designTimeRepository);
    }

    @Test
    public void testCreateProject_Valid() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "Example 1 - Bank Rating",
            null,
            null,
            false);

        assertNull(validateAndGetResult(model, validator));
    }

    @Test
    public void testCreateProject_Valid2() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating2", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating2",
            "John Doe",
            "Example 1 - Bank Rating",
            null,
            "EPBDS-10682: Project 'Example 1 - Bank Rating' is created",
            false);

        assertNull(validateAndGetResult(model, validator));
    }

    @Test
    public void testCreateProject_Valid3() throws IOException {
        mockDesignRepository(MappedRepository.class, "design-rating2", NO_EXTRA_FEATURES);
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating2",
            "John Doe",
            "Example 1 - Bank Rating",
            "foo/bar/Example 1 - Bank Rating",
            "EPBDS-10682: Project 'Example 1 - Bank Rating' is created",
            false);

        RulesProject mockProject = mock(RulesProject.class);
        when(mockProject.getRealPath()).thenReturn("foo/bar/foo");
        List res = new ArrayList<AProject>();
        res.add(mockProject);
        when(designTimeRepository.getProjects(model.getRepoName())).thenReturn(res);

        assertNull(validateAndGetResult(model, validator));
    }

    @Test
    public void testCreateProject_NotValid() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel(null, null, null, null, null, false);

        BindingResult bindingResult = validateAndGetResult(model, validator);
        assertEquals(3, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertFieldError("repoName", "Can not be empty", null, bindingResult.getFieldError("repoName"));
        assertFieldError("author", "Can not be empty", null, bindingResult.getFieldError("author"));
        assertFieldError("projectName",
            "Project name must not be empty.",
            null,
            bindingResult.getFieldError("projectName"));
    }

    @Test
    public void testCreateProject_NotValid2() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "Foo?",
            "/foo",
            null,
            false);

        BindingResult bindingResult = validateAndGetResult(model, validator);
        assertEquals(2, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertFieldError("path",
            "Path in repository cannot start with '/'.",
            model.getPath(),
            bindingResult.getFieldError("path"));
        assertFieldError("projectName",
            "Specified name is not a valid project name. Name cannot contain forbidden characters (\\, /, :, ;, <, >, ?, *, %, ', [, ], |, \"), start with space, end with space or dot.",
            model.getProjectName(),
            bindingResult.getFieldError("projectName"));
    }

    @Test
    public void testCreateProject_NotValid3() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "COM6",
            "foo/COM6/bar",
            null,
            false);

        BindingResult bindingResult = validateAndGetResult(model, validator);
        assertEquals(2, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertFieldError("path", "'COM6' is a reserved word.", model.getPath(), bindingResult.getFieldError("path"));
        assertFieldError("projectName",
            "Specified project name is a reserved word.",
            model.getProjectName(),
            bindingResult.getFieldError("projectName"));
    }

    @Test
    public void testCreateProject_NotValid4() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "Hello World",
            "foo/?/bar/Hello World",
            null,
            false);

        BindingResult bindingResult = validateAndGetResult(model, validator);
        assertEquals(1, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertFieldError("path",
            "Name cannot contain forbidden characters (\\, /, :, ;, <, >, ?, *, %, ', [, ], |, \"), start with space, end with space or dot.",
            model.getPath(),
            bindingResult.getFieldError("path"));
    }

    @Test
    public void testCreateProject_NotValid5() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "Example 1 - Bank Rating",
            "foo/bar/Example 1 - Bank Rating",
            null,
            false);
        when(designTimeRepository.hasProject("design-rating", model.getProjectName())).thenReturn(Boolean.TRUE);

        try {
            validateAndGetResult(model, validator);
            fail("Ooops...");
        } catch (ConflictException e) {
            assertEquals("Cannot create project because project with such name already exists.", getLocalMessage(e));
        }
    }

    @Test
    public void testCreateProject_NotValid6() throws IOException {
        MappedRepository mockedRepo = mockDesignRepository(MappedRepository.class, "design-rating", NO_EXTRA_FEATURES);
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "Example 1 - Bank Rating",
            "foo/bar/Example 1 - Bank Rating",
            null,
            false);

        when(mockedRepo.check(eq(model.getFullPath()))).thenReturn(mock(FileData.class));

        try {
            validateAndGetResult(model, validator);
            fail("Ooops...");
        } catch (ConflictException e) {
            assertEquals(
                "Cannot create the project because a project with such path already exists. Try to import that project from repository or create new project with another path or name.",
                getLocalMessage(e));
        }
    }

    @Test
    public void testCreateProject_NotValid7() throws IOException {
        mockDesignRepository(MappedRepository.class, "design-rating", NO_EXTRA_FEATURES);
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "Example 1 - Bank Rating",
            "foo/bar/Example 1 - Bank Rating",
            null,
            false);

        RulesProject mockProject = mock(RulesProject.class);
        when(mockProject.getRealPath()).thenReturn("foo");
        List res = new ArrayList<AProject>();
        res.add(mockProject);
        when(designTimeRepository.getProjects(model.getRepoName())).thenReturn(res);

        try {
            validateAndGetResult(model, validator);
            fail("Ooops...");
        } catch (ConflictException e) {
            assertEquals("Cannot create the project because a path conflicts with an existed project.", getLocalMessage(e));
        }
    }

    @Test
    public void testCreateProject_NotValid8() throws IOException {
        mockDesignRepository(MappedRepository.class, "design-rating", NO_EXTRA_FEATURES);
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
            "John Doe",
            "Example 1 - Bank Rating",
            "foo/bar/Example 1 - Bank Rating",
            null,
            false);

        RulesProject mockProject = mock(RulesProject.class);
        when(mockProject.getRealPath()).thenReturn("foo/bar/Example 1 - Bank Rating/foo");
        List res = new ArrayList<AProject>();
        res.add(mockProject);
        when(designTimeRepository.getProjects(model.getRepoName())).thenReturn(res);

        try {
            validateAndGetResult(model, validator);
            fail("Ooops...");
        } catch (ConflictException e) {
            assertEquals("Cannot create the project because a path conflicts with an existed project.", getLocalMessage(e));
        }
    }

    @Test
    public void testCreateProject_NotValid9() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating2", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating2",
            "John Doe",
            "Example 1 - Bank Rating",
            "foo/Example 1 - Bank Rating",
            "Project is created",
            false);

        BindingResult bindingResult = validateAndGetResult(model, validator);
        assertEquals(2, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());

        assertFieldError("path",
            "Design Repository doesn't support folders. Path must be empty.",
            model.getPath(),
            bindingResult.getFieldError("path"));

        assertFieldError("comment", "Invalid comment.", model.getComment(), bindingResult.getFieldError("comment"));
    }

    @Test
    public void testCreateProject_NotValid10() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
                "John Doe",
                "Foo",
                "foo//bar",
                null,
                false);

        BindingResult bindingResult = validateAndGetResult(model, validator);
        assertEquals(1, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertFieldError("path",
                "Name cannot contain forbidden characters (\\, /, :, ;, <, >, ?, *, %, ', [, ], |, \"), start with space, end with space or dot.",
                model.getPath(),
                bindingResult.getFieldError("path"));
    }

    @Test
    public void testCreateProject_NotValid11() throws IOException {
        mockDesignRepository(FolderRepository.class, "design-rating", builder -> builder.setVersions(true));
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating",
                "John Doe",
                "Foo",
                "/foo/",
                null,
                false);

        BindingResult bindingResult = validateAndGetResult(model, validator);
        assertEquals(2, bindingResult.getFieldErrorCount());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertTrue(bindingResult.getFieldErrors("path").stream().anyMatch(error -> "Path in repository cannot end with '/'.".equals(error.getDefaultMessage())));
        assertTrue(bindingResult.getFieldErrors("path").stream().anyMatch(error -> "Path in repository cannot start with '/'.".equals(error.getDefaultMessage())));
    }

    @Test
    public void testUpdateProject() throws IOException, ProjectException {
        mockDesignRepository(MappedRepository.class, "design-rating2", NO_EXTRA_FEATURES);
        CreateUpdateProjectModel model = new CreateUpdateProjectModel("design-rating2",
            "John Doe",
            "Example 1 - Bank Rating",
            "foo/bar/Example 1 - Bank Rating",
            "EPBDS-10682: Project 'Example 1 - Bank Rating' is created",
            true);
        RulesProject mockProject = mock(RulesProject.class);
        when(mockProject.getRealPath()).thenReturn("foo/bar/Example 1 - Bank Rating");

        when(designTimeRepository.hasProject("design-rating2", model.getProjectName())).thenReturn(Boolean.TRUE);
        when(designTimeRepository.getProject("design-rating2", model.getProjectName())).thenReturn(mockProject);

        assertNull(validateAndGetResult(model, validator));

        when(mockProject.getRealPath()).thenReturn("foo/bar/foo");
        try {
            validateAndGetResult(model, validator);
            fail("Ooops...");
        } catch (NotFoundException e) {
            assertEquals("Project 'Example 1 - Bank Rating' is not found.", getLocalMessage(e));
        }

        when(mockProject.getRealPath()).thenAnswer(inwok -> {
            throw new ProjectException("Project 'Example 1 - Bank Rating' is not found.");
        });
        try {
            validateAndGetResult(model, validator);
            fail("Ooops...");
        } catch (NotFoundException e) {
            assertEquals("Project 'Example 1 - Bank Rating' is not found.", getLocalMessage(e));
        }
    }

    private <T extends Repository> T mockDesignRepository(Class<T> tClass,
            String repoName,
            Consumer<FeaturesBuilder> featureConfig) throws IOException {
        T mockedRepo = mock(tClass);
        when(designTimeRepository.getRepository(repoName)).thenReturn(mockedRepo);

        when(mockedRepo.check(anyString())).thenReturn(null);

        FeaturesBuilder featuresBuilder = new FeaturesBuilder(mockedRepo);
        if (MappedRepository.class.isAssignableFrom(tClass)) {
            when(((MappedRepository) mockedRepo).getDelegate()).thenReturn(((MappedRepository) mockedRepo));
            featuresBuilder.setMappedFolders(true);
        }
        featureConfig.accept(featuresBuilder);
        when(mockedRepo.supports()).thenReturn(featuresBuilder.build());

        return mockedRepo;
    }

}
