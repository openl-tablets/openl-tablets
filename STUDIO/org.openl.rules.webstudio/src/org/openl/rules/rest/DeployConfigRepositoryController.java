package org.openl.rules.rest;

import java.io.IOException;

import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.PageResponse;
import org.openl.rules.rest.model.ProjectRevision;
import org.openl.rules.rest.model.RepositoryFeatures;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.rules.rest.resolver.PaginationDefault;
import org.openl.rules.rest.service.HistoryRepositoryMapper;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/deploy-config-repo")
@Tag(name = "Deploy Configuration Repository")
public class DeployConfigRepositoryController {

    private final DesignTimeRepository designTimeRepository;

    public DeployConfigRepositoryController(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    @Lookup
    protected HistoryRepositoryMapper getHistoryRepositoryMapper(Repository repository) {
        return null;
    }

    @GetMapping("/features")
    @Operation(summary = "repos.get-features.summary", description = "repos.get-features.desc")
    public RepositoryFeatures getFeatures() {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        var supports = getDeployConfigRepository().supports();
        return new RepositoryFeatures(false, supports.searchable());
    }

    @GetMapping("/configs/{config-name}/history")
    @Parameters({
            @Parameter(name = "page", description = "pagination.param.page.desc", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0")),
            @Parameter(name = "size", description = "pagination.param.size.desc", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32", minimum = "1", defaultValue = "50")) })
    @Operation(summary = "repos.get-project-revs.summary", description = "repos.get-project-revs.desc")
    @JsonView({ UserInfoModel.View.Short.class })
    public PageResponse<ProjectRevision> getProjectRevision(
            @Parameter(description = "deploy-repo.param.config-name.desc") @PathVariable("config-name") String name,
            @Parameter(description = "repo.param.search.desc") @RequestParam(value = "search", required = false) String searchTerm,
            @PaginationDefault(size = 50) Pageable page) throws IOException {
        SecurityChecker.allow(Privileges.VIEW_PROJECTS);
        Repository repository = getDeployConfigRepository();
        if (!designTimeRepository.hasDDProject(name)) {
            throw new NotFoundException("project.message", name);
        }
        String fullPath = designTimeRepository.getDeployConfigLocation() + name;
        return getHistoryRepositoryMapper(repository).getProjectHistory(fullPath, searchTerm, false, page);
    }

    private Repository getDeployConfigRepository() {
        Repository repository = designTimeRepository.getDeployConfigRepository();
        if (repository == null) {
            throw new NotFoundException("deploy-config.repo.message");
        }
        return repository;
    }
}