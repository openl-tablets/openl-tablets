package org.openl.rules.rest.tags;

import java.util.List;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.openl.rules.rest.SecurityChecker;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Path("/admin/tag-config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TagConfigService {
    private final TagTypeService tagTypeService;
    private final TagService tagService;

    public TagConfigService(TagTypeService tagTypeService, TagService tagService) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
    }

    @GET
    @Path("/types")
    public List<TagTypeDTO> getTypes() {
        SecurityChecker.allow(Privileges.ADMIN);
        return tagTypeService.getAll();
    }

    @DELETE
    @Path("/types/{id}")
    public void deleteTagType(@PathParam("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        tagTypeService.delete(id);
    }

    @DELETE
    @Path("/types/{tagTypeId}/tags/{id}")
    public void deleteTag(@PathParam("tagTypeId") final Long tagTypeId, @PathParam("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        final Tag tag = tagService.getById(id);
        WebStudioUtils.validate(Objects.equals(tag.getType().getId(), tagTypeId), "Tag type doesn't contain tag with id " + id);
        tagService.delete(id);
    }

    @POST
    @Path("/types")
    public void addTagType(TagTypeDTO typeDTO) {
        addOrUpdateTagType(null, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible());
    }

    @PUT
    @Path("/types/{id}")
    public void updateTagType(@PathParam("id") final Long id, TagTypeDTO typeDTO) {
        addOrUpdateTagType(id, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible());
    }

    private void addOrUpdateTagType(final Long id,
            final String name,
            final Boolean nullable,
            final Boolean extensible) {
        SecurityChecker.allow(Privileges.ADMIN);
        final TagType tagType;

        if (id == null) {
            tagType = new TagType();
        } else {
            tagType = tagTypeService.getById(id);
        }

        if (StringUtils.isNotBlank(name)) {
            tagType.setName(name);

            WebStudioUtils.validate(NameChecker.checkName(name), NameChecker.BAD_NAME_MSG);
            final TagType existing = tagTypeService.getByName(name);
            WebStudioUtils.validate(existing == null || existing.getId().equals(id),
                "Tag type with name \"" + name + "\" already exists.");
        }

        if (nullable != null) {
            tagType.setNullable(nullable);
        }
        if (extensible != null) {
            tagType.setExtensible(extensible);
        }

        if (id == null) {
            tagTypeService.save(tagType);
        } else {
            tagTypeService.update(tagType);
        }
    }

    @POST
    @Path("/types/{tagTypeId}/tags")
    public void addTag(@PathParam("tagTypeId") final Long tagTypeId, final String name) {
        addOrUpdateTag(tagTypeId, null, name);
    }

    @PUT
    @Path("/types/{tagTypeId}/tags/{tagId}")
    public void updateTag(@PathParam("tagTypeId") final Long tagTypeId,
            @PathParam("tagId") final Long tagId,
            final String name) {
        addOrUpdateTag(tagTypeId, tagId, name);
    }

    private void addOrUpdateTag(final Long tagTypeId, final Long tagId, final String name) {
        SecurityChecker.allow(Privileges.ADMIN);

        final Tag tag;

        if (tagId == null) {
            tag = new Tag();
            tag.setType(tagTypeService.getById(tagTypeId));
        } else {
            tag = tagService.getById(tagId);
        }

        WebStudioUtils.validate(StringUtils.isNotBlank(name), "Can not be empty");
        WebStudioUtils.validate(NameChecker.checkName(name), NameChecker.BAD_NAME_MSG);

        final Tag existing = tagService.getByName(tag.getType().getId(), name);
        WebStudioUtils.validate(existing == null || existing.getId().equals(tagId),
            "Tag with name \"" + name + "\" already exists.");

        tag.setName(name);

        if (tagId == null) {
            tagService.save(tag);
        } else {
            tagService.update(tag);
        }
    }
}
