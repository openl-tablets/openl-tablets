package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTypeService;
import org.openl.rules.webstudio.web.repository.RepositorySelectNodeStateHolder;
import org.openl.rules.webstudio.web.repository.tree.TreeNode;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class ProjectTagsBean {
    public static final long NONE_ID = -1L;
    private final TagTypeService tagTypeService;
    private final TagService tagService;
    private final OpenLProjectService projectService;
    private final RepositorySelectNodeStateHolder repositorySelectNodeStateHolder;

    private OpenLProject openlProject;
    private List<Tag> tags;
    private String tagTypeForNewTag;
    private String newTagValue;

    public ProjectTagsBean(TagTypeService tagTypeService,
            TagService tagService,
            OpenLProjectService projectService,
            RepositorySelectNodeStateHolder repositorySelectNodeStateHolder) {
        this.tagTypeService = tagTypeService;
        this.tagService = tagService;
        this.projectService = projectService;
        this.repositorySelectNodeStateHolder = repositorySelectNodeStateHolder;
    }

    public void init() {
        TreeNode selectedNode = this.repositorySelectNodeStateHolder.getSelectedNode();
        TreeProject selectedProject = selectedNode instanceof TreeProject ? (TreeProject) selectedNode : null;

        if (selectedProject == null) {
            openlProject = null;
            tags = Collections.emptyList();
        } else {
            RulesProject project = (RulesProject) selectedProject.getData();
            final String repoId = project.getRepository().getId();
            final String realPath = project.getRealPath();

            this.openlProject = projectService.getProject(repoId, realPath);
        }

        if (openlProject != null) {
            tags = new ArrayList<>(openlProject.getTags());
        } else {
            tags = new ArrayList<>();
        }
        final List<TagType> tagTypes = getTagTypes();
        tagTypes.forEach(type -> {
            final Long typeId = type.getId();
            if (tags.stream().noneMatch(tag -> tag.getType().getId().equals(typeId))) {
                final Tag t = new Tag();
                t.setId(NONE_ID);
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
        TreeNode selectedNode = this.repositorySelectNodeStateHolder.getSelectedNode();
        TreeProject selectedProject = selectedNode instanceof TreeProject ? (TreeProject) selectedNode : null;

        if (selectedProject != null) {
            boolean create = false;
            if (openlProject == null) {
                create = true;
                RulesProject project = (RulesProject) selectedProject.getData();
                final String repoId = project.getRepository().getId();
                final String realPath = project.getRealPath();

                openlProject = new OpenLProject();
                openlProject.setRepositoryId(repoId);
                openlProject.setProjectPath(realPath);
                openlProject.setTags(new ArrayList<>());
            }
            tags.removeIf(tag -> tag.getId() == null || tag.getId() == -1L);
            final List<Tag> currentTags = openlProject.getTags();
            currentTags.clear();
            tags.forEach(tag -> currentTags.add(tagService.getById(tag.getId())));
            if (create) {
                projectService.save(openlProject);
            } else {
                projectService.update(openlProject);
            }
        }
    }

    public String getTagTypeForNewTag() {
        return tagTypeForNewTag;
    }

    public void setTagTypeForNewTag(String tagTypeForNewTag) {
        this.tagTypeForNewTag = tagTypeForNewTag;
        this.newTagValue = null;
    }

    public void setNewTagValue(String newTagValue) {
        this.newTagValue = newTagValue;
    }

    public String getNewTagValue() {
        return newTagValue;
    }

    public void saveTag() {
        final TagType tagType = tagTypeService.getByName(tagTypeForNewTag);
        if (!tagType.isExtensible()) {
            throw new IllegalArgumentException("Tag type isn't extensible");
        }

        Tag tag = new Tag();
        tag.setType(tagType);
        tag.setName(newTagValue);
        tagService.save(tag);

        // TODO: unique tag check
    }
}
