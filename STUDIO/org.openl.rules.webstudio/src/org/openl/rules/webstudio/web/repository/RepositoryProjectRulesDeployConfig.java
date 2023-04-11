package org.openl.rules.webstudio.web.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.xml.bind.JAXBException;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.RulesDeploySerializerFactory;
import org.openl.rules.project.xml.SupportedVersion;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.permission.AclPermissionsSets;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@ViewScope
public class RepositoryProjectRulesDeployConfig {
    private static final String RULES_DEPLOY_CONFIGURATION_FILE = "rules-deploy.xml";
    private final Logger log = LoggerFactory.getLogger(RepositoryProjectRulesDeployConfig.class);

    private final RepositoryTreeState repositoryTreeState;
    private final RulesDeploySerializerFactory rulesDeploySerializerFactory;

    private final WebStudio studio = WebStudioUtils.getWebStudio(true);

    private final XmlRulesDeployGuiWrapperSerializer serializer;

    private RulesDeployGuiWrapper rulesDeploy;
    private UserWorkspaceProject lastProject;
    private String lastBranch;
    private String version;
    private boolean created;

    private final RepositoryAclService designRepositoryAclService;

    public RepositoryProjectRulesDeployConfig(RepositoryTreeState repositoryTreeState,
            RulesDeploySerializerFactory rulesDeploySerializerFactory,
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.repositoryTreeState = repositoryTreeState;
        this.rulesDeploySerializerFactory = rulesDeploySerializerFactory;

        serializer = new XmlRulesDeployGuiWrapperSerializer(rulesDeploySerializerFactory);

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
        rulesDeploy = new RulesDeployGuiWrapper(new RulesDeploy(), getSupportedVersion());
        rulesDeploy.setProvideRuntimeContext(true);
        rulesDeploy.setPublishers(new RulesDeploy.PublisherType[] { RulesDeploy.PublisherType.RESTFUL });
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
                .toInputStream(serializer.serialize(rulesDeploy, getSupportedVersion(project)));

            if (project.hasArtefact(RULES_DEPLOY_CONFIGURATION_FILE)) {
                AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                if (!designRepositoryAclService.isGranted(artefact, List.of(AclPermission.EDIT))) {
                    WebStudioUtils.addErrorMessage(String.format("There is no permission for modifying '%s' file.",
                        ProjectArtifactUtils.extractResourceName(artefact)));
                    return;
                }
                artefact.setContent(inputStream);
            } else {
                if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.ADD))) {
                    WebStudioUtils.addErrorMessage(String.format("There is no permission for creating '%s/%s' file.",
                        ProjectArtifactUtils.extractResourceName(project),
                        RULES_DEPLOY_CONFIGURATION_FILE));
                    return;
                }
                project.addResource(RULES_DEPLOY_CONFIGURATION_FILE, inputStream);
                AProjectArtefact projectArtefact = project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                if (!designRepositoryAclService
                    .createAcl(projectArtefact, AclPermissionsSets.NEW_FILE_PERMISSIONS, true)) {
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

    private SupportedVersion getSupportedVersion() {
        return getSupportedVersion(getProject());
    }

    private SupportedVersion getSupportedVersion(UserWorkspaceProject project) {
        if (project.getRepository() instanceof FileSystemRepository) {
            File projectFolder = new File(((FileSystemRepository) project.getRepository()).getRoot(),
                project.getFolderPath());
            return rulesDeploySerializerFactory.getSupportedVersion(projectFolder);
        }
        return SupportedVersion.getLastVersion();
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
                return serializer.deserialize(sourceString, getSupportedVersion(project));
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

    public boolean isCanEditRulesDeploy() {
        UserWorkspaceProject project = getProject();
        try {
            if (project.hasArtefact(RULES_DEPLOY_CONFIGURATION_FILE)) {
                AProjectArtefact projectArtefact = project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                return designRepositoryAclService.isGranted(projectArtefact, List.of(AclPermission.EDIT));
            } else {
                return designRepositoryAclService.isGranted(project, List.of(AclPermission.ADD));
            }
        } catch (ProjectException e) {
            return false;
        }
    }

    public boolean isCanDeleteRulesDeploy() {
        UserWorkspaceProject project = getProject();
        try {
            AProjectArtefact projectArtefact = project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
            return designRepositoryAclService.isGranted(projectArtefact, List.of(AclPermission.DELETE));
        } catch (ProjectException e) {
            return false;
        }
    }

    public boolean isVersionSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_17) >= 0;
    }

    public boolean isPublishersSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_14) >= 0;
    }

    public boolean isAnnotationTemplateClassNameSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_16) >= 0;
    }

    public boolean isRmiServiceClassSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_16) >= 0;
    }

    public boolean isGroupsSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_17) >= 0;
    }
}
