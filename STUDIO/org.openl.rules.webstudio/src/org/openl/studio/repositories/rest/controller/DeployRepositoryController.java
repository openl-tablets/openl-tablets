package org.openl.studio.repositories.rest.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.repositories.model.RepositoryViewModel;
import org.openl.studio.repositories.service.DeploymentRepositoryService;

@RestController
@RequestMapping(value = "/production-repos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Production Repository")
public class DeployRepositoryController {

    private final DeploymentRepositoryService deploymentRepositoryService;

    public DeployRepositoryController(DeploymentRepositoryService deploymentRepositoryService) {
        this.deploymentRepositoryService = deploymentRepositoryService;
    }

    @GetMapping
    @Operation(summary = "Get a list of Deployment Repositories", description = "Get a list of configured Deployment Repositories.")
    @ApiResponse(responseCode = "200", description = "List the configured Production Repositories.")
    public List<RepositoryViewModel> getRepositoryList() {
        return deploymentRepositoryService.getRepositoryList();
    }
}
