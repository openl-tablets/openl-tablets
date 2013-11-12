package org.openl.rules.webstudio.web.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ProjectDescriptorValidator;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import com.thoughtworks.xstream.XStreamException;

@ManagedBean
@ViewScoped
public class RepositoryProjectRulesConfig implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String RULES_CONFIGURATION_FILE = "rules.xml";
    private final Log log = LogFactory.getLog(RepositoryProjectRulesConfig.class);

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    private WebStudio studio = WebStudioUtils.getWebStudio(true);

    private final IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private final ProjectDescriptorValidator validator = new ProjectDescriptorValidator();

    private ProjectDescriptor descriptor;
    private UserWorkspaceProject lastProject;

    public RepositoryProjectRulesConfig() {
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public ProjectDescriptor getDescriptor() {
        UserWorkspaceProject project = getProject();
        if (lastProject != project) {
            descriptor = null;
            lastProject = project;
        }
        if (project == null) {
            return null;
        }
        if (descriptor == null) {
            if (hasRulesConfiguration(project)) {
                descriptor = loadRulesConfiguration(project);
            }
        }
        return descriptor;
    }

    public void addClasspath() {
        descriptor.getClasspath().add(new PathEntry());
    }

    public void deleteClasspath(PathEntry path) {
        Iterator<PathEntry> it = descriptor.getClasspath().iterator();
        while (it.hasNext()) {
            if (it.next() == path) {
                it.remove();
                break;
            }
        }
    }

    public List<ModuleGuiWrapper> getModules() {
        if (descriptor == null) {
            return new ArrayList<ModuleGuiWrapper>();
        }
        return ModuleGuiWrapper.wrap(descriptor.getModules());
    }

    public void addModule() {
        List<Module> modules = descriptor.getModules();
        Module module = new Module();
        postProcess(module);
        modules.add(module);
    }

    public void deleteModule(ModuleGuiWrapper moduleWrapper) {
        Module module = moduleWrapper.getModule();

        Iterator<Module> it = descriptor.getModules().iterator();
        while (it.hasNext()) {
            if (it.next() == module) {
                it.remove();
                break;
            }
        }
    }

    public void createConfiguration() {
        descriptor = new ProjectDescriptor();
        postProcess(descriptor);

        ProjectDescriptor p = studio.getProjectByName(getProject().getName());
        if (p != null) {
            descriptor.setId(p.getId());
            descriptor.setName(p.getName());
            descriptor.setComment(p.getComment());
            if (p.getClasspath() != null) {
                descriptor.setClasspath(p.getClasspath());
            }
            if (p.getModules() != null) {
                for (Module m : p.getModules()) {
                    Module module = new Module();
                    postProcess(module);
                    module.setName(m.getName());
                    module.setType(m.getType());
                    descriptor.getModules().add(module);
                }
            }
        }
    }

    public void deleteConfiguration() {
        UserWorkspaceProject project = getProject();
        if (hasRulesConfiguration(project)) {
            try {
                project.deleteArtefact(RULES_CONFIGURATION_FILE);
                repositoryTreeState.refreshSelectedNode();
                studio.reset(ReloadType.FORCED);
            } catch (ProjectException e) {
                FacesUtils.addErrorMessage("Cannot delete " + RULES_CONFIGURATION_FILE + " file");
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        descriptor = null;
    }

    public void saveConfiguration() {
        try {
            clean(descriptor);
            validator.validate(descriptor);

            UserWorkspaceProject project = getProject();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(serializer.serialize(descriptor).getBytes());

            if (project.hasArtefact(RULES_CONFIGURATION_FILE)) {
                AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_CONFIGURATION_FILE);
                artefact.setContent(inputStream);
            } else {
                project.addResource(RULES_CONFIGURATION_FILE, inputStream);
                repositoryTreeState.refreshSelectedNode();
                studio.reset(ReloadType.FORCED);
            }
        } catch (ProjectException e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage("Cannot save " + RULES_CONFIGURATION_FILE + " file");
        } catch (ValidationException e) {
            log.error(e.getMessage(), e);
            FacesUtils.addErrorMessage("Cannot save " + RULES_CONFIGURATION_FILE + " file. " + e.getMessage());
        }
        postProcess(descriptor);
    }

    private UserWorkspaceProject getProject() {
        return repositoryTreeState.getSelectedProject();
    }

    private boolean hasRulesConfiguration(UserWorkspaceProject project) {
        return project.hasArtefact(RULES_CONFIGURATION_FILE);
    }

    private ProjectDescriptor loadRulesConfiguration(UserWorkspaceProject project) {
        InputStream content = null;
        try {
            AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_CONFIGURATION_FILE);
            content = artefact.getContent();
            ProjectDescriptor d = serializer.deserialize(content);
            postProcess(d);
            return d;
        } catch (ProjectException e) {
            FacesUtils.addErrorMessage("Cannot read " + RULES_CONFIGURATION_FILE + " file");
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        } catch (XStreamException e) {
            FacesUtils.addErrorMessage("Cannot parse " + RULES_CONFIGURATION_FILE + " file");
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        } finally {
            IOUtils.closeQuietly(content);
        }

        return null;
    }

    private void postProcess(ProjectDescriptor descriptor) {
        if (descriptor.getClasspath() == null) {
            descriptor.setClasspath(new ArrayList<PathEntry>());
        }

        if (descriptor.getModules() == null) {
            descriptor.setModules(new ArrayList<Module>());
        }

        for (Module module : descriptor.getModules()) {
            postProcess(module);
        }
    }

    private void postProcess(Module module) {
        if (module.getRulesRootPath() == null) {
            module.setRulesRootPath(new PathEntry());
        }
        if (module.getMethodFilter() == null) {
            module.setMethodFilter(new MethodFilter());
        }
        if (module.getMethodFilter().getExcludes() == null) {
            module.getMethodFilter().setExcludes(new HashSet<String>());
        }
        if (module.getMethodFilter().getIncludes() == null) {
            module.getMethodFilter().setIncludes(new HashSet<String>());
        }
    }

    private void clean(ProjectDescriptor descriptor) {
        if (CollectionUtils.isEmpty(descriptor.getClasspath())) {
            descriptor.setClasspath(null);
        }

        List<Module> modules = descriptor.getModules();
        if (CollectionUtils.isEmpty(modules)) {
            descriptor.setModules(null);
            return;
        }

        for (Module module : modules) {
            PathEntry rulesRootPath = module.getRulesRootPath();
            if (rulesRootPath != null && StringUtils.isEmpty(rulesRootPath.getPath())) {
                module.setRulesRootPath(null);
            }

            MethodFilter methodFilter = module.getMethodFilter();
            if (methodFilter != null) {
                if (CollectionUtils.isEmpty(methodFilter.getIncludes())
                        && CollectionUtils.isEmpty(methodFilter.getExcludes())) {
                    module.setMethodFilter(null);
                }
            }
        }
    }

}
