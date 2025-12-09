package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.RepositorySelectNodeStateHolder;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTemplateService;
import org.openl.studio.tags.service.TagTypeService;
import org.openl.util.StringUtils;

@Service
@SessionScope
public class ProjectTagsBean {
    private static final long NONE_ID = -1L;
    private static final String NONE_NAME = "[None]";
    private final TagTypeService tagTypeService;
    private final TagService tagService;
    private final RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;
    private final TagTemplateService tagTemplateService;

    private List<Tag> tags;
    private List<TagInfo> notApplicableTagTypes;
    private List<TagInfo> notApplicableTagValues;

    private String projectName;
    private boolean tagsArePreconfigured;
    private boolean initFromOpenedProject;

    public ProjectTagsBean(TagTypeService tagTypeService,
                           TagService tagService,
                           RepositorySelectNodeStateHolder repositorySelectNodeStateHolder,
                           TagTemplateService tagTemplateService) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
        this.tagTemplateService = tagTemplateService;
    }

    public void init() {
        if (initFromOpenedProject) {
            initFromOpenedProject();
        }
        initFromTemplate();
        fillAbsentTags();
        tags.sort(Comparator.comparing((Tag tag) -> tag.getType().getName()).thenComparing(Tag::getName));
    }

    private void initFromTemplate() {
        List<Tag> newTags = new ArrayList<>();

        if (StringUtils.isNotBlank(projectName)) {
            newTags.addAll(tagTemplateService.getTags(projectName));
            newTags.removeIf(tag -> tag.getId() == null && !tag.getType().isExtensible());
        }

        if (tagsArePreconfigured || initFromOpenedProject) {
            Set<String> existingTags = tags.stream().map(Tag::getType).map(TagType::getName).collect(Collectors.toSet());
            newTags.removeIf(tag -> existingTags.contains(tag.getType().getName()));
            newTags.addAll(tags);
        }
        tags = newTags;
    }
    
    private void initFromOpenedProject() {
        TreeNode selectedNode = this.repositorySelectNodeStateHolder.getSelectedNode();
        TreeProject selectedProject = selectedNode instanceof TreeProject ? (TreeProject) selectedNode : null;

        if (selectedProject != null) {
            RulesProject project = (RulesProject) selectedProject.getData();
            Map<String, String> projectTags = project.getLocalTags();

            tags = new ArrayList<>();
            notApplicableTagTypes = new ArrayList<>();
            notApplicableTagValues = new ArrayList<>();
            for (Map.Entry<String, String> tagFromOpenedProject: projectTags.entrySet()) {
                Tag tag = tagService.getByTypeNameAndName(tagFromOpenedProject.getKey(), tagFromOpenedProject.getValue());
                if (tag != null) {
                    tags.add(tag);
                } else {
                    TagType tagType = tagTypeService.getByName(tagFromOpenedProject.getKey());
                    TagInfo tagInfo = new TagInfo(tagFromOpenedProject.getKey(), tagFromOpenedProject.getValue(), null, tagType);
                    if (tagType != null) {
                        notApplicableTagValues.add(tagInfo);
                    } else {
                        notApplicableTagTypes.add(tagInfo);
                    }
                }
            }
        }
    }

    private void fillAbsentTags() {
        final List<TagType> tagTypes = getTagTypes();
        tagTypes.forEach(type -> {
            final Long typeId = type.getId();
            if (tags.stream().noneMatch(tag -> tag.getType().getId().equals(typeId))) {
                final Tag t = new Tag();
                t.setId(NONE_ID);
                t.setName(NONE_NAME);
                t.setType(type);
                tags.add(t);
            }
        });
    }

    public List<TagType> getTagTypes() {
        return tagTypeService.getAllTagTypes();
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<Tag> getTagValues(String tagType) {
        return tagService.getByTagType(tagType);
    }
    
    public List<TagInfo> getNotApplicableTagTypes() {
        return notApplicableTagTypes;
    }

    public List<TagInfo> getNotApplicableTagValues() {
        return notApplicableTagValues;
    }

    public void validateCreate() {
        // Validate
        tags.forEach(tag -> {
            final String tagName = tag.getName();
            WebStudioUtils.validate(StringUtils.isNotBlank(tagName), "Cannot be empty");

            final TagType type = tag.getType();

            if (tagName.equals(NONE_NAME)) {
                WebStudioUtils.validate(type.isNullable(), "Tag type '" + type.getName() + "' is mandatory.");
            } else {
                final Tag existed = tagService.getByName(type.getId(), tagName);
                if (existed == null) {
                    if (tag.getId() != null && tag.getId() != NONE_ID) {
                        // It was removed externally
                        tag.setId(null);
                    }
                    WebStudioUtils.validate(NameChecker.checkName(tagName),
                            "Tag cannot contain forbidden characters (" + NameChecker
                                    .FORBIDDEN_CHARS_STRING + "), start with space, end with space or dot.");
                    WebStudioUtils.validate(type.isExtensible(),
                            String.format("'%s' is not allowed value for tag type '%s'.", tagName, type.getName()));
                }
            }
        });
    }

    private void createExtensibleTags() {
        // Save extensible tags
        tags.stream().filter(tag -> tag.getType().isExtensible()).forEach(tag -> {
            final Tag existed = tagService.getByName(tag.getType().getId(), tag.getName());
            if (existed == null) {
                // Ignore id because we can enter our new value in editable combobox.
                Tag newTag = new Tag();
                newTag.setType(tag.getType());
                newTag.setName(tag.getName());
                tagService.save(newTag);
            }
        });
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setTagsArePreconfigured(boolean tagsArePreconfigured) {
        this.tagsArePreconfigured = tagsArePreconfigured;
    }

    public void setInitFromOpenedProject(boolean initFromOpenedProject) {
        this.initFromOpenedProject = initFromOpenedProject;
    }

    public Map<String, String> saveTagsTypesAndGetTags() {
        if (tags == null) {
            return Collections.emptyMap();
        }
        tags.removeIf(tag -> tag.getName().equals(NONE_NAME));
        createExtensibleTags();
        return tags.stream().collect(Collectors.toMap(tag -> tag.getType().getName(), Tag::getName));
    }
    
    public void clearTags() {
        this.tags = Collections.emptyList();
        this.notApplicableTagTypes = Collections.emptyList();
        this.notApplicableTagValues = Collections.emptyList();
    }

    public void initTagsFromPreexisting(Map<String, String> tagsMap) {
        this.tags = new ArrayList<>();
        this.notApplicableTagTypes = new ArrayList<>();
        this.notApplicableTagValues = new ArrayList<>();
        for(Map.Entry<String, String> entry: tagsMap.entrySet()) {
            Tag tag = tagService.getByTypeNameAndName(entry.getKey(), entry.getValue());
            if (tag == null) {
                TagType tagType = tagTypeService.getByName(entry.getKey());
                if (tagType != null && tagType.isExtensible()) {
                    tag = new Tag();
                    tag.setType(tagType);
                    tag.setName(entry.getValue());
                } else {
                    TagInfo tagInfo = new TagInfo(entry.getKey(), entry.getValue(), null, tagType);
                    if (tagType != null) {
                        notApplicableTagValues.add(tagInfo);
                    } else {
                        notApplicableTagTypes.add(tagInfo);
                    }
                }
            }
            
            if (tag != null) {
                tags.add(tag);
            } 
        }
    }
}
