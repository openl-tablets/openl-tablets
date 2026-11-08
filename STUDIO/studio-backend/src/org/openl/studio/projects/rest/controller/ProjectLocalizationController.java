package org.openl.studio.projects.rest.controller;

import java.util.List;
import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Pageable;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.common.model.SortOrder;
import org.openl.studio.projects.model.localization.LocalizationActions;
import org.openl.studio.projects.model.localization.LocalizationKeyView;
import org.openl.studio.projects.model.localization.LocalizationMessagesPage;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessagePositions;
import org.openl.studio.projects.rest.annotations.ProjectId;
import org.openl.studio.projects.service.DetailedMessageDescriptionMapper;
import org.openl.studio.projects.service.WorkspaceProjectService;
import org.openl.studio.projects.service.localization.LocalizationQuery;
import org.openl.studio.projects.service.localization.ProjectLocalizationService;
import org.openl.studio.rest.resolver.PaginationDefault;

/**
 * REST controller for the localization files of a project: the {@code message*.properties} files of its {@code i18n}
 * folder.
 *
 * <p>The messages of an open project are read once into the model of the session and kept. The tree of the keys and
 * the pages of the keys with their texts in every language are answered from what is kept, and a change of the files
 * is made by actions sent together, which write only what they change.
 *
 * <p>The files are checked without compiling the project, so their problems are told at once. What a compilation
 * finds about the calls of {@code msg} in the rules comes with the status of the project.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/projects/{projectId}/localization", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Projects: Localization (BETA)", description = "Experimental localization API")
@Validated
public class ProjectLocalizationController {

    private final WorkspaceProjectService workspaceProjectService;
    private final DetailedMessageDescriptionMapper messageMapper;
    private final ProjectLocalizationService localizationService;

    @GetMapping("/keys")
    @Operation(summary = "projects.localization.keys.summary", description = "projects.localization.keys.desc")
    public List<LocalizationKeyView> getKeys(@ProjectId @PathVariable("projectId") RulesProject project) {
        return localizationService.keys(project);
    }

    @GetMapping
    @Operation(summary = "projects.localization.messages.summary", description = "projects.localization.messages.desc")
    public LocalizationMessagesPage getMessages(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "search", required = false)
            @Parameter(description = "projects.localization.param.search.desc") @Nullable String search,
            @RequestParam(value = "problems", defaultValue = "false")
            @Parameter(description = "projects.localization.param.problems.desc") boolean problems,
            @RequestParam(value = "untranslated", required = false)
            @Parameter(description = "projects.localization.param.untranslated.desc")
            @Nullable List<String> untranslated,
            @RequestParam(value = "sort", required = false)
            @Parameter(description = "projects.localization.param.sort.desc") @Nullable SortOrder sort,
            @RequestParam(value = "key", required = false)
            @Parameter(description = "projects.localization.messages.param.key.desc") @Nullable String key,
            @PaginationDefault Pageable page) {
        var query = new LocalizationQuery(search, problems, untranslated == null ? List.of() : untranslated, sort);
        return localizationService.messages(project, query, key, page);
    }

    @PostMapping(value = "/actions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "projects.localization.actions.summary", description = "projects.localization.actions.desc")
    @ApiResponse(responseCode = "204", description = "projects.localization.actions.204.desc")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void applyActions(@ProjectId @PathVariable("projectId") RulesProject project,
                             @Valid @RequestBody LocalizationActions actions) {
        localizationService.apply(project, actions.actions());
    }

    @GetMapping("/problems")
    @Operation(summary = "projects.localization.problems.summary",
            description = "projects.localization.problems.desc")
    @JsonView(GenericView.Detailed.class)
    public List<DetailedMessageDescription> getProblems(
            @ProjectId @PathVariable("projectId") RulesProject project,
            @RequestParam(value = "positions", defaultValue = "file")
            @Parameter(description = "projects.localization.problems.param.positions.desc")
            MessagePositions positions) {
        // A project nobody opened has no files in the workspace to read: it is refused, as by the other APIs.
        var descriptor = workspaceProjectService.getProjectDescriptor(project);
        var messages = workspaceProjectService.checkLocalization(descriptor);
        return messageMapper.mapFileMessages(messages, descriptor.getProjectFolder(), positions);
    }
}
