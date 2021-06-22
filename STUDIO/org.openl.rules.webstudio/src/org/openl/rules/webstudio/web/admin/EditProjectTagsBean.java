package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class EditProjectTagsBean {
    private static final long NONE_ID = -1L;
    private static final String NONE_NAME = "[None]";
    private final TagTypeService tagTypeService;
    private final TagService tagService;
    private final OpenLProjectService projectService;

    private OpenLProject openlProject;
    private List<Tag> tags;

    private String repoId;
    private String realPath;

    private Long typeId;
    private String tagName;
    private String errorMessage;

    public EditProjectTagsBean(TagTypeService tagTypeService,
            TagService tagService,
            OpenLProjectService projectService) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
        this.projectService = projectService;
    }

    public void init(TreeNode selectedNode) {
        TreeProject selectedProject = selectedNode instanceof TreeProject ? (TreeProject) selectedNode : null;

        if (selectedProject == null) {
            openlProject = null;
        } else {
            RulesProject project = (RulesProject) selectedProject.getData();
            final String repoId = project.getRepository().getId();
            final String realPath = project.getRealPath();

            this.repoId = repoId;
            this.realPath = realPath;

            this.openlProject = projectService.getProject(repoId, realPath);
        }

        if (openlProject != null) {
            tags = new ArrayList<>(openlProject.getTags());
        } else {
            tags = new ArrayList<>();
        }
        fillAbsentTags();
        if (StringUtils.isNotEmpty(errorMessage)) {
            tags.stream()
                .filter(tag1 -> tag1.getType().getId().equals(typeId))
                .findFirst()
                .ifPresent(tag -> tag.setName(tagName));
        }
        tags.sort(Comparator.comparing((Tag tag) -> tag.getType().getName()).thenComparing(Tag::getName));
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

    public void save() {
        try {
            // Validate
            WebStudioUtils.validate(StringUtils.isNotBlank(tagName), "Can not be empty");

            final TagType type = tagTypeService.getById(typeId);

            final Tag existed;
            if (tagName.equals(NONE_NAME)) {
                WebStudioUtils.validate(type.isNullable(), "Tag type '" + type.getName() + "' is mandatory.");
                existed = null;
            } else {
                WebStudioUtils.validate(NameChecker.checkName(tagName), NameChecker.BAD_NAME_MSG);
                existed = tagService.getByName(typeId, tagName);
                if (existed == null) {
                    WebStudioUtils.validate(type != null, "Tag type with id '" + typeId + "' does not exist.");
                    WebStudioUtils.validate(Objects.requireNonNull(type).isExtensible(),
                        String.format("'%s' is not allowed value for tag type '%s'.", tagName, type.getName()));
                }
            }

            if (repoId != null && realPath != null) {
                boolean create = false;
                this.openlProject = projectService.getProject(repoId, realPath);

                if (openlProject == null) {
                    create = true;

                    openlProject = new OpenLProject();
                    openlProject.setRepositoryId(repoId);
                    openlProject.setProjectPath(realPath);
                    openlProject.setTags(new ArrayList<>());
                }

                final List<Tag> currentTags = openlProject.getTags();
                currentTags.removeIf(tag -> tag.getType().getId().equals(typeId));
                if (existed == null) {
                    // If none - remove
                    if (!tagName.equals(NONE_NAME)) {
                        // Ignore id because we can enter our new value in editable combobox.
                        Tag newTag = new Tag();
                        newTag.setType(type);
                        newTag.setName(tagName);
                        tagService.save(newTag);
                        currentTags.add(tagService.getByName(typeId, tagName));
                    }
                } else {
                    currentTags.add(existed);
                }

                if (create) {
                    projectService.save(openlProject);
                } else {
                    projectService.update(openlProject);
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

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
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
}
