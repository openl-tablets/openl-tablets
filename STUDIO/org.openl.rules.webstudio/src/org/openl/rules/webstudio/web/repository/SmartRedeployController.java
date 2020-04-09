package org.openl.rules.webstudio.web.repository;

import javax.faces.event.AjaxBehaviorEvent;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.springframework.stereotype.Controller;

/**
 * @author Aleh Bykhavets
 */
@Controller
@ViewScope
public class SmartRedeployController extends AbstractSmartRedeployController {

    private boolean loading = true;

    @Override
    public AProject getSelectedProject() {
        AProjectArtefact artefact = repositoryTreeState.getSelectedNode().getData();
        if (artefact instanceof AProject) {
            return (AProject) artefact;
        }
        return null;
    }

    @Override
    public void reset() {
        setRepositoryConfigName(null);
        items = null;
        currentProject = null;
        loading = true;
    }

    public void openDialogListener(AjaxBehaviorEvent event) {
        reset();
        currentProject = getSelectedProject();
        loading = false;
    }

    public boolean isLoading() {
        return loading;
    }
}
