/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.model.CreateBranchModel;
import org.openl.rules.rest.model.ProjectStatusUpdateModel;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.rest.service.ProjectCriteriaQuery;
import org.openl.rules.rest.service.WorkspaceProjectService;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.rest.validation.NewBranchValidator;
import org.openl.rules.ui.WebStudio;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Projects REST controller
 *
 * @author Vladyslav Pikus
 */
@Hidden
@RestController
@RequestMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProjectsController {

    private static final String TAGS_PREFIX = "tags.";

    private final WorkspaceProjectService projectService;
    private final Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory;
    private final BeanValidationProvider validationProvider;

    public ProjectsController(WorkspaceProjectService projectService,
            Function<BranchRepository, NewBranchValidator> newBranchValidatorFactory,
            BeanValidationProvider validationProvider) {
        this.projectService = projectService;
        this.newBranchValidatorFactory = newBranchValidatorFactory;
        this.validationProvider = validationProvider;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping
    public List<ProjectViewModel> getProjects(@RequestParam Map<String, String> params,
            @RequestParam(value = "status", required = false) ProjectStatus status,
            @RequestParam(value = "repository", required = false) String repository) {

        var queryBuilder = ProjectCriteriaQuery.builder().repositoryId(repository).status(status);

        params.entrySet()
            .stream()
            .filter(entry -> entry.getKey().startsWith(TAGS_PREFIX))
            .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
            .forEach(entry -> {
                var tag = entry.getKey().substring(TAGS_PREFIX.length());
                queryBuilder.tag(tag, entry.getValue());
            });

        return projectService.getProjects(queryBuilder.build());
    }

    @PatchMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProjectStatus(@PathVariable("projectId") RulesProject project,
            @RequestBody ProjectStatusUpdateModel request) {
        try {
            projectService.updateProjectStatus(project, request);
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.status.update.failed.message");
        }
    }

    @PostMapping("/{projectId}/branches")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createBranch(@PathVariable("projectId") RulesProject project, @RequestBody CreateBranchModel request) {
        var repository = project.getDesignRepository();
        if (!project.isSupportsBranches()) {
            throw new ConflictException("project.branch.unsupported.message");
        }
        var validator = newBranchValidatorFactory.apply((BranchRepository) repository);
        validationProvider.validate(request.getBranch(), validator);
        try {
            projectService.createBranch(project, request);
            getWebStudio().reset();
        } catch (ProjectException e) {
            throw new ConflictException("project.branch.create.failed.message");
        }
    }

}
