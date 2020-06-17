package org.openl.rules.webstudio.web.repository;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.springframework.stereotype.Service;

/**
 * @author Aleh Bykhavets
 */
@Service
@ViewScope
public class SmartRedeployController extends AbstractSmartRedeployController {

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
    }

    public void initProject() {
        reset();
        currentProject = getSelectedProject();
    }
}
