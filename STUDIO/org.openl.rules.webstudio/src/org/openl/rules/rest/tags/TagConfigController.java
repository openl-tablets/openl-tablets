package org.openl.rules.rest.tags;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.openl.rules.rest.SecurityChecker;
import org.openl.rules.rest.exception.BadRequestException;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.rest.model.GenericView;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping(value = "/admin/tag-config")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags")
public class TagConfigController {

    private final TagTypeService tagTypeService;
    private final TagService tagService;

    @Autowired
    public TagConfigController(TagTypeService tagTypeService, TagService tagService) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
    }

    @Operation(summary = "tags.get-types.summary", description = "tags.get-types.desc")
    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagTypeDTO> getTypes() {
        SecurityChecker.allow(Privileges.ADMIN);
        return tagTypeService.getAll();
    }

    @Operation(summary = "tags.delete-tag-type.summary", description = "tags.delete-tag-type.desc")
    @DeleteMapping(value = "/types/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTagType(@Parameter(description = "tags.tag-type.id.desc") @PathVariable("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        if (!tagTypeService.delete(id)) {
            throw new NotFoundException("tag-type.message");
        }
    }

    @Operation(summary = "tags.delete-tag.summary", description = "tags.delete-tag.desc")
    @DeleteMapping("/types/{tagTypeId}/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(
            @Parameter(description = "tags.tag-type.id.desc") @PathVariable("tagTypeId") final Long tagTypeId,
            @Parameter(description = "tags.tag.id.desc") @PathVariable("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        final Tag tag = tagService.getById(id);
        if (tag == null || !Objects.equals(tag.getType().getId(), tagTypeId)) {
            throw new NotFoundException("tag.message");
        }
        if (!tagService.delete(id)) {
            throw new NotFoundException("tag.message");
        }
    }

    @Operation(summary = "tags.add-tag-type.summary", description = "tags.add-tag-type.desc")
    @PostMapping(value = "/types", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "201", description = "Created", headers = @Header(name = HttpHeaders.LOCATION, required = true))
    public ResponseEntity<Void> addTagType(@JsonView(GenericView.CreateOrUpdate.class) @RequestBody TagTypeDTO typeDTO,
            HttpServletRequest request) {
        return addOrUpdateTagType(null, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible(), request);
    }

    @Operation(summary = "tags.update-tag-type.summary", description = "tags.update-tag-type.desc")
    @PutMapping(value = "/types/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "204", description = "Updated")
    public ResponseEntity<Void> updateTagType(
            @Parameter(description = "tags.tag-type.id.desc") @PathVariable("id") final Long id,
            @JsonView(GenericView.CreateOrUpdate.class) @RequestBody TagTypeDTO typeDTO) {
        return addOrUpdateTagType(id, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible(), null);
    }

    private ResponseEntity<Void> addOrUpdateTagType(final Long id,
            final String name,
            final Boolean nullable,
            final Boolean extensible,
            final HttpServletRequest request) {
        SecurityChecker.allow(Privileges.ADMIN);
        final TagType tagType;

        if (id == null) {
            tagType = new TagType();
        } else {
            tagType = tagTypeService.getById(id);
            if (tagType == null) {
                throw new NotFoundException("tag-type.message");
            }
        }

        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("cannot.be.empty.message");
        }

        if (!NameChecker.checkName(name)) {
            throw new BadRequestException("invalid.name.message");
        }

        final TagType existing = tagTypeService.getByName(name);
        if (existing != null && !existing.getId().equals(id)) {
            throw new ConflictException("duplicated.tag-type.message", name);
        }

        tagType.setName(name);

        if (nullable != null) {
            tagType.setNullable(nullable);
        }
        if (extensible != null) {
            tagType.setExtensible(extensible);
        }

        if (id == null) {
            tagTypeService.save(tagType);
            return created(request, tagType.getId());
        } else {
            tagTypeService.update(tagType);
            return ResponseEntity.noContent().build();
        }
    }

    @Operation(summary = "tags.add-tag.summary", description = "tags.add-tag.desc")
    @PostMapping(value = "/types/{tagTypeId}/tags", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ApiResponse(responseCode = "201", description = "Created", headers = @Header(name = HttpHeaders.LOCATION, required = true))
    public ResponseEntity<Void> addTag(
            @Parameter(description = "tags.tag-type.id.desc") @PathVariable("tagTypeId") final Long tagTypeId,
            @RequestBody final String name,
            HttpServletRequest request) {
        return addOrUpdateTag(tagTypeId, null, name, request);
    }

    @Operation(summary = "tags.update-tag.summary", description = "tags.update-tag.desc")
    @PutMapping(value = "/types/{tagTypeId}/tags/{tagId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ApiResponse(responseCode = "204", description = "Updated")
    public ResponseEntity<Void> updateTag(
            @Parameter(description = "tags.tag-type.id.desc") @PathVariable("tagTypeId") final Long tagTypeId,
            @Parameter(description = "tags.tag.id.desc") @PathVariable("tagId") final Long tagId,
            @RequestBody final String name) {
        return addOrUpdateTag(tagTypeId, tagId, name, null);
    }

    private ResponseEntity<Void> addOrUpdateTag(final Long tagTypeId,
            final Long tagId,
            final String name,
            HttpServletRequest request) {
        SecurityChecker.allow(Privileges.ADMIN);

        final Tag tag;

        if (tagId == null) {
            tag = new Tag();
            final TagType tagType = tagTypeService.getById(tagTypeId);
            if (tagType == null) {
                throw new NotFoundException("tag-type.message");
            }
            tag.setType(tagType);
        } else {
            tag = tagService.getById(tagId);
            if (tag == null) {
                throw new NotFoundException("tag.message");
            }
        }

        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("cannot.be.empty.message");
        }
        if (!NameChecker.checkName(name)) {
            throw new BadRequestException("invalid.name.message");
        }

        final Tag existing = tagService.getByName(tag.getType().getId(), name);
        if (existing != null && !existing.getId().equals(tagId)) {
            throw new ConflictException("duplicated.tag.message", name);
        }

        tag.setName(name);

        if (tagId == null) {
            tagService.save(tag);
            return created(request, tag.getId());
        } else {
            tagService.update(tag);
            return ResponseEntity.noContent().build();
        }
    }

    private ResponseEntity<Void> created(HttpServletRequest request, Long id) {
        try {
            return ResponseEntity.created(new URI(request.getRequestURL() + "/" + id)).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
