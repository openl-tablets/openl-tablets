package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openl.rules.security.standalone.dao.TagDao;
import org.openl.rules.security.standalone.dao.TagTemplateDao;
import org.openl.rules.security.standalone.dao.TagTypeDao;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagTemplate;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class TagTemplateService {
    private static final Logger LOG = LoggerFactory.getLogger(TagTemplateService.class);
    private static final Pattern TAG_PATTERN = Pattern.compile("%([^%]+)%");
    private static final Pattern NONE_WILDCARD_PATTERN = Pattern.compile("([^*?]+)");

    private TagTypeDao tagTypeDao;
    private TagDao tagDao;

    private TagTemplateDao tagTemplateDao;

    public void setTagTypeDao(TagTypeDao tagTypeDao) {
        this.tagTypeDao = tagTypeDao;
    }

    public void setTagDao(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    public void setTagTemplateDao(TagTemplateDao tagTemplateDao) {
        this.tagTemplateDao = tagTemplateDao;
    }

    public List<String> getTemplates() {
        return tagTemplateDao.getAll().stream().map(TagTemplate::getTemplate).collect(Collectors.toList());
    }

    @Transactional
    public void save(List<String> templates) {
        tagTemplateDao.deleteAll();

        for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
            final String templateString = templates.get(i);
            if (StringUtils.isBlank(templateString)) {
                continue;
            }
            final TagTemplate template = new TagTemplate();
            template.setTemplate(templateString);
            template.setPriority(i + 1);
            tagTemplateDao.save(template);
        }
    }

    public List<Tag> getTags(String name) {
        final List<TagTemplate> templates = tagTemplateDao.getAll();
        for (TagTemplate template : templates) {
            List<TagType> tagTypes = new ArrayList<>();
            String regexPattern = buildPatternAndFindTagTypes(template.getTemplate(), tagTypes);

            if (tagTypes.isEmpty()) {
                LOG.warn("Template '{}' doesn't contain tag types.", template);
                continue;
            }

            final Matcher matcher = Pattern.compile(regexPattern).matcher(name);
            if (matcher.matches()) {
                List<Tag> tags = new ArrayList<>();
                for (int i = 0; i < tagTypes.size(); i++) {
                    TagType tagType = tagTypes.get(i);
                    final String tagValue = matcher.group(i + 1);
                    Tag tag = tagDao.getByTagTypeAndName(tagType.getName(), tagValue);
                    if (tag == null) {
                        tag = new Tag();
                        tag.setType(tagType);
                        tag.setName(tagValue);
                    }
                    tags.add(tag);
                }

                tags.sort(Comparator.comparing((Tag t) -> t.getType().getName()).thenComparing(Tag::getName));

                return tags;
            }
        }

        return Collections.emptyList();
    }

    public String validate(String template) {
        boolean hasTagTypes = false;
        final Matcher matcher = TAG_PATTERN.matcher(template);
        while (matcher.find()) {
            hasTagTypes = true;

            final String tagTypeName = matcher.group(1);
            if (tagTypeDao.getByName(tagTypeName) == null) {
                return "Can't find tag type '" + tagTypeName + "'.";
            }
        }
        if (!hasTagTypes) {
            return "Template '" + template + "' doesn't contain tag types.";
        }

        return null;
    }

    private String buildPatternAndFindTagTypes(String tagTemplate, List<TagType> tagTypes) {
        final Matcher matcher = TAG_PATTERN.matcher(tagTemplate);
        int pos = 0;
        StringBuilder regexPattern = new StringBuilder();
        while (matcher.find()) {
            // Part before tag type
            final int start = matcher.start();
            if (pos < start) {
                String nonTagRegex = tagTemplate.substring(pos, start);
                nonTagRegex = escapeNonTagPart(nonTagRegex);
                regexPattern.append(nonTagRegex);
            }

            // Actual tag type.
            final String tagTypeName = matcher.group(1);
            final TagType tagType = tagTypeDao.getByName(tagTypeName);
            if (tagType != null) {
                tagTypes.add(tagType);
                regexPattern.append("(.+)"); // Capturing group order will be same as in groupTypes.
            } else {
                LOG.warn("Can't find tag type '{}'. Skip it.", tagTypeName);
            }

            pos = matcher.end();
        }
        regexPattern.append(escapeNonTagPart(tagTemplate.substring(pos)));
        return regexPattern.toString();
    }

    private String escapeNonTagPart(String nonTagRegex) {
        // Escape all characters except * and ?
        nonTagRegex = NONE_WILDCARD_PATTERN.matcher(nonTagRegex).replaceAll( "\\\\Q$1\\\\E");

        // Replace File wildcard * with regex .*
        nonTagRegex = nonTagRegex.replace("*", ".*");
        return nonTagRegex;
    }
}
