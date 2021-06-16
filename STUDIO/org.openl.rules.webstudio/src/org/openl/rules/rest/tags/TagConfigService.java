package org.openl.rules.rest.tags;

import java.net.URI;
import java.net.URISyntaxException;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openl.rules.rest.SecurityChecker;
import org.openl.rules.rest.exception.BadRequestException;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.util.NameChecker;
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
    public Response deleteTagType(@PathParam("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        if (tagTypeService.delete(id)) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/types/{tagTypeId}/tags/{id}")
    public Response deleteTag(@PathParam("tagTypeId") final Long tagTypeId, @PathParam("id") final Long id) {
        SecurityChecker.allow(Privileges.ADMIN);
        final Tag tag = tagService.getById(id);
        if (tag == null || !Objects.equals(tag.getType().getId(), tagTypeId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (tagService.delete(id)) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/types")
    public Response addTagType(TagTypeDTO typeDTO, @Context UriInfo uriInfo) {
        return addOrUpdateTagType(null, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible(), uriInfo);
    }

    @PUT
    @Path("/types/{id}")
    public Response updateTagType(@PathParam("id") final Long id, TagTypeDTO typeDTO) {
        return addOrUpdateTagType(id, typeDTO.getName(), typeDTO.isNullable(), typeDTO.isExtensible(), null);
    }

    private Response addOrUpdateTagType(final Long id,
            final String name,
            final Boolean nullable,
            final Boolean extensible,
            final UriInfo uriInfo) {
        SecurityChecker.allow(Privileges.ADMIN);
        final TagType tagType;

        if (id == null) {
            tagType = new TagType();
        } else {
            tagType = tagTypeService.getById(id);
            if (tagType == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
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
            return created(uriInfo, tagType.getId());
        } else {
            tagTypeService.update(tagType);
            return Response.noContent().build();
        }
    }

    @POST
    @Path("/types/{tagTypeId}/tags")
    public Response addTag(@PathParam("tagTypeId") final Long tagTypeId, final String name, @Context UriInfo uriInfo) {
        return addOrUpdateTag(tagTypeId, null, name, uriInfo);
    }

    @PUT
    @Path("/types/{tagTypeId}/tags/{tagId}")
    public Response updateTag(@PathParam("tagTypeId") final Long tagTypeId,
            @PathParam("tagId") final Long tagId,
            final String name) {
        return addOrUpdateTag(tagTypeId, tagId, name, null);
    }

    private Response addOrUpdateTag(final Long tagTypeId, final Long tagId, final String name, UriInfo uriInfo) {
        SecurityChecker.allow(Privileges.ADMIN);

        final Tag tag;

        if (tagId == null) {
            tag = new Tag();
            final TagType tagType = tagTypeService.getById(tagTypeId);
            if (tagType == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            tag.setType(tagType);
        } else {
            tag = tagService.getById(tagId);
            if (tag == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
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
            return created(uriInfo, tag.getId());
        } else {
            tagService.update(tag);
            return Response.noContent().build();
        }
    }

    private Response created(UriInfo uriInfo, Long id) {
        try {
            return Response.created(new URI(uriInfo.getPath(false) + "/" + id)).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
