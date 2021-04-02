package org.openl.rules.webstudio.web.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.webstudio.service.OpenLProjectService;
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

    private List<ProjectTags> projectsWithoutTags = Collections.emptyList();

    public ProjectsWithoutTagsBean(OpenLProjectService projectService,
        TagTemplateService tagTemplateService,
        DesignTimeRepository designTimeRepository) {
        this.projectService = projectService;
        this.tagTemplateService = tagTemplateService;
        this.designTimeRepository = designTimeRepository;
    }

    public void init() {
        projectsWithoutTags = new ArrayList<>();

        final ArrayList<AProject> projects = new ArrayList<>(designTimeRepository.getProjects());
        projects.sort(Comparator.comparing((AProject o) -> o.getRepository().getId())
            .thenComparing(AProjectFolder::getRealPath));

        for (AProject project : projects) {
            final String repoId = project.getRepository().getId();
            final String repoName = project.getRepository().getName();
            final String realPath = project.getRealPath();
            final OpenLProject existing = projectService.getProject(repoId, realPath);

            if (existing == null || existing.getTags().isEmpty()) {
                final String projectName = project.getBusinessName();
                final List<Tag> tags = tagTemplateService.getTags(projectName);

                if (tags.isEmpty()) {
                    // Can't automatically fill tags.
                    continue;
                }

                OpenLProject openlProject = new OpenLProject();
                openlProject.setRepositoryId(repoId);
                openlProject.setProjectPath(realPath);
                openlProject.setTags(tags);

                ProjectTags projectTags = new ProjectTags(openlProject, projectName, repoName);

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
                projectService.save(openLProject.getProject());
                applied++;
            }
        }

        WebStudioUtils.addInfoMessage("Tags were added to " + applied + " projects.");
    }

    public static class ProjectTags {
        private final OpenLProject project;
        private final String projectName;
        private final String repoName;
        private boolean fillTags;

        ProjectTags(OpenLProject project, String projectName, String repoName) {
            this.project = project;
            this.projectName = projectName;
            this.repoName = repoName;
            fillTags = true;
        }

        public OpenLProject getProject() {
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

        public List<Tag> getTags() {
            return project.getTags();
        }
    }
}
