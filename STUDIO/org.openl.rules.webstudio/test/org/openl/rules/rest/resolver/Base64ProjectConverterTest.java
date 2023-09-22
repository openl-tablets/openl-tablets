/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Vladyslav Pikus
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Base64ProjectConverterTest.TestConfig.class)
public class Base64ProjectConverterTest {

    @Autowired
    private Base64ProjectConverter projectConverter;

    @Autowired
    private RepositoryAclService designRepositoryAclService;

    @Autowired
    private UserWorkspace userWorkspace;

    @Before
    public void setUp() {
        Mockito.reset(designRepositoryAclService, userWorkspace);
    }

    @Test
    public void test_convert() throws ProjectException {
        String projectPath = "projectPath/foo";
        String projectName = "projectName";
        String projectId = encode(projectPath, projectName);

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject(projectPath, projectName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(AclPermission.VIEW))).thenReturn(true);

        assertSame(rulesProject, projectConverter.convert(projectId));

        verify(designRepositoryAclService).isGranted(rulesProject, List.of(AclPermission.VIEW));
    }

    @Test
    public void test_convert_securityError() throws ProjectException {
        String projectPath = "projectPath/foo";
        String projectName = "projectName";
        String projectId = encode(projectPath, projectName);

        var rulesProject = mock(RulesProject.class);
        when(userWorkspace.getProject(projectPath, projectName)).thenReturn(rulesProject);
        when(designRepositoryAclService.isGranted(rulesProject, List.of(AclPermission.VIEW))).thenReturn(false);

        assertThrows(SecurityException.class, () -> projectConverter.convert(projectId));
    }

    @Test
    public void test_convert_incorrectId() {
        assertThrows(IllegalArgumentException.class, () -> projectConverter.convert("fooBar:asasas"));
        var encoder = Base64.getEncoder();
        var id = encoder.encodeToString("fooBar".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> projectConverter.convert(id));
    }

    @Test
    public void test_convert_notFound() throws ProjectException {
        String projectPath = "projectPath";
        String projectName = "projectName";
        String projectId = encode(projectPath, projectName);

        when(userWorkspace.getProject(projectPath, projectName)).thenThrow(new ProjectException("Not found"));

        var ex = assertThrows(NotFoundException.class, () -> projectConverter.convert(projectId));
        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }

    private String encode(String projectPath, String projectName) {
        var projectIdentifier = projectPath + Base64ProjectConverter.SEPARATOR + projectName;
        return Base64.getEncoder().encodeToString(projectIdentifier.getBytes(StandardCharsets.UTF_8));
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public Base64ProjectConverter projectConverter(RepositoryAclService designRepositoryAclService) {
            return new Base64ProjectConverter(designRepositoryAclService) {
                // just a wokraround for @Lookup
                @Override
                public UserWorkspace getUserWorkspace() {
                    return userWorkspace();
                }
            };
        }

        @Bean
        public RepositoryAclService designRepositoryAclService() {
            return mock(RepositoryAclService.class);
        }

        @Bean
        public UserWorkspace userWorkspace() {
            return mock(UserWorkspace.class);
        }
    }
}
