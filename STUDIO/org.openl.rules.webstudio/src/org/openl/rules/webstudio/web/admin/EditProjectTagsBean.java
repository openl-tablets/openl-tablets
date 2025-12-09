package org.openl.rules.webstudio.web.admin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.tags.service.TagService;
import org.openl.studio.tags.service.TagTypeService;
import org.openl.util.StringUtils;

@Service
@RequestScope
public class EditProjectTagsBean {
    private static final String NONE_NAME = "[None]";
    private final TagTypeService tagTypeService;
    private final TagService tagService;

    private List<TagInfo> tags;

    private String repoId;
    private String realPath;

    private String typeName;
    private String tagName;
    private String errorMessage;
    
    private boolean shouldAskForConfirmation;

    public EditProjectTagsBean(TagTypeService tagTypeService,
                               TagService tagService) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
    }

    public void init(TreeNode selectedNode) {
        TreeProject selectedProject = selectedNode instanceof TreeProject ? (TreeProject) selectedNode : null;

        if (selectedProject != null) {
            RulesProject project = (RulesProject) selectedProject.getData();
            
            this.repoId = project.getRepository().getId();
            this.realPath = project.getRealPath();
            this.shouldAskForConfirmation = project.isOpenedOtherVersion() && !project.isModified();

            tags = project.getLocalTags().entrySet().stream()
                    .map(entry -> {
                        Tag tag = tagService.getByTypeNameAndName(entry.getKey(), entry.getValue());
                        TagType tagType = tagTypeService.getByName(entry.getKey());
                        return new TagInfo(entry.getKey(), entry.getValue(), tag, tagType);
                    }).collect(Collectors.toList());
        }

        fillAbsentTags();
        if (StringUtils.isNotEmpty(errorMessage)) {
            tags.stream()
                    .filter(tagInfo -> tagInfo.getTypeName().equals(typeName))
                    .findFirst()
                    .ifPresent(tag -> tag.setName(tagName));
        }
        tags.sort(Comparator.comparing(TagInfo::getTypeName).thenComparing(TagInfo::getName));
    }

    private void fillAbsentTags() {
        final List<TagType> tagTypes = getTagTypes();
        tagTypes.forEach(type -> {
            if (tags.stream().noneMatch(tag -> tag.getTypeName().equals(type.getName()))) {
                final TagInfo t = new TagInfo(type.getName(), NONE_NAME, null, type);
                tags.add(t);
            }
        });
    }

    public List<TagType> getTagTypes() {
        return tagTypeService.getAllTagTypes();
    }

    public List<TagInfo> getTags() {
        return tags;
    }

    public List<Tag> getTagValues(String tagType) {
        return tagService.getByTagType(tagType);
    }

    public void save() {
        try {
            // Validate
            WebStudioUtils.validate(StringUtils.isNotBlank(tagName), "Cannot be empty");

            final TagType type = tagTypeService.getByName(typeName);
            
            if (repoId != null && realPath != null) {
                RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();
                UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
                Optional<RulesProject> rulesProjectOptional = userWorkspace.getProjectByPath(repoId, realPath);
                
                if (rulesProjectOptional.isPresent()) {
                    Tag existed = null;
                    String newTagName = tagName.equals(NONE_NAME) ? null : tagName;
                    RulesProject rulesProject = rulesProjectOptional.get();
                    Map<String, String> localTags = rulesProject.getLocalTags();
                    var currentTags = new HashMap<>(localTags);
                    
                    if (newTagName == null) {
                        if (type != null) {
                            WebStudioUtils.validate(type.isNullable(), "Tag type '" + type.getName() + "' is mandatory.");
                        }
                    } else {
                        WebStudioUtils.validate(NameChecker.checkName(newTagName), NameChecker.BAD_NAME_MSG);
                        if (type != null) {
                            existed = tagService.getByTypeNameAndName(typeName, newTagName);
                            if (existed == null) {
                                WebStudioUtils.validate(Objects.requireNonNull(type).isExtensible(),
                                        String.format("'%s' is not allowed value for tag type '%s'.", newTagName, type.getName()));
                            }
                        } else {
                            WebStudioUtils.validate(newTagName.equals(currentTags.get(typeName)),
                                    String.format("Tag %s could be changed to %s only, since it is not configured", typeName, NONE_NAME));
                        }
                    }

                    var changed = ! Objects.equals(currentTags.get(typeName), newTagName);
                    if (changed) {
                        if (newTagName != null) {
                            if (existed == null && type != null) {
                                // Ignore id because we can enter our new value in editable combobox.
                                Tag newTag = new Tag();
                                newTag.setType(type);
                                newTag.setName(newTagName);
                                tagService.save(newTag);
                                currentTags.put(type.getName(), newTagName);
                            } else {
                                currentTags.put(typeName, newTagName);
                            }
                        } else {
                            currentTags.remove(typeName);
                        }
                        rulesProject.saveTags(currentTags);
                        userWorkspace.refresh();
                    }
                } else {
                    throw new IllegalStateException("Project is not found");
                }
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getRealPath() {
        return realPath;
    }

    public void setRealPath(String realPath) {
        this.realPath = realPath;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = StringUtils.trimToNull(tagName);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isShouldAskForConfirmation() {
        return shouldAskForConfirmation;
    }
}
