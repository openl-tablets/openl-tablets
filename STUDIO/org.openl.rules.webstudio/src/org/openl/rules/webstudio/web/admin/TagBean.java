package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class TagBean {
    private final TagService tagService;
    private Long id;
    private String tagType;
    private String name;

    public TagBean(TagService tagService) {
        this.tagService = tagService;
    }

    public List<Tag> getTags() {
        return tagService.getAll();
    }

    public void setInitialTag(Long id) {
        this.id = id;
        if (id != null) {
            final Tag tag = tagService.getById(id);
            if (tag != null) {
                this.tagType = tag.getTagType();
                this.name = tag.getName();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void nameValidator(FacesContext context, UIComponent component, Object value) {
        String name = (String) value;
        WebStudioUtils.validate(StringUtils.isNotBlank(name), "Can not be empty");
        // TODO: Unique name check
    }

    public void save() {
        WebStudioUtils.validate(StringUtils.isNotBlank(tagType), "You must select tag type");

        Tag tag;
        if (id == null) {
            tag = new Tag();
        } else {
            tag = tagService.getById(id);
        }
        tag.setTagType(tagType);
        tag.setName(name);
        if (id == null) {
            tagService.save(tag);
        } else {
            tagService.update(tag);
        }
    }

    public void delete(Long id) {
        tagService.delete(id);
    }
}
