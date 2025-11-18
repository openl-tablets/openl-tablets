package org.openl.rules.webstudio.repositories.rest.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.model.RepositoryViewModel;
import org.openl.rules.webstudio.repositories.service.ProductionRepositoryService;

@RestController
@RequestMapping(value = "/production-repos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Production Repository")
public class ProductionRepositoryController {

    private final ProductionRepositoryService productionRepositoryService;

    public ProductionRepositoryController(ProductionRepositoryService productionRepositoryService) {
        this.productionRepositoryService = productionRepositoryService;
    }

    @GetMapping
    @Operation(summary = "Get a list of Production Repositories", description = "Get a list of configured Production Repositories.")
    @ApiResponse(responseCode = "200", description = "List the configured Production Repositories.")
    public List<RepositoryViewModel> getRepositoryList() {
        return productionRepositoryService.getRepositoryList();
    }
}
