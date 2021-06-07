package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.rules.webstudio.service.TagService;
import org.openl.rules.webstudio.service.TagTemplateService;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class ProjectsWithoutTagsBean {
    private final OpenLProjectService projectService;
    private final TagTemplateService tagTemplateService;
    private final DesignTimeRepository designTimeRepository;
    private final TagService tagService;

    private List<ProjectTags> projectsWithoutTags = Collections.emptyList();

    public ProjectsWithoutTagsBean(OpenLProjectService projectService,
            TagTemplateService tagTemplateService,
            DesignTimeRepository designTimeRepository,
            TagService tagService) {
        this.projectService = projectService;
        this.tagTemplateService = tagTemplateService;
        this.designTimeRepository = designTimeRepository;
        this.tagService = tagService;
    }

    public void init() {
        projectsWithoutTags = new ArrayList<>();

        final ArrayList<AProject> projects = new ArrayList<>(designTimeRepository.getProjects());
        projects.sort(
            Comparator.comparing((AProject o) -> o.getRepository().getId()).thenComparing(AProjectFolder::getRealPath));

        for (AProject project : projects) {
            final String repoId = project.getRepository().getId();
            final String repoName = project.getRepository().getName();
            final String realPath = project.getRealPath();
            final OpenLProject existing = projectService.getProject(repoId, realPath);

            final String projectName = project.getBusinessName();
            final List<Tag> tags = tagTemplateService.getTags(projectName);

            if (tags.isEmpty()) {
                // Can't automatically fill tags.
                continue;
            }

            final List<TagType> allTypes = tags.stream().map(Tag::getType).collect(Collectors.toList());

            if (existing == null || existing.getTags().isEmpty() || allTypes.stream()
                .anyMatch(type -> existing.getTags().stream().noneMatch(tag -> tag.getType().equals(type)))) {

                OpenLProject openlProject = new OpenLProject();
                openlProject.setRepositoryId(repoId);
                openlProject.setProjectPath(realPath);
                openlProject.setTags(tags);

                ProjectTags projectTags = new ProjectTags(openlProject, existing, projectName, repoName);

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

    public void applyTags() {
        int applied = 0;
        for (ProjectTags openLProject : projectsWithoutTags) {
            if (openLProject.fillTags) {
                final OpenLProject project = openLProject.getProject();
                createExtensibleIfAbsent(project.getTags());
                if (project.getTags().isEmpty()) {
                    continue;
                }

                OpenLProject existing = projectService.getProject(project.getRepositoryId(), project.getProjectPath());
                if (existing == null) {
                    projectService.save(project);
                } else {
                    existing.setTags(project.getTags());
                    projectService.update(existing);
                }

                applied++;
            }
        }

        WebStudioUtils.addInfoMessage("Tags were added to " + applied + " projects.");
    }

    private void createExtensibleIfAbsent(List<Tag> tags) {
        for (Iterator<Tag> iterator = tags.iterator(); iterator.hasNext();) {
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
        private final OpenLProject project;
        private final OpenLProject existing;
        private final String projectName;
        private final String repoName;
        private boolean fillTags;

        ProjectTags(OpenLProject project, OpenLProject existing, String projectName, String repoName) {
            this.project = project;
            this.existing = existing;
            this.projectName = projectName;
            this.repoName = repoName;
            fillTags = true;
        }

        private OpenLProject getProject() {
            return project;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getProjectPath() {
            return project.getProjectPath();
        }

        public String getRepoName() {
            return repoName;
        }

        public boolean isFillTags() {
            return fillTags;
        }

        public void setFillTags(boolean fillTags) {
            this.fillTags = fillTags;
        }

        public List<TagDTO> getTags() {
            List<TagDTO> dtoList = new ArrayList<>();
            final List<Tag> tags = project.getTags();
            for (Tag tag : tags) {
                Tag existingTag = null;
                if (existing != null) {
                    existingTag = existing.getTags()
                        .stream()
                        .filter(e -> e.getType().equals(tag.getType()))
                        .findAny()
                        .orElse(null);
                }
                final TagDTO dto = new TagDTO(tag, existingTag);
                dtoList.add(dto);
            }
            return dtoList;
        }
    }

    public static class TagDTO {
        private final Tag tag;
        private final Tag existing;

        TagDTO(Tag tag, Tag existing) {
            this.tag = tag;
            this.existing = existing;
        }

        public String getName() {
            return tag.getName();
        }

        public String getExistingName() {
            return existing.getName();
        }

        public String getType() {
            return tag.getType().getName();
        }

        public boolean isAssigned() {
            return existing != null;
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
            return existing != null && !existing.getName().equalsIgnoreCase(tag.getName()) && !isCannotCreate();
        }
    }
}
