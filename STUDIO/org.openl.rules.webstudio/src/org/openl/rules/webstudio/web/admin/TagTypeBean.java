package org.openl.rules.webstudio.web.admin;

import java.util.List;

import javax.faces.component.UIComponent;
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
    private String name;
    private boolean extensible;
    private boolean nullable;
    private boolean create;

    public TagTypeBean(TagTypeService tagTypeService) {
        this.tagTypeService = tagTypeService;
    }

    public List<TagType> getTagTypes() {
        return tagTypeService.getAllTagTypes();
    }

    public void setInitialName(String name) {
        if (StringUtils.isNotBlank(name)) {
            create = false;
            this.name = name;
            final TagType tagType = tagTypeService.getByName(name);
            if (tagType != null) {
                this.extensible = tagType.isExtensible();
                this.nullable = tagType.isNullable();
            }
        } else {
            this.create = true;
        }
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
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
        WebStudioUtils.validate(StringUtils.isNotBlank(name), "Can not be empty");
        if (create) {
            WebStudioUtils.validate(tagTypeService.getByName(name) == null, "Tag type with such name exists already");
        }
    }

    public void save() {
        TagType tagType;
        if (create) {
            tagType = new TagType();
            tagType.setName(name);
        } else {
            tagType = tagTypeService.getByName(name);
        }

        tagType.setExtensible(extensible);
        tagType.setNullable(nullable);

        if (create) {
            tagTypeService.save(tagType);
        } else {
            tagTypeService.update(tagType);
        }
    }

    public void delete(String name) {
        tagTypeService.delete(name);
    }
}
