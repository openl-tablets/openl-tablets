package org.openl.studio.tags.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.util.StringUtils;

/**
 * Validates the tag assignments requested for a project against the configured tag types.
 *
 * <p>A blank value means the tag is not set. A mandatory (non-nullable) tag type must have a value. A
 * value of a non-extensible tag type must be one of its predefined values. A value must be a valid name.
 * A value of an extensible tag type that is not yet known is registered so it becomes selectable later.
 */
@Service
public class TagAssignmentValidator {

    private final TagTypeService tagTypeService;
    private final TagService tagService;

    public TagAssignmentValidator(TagTypeService tagTypeService, TagService tagService) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
    }

    /**
     * Validates the requested tag assignments and registers new values of extensible tag types.
     *
     * @param requested tag type name to value assignments
     * @return the sanitized assignments to persist (blank values dropped)
     * @throws BadRequestException if a mandatory tag is missing, a value is not allowed, or a name is invalid
     */
    public Map<String, String> sanitize(Map<String, String> requested) {
        var types = tagTypeService.getAllTagTypes();
        var byName = types.stream().collect(Collectors.toMap(TagType::getName, Function.identity()));
        var sanitized = new LinkedHashMap<String, String>();
        requested.forEach((typeName, rawValue) -> {
            var value = validateValue(byName.get(typeName), typeName, StringUtils.trimToNull(rawValue));
            if (value != null) {
                sanitized.put(typeName, value);
            }
        });
        types.forEach(type -> {
            if (!type.isNullable() && !sanitized.containsKey(type.getName())) {
                throw new BadRequestException("project.tags.type.mandatory.message", new Object[]{type.getName()});
            }
        });
        sanitized.forEach((typeName, value) -> registerExtensible(byName.get(typeName), value));
        return sanitized;
    }

    private String validateValue(TagType type, String typeName, String value) {
        if (value == null) {
            return null;
        }
        if (type == null) {
            throw new BadRequestException("project.tags.type.unknown.message", new Object[]{typeName});
        }
        if (!NameChecker.checkName(value)) {
            throw new BadRequestException("invalid.name.message");
        }
        if (!type.isExtensible() && tagService.getByTypeNameAndName(typeName, value) == null) {
            throw new BadRequestException("project.tags.value.not-allowed.message", new Object[]{value, typeName});
        }
        return value;
    }

    private void registerExtensible(TagType type, String value) {
        if (type != null && type.isExtensible() && tagService.getByName(type.getId(), value) == null) {
            var tag = new Tag();
            tag.setType(type);
            tag.setName(value);
            tagService.save(tag);
        }
    }
}
