package org.openl.rules.webstudio.web.repository;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;

/**
 * @author Aleh Bykhavets
 */
@ManagedBean
@ViewScoped
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
