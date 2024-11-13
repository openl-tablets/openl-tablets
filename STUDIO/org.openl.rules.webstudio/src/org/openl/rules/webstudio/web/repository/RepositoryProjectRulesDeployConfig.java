package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

@Service
@ViewScope
public class RepositoryProjectRulesDeployConfig {
    private static final String RULES_DEPLOY_CONFIGURATION_FILE = "rules-deploy.xml";
    private final Logger log = LoggerFactory.getLogger(RepositoryProjectRulesDeployConfig.class);

    private final RepositoryTreeState repositoryTreeState;

    private final WebStudio studio = WebStudioUtils.getWebStudio(true);

    private final XmlRulesDeployGuiWrapperSerializer serializer;

    private RulesDeployGuiWrapper rulesDeploy;
    private UserWorkspaceProject lastProject;
    private String lastBranch;
    private String version;
    private boolean created;

    private final RepositoryAclService designRepositoryAclService;

    public RepositoryProjectRulesDeployConfig(RepositoryTreeState repositoryTreeState,
                                              @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.repositoryTreeState = repositoryTreeState;

        serializer = new XmlRulesDeployGuiWrapperSerializer();

        this.designRepositoryAclService = designRepositoryAclService;
    }

    public RulesDeployGuiWrapper getRulesDeploy() {
        UserWorkspaceProject project = getProject();
        if (project == null) {
            return null;
        }

        if (lastProject != project || !Objects.equals(lastBranch, project.getBranch()) || !Objects.equals(version,
                project.getHistoryVersion())) {
            rulesDeploy = null;
            lastProject = project;
            lastBranch = project.getBranch();
            version = project.getHistoryVersion();
        }
        if (rulesDeploy == null) {
            if (hasRulesDeploy(project)) {
                created = false;
                rulesDeploy = loadRulesDeploy(project);
            }
        }
        return rulesDeploy;
    }

    public void createRulesDeploy() {
        created = true;
        rulesDeploy = new RulesDeployGuiWrapper(new RulesDeploy());
        rulesDeploy.setProvideRuntimeContext(true);
        rulesDeploy.setPublishers(new RulesDeploy.PublisherType[]{RulesDeploy.PublisherType.RESTFUL});
    }

    public void deleteRulesDeploy() {
        UserWorkspaceProject project = getProject();
        if (hasRulesDeploy(project)) {
            try {
                AProjectArtefact projectArtefact = project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                if (!designRepositoryAclService.isGranted(projectArtefact, List.of(AclPermission.DELETE))) {
                    WebStudioUtils.addErrorMessage(String.format("There is no permission for deleting '%s' file.",
                            ProjectArtifactUtils.extractResourceName(projectArtefact)));
                    return;
                }
                project.deleteArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                repositoryTreeState.refreshSelectedNode();
                studio.reset();
            } catch (ProjectException e) {
                WebStudioUtils.addErrorMessage("Failed to delete '" + RULES_DEPLOY_CONFIGURATION_FILE + "' file.");
                log.error(e.getMessage(), e);
            }
        }

        created = false;
        rulesDeploy = null;
    }

    public boolean isCreated() {
        return created;
    }

    public void saveRulesDeploy() {
        try {
            UserWorkspaceProject project = getProject();

            InputStream inputStream = IOUtils
                    .toInputStream(serializer.serialize(rulesDeploy));

            if (project.hasArtefact(RULES_DEPLOY_CONFIGURATION_FILE)) {
                AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                if (!designRepositoryAclService.isGranted(artefact, List.of(AclPermission.WRITE))) {
                    WebStudioUtils.addErrorMessage(String.format("There is no permission for modifying '%s' file.",
                            ProjectArtifactUtils.extractResourceName(artefact)));
                    return;
                }
                artefact.setContent(inputStream);
            } else {
                if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.CREATE))) {
                    WebStudioUtils.addErrorMessage(String.format("There is no permission for creating '%s/%s' file.",
                            ProjectArtifactUtils.extractResourceName(project),
                            RULES_DEPLOY_CONFIGURATION_FILE));
                    return;
                }
                project.addResource(RULES_DEPLOY_CONFIGURATION_FILE, inputStream);
                AProjectArtefact projectArtefact = project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                if (!designRepositoryAclService.hasAcl(projectArtefact) && !designRepositoryAclService
                        .createAcl(projectArtefact, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)) {
                    String message = String.format("Granting permissions to a new file '%s' is failed.",
                            ProjectArtifactUtils.extractResourceName(projectArtefact));
                    WebStudioUtils.addErrorMessage(message);
                }
                repositoryTreeState.refreshSelectedNode();
                studio.reset();
            }
            created = false;
        } catch (ProjectException | JAXBException | IOException e) {
            WebStudioUtils.addErrorMessage("Failed to save save '" + RULES_DEPLOY_CONFIGURATION_FILE + "' file.");
            log.error(e.getMessage(), e);
        }
    }

    private UserWorkspaceProject getProject() {
        return repositoryTreeState.getSelectedProject();
    }

    private boolean hasRulesDeploy(UserWorkspaceProject project) {
        return project.hasArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
    }

    private RulesDeployGuiWrapper loadRulesDeploy(UserWorkspaceProject project) {
        try {
            AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
            try (var content = artefact.getContent()) {
                var sourceString = new String(content.readAllBytes(), StandardCharsets.UTF_8);
                return serializer.deserialize(sourceString);
            }
        } catch (IOException | ProjectException e) {
            WebStudioUtils.addErrorMessage("Failed to read '" + RULES_DEPLOY_CONFIGURATION_FILE + "' file.");
            log.error(e.getMessage(), e);
        } catch (JAXBException e) {
            WebStudioUtils.addErrorMessage("Failed to parse '" + RULES_DEPLOY_CONFIGURATION_FILE + " file.");
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public void validateServiceName(FacesContext context, UIComponent toValidate, Object value) {
        String name = (String) value;

        if (StringUtils.isNotBlank(name)) {
            String message = "Invalid service name: only latin letters, numbers and _ are allowed, name must begin with a letter";
            WebStudioUtils.validate(name.matches("[a-zA-Z][a-zA-Z_\\-\\d]*"), message);
        }
    }

    public void validateServiceClass(FacesContext context, UIComponent toValidate, Object value) {
        String className = (String) value;
        if (StringUtils.isNotBlank(className)) {
            WebStudioUtils.validate(className.matches("([\\w$]+\\.)*[\\w$]+"), "Invalid class name");
        }
    }

    private boolean isCurrentBranchProtected(UserWorkspaceProject selectedProject) {
        Repository repo = selectedProject.getDesignRepository();
        if (repo != null && repo.supports().branches()) {
            return ((BranchRepository) repo).isBranchProtected(selectedProject.getBranch());
        }
        return false;
    }

    public boolean isCanEditRulesDeploy() {
        UserWorkspaceProject project = getProject();
        if (!project.isOpenedForEditing() || isCurrentBranchProtected(project)) {
            return false;
        }
        try {
            if (project.hasArtefact(RULES_DEPLOY_CONFIGURATION_FILE)) {
                AProjectArtefact projectArtefact = project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                return designRepositoryAclService.isGranted(projectArtefact, List.of(AclPermission.WRITE));
            } else {
                return designRepositoryAclService.isGranted(project, List.of(AclPermission.CREATE));
            }
        } catch (ProjectException e) {
            return false;
        }
    }

    public boolean isCanDeleteRulesDeploy() {
        UserWorkspaceProject project = getProject();
        if (!project.isOpenedForEditing() || isCurrentBranchProtected(project)) {
            return false;
        }
        try {
            AProjectArtefact projectArtefact = project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
            return designRepositoryAclService.isGranted(projectArtefact, List.of(AclPermission.DELETE));
        } catch (ProjectException e) {
            return false;
        }
    }

    public boolean isVersionSupported() {
        return true;
    }

    public boolean isPublishersSupported() {
        return true;
    }

    public boolean isAnnotationTemplateClassNameSupported() {
        return true;
    }

    public boolean isGroupsSupported() {
        return true;
    }
}
