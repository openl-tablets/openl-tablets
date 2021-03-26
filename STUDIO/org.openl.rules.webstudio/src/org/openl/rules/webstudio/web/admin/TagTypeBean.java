package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class TagTypeBean {
    private final TagTypeService tagTypeService;
    private Long id;
    private String name;
    private boolean extensible;
    private boolean nullable;

    public TagTypeBean(TagTypeService tagTypeService) {
        this.tagTypeService = tagTypeService;
    }

    public List<TagType> getTagTypes() {
        return tagTypeService.getAllTagTypes();
    }

    public void setInitialId(Long id) {
        this.id = id;
        if (id != null) {
            final TagType tagType = tagTypeService.getById(id);
            if (tagType != null) {
                this.name = tagType.getName();
                this.extensible = tagType.isExtensible();
                this.nullable = tagType.isNullable();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExtensible() {
        return extensible;
    }

    public void setExtensible(boolean extensible) {
        this.extensible = extensible;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public void nameValidator(FacesContext context, UIComponent component, Object value) {
        String name = (String) value;
        final Long id = (Long) ((UIInput) context.getViewRoot().findComponent("editTagTypeForm:idHidden")).getValue();

        WebStudioUtils.validate(StringUtils.isNotBlank(name), "Can not be empty");
        final TagType existing = tagTypeService.getByName(name);
        WebStudioUtils.validate(existing == null || existing.getId().equals(id),
            "Tag type with such name exists already");
    }

    public void save() {
        TagType tagType;
        if (id == null) {
            tagType = new TagType();
        } else {
            tagType = tagTypeService.getById(id);
        }

        tagType.setName(name);
        tagType.setExtensible(extensible);
        tagType.setNullable(nullable);

        if (id == null) {
            tagTypeService.save(tagType);
        } else {
            tagTypeService.update(tagType);
        }
    }

    public void delete(String name) {
        tagTypeService.delete(name);
    }
}
