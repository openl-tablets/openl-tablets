package org.openl.rules.rest;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.acl.model.AclRepositoryId;
import org.openl.rules.rest.model.RepositoryViewModel;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.security.acl.repository.AclRepositoryType;

@RestController
@RequestMapping(value = "/production-repos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Production Repository")
public class ProductionRepositoryController {

    private final SecureDeploymentRepositoryService deploymentRepositoryService;

    public ProductionRepositoryController(SecureDeploymentRepositoryService deploymentRepositoryService) {
        this.deploymentRepositoryService = deploymentRepositoryService;
    }

    @GetMapping
    @Operation(summary = "Get a list of Production Repositories", description = "Get a list of configured Production Repositories.")
    @ApiResponse(responseCode = "200", description = "List the configured Production Repositories.")
    public List<RepositoryViewModel> getRepositoryList() {
        return deploymentRepositoryService.getRepositories()
                .stream()
                .map(repo -> RepositoryViewModel.builder()
                        .id(repo.getId())
                        .name(repo.getName())
                        .aclId(AclRepositoryId.builder()
                                .id(repo.getId())
                                .type(AclRepositoryType.PROD)
                                .build())
                        .build())
                .collect(Collectors.toList());
    }
}
