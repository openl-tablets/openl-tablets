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
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/admin/tag-config")
public class TagConfigController {

    private final TagTypeService tagTypeService;
    private final TagService tagService;

    @Autowired
    public TagConfigController(TagTypeService tagTypeService, TagService tagService) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
    }

    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagTypeDTO> getTypes() {
        SecurityChecker.allow(Privileges.ADMIN);
        return tagTypeService.getAll();
    }

    @DeleteMapping(value = "/types/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteTagType(@PathVariable("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        if (tagTypeService.delete(id)) {
            return ResponseEntity.noContent().build();
        } else {
            throw new NotFoundException("tag-type.message");
        }
    }

    @DeleteMapping("/types/{tagTypeId}/tags/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable("tagTypeId") final Long tagTypeId,
            @PathVariable("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        final Tag tag = tagService.getById(id);
        if (tag == null || !Objects.equals(tag.getType().getId(), tagTypeId)) {
            throw new NotFoundException("tag.message");
        }
        if (tagService.delete(id)) {
            return ResponseEntity.noContent().build();
        } else {
            throw new NotFoundException("tag.message");
        }
    }

    @PostMapping(value = "/types", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addTagType(@RequestBody TagTypeDTO typeDTO, HttpServletRequest request) {
        return addOrUpdateTagType(null, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible(), request);
    }

    @PutMapping(value = "/types/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateTagType(@PathVariable("id") final Long id, @RequestBody TagTypeDTO typeDTO) {
        return addOrUpdateTagType(id, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible(), null);
    }

    private ResponseEntity<?> addOrUpdateTagType(final Long id,
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

    @PostMapping(value = "/types/{tagTypeId}/tags", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> addTag(@PathVariable("tagTypeId") final Long tagTypeId,
            @RequestBody final String name,
            HttpServletRequest request) {
        return addOrUpdateTag(tagTypeId, null, name, request);
    }

    @PutMapping(value = "/types/{tagTypeId}/tags/{tagId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> updateTag(@PathVariable("tagTypeId") final Long tagTypeId,
            @PathVariable("tagId") final Long tagId,
            @RequestBody final String name) {
        return addOrUpdateTag(tagTypeId, tagId, name, null);
    }

    private ResponseEntity<?> addOrUpdateTag(final Long tagTypeId,
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

    private ResponseEntity<?> created(HttpServletRequest request, Long id) {
        try {
            return ResponseEntity.created(new URI(request.getRequestURL() + "/" + id)).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
