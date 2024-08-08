package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTemplateService;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;

@Service
@SessionScope
public class ProjectsWithoutTagsBean {
    private final TagTemplateService tagTemplateService;
    private final TagService tagService;

    private List<ProjectTags> projectsWithoutTags = Collections.emptyList();

    public ProjectsWithoutTagsBean(TagTemplateService tagTemplateService,
                                   TagService tagService) {
        this.tagTemplateService = tagTemplateService;
        this.tagService = tagService;
    }

    public void init() {
        projectsWithoutTags = new ArrayList<>();

        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();
        UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();

        List<RulesProject> projects = userWorkspace.getProjects().stream()
                .sorted(Comparator.comparing((AProject o) -> o.getRepository().getId()).thenComparing(AProjectFolder::getRealPath))
                .collect(Collectors.toList());

        for (RulesProject project : projects) {
            final List<Tag> tags = tagTemplateService.getTags(project.getBusinessName());

            if (tags.isEmpty()) {
                // Cannot automatically fill tags.
                continue;
            }

            final List<TagType> allTypes = tags.stream().map(Tag::getType).collect(Collectors.toList());

            Map<String, String> currentTags = project.getTags();
            if (allTypes.stream().anyMatch(type -> ! currentTags.containsKey(type.getName()))) {

                ProjectTags projectTags = new ProjectTags(project, tags);

                projectsWithoutTags.add(projectTags);
            }
        }
    }

    public List<ProjectTags> getProjectsWithoutTags() {
        return projectsWithoutTags;
    }

    public void setProjectsWithoutTags(List<ProjectTags> projectsWithoutTags) {
        this.projectsWithoutTags = projectsWithoutTags;
    }

    public void applyTags() throws ProjectException {
        int applied = 0;
        for (ProjectTags projectTags : projectsWithoutTags) {
            if (projectTags.fillTags) {
                List<Tag> tagsToBeAdded = projectTags.getRawTags();
                createExtensibleIfAbsent(tagsToBeAdded);
                if (tagsToBeAdded.isEmpty()) {
                    continue;
                }
                
                var newTagsMap = tagsToBeAdded.stream().collect(Collectors.toMap(tag -> tag.getType().getName(), Tag::getName));
                RulesProject rulesProject = projectTags.getRulesProject();
                
                var tagsToSave = new HashMap<>(rulesProject.getTags());
                tagsToSave.putAll(newTagsMap);
                rulesProject.saveTags(tagsToSave);

                applied++;
            }
        }

        WebStudioUtils.addInfoMessage("Tags were added to " + applied + " projects.");
    }

    private void createExtensibleIfAbsent(List<Tag> tags) {
        for (Iterator<Tag> iterator = tags.iterator(); iterator.hasNext(); ) {
            Tag tag = iterator.next();
            if (tag.getId() == null) {
                final Tag existed = tagService.getByName(tag.getType().getId(), tag.getName());
                if (existed == null) {
                    if (tag.getType().isExtensible()) {
                        tagService.save(tag);
                    } else {
                        iterator.remove();
                    }
                } else {
                    // Was created before.
                    tag.setId(existed.getId());
                }
            }
        }
    }

    public static class ProjectTags {
        private final RulesProject rulesProject;
        private final List<Tag> tags;
        private boolean fillTags;

        ProjectTags(RulesProject rulesProject, List<Tag> tags) {
            this.rulesProject = rulesProject;
            this.tags = tags;
            fillTags = rulesProject.isOpenedForEditing();
        }

        public String getProjectName() {
            return rulesProject.getBusinessName();
        }

        public String getProjectPath() {
            return rulesProject.getRealPath();
        }

        public String getRepoName() {
            return rulesProject.getRepository().getName();
        }
        
        public boolean isOpenedForEditing() {
            return rulesProject.isOpenedForEditing();
        }

        public RulesProject getRulesProject() {
            return rulesProject;
        }
        
        public boolean isFillTags() {
            return fillTags;
        }

        public void setFillTags(boolean fillTags) {
            this.fillTags = fillTags;
        }
        
        protected List<Tag> getRawTags() {
            return tags;
        }

        public List<TagDTO> getTags() {
            List<TagDTO> dtoList = new ArrayList<>();
            for (Tag tag : tags) {
                String existingTagName = rulesProject.getTags().get(tag.getName());
                final TagDTO dto = new TagDTO(tag, existingTagName);
                dtoList.add(dto);
            }
            return dtoList;
        }
    }

    public static class TagDTO {
        private final Tag tag;
        private final String existingTagName;

        TagDTO(Tag tag, String existingTagName) {
            this.tag = tag;
            this.existingTagName = existingTagName;
        }

        public String getName() {
            return tag.getName();
        }

        public String getExistingName() {
            return existingTagName;
        }

        public String getType() {
            return tag.getType().getName();
        }

        public boolean isAssigned() {
            return existingTagName != null;
        }

        public boolean isAddExistingTag() {
            return tag.getId() != null;
        }

        public boolean isWillCreate() {
            return tag.getId() == null && tag.getType().isExtensible();
        }

        public boolean isCannotCreate() {
            return tag.getId() == null && !tag.getType().isExtensible();
        }
        
        public boolean isWillReplace() {
            return existingTagName != null && !existingTagName.equalsIgnoreCase(tag.getName()) && !isCannotCreate();
        }
    }
}
