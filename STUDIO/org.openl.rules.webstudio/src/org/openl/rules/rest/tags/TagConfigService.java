package org.openl.rules.rest.tags;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
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
    @Path("/type")
    @Consumes(MediaType.TEXT_PLAIN)
    public void deleteTagType(final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        tagTypeService.delete(id);
    }

    @DELETE
    @Path("/tag")
    @Consumes(MediaType.TEXT_PLAIN)
    public void deleteTag(final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        tagService.delete(id);
    }

    @PATCH
    @Path("/type")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void updateTagType(@FormParam("id") final Long id,
            @FormParam("name") final String name,
            @FormParam("nullable") final Boolean nullable,
            @FormParam("extensible") final Boolean extensible) {
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

    @PATCH
    @Path("/tag")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void updateTag(@FormParam("tagTypeId") final Long tagTypeId,
            @FormParam("tagId") final Long tagId,
            @FormParam("name") final String name) {
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
