package org.openl.studio.tags.rest.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.tags.model.TagDTO;
import org.openl.studio.tags.model.TagTypeView;
import org.openl.studio.tags.service.TagTypeService;

/**
 * Read-only access to the tag-type catalog for tagging projects. Any authenticated user may read it,
 * unlike the admin-only tag configuration API. Writing tag types stays under {@code /admin/tag-config}.
 */
@RestController
@RequestMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tags")
@RequiredArgsConstructor
public class TagsController {

    private final TagTypeService tagTypeService;

    @Operation(summary = "List tag types", description = "Lists the configured tag types with their allowed values, for tagging projects.")
    @GetMapping("/types")
    public List<TagTypeView> getTagTypes() {
        return tagTypeService.getAll().stream()
                .map(type -> new TagTypeView(type.getName(), type.isExtensible(), type.isNullable(),
                        type.getTags().stream().map(TagDTO::getName).toList()))
                .toList();
    }
}
