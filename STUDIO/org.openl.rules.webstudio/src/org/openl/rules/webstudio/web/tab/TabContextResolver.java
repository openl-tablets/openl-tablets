package org.openl.rules.webstudio.web.tab;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Resolves a {@link TabContext} from the identity a tab sends with a request (repository, project, module).
 *
 * <p>Resolution reuses the session's opened-project registry and never mutates the session-global selection: it
 * looks up the project's own model, opening the module only when it is not already open. Returns {@code null}
 * when there is no session, the project cannot be resolved, or resolution fails for any reason, so callers fall
 * back to the session selection rather than failing the request.
 */
@Slf4j
public final class TabContextResolver {

    /** Request parameter names carrying a tab's identity (sent by the client on every editing request). */
    public static final String PARAM_REPOSITORY_ID = "tabRepositoryId";
    public static final String PARAM_PROJECT = "tabProject";
    public static final String PARAM_MODULE = "tabModule";

    private TabContextResolver() {
    }

    /**
     * Resolve from a request's parameters using the {@code tab*} parameter names.
     */
    public static TabContext resolve(Function<String, String> parameter) {
        return resolve(parameter.apply(PARAM_REPOSITORY_ID),
                parameter.apply(PARAM_PROJECT),
                parameter.apply(PARAM_MODULE));
    }

    public static TabContext resolve(String repositoryId, String projectName, String moduleName) {
        if (repositoryId == null || projectName == null) {
            return null;
        }
        try {
            WebStudio studio = WebStudioUtils.getWebStudio();
            if (studio == null) {
                return null;
            }
            ProjectDescriptor descriptor = studio.getProjectByName(repositoryId, projectName);
            if (descriptor == null) {
                return null;
            }
            String projectFolder = descriptor.getProjectFolder().getFileName().toString();
            RulesProject project = studio.getProject(repositoryId, projectFolder);
            if (project == null) {
                // Cannot serve a coherent tab context without the project (descriptor-only would make
                // getCurrentProjectDescriptor() disagree with getCurrentProject()); fall back to the session.
                return null;
            }
            Module module = moduleName != null ? studio.getModule(descriptor, moduleName) : null;
            ProjectModel model = resolveModel(studio, project, descriptor, module);
            return new TabContext(repositoryId, descriptor, project, module, model);
        } catch (RuntimeException e) {
            // Never let per-tab resolution break a request; fall back to the session selection.
            log.debug("Failed to resolve tab context for {}:{}", repositoryId, projectName, e);
            return null;
        }
    }

    /**
     * The project's already-opened model, or a freshly opened one when a module is known but no model is cached
     * yet (e.g. after the model was evicted). No model when the project has no opened module.
     */
    private static ProjectModel resolveModel(WebStudio studio,
                                             RulesProject project,
                                             ProjectDescriptor descriptor,
                                             Module module) {
        if (project == null) {
            return null;
        }
        ProjectModel model = studio.getModelIfPresent(project);
        if (model == null && module != null) {
            model = studio.openProjectModule(project, descriptor, module);
        }
        return model;
    }
}
